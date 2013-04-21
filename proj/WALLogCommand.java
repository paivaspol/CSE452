/**
 * enum for WAL constants
 * 
 * @author vaspol
 * 
 */
public enum WALLogCommand {

  BEGIN, COMMIT;

  /**
   * Decode the given command to a WALLogCommand
   * 
   * @param command the string representation of the command to be decoded
   * @return the WALLogCommand enum constant
   */
  public WALLogCommand decodeStringToLog(String command) {
    switch (command) {
      case ("BEGIN"):
        return BEGIN;
      case ("COMMIT"):
        return COMMIT;
      default:
        return null;
    }
  }

}