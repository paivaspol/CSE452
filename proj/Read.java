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

	public Read(Client client, RIONode rioNode, int serverAddress, String usersFile, String username) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		following = null;
		curFollowIndex = -1;
		this.usersFile = usersFile;
		this.username = username;
		newFollowingList = new StringBuilder();
	}

	public void step1() {
		// get login usernames
		TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
		client.eventIndex = 1;
	}

	public void step2(String responseString) {
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
			client.eventIndex = 1;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
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
			logError("You are not login.");
			client.eventIndex = 0;
			client.completeCommand(); 
			return;
		}
		// request the userlist
		// get the list of username that user is following
		TwitterProtocol tpGetFollowing = new TwitterProtocol(TwitterServer.READ, usersFile, null);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetFollowing.toBytes());
		client.eventIndex = 2;
	}

	public void step3(String responseString) {
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetFollowing = new TwitterProtocol(TwitterServer.READ, usersFile, null);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetFollowing.toBytes());
			client.eventIndex = 2;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		
		// Get all the followee from the response string
		String followingList = responseString.substring(8);
		following = followingList.split("\n");
		curFollowIndex = 0;
		String[] followeeInfo = following[curFollowIndex].split("\t");
		TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[0] + ".txt", null);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetTweets.toBytes());
		client.eventIndex = 3;
	}

	// 4 
	public void readTillFinish(String responseString) {
		String[] followeeInfo = following[curFollowIndex].split("\t");
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[0] + ".txt", null);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetTweets.toBytes());
			client.eventIndex = 3;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
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
		logOutput("following.length " + following.length);
		logOutput("currFollowIndex " + curFollowIndex);
		if (curFollowIndex == following.length) {
			// update the following list time by delete the the original following file
			TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, usersFile, null);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpDeleteFollowing.toBytes());
			client.eventIndex = 4;
		} else {
			// proceed with reading other people's tweet
			TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[curFollowIndex] + ".txt", null);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetTweets.toBytes());
			client.eventIndex = 3;
		}
	}

	//  public void step4(String responseString) {
	//    String[] followeeInfo = following[curFollowIndex].split("\t");
	//    Long fromTime = Long.parseLong(followeeInfo[1]);
	//    // got all the tweets for that particular user
	//    String[] tweetsToBeDisplayed = responseString.split("\n");
	//    int i = 0;
	//    for (; i < tweetsToBeDisplayed.length; i++) {
	//      String[] tSplit = tweetsToBeDisplayed[i].split("\t");
	//      long tweetFollow = Long.valueOf(tSplit[1]);
	//      if (tweetFollow >= fromTime) {
	//        break;
	//      }
	//    }
	//    // display all the unread posts
	//    for (int j = i; j < tweetsToBeDisplayed.length; j++) {
	//      logOutput(followeeInfo[0] + ": " + tweetsToBeDisplayed[j]);
	//    }
	//    newFollowingList.append(followeeInfo[0] + "\t" + System.currentTimeMillis() + "\n");
	//    ++curFollowIndex;
	//    if (curFollowIndex == following.length) {
	//      // update the following list time by delete the the original following file and create the new one
	//      // with the new time
	//      TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, usersFile, null);
	//      rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpDeleteFollowing));
	//      client.eventIndex = 5;
	//    } else {
	//    	
	//    }
	//  }

	public void step5(String responseString) {
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, usersFile, null);
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpDeleteFollowing.toBytes());
			client.eventIndex = 4;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// create the new list with the new time
		TwitterProtocol tpUpdateFollowing =
				new TwitterProtocol(TwitterServer.CREATE, usersFile, newFollowingList.toString());
		rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUpdateFollowing));
		client.eventIndex = 5;
	}

	public void step6(String responseString) {
		// append the new following list with the new timestamps to the file
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpUpdateFollowing =
					new TwitterProtocol(TwitterServer.CREATE, usersFile, newFollowingList.toString());
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpUpdateFollowing.toBytes());
			client.eventIndex = 5;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		TwitterProtocol tpUpdateFollowing =
				new TwitterProtocol(TwitterServer.APPEND, usersFile, newFollowingList.toString());
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpUpdateFollowing.toBytes());
		client.eventIndex = 6;
	}

	public void step7(String responseString) {
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpUpdateFollowing =
					new TwitterProtocol(TwitterServer.APPEND, usersFile, newFollowingList.toString());
			rioNode.RIOSend(serverAddress, Protocol.DATA, tpUpdateFollowing.toBytes());
			client.eventIndex = 6;
			return;
		}
		String[] tokens = responseString.split("\n");
		if (!tokens[0].startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// check response data, if succeeded notify the user
		logOutput("You have no more unread post.");
		client.eventIndex = 0;
		client.completeCommand();
	}

	@Override
	public List<Callback> init() {
		List<Callback> todoList = new ArrayList<Callback>();
		try {
			todoList.add(new Callback(Callback.getMethod("step1", this, null), this, null));
			todoList.add(new Callback(Callback.getMethod("step2", this, new String[] { "java.lang.String" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step3", this, new String[] { "java.lang.String" }), this, null));
			todoList.add(new Callback(Callback.getMethod("readTillFinish", this, new String[] { "java.lang.String" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step5", this, new String[] { "java.lang.String" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step6", this, new String[] { "java.lang.String" }), this, null));
			todoList.add(new Callback(Callback.getMethod("step7", this, new String[] { "java.lang.String" }), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = todoList;
		return todoList;
	}
}
