package crawler;
import static crawler.MyData.processURL;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javafx.util.Pair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


class Spider extends Thread
{

    private final MyData MyData;
    Spider(MyData MyData)
    {
        this.MyData=MyData;
    }

    @Override
    public void run ()
    {

        while(MyData.GetVisited().size()<MyData.MAX_IT)
        {
            Pair<String, String> url;
            url = MyData.fetch();
            if(url!=null)
            {
                SimpleCrawl(url);

            }
        }
    }
    public  void SimpleCrawl(Pair<String, String> url)
    {
        try {
            URL  ur = new URL(url.getKey());

            try
            {
                if(ur.getHost() != null||ur.getHost().length() != 0 )
                {
                    Connection connection = Jsoup.connect(url.getKey()).timeout(2*60*1000).followRedirects(true);
                    Document htmlDocument = connection.get();
                    String location =htmlDocument.location();
                    //location =processURL(location);
                    Pair<String, String> location_url=new Pair(location,url.getValue());
                    int response =connection.response().statusCode();
                    if(response == 200) //http
                    {
                        //  System.out.println("the response = 200 request page " + url.getKey());
                        if(htmlDocument.body()!=null)
                        {
                            String text = htmlDocument.body().text();
                            String hashword=md5Hash(text);
                            Elements linksOnPage = htmlDocument.select("a[href]");
                            MyData.InsertVisit(location_url,linksOnPage,hashword,htmlDocument);
                        }
                    }

                    else
                    {
                        //          System.out.println("Error retreiving page: " + connection.response().statusCode());
                    }
                }
            }
            catch(SocketException e){  /*    System.out.println("\n connection reset " + url);*/}
            catch (IOException | IllegalArgumentException | UncheckedIOException e){/*   System.out.println("\n We were not successful in our HTTP request " + url);*/}
        }
        catch (MalformedURLException ex) {/*Logger.getLogger(Spider.class.getName()).log(Level.SEVERE, null, ex);*/}

    }



    public static String md5Hash(String message)
    {
        String md5 = "";
        if(null == message)
            return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");//Create MessageDigest object for MD5
            digest.update(message.getBytes(), 0, message.length());//Update input string in message digest
            md5 = new BigInteger(1, digest.digest()).toString(16);//Converts message digest value in base 16 (hex)

        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return md5;
    }






}
