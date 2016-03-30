import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements ServerInterface {
  /**
  * Stores the last <code>cUID</code>. used, which is incremented for each new client.
  * This helps us to kick off older clients in favor of new clients with the same account 
  */
  private int cUID;

 /**
  * Stores the last event sequence ID used, which is incremented for each event
  */
  private int eSID;

  /**
  * A mapping from accountName to a list of [clientUID, ClientInterface] 
  */  
  private Map<String, List<Object>> loggedInUsers;


  /**
  * A mapping from clientID to user account name and eSID. 
  * these are updated upon login/logout requests    
  */   
  private Map<Integer, List<Object>> loggedInClients;
  
  /**
  * A mapping from accountName to sets of list of messages to send  
  */
  private Map<String, Set<List<Object>>> messagesToSend;
  private Map<String, Set<List<Object>>> groupMessagesToSend;

  /**
  * A mapping from cUID to set of eSIDs
  */
  private Map<Integer, Set<Integer>> messagesReceived;  
  
  /**
  * Stores a set of account names
  */
  private Set<String> accounts;

  /**
  * A mapping from group name to a set of group members
  */  
  private Map<String, Set<String>> groups;

  // General
  // -------

  /**
  * Creates a Server instance that can keep track of users, associated clients, and
  * send messages between them
  * <p>
  * In this instantiation, the Server keeps track of users and clients that are currently
  * logged in. It keeps a queue of messages to send to users that are not currently
  * logged in, as well as information to associate accounts to groups.    
  */
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

  /**
  * Generates the next event sequence ID, a unique number
  * representing each action performed by the server.
  * <p>
  * Event Sequence IDs are generated in sending a message to 
  * either an individual or a group). Using an event 
  * sequence ID allows us to ignore duplicate events received on by a client.  
  *  
  * @return         the latest event sequence ID integer  
  */
  private int nextEventSID() {
    return eSID++;
  }

  /**
  * Generates the next client user ID, a unique number representing each client. 
  * <p>
  * We can assign each new client a new client UID and incremenet cUID each time
  * to ensure that each client has a unique user ID.  
  *  
  * @return         the latest client user ID integer  
  */
  public int getClientUID() throws RemoteException {
    return cUID++;
  }

  /**
  * Creates an account for the given account name, returning the status of the creation
  * <p>
  * If the account name already exists in our server's list of accounts, returns false.
  * Otherwise, adds it and returns true  
  *  
  * @param  accountName the String identifying the account we wish to add
  * @return         a boolean representing if the account name was added successfully 
  */  
  public boolean createAccount(String accountName) throws RemoteException{
    if(accounts.contains(accountName)) {
      return false;
    }

    accounts.add(accountName);
    return true;
  };
  
  /**
  * Creates an group for the given member names
  * <p>
  * This creates a new named group containing a certain set of usernames. This group
  * can then be used to send messages to; anyone in the group can send a message
  * to the group, which will be distributed to each member in <code>memberNames</code> 
  *  
  * @param  groupName the String naming this group
  * @param  memberNames a set of Strings of names of each member in this group
  * @return         a 'true' boolean if the operation was successful
  */  
  public boolean createGroup(String groupName, Set<String> memberNames) throws RemoteException {
    groups.put(groupName, memberNames);
    return true;
  }


  /**
  * Deletes an account for the given account name, returning the status of the deletion
  * <p>
  * If the account name doesn't exist in our server's list of accounts, returns false.
  * Otherwise, removes it and returns true  
  *  
  * @param  accountName the String identifying the account we wish to add
  * @return         a boolean representing if the account name was deleted successfully 
  */ 
  public boolean deleteAccount(String accountName) throws RemoteException {
    if(accounts.contains(accountName)) {
      accounts.remove(accountName);
      return true;
    }
    return false;
  }

  /**
  * Deletes an group for the given group name, returning the status of the deletion
  * <p>
  * If the group name doesn't exist in our server's list of groups, returns false.
  * Otherwise, removes it and returns true  
  *  
  * @param  groupName the String identifying the group we wish to add
  * @return         a boolean representing if the group was deleted successfully 
  */ 
  public boolean deleteGroup(String groupName) throws RemoteException {
    if(groups.containsKey(groupName)) {
      groups.remove(groupName);
      return true;
    }
    return false;
  }
  
  /**
  * Gets a list of all accounts which have been explicitly created or have logged in.
  * <p>
  * Accounts can be created using createAccount, or by a user logging in to the system.
  * This method returns a list of all possible accounts, to provide a list of 
  * messageable users, for example.
  *  
  * @return         a list of all accounts which we have registered or logged in on this server
  */ 
  public Set<String> getAccountsList() throws RemoteException {
    return accounts;
  }
  
  public Set<String> getAccountsList(String pattern) throws RemoteException {
    return accounts;
  }

  /**
  * Gets a list of all groups which have been created
  * <p>
  * Because we cannot return a more complicated keySet, this method
  * loops through all of the group names in our record of groups, and 
  * returns a list of these names.
  *  
  * @return         a list of all group names
  */ 
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

  /**
  * Logs a given client and account name in .
  * <p>
  * This method checks for redundancy in the login request, and then
  * logs in a specified client and account name. We start by checking if the
  * same exact client user ID and account name combination are already logged in;
  * if so, there is no need to log in again. We also check that the current eSID (event ID)
  * is greater than the previoue eSID, which lets us order events for each client. This allows
  * us to process the latest command each client says.
  *
  * If this client user ID (cUID) is already logged in, then we must remove the account
  * name previously associated with this cUID. We log that account out, notify them of
  * the logout, and then log in with the currently supplied <code>accountName</code>.
  *
  * If this accountName is already logged in, then we find the client currently associated
  * with this accountName. We notify the previous client of the logout, and then log in
  * with the currently supplied  <code>cUID</code>.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          the event sequence ID from the client
  * @param  client        the ClientInterface used to communicate to this client
  * @param  accountName   the accountName we wish to log in with
  * @return               a boolean representing whether the login was successful or not  
  */
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

  /**
  * Logs a given client and account name out.
  * <p>
  * This method checks for redundancy in the logout request, and then
  * logs out a specified client and account name. 
  *
  * If the client (as identified by the client user ID) is currently logged in, then
  * we ensure that the event sequence ID for this logout action is greater than the event
  * sequence ID for the previous login action, to preserve order. We must log that client out,
  * notify them of the logout, and then remove this account name and client from
  * the lists of actively logged in users and clients. We then return 'true' to show that the
  * logout was successful. 
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          the event sequence ID from the client
  * @param  accountName   the accountName we wish to log in with
  * @return               a boolean representing whether the login was successful or not  
  */
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

  /**
  * Checks the login status of a given client user ID
  * <p>
  * This method checks if a client with a given client user ID is logged in,
  * and returns the corresponding accountName if it is. Otherwise returns the empty 
  * string.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @return               the account name for the given cUID, or "" if the cUID is not logged in
  */  
  public String getLoginStatus(int cUID) throws RemoteException {
    //check if the cUID is in loggedInClients, and get accountName if so
    if(loggedInClients.containsKey(cUID)) {
      return (String)loggedInClients.get(cUID).get(0);
    }
    else {
      return "";
    }
  }

  /**
  * Sends a message to a given individual account
  * <p>
  * This method checks to ensure that a specific (numbered) message hasn't already 
  * been sent to a certain account, and then sends the message if allowed.
  *
  * If a message with this specific event sequence ID has already been received from this
  * specific client ID, we don't send the message, and return false instead.
  *
  * If the requested recipient name is currently logged in, then we send the message to them
  * immediately. If they are not currently logged in, then we save the message to
  * send to the recipient once they log in.
  *
  * Finally, we store the fact that we have received the message with this eSID 
  * from the given client user ID.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          an event sequence ID generated by the client for this message
  * @param  senderName    the account name the message is being sent from
  * @param  recipientName the account name the message is being sent to
  * @param  message       the string we are sending
  * @param  timestamp     the the client-generated timestamp when this message was created
  * @return               a boolean representing true if the message was sent, and false if it
  *                       was already sent.
  */
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
  
    /**
  * Sends a message to a group
  * <p>
  * This method checks to ensure that a specific (numbered) message hasn't already 
  * been sent to a certain group, and then sends the message if allowed.
  *
  * If a message with this specific event sequence ID has already been received from this
  * specific client ID, we don't send the message, and return false instead.
  *
  * For each recipient in the group, we check if the the requested recipient name 
  * is currently logged in, or not. If they are, then we send the message to them
  * immediately. If they are not currently logged in, then we save the message to
  * send to the recipient once they log in.
  *
  * Finally, we store the fact that we have received the message with this eSID 
  * from the given client user ID.
  *
  * @param  cUID          an integer identifying the current client user ID
  * @param  eSID          an event sequence ID generated by the client for this message
  * @param  senderName    the account name the message is being sent from
  * @param  groupName     the account name the message is being sent to
  * @param  message       the string we are sending
  * @param  timestamp     the the client-generated timestamp when this message was created
  * @return               a boolean representing true if the message was sent, and false if it
  *                       was already sent.
  */
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
