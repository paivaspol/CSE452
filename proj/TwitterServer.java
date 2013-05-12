import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

/**
 * Twitter Server that will handle requests from Twitter Client
 * 
 * @author vaspol, leelee
 */

public class TwitterServer {

  /**
   * Method name constants for server RPC
   */
  public static final String CREATE = "create";
  public static final String READ = "read";
  public static final String APPEND = "append";
  public static final String DELETE = "delete";
  public static final String DELETE_LINES = "deletelines";
  public static final String CHECK_LAST_UPDATE = "checklastupdate";
  public static final String BEGIN_TRANSACTION = "begintransaction";
  public static final String COMMIT = "commit";
  public static final String ABORT = "abort";
  public static final String INVALIDATE = "invalidate";
  public static final String ROLLBACK = "rollback";
  public static final String ABORTED = "aborted";

  /**
   * Response indicators
   */
  public static final String SUCCESS = "SUCCESS";
  public static final String FAILURE = "FAILURE";
  public static final String RESTART = "RESTART";

  /**
   * File names
   */
  public static final String TEMP_FILENAME = ".tmp";
  private static final String USERS_FILENAME = "users.txt";
  private static final String LOGIN_FILENAME = "login.txt";
  private static final String CONTACTED_CLIENTS = "clients.txt";
  private static final String LOG_FILENAME = "logs.txt";
  public static final String REDO_LOGGING_FILENAME = "log.txt";

  /**
   * An instance of GSON for serializing and deserializing JSON objects
   */
  private final Gson gson;

  /** An instance of the wrapper for the nodes */
  private final TwitterNodeWrapper wrapper;
  private final Set<Integer> connectedNodes;
  private final Set<String> pastRequests;
  private final Map<Integer, Integer> nodeToTxn;
  private final Map<Integer, Set<String>> txnToPastRequests;

  private long transactionCounter;

  private final FileManager fileManager;

  /**
   * Constructs the server side
   */
  public TwitterServer(TwitterNodeWrapper wrapper) {
    gson = new GsonBuilder().create();
    this.wrapper = wrapper;
    connectedNodes = new HashSet<Integer>();
    pastRequests = new HashSet<String>();
    transactionCounter = 0;
    fileManager = new FileManager(this);
    nodeToTxn = new HashMap<Integer, Integer>();
    txnToPastRequests = new HashMap<Integer, Set<String>>();
  }

