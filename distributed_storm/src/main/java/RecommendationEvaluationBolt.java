
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.File;

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



/**
 * Applies found patterns in recommendation task
 * @author Tomas Chovanak
 */
public class RecommendationEvaluationBolt  implements IRichBolt  {
    
     
    private OutputCollector collector;
    
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

    void storeResults(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void storeResults(double d, double anyDouble) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
