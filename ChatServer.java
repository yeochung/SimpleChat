// https://github.com/yeochung/SimpleChat

import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id + " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){
				if(line.equals("/quit"))
					break;
				if(line.indexOf("/to ") == 0){
					sendmsg(line);
				} if(line.equals("/userlist")) {
					send_userlist();
				}
				else
					broadcast(id + " : " + line);
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	

	// send_userlist() method - synchronize로 공유되는 hashmap을 이 사용자가 사용하게 소위 줄을 세워준 상태에서 지금 이 아이디 key를 가지고
	// value, 즉 여기서는 이 아이디를 포함한 ChatThread의 pw를 가지고 와서 pw를 통해 아 아이디의 채팅창에만 사용자 수와 사용자 ID들을 출력한다.
	// 이 때 사용자 수는 사용자들의 ID가 key값으로 pw가 value로 pair되어 들어간 hashmap을 이용한다. 즉, hashmap의 size를 구하면 사용자의 수가 되고
	// hash의 key, 즉 ID 들을 출력하기 위해서 key를 print하는 keySet() 을 이용한다.
	public void send_userlist() {
		synchronized(hm) {
			Object obj = hm.get(id);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println("Total number of users : " + hm.size());
				pw.println("IDs : " + hm.keySet());
				pw.flush();
			}
		}
	}
	
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg
	public void broadcast(String msg){
		synchronized(hm){
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			Object obj = hm.get(id); // get and store value of this id (key)
			while(iter.hasNext()){
				PrintWriter pw = (PrintWriter)iter.next();
				if (pw != (PrintWriter)obj) { // it pw isn't equal to THIS obj value, continue printing the msg
					pw.println(msg);
					pw.flush();
				}
			}
		}
	} // broadcast
}