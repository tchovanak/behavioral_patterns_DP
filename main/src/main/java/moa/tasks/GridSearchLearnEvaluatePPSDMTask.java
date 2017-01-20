/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import moa.core.PPSDM.enums.SortStrategiesEnum;
import moa.core.PPSDM.enums.RecommendStrategiesEnum;
import moa.core.PPSDM.Configuration;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.PPSDMRecommendationEvaluator;
import moa.core.PPSDM.FciValue;
import moa.core.PPSDM.dto.GroupStatsResults;
import moa.learners.PersonalizedPatternsMiner;
import moa.streams.SessionsFileStream;
import moa.core.PPSDM.dto.RecommendationResults;
import moa.core.PPSDM.dto.SummaryResults;
import moa.core.PPSDM.utils.UtilitiesPPSDM;
import moa.core.PPSDM.dto.SnapshotResults;

/**
 * Task to evaluate one from configurations in grid during grid search. 
 * @author Tomas Chovanak
 */
public class GridSearchLearnEvaluatePPSDMTask implements Task {
    
    private int id;
    private int fromid;
    private Map<String,Parameter> params;
    private boolean grouping;
    private FileWriter writer;
    private SessionsFileStream stream ;
    private String pathToStream ;
    private String pathToSummaryOutputFile;
    private String pathToOutputFile;
    private String pathToCategoryMappingFile;
    
    
    public GridSearchLearnEvaluatePPSDMTask(int id, int fromid, Map<String,Parameter> params,
            String pathToStream, String pathToSummaryOutputFile, 
            String pathToOutputFile, String pathToCategoryMappingFile,
            boolean grouping) {
        this.id = id;
        this.fromid = fromid;
        this.params = params;
        this.grouping = grouping;
        this.pathToStream = pathToStream;
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
        this.pathToOutputFile = pathToOutputFile;
        this.pathToCategoryMappingFile = pathToCategoryMappingFile;
    }
    
