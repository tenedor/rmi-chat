# Readme

*last updated 03/01/16 02:33am*

## State of the code
- All filenames beginning with "Chat" are NOT part of the codebase. These should still work as the demo chat application and are helpful reference material, but do not regard them as part of the codebase. (The code in org/ is similarly not part of the codebase.)
- AFAIK, ClientInterface.java and ServerInterface.java define correct and complete interfaces for the distributed interface. They have not been tested in any way.
- AFAIK, Client.java is a correct implementation of ClientInterface that is complete except for error handling. Again, it has not been tested in any way.
- Note 1: Strictly speaking, this means Client.java is a complete implementation. When the server crashes, Client.java should throw an exception and wait for instructions from the ClientManager that created it, and this is what it does. However, the current implementation is not very robust - a single failed communication causes it to conclude the server has crashed.
- Note 2: Client.java does not implement any local logging. This is not necessary for a complete implementation and should not be worked on until either (a) everything else is done, or (b) the implementation time it would take would be outweighed by the debugging time it would save.
- Since none of these have been tested, it seems clear that they don't actually work. However, I expect they're quite close to working. Once there is an accompanying Server.java implementation (or even just parts of one!) the code can start being tested - commenting out functionality and bringing it back later will probably be helpful for initial debugging.

## Design of the code
- There are three layers to the design on both the client side and the server side: Interfaces, Implementations, and Managers. These are respectively named XInterface.java, X.java, and XManager.java for X in {Client, Server}.
- Interfaces define RMI methods that the client and server offer each other.
- Implementations implement their own RMI interfaces, implement logic for interacting with their remote counterparts' interfaces (including exception handling), and maintain all local state. If a Client concludes the Server has crashed, it throws an exception.
- Managers implement start-up logic and connect user interactions (through command line interfaces) to Implementations. The ClientManager also makes RMI calls directly to the Server for operations that do not touch client state (i.e. account and group creation, deletion, and lookups), and it handles an exception from the Client that the Server has crashed.

## What remains to be implemented
- Server: The server is the majority of what remains. Details below.
- ClientManager: At startup, this will need to locate the server and instantiate a Client. At runtime, it will need to read input typed into terminal (e.g. "login daniel", "send neel hey what's up?", "sendgroup neels-friends hi how is everyone?"), parse it into commands and parameters, and send these through the Client.
- ServerManager: At startup, this will need to instantiate a Server and register it with the RMI Naming facility. It doesn't need to do anything at runtime, though some runtime commands might be helpful for debugging.

## The design of the Server
- Client management: The server assigns clientUIDs to anyone who asks. This can just be an incremented integer.
- Account management: The server keeps track of the accounts and groups that exist.
- Log-in management: The server maintains a set of (clientUID, eventSequenceID, ClientInterface, accountName) tuples associating currently logged-in users to clients. When a client logs in, if it was already logged in that entry is removed, and if someone else was logged in as that user they are logged out and notified. Log-in/log-out requests come with event sequence IDs so that older requests can be ignored if a newer event has already been processed. (Does RMI prevent this possible event ordering? I wasn't sure.)
- Message management: The server receives messages from clients and attempts to send them to their recipients. If a client is not logged in, it immediately stores the message. If attempts to send a message repeatedly fail, the server logs off the client and stores the message and its eSID. Sent messages are given event sequence IDs (eSID's - Client.java demonstrates an implementation of this) so that clients can detect and ignore duplicates. Similarly, the server stores all (cUID, eSID) pairs that arrive with the messages to avoid duplicating incoming messages. Group messages are multiplied into individual messages. Upon a user's logging in, any stored messages are sent to the user.
