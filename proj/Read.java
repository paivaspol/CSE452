import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Placeholder for tweet.
 * 
 * @author leelee
 * 
 */
public class Read extends Function {

	private final int serverAddress;
	private String[] following;
	private int curFollowIndex;
	private StringBuilder newFollowingList;
	private final String usersFile;
	private final String username;
	private StringData strData;
	private long timestamp;

	public Read(Client client, RIONode rioNode, int serverAddress, String usersFile, String username) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		following = null;
		curFollowIndex = -1;
		this.usersFile = usersFile;
		this.username = username;
		newFollowingList = new StringBuilder();
	}

	public void step0() {
		TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
		client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
		client.eventIndex = 1;
	}

	public void step1(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
			client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
			client.eventIndex = 1;
			return;
		}
		timestamp = response.getTimestamp();
		// get login usernames
		TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null, new Entry(rioNode.addr).getHash());
		tpGetLogin.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
		client.eventIndex = 2;
	}

	public void step2(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null, new Entry(rioNode.addr).getHash());
			tpGetLogin.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
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
			logError("You are not login. Cannot read.");
			client.eventIndex = 0;
			client.completeCommand(); 
			return;
		}
		logOutput("Fecthing unread post");
		// request the userlist
		// get the list of username that user is following
		TwitterProtocol tpGetFollowing = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
		tpGetFollowing.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpGetFollowing.toBytes());
		client.eventIndex = 3;
	}

	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetFollowing = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
			tpGetFollowing.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpGetFollowing.toBytes());
			client.eventIndex = 3;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}

		// Get all the followee from the response string
		String followingList = responseString.substring(8);
		following = followingList.split("\n");
		curFollowIndex = 0;
		String[] followeeInfo = following[curFollowIndex].split("\t");
		TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[0] + ".txt", null, new Entry(rioNode.addr).getHash());
		tpGetTweets.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpGetTweets.toBytes());
		client.eventIndex = 4;
	}

	public void step4ReadTillFinish(TwitterProtocol response) {
		String responseString = response.getData();
		String[] followeeInfo = following[curFollowIndex].split("\t");
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[0] + ".txt", null, new Entry(rioNode.addr).getHash());
			tpGetTweets.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpGetTweets.toBytes());
			client.eventIndex = 4;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		Long fromTime = Long.parseLong(followeeInfo[1]);
		// got all the tweets for that particular user
		String[] tweetsToBeDisplayed = responseString.substring(8).split("\n");
		int i = 0;
		for (; i < tweetsToBeDisplayed.length; i++) {
			String[] tSplit = tweetsToBeDisplayed[i].split("\t");
			long tweetFollow = Long.valueOf(tSplit[0]);
			if (tweetFollow >= fromTime) {
				break;
			}
		}
		// display all the unread posts
		for (int j = i; j < tweetsToBeDisplayed.length; j++) {
			logOutput(followeeInfo[0] + ": " + tweetsToBeDisplayed[j]);
		}
		newFollowingList.append(followeeInfo[0] + "\t" + System.currentTimeMillis() + "\n");
		++curFollowIndex;
		if (curFollowIndex == following.length) {
			// update the following list time by delete the the original following file
			TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, usersFile, null, new Entry(rioNode.addr).getHash());
			tpDeleteFollowing.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpDeleteFollowing.toBytes());
			client.eventIndex = 5;
		} else {
			// proceed with reading other people's tweet
			TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[curFollowIndex] + ".txt", null, new Entry(rioNode.addr).getHash());
			tpGetTweets.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpGetTweets.toBytes());
			client.eventIndex = 4;
		}
	}

	public void step5(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, usersFile, null, new Entry(rioNode.addr).getHash());
			tpDeleteFollowing.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpDeleteFollowing.toBytes());
			client.eventIndex = 5;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		// create the new list with the new time
		TwitterProtocol tpCreateFollowing =
				new TwitterProtocol(TwitterServer.CREATE, usersFile, null, new Entry(rioNode.addr).getHash());
		tpCreateFollowing.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpCreateFollowing));
		client.eventIndex = 6;
	}

	public void step6(TwitterProtocol response) {
		String responseString = response.getData();
		// append the new following list with the new timestamps to the file
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpCreateFollowing =
					new TwitterProtocol(TwitterServer.CREATE, usersFile, null, new Entry(rioNode.addr).getHash());
			tpCreateFollowing.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpCreateFollowing.toBytes());
			client.eventIndex = 6;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		strData = new StringData(rioNode.addr);
		strData.setData(newFollowingList.toString());
		TwitterProtocol tpUpdateFollowing =
				new TwitterProtocol(TwitterServer.APPEND, usersFile, strData.toString(), strData.getHash());
		tpUpdateFollowing.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpUpdateFollowing.toBytes());
		client.eventIndex = 7;
	}

	public void step7(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpUpdateFollowing =
					new TwitterProtocol(TwitterServer.APPEND, usersFile, strData.toString(), strData.getHash());
			tpUpdateFollowing.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpUpdateFollowing.toBytes());
			client.eventIndex = 7;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		// commit
		TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
		tpCommit.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
		client.eventIndex = 8;
	}

	public void step8(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// commit
			TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
			tpCommit.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
			client.eventIndex = 8;
			return;
		}
		logOutput("You have no more unread post.");
		client.eventIndex = 0;
		client.completeCommand();
	}

	@Override
	public List<Callback> init() {
		List<Callback> todoList = new ArrayList<Callback>();
		try {
			todoList.add(new Callback(Callback.getMethod("step0", this, null), this, null));
			todoList.add(new Callback(Callback.getMethod("step1", this, new String[] { "TwitterProtocol"}), this, null));
			todoList.add(new Callback(Callback.getMethod("step2", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step3", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step4ReadTillFinish", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step5", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step6", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step7", this, new String[] { "TwitterProtocol" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step8", this, new String[] { "TwitterProtocol" }), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = todoList;
		return todoList;
	}
}
