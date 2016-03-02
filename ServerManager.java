import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class ServerManager {
  public static void main (String[] argv) {
    // set VM arguments
    // NOTE: this relies on a symlink from /rmi-chat to this project's directory
    System.setProperty("java.security.policy", "file:/rmi-chat/security.policy");
    System.setProperty("java.rmi.server.codebase", "file:/rmi-chat/bin");

    try {
      System.setSecurityManager(new RMISecurityManager());

      Server server = new Server();	

      Naming.rebind("rmi://localhost/ABC", server);

      System.out.println("[System] Server is ready:");      

    } catch (Exception e) {
      System.out.println("[System] Server failed: " + e);
      e.printStackTrace();
      StackTraceElement stackTrace = e.getStackTrace()[0];
      System.out.println("Unexpected Exception due at " + stackTrace.getLineNumber() + " in " + stackTrace.getFileName());
      System.out.println(stackTrace.toString());
    }
  }
}
