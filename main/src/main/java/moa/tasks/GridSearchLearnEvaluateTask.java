/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import moa.core.PPSDM.dto.Parameter;
import moa.core.PPSDM.utils.OutputManager;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import moa.core.PPSDM.enums.SortStrategiesEnum;
import moa.core.PPSDM.enums.RecommendStrategiesEnum;
import moa.core.PPSDM.Configuration;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.PPSDMRecommendationEvaluator;
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
public class GridSearchLearnEvaluateTask implements Task {
    
    private int id;
    private int fromid;
    private Map<String,Parameter> params;
    private SessionsFileStream stream ;
    private String pathToStream ;
    private String pathToSummaryOutputFile;
    private String pathToOutputFile;
    private String pathToCategoryMappingFile;
    private OutputManager om;
    
    
    public GridSearchLearnEvaluateTask(int id, int fromid, Map<String,Parameter> params,
            String pathToStream, String pathToSummaryOutputFile, 
            String pathToOutputFile, String pathToCategoryMappingFile) {
        this.id = id;
        this.fromid = fromid;
        this.params = params;
        this.pathToStream = pathToStream;
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
        this.pathToOutputFile = pathToOutputFile;
        this.pathToCategoryMappingFile = pathToCategoryMappingFile;
        this.om = new OutputManager(pathToSummaryOutputFile);
    }

    @Override
    public Object doTask() {
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
                Logger.getLogger(GridSearchLearnEvaluateTask.class.getName()).log(Level.SEVERE, null, ex);
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
        om.writeConfigurationToFile(id, this.pathToSummaryOutputFile, learner);
        learner.resetLearning();
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
        PPSDMRecommendationEvaluator evaluator = 
                new PPSDMRecommendationEvaluator(
                        this.pathToOutputFile + "results_G" + learner.getUseGrouping() + 
                                                "_id_" + id + ".csv");
        Configuration.TRANSACTION_COUNTER = 0;
        Configuration.STREAM_START_TIME = TimingUtils.getNanoCPUTimeOfCurrentThread();
        
        // STREAM PROCESSING
        while (stream.hasMoreInstances()) {
            Configuration.TRANSACTION_COUNTER++;
            // NOW EXTRACT PATTERNS TO FILE
            double[] speedResults = UtilitiesPPSDM.getActualTransSec();
            if(!Configuration.EVALUATE_SPEED){
                if(Configuration.EXTRACT_DETAILS){
                    SnapshotResults snap = learner.extractSnapshot(evaluator);
                    if(snap != null){
                        om.extractPatternsToFile(snap, 
                            this.pathToOutputFile + "patterns_" + id + ".csv",
                            this.pathToOutputFile + "snapshots_stats_" + id + ".csv");
                    }
                }
            }else{
                if(Configuration.TRANSACTION_COUNTER % Configuration.EXTRACT_SPEED_RESULTS_AT == 0){
                    om.extractSpeedResultsToFile(speedResults,this.pathToOutputFile + "speed_" + id + ".csv");
                }
            }
            Example trainInst = stream.nextInstance();
            if(!Configuration.EVALUATE_SPEED){
                Example testInst = (Example) trainInst.copy();
                if(Configuration.TRANSACTION_COUNTER > Configuration.START_EVALUATING_FROM){
                    RecommendationResults results = learner.getRecommendationsForInstance(testInst);
                    if(results != null)
                        // evaluator will evaluate recommendations and update metrics with given results     
                        evaluator.addResult(results, learner.windowSize, 
                                speedResults[0], Configuration.TRANSACTION_COUNTER); 
                }
            }
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
        }
        double[] speedResults = UtilitiesPPSDM.getActualTransSec();
        if(!Configuration.EVALUATE_SPEED){
            SummaryResults results = evaluator.getResults();
            om.writeResultsToFile(results, speedResults[0], speedResults[1], 
                Configuration.TRANSACTION_COUNTER);
        }else{
            om.writeSpeedResultsToFile(speedResults);
        }
        System.gc();
        return null;
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
    
    public int getId() {
        return id;
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
     
    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
