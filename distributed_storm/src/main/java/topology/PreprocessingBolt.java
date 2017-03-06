package topology;


import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import ppsdm.cluster.Cluster;
import ppsdm.cluster.Clustering;
import ppsdm.cluster.PPSDM.SphereClusterPPSDM;
import ppsdm.clusterers.clustream.PPSDM.WithKmeansPPSDM;
import moa.core.AutoExpandVector;

import core.PPSDM.UserModelPPSDM;
import java.io.File;
import moa.core.TimingUtils;
import ppsdm.core.PPSDM.enums.DistanceMetricsEnum;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

/**
 * 
 * @author Tomas Chovanak
 */
public class PreprocessingBolt implements IRichBolt {
    
    private OutputCollector collector;
    private WithKmeansPPSDM clusterer;
    private Clustering kmeansClustering = null;
    private Clustering cleanedKmeansClustering = null;
    private int clusteringId = 1;
    private Map<Integer,UserModelPPSDM> usermodels = new ConcurrentHashMap<>();
    private Map<Integer,Integer> catsToSupercats = new ConcurrentHashMap<>();
    private int microclusteringUpdatesCounter = 0;
    private  String categoryMappingFile = "g:\\workspace_DP2\\";
    private  String categoryMappingCloudFile = null;
    private transient CloudStorageAccount storageAccount = null; 
    private int ews = 1;
    private int counter = 0;
    private long start = 0;
    
