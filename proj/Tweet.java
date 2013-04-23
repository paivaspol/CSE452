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

  public Tweet(Client client, RIONode rioNode, int serverAddress, String usersFile, String msg) {
    super(client, rioNode);
    this.msg = msg;
    this.serverAddress = serverAddress;
    this.usersFile = usersFile;
  }

  public void step1() {
    // append the tweet to the file on the server
    TwitterProtocol tpAppendTweet =
        new TwitterProtocol(TwitterServer.APPEND, usersFile, System.currentTimeMillis() + "\t" + msg + "\n");
    rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendTweet.toBytes());
  }

  public void step2(String responseData) {
    // check response data
    String responseString = responseData.toString();
    if (!responseString.startsWith(TwitterServer.SUCCESS)) {
      rioNode.fail();
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
    } catch (Exception e) {
      e.printStackTrace();
      rioNode.fail();
    }
    eventList = todoList;
    return todoList;
  }
}
