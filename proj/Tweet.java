import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Contains a sequence of event for tweeting.
 * 
 * @author leelee
 * 
 */
public class Tweet extends Function {

	private final String msg;
	private final int serverAddress;
	private final String usersFile;
	private final String username;
	private StringData strData;
	private long timestamp;

	public Tweet(Client client, RIONode rioNode, int serverAddress, String usersFile, String msg, String username) {
		super(client, rioNode);
		this.msg = msg;
		this.serverAddress = serverAddress;
		this.usersFile = usersFile;
		this.username = username;
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
		// get login usernames
		TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null, new Entry(rioNode.addr).getHash());
		tpGetLogin.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
		client.eventIndex = 2;
	}

	public void step2(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null, new Entry(rioNode.addr).getHash());
			tpGetLogin.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
			client.eventIndex = 2;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		boolean isLogin = false;
		for (int i = 1; i < tokens.length; i++) {
			if (tokens[1].equals(username)) {
				isLogin = true;
				break;
			}
		}
		if (!isLogin) {
			logError("You are not login. Cannot tweet.");
			client.eventIndex = 0;
			client.completeCommand(); 
			return;
		}

		logOutput("Posting tweet");
		// append the tweet to the file on the server
		strData = new StringData(rioNode.addr);
		strData.setData(System.currentTimeMillis() + "\t" + msg + "\n");
		TwitterProtocol tpAppendTweet =
				new TwitterProtocol(TwitterServer.APPEND, usersFile, strData.toString(), strData.getHash());
		tpAppendTweet.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendTweet.toBytes());
		client.eventIndex = 3;
	}

	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		// check response data
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpAppendTweet =
					new TwitterProtocol(TwitterServer.APPEND, usersFile, strData.toString(), strData.getHash());
			tpAppendTweet.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendTweet.toBytes());
			client.eventIndex = 3;
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
		client.eventIndex = 4;
	}
	
	public void step4(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// commit
			TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
			tpCommit.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
			client.eventIndex = 4;
			return;
		}
		logOutput("Your tweet is posted. :)");
		client.eventIndex = 0;
		client.completeCommand();
	}

	@Override
	public List<Callback> init() {
		List<Callback> todoList = new ArrayList<Callback>();
		try {
			todoList.add(new Callback(Callback.getMethod("step0", this, null), this, null));
			todoList.add(new Callback(Callback.getMethod("step1", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step2", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step3", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step4", this, new String[] { "TwitterProtocol" }), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = todoList;
		return todoList;
	}
}
