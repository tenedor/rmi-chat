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

 /**
  * Creates a Client instance that connects to a given <code>server</code>.
  * <p>
  * This instantiation stores the given server to allow future
  * communications (both sending and receiving messages are done by
  * interfacing with this server). The instantiation also performs 
  * variable initializations. The client is assigned a client user ID
  * by the server (such that two clients won't have the same UID).
  * The client also begins keeping track of the latest event sequence ID
  * and the received sequence IDs from the server.
  *
  * @param  server  an instantiated instance of a server (from ServerInterface)
  * @see            Server
  */
  public Client(ServerInterface server) throws RemoteException {
    this.server = server;

    // TODO - if there is saved data from a previous run, resume from there
    cUID = server.getClientUID();
    eSID = 0;
    receivedServerSIDs = new HashSet<Integer>();
  }

 /**
  * Generates the next event sequence ID, a client-specific unique number
  * representing each action performed with the server.
  * <p>
  * Event Sequence IDs are generated in logging in, logging out, and
  * sending a message (to either an individual or a group). Using an event 
  * sequence ID allows us to ignore duplicate events received on a server,
  * preventing duplicate actions (which could happen if the action completed,
  * but the client never received confirmation, for example).
  *
  * @param  server  an instantiated instance of a server (from ServerInterface)
  * @return         the latest event sequence ID integer  
  */
  private int nextEventSID() {
    return eSID++;
  }


  // Log-in management
  // -----------------

  /**
  * Logs a given account name in for this client.
  * <p>
  * This method sends a request to the server including our current client UID, 
  * a unique event sequence ID for this login request, and the requested account name.
  * This method also sets our client's current account name as the given <code>accountName</code>.
  * The server returns a boolean value representing whether the login request was 
  * successful or not.
  *
  * @param  accountName the String identifying the account we wish to log in as
  * @return         a boolean representing whether the login was successful or not  
  */
  public boolean logIn(String accountName) throws RemoteException {
    boolean retval = server.logIn(cUID, nextEventSID(), this, accountName);
    this.accountName = accountName;
    System.out.println("Login results: " + retval + ", " + accountName);
    return retval;
  }

  /**
  * Logs the current account name out for this client.
  * <p>
  * This method sends a request to the server including our current client UID, 
  * a unique event sequence ID for this login request, and our client's account name.
  * The server returns a boolean value representing whether the logout request was 
  * successful or not.
  *
  * @return         a boolean representing whether the logout was successful or not  
  */
  public boolean logOut() throws RemoteException {
    //boolean retval = server.logOut(cUID, nextEventSID());
    boolean retval = server.logOut(cUID, nextEventSID(), this.accountName);
    this.accountName = "";
    return retval;
  }

  /**
  * Updates the login status for our given client user ID and updates our account 
  * name accordingly.
  * <p>
  * This method sends a request to the server with our current client UID, 
  * and the server responds with either an empty string (in the case of no logged in account)
  * or a valid account name.
  */
  public void updateLoginStatus() throws RemoteException {
    accountName = server.getLoginStatus(cUID);
  }

  /**
  * Notifies the client that they have been logged out, and updates the login status
  * accordingly.
  * <p>
  * This method can be called on a client to print information about the logout to the 
  * screen, and then run logic for updating the login status after.
  *
  * @see updateLoginStatus()
  */
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

  /**
  * Receive a message from another account.
  * <p>
  * Duplicate message receipts are avoided by examining the server's
  * {@code eSID}, and in this case a {@code false} boolean is returned to signal
  * previous receipt.
  *
  * @param  eSID          the server's event sequence ID for this message send
  * @param  senderName    the account name the message was sent from
  * @param  recipientName the account name the message was sent to
  * @param  message       the message that has been sent
  * @param  timestamp     the sender-generated timestamp when this message was
  *                       created
  * @return               a {@code true} boolean if the client has received this
  *                       message send for the first time.
  */
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

  /**
  * Receive a group message from another account.
  * <p>
  * Duplicate message receipts are avoided by examining the server's
  * {@code eSID}, and in this case a {@code false} boolean is returned to signal
  * previous receipt.
  *
  * @param  eSID          the server's event sequence ID for this message send
  * @param  groupName     the group name the message was sent from
  * @param  senderName    the account name the message was sent from
  * @param  recipientName the account name the message was sent to
  * @param  message       the message that has been sent
  * @param  timestamp     the sender-generated timestamp when this message was
  *                       created
  * @return               a {@code true} boolean if the client has received this
  *                       message send for the first time.
  */
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
