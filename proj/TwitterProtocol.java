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
  /** the timestamp for transaction */
  private long transactionTimestamp;
  /** the timestamp for every packet issued by server */
  private long persistentTimestamp;
  /** the proposal number. */
  private int proposalNumber;

  /** Send in an accept packet. */
  private String acceptValue;
  /** Send in an accepted packet. */
  private String acceptedValue;
  /** Send from server to paxos. */
  private String fileServerRequestValue;
  /** Send fr0om paxos to server. */
  private String executeValue;
  private String promiseValue;
  private String consensusValue;

  public String getConsensusValue() {
    return consensusValue;
  }

  public void setConsensusValue(String consensusValue) {
    this.consensusValue = consensusValue;
  }

  public String getPromiseValue() {
    return promiseValue;
  }

  public void setPromiseValue(String promiseValue) {
    this.promiseValue = promiseValue;
  }
  
  

  /**
   * Constructs a new structure to hold information for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   * @param hash the hash that represents this request
   */
  public TwitterProtocol(String method, String collection, String data, String hash) {
    this.method = method;
    this.collection = collection;
    this.data = data;
    this.hash = hash;
  }

  /**
   * Constructs a new structure to hold information for communication
   * 
   * @param method the method being called
   * @param collection the target collection
   * @param data extra information
   * @param hash the hash that represents this request
   * @param timestamp the transaction timestamp
   */
  public TwitterProtocol(String method, String collection, String data, String hash, long timestamp,
      long persistentTimestmp) {
    this.method = method;
    this.collection = collection;
    this.data = data;
    this.hash = hash;
    transactionTimestamp = timestamp;
    persistentTimestamp = persistentTimestmp;
  }

  /**
   * Constructs a new structure to hold information for communication
   * 
   * @param method the method being called
   * @param hash the hash that represents this request
   */
  public TwitterProtocol(String method, String hash) {
    this.method = method;
    this.hash = hash;
  }

  /**
   * Constructs a new structure to hold information for communication
   * 
   * @param method the method being called
   * @param hash the hash that represents this request
   * @param timestamp the transaction timestamp
   */
  public TwitterProtocol(String method, String hash, long timestamp) {
    this.method = method;
    this.hash = hash;
    transactionTimestamp = timestamp;
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
    hash = twitterProtocol.hash;
    transactionTimestamp = twitterProtocol.transactionTimestamp;
  }
  
  /**
   * sets the logValue
   * @param logValue the value
   */
  public void setAcceptValue(String acceptValue) {
    this.acceptValue = acceptValue;
  }
  
  /**
   * returns the logvalue
   * @return
   */
  public String getAcceptValue() {
    return this.acceptValue;
  }
  
  /**
   * sets the logValue
   * @param logValue the value
   */
  public void setAcceptedValue(String acceptedValue) {
    this.acceptedValue = acceptedValue;
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
   * 
   * @return the hash
   */
  public String getHash() {
    return hash;
  }

  /**
   * 
   * @return
   */
  public long getTimestamp() {
    return transactionTimestamp;
  }

  /**
   * 
   * @return
   */
  public long getPersistentTimestamp() {
    return persistentTimestamp;
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
   * @param hash The unique hash
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /**
   * Sets the timestamp
   * 
   * @param timestamp the transaction timestamp
   */
  public void setTimestamp(long timestamp) {
    transactionTimestamp = timestamp;
  }

  public void setPersistentTimestamp(long pTime) {
    persistentTimestamp = pTime;
  }

  /**
   * 
   * @return this object in json bytes
   */
  public byte[] toBytes() {
    return TwitterNodeWrapper.GSON.toJson(this).getBytes();
  }
  
  public void setProposalNumber(int proposalNumber) {
	  this.proposalNumber = proposalNumber; 
  }
  
  public int getProposalNumber() {
	  return proposalNumber;
  }
  
  public long getTransactionTimestamp() {
    return transactionTimestamp;
  }

  public void setTransactionTimestamp(long transactionTimestamp) {
    this.transactionTimestamp = transactionTimestamp;
  }

  public String getFileServerRequestValue() {
    return fileServerRequestValue;
  }

  public void setFileServerRequestValue(String fileServerRequestValue) {
    this.fileServerRequestValue = fileServerRequestValue;
  }

  public String getExecuteValue() {
    return executeValue;
  }

  public void setExecuteValue(String executeValue) {
    this.executeValue = executeValue;
  }

  public String getAcceptedValue() {
    return acceptedValue;
  }
}
