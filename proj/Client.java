import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * 
 * @author leelee
 * 
 */
public class Client {

	/** The filename where server store the user's information. */
	public static final String USERS_FILE = "users.txt";
	/** TwitterNodeWrapper. */
	private final TwitterNodeWrapper tnw;
	/** Contains the sequence of events to occur. */
	private List<Callback> eventList;
	/** Contains the commands received. */
	private Queue<List<Callback>> commandQueue;
	/** Keeps track of which event to execute next. */
	public int eventIndex;
	/** Map from filename to file content. */
	public Map<String, String> cache;
	
	private boolean isAbortMode;

	/**
	 * Construct a new Client object using the given TwitterNodeWrapper.
	 * 
	 * @param tnw The TwitterNodeWrapper object.
	 */
	public Client(TwitterNodeWrapper tnw) {
		this.tnw = tnw;
		isAbortMode = false;
	}

	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		assert(eventIndex != 0);
		TwitterProtocol tp = TwitterNodeWrapper.GSON.fromJson(new String(msg), TwitterProtocol.class);
		if (tp.getMethod().startsWith("TIMEOUT")) {
			logOutput("Timeout has occured while issuing request to server.");
			eventList = null;
			commandQueue.clear();
			return;
		}
		if (tp.getData() != null && tp.getData().startsWith(TwitterServer.ROLLBACK)) {
			eventIndex = 0;
			cache.clear();
			Callback cb = eventList.get(eventIndex);
			try {
				cb.invoke();
			} catch (Exception e) {
				tnw.fail();
				e.printStackTrace();
			}
		}
		if (tp.getMethod().equals(TwitterServer.INVALIDATE)) {
			if (eventIndex == 0) {
				cache.clear();
				return;
			} else {
				TwitterProtocol tpAbort = new TwitterProtocol(TwitterServer.ABORT, new Entry(tnw.addr).getHash());
				tnw.RIOSend(from, protocol, tpAbort.toBytes());
				isAbortMode = true;
				return;
			}
		}
		if (isAbortMode) {
			return;
		}
		if (tp.getMethod().equals(TwitterServer.ABORTED)) {
			eventIndex = 0;
			Callback cb = eventList.get(eventIndex);
			try {
				cb.invoke();
			} catch (Exception e) {
				tnw.fail();
				e.printStackTrace();
			}
			return;
		}
		if (tp.getMethod().equals(TwitterServer.READ)) {
			if (tp.getData().startsWith(TwitterServer.SUCCESS)) {
				logOutput("\nadd to the cache " + tp.getMethod() + "\n" + tp.getCollection() + "\n" + tp.getData() + "\n");
				cache.put(tp.getCollection(), tp.getData());
			}
		}
		if (eventList != null && eventIndex < eventList.size() && eventList.get(eventIndex) != null) {
			Callback cb = eventList.get(eventIndex);

			cb.setParams(new Object[] { tp });
			try {
				cb.invoke();
			} catch (Exception e) {
				e.printStackTrace();
				tnw.fail();
			}
		} else {
			logOutput("no event to invoke in onRIOReceive");
			if (eventList == null) {
				logOutput("eventList is null");
			} else if (eventIndex > eventList.size()) {
				logOutput("eventIndex size too big");
			} else if (eventList.get(eventIndex) == null) {
				logOutput("the callback is null");
			}
		}
	}

	public void start() {
		logOutput("Starting up client...");
		logOutput("client started");
		eventIndex = 0;
		eventList = null;
		commandQueue = new LinkedList<List<Callback>>();
		cache = new HashMap<String, String>();
		// Inform the server that this client just started/restarted.
		TwitterProtocol tpRestart = new TwitterProtocol(TwitterServer.RESTART, TwitterServer.RESTART, TwitterServer.RESTART, new Entry(tnw.addr).getHash());
		tnw.RIOSend(3, Protocol.DATA, tpRestart.toBytes());
	}

	public void onCommand(String command) throws IllegalAccessException, InvocationTargetException {
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
				return;
			}
			String name = sc.findInLine(pattern);
			if (name == null) {
				logError("signup <username> <name> <password> <serveraddress>");
				return;
			}
			String password = sc.findInLine(pattern);
			if (password == null) {
				logError("signup <username> <name> <password> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("signup <username> <name> <password> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);
			CreateUser createUser = new CreateUser(this, tnw, serverAddress, USERS_FILE, name, username, password);
			commandQueue.add(createUser.init());
			processQueue();

		} else if (token.equalsIgnoreCase("login")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("login <username> <password> <serveraddress>");
				return;
			}
			String password = sc.findInLine(pattern);
			if (password == null) {
				logError("login <username> <password> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("login <username> <password> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);
			Login login = new Login(this, tnw, serverAddress, USERS_FILE, username, password);
			commandQueue.add(login.init());
			processQueue();

		} else if (token.equalsIgnoreCase("logout")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("logout <username> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("logout <username> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);

			Logout logout = new Logout(this, tnw, serverAddress, username);
			commandQueue.add(logout.init());
			processQueue();

		} else if (token.equalsIgnoreCase("tweet")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("tweet <username> <content> <serveraddress>");
				return;
			}
			String content = sc.findInLine(pattern);
			if (content == null) {
				logError("tweet <username> <content> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("tweet <username> <content> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);

			Tweet tweet = new Tweet(this, tnw, serverAddress, username + ".txt", content, username);
			commandQueue.add(tweet.init());
			processQueue();

		} else if (token.equalsIgnoreCase("follow")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("follow <username> <username to follow> <serveraddress>");
				return;
			}
			String userToFollow = sc.findInLine(pattern);
			if (userToFollow == null) {
				logError("follow <username> <username to follow> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			if (server == null) {
				logError("follow <username> <username to follow> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);

			Follow unfollow = new Follow(this, tnw, serverAddress, username + "_following.txt", userToFollow, username);
			commandQueue.add(unfollow.init());
			processQueue();

		} else if (token.equalsIgnoreCase("unfollow")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("unfollow <username> <username to unfollow> <serveraddress>");
				return;
			}
			String userToUnfollow = sc.findInLine(pattern);
			// String userToUnfollow = tokens[1];
			if (userToUnfollow == null) {
				logError("unfollow <username> <username to unfollow> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			// String server = tokens[1];
			if (server == null) {
				logError("unfollow <username> <username to unfollow> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);
			Unfollow unfollow = new Unfollow(this, tnw, serverAddress, username + "_following.txt", userToUnfollow, username);
			commandQueue.add(unfollow.init());
			processQueue();

		} else if (token.equalsIgnoreCase("read")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("read <username> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			// String server = tokens[1];
			if (server == null) {
				logError("read <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);

			// file to read from: [username]_following.txt
			Read read = new Read(this, tnw, serverAddress, username + "_following.txt", username);
			commandQueue.add(read.init());
			processQueue();
		} else {
			logError("Invalid command");
		}
		sc.close();
	}
	
	public void RIOSend(int destAddr, int protocol, byte[] payload) {
		// Check the request to be sent to server. If the request is a read, check if we have cache
		// information for that particular collection.
		// If the request is an deletelines, append, or delete, remove the cache information that
		// we have for that collection, aka invalidates cache.
	    String jsonStr = tnw.packetBytesToString(payload);
	    TwitterProtocol request = TwitterNodeWrapper.GSON.fromJson(jsonStr, TwitterProtocol.class);
	    String method = request.getMethod();
	    String collection = request.getCollection();
	    if (method.equals(TwitterServer.READ)) {
	    	String fileContent = cache.get(collection);
	    	// get the information from the cache and send it to the person that sent the request
	    	if (fileContent != null) {
	    		// p
	    		logOutput("\nget info from cache " + method + "\n" + collection + "\n" + fileContent + "\n");
	    		TwitterProtocol response = new TwitterProtocol(request);
	    		response.setMethod("NO_CACHE");
	    		response.setData(fileContent);
	    		onRIOReceive(destAddr, protocol, response.toBytes());
	    		return;
	    	}
	    } else if (method.equals(TwitterServer.APPEND) || method.equals(TwitterServer.DELETE)
	    		|| method.equals(TwitterServer.DELETE_LINES)) {
	    	logOutput("\ninvalidate cache " + method + "\n");
	    	cache.remove(collection);
	    }
		tnw.RIOSend(destAddr, protocol, payload);
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

	/**
	 * Call the start of the next call back and reset the event index.
	 * 
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public void completeCommand() {
		if (commandQueue.size() == 0) {
			logError("no event to invoke in completeCommand");
		} else if (commandQueue.size() == 1) {
			commandQueue.poll();
			eventList = null;
		} else {
			commandQueue.poll();
			eventList = commandQueue.peek();
			try {
				eventIndex = 1;
				eventList.get(0).invoke();
			} catch (Exception e) {
				e.printStackTrace();
				tnw.fail();
			}
		}
	}

	/**
	 * Invoke first event for the command if it is the first command received.
	 * 
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void processQueue() throws IllegalAccessException, InvocationTargetException {
		if (commandQueue.size() == 0) {
			logError("no event to invoke in processQueue");
		} else if (commandQueue.size() == 1) {
			eventList = commandQueue.peek();
			eventIndex = 1;
			eventList.get(0).invoke();
		}
	}
}
