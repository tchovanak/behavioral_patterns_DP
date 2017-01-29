/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.List;
import moa.core.FrequentItemset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tomas
 */
public class RecommendationGeneratorTest {
    
    //private RecommendationGenerator recGen;
    
    public RecommendationGeneratorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
       // recGen = new RecommendationGenerator();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of generateRecommendations method, of class RecommendationGenerator.
     */
    @Test
    public void testGenerateRecommendations() {
        System.out.println("generateRecommendations");
        Integer[][] data = {{437,460,521,530,1275,1389,1390,1417,1418,1925,1938},
                            {191,1161},
                            {323,325,326,327,328,329,330,1719,1858},
                            {52,180,228,438,460,796,936,1347,1390,1654,1905,1925,2052}
        };
              
        RecommendationResults expResult = null;
        List<Integer> ew = null;
        //RecommendationResults result = recGen.generateRecommendations(ew, globItemsets, groupItemsets);
        //assertEquals(expResult, result);
        
    }
    
}
