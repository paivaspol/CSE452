import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.gson.Gson;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;

public class TwitterNodeWrapper extends RIONode {

	/**
	 * Client and Server instances
	 */
	private Client client;
	private TwitterServer server;
	private PaxosNode paxosNode;
	public static final Gson GSON = new Gson();

	@Override
	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		if (client != null) {
			// push to client
			client.onRIOReceive(from, protocol, msg);
		} else if (server != null) {
			// push to storage server
			server.onRIOReceive(from, protocol, msg);
		} else if (paxosNode != null) {
			// push to paxosNode
			paxosNode.onRIOReceive(from, protocol, msg);
		} else {
			throw new RuntimeException("Good job dude :)");
		}
	}

	@Override
	public void start() {

		if (super.addr == 0 || super.addr == 1) {
			Utils.logOutput(super.addr, "Starting up server...");
			// Generate a user-level synoptic event to indicate that the node
			// started.
			logSynopticEvent("started");
			// assuming that it is a server
			server = new TwitterServer(this);
			server.start();
		} else if (super.addr == 2 || super.addr == 3 || super.addr == 4) {
			// paxos node
			Utils.logOutput(super.addr, "Starting up Paxos node...");
			paxosNode = new PaxosNode(this);
			paxosNode.start();
		} else {
			Utils.logOutput(super.addr, "Starting up client...");
			client = new Client(this);
			client.start();
		}
	}

	@Override
	public void onCommand(String command) {
		try {
			client.onCommand(command);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * Parsing the bytes array to string representation
	 */
	@Override
	public String packetBytesToString(byte[] bytes) {
		RIOPacket packet = RIOPacket.unpack(bytes);
		if (packet == null) {
			return super.packetBytesToString(bytes);
		}
		return packet.toString();
	}

	/**
	 * @return the address of the node
	 */
	public int getAddr() {
		return super.addr;
	}

	/**
	 * @return a PersistentStorageWriter for the given filename
	 * @throws IOException
	 */
	@Override
	public PersistentStorageWriter getWriter(String filename, boolean append)
			throws IOException {
		return super.getWriter(filename, append);
	}

	/**
	 * @return a PersistentStorageWriter for the given filename
	 * @throws FileNotFoundException
	 */
	@Override
	public PersistentStorageReader getReader(String filename)
			throws FileNotFoundException {
		return super.getReader(filename);
	}
}
