/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.storm.Testing;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import org.junit.Test;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;

/**
 *
 * @author Tomas
 */
class Collections {
        public static <T> List<T> asList(T ... items) {
            List<T> list = new ArrayList<T>();
            for (T item : items) {
                list.add(item);
            }
            return list;
        }
}

@RunWith(Parameterized.class)
@PrepareForTest({RecommendationEvaluationBolt.class, java.io.*})
public class RecommendationEvaluationBoltTest {
    
    @Parameters
    public static Collection<Object[]> data() {
        return Collections.asList(new Object[][] {     
                 { Collections.asList(1, 2, 3, 4, 5, 6),Collections.asList(1, 2, 4, 6, 7,8,9,10), 4.0/6.0},
                 { Collections.asList(1, 3), Collections.asList(1), 0.5 }, 
                 { Collections.asList(5), Collections.asList(6), 0.0}  
           });
    }
    
    List<Integer> recs;
    List<Integer> testWindow;
    Double expectedResult;

    public RecommendationEvaluationBoltTest(List<Integer> recs, List<Integer> testWindow, Double expectedResult) {
        this.recs = recs;
        this.testWindow = testWindow;
        this.expectedResult = expectedResult;
    }
        
    
    @Before
    public void setUp() {
        
               
    }
    
    @After
    public void tearDown() {
    }

   

    /**
     * Test of execute method, of class RecommendationEvaluationBolt.
     */
    @Test
    public void testExecuteFromPreprocessingBoltShouldCalculatePrecisionMetricAndAppendToLogFile() {
     
        //gid, sid, task, test
        Tuple tuple = Testing.testTuple(new Values(1.0, 2, "REC_EVAL", testWindow));
                
        RecommendationEvaluationBolt spyBolt = spy(new RecommendationEvaluationBolt());
        
        spyBolt.cacheRecs(2,recs);
        spyBolt.execute(tuple);
                
        verify(spyBolt).calculatePrecision(Matchers.anyList(), eq(testWindow));
        // stores results of precision, first arg is group id, second precision metric for session
        verify(spyBolt).storeResults(eq(2), eq(1.0), anyDouble());
        
    }
    
    /**
     * Test of execute method, of class RecommendationEvaluationBolt.
     */
    @Test
    public void testExecuteFromRecommendationBoltShouldCacheRecs() {
     
        //gid, sid, task, recs 
        Tuple tuple = Testing.testTuple(new Values(1.0, 2, "REC_CACHE", recs));
                
        RecommendationEvaluationBolt spyBolt = spy(new RecommendationEvaluationBolt());
        
        spyBolt.execute(tuple);
        
        verify(spyBolt, times(1)).cacheRecs(2,recs);
        verify(spyBolt, times(0)).calculatePrecision(Matchers.anyList(), eq(testWindow));
        verify(spyBolt, times(0)).storeResults(eq(2), eq(1.0), anyDouble());
        
    }

    /**
    * Test of generate recommendations method, of class RecommendationBolt.
    */
    @Test
    public void testCalculatePrecisionShouldCalculateRightPrecisionMetricResult() {
       
       RecommendationEvaluationBolt spyBolt = new RecommendationEvaluationBolt();
       double prec = spyBolt.calculatePrecision(recs, testWindow);
       assertEquals((Double)prec,(Double)expectedResult);
       
    }
    
     /**
    * Test of generate recommendations method, of class RecommendationBolt.
    */
    @Test
    public void testStoreResultsShouldStoreGivenResultsToFileForGivenGroupWithTimestampAndTransactionId() {
        
        PrintWriter mockWriter = mock(PrintWriter.class, withSettings().serializable());
        try {
            PowerMockito.whenNew(PrintWriter.class).withAnyArguments().thenReturn(mockWriter);
        } catch (Exception ex) {
            Logger.getLogger(RecommendationEvaluationBoltTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        RecommendationEvaluationBolt spyBolt = spy(new RecommendationEvaluationBolt());
        double prec = spyBolt.calculatePrecision(recs, testWindow);
        spyBolt.storeResults(2, 1.0, prec);
        verify(mockWriter, times(1)).println("2,1.0,"+prec);
        
    }
   
    
}
