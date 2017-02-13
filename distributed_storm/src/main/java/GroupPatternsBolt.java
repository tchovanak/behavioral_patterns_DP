
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import core.FrequentItemset;
import core.PPSDM.SegmentPPSDM;
import ppsdm.core.PPSDM.charm.Itemset;
import ppsdm.learners.StormIncMine;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import static org.apache.storm.topology.BasicBoltExecutor.LOG;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 
 * @author Tomas Chovanak
 */
public class GroupPatternsBolt  implements IRichBolt  {
    
    /** The queue holding tuples in a batch. */
    protected LinkedBlockingQueue<Tuple> queue = new LinkedBlockingQueue<>();
    
    /** The threshold after which the batch should be flushed out. */
    int batchSize = 25;
    
    int counter = 0;
    
    int groupid;
    
    /**
    * The batch interval in sec. Minimum time between flushes if the batch sizes
    * are not met. This should typically be equal to
    * topology.tick.tuple.freq.secs and half of topology.message.timeout.secs
    */
    int batchIntervalInSec = 45;
    
    /** The last batch process time seconds. Used for tracking purpose */
    long lastBatchProcessTimeSeconds = 0;
    
    StormIncMine sincmine;
    
    JedisPool pool;
    
    Jedis jedis;
    
    private OutputCollector collector;

    public GroupPatternsBolt() {
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
//        pool = new JedisPool(new JedisPoolConfig(), 
//                "ppsdmcache.redis.cache.windows.net", 
//                6379,
//                1000, 
//                "u60CWY5OXG22FEA9K6iwGSiIi2OSdHSsz3mFrRbA+oM=");
        pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
        this.sincmine = new StormIncMine(15,10,0.05, 0.1, 25);
        jedis = pool.getResource();
    }

    @Override
    public void execute(Tuple tuple) {
        if (TupleHelpers.isTickTuple(tuple)) {
            // If so, it is indication for batch flush. But don't flush if previous
            // flush was done very recently (either due to batch size threshold was
            // crossed or because of another tick tuple
            //
            if ((System.currentTimeMillis() / 1000 - lastBatchProcessTimeSeconds) >= batchIntervalInSec) {
                LOG.debug("Current queue size is " + this.queue.size()
                + ". But received tick tuple so executing the batch");
                finishBatch();
            } else {
                LOG.debug("Current queue size is " + this.queue.size()
                + ". Received tick tuple but last batch was executed "
                + (System.currentTimeMillis() / 1000 - lastBatchProcessTimeSeconds)
                + " seconds back that is less than " + batchIntervalInSec
                + " so ignoring the tick tuple");
            
            }
        }else{
            // Add the tuple to queue. But don't ack it yet.
            this.queue.add(tuple);
            int queueSize = this.queue.size();
            LOG.debug("current queue size is " + queueSize);
            if (queueSize >= batchSize) {
              LOG.debug("Current queue size is >= " + batchSize
                  + " executing the batch");
              finishBatch();
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

    
    public void finishBatch() {
        LOG.debug("Finishing batch of size " + queue.size());
        lastBatchProcessTimeSeconds = System.currentTimeMillis() / 1000;
        List<Tuple> tuples = new ArrayList<>();
        queue.drainTo(tuples);
        
        // Perform INCMINE with collected segment
        
        SegmentPPSDM segment= new SegmentPPSDM(sincmine.getMin_sup(),10);
        for(Tuple t: tuples){
            Itemset itemset  = new Itemset();
            List<Double> list = (List<Double>)t.getValue(2);
            for(Double val : list){
                itemset.addItem((int)Math.round(val));
            }
            segment.addItemset(itemset);
        }
        
        sincmine.update(segment, tuples.get(0).getDouble(0), 15);
        // FINALLY ACK BACK TO SPOUT
        collector.ack(tuples.get(tuples.size()-1));
//        //store to redis
        List<FrequentItemset> sfcis = sincmine.getFciTable().getSemiFcis();
        Collections.sort(sfcis);
        
        
        //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String key = "SFCIS_GID=" + tuples.get(0).getDouble(0);
        byte[] bytes = this.serialize(sfcis);
        jedis.set(key.getBytes(), bytes);
        //byte[] results = jedis.get(key.getBytes());
        //List<FrequentItemset> res = this.deSerialize(results);
        
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
