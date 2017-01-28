
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.FrequentItemset;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Applies found patterns in recommendation task
 * @author Tomas Chovanak
 */
public class RecommendationBolt  implements IRichBolt  {
    
    JedisPool pool;
    
    Jedis jedis;
    
    private OutputCollector collector;
    
    private int counter = 0;
    
    private int ews = 1;
    
    private RecommendationGenerator recGen;
    
    private RecommendationEvaluator recEval;

    /**
     * 
     * @param ews - Evaluation window size 
     */
    public RecommendationBolt(int ews) {
        this.ews = ews;
        this.recGen = new RecommendationGenerator();
        this.recEval = new RecommendationEvaluator();
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
        jedis = pool.getResource();
    }

    
    @Override
    public void execute(Tuple tuple) {

        // get instance - session for which recommendations should be generated
        // from tuple
        List<Double> instance = (List<Double>)tuple.getValue(1);
        // get uid of session
        Integer uid = tuple.getInteger(0);
        // get last global TS
        byte[] resultsGlobal = jedis.get("SFCIS_GLOBAL".getBytes());
        List<FrequentItemset> globItemsets = null;
        if(resultsGlobal != null){
            globItemsets = this.deSerialize(resultsGlobal);
        }
        // get last group TS
        byte[] resultsGroup = jedis.get(("SFCIS_GID=" + uid).getBytes());
        List<FrequentItemset> groupItemsets = null;
        if(resultsGroup != null){
            groupItemsets = this.deSerialize(resultsGroup);
        }
        counter++;
        if(counter > 2000  && resultsGroup != null){
            try {
                FileOutputStream fos = new FileOutputStream("G:\\global");
                fos.write(resultsGlobal);
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(GlobalPatternsBolt.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                FileOutputStream fos = new FileOutputStream("G:\\group");
                fos.write(resultsGroup);
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(GlobalPatternsBolt.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        

//        
//        // get evaluation window and testing part from instance
//        List<Integer> ew = new ArrayList<>(); // items inside window 
//        List<Integer> tw = new ArrayList<>(); // items out of window 
//        
//        if((ews >= (instance.size()-2))){ 
//            return; // this is when session array is too short - it is ignored.
//        }
//        
//        RecommendationResults recs = recGen.generateRecommendations(ew,globItemsets,groupItemsets);
//        
//        recEval.addRecommendationResults(recs);
        
    }
    
    private <T> byte[] serialize(List<T> objectsToSerialize) {
        Kryo kryo = new Kryo();
        Output output = new Output();
        byte[] buffer = new byte[1024*1024];
        output.setBuffer(buffer, 1024*1024);
        kryo.writeObject(output, objectsToSerialize);
        byte[] ret = output.toBytes();
        output.close();
        return ret;
    }
    
    private List<FrequentItemset> deSerialize(byte[] buffer) {
        Kryo kryo = new Kryo();
        Input input = new Input(buffer);
        List<FrequentItemset> list = kryo.readObject(input, ArrayList.class);
        return list;
    }
    
    @Override
    public void cleanup() {
        
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer ofd) {
        ofd.declareStream("streamGroup0", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup1", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup2", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup3", new Fields("gid","uid","items"));
   
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
    
}
