import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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

  /**
   * Response indicators
   */
  public static final String SUCCESS = "SUCCESS";
  public static final String FAILURE = "FAILURE";
  public static final String RESTART = "RESTART";
  
  /**
   * File names
   */
  private static final String TEMP_FILENAME = ".tmp";
  private static final String USERS_FILENAME = "users.txt";
  private static final String LOGIN_FILENAME = "login.txt";
  private static final String CONTACTED_CLIENTS = "clients.txt";

  /**
   * An instance of GSON for serializing and deserializing JSON objects
   */
  private final Gson gson;

  /** An instance of the wrapper for the nodes */
  private final TwitterNodeWrapper wrapper;

  /**
   * Constructs the server side
   */
  public TwitterServer(TwitterNodeWrapper wrapper) {
    gson = new GsonBuilder().create();
    this.wrapper = wrapper;
  }

  /**
   * Call upon starting the server
   */
  public void start() {
    // server might just recover from a failure, so need to check
    // tmp file in case of it was in the middle of something when it crashed
    File f = new File(TEMP_FILENAME);
    if (f.exists()) {
    	Utils.logOutput(wrapper.getAddr(), "Restoring file after crash at append");
      resumeAppendExecution();
    }

    try {
      if (!Utility.fileExists(wrapper, USERS_FILENAME)) {
        createFile(USERS_FILENAME);
      }
      if (!Utility.fileExists(wrapper, LOGIN_FILENAME)) {
        createFile(LOGIN_FILENAME);
      }

      // should tell all the clients (nodes) after upon restart, so they know that you're back :)
      if (Utility.fileExists(wrapper, CONTACTED_CLIENTS)) {
        PersistentStorageReader reader = wrapper.getReader(CONTACTED_CLIENTS);
        String clientNode = "";
        while ((clientNode = reader.readLine()) != null) {
          int nodeId = Integer.parseInt(clientNode);
          TwitterProtocol msg = new TwitterProtocol(RESTART, RESTART, RESTART);
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
    Utils.logOutput(wrapper.getAddr(), "received something!!");
    try {
      if (!Utility.fileExists(wrapper, CONTACTED_CLIENTS)) {
        createFile(CONTACTED_CLIENTS);
      }
      appendFile(CONTACTED_CLIENTS, String.valueOf(from));
    } catch (IOException e) {
      e.printStackTrace();
    }

    String jsonStr = wrapper.packetBytesToString(msg);
    TwitterProtocol request = gson.fromJson(jsonStr, TwitterProtocol.class);
    String collection = request.getCollection();
    String data = request.getData();
    String responseData = SUCCESS + "\n";
    try {
      if (request.getMethod().equals(CREATE)) {
        Utils.logOutput(wrapper.getAddr(), "Creating a tweet entry...");
        if (!createFile(collection)) {
          responseData = FAILURE;
        }
      } else if (request.getMethod().equals(READ)) {
        responseData += readFile(collection);
      } else if (request.getMethod().equals(APPEND)) {
        appendFile(collection, data);
      } else if (request.getMethod().equals(DELETE)) {
        deleteFile(collection);
      } else if (request.getMethod().equals(DELETE_LINES)) {
        responseData += deleteEntries(collection, data);
      } else {
        throw new RuntimeException("Command not supported by the server");
      }
    } catch (IOException e) {
      responseData = "FAIL\n";
      e.printStackTrace();
    }
    // send back the damn respond!
    TwitterProtocol response = new TwitterProtocol(request);
    response.setData(responseData);
    wrapper.RIOSend(from, protocol, response.toBytes());
  }

  /**
   * accept commands
   */
  public void onCommand(String command) {
    // server shouldn't be accepting any command.
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

  /**
   * Delete the whole file
   * 
   * @param collectionName the filename to be deleted
   * @throws IOException
   */
  private void deleteFile(String collectionName) throws IOException {
    PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
    writer.delete();
    writer.close();
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

  /*
   * Helper method for resuming append after server crash
   */
  private void resumeAppendExecution() {
    try {
      PersistentStorageReader reader = wrapper.getReader(TEMP_FILENAME);
      if (reader.ready()) {
        String filename = reader.readLine();
        StringBuilder oldContent = new StringBuilder();
        readWholeFile(reader, oldContent);
        PersistentStorageWriter revertFile = wrapper.getWriter(filename, false);
        char[] oldContentChars = new char[oldContent.length()];
        oldContent.getChars(0, oldContent.length(), oldContentChars, 0);
        revertFile.write(oldContentChars);
      }
      PersistentStorageWriter tmpFileWriter = wrapper.getWriter(TEMP_FILENAME, false);
      tmpFileWriter.delete();
      tmpFileWriter.close();
    } catch (IOException e) {
      Utils.logError(wrapper.getAddr(), e.getMessage());
    }
  }

  // reads the whole file in the reader into the oldContent variable
  private void readWholeFile(PersistentStorageReader reader, StringBuilder builder) throws IOException {
    Utils.logOutput(wrapper.getAddr(), "EXEC!");
	String temp = "";
    while ((temp = reader.readLine()) != null) {
      builder = builder.append(temp + "\n");
    }
  }
}
