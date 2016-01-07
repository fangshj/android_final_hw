/*
 * author : guoqizhen and fangshaojie
 * date   : 12/28/2015
 * content: codes about android server using java socket and MongoDB
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.MongoClient;

/**
 * Created by Calixguo on 2015/12/15.
 */
public class Myserver {
    public static ArrayList<Socket> socketArrayList = new ArrayList<Socket>();
    public static Map<String,Socket> socketMap = new HashMap<>();
    public static void main (String[] args) throws Exception{
	System.out.println("###########initial successfully##############");
        ServerSocket ss = new ServerSocket(30000);
	// all the threads share the same MongoClient instance
	MongoClient mongoClient = new MongoClient("localhost", 27017);
        while(true) {
            Socket s = ss.accept();
            socketArrayList.add(s);
            new Thread(new ServerThread(s, mongoClient)).start();
        }
    }
}
