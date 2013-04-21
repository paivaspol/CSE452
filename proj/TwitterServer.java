import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;

/**
 * Twitter Server that will handle requests from Twitter Client
 * 
 * @author vaspol, leelee
 */

public class TwitterServer extends RIONode {

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
  public static final String SUCCESS = "SUCCESS\n";
  public static final String FAILURE = "FAILURE\n";

  /**
   * File names
   */
  private static final String TEMP_FILENAME = ".tmp";
  private static final String USERS_FILENAME = "users.txt";
  private static final String LOGIN_FILENAME = "login.txt";

  /**
   * An instance of GSON for serializing and deserializing JSON objects
   */
  private final Gson gson;

  /**
   * Constructs the server side
   */
  public TwitterServer() {
    gson = new GsonBuilder().create();
  }

  /**
   * Call upon starting the server
   */
  @Override
  public void start() {
    Utils.logOutput(super.addr, "Starting up...");

    // Generate a user-level synoptic event to indicate that the node started.
    logSynopticEvent("started");

    // server might just recover from a failure, so need to check
    // tmp file in case of it was in the middle of something when it crashed
    File f = new File(TEMP_FILENAME);
    if (f.exists()) {
      resumeAppendExecution();
    }

    File userFile = new File(USERS_FILENAME);
    File loginFile = new File(LOGIN_FILENAME);
    try {
      if (!userFile.exists()) {
        if (!userFile.createNewFile()) {
          throw new IOException("cannot create file: " + userFile.getName());
        }
      }
      if (!loginFile.exists()) {
        if (!loginFile.createNewFile()) {
          throw new IOException("cannot create file: " + loginFile.getName());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parsing the bytes array to string representation
   */
  @Override
  public String packetBytesToString(byte[] bytes) {
    RIOPacket packet = RIOPacket.unpack(bytes);
    if (packet == null) {
      return super.packetBytesToString(bytes);
    }
    return packet.toString();
  }

  /**
   * Called when the server received a packet
   * 
   * @param from the node it received the packet from
   * @param protocol which protocol it conforms to
   * @param msg the message
   */
  @Override
  public void onRIOReceive(Integer from, int protocol, byte[] msg) {
    // first check whether it is a valid protocol or not
    if (!Protocol.isPktProtocolValid(protocol)) {
      Utils.logError(from, "unknown protocol: " + protocol);
      return;
    }

    String jsonStr = packetBytesToString(msg);
    TwitterProtocol request = gson.fromJson(jsonStr, TwitterProtocol.class);
    String collection = request.getCollection();
    String data = request.getData();
    String responseData = SUCCESS;
    try {
      if (request.getMethod().equals(CREATE)) {
        Utils.logOutput(super.addr, "Creating a tweet entry...");
        if (!createFile(collection, data)) {
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
    }
    // send back the damn respond!
    TwitterProtocol response = new TwitterProtocol(request);
    response.setData(responseData);
  }

  /**
   * accept commands
   */
  @Override
  public void onCommand(String command) {
    // server shouldn't be accepting any command.
  }

  // creates a file with the collection name
  private boolean createFile(String collectionName, String data) throws IOException {
    File newFile = new File(collectionName);
    return newFile.createNewFile();
  }

  // returns all the entries from the file associated to the reader object
  private String readFile(String collectionName) throws IOException {
    StringBuilder fileContent = new StringBuilder();
    PersistentStorageReader reader = super.getReader(collectionName);
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
    PersistentStorageReader reader = super.getReader(collectionName);
    PersistentStorageWriter writer = super.getWriter(collectionName, false);
    PersistentStorageWriter tempFileWriter = super.getWriter(TEMP_FILENAME, false);
    StringBuilder oldContent = new StringBuilder(collectionName + "\n");
    readWholeFile(reader, oldContent);
    // first, write the tmp file
    tempFileWriter.write(oldContent.toString());
    // append the new content
    writer.write(oldContent.append(data).toString());
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
    PersistentStorageWriter writer = super.getWriter(collectionName, false);
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
    PersistentStorageWriter writer = super.getWriter(collectionName, false);
    PersistentStorageReader reader = super.getReader(collectionName);
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
      PersistentStorageReader reader = super.getReader(TEMP_FILENAME);
      PersistentStorageWriter tmpFileWriter = super.getWriter(TEMP_FILENAME, false);
      if (reader.ready()) {
        String filename = reader.readLine();
        StringBuilder oldContent = new StringBuilder();
        readWholeFile(reader, oldContent);
        PersistentStorageWriter revertFile = super.getWriter(filename, false);
        char[] oldContentChars = new char[oldContent.length()];
        oldContent.getChars(0, oldContent.length(), oldContentChars, 0);
        revertFile.write(oldContentChars);
      }
      tmpFileWriter.delete();
      tmpFileWriter.close();
    } catch (IOException e) {
      Utils.logError(super.addr, e.getMessage());
    }
  }

  // reads the whole file in the reader into the oldContent variable
  private void readWholeFile(PersistentStorageReader reader, StringBuilder builder) throws IOException {
    String temp = "";
    while ((temp = reader.readLine()) != null) {
      builder.append(temp);
    }
  }
}
