import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

public class FileManager {

  private final Map<Integer, TransactionalExecution> activeTransactions;
  private final TwitterServer server;

  /**
   * Constructs a new file manager
   */
  public FileManager(TwitterServer server) {
    activeTransactions = new HashMap<Integer, TransactionalExecution>();
    this.server = server;
  }

  public void onRecover() {
    // first, undo any interrupted write, find the temp file
    recoverWriteExecution();
    // second, check if a commit exists
    int numCommits = getNumCommits();
    // third, redo all the operations in the log
    redoAllLogs(numCommits);
  }

  private int getNumCommits() {
    int counter = 0;
    try {
      PersistentStorageReader reader = server.getPersistentStorageReader(TwitterServer.REDO_LOGGING_FILENAME);
      String ln = "";
      while ((ln = reader.readLine()) != null) {
        if (ln.equals(TwitterServer.COMMIT)) {
          counter++;
        }
      }
    } catch (IOException e) {
      Utils.logError(server.getNode().addr, e.getMessage());
    }
    return counter;
  }

  /*
   * Helper method for resuming append after server crash
   */
  private void recoverWriteExecution() {
    try {
      PersistentStorageReader reader = server.getPersistentStorageReader(TwitterServer.TEMP_FILENAME);
      if (reader.ready()) {
        String filename = reader.readLine();
        StringBuilder oldContent = new StringBuilder();
        readWholeFile(reader, oldContent);
        PersistentStorageWriter revertFile = server.getPersistentStorageWriter(filename, false);
        char[] oldContentChars = new char[oldContent.length()];
        oldContent.getChars(0, oldContent.length(), oldContentChars, 0);
        revertFile.write(oldContentChars);
      }
      PersistentStorageWriter tmpFileWriter = server.getPersistentStorageWriter(TwitterServer.TEMP_FILENAME, false);
      tmpFileWriter.delete();
      tmpFileWriter.close();
    } catch (IOException e) {
      Utils.logError(server.getNode().addr, e.getMessage());
    }
  }

  public void removeTransaction(int transactionId) {
    activeTransactions.remove(transactionId);
  }

  /*
   * Helper for redoing all the operations in the log file
   */
  private void redoAllLogs(int numCommits) {
    try {
      StringBuilder strBuilder = new StringBuilder();
      PersistentStorageReader reader = server.getPersistentStorageReader(TwitterServer.REDO_LOGGING_FILENAME);
      readWholeFileNoLastUpdated(reader, strBuilder);
      String[] entries = strBuilder.toString().split("\n");
      TransactionalExecution exec = new TransactionalExecution();
      for (String entry : entries) {
        if (numCommits <= 0) {
          break;
        }
        String[] tokenized = entry.split("\t");
        if (tokenized.length == 4) {
          handleMethod(Integer.parseInt(tokenized[0]), tokenized[2], tokenized[1], tokenized[3], "", exec);
        } else {
          if (tokenized[2].equals(TwitterServer.COMMIT)) {
            numCommits--;
          }
        }
      }
    } catch (IOException e) {
      Utils.logError(server.getNode().addr, e.getMessage());
    }
  }

  /**
   * Handles the transaction
   * 
   * @param transactionId
   * @param method
   * @param value
   * @return true if the operation is valid, otherwise return false.
   */
  public String handleTransaction(int transactionId, String method, String filename, String value) throws IOException {
    String retval = TwitterServer.SUCCESS + "\n";
    if (!activeTransactions.containsKey(transactionId)) {
      activeTransactions.put(transactionId, new TransactionalExecution());
    }
    TransactionalExecution exec = activeTransactions.get(transactionId);
    if (!method.equals(TwitterServer.READ) && !method.equals(TwitterServer.ABORT)) {
      exec.addLogEntry(transactionId, filename, method, value);
    }
    retval = handleMethod(transactionId, method, filename, value, retval, exec);
    return retval;
  }

  private String handleMethod(int transactionId, String method, String filename, String value, String retval,
      TransactionalExecution exec) throws IOException {
    if (method.equals(TwitterServer.CREATE)) {
      create(transactionId, filename, exec);
    } else if (method.equals(TwitterServer.APPEND)) {
      append(transactionId, filename, value, exec);
    } else if (method.equals(TwitterServer.DELETE)) {
      delete(transactionId, filename, exec);
    } else if (method.equals(TwitterServer.DELETE_LINES)) {
      deleteLines(transactionId, filename, value, exec);
    } else if (method.equals(TwitterServer.READ)) {
      retval = read(transactionId, filename, retval, exec);
    } else if (method.equals(TwitterServer.COMMIT)) {
      retval = commit(transactionId, filename, retval, exec);
    } else if (method.equals(TwitterServer.ABORT)) {
      activeTransactions.remove(transactionId);
    }
    return retval;
  }

  private void create(int transactionId, String filename, TransactionalExecution exec) throws IOException {
    StringBuilder content = new StringBuilder();
    if (Utility.fileExists(server.getNode(), filename)) {
      PersistentStorageReader reader = server.getPersistentStorageReader(filename);
      readWholeFile(reader, content);
    }
    int version = getLastModifiedVersion(filename);
    exec.modifyFile(version, filename, content.toString(), false); // put it in memory
  }

