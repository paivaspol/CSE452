import java.io.PrintStream;

public class Utils {

  public static void logError(int addr, String output) {
    log(output, addr, System.err);
  }

  public static void logOutput(int addr, String output) {
    log(output, addr, System.out);
  }

  public static void log(String output, int addr, PrintStream stream) {
    stream.println("Node " + addr + ": " + output);
  }

}
