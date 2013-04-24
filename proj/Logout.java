import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * 
 * @author leelee
 *
 */
public class Logout extends Function {

	private int serverAddress;
	private String username;
	
	public Logout(Client client, RIONode rioNode, int serverAddress, String username) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		this.username = username;
	}

	@Override
	public List<Callback> init() {
		List<Callback> list = new ArrayList<Callback>();
		try {
			list.add(new Callback(Callback.getMethod("step1", this, null), this, null));
			list.add(new Callback(Callback.getMethod("step2", this, new String[] {"java.lang.String"}), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = list;
		return list;
	}
	
	public void step1() {
		logOutput("Logging out");
		// delete this user's username from login.txt
		TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.DELETE_LINES, "login.txt", username);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
		client.eventIndex = 1;
	}
	
	public void step2(String responseString) {
		  if (responseString.startsWith(TwitterServer.RESTART)) {
			// delete this user's username from login.txt
				TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.DELETE_LINES, "login.txt", username);
				rioNode.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
				client.eventIndex = 1;
			  return;
		  }
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
		      logError("Error issuing request to server.");
		      client.eventIndex = 0;
		      client.completeCommand();
		      return;
		}
		logOutput("You are logout! " + username);
		client.eventIndex = 0;
		client.completeCommand();
	}
}