    private Map<Integer,Integer> readCategoriesMap() throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(this.pathToCategoryMappingFile));
        Map<Integer,Integer> map = new HashMap<>();
        String line = "";
        while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] words = line.split(",");
                map.put(Integer.parseInt(words[1].trim()), Integer.parseInt(words[0].trim()));
        }
        return map;
    }

    @Override
    public Object doTask() {
        System.out.println(Configuration.EXTRACT_DETAILS);
        id++; // id is always incremented
        if(fromid >= id){
            return null;
        }
        // initialize and configure learner
        PersonalizedPatternsMiner learner;
        if(this.pathToCategoryMappingFile != null){
            Map<Integer,Integer> map;
            try {
                map = readCategoriesMap();
                learner = new PersonalizedPatternsMiner(map);
            } catch (IOException ex) {
                learner = new PersonalizedPatternsMiner();
                Logger.getLogger(GridSearchLearnEvaluatePPSDMTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
             learner = new PersonalizedPatternsMiner();
        }
        boolean valid = configureLearnerWithParams(learner, params);
        // CHECK IF segment legnth and support is valid:
        if(!valid){
            return null;
        }
        // fromid is used to allow user restart evaluation from different point anytime
        this.stream = new SessionsFileStream(this.pathToStream);
        writeConfigurationToFile(this.pathToSummaryOutputFile, learner);
        learner.useGrouping = grouping; // change grouping option in learner
        learner.resetLearning();
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
        PPSDMRecommendationEvaluator evaluator = 
                new PPSDMRecommendationEvaluator(
                        this.pathToOutputFile + "results_G" + grouping + 
                                "_id_" + id + ".csv");
        Configuration.TRANSACTION_COUNTER = 0;
        Configuration.STREAM_START_TIME = TimingUtils.getNanoCPUTimeOfCurrentThread();
        int windowSize = learner.evaluationWindowSize;
        // STREAM PROCESSING
        while (stream.hasMoreInstances()) {
            Configuration.TRANSACTION_COUNTER++;
            // NOW EXTRACT PATTERNS TO FILE
            double[] speedResults = UtilitiesPPSDM.getActualTransSec();
            if(!Configuration.EVALUATE_SPEED){
                if(Configuration.EXTRACT_DETAILS){
                    SnapshotResults snap = learner.extractSnapshot(evaluator);
                    if(snap != null){
                        extractPatternsToFile(snap, 
                            this.pathToOutputFile + "patterns_" + id + ".csv",
                            this.pathToOutputFile + "snapshots_stats_" + id + ".csv");
                    }
                }
            }else{
                if(Configuration.TRANSACTION_COUNTER % Configuration.EXTRACT_SPEED_RESULTS_AT == 0){
                    extractSpeedResultsToFile(speedResults,this.pathToOutputFile + "speed_" + id + ".csv");
                }
            }
            Example trainInst = stream.nextInstance();
            if(!Configuration.EVALUATE_SPEED){
                Example testInst = (Example) trainInst.copy();
                if(Configuration.TRANSACTION_COUNTER > Configuration.START_EVALUATING_FROM){
                    RecommendationResults results = learner.getRecommendationsForInstance(testInst);
                    if(results != null)
                        // evaluator will evaluate recommendations and update metrics with given results     
                        evaluator.addResult(results, windowSize, speedResults[0], Configuration.TRANSACTION_COUNTER); 
                }
            }
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
        }
        double[] speedResults = UtilitiesPPSDM.getActualTransSec();
        if(!Configuration.EVALUATE_SPEED){
            SummaryResults results = evaluator.getResults();
            writeResultsToFile(results, speedResults[0], speedResults[1], 
                Configuration.TRANSACTION_COUNTER);
        }else{
            writeSpeedResultsToFile(speedResults);
        }
        System.gc();
        return null;
    }
    
    private void extractPatternsToFile(SnapshotResults snap, 
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
            Logger.getLogger(GridSearchLearnEvaluatePPSDMTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private void extractSpeedResultsToFile(double[] speedResults, 
            String pathToFileSpeedResults) {
        // CREATING SNAPSHOT OF RESULTS
        try {
            FileWriter swriter = new FileWriter(pathToFileSpeedResults, true);
            swriter.append(((Double)speedResults[0]).toString());
            swriter.append(',');
            swriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchLearnEvaluatePPSDMTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean configureLearnerWithParams(PersonalizedPatternsMiner learner, 
            Map<String,Parameter> params){
        // RECOMMEND PARAMETERS
        Configuration.GROUPING = ((int)params.get("GROUPING").getValue() == 1);
        learner.useGrouping = Configuration.GROUPING;
        Configuration.RECOMMEND_STRATEGY = 
                RecommendStrategiesEnum.valueOf((int) params.get("RECOMMEND STRATEGY").getValue());
        Configuration.SORT_STRATEGY = 
                SortStrategiesEnum.valueOf((int) params.get("SORT STRATEGY").getValue());
        
        Configuration.MAX_DIFFERENCE_OF_CLUSTERING_ID = 
                (int) params.get("MAXIMAL DIFFERENCE OF CLUSTERING IDS").getValue();
        
        learner.evaluationWindowSize = (int) params.get("EVALUATION WINDOW SIZE").getValue();
        learner.numberOfRecommendedItems = (int) params.get("NUMBER OF RECOMMENDED ITEMS").getValue();
        
        //FPM PARAMETERS
        learner.minSupport = params.get("MIN SUPPORT").getValue();
        learner.relaxationRate = params.get("RELAXATION RATE").getValue();
        learner.fixedSegmentLength = (int) (params.get("FIXED SEGMENT LENGTH").getValue());
        // CHECK IF MIN SUPPORT AND SEGMENT LENGTH ARE VALID
        if(learner.minSupport * learner.fixedSegmentLength <= 1){
            return false;
        }
        learner.maxItemsetLength = (int) params.get("MAX ITEMSET LENGTH").getValue();
        learner.windowSize = (int) params.get("WINDOW SIZE").getValue();
        // RESTRICTIONS PARAMETERS
        learner.numOfDimensionsInUserModel = (int) params.get("NUMBER OF DIMENSIONS IN USER MODEL").getValue();
        Configuration.MAX_FCI_SET_COUNT = params.get("MAX FCI SET COUNT").getValue();
        Configuration.MIN_TRANSSEC = params.get("MIN TRANSSEC").getValue();
        // CLUSTERING PARAMETERS
        learner.numMinNumberOfChangesInUserModel = (int) params.get("MINIMAL NUMBER OF CHANGES IN USERMODEL").getValue();
        learner.numMinNumberOfMicroclustersUpdates = (int) params.get("MINIMAL NUMBER OF CHANGES IN MICROCLUSTERS").getValue();
        learner.numberOfGroups = (int) params.get("NUMBER OF GROUPS").getValue();
        learner.maxNumKernels = (int) params.get("MAXIMAL NUMBER OF MICROCLUSTERS").getValue();
        learner.kernelRadiFactor = (int) params.get("KERNEL RADI FACTOR").getValue();
        Configuration.START_EVALUATING_FROM = (int) params.get("START EVALUATING FROM").getValue();
        Configuration.MAX_UPDATE_TIME = 20000;
        double gfsl = params.get("GROUP FIXED SEGMENT LENGTH").getValue();
        if(gfsl > 0){
            learner.groupFixedSegmentLength = (int) params.get("GROUP FIXED SEGMENT LENGTH").getValue();
        }else if(gfsl == 0){
            learner.groupFixedSegmentLength = 
                (int)(((double)learner.fixedSegmentLength)/
                        ((double)learner.numberOfGroups));
        }else{
            learner.groupFixedSegmentLength = learner.fixedSegmentLength;
        }
        return true;
        
    }
    
    private void writeConfigurationToFile(String path, PersonalizedPatternsMiner learner){
        try {
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            writer.append(((Integer)id).toString());writer.append(',');
            writer.append(((Boolean)this.grouping).toString());writer.append(',');
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
            if(this.grouping){
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
             Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
         }
    }

    private void writeResultsToFile(SummaryResults results, double transsec, double tp, int counter){
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
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeSpeedResultsToFile(double[] speed){
        try{
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            writer.append(((Double)speed[0]).toString());writer.append(',');
            writer.append(((Double)speed[1]).toString());writer.append(',');
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int getId() {
        return id;
    }
    
    public boolean isGrouping() {
        return grouping;
    }

    @Override
    public Object doTask(TaskMonitor tm, ObjectRepository or) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int measureByteSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public MOAObject copy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
     
    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
