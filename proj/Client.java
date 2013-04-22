import java.io.PrintStream;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	
	public Client(TwitterNodeWrapper tnw) {
		this.tnw = tnw;
	}

	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		// TODO(leelee): figure out how to get from onCommand to here.
		response = msg;
	}

	public void start() {
		logOutput("Starting up client...");
		logOutput("client started");
		gson = new GsonBuilder().create();
		loginUsername = null;
		counterRetry = 0;
	}

	public void onCommand(String command) {
		Pattern pattern = Pattern.compile("\"[^\"]*\"|'[^']*'|[A-Za-z']+");
		Scanner sc = new Scanner(command);
		String token = sc.findInLine(pattern);
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
				logError("signup <username> <name> <password> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			logOutput("Creating a user, name:" + name);
			// create user file that stores tweet
			String responseString = null;

			// RIOSend in sendLater takes care of the retry
			TwitterProtocol tpCreateUserFile = new TwitterProtocol("create", username + ".txt", null);
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpCreateUserFile).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			String tempString = responseData.toString();
			responseString = tempString.split("\n")[0];
			if (!responseString.equalsIgnoreCase("success")) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = null;

			// create user file that stores users that he/she is following
			TwitterProtocol tpCreateFollowingFile = new TwitterProtocol("create", username + "_" + "following.txt", null);
			responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpCreateFollowingFile).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			
			responseString = null;
			counterRetry = 0;
			
			// append this user information to users.txt
			String userInfo = username + "\t" + password + "\t" + name + "\n"; 
			TwitterProtocol tpAppendUserInfo = new TwitterProtocol("append", USERS_FILE, userInfo);
			responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpAppendUserInfo).toString().getBytes());
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			
			logOutput("You are signed up!");
			
		} else if (token.equalsIgnoreCase("login")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("login <username> <password> <serveraddress>");
			}
			String password = sc.findInLine(pattern);
			if (password == null) {
				logError("login <username> <password> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("login <username> <password> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Logging in");
			
			// check if the user exists
			TwitterProtocol tpCheckUser = new TwitterProtocol("read", USERS_FILE, null);
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());
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
			TwitterProtocol tpQuery = new TwitterProtocol("append", "login.txt", username + "\n");
			responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpQuery).toString().getBytes());
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
			if (server == null) {
				logError("logout <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Logging out");
			
			// delete this user's username from login.txt
			TwitterProtocol tpQuery = new TwitterProtocol("delete_lines", "login.txt", loginUsername);
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpQuery).toString().getBytes());
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
			if (content == null) {
				logError("tweet <content> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("tweet <content> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			logOutput("Posting tweet");
			// append the tweet to user's tweet file
			TwitterProtocol tpAppendTweet = new TwitterProtocol("append", loginUsername + ".txt", System.currentTimeMillis() + "\t" + content + "\n");
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpAppendTweet).toString().getBytes());
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
			if (userToFollow == null) {
				logError("follow <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("follow <username> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			// check if user to follow is a valid user name
			TwitterProtocol tpCheckUser = new TwitterProtocol("read", USERS_FILE, null);
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());
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
			TwitterProtocol tpAddToFollowing = new TwitterProtocol("append", loginUsername + ".txt", userToFollowInfo);
			responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpAddToFollowing).toString().getBytes());
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
			if (userToUnfollow == null) {
				logError("unfollow <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("unfollow <username> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			
			// check if user to unfollow is a valid user name
			TwitterProtocol tpCheckUser = new TwitterProtocol("read", USERS_FILE, null);
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());
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
			TwitterProtocol tpUnfollow = new TwitterProtocol("delete_lines", loginUsername + ".txt", userToUnfollow);
			responseData = sendLater(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUnfollow));
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
			if (server == null) {
				logError("read <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Fecthing unread post");
			
			// get the list of username that user is following
			TwitterProtocol tpGetFollowing = new TwitterProtocol("read", loginUsername + "_following.txt", null);
			byte[] responseData = sendLater(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpGetFollowing));
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
				TwitterProtocol tpGetTweets = new TwitterProtocol("read", info[0] + ".txt", null);
				String tweets = sendLater(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpGetTweets)).toString();
				String[] tweetsSplited = tweets.split("\n");
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
			TwitterProtocol tpDeleteFollowing = new TwitterProtocol("delete", loginUsername + "_following.txt", null);
			responseData = sendLater(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpDeleteFollowing));
			if (responseData == null) {
				logError("Error issuing request to server.");
				return;
			}
			responseString = responseData.toString();
			if (!responseString.startsWith("success")) {
				logError("Error issuing request to server.");
				return;
			}
			TwitterProtocol tpUpdateFollowing = new TwitterProtocol("create", loginUsername + "_following.txt", newFollowingList);
			responseData = sendLater(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUpdateFollowing));
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
	
	/**
	 * Note: this only works for one server, one client.
	 * @param destAddr
	 * @param protocol
	 * @param payload
	 * @return the response 
	 */
	public byte[] sendLater(int destAddr, int protocol, byte[] payload) {
		tnw.RIOSend(destAddr, protocol, payload);
		byte[] response = null;
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		long interval = end - start;
		while (this.response == null && interval < 60000) {
		}
		response = this.response;
		if (response == null) {
			return response;
		}
		this.response = null;
		return response;
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

}
