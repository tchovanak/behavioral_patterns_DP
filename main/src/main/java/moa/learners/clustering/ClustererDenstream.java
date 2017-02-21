/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.clustering;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.List;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.Clusterer;
import moa.clusterers.denstream.WithDBSCAN;
import moa.clusterers.macro.NonConvexCluster;
import moa.core.Configuration;
import moa.learners.patternMining.PersonalizedIncMine;

/**
 *
 * @author Tomas
 */
public class ClustererDenstream extends ClusteringComponent {

    private Clustering clustering;
    private WithDBSCAN clusterer;
    private int clusteringID;
    private PersonalizedIncMine incMine;
    
    public ClustererDenstream(ClusteringComponentConfiguration config, 
            PersonalizedIncMine incMine) {
        super(config);
        this.incMine = incMine;
    }
    
    @Override
    public Clusterer getClusterer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Clustering getClustering() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void performMacroclustering() {
        this.clustering = clusterer.getClusteringResult(this.clustering);
        System.out.println("CLUSTERING-----------------------------------------------" + 
                this.clustering.getClustering().size());
        //Configuration.GROUP_COUNTER = this.clustering.getClustering().size();
        this.clusteringID++;
        if(this.incMine.fciTablesGroups.size() < clustering.size()){
            for(int i = this.incMine.fciTablesGroups.size(); 
                    i < clustering.getClustering().size(); i++){
                this.incMine.addFciTable();
            }
        }
    }
    
    @Override
    public boolean isClustering() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public int getClusteringID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void trainOnInstance(Instance umInstance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateGroupingInUserModel(UserModel um) {
        if(this.clusteringID > um.getClusteringId()){
            // in um there is not set group for actual clustering so we need to set it now
            Instance umInstance = um.getInstance();   
            if(umInstance == null){
                return;
            }
            List<Cluster> clusters = null;
            if(clustering != null){  // if already clustering was performed
                clusters = this.clustering.getClustering();
            }
            for(Cluster c : clusters){
                NonConvexCluster cs = (NonConvexCluster) c; 
                //double prob = cs.getInclusionProbability(umInstance);
                double dist = cs.getDistanceToRadius(umInstance);
                NonConvexCluster bestCluster;
                //double minProb = 0.0;
                double minDist = Double.MAX_VALUE;
                //if(prob > 0.0 && prob > minProb){
                if(dist > 0.0 && dist < minDist){
                    bestCluster = cs;
                    minDist = dist;
                    um.setGroupid(bestCluster.getId());
                    um.setDistance(0);
                }
                um.setClusteringId(this.clusteringID);
            }
        }
    }
    
}
