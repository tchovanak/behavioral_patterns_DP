
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import redis.clients.jedis.JedisPoolConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import redis.clients.jedis.JedisPool;

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
    
    public static void main(String[] args) throws Exception{
       
      //Create Config instance for cluster configuration
      Config config = new Config();
      config.setDebug(true);
      config.put(Config.TOPOLOGY_WORKERS, 4);
      config.put(Config.NIMBUS_HOST, "localhost");
      
      TopologyBuilder builder = new TopologyBuilder();
      builder.setSpout("sessions-spout", new SessionsInputSpout());

      builder.setBolt("preprocessing-bolt", new PreprocessingBolt(5,18,100,6,"g:\\workspace_DP2\\results_grid\\alef\\categories_mapping.csv"))
         .shuffleGrouping("sessions-spout");
      
      builder.setBolt("recommendation-bolt", new RecommendationBolt())
         .shuffleGrouping("sessions-spout");
      
      builder.setBolt("global-patterns-bolt-1", new GlobalPatternsBolt(),1).setMaxTaskParallelism(1)
            .fieldsGrouping("preprocessing-bolt", "streamGlobal", new Fields("gid"))
            .setNumTasks(1);
      
//      builder.setBolt("clustering-bolt", new ClusteringBolt(),1).setMaxTaskParallelism(1)
//            .fieldsGrouping("preprocessing-bolt", "streamClustering", new Fields("gid"))
//            .setNumTasks(1);
      
      for(int i = 0; i < 4; i++){
          builder.setBolt("group-patterns-bolt"+i, new GroupPatternsBolt(), 1).setMaxTaskParallelism(1)
            .fieldsGrouping("preprocessing-bolt", "streamGroup" + i, new Fields("gid"))
            .setNumTasks(1);
      }
      
      LocalCluster cluster = new LocalCluster();
      
      cluster.submitTopology("PPSDMStormTopology", config, builder.createTopology());
      
      Thread.sleep(1000000);
		
      //Stop the topology
		
      cluster.shutdown();
   }
    
}
