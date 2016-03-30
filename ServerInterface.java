import java.rmi.*;
import java.util.*;

/**
 * Chat Server Interface.
 * <p>
 * The chat system manages the following entities: {@code client},
 * {@code account}, {@code group}, {@code message}. It manages a list of
 * accounts, a list of groups and the accounts belonging to each, and a mapping
 * between accounts and clients. A client can log into an account and send
 * messages to other accounts and to groups. It receives messages from the
 * server on behalf of the account it is logged into.
 * <p>
 * When the server receives a group message, it multiplexes this message send to
 * each member of that group. When an account is not logged in, any messages
 * sent to it are saved on the server until they can be delivered. Upon logging
 * into an account, a client should request all of its undelivered messages by
 * calling {@link #getUndeliveredMessages}.
 * <p>
 * Clients may also make requests to create and delete accounts and groups and
 * to get a list of those that exist. Only one client may be logged into an
 * account at a time, and a client may only be logged into one account at a
 * time. When a client logs into an account, it is logged out of any other
 * accounts and it kicks off any other client logged into that account.
 * <p>
 * Client-server communication is implemented with Java RMI ({@link java.rmi}).
 * The {@code Server} class is a remote resource and extends RMI's
 * {@code Remote}. If a method invocation fails the client is assumed to have
 * disconnected. Each client must have a client ID (abbreviated as {@code cUID})
 * for making requests; a {@code cUID} is obtained by calling
 * {@link getClientUID}.
 * <p>
 * A client is expected to attach an event sequence ID ({@code eSID}) to
 * requests where event ordering or uniqueness are important. Similarly, the
 * server attaches an {@code eSID} for some messages it sends to the client.
 * {@code eSID}s are integers that increase monotonically for message sends and
 * are unique for a given client or server.
 * <p>
 * Error handling is basic. If a method invocation on a client fails, the client
 * is assumed to have disconnected. No further effort is made to track which
 * clients are still online. The system is fully asynchronous; in particular,
 * there is no notion of request timeouts.
 * <p>
 * {@see com.github.tenedor.rmi-chat.ClientInterface}
 * {@see com.github.tenedor.rmi-chat.Server}
 */
public interface ServerInterface extends Remote {
  /**
  * Generates the next client user ID, a unique integer representing a client. 
  * <p>
  * A {@link com.github.tenedor.rmi-chat.Client} must have a client ID
  * (abbreviated as {@code cUID}) to log in and out and send and receive
  * messages.
  *  
  * @return         the latest client user ID integer  
  */
  public int getClientUID() throws RemoteException;


  // Create Entities
  // ---------------

  /**
  * Creates an account for the given account name, returning the status of the
  * creation.
  * <p>
  * If the account name already exists in our server's list of accounts, returns
  * {@code false}. Otherwise, adds it and returns true.
  *  
  * @param  accountName the String identifying the account we wish to add
  * @return         a {@code true} boolean if the operation was carried out
  */  
  public boolean createAccount(String accountName) throws RemoteException;

  /**
  * Creates an group for the given member names.
  * <p>
  * This creates a new named group containing a certain set of usernames. This group
  * can then be used to send messages to; anyone in the group can send a message
  * to the group, which will be distributed to each member in {@code memberNames}
  *  
  * @param  groupName the String naming this group
  * @param  memberNames a set of Strings of names of each member in this group
  * @return         a {@code true} boolean if the operation was carried out
  */  
  public boolean createGroup(String groupName, Set<String> memberNames)
      throws RemoteException;


  // Delete Entities
  // ---------------

  /**
  * Deletes an account for the given account name.
  * <p>
  * If the account name doesn't exist in our server's list of accounts, returns
  * {@code false}. Otherwise, removes it and returns true.
  *  
  * @param  accountName the String identifying the account we wish to add
  * @return         a {@code true} boolean if the operation was carried out
  */ 
  public boolean deleteAccount(String accountName) throws RemoteException;

  /**
  * Deletes a group for the given group name.
  * <p>
  * If the group name doesn't exist in our server's list of accounts, returns
  * {@code false}. Otherwise, removes it and returns true.
  *  
  * @param  groupName the String identifying the group we wish to add
  * @return         a {@code true} boolean if the operation was carried out
  */ 
  public boolean deleteGroup(String groupName) throws RemoteException;


  // Get List of Entities
  // --------------------

  /**
  * Get a list of all accounts which have been explicitly created or have logged
  * in.
  * <p>
  * Accounts can be created using createAccount, or by a user logging in to the
  * system. This method returns a list of all possible accounts, to provide a
  * list of messageable users, for example.
  *
  * <p>
  * Pattern-based account fetching is not currently supported.
  *  
  * @param pattern  optional. not currently supported. in the future, this will
  *                 allow a client to get only those accounts matching a
  *                 pattern.
  * @return         a list of all accounts which we have registered or logged in
  *                 on this server
  */ 
  public Set<String> getAccountsList() throws RemoteException;

