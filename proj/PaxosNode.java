import java.util.HashSet;
import java.util.Set;

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
	
	
	
	private Set<Integer> otherPaxosNodes;
	
	public PaxosNode(TwitterNodeWrapper wrapper) {
		this.wrapper = wrapper;
		otherPaxosNodes = new HashSet<Integer>();
		populateOtherPaxosNode(wrapper.addr);
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
		// TODO Auto-generated method stub
		// check if it is from other paxos or from file server?
//		if () {
//			
//		}
	}

	public void start() {
		// TODO Auto-generated method stub

	}

	public void onCommand(String command) {
		// Shouldn't be accepting any command
	}

}
