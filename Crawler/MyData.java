package crawler;
import com.mongodb.BasicDBObject;
import java.util.LinkedHashSet;
import java.util.List;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import java.io.BufferedReader;
import org.bson.Document;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.util.Pair;



final class MyData
{

    private final Map<String,String> NotVisit;;
    private final Map<String,String> Visited;
    private final LinkedHashSet<String> Content;
    private final Map<String,String> Processing;
    private final List<Integer> Priority;
    public static final int MAX_IT =5000;  //hy3ml crawling 5000 visit
    public static final int MAX_RE =5000;   //hy3ml recrawling l7d 2000 visit
    MongoCollection CollectionVisit ;
    MongoCollection CollectionNotVisit;
    MongoCollection CollectionRVisit ;
    MongoCollection CollectionRNotVisit;
    public MongoDatabase db;
    private final String Type;
    //fetch url from nonvisited and check if it is in visited list or proccessing list
    MyData(MongoDatabase db,String Type)
    {
        this.Type=Type;
        this.CollectionNotVisit = db.getCollection("NotVisit");
        this.CollectionVisit = db.getCollection("Visit");
        this.CollectionRNotVisit = db.getCollection("RNotVisit");
        this.CollectionRVisit = db.getCollection("RVisit");
        this.NotVisit = Collections.synchronizedMap(new LinkedHashMap());
        this.Visited = Collections.synchronizedMap(new LinkedHashMap());
        this.Content=new LinkedHashSet<>();
        this.Processing=Collections.synchronizedMap(new LinkedHashMap());
        this.Priority=new ArrayList<>();
        this.db=db;
        // System.out.println("Read from database");
        ReadDBCrawl();
    }





    //fetch url from nonvisited and check if it is in visited list or proccessing list
    public synchronized  Pair<String, String> fetch()
    {
        if(!GetNotVisit().isEmpty())
        {
            String URL;
            String parent;
            URL=NotVisit.entrySet().iterator().next().getKey();
            System.out.println("fetched "+URL);
            parent=NotVisit.entrySet().iterator().next().getValue();
            this.NotVisit.remove(this.NotVisit.keySet().iterator().next());
            BasicDBObject document = new BasicDBObject();
            document.put("Url",URL);
            if(this.Type.equals("Recrawling"))
                CollectionRNotVisit.deleteOne(document);
            else
                CollectionNotVisit.deleteOne(document);
            //&&Robot(URL)
            if(!this.Visited.containsKey(URL)&&!this.Processing.containsKey(URL)&&Robot(URL))
            {   Pair<String, String> p=new Pair(URL,parent);
                this.Processing.put(URL,parent);
                return p;
            }
            //if(this.Visited.containsKey(URL))
            else if(this.Visited.containsKey(URL))
            { //update ll priority w el parent
                //  System.out.println("there is a duplicate url in visit "+URL);
                int i = new ArrayList<>(this.Visited.keySet()).indexOf(URL);
                IncPriority(i,1);
                System.out.println(this.Priority.get(i));
                String newparent=new ArrayList<>(this.Visited.values()).get(i);
                if(!newparent.contains(parent))
                    newparent=newparent+parent;
                this.Visited.put(URL,newparent);

                org.bson.Document updateQueryparent =new org.bson.Document();
                org.bson.Document updateQuerypriority =new org.bson.Document();
                updateQuerypriority.append("$set", new org.bson.Document("Priority",this.Priority.get(i)));
                updateQueryparent.append("$set", new org.bson.Document("Parent",newparent));
                CollectionVisit.updateOne(eq("Url", URL),updateQueryparent);
                CollectionVisit.updateOne(eq("Url", URL),updateQuerypriority);
            }
            else if(this.Processing.containsKey(URL))
            {
                int i = new ArrayList<>(this.Processing.keySet()).indexOf(URL);
                String newparent=new ArrayList<>(this.Processing.values()).get(i);
                if(!newparent.contains(parent))
                    newparent=newparent+parent;
                this.Processing.put(URL,newparent);
            }
        }

        return null;
    }

