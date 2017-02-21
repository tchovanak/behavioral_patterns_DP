/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import moa.core.dto.Parameter;
import moa.core.utils.OutputManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.ObjectRepository;
import com.github.javacliparser.StringOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs and evaluates all experiments from parameters configurations defined in 
 * config file.
 * @author Tomas Chovanak
 */
public class GridSearch extends MainTask {

    private int id = 0;
    // from config file all needed parameters are read
    public  StringOption pathToConfigFile = new StringOption("pathToConfigFile", 'o',
            "Path to file where detail of configuration is stored.", "./");
    
    // path to stream input file 
    private String pathToStream = null;
    private int fromid = 0; // id of configuration to start grid search from.
    private String pathToSummaryOutputFile = "";
    private String pathToOutputFile = "";
    private String pathToCategoryMappingFile = "";
    // SUBTASK that runs with different configurations of parameters.
    private GridSearchLearnEvaluate gpLearnEvaluateTask = null;
    // Manages output tasks, like writing to file.
    private OutputManager om;
    
    public GridSearch(int fromid) {
        this.fromid = fromid;
    }
    
    /**
     * To start grid search from MOA
     * @param tm
     * @param or
     * @return 
     */
    @Override
    protected Object doMainTask(TaskMonitor tm, ObjectRepository or) {
        InputStream fileStream;
        BufferedReader fileReader = null;
        try {
            fileStream = new FileInputStream(this.pathToConfigFile.getValue());
            fileReader = new BufferedReader(new InputStreamReader(fileStream));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(fileReader == null){
            return null;
        }
        startEvaluation(fileReader);
        return null;
    }
    
       
    /**
     * Starts grid search as main class
     * Arguments:
     *  1. config file: path to config file where parameters for grid search are declared
     * @param args 
     */
    public static void main(String args[]){
        InputStream fileStream;
        BufferedReader fileReader = null;
        try {
            if(args.length > 0){
                fileStream = new FileInputStream(args[0]);
            }else{
                fileStream = new FileInputStream("g:\\workspace_DP2\\results_grid\\config\\config_sacbee_estDec.csv");
            }
            fileReader = new BufferedReader(new InputStreamReader(fileStream));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(fileReader == null){
            return;
        }
        startEvaluation(fileReader);
    }
    
    /**
     * Starts evaluation of configurations described in config file
     * @param fileReader 
     */
    private static void startEvaluation(BufferedReader fileReader) {
        List<Parameter> params = new ArrayList<>();
        try {
            String inputSessionFile = fileReader.readLine().split(",")[1].trim();
            String outputToDirectory = fileReader.readLine().split(",")[1].trim();
            String pathToCategoryMappingFile = fileReader.readLine().split(",")[1].trim();
            int fromid = Integer.parseInt(fileReader.readLine().split(",")[1].trim());
            // READ IN PARAMETERS 
            for(String line = fileReader.readLine(); line != null; line = fileReader.readLine()) {
                String[] row = line.split(",");
                double value = Double.parseDouble(row[1].trim());
                // LIST OF POSSIBLE VALUES
                Parameter p;
                if(value == -1){
                    p = new Parameter(value);
                    for(int i = 2; i < row.length; i++){
                        p.addPossibleValue(Double.parseDouble(row[i].trim()));
                    }
                }else {
                    //boundaries
                    p = new Parameter(0.0, value,Double.parseDouble(row[2].trim()), 
                            Double.parseDouble(row[3].trim()));   
                }
                p.setName(row[0].trim());
                params.add(p);
            }
            OutputManager.writeHeader(outputToDirectory + "summary_results.csv");
            GridSearch evaluator = new GridSearch(fromid);
            evaluator.setPathToOutputFile(outputToDirectory);
            evaluator.setPathToInputFile(inputSessionFile);
            evaluator.setPathToCategoryMappingFile(pathToCategoryMappingFile);
            evaluator.setPathToSummaryOutputFile(outputToDirectory + "summary_results.csv");
            List<Parameter> preparedParams = new ArrayList<>();
            evaluator.startGridEvaluation(params, preparedParams);  
        } catch (IOException ex) {
            Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    /**
     * Recursively tries all possible configurations of parameters
     * @param params
     * @param preparedParameters 
     */
    private void startGridEvaluation(List<Parameter> params, List<Parameter> preparedParameters){
        if(params.isEmpty()){
            this.evaluate(preparedParameters);
        }else{
            List<Parameter> origParamsCopy = deepCopy(params);
            Parameter p = origParamsCopy.remove(0);
            if(p.getValue() > 0){
                origParamsCopy = deepCopy(params);
                Parameter p2 = origParamsCopy.remove(0);
                List<Parameter> copyParams = deepCopy(preparedParameters);
                copyParams.add(p2);
                this.startGridEvaluation(origParamsCopy, copyParams);
            }else{
                //double[] b = p.getBoundaries();
                Iterator<Double> it = p.getPossibleValues().iterator();
                //for(double i = b[0]; i <= b[1]; i+= b[2]){  
                while(it.hasNext()){  
                    origParamsCopy = deepCopy(params);
                    Parameter p2 = origParamsCopy.remove(0);
                    p2.setValue(it.next());
                    List<Parameter> copyParams = deepCopy(preparedParameters);
                    copyParams.add(p2);
                    this.startGridEvaluation(origParamsCopy, copyParams);
                }
            }
        }
    }
    
    /**
     * Starts evaluation of parameter configuration 
     * @param params 
     */
    private void evaluate(List<Parameter> params){
        Map<String, Parameter> paramsMap = new HashMap<>();
        for(Parameter p : params){
            paramsMap.put(p.getName(), p);
        }
        gpLearnEvaluateTask = new GridSearchLearnEvaluate(id, fromid, paramsMap, pathToStream, 
                pathToSummaryOutputFile, pathToOutputFile, pathToCategoryMappingFile); 
        gpLearnEvaluateTask.doTask(); 
        this.id = this.gpLearnEvaluateTask.getId();
        gpLearnEvaluateTask = null;
        System.gc();
    }
    
    // HELPER
    private List<Parameter> deepCopy(List<Parameter> orig){
        List<Parameter> copy = new ArrayList<>(); 
        Iterator<Parameter> iterator = orig.iterator(); 
        while(iterator.hasNext()){ 
            try { 
                copy.add(iterator.next().clone());
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(GridSearch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return copy;
    }
    
    // GETTERS AND SETTERS
    public void setPathToInputFile(String path) {
        this.pathToStream = path;
    }
    
    public void setPathToStream(String pathToStream) {
        this.pathToStream = pathToStream;
    }

    public void setPathToSummaryOutputFile(String pathToSummaryOutputFile) {
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
    }

    public void setPathToOutputFile(String pathToOutputFile) {
        this.pathToOutputFile = pathToOutputFile;
    }

    public String getPathToCategoryMappingFile() {
        return pathToCategoryMappingFile;
    }

    public void setPathToCategoryMappingFile(String pathToCategoryMappingFile) {
        this.pathToCategoryMappingFile = pathToCategoryMappingFile;
    }

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