    public PreprocessingBolt() {
        clusterer = new WithKmeansPPSDM(Configuration.mmc);
        if(!Configuration.LOCAL && Configuration.pathToCategoryMapping != null){
            try {
                storageAccount = CloudStorageAccount.parse(
                        Configuration.storageConnectionString);
                CloudFileClient fileClient = storageAccount.createCloudFileClient();
                CloudFileShare share = fileClient.getShareReference("alefinput");
                CloudFileDirectory rootDir = share.getRootDirectoryReference();
                CloudFile file = rootDir.getFileReference("categories_mapping.csv");
                this.categoryMappingCloudFile = file.downloadText();
            } catch (URISyntaxException | StorageException | IOException | InvalidKeyException ex) {
                Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            this.categoryMappingFile = Configuration.pathToCategoryMapping;
        }
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        try {
            // prepare catsToSupercats - read from redis
            if(Configuration.pathToCategoryMapping != null){
                this.catsToSupercats = this.readCategoriesMap();
            }
        } catch (IOException ex) {
            Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void execute(Tuple tuple) {
        if(tuple.getString(2).equals("RECOMMENDATION")){
            //collector.emit("streamEval",new Values(-1.0, tuple.getValue(3),"REC_EVAL",tuple.getValue(1)));
            //collector.emit("streamGlobal",new Values(-1.0, tuple.getInteger(0),tuple.getValue(1)));
            //collector.emit("streamRec",new Values(um.getGroupid(), tuple.getInteger(0),tuple.getValue(1), tuple.getValue(3),values.subList(0, ews)));
        }else{
            counter++;
            //System.out.println("GLOBAL " + counter + "Thread.currentThread().getId()" + Thread.currentThread().getId());
            if(start == 0){
                TimingUtils.enablePreciseTiming();
                start = TimingUtils.getNanoCPUTimeOfCurrentThread();
            }
            UserModelPPSDM um = updateUserModel(tuple);
            if(kmeansClustering != null){
                updateGroupingInUserModelClustream(um);
                if(um.getGroupid() != -1.0){
                    // send it to group method
                    collector.emit("streamGroup" + (int)Math.round(um.getGroupid()),new Values(um.getGroupid(),um.getId(),tuple.getValue(1)));
                }
            }  
            // ack when finished
            collector.ack(tuple);
            // PERFORM CLUSTERING
            this.performClustering(um);
        }
    }
    
     private Map<Integer,Integer> readCategoriesMap() throws FileNotFoundException, IOException {
        BufferedReader br;
        if(!Configuration.LOCAL ){
            br = new BufferedReader(new StringReader(categoryMappingCloudFile));
        }else {
            br = new BufferedReader(new FileReader(new File(categoryMappingFile)));
        }   
        Map<Integer,Integer> map = new HashMap<>();
        String line;
        while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] words = line.split(",");
            map.put(Integer.parseInt(words[1].trim()), Integer.parseInt(words[0].trim()));
        }
        return map;
       
     }
    
    private void performMacroclusteringClustream(){
        // B7: PERFORM MACROCLUSTERING
        Clustering results = clusterer.getMicroClusteringResult();
        if(results != null){
            AutoExpandVector<Cluster> clusters = results.getClustering();
            if(clusters.size() > 0){
                // save new clustering
                List<Clustering> clusterings;
                if(this.kmeansClustering == null){
                    clusterings =
                            clusterer.kMeans_rand(Configuration.gc,results);
                    this.kmeansClustering = clusterings.get(0);
                    this.cleanedKmeansClustering = clusterings.get(1);
                }else{
                    clusterings =
                            clusterer.kMeans_gta(Configuration.gc,results, 
                                    cleanedKmeansClustering, kmeansClustering);
                    this.kmeansClustering = clusterings.get(0);
                    this.cleanedKmeansClustering = clusterings.get(1);
                }
                System.out.println("CLUSTERING---------------------------------------" + 
                this.kmeansClustering.getClustering().size());
                this.clusteringId++;
            }
        }
    }
    
    
    private void performClustering(UserModelPPSDM um) {
        if(um.getNumOfNewSessions() > Configuration.tuc){
            Instance umInstance = um.getNewSparseInstance(Configuration.dimensionsInUserModel);
            // SEND umINSTANCE TO CLUSTERING BOLT TO PERFORM CLUSTERING
            //collector.emit("streamClustering_ID" + (this.clusteringId + 1),new Values(um.getId(),umInstance));
            clusterer.trainOnInstance(umInstance);
            this.microclusteringUpdatesCounter++;
            if(this.microclusteringUpdatesCounter > Configuration.tcm){
                // perform macroclustering
                this.microclusteringUpdatesCounter = 0;
                // this means macroclustering was performed therefore clustering id is updated.
                // also flag is raised to know that clustering was updated.
                // clustering with new id should be read from redis next time 
                performMacroclusteringClustream();
                this.clearUserModels();
            }
        }
    }
    
    
    @Override
    public void cleanup() {
        
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer ofd) {
        ofd.declareStream("streamEval", new Fields("gid","sid","task","test"));
        ofd.declareStream("streamRec", new Fields("gid","uid","items", "sid", "ew"));
        for(int i = 0; i < Configuration.gc; i++){
            ofd.declareStream("streamGroup"+i, new Fields("gid","uid","items"));
            
        }
        
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
    
    private UserModelPPSDM updateUserModel(Tuple tuple){
        int uid = tuple.getInteger(0);
        if(usermodels.containsKey(uid)){
            UserModelPPSDM um = usermodels.get(uid);
            if(this.catsToSupercats.size() > 0){
                um.updateWithTuple((List<Integer>)tuple.getValue(1),catsToSupercats);
            }else{
                um.updateWithTuple((List<Integer>)tuple.getValue(1));
            }
            return um;
        }else{
            UserModelPPSDM um = new UserModelPPSDM(tuple.getInteger(0), 
                    Configuration.tuc);
            if(this.catsToSupercats.size() > 0){
                um.updateWithTuple((List<Integer>)tuple.getValue(1),catsToSupercats);
            }else{
                um.updateWithTuple((List<Integer>)tuple.getValue(1));
            }
            usermodels.put(uid, um);
            return um;
        }
    }
    
    private void updateGroupingInUserModelClustream(UserModelPPSDM um) {
        if(this.clusteringId > um.getClusteringId()){
            // in um there is not set group for actual clustering so we need to set it now
            Instance umInstance = um.getInstance();   
            if(umInstance == null){
                return;
            }
            List<Cluster> clusters = null;
            if(kmeansClustering != null){  // if already clustering was performed
                clusters = this.kmeansClustering.getClustering();
            }
            Cluster bestCluster;
            double minDist = Double.MAX_VALUE;
            for(Cluster c : clusters){
                SphereClusterPPSDM cs = (SphereClusterPPSDM) c; // kmeans produce sphere clusters
                double dist = cs.getCenterDistance(umInstance, DistanceMetricsEnum.PEARSON);///cs.getRadius();
                //double dist = cs.getDistanceToRadius(umInstance);
                if(dist < 1.0 && dist < minDist){
                    bestCluster = cs;
                    minDist = dist;
                    um.setGroupid(bestCluster.getId());
                    um.setDistance(dist);
                } 
            }
            um.setClusteringId(this.clusteringId);
        }
    }
    
    /**
     * Removes old user models - if it has clustering id older than minimal user model updates
     */
    public void clearUserModels() {
        for(Map.Entry<Integer, UserModelPPSDM> entry : this.usermodels.entrySet()){
            UserModelPPSDM model = entry.getValue();
            if(this.clusteringId - model.getClusteringId() > Configuration.tcdiff){
                // delete 
                this.usermodels.remove(entry.getKey());
            }
        }
    }
    
}
