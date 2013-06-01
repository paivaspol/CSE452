import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Paxos node that has all the Paxos roles: acceptor, learner, proposer
 */
public class PaxosNode {

	/** the wrapper of this node */
	private final TwitterNodeWrapper wrapper;
	
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

	}

	public void start() {
		// TODO Auto-generated method stub

	}

	public void onCommand(String command) {
		// Shouldn't be accepting any command
	}

}
