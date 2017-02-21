/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Tomas
 */
public interface BehavioralPattern extends Comparable{
    
    public double computeSimilarityTo(List<Integer> window);
    
    public int getSize();

    public List<Integer> getConsequenceOf(List<Integer> window);

    public Double getWeight();

    public void setWeightFactor(double distance);
    
    public double getWeightFactor();

    public List<Integer> getItems();
    
}
