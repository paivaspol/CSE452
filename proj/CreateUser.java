import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * 
 * @author leelee
 *
 */
public class CreateUser extends Function {
	
	private int serverAddress;
	private String usersFile;
	private String name;
	private String username;
	private String password;
	private User user;
	
	public CreateUser(Client client, RIONode rioNode, int serverAddress, String usersFile, String name, String username, String password) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		this.usersFile = usersFile;
		this.name = name;
		this.username = username;
		this.password = password;
		this.user = new User(rioNode.addr);
		this.user.setName(name);
		this.user.setPassword(password);
		this.user.setUserName(username);
	}
	
	public void step1() {
		// check if user already exist
		// do a read
		TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
		client.eventIndex = 1;
	}
	
	public void step2(String responseString) {
		  if (responseString.startsWith(TwitterServer.RESTART)) {
				TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
				rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
				client.eventIndex = 1;
			  return;
		  }
		String[] tokens = responseString.split("\n");
		logOutput("CreateUser tokens[0] " + tokens[0]);
		if (!tokens[0].equals(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		boolean alreadyExist = false;
		for (int i = 1; i < tokens.length; i++) {
			if (tokens[i].equals(username)) {
				alreadyExist = true;
				break;
			}
		}
		if (alreadyExist) {
			logOutput("User already created.");
			client.completeCommand();
			return;
		}
		logOutput("Creating a user, name:" + name);
		// create the new user's text file
		TwitterProtocol tpCreateUserFile = new TwitterProtocol(TwitterServer.CREATE, username + ".txt", null, new Entry(rioNode.addr).getHash());
		rioNode.RIOSend(serverAddress, Protocol.DATA,tpCreateUserFile.toBytes());
		client.eventIndex = 2;
	}
	
	public void step3(String responseString) {
		logOutput("CreateUser step3");
		  if (responseString.startsWith(TwitterServer.RESTART)) {
				TwitterProtocol tpCreateUserFile = new TwitterProtocol(TwitterServer.CREATE, username + ".txt", null, new Entry(rioNode.addr).getHash());
				rioNode.RIOSend(serverAddress, Protocol.DATA,tpCreateUserFile.toBytes());
				client.eventIndex = 2;
			  return;
		  }
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// create user file that stores users that he/she is following
		TwitterProtocol tpCreateFollowingFile = new TwitterProtocol(TwitterServer.CREATE, username + "_" + "following.txt", null, new Entry(rioNode.addr).getHash());
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCreateFollowingFile.toBytes());
		client.eventIndex = 3;
	}
	
	public void step4(String responseString) {
		logOutput("CreateUser step4");
		  if (responseString.startsWith(TwitterServer.RESTART)) {
			// create user file that stores users that he/she is following
				TwitterProtocol tpCreateFollowingFile = new TwitterProtocol(TwitterServer.CREATE, username + "_" + "following.txt", null, new Entry(rioNode.addr).getHash());
				rioNode.RIOSend(serverAddress, Protocol.DATA, tpCreateFollowingFile.toBytes());
				client.eventIndex = 3;
			  return;
		  }
		// append this user information to users.txt
		TwitterProtocol tpAppendUserInfo = new TwitterProtocol(TwitterServer.APPEND, usersFile, user.toString(), user.getHash());
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendUserInfo.toBytes());
		client.eventIndex = 4;
	}
	
	public void step5(String responseString) {
		logOutput("CreateUser step5");
		  if (responseString.startsWith(TwitterServer.RESTART)) {
				// append this user information to users.txt
				TwitterProtocol tpAppendUserInfo = new TwitterProtocol(TwitterServer.APPEND, usersFile, user.toString(), user.getHash());
				rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendUserInfo.toBytes());
				client.eventIndex = 4;
			  return;
		  }
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		logOutput("You are signed up!");
		client.eventIndex = 0;
		client.completeCommand();
	}

	@Override
	public List<Callback> init() {
		List<Callback> list = new ArrayList<Callback>();
		try {
			list.add(new Callback(Callback.getMethod("step1", this, null), this, null));
			list.add(new Callback(Callback.getMethod("step2", this, new String[] {"java.lang.String"}), this, null));
			list.add(new Callback(Callback.getMethod("step3", this, new String[] {"java.lang.String"}), this, null));
			list.add(new Callback(Callback.getMethod("step4", this, new String[] {"java.lang.String"}), this, null));
			list.add(new Callback(Callback.getMethod("step5", this, new String[] {"java.lang.String"}), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = list;
		return list;
	}
}
