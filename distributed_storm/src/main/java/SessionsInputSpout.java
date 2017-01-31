
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        BufferedReader fileReader = null;
        try {
            InputStream fileStream = new FileInputStream("g:\\workspace_DP2\\Preprocessing\\alef\\alef_sessions_aggregated.csv");
            fileReader = new BufferedReader(new InputStreamReader(
                    fileStream));
        } catch (FileNotFoundException ex) {
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
