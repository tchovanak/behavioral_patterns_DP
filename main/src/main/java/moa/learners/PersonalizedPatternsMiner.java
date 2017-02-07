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
import moa.core.PPSDM.FCITablePPSDM;
import moa.core.PPSDM.clustering.ClustererClustream;
import moa.core.PPSDM.clustering.ClusteringComponent;
import moa.core.PPSDM.dto.GroupStatsResults;
import moa.core.PPSDM.dto.SnapshotResults;
import moa.core.PPSDM.patternMining.PatternMiningComponent;
import moa.core.PPSDM.patternMining.PatternMiningIncMine;
import moa.core.PPSDM.utils.MapUtil;
import moa.core.PPSDM.utils.UtilitiesPPSDM;
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
    private PatternMiningComponent patternMiner;
    private ClusteringComponent clustererPPSDM;
    
    private Map<Integer,UserModelPPSDM> usermodels = new ConcurrentHashMap<>();
    private Map<Integer,Integer> catsToSupercats = new ConcurrentHashMap<>();
    
    private int microclusteringUpdatesCounter = 0;
    private int snapshotId = 1;
    
    private int cntAll = 0;
    private int cntOnlyGroup;
    private int cntOnlyGlobal;
    private List<Integer> recsOnlyFromGlobal = new ArrayList<>();
    private List<Integer> recsOnlyFromGroup = new ArrayList<>();
    private List<Integer> recsCombined = new ArrayList<>();
    
    
    public PersonalizedPatternsMiner(){
        super();
    }
    
    public PersonalizedPatternsMiner(Map<Integer,Integer> map) {
        this();
        this.catsToSupercats = map;
        
    }

    public PersonalizedPatternsMiner(boolean grouping) {
        super();
        this.useGrouping = grouping;
    }
    
    @Override
    public void resetLearningImpl() {
       
        // Initialize pattern mining component
        this.patternMiner = new PatternMiningIncMine(windowSize, maxItemsetLength,
                numberOfGroups, minSupport, relaxationRate,fixedSegmentLength, 
                groupFixedSegmentLength);
        
        // Initialize clustering component
        this.clustererPPSDM = new ClustererClustream(numberOfGroups,
            maxNumKernels,
            kernelRadiFactor);
        
        // clear usermodels map
        usermodels.clear();
        usermodels = new ConcurrentHashMap<>();
        
        System.gc(); // force garbage collection
    }
    
    
    @Override
    public void trainOnInstance(Example e) {
        // A3: USER MODEL UPDATE
        Instance inst = (Instance) e.getData();
        if(useGrouping){
            UserModelPPSDM um = updateUserModel(inst.copy());
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
                    this.clearUserModels();
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
        incMine.trainOnInstance(e.copy());   // then train on instance without groupid - global
    }
    
    
    public RecommendationResults getRecommendationsForInstance(Example e) {
        // append group to instance that it belongs to...
        Instance session = (Instance)e.getData();
        // we need to copy instance data to sessionArray where we can modify them 
        // because data in Instance cannot be changed i dont know why...
        ArrayList<Double> sessionArray = new ArrayList<>(); 
        for(int i = 0; i < session.numValues(); i++){
            sessionArray.add(i,session.value(i)); 
        }
        // TESTING Instance - how it performs on recommendation.
        // get window from actual instance
        List<Integer> window = new ArrayList<>(); // items inside window 
        List<Integer> outOfWindow = new ArrayList<>(); // items out of window 

        if((evaluationWindowSize >= (sessionArray.size()-2))){ 
            return null; // this is when session array is too short - it is ignored.
        }
        
        for(int i = 2; i <= evaluationWindowSize + 1; i++){ // first item is groupid, 2nd uid
            window.add((int) Math.round(sessionArray.get(i)));
        }
        
        // maximum number of evaluated future items is the same as number of recommended items.
        for(int i = evaluationWindowSize + 2, j = 0; i < sessionArray.size(); i++){
            outOfWindow.add((int) Math.round(sessionArray.get(i)));
        }
        
        //to get all fcis found 
        List<FciValue> mapFciWeight = new LinkedList<>();
        List<FciValue> mapFciWeightGroup = new LinkedList<>();
        List<FciValue> mapFciWeightGlobal = new LinkedList<>(); 
        Iterator<FrequentItemset> itFis = null;
        if(Configuration.RECOMMEND_WITH_FI){
           itFis = this.incMine.fciTableGlobal.getFcis().iterator();
        }else{
           itFis = this.incMine.fciTableGlobal.getSemiFcis().iterator();
        }
        //Iterator<SemiFCI> itFis = this.incMine.fciTableGlobal.iterator();
        int groupidSet = -1;
        while(itFis.hasNext()){
            FrequentItemset fi = itFis.next();
            //SemiFCI fi = itFis.next();
            if(fi.getSize() > 1){
                List<Integer> items = fi.getItems();
                double hitsVal = this.computeSimilarity(items,window);
                if(hitsVal == 0.0){
                    continue;
                }
                FciValue fciVal = new FciValue();
                fciVal.setItems(fi.getItems());
                fciVal.computeValue(hitsVal, fi.getSupportDouble());//.getSupportDouble());
                mapFciWeight.add(fciVal);
                mapFciWeightGlobal.add(fciVal);
            }
        }
        
        if(useGrouping && clustererPPSDM.isClustering()){
            // if already clustering was performed
            Double groupid = -1.0;
            UserModelPPSDM um = getUserModelFromInstance(session);
            double distance = 0;
            if(um != null){
                groupidSet = (int)um.getGroupid();
                distance = um.getDistance();
                groupid = um.getGroupid(); // groupids sorted by preference
            }else{
                sessionArray.set(0,-1.0);
            }            
            //         This next block performs the same with group fcis. 
            if(groupid != -1.0){
                Iterator<FrequentItemset> itFisG = null;
                        //this.incMine.fciTablesGroups.get((int) Math.round(groupid)).getSemiFcis().iterator();
                if(Configuration.RECOMMEND_WITH_FI){
                    itFisG = this.incMine.fciTablesGroups.get((int) Math.round(groupid)).getFcis().iterator();
                }else{
                    itFisG = this.incMine.fciTablesGroups.get((int) Math.round(groupid)).getSemiFcis().iterator();
                }
                //Iterator<SemiFCI> itFisG = this.incMine.fciTablesGroups.get((int) Math.round(groupid)).iterator();
                while(itFisG.hasNext()){
                    FrequentItemset fi = itFisG.next();
                    //SemiFCI fi = itFisG.next();
                    if(fi.getSize() > 1){
                        List<Integer> items = fi.getItems();
                        double hitsVal = this.computeSimilarity(items,window);
                        if(hitsVal == 0.0){
                            continue;
                        }
                        FciValue fciVal = new FciValue();
                        fciVal.setItems(fi.getItems());
                        fciVal.setDistance(distance);
                        fciVal.computeValue(hitsVal, fi.getSupportDouble());
                        mapFciWeight.add(fciVal);
                        mapFciWeightGroup.add(fciVal);
                   }
                }
            }
        }
        
        // all fcis found have to be sorted descending by its support and similarity.
        Collections.sort(mapFciWeight);
        Collections.sort(mapFciWeightGroup);
        Collections.sort(mapFciWeightGlobal);
        switch (Configuration.RECOMMEND_STRATEGY) {
            case VOTES:
                generateRecsVoteStrategy(mapFciWeightGlobal,
                        mapFciWeightGroup, window);
                break;
            case FIRST_WINS:
                generateRecsFirstWinsStrategy(mapFciWeight, mapFciWeightGlobal,
                        mapFciWeightGroup, window);
                break;
        }        
        RecommendationResults results = new RecommendationResults();
        results.setTestWindow(outOfWindow);
        results.setNumOfRecommendedItems(this.numberOfRecommendedItems);
        results.setRecommendationsGGC(recsCombined);
        results.setRecommendationsGO(recsOnlyFromGlobal);
        results.setRecommendationsOG(recsOnlyFromGroup);
        results.setGroupid(groupidSet);
        return results;
    }
    
    private double calculateSupport(SemiFCI fci)
    {
      double approxSupport = fci.getApproximateSupport();
      double support = approxSupport / (this.fixedSegmentLength * this.windowSize);
      return support;
    }
    
    public SnapshotResults extractSnapshot(PPSDMRecommendationEvaluator evaluator){
        if(!(((this.microclusteringUpdatesCounter * snapshotId) == 
                (Configuration.EXTRACT_PATTERNS_AT * clustererPPSDM.getClusteringID())))){
            return null;
        }
        SnapshotResults snap= new SnapshotResults();
        snap.setId(snapshotId++);
        List<GroupStatsResults> listGStats = new ArrayList<>();
        Map<List<Integer>, Integer> frequencyOfPatterns = new HashMap<>();
        FCITablePPSDM fciTableGlobal = this.incMine.fciTableGlobal;
        List<FCITablePPSDM> fciTablesGroup = this.incMine.fciTablesGroups;
        fciTableGlobal.computeFcis(this.minSupport,
                this.fixedSegmentLength);
        GroupStatsResults globRes = new GroupStatsResults();
        globRes.setGroupid(-1);
        globRes.setClusteringId(clustererPPSDM.getClusteringID());
        Iterator<FrequentItemset> itGlob = fciTableGlobal.getFcis().iterator();
        List<FciValue> globPatterns = new ArrayList<>();
        while(itGlob.hasNext()){
            FrequentItemset sfci = itGlob.next();
            if(frequencyOfPatterns.containsKey(sfci.getItems())){
                frequencyOfPatterns.put(sfci.getItems(), frequencyOfPatterns.get(sfci.getItems()));
            }else{
                frequencyOfPatterns.put(sfci.getItems(),1);
            }
            FciValue fciVal = new FciValue();
            fciVal.setItems(sfci.getItems());
            fciVal.setSupport(sfci.getSupportDouble());
            fciVal.setGroupid(-1);
            globPatterns.add(fciVal);
        }
        globRes.setFcis(globPatterns);
        listGStats.add(globRes);
        int groupid = 0;
        for(FCITablePPSDM gTable: fciTablesGroup){
            GroupStatsResults gRes = new GroupStatsResults();
            gRes.setClusteringId(clustererPPSDM.getClusteringID());
            gRes.setGroupid(groupid);
            gTable.computeFcis(this.minSupport,this.fixedSegmentLength);
            Iterator<FrequentItemset> itGr = gTable.getFcis().iterator();
            List<FciValue> groupPatterns = new ArrayList<>(); 
            while(itGr.hasNext()){
                FrequentItemset sfci = itGr.next();
                if(frequencyOfPatterns.containsKey(sfci.getItems())){
                    frequencyOfPatterns.put(sfci.getItems(), frequencyOfPatterns.get(sfci.getItems()) + 1);
                }else{
                    frequencyOfPatterns.put(sfci.getItems(),1);
                }
                FciValue fciVal = new FciValue();
                fciVal.setItems(sfci.getItems());
                fciVal.setSupport(sfci.getSupportDouble());
                fciVal.setGroupid(groupid);
                groupPatterns.add(fciVal);
            }
            groupid++;
            gRes.setFcis(groupPatterns);
            listGStats.add(gRes);
        }
        
        setUserModels(listGStats);
        for(GroupStatsResults gStat : listGStats){
            // compute averages
            //evaluator.getAveragePrecisionForGroup(gStat.getGroupid());
            //gStat.setAverageAccuracyForGroup(averageAccuracyForGroup);
            //gStat.setAveragePrecisionForGroup(averagePrecisionForGroup);
            // COMPUTE NUM OF UNIQUE PATTERNS
            int numOfUniquePatterns = 0;
            double sumSupportUniquePatterns = 0;
            double sumSupportAllPatterns = 0;
            for(FciValue fci : gStat.getFcis()){
                List<Integer> items = fci.getItems();
                int freq = frequencyOfPatterns.get(items);
                if(freq == 1){
                    numOfUniquePatterns++;
                    sumSupportUniquePatterns += fci.getSupport();
                }
                sumSupportAllPatterns += fci.getSupport();
            }
            gStat.setNumOfUniquePatterns(numOfUniquePatterns);
            gStat.setAverageSupportOfUniquePatterns(
                    sumSupportUniquePatterns/numOfUniquePatterns
            );
            gStat.setAverageSupport(sumSupportAllPatterns/gStat.getFcis().size());
        }
        evaluator.setSnapshotResults(listGStats);
        snap.setGroupStats(listGStats);
        return snap;
    }
    
    public List<FciValue> extractPatterns(){
        FCITablePPSDM fciTableGlobal = this.incMine.fciTableGlobal;
        List<FCITablePPSDM> fciTablesGroup = this.incMine.fciTablesGroups;
        List<FciValue> allPatterns = new ArrayList<>();
        
        fciTableGlobal.computeFcis(this.minSupport, this.fixedSegmentLength);
        Iterator<FrequentItemset> itGlob = fciTableGlobal.getFcis().iterator();
        while(itGlob.hasNext()){
            FrequentItemset sfci = itGlob.next();
            //double support = this.calculateSupport(sfci);
            //if(support >= this.minSupportOption.getValue()){
                FciValue fciVal = new FciValue();
                fciVal.setItems(sfci.getItems());
                fciVal.setSupport(sfci.getSupportDouble());
                fciVal.setGroupid(-1);
                allPatterns.add(fciVal);
            //}
        }
        
        int groupid = 0;
        for(FCITablePPSDM gTable: fciTablesGroup){
            
            gTable.computeFcis(this.minSupport, this.fixedSegmentLength);
            Iterator<FrequentItemset> itGr = gTable.getFcis().iterator();
            while(itGr.hasNext()){
                FrequentItemset sfci = itGr.next();
                //double support = this.calculateSupport(sfci);
                //if(support >= this.minSupportOption.getValue()){
                    FciValue fciVal = new FciValue();
                    fciVal.setItems(sfci.getItems());
                    //fciVal.setFci(sfci);
                    fciVal.setSupport(sfci.getSupportDouble());
                    fciVal.setGroupid(groupid);
                    allPatterns.add(fciVal);
                //}
            }
            groupid++;
        }
        return allPatterns;
    }
    
    private void setUserModels(List<GroupStatsResults> results){
        if(clustererPPSDM.isClustering()){
            for(int i = 0; i < clustererPPSDM.getClustering().getClustering().size(); i++){
                GroupStatsResults res = results.get(i);
                res.setCentre(clustererPPSDM.getClustering().getClustering().get(i).getCenter());
            }
        }
        for(UserModelPPSDM um : this.usermodels.values()){
            if(clustererPPSDM.getClusteringID() - um.getClusteringId() == 0){
                results.get((int)um.getGroupid() + 1).addUid(um.getId());
            }else{
                //results.get(0).addUid(um.getId());
            }
        }
    }
    
    public List<GroupStatsResults> extractUserModels(){
        List<GroupStatsResults> results = new ArrayList<>();
        GroupStatsResults res = new GroupStatsResults();
        res.setGroupid(-1);
        res.setClusteringId(clustererPPSDM.getClusteringID());
        results.add(res);
        if(clustererPPSDM.isClustering()){
            for(int i = 0; i < clustererPPSDM.getClustering().size(); i++){
                res = new GroupStatsResults();
                res.setGroupid(i);
                res.setClusteringId(clustererPPSDM.getClusteringID());
                res.setCentre(clustererPPSDM.getClustering().get(i).getCenter());
                results.add(res);
                //res.setUids();
            }
        }
        for(UserModelPPSDM um : this.usermodels.values()){
            if(clustererPPSDM.getClusteringID() - um.getClusteringId() <= 1){
                results.get((int)um.getGroupid() + 1).addUid(um.getId());
            }else{
                results.get(0).addUid(um.getId());
            }
        }
        return results;
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
    
    private UserModelPPSDM updateUserModel(Instance inst){
        int uid = (int)inst.value(1);
        if(usermodels.containsKey(uid)){
            UserModelPPSDM um = usermodels.get(uid);
            if(this.catsToSupercats.size() > 0){
                um.updateWithInstance(inst,catsToSupercats);
            }else{
                um.updateWithInstance(inst);
            }
            return um;
        }else{
            UserModelPPSDM um = new UserModelPPSDM((int)inst.value(1), 
                    this.numMinNumberOfChangesInUserModel);
            if(this.catsToSupercats.size() > 0){
                um.updateWithInstance(inst,catsToSupercats);
            }else{
                um.updateWithInstance(inst);
            }
            usermodels.put(uid, um);
            return um;
        }
    }
    
    private UserModelPPSDM getUserModelFromInstance(Instance inst) {
        int uid = (int)inst.value(1);
         if(usermodels.containsKey(uid)){
            UserModelPPSDM um = usermodels.get(uid);
            return um;
        }else{
            return null;
        }
    }
    
    private double computeSimilarity(List<Integer> items, List<Integer> window) {
        return ((double)UtilitiesPPSDM.computeLongestCommonSubset(items,window)) / 
                ((double)window.size());
    }

    private void generateRecsVoteStrategy(
                                    List<FciValue> mapFciWeightGlobal,
                                    List<FciValue> mapFciWeightGroup, 
                                    List<Integer> window) {
        Map<Integer, Double> mapItemsVotes = new HashMap<>();
        Map<Integer, Double> mapItemsVotesOnlyGlobal = new HashMap<>();
        Map<Integer, Double> mapItemsVotesOnlyGroup = new HashMap<>();
        Iterator<FciValue> itGlobal = mapFciWeightGlobal.iterator();
        Iterator<FciValue> itGroup = mapFciWeightGroup.iterator();
        
        while(itGlobal.hasNext() || itGroup.hasNext()){
            if(itGlobal.hasNext()){
               FciValue fci = itGlobal.next();
               Iterator<Integer> itFciItems = fci.getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();         
                   if(mapItemsVotes.containsKey(item)){   
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport();
                       mapItemsVotes.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport();
                           mapItemsVotes.put(item, newVal);
                       }
                   }
                   if(mapItemsVotesOnlyGlobal.containsKey(item)){
                        Double newVal = mapItemsVotesOnlyGlobal.get(item) + fci.getLcsVal()*fci.getSupport();
                        mapItemsVotesOnlyGlobal.put(item, newVal);
                   }else{
                        if(!window.contains(item)){
                            Double newVal =  fci.getLcsVal()*fci.getSupport();
                            mapItemsVotesOnlyGlobal.put(item, newVal);
                        }
                   }
               }
            }
            if(itGroup.hasNext()){
               FciValue fci = itGroup.next();
               Iterator<Integer> itFciItems = fci.getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();
                    double dist = fci.getDistance();
                    if(dist == 0.0){
                        dist = 1.0;
                    }else{
                        if(dist < 0){
                            dist = -dist;
                        }
                        dist = 1.0-dist;
                    }
                   if(mapItemsVotes.containsKey(item)){
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport()*(dist);
                       mapItemsVotes.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport()*(dist);
                           mapItemsVotes.put(item, newVal);
                       }
                   }  
                   if(mapItemsVotesOnlyGroup.containsKey(item)){
                       Double newVal = mapItemsVotesOnlyGroup.get(item) + fci.getLcsVal()*fci.getSupport()*dist;
                       mapItemsVotesOnlyGroup.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport()*dist;
                           mapItemsVotesOnlyGroup.put(item, newVal);
                       }
                   }   
               }
            }
        }
        mapItemsVotes = MapUtil.sortByValue(mapItemsVotes);
        mapItemsVotesOnlyGlobal = MapUtil.sortByValue(mapItemsVotesOnlyGlobal);
        mapItemsVotesOnlyGroup = MapUtil.sortByValue(mapItemsVotesOnlyGroup);
        recsCombined = new ArrayList<>();
        recsOnlyFromGlobal = new ArrayList<>();
        recsOnlyFromGroup = new ArrayList<>();
        int numRecommendedItems = this.numberOfRecommendedItems;
        cntAll = 0;
        cntOnlyGlobal = 0;
        cntOnlyGroup = 0;
        
        for(Map.Entry<Integer,Double> e : mapItemsVotes.entrySet()) {
            Integer item = e.getKey();
            recsCombined.add(item);
            cntAll++;
            if(cntAll >= numRecommendedItems){
                break;
            }       
        }
        for(Map.Entry<Integer,Double> e : mapItemsVotesOnlyGlobal.entrySet()) {
            Integer item = e.getKey();
            recsOnlyFromGlobal.add(item);
            cntOnlyGlobal++;
            if(cntOnlyGlobal >= numRecommendedItems){
                break;
            } 
        }
        for(Map.Entry<Integer,Double> e : mapItemsVotesOnlyGroup.entrySet()) {
            Integer item = e.getKey();
            recsOnlyFromGroup.add(item); 
            cntOnlyGroup++;
            if(cntOnlyGroup >= numRecommendedItems){
                break;
            } 
        }
    }

    private void generateRecsFirstWinsStrategy(List<FciValue> mapFciWeight,
                                    List<FciValue> mapFciWeightGlobal, 
                                    List<FciValue> mapFciWeightGroup, 
                                    List<Integer> window) {
        cntAll = 0;
        cntOnlyGroup = 0; 
        cntOnlyGlobal = 0;
        recsCombined = new ArrayList<>();
        recsOnlyFromGroup = new ArrayList<>();
        recsOnlyFromGlobal = new ArrayList<>();
        int numRecommendedItems = this.numberOfRecommendedItems;
        
        for(FciValue fciVal : mapFciWeight) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsCombined.contains(item)){  // create unique recommendations
                    recsCombined.add(item);
                    cntAll++;
                    if(cntAll >= numRecommendedItems){
                        break;
                    }
                }
            }
            if(cntAll >=  numRecommendedItems){
                 break;
            }
        }
        
        
        for(FciValue fciVal : mapFciWeightGroup) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsOnlyFromGroup.contains(item)){  // create unique recommendations
                    recsOnlyFromGroup.add(item);
                    cntOnlyGroup++;
                    if(cntOnlyGroup >=  numRecommendedItems){
                        break;
                    }
                }
            }
            if(cntOnlyGroup >=  numRecommendedItems){
                 break;
            }
        }
        
        for(FciValue fciVal : mapFciWeightGlobal) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsOnlyFromGlobal.contains(item)){  // create unique recommendations
                    recsOnlyFromGlobal.add(item);
                    cntOnlyGlobal++;
                    if(cntOnlyGlobal >=  numRecommendedItems){
                        break;
                    }
                }
            }
            if(cntOnlyGlobal >=  numRecommendedItems){
                 break;
            }
        }
    }

    /**
     * Removes old user models - if it has clustering id older than minimal user model updates
     */
    public void clearUserModels() {
        for(Map.Entry<Integer, UserModelPPSDM> entry : this.usermodels.entrySet()){
            UserModelPPSDM model = entry.getValue();
            if(clustererPPSDM.getClusteringID() - model.getClusteringId() > Configuration.MAX_DIFFERENCE_OF_CLUSTERING_ID){
                // delete 
                this.usermodels.remove(entry.getKey());
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Boolean getUseGrouping() {
        return useGrouping;
    }

}
