/**
 * Holds the JSON Representation for the Twitter protocol
 * 
 */

public class TwitterProtocol {

  /** method name */
  private String method;
  /** collection (filename) */
  private String collection;
  /** data */
  private String data;
  /** unique hash code */
  private String hash;
  /** transaction id of the msg */
  private int transactionId;

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   * @param operator the operator for getting the data
   */
  public TwitterProtocol(String method, String collection, String data, String hash, int transactionId) {
    this.method = method;
    this.collection = collection;
    this.data = data;
    this.hash = hash;
    this.transactionId = transactionId;
  }

  /**
   * Constructs a new structure to hold for communication
   * 
   * @param twitterProtocol another twitter protocol
   */
  public TwitterProtocol(TwitterProtocol twitterProtocol) {
    method = twitterProtocol.method;
    collection = twitterProtocol.collection;
    data = twitterProtocol.data;
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

  /**
   * @return the hash of the msg
   */
  public String getHash() {
    return hash;
  }

  /**
   * @return the transaction id
   */
  public int getTransactionId() {
    return transactionId;
  }

  /**
   * Sets the method
   * 
   * @param method the method being set
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * Sets the collection
   * 
   * @param collection the collection being set
   */
  public void setCollection(String collection) {
    this.collection = collection;
  }

  /**
   * Sets the data
   * 
   * @param data the data being set
   */
  public void setData(String data) {
    this.data = data;
  }

  /**
   * Sets the hash
   * 
   * @param hash the hash being set
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /**
   * @return a byte representation of the message
   */
  public byte[] toBytes() {
    return TwitterNodeWrapper.GSON.toJson(this).getBytes();
  }
}
