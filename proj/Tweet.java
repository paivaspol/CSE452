/**
 * Placeholder for tweet.
 * 
 * @author leelee
 *
 */
public class Tweet extends Entry {

	public Tweet(int machineId) {
		super(machineId);
	}
	
	public void setContent(String content) {
		jsonObject.addProperty("content", content);
	}
	
	public String getContent() {
		return jsonObject.get("content").getAsString();
	}
}
