/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.recommendation;

import moa.core.dto.FIWrapper;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import moa.core.BehavioralPattern;
import moa.core.Example;
import moa.core.FrequentItemset;
import moa.learners.clustering.UserModel;
import moa.learners.clustering.ClusteringComponent;
import moa.core.dto.RecommendationResults;
import moa.learners.patternMining.PatternMiningComponent;
import moa.core.utils.MapUtil;
import moa.core.utils.UtilitiesPPSDM;
import moa.core.PPSDM.SemiFCI;

/**
 * Generates recommendations for specific sessions.
 * @author Tomas
 */
public class RecommendationGenerator {
    
    private PatternMiningComponent patternsMiner;
    private ClusteringComponent clustererPPSDM;
    
    private List<Integer> recsCombined;
    private List<Integer> recsOnlyFromGlobal;
    private List<Integer> recsOnlyFromGroup;
    private int cntAll;
    private int cntOnlyGlobal;
    private int cntOnlyGroup;
    private RecommendationConfiguration config;

    
    public RecommendationGenerator(RecommendationConfiguration config, 
            PatternMiningComponent patternsMiner,
            ClusteringComponent clustererPPSDM) {
        this.config = config;
        this.patternsMiner = patternsMiner;
        this.clustererPPSDM = clustererPPSDM;
       
    }
    
    public void resetLearning() {
        this.patternsMiner.resetLearning();
        this.clustererPPSDM.resetLearning();
    }
    
    
    public RecommendationResults generateRecommendations(Example e, boolean useGrouping) {
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

        if((config.getEWS() >= (sessionArray.size()-2))){ 
            return null; // this is when session array is too short - it is ignored.
        }
        
        for(int i = 2; i <= config.getEWS() + 1; i++){ // first item is groupid, 2nd uid
            window.add((int) Math.round(sessionArray.get(i)));
        }
        
        // maximum number of evaluated future items is the same as number of recommended items.
        for(int i = config.getEWS() + 2, j = 0; i < sessionArray.size(); i++){
            outOfWindow.add((int) Math.round(sessionArray.get(i)));
        }
        
        //to get all fcis found 
        Map<BehavioralPattern, Double> mapFciWeight = new HashMap<>();
        Map<BehavioralPattern, Double> mapFciWeightGroup = new HashMap<>();
        Map<BehavioralPattern, Double> mapFciWeightGlobal = new HashMap<>(); 
        Iterator<BehavioralPattern> itFis = null;
        itFis = this.patternsMiner.iteratorGlobalPatterns();
        int groupidSet = -1;
        
        while(itFis.hasNext()){
            BehavioralPattern fi = itFis.next();
            if(fi.getSize() > 1){
                double hitsVal = fi.computeSimilarityTo(window);
                if(hitsVal == 0.0){
                    continue;
                }
                mapFciWeight.put(fi,hitsVal);
                mapFciWeightGlobal.put(fi,hitsVal);
            }
        }
        
        if(useGrouping && clustererPPSDM.isClustering()){
            // if already clustering was performed
            Double groupid = -1.0;
            UserModel um = clustererPPSDM.getUserModelFromInstance(session);
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
                Iterator<BehavioralPattern> itFisG = null;
                itFisG = this.patternsMiner.iteratorGroupPatterns((int) Math.round(groupid));
                
                while(itFisG.hasNext()){
                    BehavioralPattern fi = itFisG.next();
                    if(fi.getSize() > 1){
                        double hitsVal = fi.computeSimilarityTo(window);
                        if(hitsVal == 0.0){
                            continue;
                        }
                        fi.setWeightFactor(distance);
                        mapFciWeight.put(fi,hitsVal);
                        mapFciWeightGroup.put(fi,hitsVal);
                   }
                }
            }
        }
        