  private void append(int transactionId, String filename, String value, TransactionalExecution exec) throws IOException {
    StringBuilder content = new StringBuilder();
    String fContent = exec.readFile(filename);
    if (fContent != null) {
      // there's something
      content.append(fContent);
      content.append(value + "\n");
    } else {
      // it's null
      if (Utility.fileExists(server.getNode(), filename)) {
        PersistentStorageReader reader = server.getPersistentStorageReader(filename);
        readWholeFile(reader, content);
        content.append(value + "\n");
      } else {
        content.append(value + "\n");
      }
    }
    int version = getLastModifiedVersion(filename);
    exec.modifyFile(version, filename, content.toString(), false);
  }

  private void delete(int transactionId, String filename, TransactionalExecution exec) {
    exec.deleteFile(transactionId, filename);
  }

  private void deleteLines(int transactionId, String filename, String value, TransactionalExecution exec)
      throws IOException {
    String content = exec.readFile(filename);
    StringBuilder result = new StringBuilder();
    if (content == null) {
      PersistentStorageReader reader = server.getPersistentStorageReader(filename);
      StringBuilder cont = new StringBuilder();
      readWholeFile(reader, cont);
      content = cont.toString();
    }
    String[] file = content.toString().split("\n");
    for (String str : file) {
      if (!str.startsWith(value)) {
        result.append(str);
      }
    }
    int version = getLastModifiedVersion(filename);
    exec.modifyFile(version, filename, result.toString(), false);
  }

  private String read(int transactionId, String filename, String retval, TransactionalExecution exec)
      throws IOException {
    String content = exec.readFile(filename);
    if (content == null) {
      StringBuilder cont = new StringBuilder();
      PersistentStorageReader reader = server.getPersistentStorageReader(filename);
      readWholeFile(reader, cont);
      content = cont.toString();
      int version = getLastModifiedVersion(filename);
      exec.modifyFile(version, filename, content, true);
    }
    retval += content;
    return retval;
  }

  private String commit(int transactionId, String filename, String retval, TransactionalExecution exec)
      throws IOException {

    // we're good, let's first write the log file, then the content of
    // the files
    Map<String, FileHolder> filesModified = exec.getAllModfiedFiles();
    String logContent = exec.getLogContent();
    writeToFile(TwitterServer.REDO_LOGGING_FILENAME, logContent);
    // at this point we are sure that all the contents of the log is on
    // file.
    for (String file : filesModified.keySet()) {
      int lastTxn = 0;
      if (Utility.fileExists(server.getNode(), file)) {
        lastTxn = getLastModifiedVersion(file);
      }
      if (filesModified.get(file).getVersion() == lastTxn) {
        if (exec.isFileDeleted(file)) {
          deleteFile(transactionId, file);
        } else {
          if (!filesModified.get(file).isReadOperation()) {
            String content = transactionId + "\n";
            content += filesModified.get(file).getContent();
            writeToFile(file, content);
          }
        }
      } else {
        retval = TwitterServer.ROLLBACK;
        break;
      }
    }
    return retval;
  }

  /**
   * Returns the last transaction id that modified this file.
   * 
   * @param filename the file
   * @return
   */
  public int getLastModifiedVersion(String filename) throws IOException {
    if (!Utility.fileExists(server.getNode(), filename)) {
      return 0;
    }
    PersistentStorageReader reader = server.getPersistentStorageReader(filename);
    String lastTxn = reader.readLine();
    if (lastTxn == null) {
      return 0;
    }
    return Integer.parseInt(lastTxn);
  }

  public void deleteFile(int transactionId, String filename) throws IOException {
    PersistentStorageWriter writer = server.getPersistentStorageWriter(filename, false);
    writer.delete();
  }

  /**
   * write to a file
   * 
   * @param filename
   * @param content
   * @throws IOException
   */
  public void writeToFile(String filename, String content) throws IOException {
    PersistentStorageReader reader = null;
    PersistentStorageWriter tempFileWriter = server.getPersistentStorageWriter(TwitterServer.TEMP_FILENAME, false);
    StringBuilder tempContent = new StringBuilder(filename + "\n");
    StringBuilder oldContent = new StringBuilder();
    if (Utility.fileExists(server.getNode(), filename)) {
      reader = server.getPersistentStorageReader(filename);
      readWholeFile(reader, oldContent);
      reader.close();
    }
    // first, write the tmp file
    tempContent = tempContent.append(oldContent);
    tempFileWriter.write(tempContent.toString());
    tempFileWriter.flush();
    // we're not appending, but creating temp file should help us with
    // recovery
    PersistentStorageWriter writer = server.getPersistentStorageWriter(filename, false);
    writer.write(content);
    tempFileWriter.delete();
    // close all the file connections
    writer.flush();
    writer.close();
    tempFileWriter.close();
  }

  // reads the whole file in the reader into the oldContent variable
  public void readWholeFile(PersistentStorageReader reader, StringBuilder builder) throws IOException {
    reader.readLine(); // assuming there's the last updated
    readWholeFileNoLastUpdated(reader, builder);
  }

  // reads the whole file in the reader into the oldContent variable
  public void readWholeFileNoLastUpdated(PersistentStorageReader reader, StringBuilder builder) throws IOException {
    String temp = "";
    while ((temp = reader.readLine()) != null) {
      builder = builder.append(temp + "\n");
    }
  }
}
