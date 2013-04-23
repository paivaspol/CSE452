import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Placeholder for tweet.
 * 
 * @author leelee
 * 
 */
public class Read extends Function {

  private final int serverAddress;
  private String[] following;
  private int curFollowIndex;
  private StringBuilder newFollowingList;
  private final String usersFile;

  public Read(Client client, RIONode rioNode, int serverAddress, String usersFile) {
    super(client, rioNode);
    this.serverAddress = serverAddress;
    following = null;
    curFollowIndex = -1;
    this.usersFile = usersFile;
  }

  public void step1() {
    // request the userlist
    // get the list of username that user is following
    TwitterProtocol tpGetFollowing = new TwitterProtocol(TwitterServer.READ, usersFile, null);
    rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpGetFollowing));
  }

  public void step2(String responseString) {
    // Get all the follwees, then send a READ request on each of them
    if (following == null && curFollowIndex < 0) {
      String followingList = responseString.substring(8);
      following = followingList.split("\n");
      curFollowIndex = 0;
    }
    String[] followeeInfo = following[curFollowIndex].split("\t");
    TwitterProtocol tpGetTweets = new TwitterProtocol(TwitterServer.READ, followeeInfo[0] + ".txt", null);
    rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpGetTweets));
  }

  public void step3(String responseString) {
    String[] followeeInfo = following[curFollowIndex].split("\t");
    Long fromTime = Long.parseLong(followeeInfo[1]);
    // got all the tweets for that particular user
    String[] tweetsToBeDisplayed = responseString.split("\n");
    int i = 0;
    for (; i < tweetsToBeDisplayed.length; i++) {
      String[] tSplit = tweetsToBeDisplayed[i].split("\t");
      long tweetFollow = Long.valueOf(tSplit[1]);
      if (tweetFollow >= fromTime) {
        break;
      }
    }
    // display all the unread posts
    for (int j = i; j < tweetsToBeDisplayed.length; j++) {
      logOutput(followeeInfo[0] + ": " + tweetsToBeDisplayed[j]);
    }
    newFollowingList.append(followeeInfo[0] + "\t" + System.currentTimeMillis() + "\n");
    ++curFollowIndex;
    if (curFollowIndex == following.length) {
      // update the following list time by delete the the original following file and create the new one
      // with the new time
      TwitterProtocol tpDeleteFollowing = new TwitterProtocol(TwitterServer.DELETE, usersFile, null);
      rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpDeleteFollowing));
    }
  }

  public void step4(String responseString) {
    // create the new following list
    if (!responseString.startsWith("success")) {
      rioNode.fail();
      return;
    }
    TwitterProtocol tpUpdateFollowing =
        new TwitterProtocol(TwitterServer.CREATE, usersFile, newFollowingList.toString());
    rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUpdateFollowing));
  }

  public void step5(String responseString) {
    // append the new following list with the new timestamps to the file
    if (!responseString.startsWith("success")) {
      rioNode.fail();
      return;
    }
    TwitterProtocol tpUpdateFollowing =
        new TwitterProtocol(TwitterServer.APPEND, usersFile, newFollowingList.toString());
    rioNode.RIOSend(serverAddress, Protocol.DATA, getBytesFromTwitterProtocol(tpUpdateFollowing));
  }

  public void step6(String responseData) {
    // check response data, if succeeded notify the user
    String responseString = responseData.toString();
    if (!responseString.startsWith(TwitterServer.SUCCESS)) {
      rioNode.fail();
      return;
    }
    logOutput("You have no more unread post.");
    client.eventIndex = 0;
    client.completeCommand();
  }

  @Override
  public List<Callback> init() {
    List<Callback> todoList = new ArrayList<Callback>();
    try {
      todoList.add(new Callback(Callback.getMethod("step1", this, null), this, null));
      for (String element : following) {
        todoList.add(new Callback(Callback.getMethod("step2", this, new String[] { "java.lang.String" }), this, null));
        todoList.add(new Callback(Callback.getMethod("step3", this, new String[] { "java.lang.String" }), this, null));
      }
      todoList.add(new Callback(Callback.getMethod("step4", this, new String[] { "java.lang.String" }), this, null));
      todoList.add(new Callback(Callback.getMethod("step5", this, new String[] { "java.lang.String" }), this, null));
      todoList.add(new Callback(Callback.getMethod("step6", this, new String[] { "java.lang.String" }), this, null));
    } catch (Exception e) {
      e.printStackTrace();
      rioNode.fail();
    }
    eventList = todoList;
    return todoList;
  }
}
