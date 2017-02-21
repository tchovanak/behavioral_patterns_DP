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
public interface PersonalizedEstDecPlusConfiguration {
    
    public int getGC();
    public double getMS();
    
    public void setD(double d);
    public double getD();
    
    public void setMinSigValue(double v);
    public double getMinSigValue();
    
    public void setDeltaValue(double v);
    public double getDeltaValue();
    
    public void setMinMergeValue(double v);
    public double getMinMergeValue();
    
    public long getStreamStartTime();
    
    
}
