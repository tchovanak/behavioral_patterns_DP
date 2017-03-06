package topology;


import java.net.URL;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
// Include the following imports to use blob APIs.
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Tomas Chovanak
 */
public class PPSDMStormTopology {
    
    
    public static void main(String[] args) throws Exception{
        
        TopologyBuilder builder = new TopologyBuilder();
         //new configuration
        Config conf = new Config();
        //Set to false to disable debug information when
        // running in production on a cluster
        conf.setDebug(false);
        if(Configuration.LOCAL){
            builder.setSpout("sessions-spout", new SessionsInputSpout(2)).setMaxTaskParallelism(1);
            
            builder.setBolt("preprocessing-bolt", new PreprocessingBolt())
                  .setMaxTaskParallelism(1)
                  .fieldsGrouping("sessions-spout","streamGroup",new Fields("task") )
                  .setNumTasks(1);
            
            builder.setBolt("global-patterns-bolt-1", new GlobalPatternsBolt(),1).setMaxTaskParallelism(1)
                  .fieldsGrouping("sessions-spout", "streamGlobal", new Fields("sid"))
                  .setNumTasks(1);

            for(int i = 0; i < Configuration.gc; i++){
                builder.setBolt("group-patterns-bolt"+i, new GroupPatternsBolt(), 1).setMaxTaskParallelism(1)
                  .fieldsGrouping("preprocessing-bolt", "streamGroup" + i, new Fields("gid"))
                  .setNumTasks(1);
            }
            System.out.println("RUNNING LOCAL\n");
            conf.setMaxTaskParallelism(10);
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("PPSDMStormTopology", conf, builder.createTopology());
            Thread.sleep(1000000);
            cluster.shutdown();
        }else{
            builder.setSpout("sessions-spout", new SessionsInputSpout(2)).setMaxTaskParallelism(1);
            
            builder.setBolt("preprocessing-bolt", new PreprocessingBolt())
             .setMaxTaskParallelism(1)
             .fieldsGrouping("sessions-spout","streamGroup",new Fields("task") )
             .setNumTasks(1);
            
            builder.setBolt("global-patterns-bolt-1", new GlobalPatternsBolt(),1).setMaxTaskParallelism(1)
                  .fieldsGrouping("sessions-spout", "streamGlobal", new Fields("sid"))
                  .setNumTasks(1);

            for(int i = 0; i < Configuration.gc; i++){
                builder.setBolt("group-patterns-bolt"+i, new GroupPatternsBolt(), 1).setMaxTaskParallelism(1)
                  .fieldsGrouping("preprocessing-bolt", "streamGroup" + i, new Fields("gid"))
                  .setNumTasks(1);
            }
            // parallelism hint to set the number of workers
            System.out.println("RUNNING CLOUD\n");
            conf.setNumWorkers(4);
            //submit the topology
            StormSubmitter.submitTopology("topology-ppsdm", conf, builder.createTopology());
        
        }
        //      builder.setBolt("recommendation-bolt", new RecommendationBolt(2)).setMaxTaskParallelism(1)
        //            .fieldsGrouping("preprocessing-bolt", "streamRec", new Fields("gid"))
        //            .setNumTasks(1);

        //      builder.setBolt("recommendation-evaluation-bolt", new RecommendationEvaluationBolt(2)).setMaxTaskParallelism(1)
        //            .fieldsGrouping("preprocessing-bolt", "streamEval", new Fields("gid"))
        //            .fieldsGrouping("recommendation-bolt", "streamEvalFromRec", new Fields("gid"))
        //            .setNumTasks(1);
      
   }
    
}   
