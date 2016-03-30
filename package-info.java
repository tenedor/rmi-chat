/**
 * Provides the interfaces and implementations for a client-server distributed
 * chat system.<p>
 *
 * A {@link com.github.tenedor.rmi-chat.Server} manages a list of accounts, a
 * list of groups and the accounts belonging to each, and a mapping between
 * accounts and clients. A {@link com.github.tenedor.rmi-chat.Client} can log
 * into an account and send messages to other accounts and to groups. A
 * {@link com.github.tenedor.rmi-chat.Server} will send received messages to a
 * {@link com.github.tenedor.rmi-chat.Client} logged into the recipient account
 * if it exists and will store unsendable messages until a
 * {@link com.github.tenedor.rmi-chat.Client} logs into that account.<p>
 *
 * There are three layers to the design on both the client side and the server
 * side: Interfaces, Implementations, and Managers. These are respectively named
 * {@code XInterface.java}, {@code X.java}, and {@code XManager.java} for X in
 * {{@code Client}, {@code Server}}.
 * <ul>
 *   <li>{@code Interfaces} define RMI methods that the client and server offer
 *       each other.
 *   <li>{@code Implementations} implement their own RMI interfaces, implement
 *       logic for interacting with their remote counterparts' interfaces
 *       (including exception handling), and maintain all local state. If a
 *       {@code Client} concludes the {@code Server} has crashed, it throws an
 *       exception.
 *   <li>Managers implement start-up logic and connect user interactions
 *       (through command line interfaces) to {@code Implementations}. The
 *       {@code ClientManager} also makes RMI calls directly to the
 *       {@code Server} for operations that do not touch client state (i.e.
 *       account and group creation, deletion, and lookups), and it handles an
 *       exception from the {@code Client} that the {@code Server} has crashed.
 *</ul>
 *<p>
 *
 * Not yet implemented:
 * <ul>
 *   <li>Exception handling
 *   <li>Wildcard matching for listing account and group subsets
 * </ul>
 * <p>
 *
 * Packages:<p>
 *
 * This system makes extensive use of {@link java.rmi}. Java Remote Method
 * Invocation is Java's remote procedure call utility. The 
 * {@link com.github.tenedor.rmi-chat.ServerInterface} and
 * {@link com.github.tenedor.rmi-chat.ClientInterface} (and therefore the
 * {@link com.github.tenedor.rmi-chat.Server} and
 * {@link com.github.tenedor.rmi-chat.Client} that implement them) extend
 * {@link java.rmi.Remote} and are remote methods that may be called similarly
 * to local methods. Unlike local methods they incur network delays and may
 * throw {@code RemoteException}. The current code does little to handle
 * these exceptions.<p>
 *
 * How to run the system:<p>
 *
 * <ul>
 *   <li>Ensure that the rmiregistry is running on your machine by running
 *       {@code rmiregistry}
 *   <li>Run the Server by executing {@code java ServerManager}
 *   <li>Run each of the Client machines by running {@code java ClientManager}
 *   <li>From a client machine, type the following commands:
 *     <ul>
 *     <li> {@code create_account <account name>}
 *     <li> {@code login <account name>}
 *     <li> {@code create_group <group name> <<list of group members>>}
 *     <li> {@code message <account name> <message>}
 *     <li> {@code message_group <group name> <message>}
 *     <li> {@code list_groups}
 *     <li> {@code list_accounts}
 *     </ul>
 * </ul>
 */
package com.github.tenedor.rmi-chat;

