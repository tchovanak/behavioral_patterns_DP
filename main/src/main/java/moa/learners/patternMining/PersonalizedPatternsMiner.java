package moa.learners.patternMining;

import moa.core.dto.RecommendationResults;
import moa.core.dto.FIWrapper;

import moa.learners.clustering.UserModelPPSDM;
import java.util.*;
import moa.MOAObject;
import moa.core.*;

import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.InstanceExample;
import com.yahoo.labs.samoa.instances.SparseInstance;
import java.util.concurrent.ConcurrentHashMap;

import moa.core.Configuration;
import moa.learners.recommendation.RecommendationGenerator;

import moa.learners.clustering.ClustererClustream;
import moa.learners.clustering.ClusteringComponent;
import moa.core.dto.GroupStatsResults;
import moa.core.dto.SnapshotResults;
import moa.learners.patternMining.PatternMiningComponent;
import moa.learners.patternMining.PatternMiningConfiguration;
import moa.learners.patternMining.PatternMiningIncMine;
import moa.evaluation.RecommendationEvaluator;
import moa.learners.AbstractLearner;
import moa.learners.Learner;

/*
    This class defines new AbstractLearner that performs clustering of users to groups and 
    mining of global and group frequent itemsets.
    
*/
public class PersonalizedPatternsMiner extends AbstractLearner implements Observer {
    
    private static final long serialVersionUID = 1L;
    
    private PatternMiningComponent patternsMiner;
    private ClusteringComponent clustererPPSDM;
    private RecommendationGenerator recommendationGenerator;
    
    private Map<Integer,Integer> catsToSupercats = new ConcurrentHashMap<>();
    
    private int microclusteringUpdatesCounter = 0;
    private PatternMiningConfiguration config;
    
    
    public PersonalizedPatternsMiner(PatternMiningConfiguration config, 
            PatternMiningComponent patternsMiner,
            ClusteringComponent clusteringComponent,
            RecommendationGenerator recGenerator){
        super();
        this.config = config;
        this.patternsMiner = patternsMiner;
        this.clustererPPSDM = clusteringComponent;
        this.recommendationGenerator = recGenerator;
        
    }
    
    public PersonalizedPatternsMiner(Map<Integer,Integer> map, Configuration config,
            PatternMiningComponent patternsMiner,
            ClusteringComponent clusteringComponent,RecommendationGenerator recGenerator) {
        this(config,patternsMiner, clusteringComponent, recGenerator);
        this.catsToSupercats = map;    
    }
    
    @Override
    public void resetLearningImpl() {
       
        // Initialize pattern mining component
        this.patternsMiner.resetLearning();
        
        // Initialize clustering component
        this.clustererPPSDM.resetLearning();
        
        this.recommendationGenerator.resetLearning();
      
        System.gc(); // force garbage collection
    }
    
    
    @Override
    public void trainOnInstance(Example e) {
        // A3: USER MODEL UPDATE
        Instance inst = (Instance) e.getData();
        if(config.getGrouping()){
            UserModelPPSDM um = clustererPPSDM.updateUserModel(inst.copy(), 
                    catsToSupercats, config.getTUC());
            if(um.getNumOfNewSessions() > config.getTUC()){
                // A4: NULLING USER MODEL CHANGES NUMBER
                Instance umInstance = um.getNewSparseInstance(config.getUserModelDimensions());
                // A5: UPDATE MICROCLUSTERS AND INCREMENT MICROCLUSTERS CHANGES
                clustererPPSDM.trainOnInstance(umInstance);
                this.microclusteringUpdatesCounter++;
                if(this.microclusteringUpdatesCounter > config.getTCM()){
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
        return recommendationGenerator.generateRecommendations(e, config.getGrouping());
        
    }
    
    
    public SnapshotResults extractSnapshot(RecommendationEvaluator evaluator){
        
        if(!(((this.microclusteringUpdatesCounter * this.patternsMiner.getSnapshotId()) ==
                (evaluator.getConfiguration().getExtractPatternsAt() 
                * clustererPPSDM.getClusteringID())))){
            return null;
        }
        SnapshotResults snap = this.patternsMiner.generateSnapshot(clustererPPSDM);
        evaluator.setSnapshotResults(snap.getGroupStats());
        return snap;
    }
    
    
    public List<FIWrapper> extractPatterns(){
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


}
