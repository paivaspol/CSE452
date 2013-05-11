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
    switch (method) {
      case (TwitterServer.CREATE): {
        create(filename, exec);
        break;
      }
      case (TwitterServer.APPEND): {
        append(filename, value, exec);
        break;
      }
      case (TwitterServer.DELETE): {
        delete(filename, exec);
        break;
      }
      case (TwitterServer.DELETE_LINES): {
        deleteLines(filename, value, exec);
        break;
      }
      case (TwitterServer.READ): {
        retval = read(filename, retval, exec);
        break;
      }
      case (TwitterServer.COMMIT): {
        retval = commit(transactionId, filename, retval, exec);
      }
      case (TwitterServer.ABORT): {
        activeTransactions.remove(transactionId);
        break;
      }
    }
    return retval;
  }

  private void create(String filename, TransactionalExecution exec) throws IOException {
    StringBuilder content = new StringBuilder();
    if (Utility.fileExists(server.getNode(), filename)) {
      PersistentStorageReader reader = server.getPersistentStorageReader(filename);
      readWholeFile(reader, content);
    }
    exec.modifyFile(filename, content.toString()); // put it in memory
  }

  private void append(String filename, String value, TransactionalExecution exec) throws IOException {
    StringBuilder content = new StringBuilder();
    String fContent = exec.readFile(filename);
    if (fContent != null) {
      // there's something
      fContent += value + "\n";
      content.append(fContent);
    } else {
      // it's null
      if (Utility.fileExists(server.getNode(), filename)) {
        PersistentStorageReader reader = server.getPersistentStorageReader(filename);
        readWholeFile(reader, content);
        content.append(value + "\n");
      } else {
        content.append(value + "\n");
        exec.modifyFile(filename, content.toString()); // put it in memory
      }
    }
    exec.modifyFile(filename, content.toString());
  }

  private void delete(String filename, TransactionalExecution exec) {
    exec.deleteFile(filename);
  }

  private void deleteLines(String filename, String value, TransactionalExecution exec) throws IOException {
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
      if (str.startsWith(value)) {
        result.append(str);
      }
    }
    exec.modifyFile(filename, result.toString());
  }

  private String read(String filename, String retval, TransactionalExecution exec) throws IOException {
    String content = exec.readFile(filename);
    if (content == null) {
      StringBuilder cont = new StringBuilder();
      PersistentStorageReader reader = server.getPersistentStorageReader(filename);
      readWholeFile(reader, cont);
      content = cont.toString();
      exec.modifyFile(filename, content);
    }
    retval += content;
    return retval;
  }

  private String commit(int transactionId, String filename, String retval, TransactionalExecution exec)
      throws IOException {
    int lastTxn = getLastModifiedVersion(filename);
    if (lastTxn > transactionId) {
      // there's a conflict
      retval = TwitterServer.FAILURE;
    } else {
      // we're good, let's first write the log file, then the content of the files
      Map<String, FileHolder> filesModified = exec.getAllModfiedFiles();
      String logContent = exec.getLogContent();
      writeToFile(TwitterServer.REDO_LOGGING_FILENAME, logContent);
      // at this point we are sure that all the contents of the log is on file.
      for (String file : filesModified.keySet()) {
        if (exec.isFileDeleted(file)) {
          deleteFile(filename);
        } else {
          String content = transactionId + "\n";
          content += filesModified.get(filename);
          writeToFile(file, content);
        }
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
    PersistentStorageReader reader = server.getPersistentStorageReader(filename);
    String lastTxn = reader.readLine();
    return Integer.parseInt(lastTxn);
  }

  public void deleteFile(String filename) throws IOException {
    PersistentStorageWriter writer = server.getPersistentStorageWriter(filename, false);
    writer.delete();
    writer.flush();
  }

  /**
   * write to a file
   * 
   * @param filename
   * @param content
   * @throws IOException
   */
  public void writeToFile(String filename, String content) throws IOException {
    PersistentStorageReader reader = server.getPersistentStorageReader(filename);
    PersistentStorageWriter tempFileWriter = server.getPersistentStorageWriter(TwitterServer.TEMP_FILENAME, false);
    StringBuilder tempContent = new StringBuilder(filename + "\n");
    StringBuilder oldContent = new StringBuilder();
    readWholeFile(reader, oldContent);
    // first, write the tmp file
    tempContent = tempContent.append(oldContent);
    tempFileWriter.write(tempContent.toString());
    tempFileWriter.flush();
    // we're not appending, but creating temp file should help us with recovery
    PersistentStorageWriter writer = server.getPersistentStorageWriter(filename, false);
    writer.write(content);
    tempFileWriter.delete();
    // close all the file connections
    writer.flush();
    reader.close();
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
