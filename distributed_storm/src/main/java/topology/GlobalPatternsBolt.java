package topology;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import core.FrequentItemset;
import core.PPSDM.SegmentPPSDM;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import moa.core.TimingUtils;
import ppsdm.core.PPSDM.charm.Itemset;
import ppsdm.learners.StormIncMine;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import static org.apache.storm.topology.BasicBoltExecutor.LOG;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 
 * @author Tomas Chovanak
 */
public class GlobalPatternsBolt  implements IRichBolt  {
    
    /** The queue holding tuples in a batch. */
    protected LinkedBlockingQueue<Tuple> queue = new LinkedBlockingQueue<>();
    
    /** The threshold after which the batch should be flushed out. */
    int batchSize;
    
    int counter = 0;
    
    int groupid;
    
    StringBuilder log = new StringBuilder();
    
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
    private long start = 0;
    private OutputCollector collector;

    public GlobalPatternsBolt() {
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        if(Configuration.LOCAL){
             pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
        }else{
            pool = new JedisPool(new JedisPoolConfig(), 
                Configuration.redishost, 
                Configuration.redisport,
                1000, 
                Configuration.redisAK);
        }
        this.sincmine = new StormIncMine(Configuration.ws,Configuration.mil,Configuration.ms,
                Configuration.rr,Configuration.fsl);
        batchSize = Configuration.fsl;
        jedis = pool.getResource();
    }

    @Override
    public void execute(Tuple tuple) {
        counter++;
        //System.out.println("GLOBAL " + counter + "Thread.currentThread().getId()" + Thread.currentThread().getId());
        if(start == 0){
            TimingUtils.enablePreciseTiming();
            start = TimingUtils.getNanoCPUTimeOfCurrentThread();
        }
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
        byte[] ret;
        try (Output output = new Output()) {
            byte[] buffer = new byte[1024*1024];
            output.setBuffer(buffer, 1024*1024);
            kryo.writeObject(output, objectsToSerialize);
            ret = output.toBytes();
        }
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
        //"uid","items", "task", "sid"
        for(Tuple t: tuples){
            Itemset itemset  = new Itemset();
            List<Integer> list = (List<Integer>)t.getValue(1);
            for(Integer val : list){
                itemset.addItem(val);
            }
            segment.addItemset(itemset);
        }
        
        sincmine.update(segment, tuples.get(0).getInteger(0), 15);
        // ack back to spout
        collector.ack(tuples.get(tuples.size()-1));
        //store to redis
        List<FrequentItemset> sfcis = sincmine.getFciTable().getSemiFcis();
        Collections.sort(sfcis);
        String key = "SFCIS_GLOBAL";
        byte[] bytes = this.serialize(sfcis);
        jedis.set(key.getBytes(), bytes);
        jedis.expire(key, 2);
        try {
            outputToFileForEvaluation();
        } catch (StorageException | IOException ex) {
            Logger.getLogger(GlobalPatternsBolt.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void cleanup() {
        
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer ofd) {
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
    
    private void outputToFileForEvaluation() throws StorageException, IOException  {
          if(counter % 15000 == 0){
                long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double tp = ((double)(end - start) / 1e9);
                double transsec = counter/tp;
                if(Configuration.LOCAL){
                    try(FileWriter writer = new FileWriter("g:\\workspace_DP2\\results_grid\\results_stream_global.csv", true);) {
                        writer.append(""+(counter)+","+tp+","+transsec+"\n");
                    } catch (IOException ex) {
                        Logger.getLogger(RecommendationBolt.class.getName()).log(Level.SEVERE, null, ex);
                    }  
                }else{
                    log.append(counter).append(",").append(tp).append(",").append(transsec).append("\n");
                   
                    try {
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(
                                Configuration.storageConnectionString);
                        CloudFileClient fileClient = storageAccount.createCloudFileClient();
                        // Get a reference to the file share
                        CloudFileShare share = fileClient.getShareReference("alefinput");
                        CloudFileDirectory rootDir = share.getRootDirectoryReference();
                        CloudFile cloudFile = rootDir.getFileReference("log.txt");
                        cloudFile.uploadText(log.toString());
                    } catch (URISyntaxException | InvalidKeyException ex) {
                        Logger.getLogger(SessionsInputSpout.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
          }
    }
    
}