        // all fcis found have to be sorted descending by its support and similarity.
//        mapFciWeight = MapUtil.sortByValue(mapFciWeight);
//        mapFciWeightGroup = MapUtil.sortByValue(mapFciWeightGroup);
//        mapFciWeightGlobal = MapUtil.sortByValueThenKey(mapFciWeightGlobal);
//        
        switch (config.getRecommendationStrategy()) {
            case VOTES:
                generateRecsVoteStrategy(mapFciWeightGlobal,
                        mapFciWeightGroup, window);
                break;
//            case FIRST_WINS:
//                generateRecsFirstWinsStrategy(mapFciWeight, mapFciWeightGlobal,
//                        mapFciWeightGroup, window);
//                break;
        }        
        RecommendationResults results = new RecommendationResults();
        results.setTestWindow(outOfWindow);
        results.setNumOfRecommendedItems(config.getRC());
        results.setRecommendationsGGC(recsCombined);
        results.setRecommendationsGO(recsOnlyFromGlobal);
        results.setRecommendationsOG(recsOnlyFromGroup);
        results.setGroupid(groupidSet);
        return results;
    }
    
   
    
    private void generateRecsVoteStrategy(
                                    Map<BehavioralPattern,Double> mapFciWeightGlobal,
                                    Map<BehavioralPattern,Double> mapFciWeightGroup, 
                                    List<Integer> window) {
        Map<Integer, Double> mapItemsVotes = new HashMap<>();
        Map<Integer, Double> mapItemsVotesOnlyGlobal = new HashMap<>();
        Map<Integer, Double> mapItemsVotesOnlyGroup = new HashMap<>();
        Iterator<Map.Entry<BehavioralPattern,Double>> itGlobal = mapFciWeightGlobal.entrySet().iterator();
        Iterator<Map.Entry<BehavioralPattern,Double>> itGroup = mapFciWeightGroup.entrySet().iterator();
        
        while(itGlobal.hasNext() || itGroup.hasNext()){
            if(itGlobal.hasNext()){
               Map.Entry<BehavioralPattern,Double> fci = itGlobal.next();
               Iterator<Integer> itFciItems = fci.getKey().getConsequenceOf(window).iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();         
                   if(mapItemsVotes.containsKey(item)){   
                       Double newVal = mapItemsVotes.get(item) + fci.getValue()*fci.getKey().getWeight();
                       mapItemsVotes.put(item, newVal);
                   }else{
                        Double newVal =  fci.getValue()*fci.getKey().getWeight();
                        mapItemsVotes.put(item, newVal);
                   }
                   if(mapItemsVotesOnlyGlobal.containsKey(item)){
                        Double newVal = mapItemsVotesOnlyGlobal.get(item) + fci.getValue()*fci.getKey().getWeight();
                        mapItemsVotesOnlyGlobal.put(item, newVal);
                   }else{
                        Double newVal =   fci.getValue()*fci.getKey().getWeight();
                        mapItemsVotesOnlyGlobal.put(item, newVal);
                   }
               }
            }
            if(itGroup.hasNext()){
               Map.Entry<BehavioralPattern, Double> fci = itGroup.next();
               Iterator<Integer> itFciItems = fci.getKey().getConsequenceOf(window).iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();
                    double dist = fci.getKey().getWeightFactor();
                    if(dist == 0.0){
                        dist = 1.0;
                    }else{
                        if(dist < 0){
                            dist = -dist;
                        }
                        dist = 1.0-dist;
                    }
                   if(mapItemsVotes.containsKey(item)){
                       Double newVal = mapItemsVotes.get(item) + fci.getValue()*fci.getKey().getWeight()*(dist);
                       mapItemsVotes.put(item, newVal);
                   }else{
                        Double newVal =  fci.getValue()*fci.getKey().getWeight()*(dist);
                        mapItemsVotes.put(item, newVal);
                   }  
                   if(mapItemsVotesOnlyGroup.containsKey(item)){
                       Double newVal = mapItemsVotesOnlyGroup.get(item) +fci.getValue()*fci.getKey().getWeight()*dist;
                       mapItemsVotesOnlyGroup.put(item, newVal);
                   }else{
                        Double newVal =  fci.getValue()*fci.getKey().getWeight()*dist;
                        mapItemsVotesOnlyGroup.put(item, newVal);
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
        int numRecommendedItems = config.getRC();
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

    
    private void generateRecsFirstWinsStrategy(List<FIWrapper> mapFciWeight,
                                    List<FIWrapper> mapFciWeightGlobal, 
                                    List<FIWrapper> mapFciWeightGroup, 
                                    List<Integer> window) {
        cntAll = 0;
        cntOnlyGroup = 0; 
        cntOnlyGlobal = 0;
        recsCombined = new ArrayList<>();
        recsOnlyFromGroup = new ArrayList<>();
        recsOnlyFromGlobal = new ArrayList<>();
        int numRecommendedItems = config.getRC();
        
        for(FIWrapper fciVal : mapFciWeight) {
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
        
        
        for(FIWrapper fciVal : mapFciWeightGroup) {
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
        
        for(FIWrapper fciVal : mapFciWeightGlobal) {
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

  
}
