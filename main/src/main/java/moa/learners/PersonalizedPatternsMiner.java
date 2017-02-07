package moa.learners;

import moa.core.PPSDM.dto.RecommendationResults;
import moa.core.PPSDM.FciValue;

import moa.core.PPSDM.UserModelPPSDM;
import java.util.*;
import moa.MOAObject;
import moa.core.*;

import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.InstanceExample;
import com.yahoo.labs.samoa.instances.SparseInstance;
import java.util.concurrent.ConcurrentHashMap;

import moa.core.PPSDM.Configuration;
import moa.core.PPSDM.RecommendationGenerator;

import moa.core.PPSDM.clustering.ClustererClustream;
import moa.core.PPSDM.clustering.ClusteringComponent;
import moa.core.PPSDM.dto.GroupStatsResults;
import moa.core.PPSDM.dto.SnapshotResults;
import moa.core.PPSDM.patternMining.PatternMiningComponent;
import moa.core.PPSDM.patternMining.PatternMiningIncMine;
import moa.evaluation.PPSDMRecommendationEvaluator;

/*
    This class defines new AbstractLearner that performs clustering of users to groups and 
    mining of global and group frequent itemsets.
    
*/
public class PersonalizedPatternsMiner extends AbstractLearner implements Observer {
    
    private static final long serialVersionUID = 1L;
    
    public Integer numMinNumberOfChangesInUserModel;
    public Integer numMinNumberOfMicroclustersUpdates;
    public Integer numOfDimensionsInUserModel;
    public Integer windowSize;
    public Integer maxItemsetLength;
    public Integer numberOfGroups;
    public Double  minSupport;
    public Double  relaxationRate;
    public Integer fixedSegmentLength;
    public Integer groupFixedSegmentLength;
    public Integer numberOfRecommendedItems;
    public Integer evaluationWindowSize;
    public Integer maxNumKernels;
    public Integer kernelRadiFactor;
    public Boolean useGrouping;

    
    //private PersonalizedIncMine incMine;
    private PatternMiningComponent patternsMiner;
    private ClusteringComponent clustererPPSDM;
    private RecommendationGenerator recommendationGenerator;
    
    private Map<Integer,Integer> catsToSupercats = new ConcurrentHashMap<>();
    
    private int microclusteringUpdatesCounter = 0;
    private int snapshotId = 1;
    
    public PersonalizedPatternsMiner(){
        super();
    }
    
    public PersonalizedPatternsMiner(Map<Integer,Integer> map) {
        this();
        this.catsToSupercats = map;    
    }
    
    @Override
    public void resetLearningImpl() {
       
        // Initialize pattern mining component
        this.patternsMiner = new PatternMiningIncMine(windowSize, maxItemsetLength,
                numberOfGroups, minSupport, relaxationRate,fixedSegmentLength, 
                groupFixedSegmentLength);
        
        // Initialize clustering component
        this.clustererPPSDM = new ClustererClustream(numberOfGroups,
            maxNumKernels,
            kernelRadiFactor);
        
        this.recommendationGenerator = new RecommendationGenerator(evaluationWindowSize,
                patternsMiner,clustererPPSDM,numberOfRecommendedItems);
      
        System.gc(); // force garbage collection
    }
    
    
    @Override
    public void trainOnInstance(Example e) {
        // A3: USER MODEL UPDATE
        Instance inst = (Instance) e.getData();
        if(useGrouping){
            UserModelPPSDM um = clustererPPSDM.updateUserModel(inst.copy(), 
                    catsToSupercats, this.numMinNumberOfChangesInUserModel);
            if(um.getNumOfNewSessions() > this.numMinNumberOfChangesInUserModel){
                // A4: NULLING USER MODEL CHANGES NUMBER
                Instance umInstance = um.getNewSparseInstance(numOfDimensionsInUserModel);
                // A5: UPDATE MICROCLUSTERS AND INCREMENT MICROCLUSTERS CHANGES
                clustererPPSDM.trainOnInstance(umInstance);
                this.microclusteringUpdatesCounter++;
                if(this.microclusteringUpdatesCounter > this.numMinNumberOfMicroclustersUpdates){
                    // perform macroclustering
                    // A6: NULLING MICROCLUSTERS UPDATES COUNTER
                    this.microclusteringUpdatesCounter = 0;
                    // A7: PERFORM MACROCLUSTERING
                    clustererPPSDM.performMacroclustering();
                    // A8 : CLEAR OLD USER MODELS
                    clustererPPSDM.clearUserModels();
                }
            }
            clustererPPSDM.updateGroupingInUserModel(um);
            double groupid = um.getGroupid(); // now update instance with groupid from user model
            if(groupid > -1){   
                int nItems = inst.numValues();
                double[] attValues = new double[nItems];
                int[] indices = new int[nItems];
                attValues[0] = (int)groupid; 
                indices[0]   = 0;
                for(int idx = 1; idx < nItems; idx++){
                    attValues[idx] = inst.value(idx);
                    indices[idx] =  inst.index(idx);
                }
                Instance instanceWithGroupid = new SparseInstance(1.0,attValues,indices,nItems);
                InstanceExample instEx = new InstanceExample(instanceWithGroupid);
                //incMine.trainOnInstance(instEx.copy()); // first train on instance with groupid - group
                patternsMiner.trainOnInstance(instEx.copy());
            }
        }
        patternsMiner.trainOnInstance(e.copy());   // then train on instance without groupid - global
    }
    
    
    public RecommendationResults getRecommendationsForInstance(Example e) {
        return recommendationGenerator.generateRecommendations(e, useGrouping);
        
    }
    
    
    public SnapshotResults extractSnapshot(PPSDMRecommendationEvaluator evaluator){
        if(!(((this.microclusteringUpdatesCounter * this.patternsMiner.getSnapshotId()) == 
                (Configuration.EXTRACT_PATTERNS_AT * clustererPPSDM.getClusteringID())))){
            return null;
        }
        SnapshotResults snap = this.patternsMiner.generateSnapshot(clustererPPSDM);
        evaluator.setSnapshotResults(snap.getGroupStats());
        return snap;
    }
    
    
    public List<FciValue> extractPatterns(){
        return this.patternsMiner.extractPatterns();
    }
    
    
    public List<GroupStatsResults> extractUserModels(){
        return this.clustererPPSDM.extractUserModels();
    }
    
    
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        System.out.println("trainOnInstanceImpl");
    }

    
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        System.out.println("getModelMeasurementsImpl");
        return null;
    }

    
    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        System.out.println("");
    }

    
    @Override
    public boolean isRandomizable() {
         return false;
    }

    @Override
    public Learner[] getSublearners() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public MOAObject getModel() {
         return null;
    }

    
    @Override
    public Prediction getPredictionForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public void update(Observable o, Object arg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public double[] getVotesForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    public Boolean getUseGrouping() {
        return useGrouping;
    }

}
