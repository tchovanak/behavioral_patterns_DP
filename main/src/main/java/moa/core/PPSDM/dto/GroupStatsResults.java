/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.dto;

import java.util.ArrayList;
import java.util.List;
import moa.core.PPSDM.FciValue;

/**
 *
 * @author Tomas Chovanak
 */
public class GroupStatsResults {
    
    private int[] numRecommendedItems = {1,2,3,4,5,10,15};
    private List<Integer> uids = new ArrayList<>();
    private Integer groupid = -1;
    private List<FciValue> fcis;
    private Double averageSupport = 0.0;
    private Integer numberOfUsers = 0;
    private Integer numOfUniquePatterns = 0;
    private Double averageSupportOfUniquePatterns = 0.0;
    public Double[] sumPrecisionForGroupOG = new Double[this.numRecommendedItems.length];
    public Double[] sumPrecisionForGroupGGC = new Double[this.numRecommendedItems.length];
    public Double[] sumPrecisionForGroupGO = new Double[this.numRecommendedItems.length];
    public Double[] sumHitsForGroupOG = new Double[this.numRecommendedItems.length];
    public Double[] sumHitsForGroupGGC = new Double[this.numRecommendedItems.length];
    public Double[] sumHitsForGroupGO = new Double[this.numRecommendedItems.length];
    public Double[] sumRecsForGroupOG = new Double[this.numRecommendedItems.length];
    public Double[] sumRecsForGroupGGC = new Double[this.numRecommendedItems.length];
    public Double[] sumRecsForGroupGO = new Double[this.numRecommendedItems.length];
    public Integer[] numTransactionsForGroup = new Integer[this.numRecommendedItems.length];
    
    private double[] centre;
    private int clusteringId;

    public List<FciValue> getFcis() {
        return fcis;
    }

    public GroupStatsResults() {
        for(int i = 0; i < this.numTransactionsForGroup.length; i++){
            this.numTransactionsForGroup[i] = 0;
            this.sumPrecisionForGroupGGC[i] = 0.0;
            this.sumPrecisionForGroupGO[i] = 0.0;
            this.sumPrecisionForGroupOG[i] = 0.0;
            this.sumHitsForGroupGGC[i] = 0.0;
            this.sumHitsForGroupGO[i] = 0.0;
            this.sumHitsForGroupOG[i] = 0.0;
            this.sumRecsForGroupGGC[i] = 0.0;
            this.sumRecsForGroupGO[i] = 0.0;
            this.sumRecsForGroupOG[i] = 0.0;
        }
    }

    public Double[] getAveragePrecisionForGroupGGC() {
        Double[] averagePrecisionForGroup = new Double[this.numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            averagePrecisionForGroup[i] = this.sumPrecisionForGroupGGC[i]/
                    this.numTransactionsForGroup[i];
        }
        return averagePrecisionForGroup;
    }

    public Double[] getAverageAccuracyForGroupGGC() {
        Double[] averageAccuracyForGroup = new Double[this.numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            averageAccuracyForGroup[i] = this.sumHitsForGroupGGC[i]/
                    this.sumRecsForGroupGGC[i];
        }
        return averageAccuracyForGroup;
    }
    
    public Double[] getAveragePrecisionForGroupGO() {
        Double[] averagePrecisionForGroup = new Double[this.numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            averagePrecisionForGroup[i] = this.sumPrecisionForGroupGO[i]/
                    this.numTransactionsForGroup[i];
        }
        return averagePrecisionForGroup;
    }

    public Double[] getAverageAccuracyForGroupGO() {
        Double[] averageAccuracyForGroup = new Double[this.numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            averageAccuracyForGroup[i] = this.sumHitsForGroupGO[i]/
                    this.sumRecsForGroupGO[i];
        }
        return averageAccuracyForGroup;
    }
    
    public Double[] getAveragePrecisionForGroupOG() {
        Double[] averagePrecisionForGroup = new Double[this.numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            averagePrecisionForGroup[i] = this.sumPrecisionForGroupOG[i]/
                    this.numTransactionsForGroup[i];
        }
        return averagePrecisionForGroup;
    }

    public Double[] getAverageAccuracyForGroupOG() {
        Double[] averageAccuracyForGroup = new Double[this.numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            averageAccuracyForGroup[i] = this.sumHitsForGroupOG[i]/
                    this.sumRecsForGroupOG[i];
        }
        return averageAccuracyForGroup;
    }
    
    public Integer getNumberOfUsers() {
        return numberOfUsers;
    }

    public void setNumberOfUsers(Integer numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }

    public Integer getNumOfUniquePatterns() {
        return numOfUniquePatterns;
    }

    public void setNumOfUniquePatterns(Integer numOfUniquePatterns) {
        this.numOfUniquePatterns = numOfUniquePatterns;
    }

    public Double getAverageSupportOfUniquePatterns() {
        return averageSupportOfUniquePatterns;
    }

    public void setAverageSupportOfUniquePatterns(Double averageSupportOfUniquePatterns) {
        this.averageSupportOfUniquePatterns = averageSupportOfUniquePatterns;
    }

    public void setFcis(List<FciValue> fcis) {
        this.fcis = fcis;
    }

    public Double getAverageSupport() {
        return averageSupport;
    }

    public void setAverageSupport(Double averageSupport) {
        this.averageSupport = averageSupport;
    }
    
    public double[] getCentre() {
        return centre;
    }

    public int getClusteringId() {
        return clusteringId;
    }

    public void setClusteringId(int clusteringId) {
        this.clusteringId = clusteringId;
    }

    public void setCentre(double[] centre) {
        this.centre = centre;
    }

    public List<Integer> getUids() {
        return uids;
    }

    public void setUids(List<Integer> uids) {
        this.uids = uids;
    }
    
    public void addUid(Integer uid) {
        this.uids.add(uid);
        this.numberOfUsers = this.uids.size();
    }

    public Integer getGroupid() {
        return groupid;
    }

    public void setGroupid(Integer groupid) {
        this.groupid = groupid;
    }
    
}
