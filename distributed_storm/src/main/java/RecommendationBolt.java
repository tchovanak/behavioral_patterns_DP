
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
    
    JedisPool pool;
    
    Jedis jedis;
    
    private OutputCollector collector;

    public RecommendationBolt() {
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
        jedis = pool.getResource();
    }

    @Override
    public void execute(Tuple tuple) {
        // get last global TS
        byte[] resultsGlobal = jedis.get("SFCIS_GLOBAL".getBytes());
        if(resultsGlobal != null){
            List<FrequentItemset> globItemsets = this.deSerialize(resultsGlobal);
            for(FrequentItemset fi : globItemsets){
                System.out.println(fi.toString());
            }
        }
        // get last group TS
        byte[] resultsGroup = jedis.get("SFCIS_GROUP".getBytes());
        if(resultsGroup != null){
            List<FrequentItemset> groupItemsets = this.deSerialize(resultsGroup);
            for(FrequentItemset fi : groupItemsets){
                System.out.println(fi.toString());
            }
        }
        
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
