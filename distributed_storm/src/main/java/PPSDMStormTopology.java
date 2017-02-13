
import java.net.URL;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
// Include the following imports to use blob APIs.
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.file.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Tomas
 */
public class PPSDMStormTopology {
    
          // Configure the connection-string with your values
    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;AccountName=ppsdmclusterstore;AccountKey=wB+mi+YMrO/nNDa2gtzlwOEHdLqblUVY6ikiu1gthQw024wh9cSsNpO3m7cjDkxSaCRLDg/2Wo+m4epSDg14Vw==";

    
    
    public static void main(String[] args) throws Exception{
        
     
        
      TopologyBuilder builder = new TopologyBuilder();
      builder.setSpout("sessions-spout", new SessionsInputSpout(2)).setMaxTaskParallelism(1);

      builder.setBolt("preprocessing-bolt", new PreprocessingBolt(5,18,100,6,"g:\\workspace_DP2\\results_grid\\alef\\categories_mapping.csv"))
            .setMaxTaskParallelism(1)
            .shuffleGrouping("sessions-spout")
            .setNumTasks(1);
      
//       builder.setBolt("preprocessing-bolt", new PreprocessingBolt(5,18,100,6,new URL("https://ppsdmclusterstore.file.core.windows.net/input/categories_mapping.csv")))
//            .setMaxTaskParallelism(1)
//            .fieldsGrouping("sessions-spout",new Fields("gid") )
//            .setNumTasks(1);
      
      builder.setBolt("recommendation-bolt", new RecommendationBolt(2)).setMaxTaskParallelism(1)
            .fieldsGrouping("preprocessing-bolt", "streamRec", new Fields("gid"))
            .setNumTasks(1);
      
      builder.setBolt("recommendation-evaluation-bolt", new RecommendationEvaluationBolt(2)).setMaxTaskParallelism(1)
            .fieldsGrouping("preprocessing-bolt", "streamEval", new Fields("gid"))
            .fieldsGrouping("recommendation-bolt", "streamEvalFromRec", new Fields("gid"))
            .setNumTasks(1);
      
      builder.setBolt("global-patterns-bolt-1", new GlobalPatternsBolt(),1).setMaxTaskParallelism(1)
            .fieldsGrouping("preprocessing-bolt", "streamGlobal", new Fields("gid"))
            .setNumTasks(1);
      
      for(int i = 0; i < 6; i++){
          builder.setBolt("group-patterns-bolt"+i, new GroupPatternsBolt(), 1).setMaxTaskParallelism(1)
            .fieldsGrouping("preprocessing-bolt", "streamGroup" + i, new Fields("gid"))
            .setNumTasks(1);
      }
      //new configuration
      Config conf = new Config();
      //Set to false to disable debug information when
      // running in production on a cluster
      conf.setDebug(true);
      //Create Config instance for cluster configuration
      //If there are arguments, we are running on a cluster
      //if (args != null && args.length > 0) {
        //parallelism hint to set the number of workers
//        System.out.println("RUNNING CLOUD\n");
//        conf.setNumWorkers(3);
//        //submit the topology
//        //StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
//        StormSubmitter.submitTopology("topology-ppsdm", conf, builder.createTopology());
      //}
      //Otherwise, we are running locally
//      else {
        //Cap the maximum number of executors that can be spawned
        //for a component to 3
        System.out.println("RUNNING LOCAL\n");
        conf.setMaxTaskParallelism(1);
        //LocalCluster is used to run locally
        LocalCluster cluster = new LocalCluster();
        //submit the topology
        cluster.submitTopology("PPSDMStormTopology", conf, builder.createTopology());
        //sleep
        Thread.sleep(1000000);
        //shut down the cluster
        cluster.shutdown();
//      }
      
   }
    
}   
