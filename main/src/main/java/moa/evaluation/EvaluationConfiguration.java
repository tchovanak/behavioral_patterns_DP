/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.evaluation;

/**
 *
 * @author Tomas
 */
public interface EvaluationConfiguration {
    
    public void setOutputFile(String str);
    
    public String getOutputFile();
    
    public void setExtractDetails(Boolean str);
    
    public Boolean getExtractDetails();
    
    public void setTransactionCounter(int count);
    
    public void incrementTransactionCounter();
    
    public int getTransactionCounter();
    
    public void setStreamStartTime(long str);
    
    public long getStreamStartTime();
    
    public int getExtractSpeedResultsAt();
    
    public void setExtractSpeedResultsAt(int ext);
    
     public int getExtractPatternsAt();
    
    public void setExtractPatternsAt(int ext);
    
    public int getStartEvaluatingFrom();
    
    public void setStartEvaluatingFrom(int ext);
    
    public void setMTS(int mts);
    public int getMTS();

    public void setEvaluateSpeed(boolean mts);
    public boolean getEvaluateSpeed();


}
