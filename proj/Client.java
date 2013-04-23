import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * 
 * @author leelee
 *
 */
public class Client {
	
	/** to get the response from on receive */
	private byte[] response = null; 
	private Gson gson;
	private String loginUsername;
	private static final String USERS_FILE = "users.txt";
	private static final int NUM_RETRY = 10;
	private int counterRetry;
	private TwitterNodeWrapper tnw;
	private List<Callback> eventList;
	private int eventIndex; 
	private byte[] responseData = null;
	
	
	public Client(TwitterNodeWrapper tnw) {
		this.tnw = tnw;
	}

	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		if (eventList != null && eventList.get(eventIndex) != null) {
			Callback cb = eventList.get(eventIndex);
			TwitterProtocol tp = TwitterNodeWrapper.GSON.fromJson(new String(msg), TwitterProtocol.class);
			cb.setParams(new Object[] {tp.getData()});
			try {
				cb.invoke();
			} catch (Exception e) {
				e.printStackTrace();
				tnw.fail();
			}
			eventIndex++;
		} else {
			logOutput("no event to invoke.");
		}
	}

	public void start() {
		logOutput("Starting up client...");
		logOutput("client started");
		gson = new GsonBuilder().create();
		loginUsername = null;
		counterRetry = 0;
		eventIndex = 0;
		eventList = null;
	}

	public void onCommand(String command) throws IllegalAccessException, InvocationTargetException {
		// p
		logOutput("client: " + command);
		Pattern pattern = Pattern.compile("[A-Za-z0-9]+|\"[^\"]*\"");
		Scanner sc = new Scanner(command);
		String token = sc.findInLine(pattern);
		logOutput("token: " + token);
		if (token == null) {
			throw new RuntimeException();
		}

		// for functions
		if (token.equalsIgnoreCase("signup")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("signup <username> <name> <password> <serveraddress>");
			}
			String name = sc.findInLine(pattern);
			if (name == null) {
				logError("signup <username> <name> <password> <serveraddress>");
			}
			String password = sc.findInLine(pattern);
			if (password == null) {
				logError("signup <username> <name> <password> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				// p " "
				logOutput("server is not a string");
				logError("signup <username> <name> <password> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			CreateUser createUser = new CreateUser(tnw, serverAddress, USERS_FILE, name, username, password);
			eventList = createUser.init();
			eventIndex = 1;
			createUser.start();
			
		} else if (token.equalsIgnoreCase("login")) {
			String username = sc.findInLine(pattern);
//			String username = tokens[1];
			if (username == null) {
				logError("login <username> <password> <serveraddress>");
			}
			String password = sc.findInLine(pattern);
//			String password = tokens[2];
			if (password == null) {
				logError("login <username> <password> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
//			String server = tokens[3];
			if (server == null) {
				logError("login <username> <password> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Logging in");
			
			// check if the user exists
			TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, USERS_FILE, null);
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			String usersList = responseString.substring(8);
			String[] names = usersList.split("\n");
			boolean isValidUser = false;
			for (int i = 0; i < names.length; i++) {
				if (names[i].startsWith(username)) {
					String[] info = names[i].split("\t");
					if (info[1].equals(password)) {
						isValidUser = true;
						break;
					}
				}
			}
			if (!isValidUser) {
				logError("username:" + " " + username + " password: " + password + " does not exist." );
				return;
			}
			
			// correct username and password so proceed with login user
			TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.APPEND, "login.txt", username + "\n");
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpQuery).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			
			loginUsername = username;
			logOutput("You are login!");
		} else if (token.equalsIgnoreCase("logout")) {
			if (loginUsername == null) {
				logError("you are not login");
				return;
			}
			String server = sc.findInLine(pattern);
//			String server = tokens[1];
			if (server == null) {
				logError("logout <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Logging out");
			
			// delete this user's username from login.txt
			TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.DELETE_LINES, "login.txt", loginUsername);
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpQuery).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			loginUsername = null;
			
			logOutput("You are logout!");
		} else if (token.equalsIgnoreCase("tweet")) {
			if (loginUsername == null) {
				logError("You are not login.");
				return;
			}
			String content = sc.findInLine(pattern);
//			String content = tokens[1];
			if (content == null) {
				logError("tweet <content> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
//			String server = tokens[2];
			if (server == null) {
				logError("tweet <content> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			logOutput("Posting tweet");
			// append the tweet to user's tweet file
			TwitterProtocol tpAppendTweet = new TwitterProtocol(TwitterServer.APPEND, loginUsername + ".txt", System.currentTimeMillis() + "\t" + content + "\n");
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpAppendTweet).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			logOutput("Your tweet is posted. :)");
			
		} else if (token.equalsIgnoreCase("follow")) {
			if (loginUsername == null) {
				logError("You are not login.");
				return;
			}
			String userToFollow = sc.findInLine(pattern);
//			String userToFollow = tokens[1];
			if (userToFollow == null) {
				logError("follow <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
//			String server = tokens[2];
			if (server == null) {
				logError("follow <username> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			// check if user to follow is a valid user name
			TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, USERS_FILE, null);
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			String usersList = responseString.substring(8);
			String[] names = usersList.split("\n");
			boolean isValidUser = false;
			for (int i = 0; i < names.length; i++) {
				if (names[i].startsWith(userToFollow)) {
					isValidUser = true;
					break;
				}
			}
			if (!isValidUser) {
				logError("username:" + " " + userToFollow  + " does not exist." );
				return;
			}
			
			// adding follower
			logOutput("adding " + userToFollow);
			String userToFollowInfo = userToFollow + "\t" + System.currentTimeMillis() + "\n";
			TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.APPEND, loginUsername + ".txt", userToFollowInfo);
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpAddToFollowing).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			logOutput("Congratulation! You are now following " + userToFollow);
			
		} else if (token.equalsIgnoreCase("unfollow")) {
			if (loginUsername == null) {
				logError("You are not login.");
				return;
			}
			String userToUnfollow = sc.findInLine(pattern);
//			String userToUnfollow = tokens[1];
			if (userToUnfollow == null) {
				logError("unfollow <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
//			String server = tokens[1];
			if (server == null) {
				logError("unfollow <username> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			// check if user to unfollow is a valid user name
			TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, USERS_FILE, null);
			tnw.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			String usersList = responseString.substring(8);
			String[] names = usersList.split("\n");
			boolean isValidUser = false;
			for (int i = 0; i < names.length; i++) {
				if (names[i].startsWith(userToUnfollow)) {
					isValidUser = true;
					break;
				}
			}
			if (!isValidUser) {
				logError("username:" + " " + userToUnfollow  + " does not exist." );
				return;
			}
			
			// delete entry from follower collection
			TwitterProtocol tpUnfollow = new TwitterProtocol(TwitterServer.DELETE_LINES, loginUsername + ".txt", userToUnfollow);
			tnw.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUnfollow));
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			logOutput("You are no longer following " + userToUnfollow);
		} else if (token.equalsIgnoreCase("read")) {
			if (loginUsername == null) {
				logError("You are not login.");
				return;
			}
			String server = sc.findInLine(pattern);
//			String server = tokens[1];
			if (server == null) {
				logError("read <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Fecthing unread post");
			
			// get the list of username that user is following
			TwitterProtocol tpGetFollowing = new TwitterProtocol(TwitterServer.READ, loginUsername + "_following.txt", null);
			tnw.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpGetFollowing));
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			String followingList = responseString.substring(8);
			String[] eachFollowing = followingList.split("\n");
			String newFollowingList = "";
			// get all the tweets from the user that this person is following
			for (String f : eachFollowing) {
				String[] info = f.split("\t");
				long timeFollow = Long.valueOf(info[1]);
				TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, info[0] + ".txt", null);
				tnw.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpGetTweets));
//				String[] tweetsSplited = tweets.split("\n");
				String[] tweetsSplited = new String[0];
				int i = 0;
				for (; i < tweetsSplited.length; i++) {
					String[] tSplit = tweetsSplited[i].split("\t");
					long tweetFollow = Long.valueOf(tSplit[1]);
					if (tweetFollow >= timeFollow) {
						break;
					}
				}
				// display all the unread posts
				for (int j = i; j < tweetsSplited.length; j++) {
					logOutput(info[0] + ": " + tweetsSplited[j]);
				}
				newFollowingList += info[0] + "\t" + System.currentTimeMillis() + "\n";
			}
			
			// update the following list time by delete the the original following file and create the new one
			// with the new time
			TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, loginUsername + "_following.txt", null);
			tnw.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpDeleteFollowing));
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			TwitterProtocol tpUpdateFollowing = new TwitterProtocol(TwitterServer.APPEND, loginUsername + "_following.txt", newFollowingList);
			tnw.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUpdateFollowing));
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			logOutput("You have no more unread post.");
		} else {
			logError("Invalid command");
		}
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
	
	private byte[] getBytesFromTwitterProtocol(TwitterProtocol tp) {
		return gson.toJson(tp).toString().getBytes();
	}
	
	public void processEvent() throws IllegalAccessException, InvocationTargetException, SecurityException, ClassNotFoundException, NoSuchMethodException {
		for (Callback cb : eventList) {
			cb.invoke();
		}
		tnw.addTimeout(new Callback(Callback.getMethod("processEvent", this, null), this, null), 1);
	}

}
