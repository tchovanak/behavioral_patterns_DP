/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.storm.Testing;

import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.After;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import org.junit.Test;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
@PrepareForTest(RecommendationEvaluationBolt.class)
public class RecommendationEvaluationBoltTest {
    
    @Parameters
    public static Collection<Object[]> data() {
        return Collections.asList(new Object[][] {     
                 { Collections.asList(1, 2, 3, 4, 5, 6),Collections.asList(1, 2, 4, 6, 7,8,9,10), 4.0/6.0},
                 { Collections.asList(1, 3), Collections.asList(1), 1.0 }, 
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
    public void testExecuteShouldCalculatePrecisionMetricAndAppendToLogFile() {
        RecommendationResults testResults = new RecommendationResults();
                        
        testResults.setRecommendations(recs);
        testResults.setTestWindow(testWindow);
        testResults.setNumOfRecommendedItems(6);
        //gid, recommendations, test window
        Tuple tuple = Testing.testTuple(new Values(1.0,testResults));
                
        RecommendationEvaluationBolt spyBolt = spy(new RecommendationEvaluationBolt());
        
        spyBolt.execute(tuple);
        
        verify(spyBolt).calculatePrecision(recs,testWindow);
        // stores results of precision, first arg is group id, second precision metric for session
        verify(spyBolt).storeResults(1.0, anyDouble());
        
    }

    /**
    * Test of generate recommendations method, of class RecommendationBolt.
    */
    @Test
    public void testCalculatePrecisionShouldCalculateRightPrecisionMetricResult() {
       
       RecommendationEvaluationBolt spyBolt = spy(new RecommendationEvaluationBolt());
       double prec = spyBolt.calculatePrecision(recs, testWindow);
       assertTrue(prec == expectedResult);
       
    }
    
     /**
    * Test of generate recommendations method, of class RecommendationBolt.
    */
    @Test
    public void testStoreResultsShouldStoreGivenResultsToFileForGivenGroupWithTimestampAndTransactionId() {
        
        
        FileWriter mockFileWriter = mock(FileWriter.class);
        try {
            PowerMockito.whenNew(FileWriter.class).withArguments(File.class,true).thenReturn(mockFileWriter);
        } catch (Exception ex) {
            Logger.getLogger(RecommendationEvaluationBoltTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PrintWriter mockWriter = mock(PrintWriter.class);
        try {
            PowerMockito.whenNew(PrintWriter.class).withAnyArguments().thenReturn(mockWriter);
        } catch (Exception ex) {
            Logger.getLogger(RecommendationEvaluationBoltTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        RecommendationEvaluationBolt spyBolt = spy(new RecommendationEvaluationBolt());
        double prec = spyBolt.calculatePrecision(recs, testWindow);
        spyBolt.storeResults(1.0, prec);
        verify(mockWriter, times(1)).println(""+prec);
        
    }

    
    
}
