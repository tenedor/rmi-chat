import java.rmi.*;
import java.util.*;


public interface ServerInterface extends Remote {
  // client uid
  public int getClientUID() throws RemoteException;

  // create entities
  //  returns `false` if an entity already exists with the given name
  public boolean createAccount(String accountName) throws RemoteException;
  public boolean createGroup(String groupName, Set<String> memberNames)
      throws RemoteException;

  // delete entities
  //   returns `false` if no entity exists to be deleted
  public boolean deleteAccount(String accountName) throws RemoteException;
  public boolean deleteGroup(String groupName) throws RemoteException;

  // get list of entities
  public Set<String> getAccountsList() throws RemoteException;
  public Set<String> getAccountsList(String pattern) throws RemoteException;
  public Set<String> getGroupsList() throws RemoteException;
  public Set<String> getGroupsList(String pattern) throws RemoteException;

  // log in, log out
  //   returns `false` if the operation was redundant
  //   an event sequence ID enables ordering of per-client log in/out events
  public boolean logIn(int cUID, int eSID, ClientInterface client,
      String accountName) throws RemoteException;
  //public boolean logOut(int cUID, int eSID) throws RemoteException;
  public boolean logOut(int cUID, int eSID, String accountName) throws RemoteException;

  // check logged-in status
  //   returns empty string if the client is not logged in
  public String getLoginStatus(int cUID) throws RemoteException;  

  //gets a list of undelivered messages
  public boolean getUndeliveredMessages(ClientInterface client, String accountName) throws RemoteException;

  // send messages
  //   returns `false` if the server previously received this message
  //   a (client UID, event sequence ID) tuple uniquely identifies a message
  public boolean sendMessageToAccount(int cUID, int eSID, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException;
  public boolean sendMessageToGroup(int cUID, int eSID, String senderName,
      String groupName, String message, int timestamp) throws RemoteException;
}
