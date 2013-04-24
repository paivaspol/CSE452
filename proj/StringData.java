
public class StringData extends Entry {

	private static final String DATA_KEY = "data";
	
	public StringData(int machineId) {
		super(machineId);
		// TODO Auto-generated constructor stub
	}
	
	public String getData() {
		return jsonObject.get(DATA_KEY).getAsString();
	}
	
	public void setData(String data) {
		jsonObject.addProperty(DATA_KEY, data);
	}
	
	public String toString() {
		return getData();
	}

}
