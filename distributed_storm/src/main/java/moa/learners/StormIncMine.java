/*
 *    IncMine.java
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
package moa.learners;

import moa.core.PPSDM.utils.UtilitiesPPSDM;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import moa.core.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.PPSDM.Configuration;
import moa.core.PPSDM.FCITablePPSDM;
import moa.core.PPSDM.SegmentPPSDM;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;


/*
    (Tomas Chovanak) Modified IncMine alghoritm to mine simultanly group and global 
     frequent itemsets of users.
*/
public class StormIncMine {
    
    private static final long serialVersionUID = 1L;
    
    private int windowSizeOption;
    private int maxItemsetLengthOption;
    private double minSupportOption;
    private double relaxationRateOption;
    private int fixedSegmentLengthOption;
    private double sigma;
    private double r;
    private double min_sup;
    private int[] minsup;
    private Jedis jedis;
    private FCITablePPSDM fciTable; 

    public double getMin_sup() {
        return min_sup;
    }
    
    public StormIncMine(int windowSizeOption,int maxItemsetLengthOption,
            double minSupportOption,
            double relaxationRateOption, int fixedSegmentLengthOption
            ){
        this.windowSizeOption = windowSizeOption;
        this.maxItemsetLengthOption = maxItemsetLengthOption;
        this.minSupportOption = minSupportOption;
        this.relaxationRateOption = relaxationRateOption;
        this.fixedSegmentLengthOption = fixedSegmentLengthOption;
        this.sigma = this.minSupportOption;
        this.r = this.relaxationRateOption;
        this.fciTable = new FCITablePPSDM();
        min_sup = new BigDecimal(this.r*this.sigma).setScale(8, RoundingMode.DOWN).doubleValue(); //necessary to correct double rounding error
    
//        // INITIALIZE REDIS
//        this.jedis = jedis;
//        // set the value of the key only if the key doesnt exists.
//        this.jedis.setnx("max_fci_size","0");
        
    }
    
