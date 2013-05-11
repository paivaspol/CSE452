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
	private String beginTransactionHash;

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
		beginTransactionHash = new Entry(rioNode.addr).getHash();
		TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, beginTransactionHash);
		client.eventIndex = 1;
		client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
	}

	public void step1(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, beginTransactionHash);
			client.eventIndex = 1;
			client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
			return;
		}
		timestamp = response.getTimestamp();
		logOutput("Logging out");
		// delete this user's username from login.txt
		TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.DELETE_LINES, "login.txt", username, new Entry(rioNode.addr).getHash());
		tpQuery.setTimestamp(timestamp);
		client.eventIndex = 2;
		client.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
	}

	public void step2(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, beginTransactionHash);
			client.eventIndex = 1;
			client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
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
		client.eventIndex = 3;
		client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
	}

	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, beginTransactionHash);
			client.eventIndex = 1;
			client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
			return;
		}
		logOutput("You are logout! " + username);
		client.eventIndex = 0;
		client.completeCommand();
	}
}
