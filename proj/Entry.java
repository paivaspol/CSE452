import java.util.Date;

import com.google.gson.JsonObject;

/**
 * 
 * @author leelee
 *
 */
public class Entry {
	
	private Date time;
	private int machineId;
	private int counter;
	protected JsonObject jsonObject;
	private String hash;
	
	private static int counterGenerator = 0;
	
	// A hash is consist of 32 bits for time,
	// 12 bits for machine identification
	// (for future assignments that involved multiple machines), 20 (1048576) bits for counter
	public Entry(int machineId) {
		if (machineId < 0 || machineId >= 4096) {
			throw new IllegalArgumentException("machindId should be within 0 to 4095");
		}
		this.time = new Date();
		this.machineId = machineId;

		this.counter = counterGenerator++;
		if (counterGenerator >= 1048576) {
			counterGenerator = 0;
			this.counter = counterGenerator;
		}
		this.jsonObject = new JsonObject();
		// generate a new hash
		componentsToHash();
	}
	
	public Entry(String hash, JsonObject jsonObject) {
		this.hash = hash;
		this.jsonObject = jsonObject;
		hashToComponents();
	}
	
	public JsonObject getJsonObject() {
		return jsonObject;
	}
	
	private void componentsToHash() {
		long temp = (this.time.getTime()/1000 << 32) + (this.machineId << 20) + this.counter;
		System.out.println("temp " + temp);
		this.hash = Long.toString(temp);
	}
	
	private void hashToComponents() {
		long temp = Long.parseLong(this.hash);
		this.time = new Date((temp >> 32) * 1000); 
		this.counter = (int)(temp & 1048575);
		this.machineId = (int) ((temp & 4293918720L) >> 20);
	}
	
	public String getHash() {
		return hash;
	}
	
	public int getMachineId() {
		return machineId;
	}
	
	public int getCounter() {
		return counter;
	}
	
	public Date getTime() {
		return time;
	}
}
