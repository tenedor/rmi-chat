import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements ServerInterface {
  //stores the last cUID used, which is incremented for each new client
  //this helps us to kick off older clients in favor of new clients with the same account 
  private int cUID;

  //stores the last event sequence ID used, which is incremented for each event
  private int eSID;

  //a mapping from accountName to a list of [clientUID, ClientInterface] 
  private Map<String, List<Object>> loggedInUsers;

  //a mapping from clientID to user account name and eSID
  //these are updated upon login/logout requests    
  private Map<Integer, List<Object>> loggedInClients;

  //a mapping from accountName to sets of list of messages to send  
  private Map<String, Set<List<Object>>> messagesToSend;
  private Map<String, Set<List<Object>>> groupMessagesToSend;

  //a set of [cUID, eSID] lists to identify messages
  //map from cUID to set of eSIDs
  private Map<Integer, Set<Integer>> messagesReceived;  

  //stores a set of account names
  private Set<String> accounts;

  //a mapping from group name to a set of group members
  private Map<String, Set<String>> groups;

  // General
  // -------

  public Server() throws RemoteException {    
    cUID = 0;
    eSID = 0;
       
    loggedInUsers = new HashMap<String,List<Object>>();
    loggedInClients =  new HashMap<Integer,List<Object>>();
    messagesToSend = new HashMap<String,Set<List<Object>>>();
    groupMessagesToSend = new HashMap<String,Set<List<Object>>>();
    messagesReceived = new HashMap<Integer,Set<Integer>>();

    accounts = new HashSet<String>();
    groups = new HashMap<String,Set<String>>();
  }

  private int nextEventSID() {
    return eSID++;
  }

  public int getClientUID() throws RemoteException {
    return cUID++;
  }

  // create entities
  //  returns `false` if an entity already exists with the given name
  public boolean createAccount(String accountName) throws RemoteException{
    if(accounts.contains(accountName)) {
      return false;
    }

    accounts.add(accountName);
    return true;
  };
  
  //creates the group
  //TODO: check if group already exists
  public boolean createGroup(String groupName, Set<String> memberNames) throws RemoteException {
    groups.put(groupName, memberNames);
    return true;
  }

  // delete entities
  //   returns `false` if no entity exists to be deleted
  public boolean deleteAccount(String accountName) throws RemoteException {
    if(accounts.contains(accountName)) {
      accounts.remove(accountName);
      return true;
    }
    return false;
  }

  public boolean deleteGroup(String groupName) throws RemoteException {
    if(groups.containsKey(groupName)) {
      groups.remove(groupName);
      return true;
    }
    return false;
  }

  // get list of entities
  public Set<String> getAccountsList() throws RemoteException {
    return accounts;
  }
  
  public Set<String> getAccountsList(String pattern) throws RemoteException {
    return accounts;
  }

  //gets a list of groups
  public Set<String> getGroupsList() throws RemoteException {
    Set<String> keys = groups.keySet();
    Set<String> cleanedKeys = new HashSet<String>();
        for(String key: keys){
          cleanedKeys.add(key);            
        }

    return cleanedKeys;
  }

  public Set<String> getGroupsList(String pattern) throws RemoteException {
    return groups.keySet();
  }

  // log in
  //   returns `false` if the operation was redundant
  //   an event sequence ID enables ordering of per-client log in/out events
  public boolean logIn(int cUID, int eSID, ClientInterface client, String accountName) throws RemoteException {        
    if(!accounts.contains(accountName)) {
      throw new RemoteException();
    }

    //if we are logged in with the same cUID and accountName as before, this is
    //redundant, so we can return false immediately
    if(loggedInClients.containsKey(cUID) && loggedInUsers.containsKey(accountName)) {
      String previousAccountName = (String)loggedInClients.get(cUID).get(0);
      int previousESID = (int)loggedInClients.get(cUID).get(1);
      int previousCUID = (int)loggedInUsers.get(accountName).get(0);

      if(previousAccountName.equals(accountName) && previousCUID==cUID && previousESID >= eSID) {
        return false;
      }
    }

    if(loggedInClients.containsKey(cUID)) {
      //if the cUID is logged in, get the previous account name and eSID
      String previousAccountName = (String)loggedInClients.get(cUID).get(0);      
      ClientInterface previousClientForCUID = (ClientInterface)loggedInUsers.get(previousAccountName).get(1);
      
      previousClientForCUID.notifyOfLogOut();      
    }

    //we are keying the loggedInUsers with the account ID    
    if(loggedInUsers.containsKey(accountName)) {
      //if the account is logged in, get the previous cUID and client
      int previousCUID = (int)loggedInUsers.get(accountName).get(0);
      ClientInterface previousClientForAccount = (ClientInterface)loggedInUsers.get(accountName).get(1); 

      previousClientForAccount.notifyOfLogOut();                           
    }    

    //if the client is not logged in, log them in
    List<Object> accountInfo = Arrays.asList(cUID, client);
    loggedInUsers.put(accountName, accountInfo);
      
    //also update the user account for this client
    List<Object> clientInfo = Arrays.asList(accountName, eSID);
    loggedInClients.put(cUID, clientInfo);      

    accounts.add(accountName);

    return true;
  }

  public boolean getUndeliveredMessages(ClientInterface client, String accountName) throws RemoteException{
    //check if there are undelivered messages for this client, and send them if so
    if(messagesToSend.containsKey(accountName)) {
      Set<List<Object>> messageList = messagesToSend.get(accountName);

      for(List message : messageList) {        
        client.messageFromAccount((int)message.get(0), (String)message.get(1), (String)message.get(2), (String)message.get(3), (int)message.get(4));
        messageList.remove(message);
      }

      messagesToSend.put(accountName, messageList);
    }

    //check if there are undelivered group messages for this client, and send them if so
    if(groupMessagesToSend.containsKey(accountName)) {
      Set<List<Object>> messageList = groupMessagesToSend.get(accountName);

      for(List message : messageList) {
        client.messageFromGroup((int)message.get(0), (String)message.get(1), (String)message.get(2), (String)message.get(3), (String)message.get(4), (int)message.get(5));
        messageList.remove(message);
      }

      groupMessagesToSend.put(accountName, messageList);
    } 

    return true;
  }

  //return True if the logOut is successful
  public boolean logOut(int cUID, int eSID, String accountName) throws RemoteException {
    //if the user is logged in, and eSID has increased, log them out
    if(loggedInClients.containsKey(cUID)) {
      String previousAccountName = (String)loggedInClients.get(cUID).get(0);      
      int previousESID = (int)loggedInClients.get(cUID).get(1);        

      if(!previousAccountName.equals(accountName) || eSID <= previousESID) {
        return false;
      }

      ClientInterface previousClient = (ClientInterface)loggedInUsers.get(accountName).get(1);   
      previousClient.notifyOfLogOut();

      //remove the account from logged in users
      loggedInUsers.remove(accountName);

      //remove the client id from logged in clients
      loggedInClients.remove(cUID);

      //remove the client from accounts
      accounts.remove(accountName);
    }
    else {
      return false;
    }

    return true;
  }

  // check logged-in status
  //   returns empty string if the client is not logged in
  public String getLoginStatus(int cUID) throws RemoteException {
    //check if the cUID is in loggedInClients, and get accountName if so
    if(loggedInClients.containsKey(cUID)) {
      return (String)loggedInClients.get(cUID).get(0);
    }
    else {
      return "";
    }
  }

  // send messages
  //   returns `false` if the server previously received this message
  //   a (client UID, event sequence ID) tuple uniquely identifies a message
  public boolean sendMessageToAccount(int cUID, int eSID, String senderName,
      String recipientName, String message, int timestamp)
      throws RemoteException {
      //check if this message has been received
      if(messagesReceived.containsKey(cUID)) {
        if(messagesReceived.get(cUID).contains(eSID)) {
          return false;
        }        
      }

      //lookup the client based on recipientName
      if(loggedInUsers.containsKey(recipientName)) {
        ClientInterface recipientClient = (ClientInterface)loggedInUsers.get(recipientName).get(1);
        recipientClient.messageFromAccount(nextEventSID(), senderName, recipientName, message, timestamp);
      }
      else {
        //otherwise, save this message to send later
        //messages to send is a set of messages keyed by accountName
        if(!messagesToSend.containsKey(recipientName)) {
          messagesToSend.put(recipientName, new HashSet<List<Object>>());
        }
        Set<List<Object>> previousMessagesToSendForAccount = messagesToSend.get(recipientName);
        List<Object> newMessage = Arrays.asList(nextEventSID(), senderName, recipientName, message, timestamp);    
        previousMessagesToSendForAccount.add(newMessage);

        messagesToSend.put(recipientName, previousMessagesToSendForAccount);
      }
      
      //adds this message to the list of received messages
      if(!messagesReceived.containsKey(cUID)) {
        messagesReceived.put(cUID, new HashSet<Integer>());
      }
      Set<Integer> previousESIDs = messagesReceived.get(cUID);
      previousESIDs.add(eSID);
      messagesReceived.put(cUID, previousESIDs);

      return true;
  }
  
  public boolean sendMessageToGroup(int cUID, int eSID, String senderName,
      String groupName, String message, int timestamp) throws RemoteException {
      //check if this message has been received
      if(messagesReceived.containsKey(cUID)) {
        if(messagesReceived.get(cUID).contains(eSID)) {
          return false;
        }        
      }

      //lookup the client based on recipientName
      //TODO: error checking here
      Set<String> groupMembers = groups.get(groupName);

      for (String recipientName : groupMembers) {
        //check that users don't group message themselves
        if(!recipientName.equals(senderName)) {
          //lookup the client based on recipientName
          if(loggedInUsers.containsKey(recipientName)) {
            ClientInterface recipientClient = (ClientInterface)loggedInUsers.get(recipientName).get(1);
            recipientClient.messageFromGroup(nextEventSID(), groupName, senderName, recipientName, message, timestamp);
          }
          else {
            //otherwise, save this message to send later
            //messages to send is a set of messages keyed by accountName
            if(!groupMessagesToSend.containsKey(recipientName)) {
              groupMessagesToSend.put(recipientName, new HashSet<List<Object>>());
            }
            Set<List<Object>> previousMessagesToSendForAccount = groupMessagesToSend.get(recipientName);
            List<Object> newMessage = Arrays.asList(nextEventSID(), groupName, senderName, recipientName, message, timestamp);
            previousMessagesToSendForAccount.add(newMessage);

            groupMessagesToSend.put(recipientName, previousMessagesToSendForAccount);
          }
        }
      }
     
      //adds this message to the list of received messages
      if(!messagesReceived.containsKey(cUID)) {
        messagesReceived.put(cUID, new HashSet<Integer>());
      }
      Set<Integer> previousESIDs = messagesReceived.get(cUID);
      previousESIDs.add(eSID);
      messagesReceived.put(cUID, previousESIDs);

      return true;
  }
}
