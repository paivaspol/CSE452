import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
  public static final String CONSE = "conses";
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String CHANGE = "change";
  public static final String ACCEPTED = "accepted";
  public static final String PREPARE_FAILED = "prepFailed";
  public static final String ACCEPT_FAILED = "acceptFailed";

  /** filenames */
  private static final String PAXOS_STATE_FILENAME = "paxos_state.txt";
  private static final String VALUE_FILENAME = "value_file.txt";
  private static final String FILE_SERVER_REQ_VALUE = "file_server_req_val.txt";

  private final String paxosLogFileName;
  private String fileServerRequest;

  private int promiseReceivedCounter;
  private int acceptedReceivedCounter;
  private int fileServer;
  private long prevChangeAnnounceId;
  private long currServerRequestId;
  private long inProgressId;

  public enum State {
    PROPOSE, ACTIVE, ACCEPTED, CHANGE, ACCEPT;

    public static State strToState(String str) {
      if (str.equals(PROPOSE.toString())) {
        return PROPOSE;
      } else if (str.equals(ACTIVE.toString())) {
        return ACTIVE;
      } else if (str.equals(ACCEPTED.toString())) {
        return ACCEPTED;
      } else {
        return null;
      }
    }
  }

  public int highestProposalNumberSeen;
  public String value;

  public State state;
  private final List<Integer> otherPaxosNodes;

  public PaxosNode(TwitterNodeWrapper wrapper) {
    this.wrapper = wrapper;
    otherPaxosNodes = new ArrayList<Integer>();
    populateOtherPaxosNodeAndServer(wrapper.addr);
    paxosLogFileName = "paxosLog" + wrapper.addr;
    state = State.ACTIVE;
    highestProposalNumberSeen = 0;
    promiseReceivedCounter = 1;
    acceptedReceivedCounter = 1;
    prevChangeAnnounceId = -1;
    currServerRequestId = -1;
    inProgressId = -1;
  }

  // assuming there are only three paxos nodes that corresponds to 2, 3, 4
  private void populateOtherPaxosNodeAndServer(int addr) {
    if (addr == 2) {
      fileServer = 0;
      otherPaxosNodes.add(4);
      otherPaxosNodes.add(3);
    } else if (addr == 3) {
      fileServer = 1;
      otherPaxosNodes.add(4);
      otherPaxosNodes.add(2);
    } else if (addr == 4) {
      fileServer = -1;
      otherPaxosNodes.add(2);
      otherPaxosNodes.add(3);
    } else {
      throw new RuntimeException("How can you get here!?");
    }
  }

  public void onRIOReceive(Integer from, int protocol, byte[] msg) {
    TwitterProtocol tp = TwitterNodeWrapper.GSON.fromJson(new String(msg), TwitterProtocol.class);
    String method = tp.getMethod();
    String response = "";
    
    Utils.logOutput(wrapper.addr, "HHHHHHHH " + from + " method: " + method);
    
    
    // check if it is from other paxos or from file server?
    if (from == 0 || from == 1) {
      // it is a server
      if (method.startsWith(TwitterServer.RESTART)) {
        try {
          response = readFile(paxosLogFileName);
          assert (response != null);
          tp.setMethod(response);
          wrapper.RIOSend(from, protocol, tp.toBytes());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else if (method.startsWith(PaxosNode.CHANGE)) {
        // propose the change to other paxos node
        try {

          prepare(tp.getFileServerRequestValue(), tp.getTransactionTimestamp());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        throw new IllegalArgumentException("Unexpected method name from server: " + method);
      }

    } else if (otherPaxosNodes.contains(from)) {
      if (method.equals("TIMEOUT") && fileServer != -1) {
        Utils.logOutput(wrapper.addr, "\t\t\tHERE!");
        try {
          prepare(value, inProgressId);
        } catch (IOException e) {
         throw new RuntimeException(e);
        }
        return;
      }
      if (method.equals(PaxosNode.PREPARE)) {
        // other paxos node wants to propose
        handlePrepare(tp.getProposalNumber(), from, tp.getTransactionTimestamp());
      } else if (method.equals(PaxosNode.PROMISE)) {
        // other paxos promise
        handlePromise(tp.getPromiseValue(), from, tp.getTransactionTimestamp());
      } else if (method.equals(PaxosNode.ACCEPT)) {
        // other paxos is announcing the change
        handleAccept(tp.getProposalNumber(), tp.getAcceptValue(), from, tp.getTransactionTimestamp());
      } else if (method.equals(PaxosNode.CONSE)) {
        // someone announced the change, we need to ask our server to execute it.
        // first compare our paxos log with the one sent by them, see which one
        // we miss, then ask our server to execute, may be more than one
        // transaction.
        try {
          Utils.logOutput(wrapper.addr, tp.getConsensusValue());

          handleConsensus(tp.getConsensusValue(), tp.getTransactionTimestamp());
        } catch (IOException e) {
          throw new RuntimeException("something went wrong in handleChange\n" + e.getMessage());
        }
      } else if (method.equals(PaxosNode.ACCEPTED)) {
        try {
          handleAccepted(tp.getAcceptedValue(), tp.getTransactionTimestamp()); // TODO: double check if this is the
                                                                               // correct value or not
        } catch (IOException e) {
          throw new RuntimeException("something went wrong in handleAccepted()\n" + e.getMessage());
        }
      } else if (method.equals(PaxosNode.ACCEPT_FAILED)) {
        try {
          handleAcceptFailed(value, tp.getTransactionTimestamp());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else if (method.equals(PaxosNode.PREPARE_FAILED)) {
        try {
          handlePrepareFailed(value, tp.getTransactionTimestamp());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      }
    } else {
      throw new IllegalArgumentException("Don't send request to me!");
    }
  }

  // Check if the proposal number if greater than the current one.
  // returns the content of the value, null when the node should reject.
  private void handlePrepare(int n, int from, long id) {
    if (highestProposalNumberSeen >= n) {
      TwitterProtocol prepFailed = new TwitterProtocol(PaxosNode.PREPARE_FAILED, new Entry(wrapper.addr).getHash());
      prepFailed.setProposalNumber(highestProposalNumberSeen);
      prepFailed.setTransactionTimestamp(n);
      wrapper.RIOSend(from, Protocol.DATA, prepFailed.toBytes());
    } else {
      inProgressId = id;
      TwitterProtocol promise = new TwitterProtocol(PaxosNode.PROMISE, new Entry(wrapper.addr).getHash());
      promise.setProposalNumber(highestProposalNumberSeen);
      highestProposalNumberSeen = n;
      promise.setPromiseValue(value);
      promise.setTransactionTimestamp(n);
      wrapper.RIOSend(from, Protocol.DATA, promise.toBytes());

    }
  }

  /**
   * Received promise so is a majority, send accept.
   * 
   * @param valuePAXOS_STATE_FILENAME
   * @return
   */
  private void handlePromise(String value, int from, long id) {
    // send accept()
    promiseReceivedCounter++;
    state = State.ACCEPT;
    inProgressId = id;
    if (promiseReceivedCounter >= 2 && id == highestProposalNumberSeen) {
      if (value != null) {
        this.value = value;
      }
      TwitterProtocol accept = new TwitterProtocol(PaxosNode.ACCEPT, new Entry(wrapper.addr).getHash());
      // our proposal number
      accept.setProposalNumber(highestProposalNumberSeen);
      accept.setTransactionTimestamp(inProgressId);
      if (value == null) {
        Utils.logOutput(wrapper.addr, "VALUE IS NULLLLLL");
      }
      accept.setAcceptValue(this.value);
      wrapper.RIOSend(from, Protocol.DATA, accept.toBytes());
    }
  }

  private void handlePrepareFailed(String value, long id) throws IOException {
    if (state.equals(State.PROPOSE) && id == highestProposalNumberSeen) {
      prepare(value, id);
    }
  }

  private void handleAcceptFailed(String value, long id) throws IOException {
    if (state.equals(State.ACCEPT) && id == highestProposalNumberSeen) {
      prepare(value, id);
    }
  }

  /**
   * Server wants to change, so propose for server by sending prepare.
   * 
   * @param value
   * @throws IOException
   */
  private void prepare(String value, long id) throws IOException {
    // TODO(vaspol): need to consider the case where we are not in the state of
    // accepting?
    currServerRequestId = id;
    inProgressId = id;
    state = State.PROPOSE;
    highestProposalNumberSeen++;
    appendFile(PAXOS_STATE_FILENAME, PREPARE + "\n" + highestProposalNumberSeen);
    this.value = value;
    fileServerRequest = value;
    // send to other nodes
    TwitterProtocol proposal = new TwitterProtocol(PaxosNode.PREPARE, new Entry(wrapper.addr).getHash());
    proposal.setProposalNumber(highestProposalNumberSeen);
    for (int i : otherPaxosNodes) {
      wrapper.RIOSend(i, Protocol.DATA, proposal.toBytes());
    }
  }

  /**
   * Reached consensus, so ask server to do the transaction
   * 
   * @throws IOException
   */
  private void handleConsensus(String logEntry, long id) throws IOException {
    Utils.logOutput(wrapper.addr, "LogEntry: " + logEntry);
    RandomAccessFile raf = new RandomAccessFile(getFullPath(paxosLogFileName), "rw");
    long length = raf.length();
    String toExecute;
    toExecute = logEntry.substring((int) length);

    raf.seek(length);
    // TODO(): possible bugs, look here if there are bugs!
    Utils.logOutput(wrapper.addr, "bef toExec: " + toExecute);
    raf.writeBytes(toExecute);
    raf.close();
    Utils.logOutput(wrapper.addr, "aft toExec: " + toExecute);
    if (fileServer != -1) {
      boolean isYes = false;
      if (fileServerRequest != null) {
        isYes = toExecute.contains(fileServerRequest);
      }
      executeConsensus(toExecute, isYes, id);
    }
    value = null;
  }

  private void handleAccepted(String newValue, long id) throws IOException {
    if (id == highestProposalNumberSeen) {
      acceptedReceivedCounter++;
      Utils.logOutput(wrapper.addr, "\t" + id + " " + prevChangeAnnounceId + " " + currServerRequestId);
      if (acceptedReceivedCounter >= 2 && id != prevChangeAnnounceId) {
        announceChange();
      }
    }
  }

  private void handleAccept(int n, String value, int from, long id) {
    if (n >= highestProposalNumberSeen) {
      inProgressId = id;
      TwitterProtocol accepted = new TwitterProtocol(PaxosNode.ACCEPTED, new Entry(wrapper.addr).getHash());
      accepted.setAcceptedValue(value);
      accepted.setTransactionTimestamp(n);
      wrapper.RIOSend(from, Protocol.DATA, accepted.toBytes());
    } else {
      TwitterProtocol acceptFailed = new TwitterProtocol(PaxosNode.ACCEPT_FAILED, new Entry(wrapper.addr).getHash());
      acceptFailed.setProposalNumber(highestProposalNumberSeen);
      acceptFailed.setTransactionTimestamp(n);
      wrapper.RIOSend(from, Protocol.DATA, acceptFailed.toBytes());
    }
  }

  /**
   * tell the corresponding server
   * 
   * @throws IOException
   */
  private void announceChange() throws IOException {
    prevChangeAnnounceId = inProgressId;
    promiseReceivedCounter = 1;
    acceptedReceivedCounter = 1;
    // send to every paxos node the value.
    TwitterProtocol decided = new TwitterProtocol(PaxosNode.CONSE, new Entry(wrapper.addr).getHash());
    // send whole paxos log
    // append current value to paxosLogFile then readFile
    appendFile(paxosLogFileName, value);
    // p
    Utils.logOutput(wrapper.addr, "readFileContent " + readFile(paxosLogFileName));
    decided.setConsensusValue(readFile(paxosLogFileName));
    decided.setTransactionTimestamp(currServerRequestId);
    for (int paxosNum : otherPaxosNodes) {
      wrapper.RIOSend(paxosNum, Protocol.DATA, decided.toBytes());
    }
    // ask own fileserver to execute if this paxos node has a mapping to a fileserver
    if (fileServer != -1) {
      boolean isYes = false;
      if (value.equals(fileServerRequest)) {
        isYes = true;
      }
      // executeConsensus(fileServerRequest, isYes, inProgressId);
      executeConsensus(value, isYes, inProgressId);
    }
  }

  // someone annouce change, we need to ask our server to execute it.
  // first compare our paxos log with the one sent by them, see which one
  // we miss, then ask our server to execute, may be more than one transaction.
  private void executeConsensus(String logEntry, boolean isYes, long id) {
    TwitterProtocol tp;

    if (isYes) {
      tp = new TwitterProtocol(PaxosNode.YES, new Entry(wrapper.addr).getHash());
    } else {
      tp = new TwitterProtocol(PaxosNode.NO, new Entry(wrapper.addr).getHash());
    }
    // no need to send no because we are trying for the file server already.
    tp.setData(logEntry);
    tp.setTransactionTimestamp(id);
    wrapper.RIOSend(fileServer, Protocol.DATA, tp.toBytes());
  }

  public void start() {
    if (!Utility.fileExists(wrapper, paxosLogFileName)) {
      try {
        createFile(paxosLogFileName);
        createFile(PAXOS_STATE_FILENAME);
        createFile(VALUE_FILENAME);
        createFile(FILE_SERVER_REQ_VALUE);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      // TODO(vaspol): retrieve the file and set back the state
      String stateString = "";
      String valueStr = "";
      String fileServerStr = "";
      try {
        stateString = readFile(PAXOS_STATE_FILENAME);
        valueStr = readFile(VALUE_FILENAME);
        fileServerStr = readFile(FILE_SERVER_REQ_VALUE);
      } catch (Exception e) {
        throw new RuntimeException("Read failed");
      }
      Scanner scan = new Scanner(stateString);
      state = State.strToState(scan.nextLine());
      highestProposalNumberSeen = Integer.parseInt(scan.nextLine());
      value = valueStr;
      fileServerRequest = fileServerStr;
      scan.close();
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

  /**
   * Append a data to the end of the file
   * 
   * @param collectionName the filename
   * @param data the line to be appended at the end of the file
   * @throws IOException
   */
  private void appendFile(String collectionName, String data) throws IOException {
    PersistentStorageReader reader = wrapper.getReader(collectionName);
    PersistentStorageWriter tempFileWriter = wrapper.getWriter(TwitterServer.TEMP_FILENAME, false);
    StringBuilder tempContent = new StringBuilder(collectionName + "\n");
    StringBuilder oldContent = new StringBuilder();
    readWholeFile(reader, oldContent);
    // first, write the tmp file
    tempContent = tempContent.append(oldContent);
    tempFileWriter.write(tempContent.toString());
    // append the new content
    oldContent = oldContent.append(data);
    PersistentStorageWriter writer = wrapper.getWriter(collectionName, false);
    writer.write(oldContent.toString());
    tempFileWriter.delete();
    // close all the file connections
    reader.close();
    writer.close();
    tempFileWriter.close();
  }

  public void onCommand(String command) {
    // Shouldn't be accepting any command
    throw new RuntimeException("No commands for paxos please!");
  }

  private String getFullPath(String filename) {
    return Utility.realFilename(wrapper.addr, filename);
  }

}
