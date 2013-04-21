
/**
 * 
 * @author leelee
 *
 */

public class TwitterProtocol {

  private final String method;
  private final String collection;
  private final Entry data;

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   */
  public TwitterProtocol(String method, String collection, Entry data) {
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
  public Entry getData() {
    return data;
  }

}
