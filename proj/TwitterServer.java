import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;

/**
 * Twitter Server that will handle requests from Twitter Client
 * 
 * @author vaspol
 */

public class TwitterServer extends RIONode {

  /**
   * Method name constants for server RPC
   */
  public static final String CREATE = "create";
  public static final String READ = "read";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

  private static final String TEMP_FILENAME = ".tmp";

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
    TwitterProtocol result = gson.fromJson(jsonStr, TwitterProtocol.class);
    JsonObject data = result.getData();
    String key = result.getKey();
    String operation = result.getCollection();
    if (result.getMethod().equals(CREATE)) {
      Utils.logOutput(super.addr, "Creating a tweet entry...");
      createEntry(result.getCollection(), data);
    } else if (result.getMethod().equals(READ)) {
      Utils.logOutput(super.addr, "Reading a file...");
      String entries = readEntries(result.getCollection(), data);
    } else if (result.getMethod().equals(UPDATE)) {
      Utils.logOutput(super.addr, "Updating a file...");
    } else if (result.getMethod().equals(DELETE)) {
      Utils.logOutput(super.addr, "Deleting a file...");

    } else {
      throw new RuntimeException("Command not supported by the server");
    }
  }

  /**
   * accept commands
   */
  @Override
  public void onCommand(String command) {

  }

  // appends the file with the tweet data
  // writing to user-[collectionName]
  private void createEntry(String collectionName, Entry data) {
    // assuming that username field is always going to be there
    String filename = generateFilename(collectionName, data);
    try {
      PersistentStorageReader reader = super.getReader(filename);
      PersistentStorageWriter writer = super.getWriter(filename, false);
      PersistentStorageWriter tempFileWriter = super.getWriter(TEMP_FILENAME, false);
      List<JsonObject> oldContent = new LinkedList<JsonObject>();
      readWholeFile(reader, oldContent, collectionName);
      StringBuilder builder = generateFile(collectionName, data, filename, oldContent);
      // append the new content
      writer.write(builder.toString());
      tempFileWriter.delete();
      // close all the file connections
      reader.close();
      writer.close();
      tempFileWriter.close();
    } catch (IOException e) {
      Utils.logError(super.addr, e.getMessage());
    }
  }

  private StringBuilder generateFile(String collectionName, Entry data, String filename, List<JsonObject> oldContent) {
    StringBuilder builder = new StringBuilder(filename + "\n" + collectionName + "\n");
    for (JsonObject entry : oldContent) {
      builder.append(gson.toJson(entry) + "\n");
    }
    builder.append(gson.toJson(data));
    return builder;
  }

  // returns all the entries from the file associated to the reader object
  private List<Entry> readEntries(String collectionName, Entry data) {
    String filename = generateFilename(collectionName, data);
    StringBuilder fileContent = new StringBuilder();
    try {
      PersistentStorageReader reader = super.getReader(filename);
      readWholeFile(reader, fileContent);
    } catch (IOException e) {
      Utils.logError(super.addr, e.getMessage());
    }
    return fileContent.toString();
  }

  private void updateFile(PersistentStorageWriter writer) {

  }

  private void deleteEntry(String collectionName, Entry data) {
    String filename = generateFilename(collectionName, data);
    try {
      PersistentStorageWriter writer = super.getWriter(filename, false);

    } catch (IOException e) {
      Utils.logError(super.addr, e.getMessage());
    }
  }

  // Called when there is a temp file in the system after recovery
  // need to continue with all the appends
  private void resumeAppendExecution() {
    try {
      PersistentStorageReader reader = super.getReader(TEMP_FILENAME);
      PersistentStorageWriter tmpFileWriter = super.getWriter(TEMP_FILENAME, false);
      if (reader.ready()) {
        String filename = reader.readLine();
        List<JsonObject> oldContent = new LinkedList<JsonObject>();
        readWholeFile(reader, oldContent, collection);
        PersistentStorageWriter revertFile = super.getWriter(filename, false);
        char[] oldContentChars = new char[oldContent.size()];
        oldContent.getChars(0, oldContent.size(), oldContentChars, 0);
        revertFile.write(oldContentChars);
      }
      tmpFileWriter.delete();
      tmpFileWriter.close();
    } catch (IOException e) {
      Utils.logError(super.addr, e.getMessage());
    }
  }

  // reads the whole file in the reader into the oldContent variable
  private void readWholeFile(PersistentStorageReader reader, List<JsonObject> oldContent, String collection)
      throws IOException {
    String temp = "";
    while ((temp = reader.readLine()) != null) {
      JsonObject obj = gson.fromJson(temp, JsonObject.class);
      oldContent.add(obj);
    }
  }

  // generates the filename in the following format: username-collectionName
  private String generateFilename(String collectionName, Entry data) {
    String username = ((User) data).getUserName();
    String filename = username + "-" + collectionName;
    return filename;
  }
}