    /**
     * Update the FCITable and the InvertedFCIIndex to keep semiFCIs up to date
     * @param o
     * @param arg
     */
    public void update(SegmentPPSDM segment, double groupid, int windowSize) {  
        fciTable.nAdded = 0;
        fciTable.nRemoved = 0;
        int lastSegmentLenght = segment.size();
        this.minsup = UtilitiesPPSDM.getIncMineMinSupportVector(sigma,r,windowSizeOption,lastSegmentLenght);
        List<SemiFCI> semiFCIs = null;
        try {
            semiFCIs = segment.getFCI(windowSize);
        } catch (Exception ex) {
            Logger.getLogger(StormIncMine.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //for each FCI in the last segment in size ascending order 
        for(SemiFCI fci: semiFCIs) {
            if(fci.getItems().size() == 1){ continue; }            
            SemiFCIid fciId = fciTable.select(fci.getItems());
            boolean newfci = false;
            if(fciId.isValid()) {
                //fci is already in the FCITable
                fciTable.getFCI(fciId).pushSupport(fci.currentSupport());
                computeK(fciId, 0, fciTable);                
            }else{
                //fci is not in the FCITable yet
                newfci = true;
                //set semiFCI support to support of his SFS (last segment excluded)
                SemiFCIid sfsId = fciTable.selectSFS(fci, false);
                
                if(sfsId.isValid()) {
                    int[] fciSupVector = fci.getSupports();
                    int[] sfsSupVector = fciTable.getFCI(sfsId).getSupports(); 
                    //note: the SFS has not been updated yet! so his old support goes from index 0 to length-2
                    if(fciSupVector.length > 1){
                        System.arraycopy(sfsSupVector, 0, fciSupVector, 1, 
                                fciSupVector.length - 2);
                        fci.setSupports(fciSupVector);
                    }
                }
                fciId = fciTable.addSemiFCI(fci);
                computeK(fciId, 0, fciTable);
            }
            
            if(fci.size() > 1){
                enumerateSubsets(new Subset(fci.getItems(),0,false),
                        new ArrayList<Integer>(), fci.getSupports(), fciId, 
                        newfci, fciTable);
            }    
               
        }
        
        //iterate in size-descending order over the entire FCITable to remove unfrequent semiFCIs
        for(Iterator<SemiFCI> iter =  fciTable.iterator(); iter.hasNext(); ) {
            SemiFCI s = iter.next();
            if(!s.isUpdated()) {
                s.pushSupport(0);
                s.setUpdated(false);
                int k = computeK(s.getId(), 0, fciTable);
                if (k == -1){
                    fciTable.removeSemiFCI(s.getId(), iter);
                }
                else{
                    SemiFCIid sfsId = fciTable.selectSFS(s, true);
                    if(sfsId.isValid())
                        fciTable.removeSemiFCI(s.getId(), iter);
                }
            }else{
                s.setUpdated(false);
            }
        }    
        
        fciTable.clearNewItemsetsTable();   
        // check all fciTable
        semiFCIs.clear();
        fciTable.incrementCounter();
        if(fciTable.getCounter() % 10000 == 0){
            fciTable.setCounter(0);
            fciTable.clearTable();
            System.gc();
        }
        
        fciTable.computeSemiFcis(this.fixedSegmentLengthOption);
        //System.out.println(fciTable.size() + " SemiFCIs actually stored\n");
        if(Configuration.RECOMMEND_WITH_FI){
            fciTable.computeFis(minSupportOption, lastSegmentLenght);
        }
        
        // end of update in  REDIS
        //this.jedisCommands.set("max_fci_size", String.valueOf(fciTable.getMaxFCISize()));
        // ADD ALL SEMIFCIs from tmpSemifcis
    }

    /**
     * Enumerates all the proper subsets of the passed itemset with skip of repeated subsets.
     * It allows to skip subsets of semiFCI that have been already updated.
     * @param origSubset original subset to be enumerated
     * @param skipList index of the subsets to be skipped
     * @param supersetSupportVector 
     * @param originalFCI id of the original SemiFCI
     * @param newFCI true if the SemiFCI is a new FCI for the window, false otherwise
     */
    private void enumerateSubsets(Subset origSubset, List<Integer> skipList, 
            int[] supersetSupportVector, SemiFCIid originalFCIid, boolean newFCI, 
            FCITablePPSDM fciTable)
    {
        
        List<Subset> subList = new ArrayList<>();
        List<Integer> blackList = new ArrayList<>();
        
        for(int removeIndex = origSubset.getStartIndex(); removeIndex < origSubset.getItemset().size(); removeIndex++) {
            if(skipList.contains(removeIndex)) { //don't process subsets of an already updated SemiFCI
                if(removeIndex > 0)     blackList.add(removeIndex-1);
                continue;
            }

            //create subset
            List<Integer> reducedItemset = origSubset.getItemset();
            reducedItemset.remove(reducedItemset.size()-removeIndex-1);
            Subset newSubset = new 
                    Subset(reducedItemset, removeIndex, origSubset.skipSubsetsNotInL);
            SemiFCIid id = fciTable.select(newSubset.getItemset());

            if(id.isValid()) {//subset in L
                SemiFCI subFCI = fciTable.getFCI(id); 
                if(subFCI.isUpdated()) {
                    if(removeIndex > 0) blackList.add(removeIndex-1); //its subsets don't have to be updated
                }else {
                    updateSubsetInL(id, supersetSupportVector, originalFCIid, fciTable);
                    subList.add(newSubset);
                    supersetSupportVector = subFCI.getSupports();
                    if(newFCI) newSubset.skipSubsetsNotInL = true;
                }
            }
            else {//subset not in L
                if(!newSubset.skipSubsetsNotInL)
                    subList.add(newSubset);
                if(newFCI)
                    updateSubsetNotInL(newSubset.getItemset(), originalFCIid, fciTable);
            }
        }
        
        for(Subset s:subList) {
            if(s.itemset.size() == 1) return;
            enumerateSubsets(s, blackList, supersetSupportVector, originalFCIid,
                             newFCI, fciTable);
        }
        subList.clear();
        blackList.clear();
    }
    

    /**
     * Updates the support vector of the proper subset passed of a fciId. It also checks
     * if closure condition holds looking at the most recent superset support. If it doesn't hold
     * delete the SemifFCI.
     * @param subsetId id of the subset to be updated
     * @param supersetSupportVector support vector of the most recently processed superset
     * @param fciId id of the original SemiFCI
     */
    private void updateSubsetInL(SemiFCIid subsetId, int[] supersetSupportVector,
            SemiFCIid fciId, FCITablePPSDM fciTable) {

        SemiFCI fci = fciTable.getFCI(fciId);

        fciTable.getFCI(subsetId).pushSupport(fci.currentSupport());
        
        int k = computeK(subsetId, 1, fciTable);

        if(k == -1){
            fciTable.removeSemiFCI(subsetId);
        }else{
            //checking closure property
            if(fciTable.getFCI(subsetId).getApproximateSupport(k) == UtilitiesPPSDM.cumSum(supersetSupportVector, k))
                fciTable.removeSemiFCI(subsetId);
        }
    }
    
    /**
     * Updates a subset that isn't included in the actual semiFCIs set.
     * @param subset subset to be added
     * @param superFCIid original semiFCI
     */
    private void updateSubsetNotInL(List<Integer> subset, SemiFCIid superFCIid, 
                                    FCITablePPSDM fciTable)
    {
        SemiFCI superFCI = fciTable.getFCI(superFCIid);
        SemiFCIid sfsId = fciTable.selectSFS(subset, superFCI.getItems());

        if(sfsId.isValid()) {
            int[] supportVector = fciTable.getFCI(sfsId).getSupports();
            supportVector[0] = superFCI.getSupports()[0];
            
            SemiFCI subFCI = new SemiFCI(subset, 0, this.windowSizeOption);
            subFCI.setSupports(supportVector);
            
            int k = subFCI.computeK(this.minsup,1);
            if(k > -1)
                fciTable.addSemiFCI(subFCI);
            
        }
    }
    
    /**
     * Calls computeK method the passed semiFCI and deletes it if no longer holds
     * the conditions to be maintained in the window
     * @param id id of the semiFCI
     * @param startK intial k value
     * @param fciTable
     * @return value of k
     */
    public int computeK(SemiFCIid id, int startK, FCITablePPSDM fciTable) {
        int k = fciTable.computeK(id, this.minsup, startK);
//        if(k == -1)
//            this.fciTable.removeSemiFCI(id);
        return k;
    }
    
    public int getNumFCIs(FCITablePPSDM fciTable){
        return fciTable.size();
    }
    
    
    public int getNAdded(FCITablePPSDM fciTable){
        return fciTable.nAdded;
    }
    
    public int getNRemoved(FCITablePPSDM fciTable){
        return fciTable.nRemoved;
    }

    public FCITablePPSDM getFciTable() {
        return fciTable;
    }
    
    private class Subset {
        private List<Integer> itemset;
        private int startIndex;
        private boolean skipSubsetsNotInL;
        public Subset(List<Integer> itemset, int startIndex, boolean skipSubsetsNotInL)
        {
            List<Integer> items = new ArrayList<>();
            for(Integer i : itemset){
                items.add(i);
            }
            this.itemset = items;
            this.startIndex = startIndex;
            this.skipSubsetsNotInL = skipSubsetsNotInL;
        }

        public List<Integer> getItemset() {
            List<Integer> items = new ArrayList<>();
            for(Integer i : itemset){
                items.add(i);
            }
            return items;
        }

        public void setItemset(List<Integer> itemset) {
            this.itemset = itemset;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public boolean isSkipSubsetsNotInL() {
            return skipSubsetsNotInL;
        }

        public void setSkipSubsetsNotInL(boolean skipSubsetsNotInL) {
            this.skipSubsetsNotInL = skipSubsetsNotInL;
        }
    }
    

}
