/*
 * author : guoqizhen and fangshaojie
 * date   : 12/28/2015
 * content: codes about android server using java socket and MongoDB
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.json.*;
import java.net.SocketException;
import java.util.Iterator;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static java.util.Arrays.asList;
import java.util.*;
import java.lang.Integer;

public class ServerThread implements Runnable{
    Socket s = null;
    BufferedReader br = null;
    private final MongoClient mongoClient;
    private final MongoDatabase db;
    public ServerThread(Socket s, final MongoClient mongoClient) throws Exception{
        this.s = s;
	this.mongoClient = mongoClient;
	this.db = mongoClient.getDatabase("androidhw");
	System.out.println("############Connect to client successfully##############");
        br = new BufferedReader(new InputStreamReader(s.getInputStream(),"utf-8"));
        System.out.println(s.getInetAddress());
    }

    public void run() {
        try {
	    System.out.println("############Connect to database androidhw successfully##############");
            String content = null;

            while((content = readFromClient()) != null) {
            	JSONObject object = new JSONObject(content);
           	String type = object.getString("type").toString();
          	String user = object.getString("userName").toString();
            	if(type.equals("login")) {
			String password = object.getString("password").toString();
			MongoCollection<Document> usersCollection = db.getCollection("usersCollection");
			if (usersCollection.count(eq("userName", user)) == 0) {
				try {
					OutputStream os = s.getOutputStream();
					JSONObject outObject = new JSONObject();
					outObject.put("response", false);
					outObject.put("type", "responseLogin");
					outObject.put("remark", "username wrong");
					os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				Document doc = usersCollection.find(eq("userName", user)).first();
				String rightPassword = doc.get("password").toString();
				if (rightPassword.equals(password)) {
            				Myserver.socketMap.put(user, s);
					MongoCollection<Document> unSentHwCollection = db.getCollection("unSentHwCollection");
					Document hwDoc = unSentHwCollection.find(eq("userName", user)).first();
					int biggestHwNumber = (int)doc.get("biggestHwNumber");
					System.out.println("############size of the hwDoc is " + hwDoc.size() + "##############");
					if (hwDoc.size() == 2) {// 2 means ObjectID and userName which must exist
						try {
							OutputStream os = s.getOutputStream();
							JSONObject outObject = new JSONObject();
							outObject.put("response", true);
							outObject.put("type", "responseLogin");
							outObject.put("remark", "notHaveUnReceivedHw");
							outObject.put("biggestHwNumber", biggestHwNumber);
							os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
						} catch (SocketException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}	
					} else {
						// response to login request
						try {
							OutputStream os = s.getOutputStream();
							JSONObject outObject = new JSONObject();
							outObject.put("response", true);
							outObject.put("type", "responseLogin");
							outObject.put("remark", "haveUnReceivedHw");
							outObject.put("biggestHwNumber", biggestHwNumber);
							os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
						} catch (SocketException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}	
						// have unreceive homeworks, find them and send them	
						try {
							OutputStream os = s.getOutputStream();
							Set<String> set = new HashSet<String>();
							set = hwDoc.keySet();
							Iterator<String> it = set.iterator();
							while (it.hasNext()) {
								String key = it.next();
								if (!key.equals("userName") && !key.equals("_id")) {
									Document itemHwDoc = (Document)hwDoc.get(key);
									JSONObject outObject = new JSONObject();
									outObject.put("response", true);
									outObject.put("type", "responseLogin");
									outObject.put("remark", "haveNewHw");
									outObject.put("subject", itemHwDoc.get("subject").toString());
									outObject.put("deadline", itemHwDoc.get("deadline").toString());
									outObject.put("hwRemark", itemHwDoc.get("remark").toString());
									outObject.put("questionNumber", itemHwDoc.get("questionNumber").toString());
									os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
									hwDoc.remove(key);
								}
							}
							unSentHwCollection.deleteOne(new Document("userName", user));
							unSentHwCollection.insertOne(hwDoc);
						} catch (SocketException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}	
					}
				} else {
					try {
						OutputStream os = s.getOutputStream();
						JSONObject outObject = new JSONObject();
						outObject.put("response", false);
						outObject.put("type", "responseLogin");
						outObject.put("remark", "password wrong");
						os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
				}
			}
            	} else if(type.equals("sendHomework")) {
            		String receiverName = object.getString("receiverName").toString();
            		Socket rs = Myserver.socketMap.get(receiverName);

		    	int hwNumber= object.getInt("hwNumber");	
			String hwNumberStr = Integer.toString(hwNumber);

            		if(rs == null) {
				MongoCollection<Document> hwCollection = db.getCollection("hwCollection");
				Document hwDoc = hwCollection.find(eq("userName", user)).first();
				Document hw = (Document)hwDoc.get(hwNumberStr);
				
				MongoCollection<Document> unSentHwCollection = db.getCollection("unSentHwCollection");
				Document doc = unSentHwCollection.find(eq("userName", receiverName)).first();
				// user + hwNumber as key in case it may be the same
				doc.put(user + hwNumberStr, hw);
				unSentHwCollection.deleteOne(new Document("userName", receiverName));
				unSentHwCollection.insertOne(doc);
				try {
					OutputStream os = s.getOutputStream();
					JSONObject outObject = new JSONObject();
					outObject.put("response", true);
					outObject.put("type", "responseSendHomework");
					os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
            		} else {
				MongoCollection<Document> hwCollection = db.getCollection("hwCollection");
				Document doc = hwCollection.find(eq("userName", user)).first();
				Document hw = (Document)doc.get(hwNumberStr);
				try {
					OutputStream os = rs.getOutputStream();
					JSONObject outObject = new JSONObject();
					outObject.put("response", true);
					outObject.put("type", "responseHaveNewHw");
					outObject.put("subject", hw.get("subject").toString());
					outObject.put("questionNumber", hw.get("questionNumber").toString());
					outObject.put("hwRemark", hw.get("remark").toString());
					outObject.put("remark", "haveNewHw");
					outObject.put("deadline", hw.get("deadline").toString());
					os.write((outObject.toString() + "\r\n").getBytes("utf-8"));

					OutputStream ros = s.getOutputStream();
					JSONObject outObject2 = new JSONObject();
					outObject2.put("response", true);
					outObject2.put("type", "responseSendHomework");
					ros.write((outObject2.toString() + "\r\n").getBytes("utf-8"));
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
            		}
            	} else if(type.equals("register")) {
			String password = object.getString("password").toString();
			String avatar = object.getString("avatar").toString();
			MongoCollection<Document> usersCollection = db.getCollection("usersCollection");
			if (usersCollection.count(eq("userName", user)) == 0) {
				try {
					db.getCollection("unSentHwCollection").insertOne(new Document("userName", user));
					db.getCollection("friendsListCollection").insertOne(new Document("userName", user));
					db.getCollection("hwCollection").insertOne(new Document("userName", user));
					usersCollection.insertOne(new Document("userName", user).append("password", password).append("avatar", avatar).append("biggestHwNumber", 0));
					OutputStream os = s.getOutputStream();
					JSONObject outObject = new JSONObject();
					outObject.put("response", true);
					outObject.put("type", "responseRegister");
					os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				try {
					OutputStream os = s.getOutputStream();
					JSONObject outObject = new JSONObject();
					outObject.put("response", false);
					outObject.put("type", "responseRegister");
					outObject.put("remark", "username existed");
					os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
            	} else if(type.equals("logout")) {
			try {
				OutputStream os = s.getOutputStream();
				JSONObject outObject = new JSONObject();
				outObject.put("response", true);
				outObject.put("type", "responseLogout");
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("############logout successfully##############");
            		Myserver.socketMap.remove(user);
            		Myserver.socketArrayList.remove(s);
            	} else if (type.equals("addFriend")) {
			String friendName = object.getString("friendName").toString();
			MongoCollection<Document> friendsListCollection = db.getCollection("friendsListCollection");
			Document doc = friendsListCollection.find(eq("userName", user)).first();
			friendsListCollection.deleteOne(new Document("userName", user));
			doc.put(friendName, friendName);
			friendsListCollection.insertOne(doc);

			MongoCollection<Document> usersCollection = db.getCollection("usersCollection");
			Document friendDoc = usersCollection.find(eq("userName", friendName)).first();
			String avatar = friendDoc.get("avatar").toString();
			try {
				OutputStream os = s.getOutputStream();
				JSONObject outObject = new JSONObject();
				outObject.put("response", true);
				outObject.put("type", "responseAddFriend");
				outObject.put("avatar", avatar);
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if (type.equals("deleteFriend")) {
			String friendName = object.getString("friendName").toString();
			MongoCollection<Document> friendsListCollection = db.getCollection("friendsListCollection");
			Document doc = friendsListCollection.find(eq("userName", user)).first();
			friendsListCollection.deleteOne(new Document("userName", user));
			doc.remove(friendName);
			friendsListCollection.insertOne(doc);
			try {
				OutputStream os = s.getOutputStream();
				JSONObject outObject = new JSONObject();
				outObject.put("response", true);
				outObject.put("type", "responseDeleteFriend");
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();

		}
            } else if (type.equals("addHomework")) {
		    	String subject = object.getString("subject").toString();	
		    	int hwNumber= object.getInt("hwNumber");	
			String hwNumberStr = Integer.toString(hwNumber);
			System.out.println("############hwNumber is : "+hwNumberStr+" ##############");
		    	String questionNumber = object.getString("questionNumber").toString();	
		    	String deadline = object.getString("deadline").toString();	
		    	String remark = object.getString("remark").toString();	
			// update the biggest hwNumber to the newest one
			MongoCollection<Document> usersCollection = db.getCollection("usersCollection");
			Document userDoc = usersCollection.find(eq("userName", user)).first();
	 		usersCollection.updateOne(new Document("userName", user), new Document("$set", new Document("biggestHwNumber", hwNumber)));
			

			MongoCollection<Document> hwCollection = db.getCollection("hwCollection");
			Document doc = hwCollection.find(eq("userName", user)).first();
			hwCollection.deleteOne(new Document("userName", user));
			Document hw = new Document("subject", subject).append("questionNumber", questionNumber).append("deadline", deadline).append("remark", remark);
			doc.put(hwNumberStr, hw);
			hwCollection.insertOne(doc);
			try {
				OutputStream os = s.getOutputStream();
				JSONObject outObject = new JSONObject();
				outObject.put("response", true);
				outObject.put("type", "responseAddHomework");
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
	    } else if (type.equals("deleteHomework")) {
		    	int hwNumber= object.getInt("hwNumber");	
			String hwNumberStr = Integer.toString(hwNumber);

			MongoCollection<Document> hwCollection = db.getCollection("hwCollection");
			Document doc = hwCollection.find(eq("userName", user)).first();
			hwCollection.deleteOne(new Document("userName", user));
			doc.remove(hwNumberStr);
			hwCollection.insertOne(doc);
			try {
				OutputStream os = s.getOutputStream();
				JSONObject outObject = new JSONObject();
				outObject.put("response", true);
				outObject.put("type", "responseDeleteHomework");
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
	    } else if (type.equals("updateHomework")) {
		    	String subject = object.getString("subject").toString();	
		    	int hwNumber= object.getInt("hwNumber");	
			String hwNumberStr = Integer.toString(hwNumber);
		    	String questionNumber = object.getString("questionNumber").toString();	
		    	String deadline = object.getString("deadline").toString();	
		    	String remark = object.getString("remark").toString();	

			MongoCollection<Document> hwCollection = db.getCollection("hwCollection");
			Document doc = hwCollection.find(eq("userName", user)).first();
			hwCollection.deleteOne(new Document("userName", user));
			Document hw = new Document("subject", subject).append("questionNumber", questionNumber).append("deadline", deadline).append("remark", remark);
			doc.remove(hwNumberStr);
			doc.put(hwNumberStr, hw);
			hwCollection.insertOne(doc);
			try {
				OutputStream os = s.getOutputStream();
				JSONObject outObject = new JSONObject();
				outObject.put("response", true);
				outObject.put("type", "responseUpdateHomework");
				os.write((outObject.toString() + "\r\n").getBytes("utf-8"));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
	    }
        
	}
	}
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (JSONException e) {
       	e.printStackTrace();
       }
    }

    private String readFromClient() {
        try {
            return br.readLine();
        }catch (IOException e) {
            e.printStackTrace();
            Myserver.socketArrayList.remove(s);
        }
        return null;
    }
}

