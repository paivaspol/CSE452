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
public class Client extends RIONode {
	
	/** to get the response from on receive */
	private byte[] response = null; 
	private Gson gson;

	@Override
	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		// TODO(leelee): figure out how to get from onCommand to here.
		response = msg;
	}

	@Override
	public void start() {
		logOutput("Starting up...");

		// Generate a user-level synoptic event to indicate that the node started.
		logSynopticEvent("started");
		gson = new GsonBuilder().create();
	}

	@Override
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
			// TODO(leelee): machine id 0 for now
			// create entry user.
			User user = new User(0);
			user.setUserName(username);
			user.setName(name);
			user.setPassword(password);
			user.setIsLogin(false);
			// create TwitterProtocol.
			TwitterProtocol tp = new TwitterProtocol("createEntry", "user", user);
			logOutput("Creating a user, name:" + name);
			// send to the server the tp as bytes array
			sendLater(serverAddress, Protocol.DATA, gson.toJson(tp).toString().getBytes());
			
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
			// query from user to get the user object from server
			// TODO(leelee): machine id 0 for now.
			User user = new User(0);
			user.setUserName(username);
			user.setPassword(password);
			TwitterProtocol tpQuery = new TwitterProtocol("queryEntry", "user", user);
			byte[] clientData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpQuery).toString().getBytes());
			// clientData is empty if the client does not exist.
			if (clientData.length == 0) {
				logError("username:" + " " + username + " password: " + password + " does not exist." );
				return;
			}
			TwitterProtocol result = gson.fromJson(clientData.toString(), TwitterProtocol.class);
			User userCollection = (User) result.getData();
			// set user to login to true
			userCollection.setIsLogin(true);
			// update entry, user login logout
			TwitterProtocol tpUpdate = new TwitterProtocol("updateEntry", "user", userCollection);
			sendLater(serverAddress, Protocol.DATA, gson.toJson(tpUpdate).toString().getBytes());
			logOutput("You are login!");
		} else if (token.equalsIgnoreCase("logout")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("logout <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("logout <usernmae> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Logging out");
			// get from the map this username
			User user = new User(0);
			user.setUserName(username);
			TwitterProtocol tpQuery = new TwitterProtocol("queryEntry", "user", user);
			byte[] clientData = sendLater(serverAddress, Protocol.DATA, gson.toJson(tpQuery).toString().getBytes());
			// clientData is empty if the client does not exist.
			if (clientData.length == 0) {
				logError("username:" + " " + username + " does not exist." );
				return;
			}
			TwitterProtocol result = gson.fromJson(clientData.toString(), TwitterProtocol.class);
			User userCollection = (User) result.getData();
			userCollection.setIsLogin(false);
			// update entry, user login logout
			TwitterProtocol tpUpdate = new TwitterProtocol("updateEntry", "user", userCollection); 
			
			// else logout
			logOutput("You are logout!");
		} else if (token.equalsIgnoreCase("tweet")) {
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
			// create a new entry in tweet collection
		} else if (token.equalsIgnoreCase("follow")) {
			String follower = sc.findInLine(pattern);
			if (follower == null) {
				logError("follow <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("follow <username> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			// check if follower is a valid user name

			// if yes, syso: adding follower
			// create a new entry in follower

			// if no, syso: username not found, continue
		} else if (token.equalsIgnoreCase("unfollow")) {
			String follower = sc.findInLine(pattern);
			if (follower == null) {
				logError("unfollow <username> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("unfollow <username> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			// delete entry from follower collection
			logOutput(follower + " is being removed from your follower list");
		} else if (token.equalsIgnoreCase("quit")) {
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("quit <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Bye");
		} else if (token.equalsIgnoreCase("read")) {
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("read <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Fecthing unread post");
			// get all the unread post
		}
	}
	
	/**
	 * Note: this only works for one server, one client.
	 * @param destAddr
	 * @param protocol
	 * @param payload
	 * @return
	 */
	public byte[] sendLater(int destAddr, int protocol, byte[] payload) {
		RIOSend(destAddr, protocol, payload);
		byte[] response = null;
		while (this.response == null) {
		}
		response = this.response;
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
		stream.println("Node " + addr + ": " + output);
	}

}
