import com.mongodb.MongoClient;
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

public class MongoDBJDBC{
   public static void main( String args[] ){
      try{   
	     // 连接到 mongodb 服务
         MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
         // 连接到数据库
	 MongoDatabase db = mongoClient.getDatabase("test");
	 System.out.println("Connect to database successfully");
         MongoCollection<Document> coll = db.getCollection("firstCollection");
         System.out.println("Collection created successfully");
/*	 Document doc = coll.find(eq("title", "MongoDB")).first();
	 System.out.println(doc.toJson());
	 System.out.println("=====================================\n");
	 
	 FindIterable<Document> iterable = coll.find(new Document("tittle", "test"));
	 iterable.forEach(new Block<Document>() {
	 	@Override
		public void apply(final Document document) {
			System.out.println("--------------Here is a doc of Collection firstCollection----------------\n");
			System.out.println(document);
		}
	 });*/
	 coll.insertOne(new Document("tittle", "happy").append("by", "ff").append("time", "today"));
	 FindIterable<Document> iterable2 = coll.find(new Document("tittle", "happy"));
	 iterable2.forEach(new Block<Document>() {
	 	@Override
		public void apply(final Document document) {
			System.out.println("--------------Here is a doc of Collection firstCollection----------------\n");
			System.out.println(document);
		}
	 });

	 coll.updateOne(new Document("tittle", "happy"), new Document("$set", new Document("by", "FF")));
	 FindIterable<Document> iterable3 = coll.find(new Document("tittle", "happy"));
	 iterable3.forEach(new Block<Document>() {
	 	@Override
		public void apply(final Document document) {
			System.out.println("--------------Here is a doc of Collection firstCollection----------------\n");
			System.out.println(document);
		}
	 });
	 System.out.println("--------------TAG----------------\n");

	 coll.deleteMany(new Document("by", "ff"));
	 FindIterable<Document> iterable4 = coll.find(new Document("tittle", "happy"));
	 iterable4.forEach(new Block<Document>() {
	 	@Override
		public void apply(final Document document) {
			System.out.println("--------------Here is a doc of Collection firstCollection----------------\n");
			System.out.println(document);
		}
	 });
	 
	 System.out.println("----------------------------------TAG----------------------------------\n");
	 db.createCollection("newInsertCollection");
         MongoCollection<Document> collNew = db.getCollection("newInsertCollection");
	 System.out.println("create new collection successfullly\n");
	 collNew.insertOne(new Document("host", "sysu").append("address", "east"));
	 FindIterable<Document> iterable5 = collNew.find(new Document("host", "sysu"));
	 iterable5.forEach(new Block<Document>() {
	 	@Override
		public void apply(final Document document) {
			System.out.println("--------------Here is a doc of Collection newInsertCollection----------------\n");
			System.out.println(document);
		}
	 });
	 
	 
	 
      }catch(Exception e){
	     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	  }
   }
}
