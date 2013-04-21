import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Singleton that handles Write Ahead Logging mechanism
 * 
 * @author vaspol
 * 
 */
public class WALLogWriter {

  /** instance of the logger */
  private static final WALLogWriter instance = new WALLogWriter();

  /**
   * WALLogger string constants
   */
  public static final String COMMIT = "commit";
  public static final String BEGIN = "begin";
  private static final String LOG_FILENAME = "log";

  private BufferedWriter writer;

  /**
   * Initializes the logger object
   */
  private WALLogWriter() {
    try {
      writer = new BufferedWriter(new FileWriter(new File(LOG_FILENAME), false));
    } catch (IOException e) {
      writer = null;
      throw new RuntimeException(e);
    }
  }

  /**
   * Access method for the instance of the logger
   * 
   * @return the logger object
   */
  public static WALLogWriter getInstance() {
    return instance;
  }

  /**
   * Logs an event to the log file
   * 
   * @param command the command that needs to be logged such as insert, delete, update
   * @param arguments the arguments of that command such as the old value, new value
   */
  public void log(String command, List<String> arguments) {
    try {
      StringBuilder logEntry = new StringBuilder(command);
      for (String arg : arguments) {
        logEntry.append(" " + arg);
      }
      writer.write(logEntry.toString());
      // these log files has to flush everything to disk every time it logs something
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
