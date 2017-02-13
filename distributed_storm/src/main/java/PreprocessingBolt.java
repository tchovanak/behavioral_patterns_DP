
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
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
import core.PPSDM.Configuration;
import core.PPSDM.UserModelPPSDM;
import java.io.File;
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
    private WithKmeansPPSDM clusterer = new WithKmeansPPSDM();
    public Integer numberOfGroups = 6;
    private Clustering kmeansClustering = null;
    private Clustering cleanedKmeansClustering = null;
    private int clusteringId = 1;
    private Map<Integer,UserModelPPSDM> usermodels = new ConcurrentHashMap<>();
    private Map<Integer,Integer> catsToSupercats = new ConcurrentHashMap<>();
    private int numMinNumberOfChangesInUserModel = 5;
    private int numOfDimensionsInUserModel = 18;
    private int microclusteringUpdatesCounter = 0;
    private int numMinNumberOfMicroclustersUpdates;
    private transient String categoryMappingFile = null;
    private  String categoryMappingCloudFile = null;
    private transient CloudStorageAccount storageAccount = null; 
    
    public PreprocessingBolt(int numMinNumberOfChangesInUserModel, int numOfDimensionsInUserModel, 
            int numMinNumberOfMicroclustersUpdates, int numOfGroups, String pathToCategoryMappingFile) {
        this.numOfDimensionsInUserModel = numOfDimensionsInUserModel;
        this.numMinNumberOfChangesInUserModel = numMinNumberOfChangesInUserModel;
        this.numMinNumberOfMicroclustersUpdates = numMinNumberOfMicroclustersUpdates;
        
        this.categoryMappingFile  = pathToCategoryMappingFile;
     
        this.numberOfGroups = numOfGroups;
    }
    
    public PreprocessingBolt(int numMinNumberOfChangesInUserModel, int numOfDimensionsInUserModel, 
            int numMinNumberOfMicroclustersUpdates, int numOfGroups, URL pathToCategoryMappingFile) {
        this.numOfDimensionsInUserModel = numOfDimensionsInUserModel;
        this.numMinNumberOfChangesInUserModel = numMinNumberOfChangesInUserModel;
        this.numMinNumberOfMicroclustersUpdates = numMinNumberOfMicroclustersUpdates;
        
        // Use the CloudStorageAccount object to connect to your storage account
        try {
            storageAccount = CloudStorageAccount.parse(
                    PPSDMStormTopology.storageConnectionString);
                        // Create the file storage client.
            CloudFileClient fileClient = storageAccount.createCloudFileClient();
            // Get a reference to the file share
            CloudFileShare share = fileClient.getShareReference("input");
            //Get a reference to the root directory for the share.
            CloudFileDirectory rootDir = share.getRootDirectoryReference();
            //Get a reference to the file you want to download
            CloudFile file = rootDir.getFileReference("categories_mapping.csv");
            this.categoryMappingCloudFile = file.downloadText();
        } catch (URISyntaxException ex) {
            Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
        } catch (StorageException ex) {
            Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.numberOfGroups = numOfGroups;
    }
    
    @Override
    public void prepare(Map map, TopologyContext tc, OutputCollector oc) {
        this.collector = oc;
        try {
            // prepare catsToSupercats - read from redis
            this.catsToSupercats = this.readCategoriesMap();
        } catch (IOException ex) {
            Logger.getLogger(PreprocessingBolt.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void execute(Tuple tuple) {
        if(tuple.getString(2).equals("RECOMMENDATION")){
            // this tuple is to be used for recommendation purpose only
            // send it to RecommendationBolt with identified GID
            // first send tuple to global method
            UserModelPPSDM um = updateUserModel(tuple);
            collector.emit("streamRec",new Values(um.getGroupid(), tuple.getInteger(0),tuple.getValue(1), tuple.getValue(3)));
            
        }else{
            // send tuple to recommendation evaluation bolt
            // GID, SID, TASK, TEST WINDOW
            collector.emit("streamEval",new Values(-1.0, tuple.getValue(3),"REC_EVAL",tuple.getValue(1)));
            // first send tuple to global method
            collector.emit("streamGlobal",new Values(-1.0, tuple.getInteger(0),tuple.getValue(1)));
            // then resolve group uid belongs to
            // get user model by uid
            UserModelPPSDM um = updateUserModel(tuple);

            if(kmeansClustering != null){
                updateGroupingInUserModelClustream(um);
                if(um.getGroupid() != -1.0){
                    // send it to group method
                    collector.emit("streamGroup" + (int)Math.round(um.getGroupid()),new Values(um.getGroupid(),um.getId(),tuple.getValue(1)));
                }
            }   
            // PERFORM CLUSTERING
            this.performClustering(um); 
        }
        // ack when finished
        collector.ack(tuple);
        
    }
    
     private Map<Integer,Integer> readCategoriesMap() throws FileNotFoundException, IOException {
        BufferedReader br;
        if(categoryMappingCloudFile != null)
            br = new BufferedReader(new StringReader(categoryMappingCloudFile));
        else
            br = new BufferedReader(new FileReader(new File(categoryMappingFile)));
        Map<Integer,Integer> map = new HashMap<>();
        String line = "";
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
                            clusterer.kMeans_rand(this.numberOfGroups,results);
                    this.kmeansClustering = clusterings.get(0);
                    this.cleanedKmeansClustering = clusterings.get(1);
                }else{
                    clusterings =
                            clusterer.kMeans_gta(this.numberOfGroups,results, 
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
        if(um.getNumOfNewSessions() > this.numMinNumberOfChangesInUserModel){
            Instance umInstance = um.getNewSparseInstance(numOfDimensionsInUserModel);
            // SEND umINSTANCE TO CLUSTERING BOLT TO PERFORM CLUSTERING
            //collector.emit("streamClustering_ID" + (this.clusteringId + 1),new Values(um.getId(),umInstance));
            clusterer.trainOnInstance(umInstance);
            this.microclusteringUpdatesCounter++;
            if(this.microclusteringUpdatesCounter > this.numMinNumberOfMicroclustersUpdates){
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
        ofd.declareStream("streamRec", new Fields("gid","uid","items", "sid"));
        ofd.declareStream("streamGlobal", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup0", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup1", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup2", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup3", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup4", new Fields("gid","uid","items"));
        ofd.declareStream("streamGroup5", new Fields("gid","uid","items"));
   
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
                um.updateWithTuple((List<Double>)tuple.getValue(1),catsToSupercats);
            }else{
                um.updateWithTuple((List<Double>)tuple.getValue(1));
            }
            return um;
        }else{
            UserModelPPSDM um = new UserModelPPSDM(tuple.getInteger(0), 
                    this.numMinNumberOfChangesInUserModel);
            if(this.catsToSupercats.size() > 0){
                um.updateWithTuple((List<Double>)tuple.getValue(1),catsToSupercats);
            }else{
                um.updateWithTuple((List<Double>)tuple.getValue(1));
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
            if(this.clusteringId - model.getClusteringId() > Configuration.MAX_DIFFERENCE_OF_CLUSTERING_ID){
                // delete 
                this.usermodels.remove(entry.getKey());
            }
        }
    }
    
}
