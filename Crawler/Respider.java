package crawler;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Filters.eq;
import static crawler.MyData.MAX_IT;
import static crawler.MyData.MAX_RE;
import static crawler.Spider.md5Hash;
import java.io.IOException;
import java.net.SocketException;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author menna
 */
public class Respider extends Thread
{
    private final MyData MyData;
    Respider(MyData MyData)
    {
        this.MyData=MyData;
    }

    @Override
    public void run ()
    { //3wza azwd eno ya5d mn l notvisit el 2odam
        while(MyData.GetVisited().size()<MyData.MAX_RE&&!MyData.GetNotVisit().isEmpty())
        {
            Pair<String, String> url;
            url = MyData.fetch();
            if(url!=null)
                SimpleReCrawl(url);
        }

    }



    public  void SimpleReCrawl(Pair<String, String> url)
    { String parent;
        int priority;
        String id;
        try
        {
            org.jsoup.Connection connection = Jsoup.connect(url.getKey()).timeout(2*60*1000).followRedirects(true);
            Document htmlDocument = connection.get();
            String location =htmlDocument.location();
            Pair<String, String> location_url=new Pair(location,url.getValue());
            int response =connection.response().statusCode();
            if(response == 200) //http
            {
                System.out.println("the response =200 request page " + location_url.getKey());
                if(htmlDocument.body()!=null)
                {

                    String text = htmlDocument.body().text();
                    String hashword=md5Hash(text);
                    MongoCollection collection = MyData.db.getCollection("Visit");
                    BasicDBObject query = new BasicDBObject("Url", new BasicDBObject("$eq",location_url.getKey()));
                    FindIterable <org.bson.Document> doc = collection.find(query);
                    org.bson.Document mydoc=doc.first();
                    if(mydoc!=null) //the url exist in my db
                    {
                        priority = Integer.parseInt(mydoc.get("Priority").toString());
                        String con= mydoc.get("Content").toString();
                        id =mydoc.get("Id").toString();
                        if(!hashword.equals(con))
                        {
                            System.out.println("there is a modified document");
                            //collection.updateOne(eq(new BasicDBObject("Url",url.getKey())),new BasicDBObject("Content",new BasicDBObject("$set",hashword)));
                            org.bson.Document updateQuery =new org.bson.Document();
                            updateQuery.append("$set", new org.bson.Document("Content", hashword));
                            updateQuery.append("$set", new org.bson.Document("Priority", priority+10));
                            collection.updateOne(eq("Url", url.getKey()),updateQuery);
                            // MyData.writer(htmlDocument, Integer.parseInt(mydoc.get("Id").toString()),"mod\\");
                            MyData.writer(htmlDocument, Integer.parseInt(mydoc.get("Id").toString()),"crawl\\");
                            Elements linksOnPage = htmlDocument.select("a[href]");
                            if(MyData.GetVisited().size()+MyData.GetNotVisit().size()<MAX_RE*1.5)
                                MyData.InsertNotVisit(linksOnPage,id);
                            con=hashword;
                        }
                        MyData.InsertRec(id,url,priority,con); //insert in visit list and in Rvisit collection

                    }
                    else
                    {

                        MyData.InsertVisit(url,null,hashword,htmlDocument);

                    }
                }
            }

            else
            {
                System.out.println("Error retreiving page: " + connection.response().statusCode());
            }


        }
        catch(SocketException e){/*System.out.println("\n connection reset " + url);*/}
        catch (IOException | IllegalArgumentException | UncheckedIOException e){/*   System.out.println("\n We were not successful in our HTTP request " + url);*/}

    }



}