  /**
   * {@see #getAccountsList}.
   */
  public Set<String> getAccountsList(String pattern) throws RemoteException;

  /**
  * Get a list of all groups.
  *
  * <p>
  * Pattern-based group fetching is not currently supported.
  *  
  * @param pattern  optional. not currently supported. in the future, this will
  *                 allow a client to get only those groups matching a pattern.
  * @return         a list of all groups which have been created
  */ 
  public Set<String> getGroupsList() throws RemoteException;

  /**
   * {@see #getGroupsList}.
   */
  public Set<String> getGroupsList(String pattern) throws RemoteException;


  // Log In, Log Out
  // ---------------

  /**
  * Logs in a client to a specified account.
  * <p>
  * The client will be logged in as the specified account, booting any other
  * client currently logged in as that account and automatically logging out of
  * any other account this client was logged into. This request will be ignored
  * if it is received after a conflicting later request by the client
  * (determined by comparing the client's {@code eSID}), such as a log-out.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          the event sequence ID from the client
  * @param  client        the ClientInterface used to communicate to this client
  * @param  accountName   the accountName we wish to log in with
  * @return               a {@code true} boolean if the login succeeded
  */
  public boolean logIn(int cUID, int eSID, ClientInterface client,
      String accountName) throws RemoteException;

  /**
  * Logs out a client from a specified account.
  * <p>
  * This request will be ignored if it is received after a conflicting later
  * request by the client (determined by comparing the client's {@code eSID}),
  * such as a log-in.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          the event sequence ID from the client
  * @param  accountName   the accountName we wish to log in with
  * @return               a {@code true} boolean if the logout succeeded
  */
  public boolean logOut(int cUID, int eSID, String accountName) throws RemoteException;

  /**
  * Returns the name of the account a client is logged in as.
  * <p>
  * If the client is not logged in, the empty string is returned.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @return               the {@code accountName} for the given {@code cUID}, or
  *                       {@code ""} if the {@code cUID} is not logged in
  */  
  public String getLoginStatus(int cUID) throws RemoteException;  

  /**
  * Receives the messages not yet delivered to the specified account.
  * <p>
  * Upon logging in, a {@code client} should ask for undelivered messages that
  * were sent since the account was last logged in. This method returns those
  * messages.
  *
  * @param client         the client making the request
  * @param accountName    the account the client has logged into
  * @return               a {@code true} boolean if the operation succeeded
  */
  public boolean getUndeliveredMessages(ClientInterface client, String accountName) throws RemoteException;


  // Send Messages
  // -------------

  /**
  * Send a message to a specified account.
  * <p>
  * If the message recipient is logged in, the message will be sent to them
  * promptly. Else, it will be stored and sent when the recipient next logs in.
  * Duplicate message sends are avoided by examining the client's {@code eSID},
  * and in this case a {@code false} boolean is returned to signal previous
  * receipt.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          the client's event sequence ID for this message send
  * @param  senderName    the account name the message is being sent from
  * @param  recipientName the account name the message is being sent to
  * @param  message       the string we are sending
  * @param  timestamp     the client-generated timestamp when this message was
  *                       created
  * @return               a {@code true} boolean if the server has registered
  *                       this message send for the first time.
  */
  public boolean sendMessageToAccount(int cUID, int eSID, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException;

  /**
  * Send a message to all members of a group.
  * <p>
  *
  * For each recipient in the group, we check if the the requested recipient name 
  * is currently logged in, or not. If they are, then we send the message to them
  * immediately. If they are not currently logged in, then we save the message to
  * send to the recipient once they log in.
  * For each member of the group, if the member is logged in, the message will
  * be sent to them promptly. Else, it will be stored and sent when they next
  * log in. Duplicate message sends are avoided by examining the client's
  * {@code eSID}, and in this case a {@code false} boolean is returned to signal
  * previous receipt.
  *
  * <p>
  * Similar to {@link #sendMessageToAccount}.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          an event sequence ID generated by the client for this message
  * @param  senderName    the account name the message is being sent from
  * @param  groupName     the group name the message is being sent to
  * @param  message       the string we are sending
  * @param  timestamp     the client-generated timestamp when this message was created
  * @return               a boolean representing true if the message was sent, and false if it
  *                       was already sent.
  */
  public boolean sendMessageToGroup(int cUID, int eSID, String senderName,
      String groupName, String message, int timestamp) throws RemoteException;
}
