/*
 *    FrequentItemset.java
 *    Copyright (C) 2012 Universitat Politècnica de Catalunya
 *    @author Massimo Quadrana <max.square@gmail.com>
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moa.core.PPSDM.SemiFCI;
import moa.core.utils.UtilitiesPPSDM;

/*
    (Tomas Chovanak) Added support relative to segment length and k representation.
*/
public class FrequentItemset implements BehavioralPattern {
    
    protected List<Integer> items = new ArrayList<>();
    protected int support;
    protected double supportDouble;
    protected int size;
    private double weightFactor = 1;
    
    /***
     * Constructs a Frequent Itemset given a list of items and their support
     * @param items List of items
     * @param support Support value
     */
    public FrequentItemset(List items, int support, double supportDouble){
        this.items = items;
        this.support = support;
        this.size = this.items.size();
        this.supportDouble = supportDouble;
    }
    
    public FrequentItemset(int[] itemset, int support, double supportDouble) {
        for (int index = 0; index < itemset.length; index++){
            items.add(itemset[index]);
        }
        this.support = support;
        this.size = this.items.size();
        this.supportDouble = supportDouble;
    }
    
    public FrequentItemset(int[] itemset, double supportDouble) {
        for (int index = 0; index < itemset.length; index++){
            items.add(itemset[index]);
        }
        this.support = support;
        this.size = this.items.size();
        this.supportDouble = supportDouble;
    }

    public List<Integer> getItems() {
        return items;
    }

    public void setItems(List<Integer> items) {
        this.items = items;
    }

    public int getSupport() {
        return support;
    }

    public void setSupport(int support) {
        this.support = support;
    }

    @Override
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public double getSupportDouble() {
        return supportDouble;
    }
    
    private List<FrequentItemset> getDirectSubsets(){
        List<FrequentItemset> res = new ArrayList<FrequentItemset>();
        for(int r=0;r<items.size();r++){
            List<Integer> subset = new ArrayList<Integer>(this.items);
            subset.remove(r);
            res.add(new FrequentItemset(subset,this.support, this.supportDouble));
        }
        return res;
    }

    @Override
    public boolean equals(Object obj){
        return this == obj ||
                (obj instanceof FrequentItemset &&
                ((FrequentItemset)obj).items.equals(this.items));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.items != null ? this.items.hashCode() : 0);
        hash = 71 * hash + this.size;
        return hash;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(this.items.toString()).append(":").append(Integer.toString(this.support));
        return sb.toString();
    }
    
    /***
     * Extracts Frequent Itemsets from a set of Frequent Closed Itemsets
     * @param iter Iterator of the list of Frequent Closed Itemsets
     * @param minSupport Minimum support threshold
     * @param segmentLength Number of transactions per segment
     * @return The list of frequent itemsets
     */
    
    public static List<BehavioralPattern> getFCIset(Iterator<SemiFCI> iter, double minSupport, int segmentLength) {
        List<ArrayList<FrequentItemset>> levels = new ArrayList<>();
        int last_size = -1;
        
        //every closed frequent itemset is a frequent itemset
        while(iter.hasNext()){
            SemiFCI fci = iter.next();
            FrequentItemset fi = new FrequentItemset(fci.getItems(), 
                    fci.getApproximateSupport(fci.getKValue()),
                    (double)fci.getApproximateSupport(fci.getKValue())/(double)(segmentLength * (fci.getKValue() + 1))
            );
            
            if(fi.support >= Math.ceil(minSupport * segmentLength * (fci.getKValue() + 1))){
                if(last_size != fi.size){
                    levels.add(new ArrayList<FrequentItemset>());
                    last_size = fi.size;
                }
                levels.get(levels.size()-1).add(fi);
            }                               
        }
        List<BehavioralPattern> ret = new ArrayList<>();
        for(List<FrequentItemset> level:levels)
            ret.addAll(level);
        
        return ret;
    }
    
    
    public static List<BehavioralPattern> getFIset(Iterator<SemiFCI> iter, double minSupport, int segmentLength){
        
        List<ArrayList<FrequentItemset>> levels = new ArrayList<>();
        int last_size = -1;
        
        //every closed frequent itemset is a frequent itemset
        while(iter.hasNext()){
            SemiFCI fci = iter.next();
            FrequentItemset fi = new FrequentItemset(fci.getItems(), 
                    fci.getApproximateSupport(fci.getKValue()),
                    (double)fci.getApproximateSupport(fci.getKValue())/(double)(segmentLength * (fci.getKValue() + 1))
            );
            
            if(fi.support >= Math.ceil(minSupport * segmentLength * (fci.getKValue() + 1))){
                if(last_size != fi.size){
                    levels.add(new ArrayList<FrequentItemset>());
                    last_size = fi.size;
                }
                levels.get(levels.size()-1).add(fi);
            }                               
        }
        
        //now for each fci generate fi by subsets generation
        for(int level = 0; level<levels.size()-1; level++){
            
            for(FrequentItemset fi:levels.get(level)){
                //generate subsets and check support
                for(FrequentItemset subFi:fi.getDirectSubsets()){
                    int index = levels.get(level+1).indexOf(subFi);
                    if(index == -1)
                        levels.get(level+1).add(subFi); //add new fi
                    else
                        levels.get(level+1).get(index).support =
                                Math.max(levels.get(level+1).get(index).support, subFi.support);
                }
            }
        }
        
        List<BehavioralPattern> ret = new ArrayList<>();
        for(List<FrequentItemset> level:levels)
            ret.addAll(level);
        
        return ret;
    }

    @Override
    public double computeSimilarityTo(List<Integer> window) {
        return ((double)UtilitiesPPSDM.computeLongestCommonSubset(items,window)) / 
            ((double)window.size());
    }

    @Override
    public List<Integer> getConsequenceOf(List<Integer> window) {
        List<Integer> toReturn = new ArrayList<>(this.items);
        toReturn.removeAll(window);
        return toReturn;
    }

    @Override
    public Double getWeight() {
        return this.getSupportDouble();
    }

    @Override
    public void setWeightFactor(double distance) {
        this.weightFactor = distance;
    }

    @Override
    public double getWeightFactor() {
        return weightFactor;
    }

    @Override
    public int compareTo(Object o) {
        FrequentItemset other = (FrequentItemset) o;
        if(other.getWeight() < this.getWeight()){
           return 1; 
        }else if(other.getWeight() > this.getWeight()){
            return -1;
        }else{
            return 0;
        }
    }
    

}
