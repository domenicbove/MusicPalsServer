import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Group {

	private ConcurrentLinkedQueue<PrintStream> members;
	private String currentSongId;
	private int startTime;
	private ArrayList<User> users;
	
	public Group(){
		members = new ConcurrentLinkedQueue<PrintStream>();
		users = new ArrayList<User>();
	}

	public void add(PrintStream outStream) {
		members.add(outStream);
	}
	
	public ConcurrentLinkedQueue<PrintStream> getMembers(){
		return members;
	}

	public String getCurrentSongId() {
		return currentSongId;
	}

	public void setCurrentSongId(String currentSongId) {
		this.currentSongId = currentSongId;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public void addUser(User u) {
		users.add(u);
		members.add(u.getPrintStream());
	}
}
