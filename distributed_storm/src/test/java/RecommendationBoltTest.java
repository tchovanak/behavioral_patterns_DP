/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.FrequentItemset;
import org.apache.storm.Testing;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Matchers;
import static org.mockito.Mockito.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import static org.mockito.Matchers.*;
import org.mockito.Mockito;
/**
 *
 * @author Tomas
 */
public class RecommendationBoltTest {
    
    private byte[] globalPatterns;
    private byte[] groupPatterns;
    private List<FrequentItemset> globalPatternsDES;
    private List<FrequentItemset> groupPatternsDES;
    private Integer[][] testData;
    
    @Before
    public void setUp() {
        ClassLoader classLoader = getClass().getClassLoader();
        File fileGlobal = new File(classLoader.getResource("global").getFile());
        File fileGroup = new File(classLoader.getResource("group").getFile());
        try {
            globalPatterns = Files.readAllBytes(fileGlobal.toPath());
            groupPatterns = Files.readAllBytes(fileGroup.toPath());
        } catch (IOException ex) {
            Logger.getLogger(RecommendationBoltTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        globalPatternsDES = this.deSerialize(globalPatterns);
        groupPatternsDES = this.deSerialize(groupPatterns);
        
        testData = new Integer[][] {{437,460,521,530,1275,1389,1390,1417,1418,1925,1938},{191,1161},
                    {323,325,326,327,328,329,330,1719,1858},
                    {52,180,228,438,460,796,936,1347,1390,1654,1905,1925,2052}};
                
    }
    
    @After
    public void tearDown() {
    }

   

    /**
     * Test of execute method, of class RecommendationBolt.
     */
    @Test
    public void testExecuteShouldGetPatternsFromRedisCallToGenerateRecommendationsEmitToEvaluationBolt() {
        List<Double> instance = new ArrayList<>();
        for(Integer i : testData[0]){
            instance.add((double)i);
        }
        Tuple tuple = Testing.testTuple(new Values(1.0,437,instance));
        
        // create mocks for redis connection
        JedisPool mockJedisPool = mock(JedisPool.class);
        Jedis mockJedis = mock(Jedis.class);
        OutputCollector mockCollector = mock(OutputCollector.class);
        
        //stubbing
        when(mockJedisPool.getResource()).thenReturn(mockJedis);
        when(mockJedis.get("SFCIS_GID=1.0".getBytes())).thenReturn(groupPatterns);
        when(mockJedis.get("SFCIS_GLOBAL".getBytes())).thenReturn(globalPatterns);
        
        RecommendationBolt recBolt = Mockito.spy(new RecommendationBolt(mockJedisPool, 2, mockCollector));
        recBolt.execute(tuple);
        
        // verify interactions
        verify(mockJedis, times(1)).get("SFCIS_GID=1.0".getBytes());
        verify(mockJedis, times(1)).get("SFCIS_GLOBAL".getBytes());
        verify(mockCollector, times(1)).emit("streamEval",Matchers.any(Values.class));
        verify(recBolt, times(1)).generateRecommendations(Matchers.anyList(), Matchers.anyList(), Matchers.anyList(), Matchers.anyInt());
        
    }

    /**
    * Test of generate recommendations method, of class RecommendationBolt.
    */
    @Test
    public void testGenerateRecommendationsShouldReturnRightNumberOfRightRecommendations() {
        List<Integer> ew = new ArrayList<>();
        for(int i: testData[0]){
            ew.add(i);
            if(ew.size() == 2){
                break;
            }
        }
        RecommendationBolt recBolt = new RecommendationBolt(2);
        for(int i = 1; i < 10; i++){
            RecommendationResults results = recBolt.generateRecommendations(ew, globalPatternsDES, groupPatternsDES, i);
            assertTrue(results.getNumOfRecommendedItems() == i);
            assertTrue(results.getRecommendations().size() == results.getNumOfRecommendedItems());
        }      
        
        int i = -1;
        RecommendationResults results = recBolt.generateRecommendations(ew, globalPatternsDES, groupPatternsDES, i);
        assertTrue(results == null);
        
    }

    
    private List<FrequentItemset> deSerialize(byte[] buffer) {
        Kryo kryo = new Kryo();
        Input input = new Input(buffer);
        List<FrequentItemset> list = kryo.readObject(input, ArrayList.class);
        return list;
    }
}
