import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Contains a sequence of event for logging out.
 * 
 * @author leelee
 *
 */
public class Logout extends Function {

	private int serverAddress;
	private String username;
	private long timestamp;

	public Logout(Client client, RIONode rioNode, int serverAddress, String username) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		this.username = username;
	}

	@Override
	public List<Callback> init() {
		List<Callback> list = new ArrayList<Callback>();
		try {
			list.add(new Callback(Callback.getMethod("step0", this, null), this, null));
			list.add(new Callback(Callback.getMethod("step1", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step2", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step3", this, new String[] {"TwitterProtocol"}), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = list;
		return list;
	}

	public void step0() {
		TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
		client.eventIndex = 1;
	}

	public void step1(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
			client.eventIndex = 1;
			return;
		}
		timestamp = response.getTimestamp();
		logOutput("Logging out");
		// delete this user's username from login.txt
		TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.DELETE_LINES, "login.txt", username, new Entry(rioNode.addr).getHash());
		tpQuery.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
		client.eventIndex = 2;
	}

	public void step2(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// delete this user's username from login.txt
			TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.DELETE_LINES, "login.txt", username, new Entry(rioNode.addr).getHash());
			tpQuery.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
			client.eventIndex = 2;
			return;
		}
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		// commit
		TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
		tpCommit.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
		client.eventIndex = 3;
	}

	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// commit
			TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
			tpCommit.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
			client.eventIndex = 3;
			return;
		}
		logOutput("You are logout! " + username);
		client.eventIndex = 0;
		client.completeCommand();
	}
}
