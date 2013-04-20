import com.google.gson.JsonObject;

/**
 * 
 * @author leelee
 *
 */
public class TwitterProtocol {
	
	private static final String METHOD_KEY = "method";
	private static final String COLLECTION_KEY = "collection";
	private static final String HASH_KEY = "hash";
	private static final String ID_KEY = "id";
	private static final String DATA_KEY = "data";
	
	private static int sequenceNumberGenerator = 0;
	private JsonObject obj;
	
	public TwitterProtocol(String method, String collection, JsonObject data) {
		obj = new JsonObject();
		obj.addProperty(METHOD_KEY, method);
		obj.addProperty(COLLECTION_KEY, collection);
		obj.addProperty(ID_KEY, sequenceNumberGenerator+=1);
		obj.add(DATA_KEY, data);
	}
	
	public TwitterProtocol(String method, String collection, String hash, JsonObject data) {
		obj = new JsonObject();
		obj.addProperty(METHOD_KEY, method);
		obj.addProperty(COLLECTION_KEY, collection);
		obj.addProperty(HASH_KEY, hash);
		obj.addProperty(ID_KEY, sequenceNumberGenerator+=1);
		obj.add("data", data);
	}
	
	public TwitterProtocol(String method, String collection, String hash) {
		obj = new JsonObject();
		obj.addProperty(METHOD_KEY, method);
		obj.addProperty(COLLECTION_KEY, collection);
		obj.addProperty(HASH_KEY, hash);
		obj.addProperty(ID_KEY, sequenceNumberGenerator+=1);
	}
	
	public String getMethod() {
		return obj.get(METHOD_KEY).getAsString();
	}
	
	public String getCollection() {
		return obj.get(COLLECTION_KEY).getAsString();
	}
	
	public String getHash() {
		return obj.get(HASH_KEY).getAsString();
	}
	
	public String getId() {
		return obj.get(ID_KEY).getAsString();
	}
	
	public JsonObject getData() {
		return obj.get(DATA_KEY).getAsJsonObject();
	}
	
	public byte[] getBytes() {
		return obj.toString().getBytes();
	}
}
