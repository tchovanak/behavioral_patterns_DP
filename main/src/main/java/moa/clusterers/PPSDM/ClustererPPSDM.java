/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.clusterers.PPSDM;

import moa.cluster.Clustering;
import moa.clusterers.Clusterer;

/**
 * This class is
 * @author Tomas Chovanak
 */
public interface ClustererPPSDM {
    
    Clusterer getClusterer();
    Clustering getClustering();
    void performMacroclustering();
    void updateGroupingInUserModel();
    boolean isClustering();

    public void resetLearning();
        
}
