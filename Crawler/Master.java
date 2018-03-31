package crawler;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Scanner;
import org.bson.Document;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class Master implements Job
{
    private static  int MAX_TH;
    private static final int NUM_RESEED=2000; //el seed hya5d mnha ad a y3ml 3mleha recrawling
    MongoClient mongoClient = new MongoClient( "localhost" , 27017 );  //create mongo client
    MongoDatabase db = mongoClient.getDatabase("searchengine"); //accessing database
    MongoCollection CollectionState = db.getCollection("State");
    MongoCollection CollectionRNotVisit=db.getCollection("RNotVisit");
    MongoCollection CollectionRVisit=db.getCollection("RVisit");
    MongoCollection CollectionVisit=db.getCollection("Visit");
    MongoCollection CollectionNotVisit=db.getCollection("NotVisit");
    FindIterable <Document> DocState = CollectionState.find();
    String State;
    String ReadSeed;
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException
    {    State= DocState.first().get("Crawling").toString();
        ReadSeed=DocState.first().get("ReadSeed").toString();
        System.out.println("Connected to the database successfully");
        Scanner reader = new Scanner(System.in);
        System.out.println("Enter the Number of Threads: ");
        MAX_TH = reader.nextInt();

        if(ReadSeed.equals("False"))
        {
            try {
                Seeds();
                CollectionState.updateOne(eq("Id",1),new org.bson.Document("$set", new org.bson.Document("ReadSeed","True")));
            }
            catch (IOException ex) {}
        }

        if(State.equals("False"))
        {
            MyData cr_MyData = new MyData(db,"Crawling");
            ArrayList<Thread> cr_thread = new ArrayList<>();
            for(int i=0;i<MAX_TH;i++)
            {
                cr_thread.add(new Spider(cr_MyData));
                cr_thread.get(i).start();
            }

            for(int i=0;i<MAX_TH;i++)
            {
                try {
                    cr_thread.get(i).join();
                } catch (InterruptedException ex) {
                    // Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(cr_MyData.GetNotVisit().isEmpty())
                System.out.println("the NotVisited List Is Empty");
            else
                System.out.println("finishing crawling");
            CollectionState.updateOne(eq("Id",1),new org.bson.Document("$set", new org.bson.Document("Crawling","Maybe")));
            State= DocState.first().get("Crawling").toString();
        }
        if(State.equals("Maybe"))
        {
            System.out.println("Start Read DB Recrawling");
            CollectionRNotVisit.drop();
            CollectionRVisit.drop();
            db.createCollection("RNotVisit");
            db.createCollection("RVisit");
            CollectionRNotVisit = db.getCollection("RNotVisit");
            FindIterable<Document> doc = CollectionVisit.find();
            BasicDBObject query =new BasicDBObject("Priority", -1);
            doc.sort(query).limit(NUM_RESEED);
            //read data in map of not visited
            for (Document myDoc : doc)
            {
                //System.out.println("insert in recrawling");
                CollectionRNotVisit.insertOne(myDoc);
            }
            CollectionState.updateOne(eq("Id",1),new org.bson.Document("$set", new org.bson.Document("Crawling","True")));
            State= DocState.first().get("Crawling").toString();
        }
        //T***********************************************
        if(State.equals("True"))
        {
            System.out.print("Start To Recrawl");
            ArrayList<Thread> rcr_thread = new ArrayList<>();
            MyData rcr_MyData = new MyData(db,"Recrawling");
            for(int i=0;i<MAX_TH;i++)
            {
                rcr_thread.add(new Respider(rcr_MyData));
                rcr_thread.get(i).start();
            }
            for(int i=0;i<MAX_TH;i++)
            {
                try {
                    rcr_thread.get(i).join();
                } catch (InterruptedException ex) {
                    //Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(rcr_MyData.GetNotVisit().isEmpty())
            {
                System.out.println("the NotVisited List Is Empty");}
            System.out.println(rcr_MyData.GetPriority());
            System.out.println(rcr_MyData.GetPriority().size());
            System.out.println("finished Recrawling");
            CollectionState.updateOne(eq("Id",1),new org.bson.Document("$set", new org.bson.Document("Crawling","Maybe")));

        }
    }
    // Connection obj = new Connection(MyData.GetVisited(),MyData.GetContent());
    // Collector collector = new Collector(MyData);
    //collector.WriteDB();


    public void Seeds() throws IOException
    {
        CollectionRNotVisit.drop();
        CollectionRVisit.drop();
        db.createCollection("RNotVisit");
        db.createCollection("RVisit");
        CollectionVisit.drop();
        CollectionNotVisit.drop();
        db.createCollection("NotVisit");
        db.createCollection("Visit");
        CollectionNotVisit = db.getCollection("NotVisit");
        LinkedHashSet<String> NotVisit=new LinkedHashSet();
        File Notvisit = new File("seeds.txt");
        try
        {
            String st;
            BufferedReader bn = new BufferedReader(new FileReader(Notvisit));

            while ((st = bn.readLine()) != null&&!st.equals(""))
            {

                NotVisit.add(st);
            }
            System.out.println(NotVisit.size());

        }
        catch (FileNotFoundException ex) {}
        Iterator<String> notvisit = NotVisit.iterator();
        while(notvisit.hasNext())
        {
            Document document = new Document("Url",notvisit.next())
                    .append("Parent"," ");
            CollectionNotVisit.insertOne(document);
            //System.out.println("Document Notvisit inserted successfully");
        }


        //System.out.println("finished to put seeds");

    }



}
