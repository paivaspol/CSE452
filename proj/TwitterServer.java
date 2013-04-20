import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;

/**
 * Twitter Server that will handle requests from Twitter Client
 * 
 * @author vaspol
 */

public class TwitterServer extends RIONode {

  /**
   * JSON Keys for sending to Server
   */
  public static final String METHOD = "method";
  public static final String COLLECTION = "collection";
  public static final String DATA = "data";

  /**
   * Method name constants for server RPC
   */
  public static final String CREATE = "create";
  public static final String READ = "read";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

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
    @SuppressWarnings("unchecked")
    Map<String, Object> result = gson.fromJson(jsonStr, Map.class);

    String method = (String) result.get(METHOD);
    String collection = (String) result.get(COLLECTION);
    Map<String, Object> data = (Map<String, Object>) result.get(DATA);

    PersistentStorageWriter storageWriter;
    PersistentStorageReader storageReader;

    if (method.equals(CREATE)) {
      Utils.logOutput(super.addr, "Creating a tweet entry...");

    } else if (method.equals(READ)) {
      Utils.logOutput(super.addr, "Reading a file...");
    } else if (method.equals(UPDATE)) {
      Utils.logOutput(super.addr, "Updating a file...");
    } else if (method.equals(DELETE)) {
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
  private void createEntry(PersistentStorageWriter writer) {
  }

  // returns all the entries from the file associated to the reader object
  private void readEntries(PersistentStorageReader reader) {

  }

  private void updateFile(PersistentStorageWriter writer) {

  }

  private void deleteFile(PersistentStorageWriter writer) {

  }
}
