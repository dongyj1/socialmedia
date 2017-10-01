package com.socialmedia;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;


public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;

	public static void main(String[] args) {

		// The default port.
		int portNumber = 8000;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out
			.println("Usage: java User <host> <portNumber>\n"
					+ "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
		try {
			userSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
					+ host);
		}

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
		if (userSocket != null && output_stream != null && input_stream != null) {
			try {                
				/* Create a thread to read from the server. */
				new Thread(new User()).start();

				// Get user name and join the social net
				System.out.println("Ready for starting joining the network.");
				while (!closed) {
					String userMessage = new String();
					String userInput = inputLine.readLine().trim();
					
					// Read user input and send protocol message to server
					if (userInput.startsWith("@connect ")) {
						output_stream.println("#friendme "+ getContent(userInput, "@connect "));
					} else if (userInput.startsWith("@friend ")) {
						output_stream.println("#friends "+ getContent(userInput, "@friend "));
					} else if (userInput.startsWith("@deny ")) {
						output_stream.println("#DenyFriendRequest "+ getContent(userInput, "@deny "));
					} else {
						output_stream.println(userInput);
					}
					if (userInput.equals("Exit")) {
						System.out.println("End connection.");
						break;
					}

				}
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	/*
	 * Create a thread to read from the server.
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
		String responseLine;
		
		try {
			while ((responseLine = input_stream.readLine()) != null) {
//				System.out.println(responseLine);
				// Display on console based on what protocol message we get from server.
				if (responseLine.startsWith("#welcome")) {
					System.out.println("Connection established " + getContent(responseLine,"#welcome ").trim());
				} else if (responseLine.startsWith("#busy")) {
					System.out.println("Server is busy.");
				} else if (responseLine.startsWith("#newuser")) {
					System.out.println("New user " + getContent(responseLine,"#newuser").trim() + " joined");
				} else if (responseLine.startsWith("#newStatus")) {
					System.out.println(getContent(responseLine, "#newStatus").trim());
				} else if (responseLine.startsWith("#Leave ")) {
					System.out.println("User "+getContent(responseLine, "#Leave").trim() + " has left.");
				} else if (responseLine.startsWith("#Bye")) {
					System.out.println("Bye.");
					break;
				} else if (responseLine.startsWith("#friendme ")) {
					System.out.println("Friend request from " + getContent(responseLine, "#friendme ") 
							+ ". Type @friend + username, type @deny + username to deny");
				}
			}
			closed = true;
			output_stream.close();
			input_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
	private static String getContent(String word, String tag) {
		if (word.startsWith(tag)) {
			return word.substring(tag.length(),word.length());
		}
		return word;
	}
	
}


