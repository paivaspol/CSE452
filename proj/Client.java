import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

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
	private Queue<Function> commandQueue;
	/** Keeps track of which event to execute next. */
	public int eventIndex;
	/** Map from filename to file content. */
	public Map<String, String> cache;
	
	private boolean isAbortMode;
	private static final String FILENAME = "clientState.txt";
	private static final String TMPFILE = "clientState.tmp";
	private boolean isFromFile;
	private PersistentStorageReader fileReader;
	private PersistentStorageWriter fileWriter;
	
	/**
	 * Construct a new Client object using the given TwitterNodeWrapper.
	 * 
	 * @param tnw The TwitterNodeWrapper object.
	 */
	public Client(TwitterNodeWrapper tnw) {
		this.tnw = tnw;
		isAbortMode = false;
		isFromFile = false;
	}

	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		assert(eventIndex != 0);
		TwitterProtocol tp = TwitterNodeWrapper.GSON.fromJson(new String(msg), TwitterProtocol.class);
		if (tp.getMethod().equals(TwitterServer.ABORTED)) {
			eventIndex = 0;
			Callback cb = eventList.get(eventIndex);
			try {
				cb.invoke();
			} catch (Exception e) {
				tnw.fail();
				e.printStackTrace();
			}
			isAbortMode = false;
			return;
		}
		if (isAbortMode) {
			return;
		}
		if (tp.getMethod().startsWith("TIMEOUT")) {
			logOutput("Timeout has occured while issuing request to server.");
			eventList = null;
			commandQueue.clear();
			return;
		}
		if (tp.getMethod().equals(TwitterServer.INVALIDATE)) {
			cache.clear();
			if (eventIndex != 0) {
				TwitterProtocol tpAbort = new TwitterProtocol(TwitterServer.ABORT, new Entry(tnw.addr).getHash());
				tnw.RIOSend(from, protocol, tpAbort.toBytes());
				isAbortMode = true;
			}
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
		commandQueue = new LinkedList<Function>();
		cache = new HashMap<String, String>();
		// Inform the server that this client just started/restarted.
		TwitterProtocol tpRestart = new TwitterProtocol(TwitterServer.RESTART, TwitterServer.RESTART, TwitterServer.RESTART, new Entry(tnw.addr).getHash());
		tnw.RIOSend(3, Protocol.DATA, tpRestart.toBytes());
		if (Utility.fileExists(tnw, FILENAME)) {
			try {
				loadState();
				processQueue();
				logOutput("file exist");
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	public void onCommand(String command) throws IllegalAccessException, InvocationTargetException {
		enqueue(command);
		try {
			saveState();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		processQueue();
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
			try {
				saveState();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} else {
			commandQueue.poll();
			eventList = commandQueue.peek().init();
			try {
				saveState();
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
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
//		StackTraceElement[] elem = Thread.currentThread().getStackTrace();
//		for (StackTraceElement e : elem) {
//			logOutput(e.toString());
//		}
		if (commandQueue.size() == 0) {
			logError("no event to invoke in processQueue");
		} else if (commandQueue.size() == 1) {
			eventList = commandQueue.peek().init();
			eventIndex = 1;
			eventList.get(0).invoke();
		}
	}
	
	/**
	 * Create file.
	 * @param collectionName
	 * @return
	 * @throws IOException
	 */
	private boolean createFile(String collectionName) throws IOException {
		if (!Utility.fileExists(tnw, collectionName)) {
			PersistentStorageWriter writer = tnw.getWriter(collectionName, false);
			writer.write(new char[] {});
		}
		return true;
	}
	
	private void saveState() throws IOException {
		// iterate command queue, save to a file
//		1: String oldFile = read foo.txt
//		2: PSWriter temp = getWriter(.temp, false)
//		3: temp.write("foo.txt\n" + oldFile)
//		4: PSWriter newFile = getWriter(foo.txt, false)
//		5: newFile.write(contents)
//		6: delete temp
		fileWriter = tnw.getWriter(FILENAME, true);
		Queue<Function> tmpQueue = new LinkedList<Function>();
		while (!commandQueue.isEmpty()) {
			Function f = commandQueue.poll();
			fileWriter.append(f.toString());
			tmpQueue.add(f);
		}
		commandQueue = tmpQueue;
	}
	
	private void loadState() throws IOException, IllegalAccessException, InvocationTargetException {
		// read line and call enqueue
		fileReader = tnw.getReader(FILENAME);
		String command = fileReader.readLine();
		while (command != null) {
			enqueue(command);
			command = fileReader.readLine();
		}
		eventIndex = 0;
	}
	
	private void enqueue(String command) {
		Pattern pattern = Pattern.compile("[A-Za-z0-9]+|\"[^\"]*\"");
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
			commandQueue.add(createUser);

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
			commandQueue.add(login);

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
			commandQueue.add(logout);

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
			commandQueue.add(tweet);

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
			commandQueue.add(unfollow);

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
			commandQueue.add(unfollow);

		} else if (token.equalsIgnoreCase("read")) {
			String username = sc.findInLine(pattern);
			if (username == null) {
				logError("read <username> <serveraddress>");
				return;
			}
			String server = sc.findInLine(pattern);
			// String server = tokens[1];
			if (server == null) {
				logError("read <username> <serveraddress>");
				return;
			}
			int serverAddress = Integer.valueOf(server);

			// file to read from: [username]_following.txt
			Read read = new Read(this, tnw, serverAddress, username + "_following.txt", username);
			commandQueue.add(read);
			
		} else {
			logError("Invalid command");
		}
		sc.close();
	}
}