  /**
   * Call upon starting the server
   */
  public void start() {
    // server might just recover from a failure, so need to check
    // tmp file in case of it was in the middle of something when it crashed
    if (Utility.fileExists(wrapper, TEMP_FILENAME)) {
      Utils.logOutput(wrapper.getAddr(), "Restoring file after crash at append");
      fileManager.onRecover();
    }

    try {
      if (!Utility.fileExists(wrapper, USERS_FILENAME)) {
        createFile(USERS_FILENAME);
      }
      if (!Utility.fileExists(wrapper, LOGIN_FILENAME)) {
        createFile(LOGIN_FILENAME);
      }
      if (!Utility.fileExists(wrapper, LOG_FILENAME)) {
        createFile(LOG_FILENAME);
      } else {
        String pastRequest = "";
        PersistentStorageReader reader = wrapper.getReader(LOG_FILENAME);
        while ((pastRequest = reader.readLine()) != null) {
          pastRequests.add(pastRequest);
        }
      }

      // should tell all the clients (nodes) after upon restart, so they know that you're back :)
      if (Utility.fileExists(wrapper, CONTACTED_CLIENTS)) {
        PersistentStorageReader reader = wrapper.getReader(CONTACTED_CLIENTS);
        String clientNode = "";
        while ((clientNode = reader.readLine()) != null) {
          int nodeId = Integer.parseInt(clientNode);
          connectedNodes.add(nodeId);
          TwitterProtocol msg = new TwitterProtocol(RESTART, RESTART, RESTART, new Entry(wrapper.getAddr()).getHash());
          wrapper.RIOSend(nodeId, Protocol.DATA, gson.toJson(msg).getBytes());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Called when the server received a packet
   * 
   * @param from the node it received the packet from
   * @param protocol which protocol it conforms to
   * @param msg the message
   */
  public void onRIOReceive(Integer from, int protocol, byte[] msg) {
    // first check whether it is a valid protocol or not
    if (!Protocol.isPktProtocolValid(protocol)) {
      Utils.logError(from, "unknown protocol: " + protocol);
      return;
    }

    try {
      if (!Utility.fileExists(wrapper, CONTACTED_CLIENTS)) {
        createFile(CONTACTED_CLIENTS);
      }
      if (!connectedNodes.contains(from)) {
        appendFile(CONTACTED_CLIENTS, String.valueOf(from));
        connectedNodes.add(from);
      }
      if (!nodeToTxn.containsKey(from)) {
        nodeToTxn.put(from, (int) transactionCounter);
        transactionCounter++;
      }
      if (!txnToPastRequests.containsKey(from)) {
        txnToPastRequests.put(from, new HashSet<String>());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    String jsonStr = wrapper.packetBytesToString(msg);
    TwitterProtocol request = gson.fromJson(jsonStr, TwitterProtocol.class);
    String collection = request.getCollection();
    String data = request.getData();
    String responseData = SUCCESS + "\n";
    String hash = request.getHash();
    long transactionId = request.getTimestamp();
    try {
      if (request.getMethod().equals(CREATE) || request.getMethod().equals(DELETE_LINES)
          || request.getMethod().equals(READ) || request.getMethod().equals(DELETE)) {
        responseData = fileManager.handleTransaction((int) transactionId, request.getMethod(), collection, data);
      } else if (request.getMethod().equals(APPEND)) {
        if (!txnToPastRequests.get(from).contains(hash)) {
          responseData = fileManager.handleTransaction((int) transactionId, request.getMethod(), collection, data);
        }
      } else if (request.getMethod().equals(COMMIT)) {
        responseData = fileManager.handleTransaction((int) transactionId, request.getMethod(), collection, data);
        nodeToTxn.remove(from);
        txnToPastRequests.remove(from);
        TwitterProtocol response = new TwitterProtocol(request);
        response.setData(responseData);
        wrapper.RIOSend(from, protocol, response.toBytes());
        for (Integer i : connectedNodes) {
          if (from != i) {
            TwitterProtocol invalidation = new TwitterProtocol(INVALIDATE, INVALIDATE, INVALIDATE, INVALIDATE);
            wrapper.RIOSend(i, protocol, invalidation.toBytes());
          }
        }
        return;
      } else if (request.getMethod().equals("TIMEOUT")) {
        TwitterProtocol response =
            new TwitterProtocol(RESTART, RESTART, RESTART, new Entry(wrapper.getAddr()).getHash());
        wrapper.RIOSend(from, protocol, response.toBytes());
        return;
      } else if (request.getMethod().equals(BEGIN_TRANSACTION)) {
        if (!txnToPastRequests.get(from).contains(hash)) {
          TwitterProtocol response =
              new TwitterProtocol(request.getMethod(), request.getCollection(), responseData, request.getHash(),
                  transactionCounter);
          wrapper.RIOSend(from, protocol, response.toBytes());
        } else {
          TwitterProtocol response =
              new TwitterProtocol(ROLLBACK, request.getCollection(), responseData, request.getHash(),
                  transactionCounter);
          wrapper.RIOSend(from, protocol, response.toBytes());
          nodeToTxn.remove(from);
          txnToPastRequests.remove(from);
        }
        return;
      } else if (request.getMethod().equals(RESTART)) {
        if (nodeToTxn.containsKey(from)) {
          fileManager.removeTransaction(nodeToTxn.get(from));
          nodeToTxn.remove(from);
          txnToPastRequests.remove(from);
        }
        return;
      } else if (request.getMethod().equals(ABORT)) {
        fileManager.handleTransaction((int) transactionId, request.getMethod(), collection, data);
        nodeToTxn.remove(from);
        TwitterProtocol twitterProtocol = new TwitterProtocol(ABORTED, null);
        wrapper.RIOSend(from, protocol, twitterProtocol.toBytes());
        return;
      } else {
        throw new RuntimeException("Command not supported by the server" + request.getMethod());
      }
    } catch (IOException e) {
      responseData = FAILURE + "\n";
      e.printStackTrace();
    }
    TwitterProtocol response = new TwitterProtocol(request);
    response.setData(responseData);
    wrapper.RIOSend(from, protocol, response.toBytes());
    txnToPastRequests.get(from).add(hash);
    try {
      appendFile(LOG_FILENAME, hash + "\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * accept commands
   * 
   * @throws IOException
   */
  public void onCommand(String command) throws IOException {
    String[] tokenized = command.split("\t");
    // by assumption: tokenized[0] = method, tokenized[1] = filename, other arguments
    if (tokenized[0].equals(CREATE)) {
      createFile(tokenized[1]);
    } else if (tokenized[0].equals(APPEND)) {
      appendFile(tokenized[1], tokenized[2]);
    } else if (tokenized[0].equals(DELETE)) {
      deleteFile(tokenized[1]);
    } else if (tokenized[0].equals(DELETE_LINES)) {
      deleteEntries(tokenized[1], tokenized[2]);
    } else if (tokenized[0].equals(READ)) {
      readFile(tokenized[1]);
    }
  }

  // creates a file with the collection name
  private boolean createFile(String collectionName) throws IOException {
    if (!Utility.fileExists(wrapper, collectionName)) {
      PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
      writer.write(new char[] {});
    }
    return true;
  }

  // returns all the entries from the file associated to the reader object
  private String readFile(String collectionName) throws IOException {
    StringBuilder fileContent = new StringBuilder();
    PersistentStorageReader reader = wrapper.getReader(collectionName);
    readWholeFile(reader, fileContent);
    return fileContent.toString();
  }

  /**
   * Delete the whole file
   * 
   * @param collectionName the filename to be deleted
   * @throws IOException
   */
  private void deleteFile(String collectionName) throws IOException {
    if (Utility.fileExists(wrapper, collectionName)) {
      PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
      writer.delete();
      writer.close();
    }
  }

  /**
   * Deletes all the entries that starts with the targetString
   * 
   * @param collectionName the filename
   * @param targetString the target string to be deleted
   * @return the lines that were deleted
   * @throws IOException
   */
  private String deleteEntries(String collectionName, String targetString) throws IOException {
    PersistentStorageReader reader = wrapper.getReader(collectionName);
    String temp = "";
    StringBuilder newFile = new StringBuilder();
    StringBuilder linesDeleted = new StringBuilder();
    while ((temp = reader.readLine()) != null) {
      if (!temp.startsWith(targetString)) {
        newFile.append(temp);
      } else {
        linesDeleted.append(temp);
      }
    }
    PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
    writer.write(newFile.toString());
    reader.close();
    writer.flush();
    writer.close();
    return linesDeleted.toString();
  }

  // reads the whole file in the reader into the oldContent variable
  public void readWholeFile(PersistentStorageReader reader, StringBuilder builder) throws IOException {
    String temp = "";
    while ((temp = reader.readLine()) != null) {
      builder = builder.append(temp + "\n");
    }
  }

  /**
   * Append a data to the end of the file
   * 
   * @param collectionName the filename
   * @param data the line to be appended at the end of the file
   * @throws IOException
   */
  private void appendFile(String collectionName, String data) throws IOException {
    PersistentStorageReader reader = wrapper.getReader(collectionName);
    PersistentStorageWriter tempFileWriter = wrapper.getWriter(TEMP_FILENAME, false);
    StringBuilder tempContent = new StringBuilder(collectionName + "\n");
    StringBuilder oldContent = new StringBuilder();
    readWholeFile(reader, oldContent);
    // first, write the tmp file
    tempContent = tempContent.append(oldContent);
    tempFileWriter.write(tempContent.toString());
    // append the new content
    oldContent = oldContent.append(data);
    PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
    writer.write(oldContent.toString());
    tempFileWriter.delete();
    // close all the file connections
    reader.close();
    writer.close();
    tempFileWriter.close();
  }

  RIONode getNode() {
    return wrapper;
  }

  PersistentStorageReader getPersistentStorageReader(String filename) throws IOException {
    return wrapper.getReader(filename);
  }

  PersistentStorageWriter getPersistentStorageWriter(String filename, boolean allowsAppend) throws IOException {
    return wrapper.getWriter(filename, allowsAppend);
  }
}
