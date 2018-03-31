import java.io.IOException;

public class Master extends Thread{
    indexer indexerObj;
    Master(indexer ind){
        indexerObj = ind;
    }
    public void run(){
            indexerObj.start();
    }

}

