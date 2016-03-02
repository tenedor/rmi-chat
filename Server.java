import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements ServerInterface {
  //stores the last cUID used, which is incremented for each new client
  //this helps us to kick off older clients in favor of new clients with the same account 
  private cUID;

  //stores the last event sequence ID used, which is incremented for each event
  private eSID;

  //a mapping from accountName to a list of [clientUID, eventSequenceID, ClientInterface]
  //these are updated upon login/logout requests    
  private Map<String, List<Object>> loggedInUsers;

  //a mapping from clientID to user account name
  private Map<int, String> loggedInClients;

  //a mapping from accountName to sets of list of messages to send  
  private Map<String, Set<List<Object>>> messagesToSend;
  private Map<String, Set<List<Object>>> groupMessagesToSend;

  //a set of [cUID, eSID] lists to identify messages
  private Set<List<int>> messagesReceived;

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
    loggedInClients = new HashMap<int,String>();
    messagesToSend = new HashMap<String,Set<List<Object>>>();
    groupMessagesToSend = new HashMap<String,Set<List<Object>>>();
    messagesReceived = new HashSet<List<int>>();

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
      accounts.remove(accountName)
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
    return groups.keySet();
  }

  public Set<String> getGroupsList(String pattern) throws RemoteException {
    return groups.keySet();
  }

  // log in
  //   returns `false` if the operation was redundant
  //   an event sequence ID enables ordering of per-client log in/out events
  public boolean logIn(int cUID, int eSID, ClientInterface client, String accountName) throws RemoteException {
    //we are keying the loggedInUsers with the account ID    
    if(loggedInUsers.containsKey(accountName)) {
      //if the client is logged in and eSID has increased, log them in
      int previousESID = loggedInUsers.get(accountName)[1];
      if(previousESID < eSID) {        
        loggedInUsers.put(accountName, [cUID, eSID, client]);

        //also update the user account for this client
        loggedInClients.put(cUID, accountName);        
      }
      else {
        return false;
      }
    }
    //if the client is not logged in, log them in
    else {
      loggedInUsers.put(accountName, [cUID, eSID, client]);
      
      //also update the user account for this client
      loggedInClients.put(cUID, accountName);      
    }

    //check if there are undelivered messages for this client, and send them if so
    if(messagesToSend.containsKey(accountName)) {
      Set<List<Object>> messageList = messagesToSend.get(accountName);

      for(List message : messageList) {
        recipientClient.messageFromAccount(message[0], message[1], message[2], message[3], message[4]);
        messageList.remove(message);
      }

      messagesToSend.put(accountName, messageList);
    }

    //check if there are undelivered group messages for this client, and send them if so
    if(groupMessagesToSend.containsKey(accountName)) {
      Set<List<Object>> messageList = groupMessagesToSend.get(accountName);

      for(List message : messageList) {
        recipientClient.messageFromGroup(message[0], message[1], message[2], message[3], message[4], message[5]);
        messageList.remove(message);
      }

      groupMessagesToSend.put(accountName, messageList);
    }

    return true;
  }

  public boolean logOut(String accountName, int eSID) throws RemoteException {
    //if the user is logged in, and eSID has increased, log them out
    if(loggedInUsers.containsKey(accountName)) {
      int previousESID = loggedInUsers.get(accountName)[1];

      if(previousESID < eSID) {
        loggedInUsers.remove(accountName);                
        return true;
      }
      else {
        return false;
      }
    }
    else {
      return false;
    }
  }

  // check logged-in status
  //   returns empty string if the client is not logged in
  public String getLoginStatus(int cUID) throws RemoteException {
    //check if the cUID is in loggedInClients, and get accountName if so
    if(loggedInClients.containsKey(cUID)) {
      return loggedInClients.get(cUID);
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
      if(messagesReceived.contains([cUID, eSID])) {
        return false;
      }

      //lookup the client based on recipientName
      if(loggedInUsers.containsKey(recipientName)) {
        int recipientClient = loggedInUsers.get(recipientName)[2];
        recipientClient.messageFromAccount(nextEventSID(), senderName, recipientName, message, timestamp);
      }
      else {
        //otherwise, save this message to send later
        //messages to send is a set of messages keyed by accountName
        if(!messagesToSend.containsKey(recipientName)) {
          messagesToSend.put(recipientName, new HashSet<List<Object>>());
        }
        Set<List<Object>> previousMessagesToSendForAccount = messagesToSend.get(recipientName);
        previousMessagesToSendForAccount.add([nextEventSID(), senderName, recipientName, message, timestamp]);

        messagesToSend.put(recipientName, previousMessagesToSendForAccount);
      }
      
      //adds this message to the list of received messages
      messagesReceived.add([cUID, eSID]);
  }
  
  public boolean sendMessageToGroup(int cUID, int eSID, String senderName,
      String groupName, String message, int timestamp) throws RemoteException {
      //check if this message has been received
      if(messagesReceived.contains([cUID, eSID])) {
        return false;
      }

      //lookup the client based on recipientName
      //TODO: error checking here
      List<String> groupMembers = groups.get(groupName);

      for (String recipientName : groupMembers) {
        
        //lookup the client based on recipientName
        if(loggedInUsers.containsKey(recipientName)) {
          int recipientClient = loggedInUsers.get(recipientName)[2];
          recipientClient.messageFromGroup(nextEventSID(), groupName, senderName, member, message, timestamp);
        }
        else {
          //otherwise, save this message to send later
          //messages to send is a set of messages keyed by accountName
          if(!groupMessagesToSend.containsKey(recipientName)) {
            groupMessagesToSend.put(recipientName, new Set<List<Object>>);
          }
          Set<List<Object>> previousMessagesToSendForAccount = groupMessagesToSend.get(recipientName);
          previousMessagesToSendForAccount.add([nextEventSID(), groupName, senderName, member, message, timestamp]);

          groupMessagesToSend.put(recipientName, previousMessagesToSendForAccount);
        }
      }
     
      //adds this message to the list of received messages
      messagesReceived.add([cUID, eSID]);
  }
}
