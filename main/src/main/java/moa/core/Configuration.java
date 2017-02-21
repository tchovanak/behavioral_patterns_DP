/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core;

import moa.learners.recommendation.RecommendationConfiguration;
import moa.learners.clustering.ClusteringComponentConfiguration;
import moa.learners.clustering.ClustreamConfiguration;
import moa.learners.patternMining.PersonalizedIncMineConfiguration;
import moa.core.enums.DistanceMetricsEnum;
import moa.core.enums.RecommendStrategiesEnum;
import moa.learners.patternMining.PatternMiningConfiguration;
import moa.evaluation.EvaluationConfiguration;
import moa.learners.patternMining.PersonalizedCloStreamConfiguration;
import moa.learners.patternMining.PersonalizedEstDecPlusConfiguration;


/**
 * Configuration of PPSDM method with default values
 * @author Tomas Chovanak
 */
public class Configuration implements GeneralConfiguration, 
        PatternMiningConfiguration, PersonalizedIncMineConfiguration,
        ClusteringComponentConfiguration,ClustreamConfiguration, 
        RecommendationConfiguration, EvaluationConfiguration , 
        PersonalizedCloStreamConfiguration, PersonalizedEstDecPlusConfiguration {
        
    //PATTERN MINING 
    private int TUC;
    private int TCM;
    private int userModelDimensions;
    
    // INCMINE FIELDS
    private int WS;
    private int MIL;
    private int GC = 6;
    private double MS = 0.05;
    private double RR;
    private int FSL;
    private int GFSL;
    private long StartUpdateTime;
    
    //ESTDECPLUS
    private double d = 1;
    private double minSigValue = 0.4 * MS;
    private double deltaValue = 0.03;
    private double minMergeValue = 0.001;
    
    // CLUSTREAM FIELDS
    private int maxNumKernels;
    private int kernelRadiFactor;
    
    // RECOMMENDATION FIELDS
    private int EWS;
    private int RC;
    private RecommendStrategiesEnum recommendationStrategy = RecommendStrategiesEnum.VOTES;
    
    // CLUSTERING
    private int tcdiff;
    
    // EVALUATION
    private String outputFile;
    private Boolean extractDetails = false;
    private int transactionCounter = 0;
    private long streamStartTime;
    private int extractSpeedResultsAt = 2000;
    private int extractPatternsAt = 795;
    private int startEvaluatingFrom = 0;
    private boolean evaluateSpeed = false;
    
    // GENERAL 
    private boolean grouping = true;
    private int MTS = 15;
    private DistanceMetricsEnum distanceMetric = DistanceMetricsEnum.EUCLIDEAN;
    
    @Override
    public int getWS() {
        return WS;
    }

    @Override
    public void setWS(int WS) {
        this.WS = WS;
    }

    @Override
    public int getMIL() {
        return MIL;
    }

    @Override
    public void setMIL(int MIL) {
        this.MIL = MIL;
    }

    @Override
    public int getGC() {
        return GC;
    }

    @Override
    public void setGC(int GC) {
        this.GC = GC;
    }

    @Override
    public double getMS() {
        return MS;
    }

    @Override
    public void setMS(double MS) {
        this.MS = MS;
    }

    @Override
    public double getRR() {
        return RR;
    }

    @Override
    public void setRR(double RR) {
        this.RR = RR;
    }

    @Override
    public int getFSL() {
        return FSL;
    }

    @Override
    public void setFSL(int FSL) {
        this.FSL = FSL;
    }

    @Override
    public int getGFSL() {
        return GFSL;
    }

    @Override
    public void setGFSL(int GFSL) {
        this.GFSL = GFSL;
    }

    @Override
    public long getStartUpdateTime() {
        return StartUpdateTime;
    }

    @Override
    public void setStartUpdateTime(long StartUpdateTime) {
        this.StartUpdateTime = StartUpdateTime;
    }

    @Override
    public int getMaxNumKernels() {
        return maxNumKernels;
    }

    @Override
    public int getKernelRadiFactor() {
        return kernelRadiFactor;
    }

    @Override
    public void setMaxNumKernels(int gc) {
        this.maxNumKernels = gc;
    }

    @Override
    public void setKernelRadiFactor(int gc) {
       this.kernelRadiFactor = gc; 
    }

    @Override
    public void setEWS(int ews) {
        this.EWS = ews;
    }

    @Override
    public int getEWS() {
        return EWS;
    }

    @Override
    public int getRC() {
        return RC;
    }

    @Override
    public void setRC(int rc) {
        this.RC = rc;
    }

    @Override
    public void setRecommendationStrategy(RecommendStrategiesEnum strat) {
        this.recommendationStrategy = strat;
    }

    @Override
    public RecommendStrategiesEnum getRecommendationStrategy() {
        return recommendationStrategy;
    }

    @Override
    public int getTcdiff() {
        return tcdiff;
    }

    @Override
    public void setTcdiff(int tcdiff) {
        this.tcdiff = tcdiff;
    }

    @Override
    public String getOutputFile() {
        return outputFile;
    }

    @Override
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public Boolean getExtractDetails() {
        return extractDetails;
    }

    @Override
    public void setExtractDetails(Boolean extractDetails) {
        this.extractDetails = extractDetails;
    }

    @Override
    public void setTransactionCounter(int count) {
        this.transactionCounter = count;
    }

    @Override
    public void incrementTransactionCounter() {
       this.transactionCounter++;
    }

    @Override
    public int getTransactionCounter() {
        return transactionCounter;
    }

    @Override
    public boolean getGrouping() {
        return grouping;
    }

    @Override
    public void setGrouping(boolean grouping) {
        this.grouping = grouping;
    }

    @Override
    public long getStreamStartTime() {
        return streamStartTime;
    }

    @Override
    public void setStreamStartTime(long streamStartTime) {
        this.streamStartTime = streamStartTime;
    }

    @Override
    public int getExtractSpeedResultsAt() {
        return extractSpeedResultsAt;
    }

    @Override
    public void setExtractSpeedResultsAt(int extractSpeedResultsAt) {
        this.extractSpeedResultsAt = extractSpeedResultsAt;
    }

    @Override
    public int getStartEvaluatingFrom() {
        return startEvaluatingFrom;
    }

    @Override
    public void setStartEvaluatingFrom(int startEvaluatingFrom) {
        this.startEvaluatingFrom = startEvaluatingFrom;
    }

    public int getTUC() {
        return TUC;
    }

    @Override
    public void setTUC(int TUC) {
        this.TUC = TUC;
    }

    @Override
    public int getTCM() {
        return TCM;
    }

    @Override
    public void setTCM(int TCM) {
        this.TCM = TCM;
    }

    @Override
    public int getUserModelDimensions() {
        return userModelDimensions;
    }

    @Override
    public void setUserModelDimensions(int userModelDimensions) {
        this.userModelDimensions = userModelDimensions;
    }

    @Override
    public void setMTS(int mts) {
         this.MTS = mts;
    }

    @Override
    public int getMTS() {
        return MTS;
    }

    @Override
    public int getExtractPatternsAt() {
        return extractPatternsAt;
    }

    @Override
    public void setExtractPatternsAt(int ext) {
        extractPatternsAt = ext;
    }

    @Override
    public boolean getEvaluateSpeed() {
        return evaluateSpeed;
    }

    @Override
    public void setEvaluateSpeed(boolean evaluateSpeed) {
        this.evaluateSpeed = evaluateSpeed;
    }

    public DistanceMetricsEnum getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(DistanceMetricsEnum distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

   
    @Override
    public void setD(double d) {
        this.d = d;
    }

    @Override
    public double getD() {
        return d;
    }

    @Override
    public void setMinSigValue(double v) {
        this.minSigValue = v;
    }

    @Override
    public double getMinSigValue() {
        return minSigValue;
    }

    @Override
    public void setDeltaValue(double v) {
        this.deltaValue = v;
    }

    @Override
    public double getDeltaValue() {
        return deltaValue;
    }

    @Override
    public void setMinMergeValue(double v) {
        this.minMergeValue = v;
    }

    @Override
    public double getMinMergeValue() {
        return minMergeValue;
    }

  
}
