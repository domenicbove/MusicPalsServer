import java.io.PrintStream;
import java.net.Socket;


public class User {

	private Socket socket;
	private PrintStream printStream;
	private String userName;
	private String password;
	
	public User(String username, Socket socket, PrintStream printStream){
		setSocket(socket);
		setUserName(username);
		setPrintStream(printStream);
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public PrintStream getPrintStream() {
		return printStream;
	}

	public void setPrintStream(PrintStream printStream) {
		this.printStream = printStream;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
