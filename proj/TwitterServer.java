/**
 * Twitter Server that will handle requests from Twitter Client
 * @author vaspol
 */

public class TwitterServer extends RIONode {

    public static final String CREATE = "create";
    public static final String READ = "read";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    
    @Override
    public void onRIOReceive(Integer from, int protocol, byte[] msg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void start() {
        
    }

    /**
     * accept commands
     */
    @Override
    public void onCommand(String command) {
        if (command.equals(CREATE)) {
            
        } else if (command.equals(READ)) {
            
        } else if (command.equals(UPDATE)) {
            
        } else if (command.equals(DELETE)) {
            
        } else {
            throw new RuntimeException("Command not supported by the server");
        }
    }
}
