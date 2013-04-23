import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;


public class CreateUser extends Function {
	
	private int serverAddress;
	private String usersFile;
	private String name;
	private String username;
	private String password;
	
	
	public CreateUser(RIONode rioNode, int serverAddress, String usersFile, String name, String username, String password) {
		super(rioNode);
		this.serverAddress = serverAddress;
		this.usersFile = usersFile;
		this.name = name;
		this.username = username;
		this.password = password;
	}
	
	public void step1() {
		// check if user already exist
		// do a read
		TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
	}
	
	public void step2(String responseString) {
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
			return;
		}
		logOutput("Creating a user, name:" + name);
		// create the new user's text file
		TwitterProtocol tpCreateUserFile = new TwitterProtocol(TwitterServer.CREATE, username + ".txt", null);
		rioNode.RIOSend(serverAddress, Protocol.DATA,tpCreateUserFile.toBytes());
	}
	
	public void step3(String responseString) {
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		// create user file that stores users that he/she is following
		TwitterProtocol tpCreateFollowingFile = new TwitterProtocol(TwitterServer.CREATE, username + "_" + "following.txt", null);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCreateFollowingFile.toBytes());
	}
	
	public void step4(String responseString) {
		// append this user information to users.txt
		String userInfo = username + "\t" + password + "\t" + name + "\n"; 
		TwitterProtocol tpAppendUserInfo = new TwitterProtocol(TwitterServer.APPEND, usersFile, userInfo);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpAppendUserInfo.toBytes());
	}
	
	public void step5(String responseString) {
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		logOutput("You are signed up!");
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
