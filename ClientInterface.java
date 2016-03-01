import java.rmi.*;

public interface ClientInterface extends Remote {
  // receive messages
  //   returns `false` if the client previously received this message
  //   a server event sequence ID uniquely identifies a message
  public boolean messageFromAccount(int eSID, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException;
  public boolean messageFromGroup(int eSID, String groupName, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException;

  // notify client that it was logged out
  public void notifyOfLogOut() throws RemoteException;
}
