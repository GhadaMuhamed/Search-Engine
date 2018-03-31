import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.*;

public class indexing {

    Map<String, Integer> stopWords;
    public indexing( Map<String, Integer> mapStopWords){
        stopWords=mapStopWords;
    }
    Map<String, Integer> importance = new HashMap<>();
    static PorterAlgo pa = new PorterAlgo();

    public void addHeader(int val, String s) {
        String tmp = "";
        s=s.toLowerCase();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ',' || c == '"' || c == '+' || c == '-' || c == '}' ||
                    c == '/' || c == '*' || c == '(' || c == ')' || c == '{' ||
                    c == '!' || c == ':' || c == '?' || c == '[' || c == ']' ||
                    c == '&' || c == '@' || c == ' ') {
                if (tmp.equals("") || isStopWord(tmp)) {
                    tmp = "";
                    continue;
                }

                if (!importance.containsKey(tmp))
                    importance.put(tmp, val);
                tmp = "";
            } else tmp += c;
        }
        if (!tmp.equals("")) {
            if (!importance.containsKey(tmp))
                importance.put(tmp, val);
        }
    }
    public  Boolean isStopWord(String s){
        return stopWords.containsKey(s);
    }
    public  Map<String,Double> calculateTf(Map<String, List<Integer>> mp){
        Double dom = 0.0;
        Map <String,Double> ret = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : mp.entrySet())
            dom +=entry.getValue().size()*entry.getValue().size();
        dom=Math.sqrt(dom);
        for (Map.Entry<String, List<Integer>> entry : mp.entrySet()) {
            Integer num=1;
            if (importance.get(entry.getKey()) !=null)
                num=importance.get(entry.getKey());
            ret.put(entry.getKey(), (entry.getValue().size() / dom) * num);
        }
        return ret;
    }

    public  Map<String,Double> Index( File input,Map<String, List<Integer>> mp) {
        org.jsoup.nodes.Document htmlDocument = null;
        try {
            htmlDocument = Jsoup.parse(input, "UTF-8", "");
        } catch (IOException e) {
            System.out.println("can't parse in index document : " + input.getName());
        }
        if (htmlDocument== null)
            return new HashMap<>();
        String body = htmlDocument.body().text();
        //System.out.println(body);
        String title = htmlDocument.title();
        String tmp = "";
        title = title.toLowerCase();
        for (int i = 0; i < title.length(); ++i) {
            char c = title.charAt(i);
            if (c == ',' || c == '"' || c == '+' || c == '-' || c == '}' ||
                    c == '/' || c == '*' || c == '(' || c == ')' || c == '{' ||
                    c == '!' || c == ':' || c == '?' || c == '[' || c == ']' ||
                    c == '&' || c == '@' || c == ' ' || c=='.') {
                if (tmp.equals("") || isStopWord(tmp)) {
                    tmp = "";
                    continue;
                }
                addWordsToMap(mp, tmp, i);
                if (!importance.containsKey(tmp))
                    importance.put(tmp, 8);
                tmp = "";
            } else tmp += c;

        }
        if (!tmp.equals("")) {
            addWordsToMap(mp, tmp, title.length());
            if (!importance.containsKey(tmp))
                importance.put(tmp, 8);
        }
        Elements h1Tags = htmlDocument.select("h1");
        String sH1 = h1Tags.text();
        addHeader(2, sH1);
        // Group of all h2-Tags
        Elements h2Tags = htmlDocument.select("h2");
        String sH2 = h2Tags.text();
        addHeader(3, sH2);
        Elements h3Tags = htmlDocument.select("h3");
        String sH3 = h3Tags.text();
        addHeader(4, sH3);
        // Group of all h2-Tags
        Elements h4Tags = htmlDocument.select("h4");
        String sH4 = h4Tags.text();
        addHeader(5, sH4);
        Elements h5Tags = htmlDocument.select("h5");
        String sH5 = h5Tags.text();
        addHeader(6, sH5);
        // Group of all h2-Tags
        Elements h6Tags = htmlDocument.select("h6");
        String sH6 = h1Tags.text();
        addHeader(7, sH6);
        String cont = body;
        cont=cont.toLowerCase();
        tmp = "";
        for (int i = 0; i < cont.length(); ++i) {
            char c = cont.charAt(i);
            if (c == ',' || c == '"' || c == '+' || c == '-' || c == '}' ||
                    c == '/' || c == '*' || c == '(' || c == ')' || c == '{' ||
                    c == '!' || c == ':' || c == '?' || c == '[' || c == ']' ||
                    c == '&' || c == '@' || c == ' ' || c == '.') {
                if (tmp.equals("") || isStopWord(tmp)) {tmp="";continue;}
                //tmp = tmp.toLowerCase();

                addWordsToMap(mp, tmp, i + title.length() + 1);
                if (!importance.containsKey(tmp))
                    importance.put(tmp, 1);
                tmp = "";
            } else tmp += c;
        }
        if (!tmp.equals(""))
            addWordsToMap(mp, tmp, cont.length() + title.length() + 1);
        // add words indices to DB
        return calculateTf(mp);
    }
    public static void addWordsToMap(Map<String, List<Integer>> mp,String str,Integer idx)  {
        //str = str.replaceAll("[^A-Za-z0-9]", "");
        if (!mp.containsKey(str))
            mp.put(str,new ArrayList<>());
        mp.get(str).add(idx - str.length());
    }


}
