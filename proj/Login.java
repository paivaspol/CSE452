import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;


public class Login extends Function {
	
	private int serverAddress;
	private String usersFile;
	private String username;
	private String password;

	public Login(RIONode rioNode, int serverAddress, String usersFile, String username, String password) {
		super(rioNode);
		this.serverAddress = serverAddress;
		this.usersFile = usersFile;
		this.username = username;
		this.password = password;
	}

	@Override
	public List<Callback> init() {
		List<Callback> list = new ArrayList<Callback>();
		try {
			list.add(new Callback(Callback.getMethod("step1", this, null), this, null));
			list.add(new Callback(Callback.getMethod("step2", this, new String[] {"java.lang.String"}), this, null));
			list.add(new Callback(Callback.getMethod("step3", this, new String[] {"java.lang.String"}), this, null));
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = list;
		return list;
	}
	
	public void step1() {
		// check if the user exists
		TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null);
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
	}
	
	public void step2(String responseString) {
		logOutput("Login " + responseString);
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		String usersList = responseString.substring(8);
		String[] names = usersList.split("\n");
		boolean isValidUser = false;
		for (int i = 0; i < names.length; i++) {
			if (names[i].startsWith(username)) {
				String[] info = names[i].split("\t");
				if (info[1].equals(password)) {
					isValidUser = true;
					break;
				}
			}
		}
		if (!isValidUser) {
			logError("username:" + " " + username + " password: " + password + " does not exist." );
			return;
		}
		
		// correct username and password so proceed with login user
		TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.APPEND, "login.txt", username + "\n");
		rioNode.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
	}
	
	public void step3(String responseString) {
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			rioNode.fail();
			return;
		}
		logOutput("You are login!");
	}
}
