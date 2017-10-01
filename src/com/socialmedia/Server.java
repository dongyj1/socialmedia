package com.socialmedia;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;



/*
 * A server that delivers status messages to other users.
 */
public class Server {

	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static UserThread[] threads = null;
	// Count of active users
	private static Hashtable<String, HashSet<String>> userFriendList = null;

	public static void main(String args[]) {

		// The default port number.
		int portNumber = 8000;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		
		UserThread[] threads = new UserThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
			userFriendList = new Hashtable<>();
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new UserThread(userSocket, threads, userFriendList);
						threads[i].start();
						System.out.println("new thread " + i);
						break;
					} 
				}
				if (i == maxUsersCount) {
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class UserThread extends Thread {

	private String userName = null;
	public String getUserName() {
		return userName;
	}

	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final UserThread[] threads;
	private int maxUsersCount;
	private Hashtable<String, HashSet<String>> userFriendList;
	

	public Hashtable<String, HashSet<String>> getUserFriendList() {
		return userFriendList;
	}
	public void setUserFriendList(Hashtable<String, HashSet<String>> userFriendList) {
		this.userFriendList = userFriendList;
	}
	public UserThread(Socket userSocket, UserThread[] threads, Hashtable<String, HashSet<String>> userFriendList) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
		this.userFriendList = userFriendList;
	}
	public synchronized void printToOutStream(String words) {
		System.out.println("Server print "+ this.userName + " " + words);
		this.output_stream.println(words);
	}
	public void run() {
		int maxUsersCount = this.maxUsersCount;
		UserThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));;
			output_stream = new PrintStream(userSocket.getOutputStream());
			
			
			output_stream.println("Please input your name.");
			

			/* Welcome the new user. */
			
			/* Start the conversation. */
			while (true) {
				String input = input_stream.readLine().trim();
				System.out.println("input: " + input);
//				System.out.println(input.startsWith("#friendme "));
				if (input.startsWith("Exit")) {
					for (int i = 0; i < threads.length; i++) {
						UserThread userThread = threads[i];
						if (userThread != null && userThread != this) {
							String word = "#Leave "+this.userName;
							userThread.printToOutStream(word);
						}
					}
					break;
				} else if (userName == null && input.startsWith("#join ")) {
					userName = getContent(input, "#join ").trim();
					output_stream.println("#welcome "+userName);
					
					userFriendList.put(userName, new HashSet<String>());
					
					if (userName.equals("u2")) {
						System.out.println(userFriendList.get(userName) == null);
					}
					for (int i = 0; i < threads.length; i++) {
						UserThread userThread = threads[i];
						if (userThread != null && userThread != this) {
							String word = "#newuser "+this.userName;
							userThread.printToOutStream(word);
						}
					}
				} else if (input.startsWith("#status ")) {
					for (int i = 0; i < threads.length; i++) {
						UserThread userThread = threads[i];
						if (userThread != null && userName != null && userThread.getUserName() != null
								&& userFriendList.get(userName).contains(userThread.getUserName())) {
							String word = "#newStatus " + userName + " " + getContent(input, "#status").trim();
							System.out.println(word);
							userThread.printToOutStream(word);
						} else if (userThread == this){
							System.out.println("On Thread "+ i);
							System.out.println(getContent(input, "#status").trim());
							this.output_stream.println("#statusPosted " + getContent(input, "#status"));
						}
					}
				} else if (input.startsWith("#friendme ")) {
						String targetUsername = getContent(input, "#friendme").trim();
//						System.out.println("friend me " + targetUsername);
						if (userFriendList.containsKey(targetUsername) 
								&& userFriendList.get(targetUsername).contains(userName) 
								&& userFriendList.get(userName).contains(targetUsername)) {
							this.output_stream.println("You already have this friend.");
						} else {
							for (int i = 0; i < threads.length; i++) {
								UserThread userThread = threads[i];
								if (userThread != null && userThread.getUserName().equals(targetUsername)) {
									String word = "#friendme " + userName;
//									System.out.println(word);
									userThread.printToOutStream(word);
								}
							}
						}
					} else if (input.startsWith("#friends ")) {
						// add friend on both sides
						String newFriend = getContent(input, "#friendme").trim();
						System.out.println(newFriend + " " + userName + " " + userFriendList.get(newFriend)==null);
						synchronized (userFriendList) {
							try {
								System.out.println(userFriendList.get(userName) == null);
								HashSet<String> set1 = userFriendList.get(userName);
								set1.add(newFriend);
								userFriendList.put(userName, new HashSet<String>(set1));
								
								System.out.println(userFriendList.get(newFriend) == null);
								HashSet<String> set2 = userFriendList.get(newFriend);
								set2.add(userName);
								userFriendList.put(newFriend, new HashSet<String>(set2));
								
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						output_stream.println("#OKfriends " + userName + " " + newFriend);
						for (int i = 0; i < threads.length; i++) {
							UserThread userThread = threads[i];
							if (userThread != null && userThread.getUserName() == newFriend) {
								String word = "#OKfriends " + userName + " " + newFriend;
								userThread.printToOutStream(word);
							}
						}
					} else if (input.startsWith("#DenyFriendRequest ")) {
						String requester = getContent(input, "#DenyFriendRequest ").trim();
						for (int i = 0; i < threads.length; i++) {
							UserThread userThread = threads[i];
							if (userThread != null && userThread.getUserName() == requester) {
								String word = " #FriendRequestDenied " + userName;
								userThread.printToOutStream(word);
							}
						}
					}
			}

			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (UserThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
		}
	}
	
	private synchronized int getActiveUserNum(UserThread[] threads) {
		int count = 0;
		for (int i = 0; i < threads.length; i++) {
			if (threads[i] != null) {
				count++;
			}
		}
		return count;
	}
	
	private static String getContent(String word, String tag) {
		if (word.startsWith(tag)) {
			return word.substring(tag.length(),word.length());
		}
		return word;
	}
}



