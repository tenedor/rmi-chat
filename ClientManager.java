import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class ClientManager {  
  public static void main (String[] argv) {
    // NOTE: this relies on a symlink from /rmi-chat to this project's directory
    System.setProperty("java.security.policy", "file:/rmi-chat/security.policy");
    System.setProperty("java.rmi.server.codebase", "file:/rmi-chat");

    try {
      System.setSecurityManager(new RMISecurityManager());      

      ServerInterface server = (ServerInterface) Naming.lookup("rmi://localhost/ABC");
      Client client = new Client(server);

      //logs in with an initial name
      Scanner s = new Scanner(System.in);      

      //uses a loop to react to each message
      //possible message configurations are:
      //  login <user_name>, logout, <user_name>
      //  message <user_name> <message>
      //  create_group <group_name> <group_member_1> <group_member_2>...
      //  message_group <group_name> <message>
      while (true) {
        String inputString = s.nextLine().trim();                  
        
        if(inputString.contains(" ")) {
          //splits the string into a prefix followed by a space
          String[] splitInput = inputString.split(" ", 2);
          String prefix = splitInput[0];      
          String suffix = splitInput[1];  

          if(prefix.equals("login")) {
            try {
              client.logIn(suffix);
              server.getUndeliveredMessages(client, client.accountName);
            }
            catch(RemoteException e) {
              System.out.println("Error logging in - have you created an account for " + suffix + " yet?");
            }
          }
          else if(prefix.equals("logout")) {
            client.logOut();
          }
          else if(prefix.equals("create_account")) {
            try {              
              server.createAccount(suffix);
            }
            catch(RemoteException e) {
              System.out.println("Error creating an account for " + suffix + ".");
            }
          }
          else if(prefix.equals("delete_account")) {
            try {

              server.deleteAccount(suffix);
            }
            catch(RemoteException e) {
              System.out.println("Error deleting an account for " + suffix + ".");
            }
          }
          else if(prefix.equals("create_group")) {
            try {
              //splits the suffix into a group name followed by list of names
              String[] splitCommand = suffix.split(" ", 2);
              String groupName = splitCommand[0];      
              String[] namesInGroup = splitCommand[1].split(" ");
              Set<String> namesInGroupSet = new HashSet<String>(Arrays.asList(namesInGroup)); 

              server.createGroup(groupName, namesInGroupSet);
            }
            catch(RemoteException e) {
              System.out.println("Error creating group, sorry!");
            }
          }
          else if(prefix.equals("message_group")) {
            try {
              String[] splitCommand = suffix.split(" ", 2);
              String groupName = splitCommand[0];  
              String message = splitCommand[1];

              client.sendMessageToGroup(groupName, message, 0);
            }
            catch(RemoteException e) {
              System.out.println("Error messaging group, sorry!");
            }
          }
          else if(prefix.equals("message")) {
            try {
              String[] splitCommand = suffix.split(" ", 2);
              String recipientName = splitCommand[0];  
              String message = splitCommand[1];

              client.sendMessageToAccount(recipientName, message, 0);
            }
            catch(RemoteException e) {
              System.out.println("Error sending message, sorry!");
            }
          }
          else {
            System.out.println("Unrecognized command, please try again.");
          }
        }
        else if(inputString.equals("list_groups")) {
          System.out.println(server.getGroupsList());
        }
        else if(inputString.equals("list_accounts")) {
          System.out.println(server.getAccountsList());
        }
        else {
          System.out.println("Unrecognized command, please try again.");
        }

      }
      

    } catch (Exception e) {
      System.out.println("[System] Client failed: " + e);
      e.printStackTrace();
      StackTraceElement stackTrace = e.getStackTrace()[0];
      System.out.println("Unexpected Exception due at " + stackTrace.getLineNumber() + " in " + stackTrace.getFileName());
      System.out.println(stackTrace.toString());
    }
  }
}
