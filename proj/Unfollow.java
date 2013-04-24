import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Placeholder for tweet.
 * 
 * @author leelee
 * 
 */
public class Unfollow extends Function {

  private final String followee;
  private final int serverAddress;
  private final String usersFile;
  private final String username;

  public Unfollow(Client client, RIONode rioNode, int serverAddress, String usersFile, String followee, String username) {
    super(client, rioNode);
    this.followee = followee;
    this.serverAddress = serverAddress;
    this.usersFile = usersFile;
    this.username = username;
  }

  public void step1() {
	  // get login usernames
	  TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null, new Entry(rioNode.addr).getHash());
	  rioNode.RIOSend(serverAddress, Protocol.DATA, tpGetLogin.toBytes());
	  client.eventIndex = 1;
  }
  
  public void step2(String responseString) {
	  if (responseString.startsWith(TwitterServer.RESTART)) {
		  TwitterProtocol tpGetLogin = new TwitterProtocol(TwitterServer.READ, "login.txt", null, new Entry(rioNode.addr).getHash());
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

	  logOutput("unfollowing " + followee);
    // delete the username to follow in the following file
    TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.DELETE_LINES, usersFile, followee, new Entry(rioNode.addr).getHash());
    rioNode.RIOSend(serverAddress, Protocol.DATA, tpAddToFollowing.toBytes());
    client.eventIndex = 2;
  }


  public void step3(String responseString) {
    // check response data, if succeeded notify the user
	  if (responseString.startsWith(TwitterServer.RESTART)) {
		    TwitterProtocol tpAddToFollowing = new TwitterProtocol(TwitterServer.DELETE_LINES, usersFile, followee, new Entry(rioNode.addr).getHash());
		    rioNode.RIOSend(serverAddress, Protocol.DATA, tpAddToFollowing.toBytes());
		    client.eventIndex = 3;
		  return;
	  }
    if (!responseString.startsWith(TwitterServer.SUCCESS)) {
        logError("Error issuing request to server.");
        client.eventIndex = 0;
        client.completeCommand();
        return;
    }
    logOutput("You are no longer following " + followee);
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
