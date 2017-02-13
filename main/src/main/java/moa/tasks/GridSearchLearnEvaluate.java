/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import moa.core.dto.Parameter;
import moa.core.utils.OutputManager;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import moa.core.enums.RecommendStrategiesEnum;
import moa.core.Configuration;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.learners.recommendation.RecommendationGenerator;
import moa.learners.clustering.ClustererClustream;
import moa.learners.clustering.ClusteringComponent;
import moa.core.TimingUtils;
import moa.evaluation.RecommendationEvaluator;
import moa.learners.patternMining.PersonalizedPatternsMiner;
import moa.streams.SessionsFileStream;
import moa.core.dto.RecommendationResults;
import moa.core.dto.SummaryResults;
import moa.core.utils.UtilitiesPPSDM;
import moa.core.dto.SnapshotResults;
import moa.learners.patternMining.PatternMiningComponent;
import moa.learners.patternMining.PatternMiningIncMine;

/**
 * Task to evaluate one from configurations in grid during grid search. 
 * @author Tomas Chovanak
 */
public class GridSearchLearnEvaluate implements Task {
    
    private int id;  // id of configuration
    private int fromid; // id to start from (if id < fromid) configuration is ignored
    private Map<String,Parameter> params;
    private SessionsFileStream stream;
    private String pathToStream ;
    private String pathToSummaryOutputFile;
    private String pathToOutputFile;
    private String pathToCategoryMappingFile;
    private OutputManager om;
    private Configuration config;
    
