import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Placeholder for tweet.
 * 
 * @author leelee
 * 
 */
public class Follow extends Function {

  private final String followee;
  private final int serverAddress;
  private final String usersFile;

  public Follow(Client client, RIONode rioNode, int serverAddress, String usersFile, String followee) {
    super(client, rioNode);
    this.followee = followee;
    this.serverAddress = serverAddress;
    this.usersFile = usersFile;
  }

  public void step1() {
    // request the userlist
    TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, Client.USERS_FILE, null);
    rioNode.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpCheckUser).toString().getBytes());

  }

  public void step2(String responseString) {
    // check if the user is a valid user or not
    if (!responseString.startsWith("success")) {
      logError("Error issuing request to server.");
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
      return;
    }
    // append the tweet to the file on the server
    TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.APPEND, usersFile, followee);
    rioNode.RIOSend(serverAddress, Protocol.DATA, gson.toJson(tpAddToFollowing).toString().getBytes());
  }

  public void step3(String responseData) {
    // check response data, if succeeded notify the user
    String responseString = responseData.toString();
    if (!responseString.startsWith(TwitterServer.SUCCESS)) {
      rioNode.fail();
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
      todoList.add(new Callback(Callback.getMethod("step1", this, null), this, null));
      todoList.add(new Callback(Callback.getMethod("step2", this, new String[] { "java.lang.String" }), this, null));
      todoList.add(new Callback(Callback.getMethod("step3", this, new String[] { "java.lang.String" }), this, null));
    } catch (Exception e) {
      e.printStackTrace();
      rioNode.fail();
    }
    eventList = todoList;
    return todoList;
  }
}
