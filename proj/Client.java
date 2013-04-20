import java.io.PrintStream;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;


public class Client extends RIONode {
	
	public static final String methodKey = "method";
	public static final String collectionKey = "collection";
	public static final String idKey = "id";
	public static final String dataKey = "data";
	public static final String usernameKey = "username";
	public static final String passwordKey = "password";
	public static final String nameKey = "name";
	
	private static int sequenceNumberGenerator = 0;

	@Override
	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		logOutput("Starting up...");

		// Generate a user-level synoptic event to indicate that the node started.
		logSynopticEvent("started");
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
				logError("create <username> <name> <password> <serveraddress>");
			}
			String password = sc.findInLine(pattern);
			if (password == null) {
				logError("create <username> <name> <password> <serveraddress>");
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("create <username> <name> <password> <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			// format a new JsonObject and pass it to the server.
			JsonObject obj = new JsonObject();
			obj.addProperty(methodKey, "createEntry");
			obj.addProperty(collectionKey, "user");
			obj.addProperty("sequenceNumber", sequenceNumberGenerator+=1);
			JsonObject data = new JsonObject();
			data.addProperty("username", username);
			data.addProperty("name", name);
			data.addProperty("password", password);
			obj.add(dataKey, data);
			logOutput("Creating a user, name:" + name);
			// create entry is user.
			RIOSend(serverAddress, Protocol.DATA, obj.toString().getBytes());
			
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
			// get from the map this username

			// if null, syso user not found and continue

			// else update entry with this particular hash to log in
		} else if (token.equalsIgnoreCase("logout")) {
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("logout <serveraddress>");
			}
			int serverAddress = Integer.valueOf(server);
			logOutput("Logging out");
			// get from the map this username

			// if null, syso user is not login

			// else logout
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