    public GridSearchLearnEvaluate(int id, int fromid, Map<String,Parameter> params,
            String pathToStream, String pathToSummaryOutputFile, 
            String pathToOutputFile, String pathToCategoryMappingFile) {
        this.id = id;
        this.fromid = fromid;
        this.params = params;
        this.pathToStream = pathToStream;
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
        this.pathToOutputFile = pathToOutputFile;
        this.pathToCategoryMappingFile = pathToCategoryMappingFile;
        this.config = new Configuration();
        this.om = new OutputManager(pathToSummaryOutputFile);
    }

    
    @Override
    public Object doTask() {
        id++; // id is always incremented
        // fromid is used to allow user restart evaluation from different point anytime
        if(fromid >= id){
            return null;
        }
        boolean valid = configureParams(params);
        // CHECK IF segment legnth and support is valid:
        if(!valid){ return null; }
        // initialize and configure learner
        PersonalizedPatternsMiner learner;
        // USE INCMINE ,  CLUSTREAM and Recommendation
        PatternMiningComponent patternMining = new PatternMiningIncMine(config);
        ClusteringComponent clusteringComponent = new ClustererClustream(config,config);
        RecommendationGenerator recGenerator = new RecommendationGenerator(config, 
                patternMining, clusteringComponent);
        if(this.pathToCategoryMappingFile != null && !this.pathToCategoryMappingFile.equals("")){
            Map<Integer,Integer> map;
            try {
                map = readCategoriesMap();
                learner = new PersonalizedPatternsMiner(map, config,patternMining,
                        clusteringComponent, recGenerator);
            } catch (IOException ex) {
                learner = new PersonalizedPatternsMiner(config,patternMining,
                        clusteringComponent, recGenerator);
                Logger.getLogger(GridSearchLearnEvaluate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
             learner = new PersonalizedPatternsMiner(config,patternMining,
                        clusteringComponent, recGenerator);
        }
        
        
        // initialize input stream
        this.stream = new SessionsFileStream(this.pathToStream);
        
        om.writeConfigurationToFile(id, this.pathToSummaryOutputFile, config);
        
        learner.resetLearning();
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
                
        RecommendationEvaluator evaluator = 
                new RecommendationEvaluator(config);
        
        config.setTransactionCounter(0);
        config.setStreamStartTime(TimingUtils.getNanoCPUTimeOfCurrentThread());
        
        // STREAM PROCESSING
        while (stream.hasMoreInstances()) {
            config.incrementTransactionCounter();
            // NOW EXTRACT PATTERNS TO FILE
            double[] speedResults = UtilitiesPPSDM.getActualTransSec(
                                                    config.getTransactionCounter(),
                                                    config.getStreamStartTime());
            if(!config.getEvaluateSpeed()){
                if(config.getExtractDetails()){
                    SnapshotResults snap = learner.extractSnapshot(evaluator);
                    if(snap != null){
                        om.extractPatternsToFile(snap, config,
                            this.pathToOutputFile + "patterns_" + id + ".csv",
                            this.pathToOutputFile + "snapshots_stats_" + id + ".csv");
                    }
                }
            }else{
                if(config.getTransactionCounter() % config.getExtractSpeedResultsAt() == 0){
                    om.extractSpeedResultsToFile(speedResults,this.pathToOutputFile + "speed_" + id + ".csv");
                }
            }
            Example trainInst = stream.nextInstance();
            if(!config.getEvaluateSpeed()){
                Example testInst = (Example) trainInst.copy();
                if(config.getTransactionCounter() > config.getStartEvaluatingFrom()){
                    RecommendationResults results = learner.getRecommendationsForInstance(testInst);
                    if(results != null)
                        // evaluator will evaluate recommendations and update metrics with given results     
                        evaluator.addResult(results, config.getWS(), 
                                speedResults[0], config.getTransactionCounter()); 
                }
            }
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
        }
        double[] speedResults = UtilitiesPPSDM.getActualTransSec(
                                                    config.getTransactionCounter(),
                                                    config.getStreamStartTime());
        if(!config.getEvaluateSpeed()){
            SummaryResults results = evaluator.getResults();
            om.writeResultsToFile(results, speedResults[0], speedResults[1], 
                config.getTransactionCounter());
        }else{
            om.writeSpeedResultsToFile(speedResults);
        }
        System.gc();
        return null;
    }
    
    
    private boolean configureParams(Map<String,Parameter> params){
        config.setOutputFile(this.pathToOutputFile + "results_G" + config.getGrouping() + "_id_" + id + ".csv");
        // RECOMMEND PARAMETERS
        config.setGrouping((int)params.get("GROUPING").getValue() == 1);
        config.setRecommendationStrategy( 
                RecommendStrategiesEnum.valueOf((int) params.get("RECOMMEND STRATEGY").getValue()));
        
        config.setTcdiff((int) params.get("MAXIMAL DIFFERENCE OF CLUSTERING IDS").getValue());
        config.setEWS((int) params.get("EVALUATION WINDOW SIZE").getValue());
        config.setRC((int) params.get("NUMBER OF RECOMMENDED ITEMS").getValue());
        //FPM PARAMETERS
        config.setMS(params.get("MIN SUPPORT").getValue());
        config.setRR(params.get("RELAXATION RATE").getValue());
        config.setFSL((int) (params.get("FIXED SEGMENT LENGTH").getValue()));
        
        // CHECK IF MIN SUPPORT AND SEGMENT LENGTH ARE VALID
        if(config.getMS() * config.getFSL() <= 1){
            return false;
        }
        config.setMIL((int) params.get("MAX ITEMSET LENGTH").getValue());
        config.setWS((int) params.get("WINDOW SIZE").getValue());
        
        // RESTRICTIONS PARAMETERS
        config.setUserModelDimensions((int) params.get("NUMBER OF DIMENSIONS IN USER MODEL").getValue());
        config.setMTS((int)Math.round(params.get("MIN TRANSSEC").getValue()));
        
        // CLUSTERING PARAMETERS
        config.setTUC((int) params.get("MINIMAL NUMBER OF CHANGES IN USERMODEL").getValue());
        config.setTCM((int) params.get("MINIMAL NUMBER OF CHANGES IN MICROCLUSTERS").getValue());
        config.setGC((int) params.get("NUMBER OF GROUPS").getValue());
        config.setMaxNumKernels((int) params.get("MAXIMAL NUMBER OF MICROCLUSTERS").getValue());
        config.setKernelRadiFactor((int) params.get("KERNEL RADI FACTOR").getValue());
        config.setStartEvaluatingFrom((int) params.get("START EVALUATING FROM").getValue());
        
        double gfsl = params.get("GROUP FIXED SEGMENT LENGTH").getValue();
        if(gfsl > 0){
            config.setGFSL((int) params.get("GROUP FIXED SEGMENT LENGTH").getValue());
        }else if(gfsl == 0){
            config.setGFSL((int)((config.getFSL()/
                        (config.getGC()))));
        }else{
            config.setGFSL(config.getFSL());
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
