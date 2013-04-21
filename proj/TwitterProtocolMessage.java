import java.util.Map;

/**
 * Holds the structure for a request for GSON to parse and put it into this object structure
 * 
 * @author vaspol
 * 
 */

public class TwitterProtocolMessage {

  private final long hash;
  private final long id;
  private final String method;
  private final String collection;
  private final Map<String, Object> data;

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   */
  public TwitterProtocolMessage(String method, String collection, Map<String, Object> data, String hash, String id) {
    this.hash = Long.parseLong(hash);
    this.id = Long.parseLong(id);
    this.method = method;
    this.collection = collection;
    this.data = data;
  }

  /**
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * @return the hash
   */
  public long getHash() {
    return hash;
  }

  /**
   * @return the method
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return the collection
   */
  public String getCollection() {
    return collection;
  }

  /**
   * @return the data hold in this request
   */
  public Map<String, Object> getData() {
    return data;
  }

}
