import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Contains a sequence of events for following action.
 * 
 * @author leelee
 * 
 */
public class Follow extends Function {

	private final String followee;
	private final int serverAddress;
	private final String usersFile;
	private final String username;
	private StringData strData;
	private long timestamp;

	public Follow(Client client, RIONode rioNode, int serverAddress, String usersFile, String followee, String username) {
		super(client, rioNode);
		this.followee = followee;
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
			logError("You are not login. Cannot follow.");
			client.eventIndex = 0;
			client.completeCommand(); 
			return;
		}
		// request the userlist
		TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, Client.USERS_FILE, null, new Entry(rioNode.addr).getHash());
		tpCheckUser.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
		client.eventIndex = 3;
	}

	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, Client.USERS_FILE, null, new Entry(rioNode.addr).getHash());
			tpCheckUser.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
			client.eventIndex = 3;
			return;
		}
		// check if the user is a valid user or not
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		String usersList = responseString.substring(8);
		String[] names = usersList.split("\n");
		boolean isValidUser = false;
		for (String name : names) {
			if (name.startsWith(followee)) {
				isValidUser = true;
				break;
			}
		}
		if (!isValidUser) {
			logError("username:" + " " + followee + " does not exist.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		// append the tweet to the file on the server
		String followeeDetail = followee + "\t" + System.currentTimeMillis();
		strData = new StringData(rioNode.addr);
		strData.setData(followeeDetail);
		TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.APPEND, usersFile, strData.toString(), strData.getHash());
		tpAddToFollowing.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpAddToFollowing.toBytes());
		client.eventIndex = 4;
	}

	public void step4(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.APPEND, usersFile, strData.toString(), strData.getHash());
			tpAddToFollowing.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpAddToFollowing.toBytes());
			client.eventIndex = 4;
			return;
		}
		// check response data, if succeeded notify the user
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
		tpCommit.setTimestamp(timestamp);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
		client.eventIndex = 5;
	}
	
	public void step5(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
			tpCommit.setTimestamp(timestamp);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
			client.eventIndex = 5;
			return;
		}
		logOutput("Congratulation! You are now following " + followee);
		client.eventIndex = 0;
		client.completeCommand();
	}

	@Override
	public List<Callback> init() {
		List<Callback> todoList = new ArrayList<Callback>();
		try {
			todoList.add(new Callback(Callback.getMethod("step0", this, null), this, null));
			todoList.add(new Callback(Callback.getMethod("step1", this, new String[] {"TwitterProtocol"}), this, null));
			todoList.add(new Callback(Callback.getMethod("step2", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step3", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step4", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step5", this, new String[] { "TwitterProtocol" }), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = todoList;
		return todoList;
	}
}
