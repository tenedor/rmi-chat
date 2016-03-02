import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class Client extends UnicastRemoteObject implements ClientInterface {

  private ServerInterface server;
  private int cUID;
  private int eSID;
  private Set<Integer> receivedServerSIDs;

  public String accountName = ""; // empty string means client is not logged in


  // General
  // -------

  public Client(ServerInterface server) throws RemoteException {
    this.server = server;

    // TODO - if there is saved data from a previous run, resume from there
    cUID = server.getClientUID();
    eSID = 0;
    receivedServerSIDs = new HashSet<Integer>();
  }

  private int nextEventSID() {
    return eSID++;
  }


  // Log-in management
  // -----------------

  public boolean logIn(String accountName) throws RemoteException {
    boolean retval = server.logIn(cUID, nextEventSID(), this, accountName);
    this.accountName = accountName;
    System.out.println("Login results: " + retval + ", " + accountName);
    return retval;
  }

  public boolean logOut() throws RemoteException {
    //boolean retval = server.logOut(cUID, nextEventSID());
    boolean retval = server.logOut(cUID, nextEventSID(), this.accountName);
    this.accountName = "";
    return retval;
  }

  public void updateLoginStatus() throws RemoteException {
    accountName = server.getLoginStatus(cUID);
  }

  public void notifyOfLogOut() throws RemoteException {
    // Is sequential ordering w/r/t client logIn/logOut calls guaranteed here?
    // If so, setting `accountName = ""` and returning is sufficient. I'm not
    // clear on RMI timing interleaving when the server and client call each
    // other concurrently.
    System.out.println("You have been logged out.");

    updateLoginStatus();
  }


  // Sending messages
  // ----------------

  public boolean sendMessageToAccount(String recipientName, String message,
      int timestamp) throws RemoteException {
    return server.sendMessageToAccount(cUID, nextEventSID(), accountName,
        recipientName, message, timestamp);
  }

  public boolean sendMessageToGroup(String groupName, String message,
      int timestamp) throws RemoteException {
    return server.sendMessageToGroup(cUID, nextEventSID(), accountName,
        groupName, message, timestamp);
  }


  // Receiving messages
  // ------------------

  public boolean messageFromAccount(int eSID, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException {
    if (receivedServerSIDs.contains(eSID)) {
      return false;

    } else {
      // if this client's logged-in account doesn't match the recipient, error
      if (!recipientName.equals(accountName)) {
        System.out.println(recipientName + ", " + accountName);
        System.out.println("Error, not to the right account");
        throw new RemoteException();
      }

      receivedServerSIDs.add(eSID);
      System.out.println("[" + senderName + " @ " + timestamp + "] " + message);
      return true;
    }
  }

  public boolean messageFromGroup(int eSID, String groupName, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException {
    if (receivedServerSIDs.contains(eSID)) {
      return false;

    } else {
      // if this client's logged-in account doesn't match the recipient, error
      if (!recipientName.equals(accountName)) {
        System.out.println("Error, not to the right account");
        throw new RemoteException();
      }

      receivedServerSIDs.add(eSID);
      System.out.println("[" + groupName + ":" + senderName + " @ " +
          timestamp + "] " + message);
      return true;
    }
  }
}
