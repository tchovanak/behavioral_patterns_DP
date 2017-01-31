
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import moa.core.FrequentItemset;
import moa.core.PPSDM.FciValue;
import moa.core.PPSDM.utils.MapUtil;
import moa.core.PPSDM.utils.UtilitiesPPSDM;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
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
    public RecommendationBolt(JedisPool pool, int ews, OutputCollector oc) {
        this.ews = ews;
        this.pool = pool;
        jedis = pool.getResource(); 
        this.collector = oc;
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
        // get id of session 
        Integer sid = tuple.getInteger(3);
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
        
        if((ews >= (instance.size()-2))){ 
            return; // this is when session array is too short - it is ignored.
        }
        
        RecommendationResults recs = generateRecommendations(gid,ew,globItemsets,groupItemsets, 5);
        
        // send recommendations to evaluation bolt
        collector.emit("streamEval",new Values(recs, sid));
        
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
        ofd.declareStream("streamEval", new Fields("recs"));
       
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }

    
    public RecommendationResults generateRecommendations(Double gid, List<Integer> ew, 
            List<FrequentItemset> globalPatternsDES, 
            List<FrequentItemset> groupPatternsDES, 
            int rc) {
        //to get all fcis found 
        List<FciValue> mapFciWeightGlobal = new LinkedList<>();
        Iterator<FrequentItemset> itFis = globalPatternsDES.iterator();
        while(itFis.hasNext()){
            FrequentItemset fi = itFis.next();
            if(fi.getSize() > 1){
                List<Integer> items = fi.getItems();
                double hitsVal = this.computeSimilarity(items,ew);
                if(hitsVal == 0.0){ continue; }
                FciValue fciVal = new FciValue();
                fciVal.setItems(fi.getItems());
                fciVal.computeValue(hitsVal, fi.getSupportDouble());
                mapFciWeightGlobal.add(fciVal);
            }
        }
        List<FciValue> mapFciWeightGroup = new LinkedList<>();
        if(gid >= 0.0){
            Iterator<FrequentItemset> itFisG = groupPatternsDES.iterator();
            while(itFisG.hasNext()){
                FrequentItemset fi = itFisG.next();
                if(fi.getSize() > 1){
                    List<Integer> items = fi.getItems();
                    double hitsVal = this.computeSimilarity(items,ew);
                    if(hitsVal == 0.0){ continue; }
                    FciValue fciVal = new FciValue();
                    fciVal.setItems(fi.getItems());
                    fciVal.computeValue(hitsVal, fi.getSupportDouble());
                    mapFciWeightGroup.add(fciVal);
               }
            }
        }
        // all fcis found have to be sorted descending by its support and similarity.
        Collections.sort(mapFciWeightGlobal);
        Collections.sort(mapFciWeightGroup);
        List<Integer> recs = generateRecsVoteStrategy(mapFciWeightGlobal, mapFciWeightGroup, ew, rc);
        RecommendationResults results = new RecommendationResults();
        results.setNumOfRecommendedItems(rc);
        results.setRecommendations(recs);
        return results;
        
    }
    
    private List<Integer> generateRecsVoteStrategy(
                                    List<FciValue> mapFciWeightGlobal,
                                    List<FciValue> mapFciWeightGroup, 
                                    List<Integer> ew, int rc){
        Map<Integer, Double> mapItemsVotes = new HashMap<>();
        Iterator<FciValue> itGlobal = mapFciWeightGlobal.iterator();
        Iterator<FciValue> itGroup = mapFciWeightGroup.iterator();
        while(itGlobal.hasNext() || itGroup.hasNext()){
            if(itGlobal.hasNext()){
               FciValue fci = itGlobal.next();
               Iterator<Integer> itFciItems = fci.getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();         
                   if(mapItemsVotes.containsKey(item)){   
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport();
                       mapItemsVotes.put(item, newVal);
                   }else{
                       if(!ew.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport();
                           mapItemsVotes.put(item, newVal);
                       }
                   }
               }
            }
            if(itGroup.hasNext()){
               FciValue fci = itGroup.next();
               Iterator<Integer> itFciItems = fci.getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();
                   if(mapItemsVotes.containsKey(item)){
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport();
                       mapItemsVotes.put(item, newVal);
                   }else{
                       if(!ew.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport();
                           mapItemsVotes.put(item, newVal);
                       }
                   }
               }
            }
        }
        mapItemsVotes = MapUtil.sortByValue(mapItemsVotes);
        int cntAll = 0;
        List<Integer> recsCombined = new ArrayList<>();
        for(Map.Entry<Integer,Double> e : mapItemsVotes.entrySet()) {
            Integer item = e.getKey();
            recsCombined.add(item);
            cntAll++;
            if(cntAll >= rc){
                break;
            }       
        }
        return recsCombined;
        
    }

    private double computeSimilarity(List<Integer> items, List<Integer> window) {
        return ((double)UtilitiesPPSDM.computeLongestCommonSubset(items,window)) / 
                ((double)window.size());
    }
       
}
