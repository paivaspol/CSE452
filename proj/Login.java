import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.cse490h.lib.Callback;

/**
 * Contains a sequence of events for logging in.
 * 
 * @author leelee
 *
 */
public class Login extends Function {

	private int serverAddress;
	private String usersFile;
	private String username;
	private String password;
	private StringData strData;
	private long timestamp;

	public Login(Client client, RIONode rioNode, int serverAddress, String usersFile, String username, String password) {
		super(client, rioNode);
		this.serverAddress = serverAddress;
		this.usersFile = usersFile;
		this.username = username;
		this.password = password;
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
		} catch (Exception e) {
			e.printStackTrace();
			rioNode.fail();
		}
		eventList = list;
		return list;
	}

	public void step0() {
		TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
		client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
		client.eventIndex = 1;
	}

	public void step1(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpBeginT = new TwitterProtocol(TwitterServer.BEGIN_TRANSACTION, new Entry(rioNode.addr).getHash());
			client.RIOSend(serverAddress, Protocol.DATA, tpBeginT.toBytes());
			client.eventIndex = 1;
			return;
		}
		timestamp = response.getTimestamp();
		// check if the user exists
		TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
		tpCheckUser.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
		client.eventIndex = 2;
	}

	public void step2(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpCheckUser = new TwitterProtocol(TwitterServer.READ, usersFile, null, new Entry(rioNode.addr).getHash());
			tpCheckUser.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpCheckUser.toBytes());
			client.eventIndex = 2;
			return;
		}
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
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
			client.completeCommand();
			return;
		}

		// correct username and password so proceed with login user
		strData = new StringData(rioNode.addr);
		strData.setData(username + "\n");
		TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.APPEND, "login.txt", strData.toString(), strData.getHash());
		tpQuery.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
		client.eventIndex = 3;
	}

	public void step3(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			// correct username and password so proceed with login user
			TwitterProtocol tpQuery = new TwitterProtocol(TwitterServer.APPEND, "login.txt", strData.toString(), strData.getHash());
			tpQuery.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpQuery.toBytes());
			client.eventIndex = 3;
			return;
		}
		if (!responseString.startsWith(TwitterServer.SUCCESS)) {
			logError("Error issuing request to server.");
			client.eventIndex = 0;
			client.completeCommand();
			return;
		}
		TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
		tpCommit.setTimestamp(timestamp);
		client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
		client.eventIndex = 4;
	}
	
	public void step4(TwitterProtocol response) {
		String responseString = response.getData();
		if (responseString.startsWith(TwitterServer.RESTART)) {
			TwitterProtocol tpCommit = new TwitterProtocol(TwitterServer.COMMIT, new Entry(rioNode.addr).getHash());
			tpCommit.setTimestamp(timestamp);
			client.RIOSend(serverAddress, Protocol.DATA, tpCommit.toBytes());
			client.eventIndex = 4;
			return;
		}
		logOutput("You are login!");
		client.eventIndex = 0;
		client.completeCommand();
	}
}