    public static String processURL(String theURL)
    {
        int endPos;
        if (theURL.indexOf("?") > 0)
        {
            endPos = theURL.indexOf("?");
        }
        else if (theURL.indexOf("#") > 0)
        {
            endPos = theURL.indexOf("#");
        }
        else
        {
            endPos = theURL.length();
        }
        String url_lower=theURL.substring(0, endPos).toLowerCase();
        if(url_lower.lastIndexOf('/')==url_lower.length()-1)
            return url_lower.substring(0, url_lower.length()-1);
        return url_lower;
    }

    public synchronized void InsertVisit(Pair<String, String> url, Elements linksOnPage,String content,org.jsoup.nodes.Document doc) throws IOException
    {
        if(!this.Visited.containsKey(url.getKey()))
        {
            // System.out.println("the visited url "+url.getKey());
            if( this.Content.add(content))
            {this.Visited.put(url.getKey(), url.getValue());
                this.Priority.add(1);
                int id =(int)CollectionVisit.count();
                String parent=Integer.toString(id);
                String path="crawl\\";
          /*  System.out.println("size of content "+this.Content.size());
            System.out.println("size of visited "+this.Visited.size());
            System.out.println("size of counter "+this.Priority.size());
            System.out.println("size of notvisit "+this.NotVisit.size());*/
                //System.out.println("insert **** "+url.getKey());
                Document document = new Document("Id",id)
                        .append("Url",url.getKey())
                        .append("Parent",url.getValue())
                        .append("Content",content)
                        .append("Priority",1);
                CollectionVisit.insertOne(document);

           /* if(this.Type.equals("Crawling"))
                 path="old\\";
             else
            {
                path="new\\";
                CollectionRVisit.insertOne(document);
            }*/
                if(this.Type.equals("Recrawling"))
                {   System.out.println("insert wgh gded");
                    CollectionRVisit.insertOne(document);
                }

                writer(doc,id,path);

                //  System.out.println("Document inserted successfully");
                if(Visited.size()+NotVisit.size()<MAX_IT*1.5&&linksOnPage!=null)
                {
                    InsertNotVisit(linksOnPage,parent);
                }
            }

        }
        else
        { //the visit contain the location
            //  System.out.println("there is a duplicate url "+url.getKey());
            // int i = new ArrayList<>(this.Visited).indexOf(URL);
            int i = new ArrayList<>(this.Visited.keySet()).indexOf(url.getKey());
            IncPriority(i,1);
            String newparent=new ArrayList<>(this.Visited.values()).get(i);
            if(!newparent.contains(url.getValue()))
                newparent=newparent+" "+url.getValue();
            this.Visited.put(url.getKey(),newparent);
            org.bson.Document updateQueryparent =new org.bson.Document();
            org.bson.Document updateQuerypriority =new org.bson.Document();
            updateQuerypriority.append("$set", new org.bson.Document("Priority",this.Priority.get(i)));
            updateQueryparent.append("$set", new org.bson.Document("Parent",newparent));
            CollectionVisit.updateOne(eq("Url", url.getKey()),updateQueryparent);
            CollectionVisit.updateOne(eq("Url", url.getKey()),updateQuerypriority);

        }

    }



    public synchronized void InsertNotVisit(Elements linksOnPage,String parent)
    { //System.out.println("size of retrive "+linksOnPage.size());
        for(Element link : linksOnPage)
        {
            String Link=link.absUrl("href");
            if(Link!="")
            {
                //Link =processURL(Link);
                System.out.println("insert not visit "+Link);
                if(!this.NotVisit.containsKey(Link))
                {
                    this.NotVisit.put(Link,parent);
                    Document docnotvisit = new Document("Url",Link)
                            .append("Parent"," "+parent);
                    if(this.Type.equals("Recrawling"))
                        CollectionRNotVisit.insertOne(docnotvisit);
                    else
                        CollectionNotVisit.insertOne(docnotvisit);
                    //System.out.println("Document Notvisit inserted successfully");
                }
                else
                {
                    //hzwd l parent
                    System.out.println("there is a duplicate urlin not visit "+Link);
                    int i = new ArrayList<>(this.NotVisit.keySet()).indexOf(Link);
                    String newparent=new ArrayList<>(this.NotVisit.values()).get(i);
                    if(!newparent.contains(parent))
                    {   newparent=newparent+parent;
                        this.NotVisit.put(Link,newparent);
                        org.bson.Document updateQuery =new org.bson.Document();
                        updateQuery.append("$set", new org.bson.Document("Parent",newparent));
                        CollectionVisit.updateOne(eq("Url", Link),updateQuery);
                    }

                }
            }

        }
    }


