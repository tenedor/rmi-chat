# Readme

*last updated 03/01/16 02:33am*

## Design of the code
- There are three layers to the design on both the client side and the server side: Interfaces, Implementations, and Managers. These are respectively named XInterface.java, X.java, and XManager.java for X in {Client, Server}.
- Interfaces define RMI methods that the client and server offer each other.
- Implementations implement their own RMI interfaces, implement logic for interacting with their remote counterparts' interfaces (including exception handling), and maintain all local state. If a Client concludes the Server has crashed, it throws an exception.
- Managers implement start-up logic and connect user interactions (through command line interfaces) to Implementations. The ClientManager also makes RMI calls directly to the Server for operations that do not touch client state (i.e. account and group creation, deletion, and lookups), and it handles an exception from the Client that the Server has crashed.

### Design of the Server
- Client management: The server assigns clientUIDs to anyone who asks. This can just be an incremented integer.
- Account management: The server keeps track of the accounts and groups that exist.
- Log-in management: The server maintains a set of (clientUID, eventSequenceID, accountName) tuples and a set of (accountName, clientUID, ClientInterface) tuples associating currently logged-in users to clients. When a client logs in, if it was already logged in that entry is removed, and if someone else was logged in as that user they are logged out and notified. Log-in/log-out requests come with per-client event sequence IDs so that older requests can be ignored if a newer event has already been processed.
- Message management: The server receives messages from clients and attempts to send them to their recipients. If a client is not logged in, it immediately stores the message. If attempts to send a message repeatedly fail, the server logs off the client and stores the message and its eSID. Sent messages are given event sequence IDs (eSIDs) so that clients can detect and ignore duplicates. Similarly, the server stores all (cUID, eSID) pairs that arrive with the messages to avoid duplicating incoming messages. Group messages are multiplied into individual messages. Upon a user's logging in, any stored messages are sent to the user.

## What remains to be implemented
- Exception handling.
- Wildcard matching for listing account and group subsets.
