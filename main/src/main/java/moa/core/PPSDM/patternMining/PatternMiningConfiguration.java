/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.patternMining;

/**
 *
 * @author Tomas
 */
public interface PatternMiningConfiguration {
    
    public void setTUC(int tuc);
    public int getTUC();
    public void setTCM(int tcm);
    public int getTCM();
    public void setUserModelDimensions(int tcm);
    public int getUserModelDimensions();
        public void setGrouping(boolean grouping);
    public boolean getGrouping();
    
}
