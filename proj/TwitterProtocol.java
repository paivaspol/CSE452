import com.google.gson.JsonObject;


/**
 * 
 * @author leelee
 * 
 */

public class TwitterProtocol {

  private final String method;
  private final String collection;
  private final String data;

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   * @param operator the operator for getting the data
   */
  public TwitterProtocol(String method, String collection, String data) {
    this.method = method;
    this.collection = collection;
    this.data = data;
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
  public String getData() {
    return data;
  }
}
