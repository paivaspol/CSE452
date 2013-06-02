import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

/**
 * Represents a Paxos node that has all the Paxos roles: acceptor, learner, proposer
 */
public class PaxosNode {

	/** the wrapper of this node */
	private final TwitterNodeWrapper wrapper;
	public static final String PREPARE = "prepare";
	public static final String PROMISE = "promise";
	public static final String ACCEPT = "accept";
	public static final String ACCEPTED = "accepted";
	public static final String YES = "yes";
	public static final String NO = "no";
	public static final String CHANGE = "change";
	public String paxosLogFileName;
	public enum State {
		PROPOSE, OPEN
	}
	
	public int highestProposalNumberSeen;
	public int highestValueId;
	
	public State state;
	private Set<Integer> otherPaxosNodes;
	
	public PaxosNode(TwitterNodeWrapper wrapper) {
		this.wrapper = wrapper;
		otherPaxosNodes = new HashSet<Integer>();
		populateOtherPaxosNode(wrapper.addr);
		paxosLogFileName = "paxosLog" + wrapper.addr;
		state = State.OPEN;
		highestProposalNumberSeen = 0;
	}
	
	// assuming there are only three paxos nodes that corresponds to 2, 3, 4
	private void populateOtherPaxosNode(int addr) {
		if (addr == 2) {
			otherPaxosNodes.add(4);
			otherPaxosNodes.add(3);
		} else if (addr == 3) {
			otherPaxosNodes.add(4);
			otherPaxosNodes.add(2);
		} else if (addr == 4) {
			otherPaxosNodes.add(2);
			otherPaxosNodes.add(3);
		} else {
			throw new RuntimeException("How can you get here!?");
		}
	}
	
	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
		TwitterProtocol tp = TwitterNodeWrapper.GSON.fromJson(new String(msg), TwitterProtocol.class);
		String method = tp.getMethod();
		String response = null;
		// check if it is from other paxos or from file server?
		if (from == 0 || from == 1) {
			// it is a server
			if (method.startsWith(TwitterServer.RESTART)) {
				try {
					response = readFile(paxosLogFileName);
					assert(response != null);
					tp.setMethod(response);
					wrapper.RIOSend(from, protocol, tp.toBytes());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else if (method.startsWith(PaxosNode.CHANGE)) {
				// propose the change to other paxos node
				
				propose();
			} else {
				throw new IllegalArgumentException("Unexpected method name from server: " + method);
			}

		} else if (otherPaxosNodes.contains(from)) {
			// other paxos node wants to propose
			handlePropose();
			// other paxos is anouncing the change
			handleChange();
			// other paxos promise
			handlePromise();
			// majority accepted, yeah!
			announceChange();
		} else {
			throw new IllegalArgumentException("Don't send request to me!");
		}

	}
	
	// Check if the proposal number if greater than the current one.
	private void handlePropose() {
		
	}
	
	private void handlePromise() {
		
	}
	
	private void propose() {
		state = State.PROPOSE;
		// TODO(vaspol): force write the new state to disk: PROPOSE highestProposalNumberSeen
		highestProposalNumberSeen++;
		// send to other nodes
		TwitterProtocol proposal = new TwitterProtocol(PaxosNode.PREPARE, new Entry(wrapper.addr).getHash());
		proposal.setProposalNumber(highestProposalNumberSeen);
		for (int i : otherPaxosNodes) {
			wrapper.RIOSend(i, Protocol.DATA, proposal.toBytes());
		}
	}

	private void handleChange() {
		
	}
	
	private void announceChange() {
		
	}
	
	public void start() {
		if (!Utility.fileExists(wrapper, paxosLogFileName)) {
			try {
				createFile(paxosLogFileName);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			// TODO(vaspol): retrieve the file and set back the state
		}
	}
	
	// creates a file with the collection name
	private boolean createFile(String collectionName) throws IOException {
		if (!Utility.fileExists(wrapper, collectionName)) {
			PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
			writer.write(new char[] {});
		}
		return true;
	}
	
	// returns all the entries from the file associated to the reader object
	private String readFile(String collectionName) throws IOException {
		StringBuilder fileContent = new StringBuilder();
		PersistentStorageReader reader = wrapper.getReader(collectionName);
		readWholeFile(reader, fileContent);
		return fileContent.toString();
	}
	
	// reads the whole file in the reader into the oldContent variable
	public void readWholeFile(PersistentStorageReader reader, StringBuilder builder) throws IOException {
		String temp = "";
		while ((temp = reader.readLine()) != null) {
			builder = builder.append(temp + "\n");
		}
	}

	public void onCommand(String command) {
		// Shouldn't be accepting any command
		throw new RuntimeException("No commands for paxos please!");
	}

}
