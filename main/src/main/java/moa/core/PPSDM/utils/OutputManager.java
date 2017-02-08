/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.PPSDM.Configuration;
import moa.core.PPSDM.FciValue;
import moa.core.PPSDM.dto.GroupStatsResults;
import moa.core.PPSDM.dto.SnapshotResults;
import moa.core.PPSDM.dto.SummaryResults;
import moa.learners.PersonalizedPatternsMiner;
import moa.tasks.GridSearch;
import moa.tasks.GridSearchLearnEvaluateTask;

/**
 *
 * @author Tomas
 */
public class OutputManager {

    private FileWriter writer;
    private String pathToSummaryOutputFile;
    
    public OutputManager(String pathToSummaryOutputFile) {
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
    }

    public OutputManager() {
        
    }
    
    public static void writeHeader(String path) {
        try {
            try (FileWriter writer = new FileWriter(path, true)) {
                writer.append("FILE ID");writer.append(',');
                writer.append("USE GROUPING");writer.append(',');
                // RECOMMEND PARAMETERS
                writer.append("REC:RECOMMEND STRATEGY");writer.append(',');
                writer.append("GEN:MAXIMAL DIFFERENCE OF CLUSTERING IDs");writer.append(',');
                writer.append("REC:EVALUATION WINDOW SIZE");writer.append(',');
                writer.append("REC:NUM OF RECOMMENDED ITEMS");writer.append(',');
                // INCMINE PARAMETERS
                writer.append("FPM:MIN SUPPORT");writer.append(',');
                writer.append("FPM:RELAXATION RATE");writer.append(','); 
                writer.append("FPM:FIXED SEGMENT LENGTH");writer.append(',');
                writer.append("FPM:GROUP FIXED SEGMENT LENGTH");writer.append(',');
                writer.append("FPM:MAX ITEMSET LENGTH");writer.append(',');
                writer.append("FPM:WINDOW SIZE");writer.append(',');
                // UNIVERSAL PARAMETERS - RESTRICTIONS
                writer.append("RES:NUM OF DIMENSTIONS IN USER MODEL");writer.append(',');
                writer.append("RES:MAX FCI SET COUNT");writer.append(',');
                writer.append("RES:MIN TRANSACTIONS PER SECOND");writer.append(',');
                writer.append("RES:MAX UPDATE TIME");writer.append(',');
                writer.append("RES:START EVALUATING FROM TID");writer.append(',');
                // CLUSTERING PARAMETERS
                writer.append("CLU:MIN NUM OF CHANGES IN USER MODEL");writer.append(',');
                writer.append("CLU:MIN NUM OF CHANGES IN MICROCLUSTERS");writer.append(',');
                writer.append("CLU:NUM OF GROUPS");writer.append(',');
                writer.append("CLU:NUM OF MICROKERNELS");writer.append(',');
                writer.append("CLU:KERNEL RADI FACTOR");writer.append(',');
                // RESULTS 
                writer.append("GGC:ALL HITS");writer.append(',');
                writer.append("GGC:REAL RECOMMENDED");writer.append(',');
                writer.append("GGC:PRECISION");writer.append(',');
                writer.append("GGC:RECALL");writer.append(',');
                writer.append("GGC:F1");writer.append(',');
                writer.append("GGC:NDCG");writer.append(',');
                
                writer.append("GO:ALL HITS");writer.append(',');
                writer.append("GO:REAL RECOMMENDED ITEMS");writer.append(',');
                writer.append("GO:PRECISION");writer.append(',');
                writer.append("GO:RECALL");writer.append(',');
                writer.append("GO:F1");writer.append(',');
                writer.append("GO:NDCG");writer.append(',');
                
                writer.append("OG: ALL HITS");writer.append(',');
                writer.append("OG: REAL RECOMMENDED ITEMS");writer.append(',');
                writer.append("OG:PRECISION");writer.append(',');
                writer.append("OG:RECALL");writer.append(',');
                writer.append("OG:F1");writer.append(',');
                writer.append("OG:NDCG");writer.append(',');
                
                writer.append("ALL TESTED ITEMS");writer.append(',');
                writer.append("ALL TESTED TRANSACTIONS");writer.append(',');
                writer.append("MAX RECOMMENDED ITEMS");writer.append(',');
                writer.append("DURATION IN SECONDS");writer.append(',');
                writer.append("TRANSACTIONS PER SECOND");writer.append(',');
                writer.append("NUM ANALYZED TRANSACTIONS");writer.append(',');
                writer.append('\n');
                writer.close();
                
            }
        } catch (IOException ex) {
            Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void extractPatternsToFile(SnapshotResults snap, 
            String pathToFilePatterns, String pathToFileSnapshotStats) {
        // CREATING SNAPSHOT OF RESULTS
        try {
            FileWriter pwriter = new FileWriter(pathToFilePatterns, true);
            FileWriter snapwriter = new FileWriter(pathToFileSnapshotStats, true);
            if(snap.getId() == 0){
                pwriter.append("ID");pwriter.append(',');
                pwriter.append("TRANSACTION");pwriter.append(',');
                pwriter.append("GROUPID");pwriter.append(',');
                pwriter.append("SUPPORT");pwriter.append(',');
                pwriter.append("ITEMS");pwriter.append(',');
                pwriter.append('\n');
            }
            for(GroupStatsResults gsr : snap.getGroupStats()){
                for(FciValue fci : gsr.getFcis()){
                    pwriter.append(((Integer)snap.getId()).toString());pwriter.append(',');
                    pwriter.append(((Integer)Configuration.TRANSACTION_COUNTER).toString());pwriter.append(','); 
                    pwriter.append(((Integer)fci.getGroupid()).toString());pwriter.append(','); 
                    pwriter.append(((Double)fci.getSupport()).toString());pwriter.append(','); 
                    pwriter.append((fci.getItems()).toString().replaceAll(","," "));pwriter.append(','); 
                    pwriter.append('\n');
                }
            }
            
            if(snap.getId() == 0){
                snapwriter.append("ID");snapwriter.append(',');
                snapwriter.append("TRANSACTION");snapwriter.append(',');
                snapwriter.append("GROUPID");snapwriter.append(',');
                snapwriter.append("CLUSTERING ID");snapwriter.append(',');
                snapwriter.append("NUMBER OF GROUPS");snapwriter.append(',');
                snapwriter.append("NUMBER OF USERS IN GROUP");snapwriter.append(',');
                snapwriter.append("NUMBER OF ALL PATTERNS IN GROUP");snapwriter.append(',');
                snapwriter.append("NUMBER OF UNIQUE PATTERNS IN GROUP");snapwriter.append(',');
                snapwriter.append("AVERAGE SUPPORT OF UNIQUE PATTERNS IN GROUP");snapwriter.append(',');
                snapwriter.append("AVERAGE SUPPORT OF ALL PATTERNS IN GROUP");snapwriter.append(',');
                snapwriter.append("ACCURACY GGC");snapwriter.append(',');
                snapwriter.append("ACCURACY GO");snapwriter.append(',');
                snapwriter.append("ACCURACY OG");snapwriter.append(',');
                snapwriter.append("PRECISION GGC");snapwriter.append(',');
                snapwriter.append("PRECISION GO");snapwriter.append(',');
                snapwriter.append("PRECISION OG");snapwriter.append(',');
                snapwriter.append("CENTER");snapwriter.append(',');
                snapwriter.append("UIDS");
                snapwriter.append('\n');
            }
            
            for(GroupStatsResults gsr : snap.getGroupStats()){
                snapwriter.append(((Integer)snap.getId()).toString());snapwriter.append(','); 
                snapwriter.append(((Integer)Configuration.TRANSACTION_COUNTER).toString());snapwriter.append(','); 
                snapwriter.append((gsr.getGroupid()).toString());snapwriter.append(','); 
                snapwriter.append(((Integer)gsr.getClusteringId()).toString());snapwriter.append(','); 
                snapwriter.append(((Integer)(snap.getGroupStats().size() - 1)).toString());snapwriter.append(','); 
                snapwriter.append(((gsr.getNumberOfUsers())).toString());snapwriter.append(','); 
                snapwriter.append((((Integer)gsr.getFcis().size())).toString());snapwriter.append(','); 
                snapwriter.append(((gsr.getNumOfUniquePatterns())).toString());snapwriter.append(','); 
                snapwriter.append(((gsr.getAverageSupportOfUniquePatterns())).toString());snapwriter.append(',');
                snapwriter.append(((gsr.getAverageSupport())).toString());snapwriter.append(',');
                
                if(gsr.getAverageAccuracyForGroupGGC() != null){
                    for(double r:gsr.getAverageAccuracyForGroupGGC()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getAverageAccuracyForGroupGO() != null){
                    for(double r:gsr.getAverageAccuracyForGroupGO()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getAverageAccuracyForGroupOG() != null){
                    for(double r:gsr.getAverageAccuracyForGroupOG()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getAveragePrecisionForGroupGGC() != null){
                    for(double r:gsr.getAveragePrecisionForGroupGGC()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getAveragePrecisionForGroupGO() != null){
                    for(double r:gsr.getAveragePrecisionForGroupGO()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getAveragePrecisionForGroupOG() != null){
                    for(double r:gsr.getAveragePrecisionForGroupOG()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getCentre() != null){
                    for(double r:gsr.getCentre()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }snapwriter.append(',');
                if(gsr.getUids() != null){
                    for(double r:gsr.getUids()){
                        snapwriter.append(((Double)r).toString());snapwriter.append(':');
                    }
                }
                snapwriter.append('\n');
            }
            pwriter.close();
            snapwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchLearnEvaluateTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void extractSpeedResultsToFile(double[] speedResults, 
            String pathToFileSpeedResults) {
        // CREATING SNAPSHOT OF RESULTS
        try {
            FileWriter swriter = new FileWriter(pathToFileSpeedResults, true);
            swriter.append(((Double)speedResults[0]).toString());
            swriter.append(',');
            swriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchLearnEvaluateTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeConfigurationToFile(int id, String path, PersonalizedPatternsMiner learner){
        try {
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            writer.append(((Integer)id).toString());writer.append(',');
            writer.append(((Boolean)learner.useGrouping).toString());writer.append(',');
            // RECOMMEND PARAMETERS
            writer.append(Configuration.RECOMMEND_STRATEGY.toString());writer.append(',');
            writer.append(((Integer)Configuration.MAX_DIFFERENCE_OF_CLUSTERING_ID).toString());writer.append(',');
            writer.append((learner.evaluationWindowSize).toString());writer.append(',');
            writer.append((learner.numberOfRecommendedItems).toString());writer.append(',');
            // INCMINE PARAMETERS
            writer.append((learner.minSupport).toString());writer.append(',');
            writer.append((learner.relaxationRate).toString());writer.append(',');
            writer.append((learner.fixedSegmentLength).toString());writer.append(',');
            writer.append((learner.groupFixedSegmentLength).toString());writer.append(',');
            writer.append((learner.maxItemsetLength).toString());writer.append(',');
            writer.append((learner.windowSize).toString());writer.append(',');
            
            //UNIVERSAL PARAMETERS
            writer.append((learner.numOfDimensionsInUserModel).toString());writer.append(',');
            writer.append(((Double)Configuration.MAX_FCI_SET_COUNT).toString());writer.append(',');
            writer.append(((Double)Configuration.MIN_TRANSSEC).toString());writer.append(',');
            writer.append(((Double)Configuration.MAX_UPDATE_TIME).toString());writer.append(',');
            writer.append(((Integer)Configuration.START_EVALUATING_FROM).toString());writer.append(',');
            // CLUSTERING PARAMETERS
            if(learner.useGrouping){
                writer.append(( learner.numMinNumberOfChangesInUserModel).toString());writer.append(',');
                writer.append(( learner.numMinNumberOfMicroclustersUpdates).toString());writer.append(',');
                writer.append(( learner.numberOfGroups).toString());writer.append(',');
                writer.append(( learner.maxNumKernels).toString());writer.append(',');
                writer.append(( learner.kernelRadiFactor).toString());writer.append(',');
            }else{
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
            }
            writer.close();
            writer = null;
         } catch (IOException ex) {
             Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
         }
    }

    public void writeResultsToFile(SummaryResults results, double transsec, double tp, int counter){
        try{
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            
            writer.append(toInternalDataString(results.getAllHitsGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getRealRecommendedGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getPrecisionGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getRecallGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getF1GGC()));writer.append(',');
            writer.append(toInternalDataString(results.getNdcgGGC()));writer.append(',');
            
            writer.append(toInternalDataString(results.getAllHitsGO()));writer.append(',');
            writer.append(toInternalDataString(results.getRealRecommendedGO()));writer.append(',');
            writer.append(toInternalDataString(results.getPrecisionGO()));writer.append(',');
            writer.append(toInternalDataString(results.getRecallGO()));writer.append(',');
            writer.append(toInternalDataString(results.getF1GO()));writer.append(',');
            writer.append(toInternalDataString(results.getNdcgGO()));writer.append(',');
            
            writer.append(toInternalDataString(results.getAllHitsOG()));writer.append(',');
            writer.append(toInternalDataString(results.getRealRecommendedOG()));writer.append(',');
            writer.append(toInternalDataString(results.getPrecisionOG()));writer.append(',');
            writer.append(toInternalDataString(results.getRecallOG()));writer.append(',');
            writer.append(toInternalDataString(results.getF1OG()));writer.append(',');
            writer.append(toInternalDataString(results.getNdcgOG()));writer.append(',');
            
            writer.append(toInternalDataString(results.getAllTestedItems()));writer.append(',');
            writer.append(toInternalDataString(results.getAllTestedTransactions()));writer.append(',');
            writer.append(toInternalDataString(results.getMaxRecommendedItems()));writer.append(',');
            writer.append(((Double)tp).toString());writer.append(',');
            writer.append(((Double)transsec).toString());writer.append(',');
            writer.append(((Integer)counter).toString());writer.append('\n');
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeSpeedResultsToFile(double[] speed){
        try{
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            writer.append(((Double)speed[0]).toString());writer.append(',');
            writer.append(((Double)speed[1]).toString());writer.append(',');
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String toInternalDataString(double[] list) {
        StringBuilder strBuild = new StringBuilder();
        for(int i = 0; i < list.length; i++){
            strBuild.append(list[i]);
            if(i < list.length - 1){
               strBuild.append(":"); 
            }
        }
        return strBuild.toString();
    }
    
    private String toInternalDataString(int[] list) {
        StringBuilder strBuild = new StringBuilder();
        for(int i = 0; i < list.length; i++){
            strBuild.append(list[i]);
            if(i < list.length - 1){
               strBuild.append(":"); 
            }
        }
        return strBuild.toString();
    }
    
}
