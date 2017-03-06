package topology;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;



/**
 * Applies found patterns in recommendation task
 * @author Tomas Chovanak
 */
public class RecommendationEvaluationBolt  implements IRichBolt  {
    
     
    private OutputCollector collector;
    
    private Map<Integer,List<Integer>> SessionRecsMap = new HashMap<>();
    private int ews = 2;
     
    /**
     * 
     */
    public RecommendationEvaluationBolt(int ews) {
      this.ews = ews;
    }

    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        
    }

    
    @Override
    public void execute(Tuple tuple) {
        String task = tuple.getString(2);
        int sid = tuple.getInteger(1);
        double gid = tuple.getDouble(0);
        
        List<Integer> items = (List<Integer>)tuple.getValue(3);
        if(task.equals("REC_EVAL")){
            // get test window 
            if(items.size() <= ews){
                return;
            }
            List<Integer> test = items.subList(ews, items.size());
            // find corresponding recs
            List<Integer> recs = this.SessionRecsMap.get(sid);
            if(recs != null){
               double prec = this.calculatePrecision(recs, test);
               this.storeResults(sid,gid,prec);
            }
            
        }else{
            this.cacheRecs(sid, items);
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

    double calculatePrecision(List<Integer> recs, List<Integer> testWindow) {
        List<Integer> hits = new ArrayList<>(recs);
        hits.retainAll(testWindow);
        return (double)hits.size()/(double)recs.size();
    }
    
    void storeResults(int sid, double gid, double precision) {
        try(FileWriter fw = new FileWriter("log_prec_gid_" + gid, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(sid+","+gid+","+precision);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    void cacheRecs(int sid, List<Integer> recs) {
        this.SessionRecsMap.put(sid, recs);
    }
    
 
 
}
