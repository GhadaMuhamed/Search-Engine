import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

import java.io.*;
import java.util.*;

public class main {
    public static Map<String, Integer> stopWords;
    public static void readStopWords(){
        stopWords = new HashMap<String, Integer>();
        File file=new File("stopWords.txt");
        FileReader fileReader;
        try{ fileReader=new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);
            String stopWord;
            try {
                stopWord = br.readLine();
                while (stopWord != null) {
                    stopWords.put(stopWord,1);
                    stopWord = br.readLine();
                }
            }
            catch(IOException e){}
            try{ fileReader.close();}
            catch (IOException e){
            }
        }
        catch (FileNotFoundException x){
            System.out.println("file not found");
        }
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String,Boolean> oldFiles=new HashMap<>();
        File oldFolder = new File("old");
        for (final File fileEntry : oldFolder.listFiles())
            oldFiles.put(fileEntry.getName(),true);

        MongoClient mongoClient = new MongoClient("localhost",27017);  //create mongo client
        MongoDatabase db = mongoClient.getDatabase("searchEngine");
        MongoCollection col;
        String colName = "invertedDocument";
        col = db.getCollection(colName);
        col.createIndex(Indexes.descending("word"));
        System.out.println("Connected to the database successfully");
        readStopWords();
        indexer ind = new indexer(db,stopWords,col,oldFiles);
        ArrayList<Thread>threads=new ArrayList<Thread>();
        System.out.println("Enter number of threads in indexer:");
        Scanner sc = new Scanner(System.in);
        Integer th = sc.nextInt();
        for(Integer i=0;i<th;++i){
            threads.add(new Thread( new Master(ind),"T"+i.toString()));
            threads.get(i).start();
        }

    }
}

