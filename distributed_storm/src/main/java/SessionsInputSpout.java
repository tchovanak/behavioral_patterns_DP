
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichSpout;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import java.util.concurrent.TimeUnit;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Tomas
 */
public class SessionsInputSpout implements IRichSpout {

    //Create instance for SpoutOutputCollector which passes tuples to bolt.
    private SpoutOutputCollector collector;
    private boolean completed = false;
    private int ews = 1;
    private int id = 0;
    private transient CloudStorageAccount storageAccount; 
    
     //Create instance for TopologyContext which contains topology data.
    private TopologyContext context;

    public SessionsInputSpout(int ews) {
       ews = 2;
       id = 0;
       
       
    }
    
    @Override
    public void open(Map map, TopologyContext tc, SpoutOutputCollector soc) {
        this.context = tc;
        this.collector = soc;
       
    }
    
     @Override
    public void nextTuple() {
        // session id - in future unique value generated 
        id++;
        // Use the CloudStorageAccount object to connect to your storage account
        
        BufferedReader fileReader = null;
        try {
            storageAccount = CloudStorageAccount.parse(
                    PPSDMStormTopology.storageConnectionString);
        } catch (URISyntaxException ex) {
            Logger.getLogger(SessionsInputSpout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SessionsInputSpout.class.getName()).log(Level.SEVERE, null, ex);
        }
        CloudFileClient fileClient = storageAccount.createCloudFileClient();
        try {
            // Get a reference to the file share
            CloudFileShare share = fileClient.getShareReference("input");
            //Get a reference to the root directory for the share.
            CloudFileDirectory rootDir = share.getRootDirectoryReference();
            //Get a reference to the file you want to download
            CloudFile file = rootDir.getFileReference("alef_sessions_aggregated.csv");
            //InputStream fileStream = new FileInputStream("g:\\workspace_DP2\\Preprocessing\\alef\\alef_sessions_aggregated.csv");
            InputStream fileStream = file.openRead();//new URL("https://ppsdmclusterstore.file.core.windows.net/input/alef_sessions_aggregated.csv").openStream();
        
            fileReader = new BufferedReader(new InputStreamReader(
                fileStream));
        } catch (URISyntaxException ex) {
            Logger.getLogger(SessionsInputSpout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (StorageException ex) {
            Logger.getLogger(SessionsInputSpout.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(fileReader == null){return;}
        try {
            String line;
            int in = 0;
            while((line = fileReader.readLine()) != null){
                in++;
                String[] lineSplitted = line.split(",");
                List<Double> values = new ArrayList<>();
                List<Double> valuesEW = new ArrayList<>();
                
                int uid = (int)Math.round(Double.parseDouble(lineSplitted[0].trim()));
                for(int i = 1; i < lineSplitted.length; i++){
                    Double val = Double.parseDouble(lineSplitted[i].trim());
                    values.add(val);
                    valuesEW.add(val);
                    if(valuesEW.size() == ews){
                        this.collector.emit(new Values(uid,valuesEW,"RECOMMENDATION",id));
                    }
                }
                this.collector.emit(new Values(uid,values,"LEARNING",id));
                System.out.println(in);
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SessionsInputSpout.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("END OF FILE");
            fileReader.close();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "SessionFileStream failed to read instance from stream.", ex);
        }
    }
    
    @Override
    public void declareOutputFields(OutputFieldsDeclarer ofd) {
        ofd.declare(new Fields("uid","items", "task", "sid"));
    }

    @Override
    public void close() {
        
    }

    @Override
    public void activate() {
        
    }

    @Override
    public void deactivate() {
        
    }

   

    @Override
    public void ack(Object o) {
       
    }

    @Override
    public void fail(Object o) {
       
    }

    

    @Override
    public Map<String, Object> getComponentConfiguration() {
       return null;
    }

   
}
