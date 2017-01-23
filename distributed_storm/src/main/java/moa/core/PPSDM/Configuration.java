/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM;

import moa.core.PPSDM.enums.ClusteringMethodsEnum;
import moa.core.PPSDM.enums.DistanceMetricsEnum;
import moa.core.PPSDM.enums.RecommendStrategiesEnum;
import moa.core.PPSDM.enums.SortStrategiesEnum;

/**
 * Static configuration of PPSDM method with default values
 * @author Tomas Chovanak
 */
public class Configuration {
    
    /*
        Flag if speed is evaluated or not
    */
    public static boolean EVALUATE_SPEED = false;
    
    /*
        Flag if speed is evaluated or not
    */
    public static boolean EXTRACT_DETAILS = false;
    
    
    /*
        At which transaction speed results should be extracted
    */
    public static int EXTRACT_SPEED_RESULTS_AT = 2000;
    
    /*
        Flag determines if personalized mining is turned on 
    */
    public static boolean GROUPING = false;
    
    /*
        Recommendation strategy only with frequent itemsets or with all semifcis.
    */
    public static boolean RECOMMEND_WITH_FI = false;
    
    /*
        Maximal time of semifci's update after segment of data
    */
    public static double MAX_UPDATE_TIME = 20000; 
    /*
        Restriction of maximal number of fci items in memory
    */
    public static double MAX_FCI_SET_COUNT = 500000; 
    /*
        Speed restriction 
    */
    public static double MIN_TRANSSEC = 5;
    
    /*
        From which transaction should evaluation start 
    */
    public static int START_EVALUATING_FROM = 1000;
    
    /*
        Recommendation strategy BEST WINS or VOTING
    */
    public static RecommendStrategiesEnum RECOMMEND_STRATEGY = RecommendStrategiesEnum.VOTES;
    
    /*
        Strategy of sorting fcis 
    */
    public static SortStrategiesEnum SORT_STRATEGY = SortStrategiesEnum.LCSANDSUPPORT;
    
    /*
        On which microcluster update shoul be snapshot of patterns and groupings extracted
    */
    public static int EXTRACT_PATTERNS_AT = 150;
    
    /*
        Maximum number of user sessions stored in user model history
    */
    public static int MAX_USER_SESSIONS_HISTORY_IN_USER_MODEL = 5;
    
    public static int MAX_INSTANCES_IN_MICROCLUSTER = 100;
    
    /*
        Maximum difference in clustering ids of curent clustering and clustering id 
        in user model. When difference is greater than user model is deleted.
    */
    public static int MAX_DIFFERENCE_OF_CLUSTERING_ID = 5;
    
    /*
        Distance metric used with clustering
    */
    public static DistanceMetricsEnum DISTANCE_METRIC = DistanceMetricsEnum.EUCLIDEAN;
    
    /*
        Clustering module used 
    */
    public static ClusteringMethodsEnum CLUSTERING_METHOD = ClusteringMethodsEnum.CLUSTREAM;
    
    /*
        Number of transactions processed from start 
    */
    public static int TRANSACTION_COUNTER = 0;
    
    /*
        Stores time of stream processing start
    */
    public static double STREAM_START_TIME = 0;
    
    /*
        Stores start of update time
    */
    public static double START_UPDATE_TIME = 0;
    
    /*
        Group counter 
    */
    public static int GROUP_COUNTER = 0;
    
    /*
        Group changes counters
    */
    public static double GROUP_CHANGES = 0;
    public static double GROUP_CHANGED_TIMES = 0; 
    
    /*
        (Experimental) FciValue parameters constants 
    */
    public static double A = 0.0;
    public static double B = 0.0;
    public static double C = 0.0;
    
    /*
        (Experimental) How to compute similarity between evaluation window and existing fcis
    */
    public static String INTERSECT_STRATEGY = "LCS";
    
    
}
