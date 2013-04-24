import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Placeholder for tweet.
 * 
 * @author leelee
 * 
 */
public class Tweet extends Function {

  private final String msg;
  private final int serverAddress;
  private final String usersFile;
  private final String username;
  
  public Tweet(Client client, RIONode rioNode, int serverAddress, String usersFile, String msg, String username) {
    super(client, rioNode);
    this.msg = msg;
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
	  
	  logOutput("Posting tweet");
	  // append the tweet to the file on the server
	  TwitterProtocol tpAppendTweet =
        new TwitterProtocol(TwitterServer.APPEND, usersFile, System.currentTimeMillis() + "\t" + msg + "\n");
	  try {
		  rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendTweet.toBytes());
	  } catch(RuntimeException e) {
		  e.printStackTrace();
	  }
	  client.eventIndex = 2;
  }

  public void step3(String responseString) {
    // check response data
	  if (responseString.startsWith(TwitterServer.RESTART)) {
		  TwitterProtocol tpAppendTweet =
			        new TwitterProtocol(TwitterServer.APPEND, usersFile, System.currentTimeMillis() + "\t" + msg + "\n");
				  rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendTweet.toBytes());
		  client.eventIndex = 2;
		  return;
	  }
	  if (!responseString.startsWith(TwitterServer.SUCCESS)) {
	      logError("Error issuing request to server.");
	      client.eventIndex = 0;
	      client.completeCommand();
	      return;
	  }
	  logOutput("Your tweet is posted. :)");
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
