/**
 * Placeholder for tweet.
 * 
 * @author leelee
 *
 */
public class Tweet extends Entry {

	private String content;
	
	public Tweet(int machineId) {
		super(machineId);
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return this.content;
	}
}
