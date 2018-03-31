import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.jsoup.Jsoup;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class indexer extends indexing {
    MongoDatabase db;
    MongoCollection col;
    Map<String,Boolean> oldFiles;
    public static String newFolderName="C:\\Users\\ghada\\Documents\\new";
    public static String oldFolderName="C:\\Users\\ghada\\Documents\\old";
    File newFolder = new File(newFolderName);
    public indexer( MongoDatabase database, Map<String, Integer> stopWords,MongoCollection c,Map<String,Boolean> of){
        super(stopWords);
        db=database;
        col = c;
        oldFiles=of;
    }

    public synchronized String getName(Map<String,Boolean> old) throws  InterruptedException {
        String name="";
        for (File fileEntry : newFolder.listFiles()){
            name=fileEntry.getName();
            break;
        }
        if (name.equals(""))
            return name;
        if (oldFiles.containsKey(name)) {
            System.out.println("mwgod");
            try {
                String f = oldFolderName + "\\" + name;
                readOld(old, f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else oldFiles.put(name,true);
        try {
            Path temp = Files.move
                    (Paths.get(newFolderName+"\\"+name),
                            Paths.get(oldFolderName+"\\"+name),REPLACE_EXISTING);
        }
        catch (IOException e){
            System.out.println("Error in move files");
        }
        return name;
    }

    public void start() throws IOException, InterruptedException {
        File input=null;
        while (true){
            Map<String,Boolean> old=new HashMap<>();
            String name = "";
            name = getName(old);
            input = new File(oldFolderName+"\\"+name);
            if (!name.equals("")){
                Map<String, List<Integer>> mp =new HashMap<>();
                Map<String, Double> tf =  super.Index(input,mp);
                String id=name.substring(0,name.length()-5);
                addWordsToDB(mp,old, Integer.parseInt(id), tf);
            }
        }
    }
    public void readOld(Map<String,Boolean> old,String in) throws IOException {
        File input=new File(in);
        org.jsoup.nodes.Document htmlDocument = Jsoup.parse(input, "UTF-8", "");
        String body = htmlDocument.body().text();
        String title = htmlDocument.title();
        String tmp = "";
        title = title.toLowerCase();
        for (int i = 0; i < title.length(); ++i) {
            char c = title.charAt(i);
            if (c == ',' || c == '"' || c == '+' || c == '-' || c == '}' ||
                    c == '/' || c == '*' || c == '(' || c == ')' || c == '{' ||
                    c == '!' || c == ':' || c == '?' || c == '[' || c == ']' ||
                    c == '&' || c == '@' || c == ' ' || c=='.') {
                if (tmp.equals("") || super.isStopWord(tmp)) {
                    tmp = "";
                    continue;
                }
                addOldWords(old, tmp);
                tmp = "";
            } else tmp += c;
        }
        if (!tmp.equals(""))
            addOldWords(old, tmp);

        String cont = body;
        cont=cont.toLowerCase();
        tmp = "";
        for (int i = 0; i < cont.length(); ++i) {
            char c = cont.charAt(i);
            if (c == ',' || c == '"' || c == '+' || c == '-' || c == '}' ||
                    c == '/' || c == '*' || c == '(' || c == ')' || c == '{' ||
                    c == '!' || c == ':' || c == '?' || c == '[' || c == ']' ||
                    c == '&' || c == '@' || c == ' ' || c == '.') {
                if (tmp.equals("") || super.isStopWord(tmp)) {
                    tmp="";continue;
                }
                addOldWords(old, tmp);
                tmp = "";
            } else tmp += c;
        }
        if (!tmp.equals(""))
            addOldWords(old, tmp);
        // add words indices to DB
    }
    public static void addOldWords(Map<String, Boolean> mp,String str) throws IOException {
        str = str.replaceAll("[^A-Za-z0-9]", "");
        mp.put(str,true);
    }


    public void addWordsToDB(Map<String, List<Integer>> mp,Map<String, Boolean> old, Integer docID,Map<String, Double> tf) throws InterruptedException {
        Iterator it = mp.entrySet().iterator();
        List<WriteModel<Document>> writes = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if (pair.getKey().toString().length()<1){
                it.remove();  continue;
            }
            String str=pair.getKey().toString();
            List<Integer> ls= (List<Integer>) pair.getValue();

            if (old.containsKey(str)){
                Document mtch1=new Document("doc.docNum",docID);
                mtch1.append("word",str);
                Document updateIdx= new Document("$set",new Document("doc.$.idx",ls));
                Document updateTf= new Document("$set",new Document("doc.$.tf",tf.get(str)));
                writes.add(new UpdateOneModel<>(mtch1,updateIdx,new UpdateOptions()));
                writes.add(new UpdateOneModel<>(mtch1,updateTf,new UpdateOptions()));
                old.remove(str);
            }else {
                Document mtch1 = new Document("word", pair.getKey().toString());
                mtch1.append("stemmed",super.pa.stripAffixes(pair.getKey().toString()));
                Document doc = new Document("docNum", docID).append("idx", ls).append("tf", tf.get(pair.getKey()));
                Document pushID = new Document("$push", new Document("doc", doc));
                writes.add(new UpdateOneModel<>(mtch1,pushID,new UpdateOptions().upsert(true)));
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        it = old.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            if (pair.getKey().toString().length()<1){
                it.remove();  continue;
            }
            String str=pair.getKey().toString();
            Document ob0 = new Document("docNum",docID);
            BasicDBObject ob1 = new BasicDBObject("doc",ob0);
            BasicDBObject rem = new BasicDBObject("$pull",ob1);
            BasicDBObject matchObject = new BasicDBObject("word",str);
            writes.add(new UpdateOneModel<>(matchObject,rem));
        }
        synchronized (col){
            col.bulkWrite(writes,new BulkWriteOptions().ordered(false));
        }
    }
}
