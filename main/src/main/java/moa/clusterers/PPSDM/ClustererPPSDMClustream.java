/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.clusterers.PPSDM;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.List;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.PPSDM.SphereClusterPPSDM;
import moa.clusterers.Clusterer;
import moa.clusterers.clustream.PPSDM.WithKmeansPPSDM;
import moa.core.AutoExpandVector;
import moa.core.PPSDM.UserModelPPSDM;
import moa.core.PPSDM.enums.DistanceMetricsEnum;

/**
 *
 * @author Tomas
 */
public class ClustererPPSDMClustream implements ClustererPPSDM {

    private WithKmeansPPSDM clusterer;
    private Clustering clustering;
    private Clustering cleanedClustering;
    private int numOfGroups;
    private int clusteringID;
    
    public ClustererPPSDMClustream(Integer numberOfGroups, Integer maxNumKernels, Integer kernelRadiFactor) {
         this.clusterer = new WithKmeansPPSDM();
         this.numOfGroups = numberOfGroups;
         this.clusterer.kOption.setValue(numberOfGroups);
         this.clusterer.maxNumKernelsOption.setValue(maxNumKernels);
         this.clusterer.kernelRadiFactorOption.setValue(kernelRadiFactor);
         this.clusterer.resetLearning();
         this.clusteringID = 0;
         
    }

    @Override
    public Clusterer getClusterer() {
        return clusterer;
    }

    @Override
    public Clustering getClustering() {
        return clustering;
    }

    @Override
    public void performMacroclustering() {
        Clustering results = clusterer.getMicroClusteringResult();
        if(results != null){
            AutoExpandVector<Cluster> clusters = results.getClustering();
            if(clusters.size() > 0){
                // save new clustering
                List<Clustering> clusterings;
                if(this.clustering == null){
                    clusterings =
                            clusterer.kMeans_rand(this.numOfGroups,results);
                    this.clustering = clusterings.get(0);
                    this.cleanedClustering = clusterings.get(1);
                }else{
                    clusterings =
                            clusterer.kMeans_gta(this.numOfGroups,results, 
                                    cleanedClustering, clustering);
                    this.clustering = clusterings.get(0);
                    this.cleanedClustering = clusterings.get(1);
                }
                System.out.println("CLUSTERING---------------------------------------" + 
                this.clustering.getClustering().size());
                this.clusteringID++;
            }
        }
    }

    @Override
    public boolean isClustering() {
        return clustering != null;
    }

    @Override
    public int getClusteringID() {
        return clusteringID;
    }

    @Override
    public void trainOnInstance(Instance umInstance) {
        clusterer.trainOnInstance(umInstance);
    }

    @Override
    public void updateGroupingInUserModel(UserModelPPSDM um) {
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
            um.setClusteringId(this.clusteringID);
        }
    }
    
}
