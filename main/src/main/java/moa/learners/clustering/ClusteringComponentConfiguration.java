/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.clustering;

import moa.core.enums.DistanceMetricsEnum;

/**
 *
 * @author Tomas
 */
public interface ClusteringComponentConfiguration {
    
    public void setTcdiff(int tcdiff);
    public int getTcdiff();
    public DistanceMetricsEnum getDistanceMetric();
    public void setDistanceMetric(DistanceMetricsEnum distanceMetric);
    
}
