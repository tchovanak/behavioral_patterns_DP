
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
    
    /**
     * 
     */
    public RecommendationEvaluationBolt() {
      
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
            // find corresponding recs
            List<Integer> recs = this.SessionRecsMap.get(sid);
            if(recs != null){
               double prec = this.calculatePrecision(recs, items);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    void storeResults(int sid, double gid, double precision) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void cacheRecs(int i, List<Integer> recs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

  

}