    public synchronized void InsertRec(String id,Pair<String,String> url,int in,String con)
    {
        Visited.put(url.getKey(), url.getValue());
        Priority.add(in);
        Content.add(con);
        Document document = new Document("Id",id)
                .append("Url",url.getKey())
                .append("Parent",url.getValue())
                .append("Content",con)
                .append("Priority",in);
        CollectionRVisit.insertOne(document);
    }

    public synchronized LinkedHashSet GetContent()
    {
        return Content;
    }

    public synchronized Map GetVisited()
    {
        return Visited;
    }

    public synchronized Map GetNotVisit()
    {
        return NotVisit;
    }
    public synchronized void IncPriority(int index,int num)
    {
        Priority.set(index,(Priority.get(index)+num));
    }
    public List<Integer> GetPriority()
    {
        return Priority;
    }




    public synchronized void ReadDBCrawl()
    {
        FindIterable <Document> DocVisit;
        FindIterable <Document> DocNotVisit;
        if(this.Type.equals("Recrawling"))
        {DocNotVisit = CollectionRNotVisit.find(); DocVisit = CollectionRVisit.find();}
        else
        {DocNotVisit=CollectionNotVisit.find(); DocVisit = CollectionVisit.find();}

        //read all info belongs to visits wesites
        for (Document myDoc : DocVisit)
        {
            String url = myDoc.get("Url").toString();
            String content=myDoc.get("Content").toString();
            String parent =myDoc.get("Parent").toString();
            int priority=Integer.parseInt(myDoc.get("Priority").toString());
            this.Visited.put(url, parent);
            this.Content.add(content);
            this.Priority.add(priority);

        }
        //read urls which are not visited
        for (Document myDoc : DocNotVisit)
        {
            String url = myDoc.get("Url").toString();
            String parent = myDoc.get("Parent").toString();
            this.NotVisit.put(url,parent);

        }

    }


