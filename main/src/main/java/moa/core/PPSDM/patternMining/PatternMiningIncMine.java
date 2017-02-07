/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.patternMining;

import moa.learners.PersonalizedIncMine;

/**
 *
 * @author Tomas
 */
public class PatternMiningIncMine implements PatternMiningComponent{

    private PersonalizedIncMine incMine;
    
    public PatternMiningIncMine(Integer windowSize, Integer maxItemsetLength, Integer numberOfGroups, 
            Double minSupport, Double relaxationRate, Integer fixedSegmentLength,
            Integer groupFixedSegmentLength) {
        
        this.incMine = new PersonalizedIncMine(windowSize,maxItemsetLength,
                            numberOfGroups, minSupport, relaxationRate, 
                            fixedSegmentLength, groupFixedSegmentLength);
         
        this.incMine.resetLearning();
        
    }

   
    
}
