/**
*  Group Chat Server
*  Ames Spring 2014
*  Developed on my PC first, then moved to cscilab.bc.edu ameswi cs344s14
*  
*  Protocol: first line received from client is the group name, unencrypted.
*  Subsequent lines received from client get sent by server to everyone else who connected with the identical group name.
*  Messages must be UTF-8 strings, assumed to be hex64 encoding of encrypted message.
*  All lines must be "\n" terminated.
*  
*  Security problems:
*  A listener could spoof the group name, and repeat message sent by others.
*  Not protected from traffic analysis.
*/

// NEXT TIME:  tweet the log? so students can see it.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GroupChatServer {
	
	public static ConcurrentHashMap<String, User> users;
	
	
	private static final int SERVER_SOCKET = 10002;
	public static void main(String args[]) {
		
		System.out.println("Server starting up");
		users = new ConcurrentHashMap<String, User>(); //initialize the table of users
		
		while (true ) { // this loop is for attempting to recover from errors and automatically restart
			ServerSocket ss = null;
			try{
				ss = new ServerSocket(SERVER_SOCKET);   //start server socket
			} catch (IOException e) {
				System.out.println("Couldn't open server socket, will try again in a minute.");
				System.out.println(e);
				try { Thread.sleep(60*1000); } catch (InterruptedException e2) { /* ignore, shouldn't happen */ }
				continue;
			}
			while(true) {
				Socket socket = null;
				try {
					socket = ss.accept();   //wait for phone to connect
				} catch (IOException e) {
					System.out.println("Failed to accept a connection, will attempt server restart in a minute.");
					try { ss.close(); } catch (IOException e1) { /* tried */ }
					try { Thread.sleep(60*1000); } catch (InterruptedException e2) { /* ignore */ }
					break; // Try getting the socket again, attempt to recover
				}
				System.out.println("Got new client");
				
				(new Listener(socket)).start();   //start thread with phone
			}
		}
	}
}

// Each instance of this class listens to ONE client, sends to ALL clients in same group
class Listener extends Thread {
    // Notice static and concurrent usage here
	// ConcurrentLinkedQueue: really only need a concurrent list, but I couldn't find one in the library.  A Queue will suffice as a list.
	//private static ConcurrentHashMap<String, ConcurrentLinkedQueue<PrintStream>> streamsHashMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<PrintStream>>();
	
	// Start hashmap for all the groups
	private static ConcurrentHashMap<String, Group> groupsHashMap = new ConcurrentHashMap<String, Group>();
	
	private Socket socket;

    public Listener(Socket socket) {
        this.socket  = socket;
    }

    private void closeStreams(BufferedReader inStream, PrintStream outStream, Socket socket) {
    	if (inStream != null)
    		try { inStream.close(); } catch (IOException ex) { /* we tried */ }
    	if (outStream != null)
    		outStream.close(); // doesn't throw an exception
    	if (socket != null)
    		try { socket.close();   } catch (IOException ex) { /* we tried */ }
    }

    //The thread for each phone
    public void run() {    
    	BufferedReader inStream  = null;
    	PrintStream    outStream = null;
    	ConcurrentLinkedQueue<PrintStream> membersPrintStream = null;   //all the Printstreams related to our given group
    	String userName = null;
    	InetAddress who = null;
    	
    	Group group = null;
    	
    	try {
        	inStream  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            outStream = new PrintStream(socket.getOutputStream(), true);		//create client input/output streams for communicating

	        who = socket.getInetAddress(); // for logging purposes

			userName = inStream.readLine(); // first step of protocol, get user registration
			System.out.println("Beginning communication with client " + userName);
			
			User u = new User(userName, socket, outStream);		//update user info on server database
			GroupChatServer.users.put(userName, u);
			
			String groupOwner = userName;   			//initially the groupName will be the groupOwner
			
			String nextString = inStream.readLine();
			
			if(nextString.contains(":FRIEND")){        //if client sends a friend an invitation
				String friendName = tokenize(nextString);
				User friend = checkFriend(friendName);
				if(friend == null){
					outStream.println("invalid friend id");
				}else{
					addFriend(friend, groupOwner);
				}
			}
			
			if(nextString.contains(":ADDME")){			//if client responds yes to group invitation
				StringTokenizer s = new StringTokenizer(nextString, ",");
				s.nextElement();
				groupOwner = (String) s.nextElement();  //get the true groupowner
				
			}

			//streamsHashMap.putIfAbsent(groupOwner, new ConcurrentLinkedQueue<PrintStream>()); // TODO scalability weakness, makes unused queues
		//	members = streamsHashMap.get(groupOwner);		//get the printstreams of proper group
		//	members.add(outStream);							//add clients printstream to it
	        
			groupsHashMap.putIfAbsent(groupOwner, new Group()); //if groupOwner doesn't have a group, make him one
			group = groupsHashMap.get(groupOwner);				//grab the groupOwners group
			//group.add(outStream);
			group.addUser(u);
			
			membersPrintStream = group.getMembers();
			
	        System.out.println("Group " + groupOwner + " now contains " + membersPrintStream.size() + " members");

	        while (true) {
	        	String data = inStream.readLine();
	        	if (data == null) {
	        		throw new IOException("Connection terminated by client");
	        	}
	        	System.out.println(userName + ":" + who + ":" + data);

	        	if(data.contains(":FRIEND")){		//either add next friend or send message out
	        		String friendName = tokenize(data);
	        		User friend = checkFriend(friendName);
	        		if(friend == null){
	        			outStream.println("invalid user id");
	        		}else{
	        			addFriend(friend, groupOwner);	        			
	        		}
	        	}else{
	        		membersPrintStream=group.getMembers();
	        		// Send the message to all group members, including self
	        		for (PrintStream os: membersPrintStream)
	        			synchronized(os) {
	        				os.println(data); // including self; could compare os==outStream to avoid self
	        		}
	        	}
	        	
	        }
    	} catch (Exception e) {
    		System.out.println("Lost communication with " + who + ": " + e);
    		if (membersPrintStream!=null && outStream!=null  && membersPrintStream.remove(outStream)) // TODO scalability weakness, if no more members should remove the queue itself
    			System.out.println("Group " + userName + " now contains " + membersPrintStream.size() + " members");
    		closeStreams(inStream, outStream, socket);
    		return;  // give up on this client
    	}
    }
    
    public String tokenize(String s){
    	StringTokenizer st = new StringTokenizer(s, ",");
    	st.nextElement();
    	return (String) st.nextElement();
    }
    
    public User checkFriend(String friendName){
    	return GroupChatServer.users.get(friendName);
    }
    
    public void addFriend(User friend, String groupOwner){
		PrintStream friendStream = friend.getPrintStream();
		friendStream.println("groupname,"+groupOwner+",like to accept?");
    }
}