    /*  public void ReadDBRecrawl()
      {

          System.out.println("Start Read DB Recrawling");
          int NumOfSeeds = 100;
          FindIterable<Document> doc = CollectionVisit.find();
          BasicDBObject query =new BasicDBObject("Priority", -1);
          doc.sort(query).limit(NumOfSeeds);
          //read data in map of not visited
           for (Document myDoc : doc)
           {
           String Url = (String) myDoc.get( "Url");
           String Parent = (String) myDoc.get( "Parent");
           this.NotVisit.put(Url, Parent);
           System.out.println("Parent is: "+ Parent + " & Url is: "+Url);
           }



      }
      */
    //writing html files in the folder
    public void writer(org.jsoup.nodes.Document htmldoc,int id,String path) throws IOException
    {
        //  System.out.println("hktbbbb");
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path+id+".html"), "utf-8"));
        writer.write(htmldoc.toString());
        writer.close();

    }



    /*

     //writing in the DB each iteration after constant time
    public synchronized  void  WriteDBAfterTime()
    {
        System.out.println("writing in db");
        //insert all info belongs to visited url
        for(int j=(int)CollectionVisit.count();j<new ArrayList<>(Visited.values()).size();j++)
        {
            int in = Priority.get(j);
            Document document = new Document("Id",j)
            .append("Url", new ArrayList<>(Visited.keySet()).get(j))
            .append("Parent",new ArrayList<>(Visited.values()).get(j))
            .append("Content",new ArrayList<>(Content).get(j))
            .append("Priority",in);
            CollectionVisit.insertOne(document);
            System.out.println("Document inserted successfully");
         }

        //insert all not visit ursl
        CollectionNotVisit.drop();
        db.createCollection("NotVisit");
        CollectionNotVisit= db.getCollection("NotVisit");
        for (Map.Entry<String,String> entry : NotVisit.entrySet())
        {
            Document document = new Document("Url",entry.getKey())
                    .append("Parent",entry.getValue());
            CollectionNotVisit.insertOne(document);
            System.out.println("Document Notvisit inserted successfully");
        }
    }
*/

     /* public synchronized  void InsertNotVisit(Elements l,String parent)
        {
            int count =0;
        if(Visited.size()+NotVisit.size()<MAX_IT)
        { //System.out.println("insert notvisited from thread "+Thread.currentThread().getId());
            //System.out.println("****inseeeeeeeeeeeeeeeeeeeert*******************************");
            for(Element link : l)
                {
                    if(link.absUrl("href")!=""&&!this.Visited.containsKey(link.absUrl("href"))&&!this.NotVisit.containsKey(link.absUrl("href")))
                    //if(link.absUrl("href")!=""&&!this.Visited.contains(link.absUrl("href"))&&!this.NotVisit.contains(link.absUrl("href"))&&!this.HashCodes.contains(content.hashCode()))
                    { this.NotVisit.put(link.absUrl("href"),parent);
                        count++;
                    }
                }
            System.out.println("the size of inserted pages "+ count);
           // System.out.println("size of notvisited "+this.NotVisit.size());
          //  System.out.println("size of visited "+this.Visited.size());
        }
        }*/


    public static Boolean Robot(String Url){
        List<String> allMatches = new ArrayList<>();
        List<String> allMatches_Allow = new ArrayList<>();
        String SubUrl ="";
        // System.out.println("da5laa");

        try {
            URL url = new URL(Url);
            SubUrl =url.getProtocol() + "://" + url.getHost();

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (SubUrl.equals(Url)||Url.equals(SubUrl+"/")){
            return true;
        }

        try(BufferedReader in = new BufferedReader(new InputStreamReader(new URL(SubUrl+"/robots.txt").openStream()))) {
            String line;
            boolean found_user=false;
            while((line = in.readLine()) != null) {
                int index1 = line.indexOf("User-agent: *");
                int index2 = line.indexOf("User-Agent: *");
                if (index1 !=-1 ||index2 !=-1 ){
                    found_user=true;
                    continue;
                }
                if (found_user) {
                    //System.out.println("d5l");
                    //System.out.println(line);
                    int m = line.indexOf("Disallow: ");
                    int Allow = line.indexOf("Allow: ");
                    //System.out.println(m);
                    //System.out.println(Allow);
                    if (m != -1){
                        String disallow;
                        disallow=line.substring(10,line.length());
                        //  System.out.println("disAlow");

                        allMatches.add(disallow.replaceAll(" ", ""));
                    }
                    else if (Allow != -1) {
                        String allow;
                        allow=line.substring(8,line.length());
                        //System.out.println("Alow");
                        allMatches_Allow.add(allow.replaceAll(" ",""));

                    }


                }
            }
        }
        catch (IOException e) {
        }
        //System.out.println("5alst awl while");

        int size_disallow=0;
        int size_allow=0;
        for (int counter = 0; counter < allMatches.size(); counter++) {
            boolean found_disallow=Url.contains(allMatches.get(counter));
            if (found_disallow) {
                size_disallow= allMatches.get(counter).length();
                break;

            }
        }
        // System.out.println("5alst tany while");

        for (int counter = 0; counter < allMatches_Allow.size(); counter++) {
            boolean found_allow=Url.contains(allMatches_Allow.get(counter));
            if (found_allow) {
                size_allow= allMatches_Allow.get(counter).length();
                break;
            }
        }
        // System.out.println("5alst talt while");

        System.out.println("5argaa");
        return size_disallow <= size_allow;
    }





}