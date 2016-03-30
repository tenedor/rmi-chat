import java.rmi.*;

/**
 * Chat Client Interface.
 * <p>
 * A {@code client} provides a short remote interface: it receives messages from
 * an {@code account} or {@code group} and can be notified that it was logged
 * out. It is intended for use by a chat server.
 * <p>
 * As in the {@link com.github.tenedor.rmi-chat.ServerInterface}, event sequence
 * IDs ({@code eSID}s) are attached to requests where event ordering or
 * uniqueness are important. {@code eSID}s are integers that increase
 * monotonically for message sends and are unique for a given client or server.
 * <p>
 * {@see com.github.tenedor.rmi-chat.ServerInterface}
 * {@see com.github.tenedor.rmi-chat.Client}
 */
public interface ClientInterface extends Remote {

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
      throws RemoteException;

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
      throws RemoteException;


  // Log-in management
  // -----------------

  /**
  * Notify this client that it has been logged out.
  * <p>
  * This method should be called on a client that is remotely logged out.
  */
  public void notifyOfLogOut() throws RemoteException;

}
