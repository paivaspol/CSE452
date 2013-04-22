import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 
 * @author vaspol
 * 
 */
public class WALLogReader {

  /** singleton instance for WALLogReader */
  private static final WALLogReader instance = new WALLogReader();

  private static final String LOG_FILENAME = "log";

  private BufferedReader reader;

  /**
   * Constructs a new WALLogReader object
   */
  private WALLogReader() {
    try {
      reader = new BufferedReader(new FileReader(new File(LOG_FILENAME)));
    } catch (IOException e) {
      reader = null;
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the singleton instance of this WAL reader
   */
  public static WALLogReader getInstance() {
    return instance;
  }
}
