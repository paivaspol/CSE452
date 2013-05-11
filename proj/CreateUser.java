import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Contains a sequence of events for signup action.
 * 
 * @author leelee
 *
 */
public class CreateUser extends Function {

	public static final String BEGIN_TRANSACTION = "BEGIN_TRANSACTION";
	public static final String COMMIT = "COMMIT";

	private int serverAddress;
	private String usersFile;
	private String name;
	private String username;
	private User user;
	private long timestamp;

	public CreateUser(Client client, RIONode rioNode, int serverAddress, String usersFile, String name, String username, String password) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		this.usersFile = usersFile;
		this.name = name;
		this.username = username;
		this.user = new User(rioNode.addr);
		this.user.setName(name);
		this.user.setPassword(password);
		this.user.setUserName(username);
	}

	public void step0() {
		TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
		client.eventIndex = 1;
		client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
	}
	
	public void step1(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
			client.eventIndex = 1;
			client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
			return;
		}
		timestamp = response.getTimestamp();
		// check if user already exist
		// do a read
		TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
		tpCheckUser.setTimestamp(timestamp);
		client.eventIndex = 2;
		client.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
	}

	
	public void step2(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
			tpCheckUser.setTimestamp(timestamp);
			client.eventIndex = 2;
			client.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
			return;
		}
		String[] tokens = responseString.split("\n");
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
		tpCreateUserFile.setTimestamp(timestamp);
		client.eventIndex = 3;
		client.RIOSend(serverAddress, Protocol.DATA,tpCreateUserFile.toBytes());
	}
	
	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// create the new user's text file
			TwitterProtocol tpCreateUserFile = new TwitterProtocol(TwitterServer.CREATE, username + ".txt", null, new Entry(rioNode.addr).getHash());
			tpCreateUserFile.setTimestamp(timestamp);
			client.eventIndex = 3;
			client.RIOSend(serverAddress, Protocol.DATA,tpCreateUserFile.toBytes());
		}
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// create user file that stores users that he/she is following
		TwitterProtocol tpCreateFollowingFile = new TwitterProtocol(TwitterServer.CREATE, username + "_" + "following.txt", null, new Entry(rioNode.addr).getHash());
		tpCreateFollowingFile.setTimestamp(timestamp);
		client.eventIndex = 4;
		client.RIOSend(serverAddress, Protocol.DATA, tpCreateFollowingFile.toBytes());
	}
	
	public void step4(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// create user file that stores users that he/she is following
			TwitterProtocol tpCreateFollowingFile = new TwitterProtocol(TwitterServer.CREATE, username + "_" + "following.txt", null, new Entry(rioNode.addr).getHash());
			tpCreateFollowingFile.setTimestamp(timestamp);
			client.eventIndex = 4;
			client.RIOSend(serverAddress, Protocol.DATA, tpCreateFollowingFile.toBytes());
			return;
		}
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// append this user information to users.txt
		TwitterProtocol tpAppendUserInfo = new TwitterProtocol(TwitterServer.APPEND, usersFile, user.toString(), user.getHash());
		tpAppendUserInfo.setTimestamp(timestamp);
		client.eventIndex = 5;
		client.RIOSend(serverAddress, Protocol.DATA, tpAppendUserInfo.toBytes());
	}
	
	public void step5(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// append this user information to users.txt
			TwitterProtocol tpAppendUserInfo = new TwitterProtocol(TwitterServer.APPEND, usersFile, user.toString(), user.getHash());
			tpAppendUserInfo.setTimestamp(timestamp);
			client.eventIndex = 5;
			client.RIOSend(serverAddress, Protocol.DATA, tpAppendUserInfo.toBytes());
			return;
		}
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// commit
		TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
		tpCommit.setTimestamp(timestamp);
		client.eventIndex = 6;
		client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
	}
	
	public void step6(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// commit
			TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
			tpCommit.setTimestamp(timestamp);
			client.eventIndex = 6;
			client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
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
			list.add(new Callback(Callback.getMethod("step0", this, null), this, null));
			list.add(new Callback(Callback.getMethod("step1", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step2", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step3", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step4", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step5", this, new String[] {"TwitterProtocol"}), this, null));
			list.add(new Callback(Callback.getMethod("step6", this, new String[] {"TwitterProtocol"}), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = list;
		return list;
	}
}
