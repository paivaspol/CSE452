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
  private final String username;

  public Follow(Client client, RIONode rioNode, int serverAddress, String usersFile, String followee, String username) {
    super(client, rioNode);
    this.followee = followee;
    this.serverAddress = serverAddress;
    this.usersFile = usersFile;
    this.username = username;
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
		  logError("You are not login.");
		  client.eventIndex = 0;
		  client.completeCommand(); 
		  return;
	  }
    // request the userlist
    TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, Client.USERS_FILE, null);
    rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
    client.eventIndex = 2;
  }

  public void step3(String responseString) {
	  if (responseString.startsWith(TwitterServer.RESTART)) {
		    TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, Client.USERS_FILE, null);
		    rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
		    client.eventIndex = 2;
		  return;
	  }
    // check if the user is a valid user or not
    if (!responseString.startsWith(TwitterServer.SUCCESS)) {
      logError("Error issuing request to server.");
      client.eventIndex = 0;
      client.completeCommand();
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
      client.eventIndex = 0;
      client.completeCommand();
      return;
    }
    // append the tweet to the file on the server
    String followeeDetail = followee + "\t" + System.currentTimeMillis();
    TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.APPEND, usersFile, followeeDetail);
    rioNode.RIOSend(serverAddress, Protocol.DATA, tpAddToFollowing.toBytes());
    client.eventIndex = 3;
  }

  public void step4(String responseString) {
	  if (responseString.startsWith(TwitterServer.RESTART)) {
		    TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.APPEND, usersFile, followee);
		    rioNode.RIOSend(serverAddress, Protocol.DATA, tpAddToFollowing.toBytes());
		    client.eventIndex = 3;
		  return;
	  }
    // check response data, if succeeded notify the user
    if (!responseString.startsWith(TwitterServer.SUCCESS)) {
        logError("Error issuing request to server.");
        client.eventIndex = 0;
        client.completeCommand();
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
      todoList.add(new Callback(Callback.getMethod("step4", this, new String[] { "java.lang.String" }), this, null));
    } catch (Exception e) {
      e.printStackTrace();
      rioNode.fail();
    }
    eventList = todoList;
    return todoList;
  }
}
