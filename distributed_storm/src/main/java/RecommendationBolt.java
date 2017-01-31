
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    
    private  JedisPool pool;
    
    private  Jedis jedis;
    
    private  OutputCollector collector;
    
    private  int counter = 0;
    
    private  int ews = 1;
    
    /**
     * 
     * @param ews - Evaluation window size 
     */
    public RecommendationBolt(JedisPool pool, int ews) {
        this.ews = ews;
        this.pool = pool;
        jedis = pool.getResource(); 
    }
    
    /**
     * 
     * @param ews - Evaluation window size 
     */
    public RecommendationBolt(int ews) {
        this.ews = ews;
        pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
        jedis = pool.getResource(); 
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        
    }

    
    @Override
    public void execute(Tuple tuple) {
        // get instance - session for which recommendations should be generated
        // from tuple
        List<Double> instance = (List<Double>)tuple.getValue(2);
        // get uid of session
        Integer uid = tuple.getInteger(1);
        // get gid of session 
        Double gid = tuple.getDouble(0);
        // get last global TS
        byte[] resultsGlobal = jedis.get("SFCIS_GLOBAL".getBytes());
        List<FrequentItemset> globItemsets = null;
        if(resultsGlobal != null){
            globItemsets = this.deSerialize(resultsGlobal);
        }
        // get last group TS
        byte[] resultsGroup = jedis.get(("SFCIS_GID=" + gid).getBytes());
        List<FrequentItemset> groupItemsets = null;
        if(resultsGroup != null){
            groupItemsets = this.deSerialize(resultsGroup);
        }
        counter++;
        
        // get evaluation window and testing part from instance
        List<Integer> ew = new ArrayList<>(); // items inside window 
        List<Integer> tw = new ArrayList<>(); // items out of window 
        
        if((ews >= (instance.size()-2))){ 
            return; // this is when session array is too short - it is ignored.
        }
        
        RecommendationResults recs = generateRecommendations(ew,globItemsets,groupItemsets, 5);
       
        
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



    RecommendationResults generateRecommendations(List<Integer> ew, List<FrequentItemset> globalPatternsDES, List<FrequentItemset> groupPatternsDES, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   
    
}
