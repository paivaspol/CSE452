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
	public TwitterProtocol(String method, String collection, String data, String hash, long timestamp) {
		this.method = method;
		this.collection = collection;
		this.data = data;
		this.hash = hash;
		this.transactionTimestamp = timestamp;
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
		this.transactionTimestamp = timestamp;
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
		this.transactionTimestamp = timestamp;
	}
	
	public void setPersistentTimestamp(long pTime) {
		this.persistentTimestamp = pTime;
	}

	/**
	 * 
	 * @return this object in json bytes
	 */
	public byte[] toBytes() {
		return TwitterNodeWrapper.GSON.toJson(this).getBytes();
	}
}
