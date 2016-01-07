import com.mongodb.MongoClient;
/**
 * Created by Calixguo on 2015/12/15.
 */
public class test {
    public static void main (String[] args) throws Exception{
	System.out.println("###########initial successfully##############");
         MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
	System.out.println("###########initial successfully##############");
    }
}
