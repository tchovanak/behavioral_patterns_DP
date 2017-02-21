/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.patternMining;



/**
 *
 * @author Tomas
 */
public interface PersonalizedIncMineConfiguration {
    
    public void setWS(int ws);
    public void setMIL(int mil);
    public void setGC(int gc);
    public void setMS(double ms);
    public void setRR(double rr);
    public void setFSL(int fsl);
    public void setGFSL(int gfsl);
    public void setStartUpdateTime(long start);
  
    public int getWS();
    public int getMIL();
    public int getGC();
    public double getMS();
    public double getRR();
    public int getFSL();
    public int getGFSL();
    public long getStartUpdateTime();
    public void setStreamStartTime(long str);
    public long getStreamStartTime();
    public void setMTS(int mts);
    public int getMTS();
    public void setTransactionCounter(int count);
    public void incrementTransactionCounter();
    public int getTransactionCounter();
}
