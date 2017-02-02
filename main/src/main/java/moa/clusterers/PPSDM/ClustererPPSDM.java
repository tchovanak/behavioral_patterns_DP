/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.clusterers.PPSDM;

import com.yahoo.labs.samoa.instances.Instance;
import moa.cluster.Clustering;
import moa.clusterers.Clusterer;
import moa.core.PPSDM.UserModelPPSDM;

/**
 * This interface is common for all different clustering components of PPSDM method.
 * @author Tomas Chovanak
 */
public interface ClustererPPSDM {
    
    Clusterer getClusterer();
    Clustering getClustering();
    void performMacroclustering();
    boolean isClustering();
    int getClusteringID();
    void trainOnInstance(Instance umInstance);
    void updateGroupingInUserModel(UserModelPPSDM um);
        
}
