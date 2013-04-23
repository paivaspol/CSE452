import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.google.gson.Gson;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * 
 * @author leelee
 *
 */
public abstract class Function {
	protected RIONode rioNode;
	private Gson gson;
	protected List<Callback> eventList;
	protected Client client;
	
	public Function(Client client, RIONode rioNode) {
		this.rioNode = rioNode;
		gson = new Gson();
		eventList = null;
		this.client = client;
	}
	
	public abstract List<Callback> init();
	
	public void start() throws IllegalAccessException, InvocationTargetException {
		if (eventList != null && eventList.size() != 0) {
			eventList.get(0).invoke();
		}
	}
	
	public byte[] getBytesFromTwitterProtocol(TwitterProtocol tp) {
		return gson.toJson(tp).toString().getBytes();
	}
	
	public void logError(String output) {
		log(output, System.err);
	}

	public void logOutput(String output) {
		log(output, System.out);
	}

	public void log(String output, PrintStream stream) {
		stream.println(output);
	}
}
