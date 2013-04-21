/**
 * 
 * @author leelee
 * 
 */

public class TwitterProtocol {

  private final String method;
  private final String collection;
  private final Entry data;
  private final String operator;

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   * @param operator the operator for getting the data
   */
  public TwitterProtocol(String method, String collection, Entry data, String operator) {
    this.method = method;
    this.collection = collection;
    this.data = data;
    this.operator = operator;
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

  /**
   * @return the operator for querying the data from the server
   */
  public String getOperator() {
    return operator;
  }

}
