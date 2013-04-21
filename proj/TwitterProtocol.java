
/**
 * 
 * @author leelee
 * 
 */

public class TwitterProtocol {

  private final String method;
  private final String collection;
  private final JsonObject data;
  private final String key;
  private final String operation;

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   * @param operator the operator for getting the data
   */
  public TwitterProtocol(String method, String collection, Entry data) {
    this.method = method;
    this.collection = collection;
    this.data = data;
    this.key = key;
    this.operation = operation;
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
  public JsonObject getData() {
    return data;
  }
  
  public String getKey() {
	  return key;
  }
  
  public String getOperation() {
	  return operation;
  }
}
