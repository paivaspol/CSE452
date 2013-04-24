import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
  private final byte[] response = null;
  private Gson gson;
  private String loginUsername;
  static final String USERS_FILE = "users.txt";
  private static final int NUM_RETRY = 10;
  private int counterRetry;
  private final TwitterNodeWrapper tnw;
  private List<Callback> eventList;
  private Queue<List<Callback>> commandQueue;
  public int eventIndex;

  public Client(TwitterNodeWrapper tnw) {
    this.tnw = tnw;
  }

  public void onRIOReceive(Integer from, int protocol, byte[] msg) {
    logOutput("at onRIOReceive queue size index " + commandQueue.size() + " " + eventIndex);
    if (protocol == 1) {
      logOutput("Client line 41 ack received!");
    }
    if (eventList != null && eventIndex < eventList.size() && eventList.get(eventIndex) != null) {
      Callback cb = eventList.get(eventIndex);
      TwitterProtocol tp = TwitterNodeWrapper.GSON.fromJson(new String(msg), TwitterProtocol.class);
      cb.setParams(new Object[] { tp.getData() });
      try {
        logOutput("Client eventIndex: " + eventIndex);
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
    gson = new GsonBuilder().create();
    loginUsername = null;
    counterRetry = 0;
    eventIndex = 0;
    eventList = null;
    commandQueue = new LinkedList<List<Callback>>();
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
      logOutput("Logging in");
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
      logOutput("Logging out");

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
      logOutput("Fecthing unread post");
      // file to read from: [username]_following.txt
      Read read = new Read(this, tnw, serverAddress, username + "_following.txt", username);
      commandQueue.add(read.init());
      processQueue();
    } else {
      logError("Invalid command");
    }
    sc.close();
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

  public void processEvent() throws IllegalAccessException, InvocationTargetException, SecurityException,
      ClassNotFoundException, NoSuchMethodException {
    for (Callback cb : eventList) {
      cb.invoke();
    }
    tnw.addTimeout(new Callback(Callback.getMethod("processEvent", this, null), this, null), 1);
  }

  /**
   * Dequeue to call the start of the next call back and reset the index.
   * 
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  public void completeCommand() {
    logOutput("in complete command with queue size " + commandQueue.size());
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
