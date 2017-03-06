package topology;


import ppsdm.core.PPSDM.enums.DistanceMetricsEnum;

/**
 *
 * @author Tomas Chovanak
 */
public class ConfigurationNP {
    
    public static boolean LOCAL = false;
    public static String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=ppsdmcluster;AccountKey=ZNq9UPEklSXUNlG6ZIHxFqfOO2Np/4XZm7o6Ts1zyOj8/PBj2z9b4TrbNtgheWL2Y/hsNlXTdRqCr/gCqEAOag==;";
    public static int ews = 1;
    public static String pathToCategoryMapping = null;
    
    // clustering
    public static int mmc = 1000;
    public static int tcm = 800;
    public static int tuc = 5;
    public static int gc = 8;
    public static int dimensionsInUserModel = 85;
    public static int tcdiff = 15;
    public static DistanceMetricsEnum distanceMetric = DistanceMetricsEnum.EUCLIDEAN;
    
    // incmine 
    public static int ws = 15;
    public static double rr = 0.5;
    public static int mil = 10;
    public static double ms = 0.05;
    public static int fsl = 25;
    
    // redis
    public static String redishost = "ppsdmcluster.redis.cache.windows.net";
    public static int redisport =  6379;
    public static String redisAK = "oTJ9lJcGG412rtYDiRCSGwOINGrOq2XkhXypoVAy6tE=";

}
