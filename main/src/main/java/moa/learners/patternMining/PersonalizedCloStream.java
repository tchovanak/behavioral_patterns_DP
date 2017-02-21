package moa.learners.patternMining;
/* This file is copyright (c) 2008-2013 Philippe Fournier-Viger
* 
* This file is part of the SPMF DATA MINING SOFTWARE
* (http://www.philippe-fournier-viger.com/spmf).
* 
* SPMF is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* SPMF. If not, see <http://www.gnu.org/licenses/>.
*/

import ca.pfv.spmf.algorithms.frequentpatterns.clostream.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Arrays;
import java.util.Iterator;
import moa.core.Example;
import moa.core.FrequentItemset;

/**
 * This is an implementation of the CloStream algorithm for mining
 * closed itemsets from a stream as proposed 
 * by S.J Yen et al.(2009)
 * in the proceedings of the IEA-AIE 2009 conference, pp.773.
 * <br/><br/>
 * 
 * It is a very simple algorithm that do not use a minimum support threshold.
 * It thus finds all closed itemsets.
 *
 *@see Itemset
 *@author Philippe Fournier-Viger
 */
public class PersonalizedCloStream {
	
    
    PersonalizedCloStreamConfiguration config;

    // a table to store the closed itemsets
    List<Itemset> tableGlobal = new ArrayList<>();
    List<List<Itemset>> tablesGroups = new ArrayList<>();
    
    long globalCounter = 0;
    long[] groupCounters;
    int cleaningConstantSupport = 10;
    
    Map<Integer, List<Integer>> cidListMapGlobal = new HashMap<>();

    /**
     * Constructor that also initialize the algorithm
     */
    public PersonalizedCloStream(PersonalizedCloStreamConfiguration config) { 
            this.config = config;
            // create the empty set with a support of 0
            Itemset emptySet = new Itemset(new int[] {});
            emptySet.setAbsoluteSupport(0);
            // add the empty set in the list of closed sets
            tableGlobal.add(emptySet);
            groupCounters = new long[config.getGC()];
            for(int i = 0; i < config.getGC(); i++){
                emptySet = new Itemset(new int[] {});
                emptySet.setAbsoluteSupport(0);
                tablesGroups.add(new ArrayList<Itemset>());
                tablesGroups.get(i).add(emptySet);
                groupCounters[i] = 0;
            }
    }

    /**
     * This method process a new transaction from a stream to update
     * the set of closed itemsets.
     * @param transaction a transaction (Itemset)
     */
    public void processNewTransaction(Itemset transaction, List<Itemset> tableClosed,
            Map<Integer, List<Integer>> cidListMap){
            
            // a temporary table (as described in the paper) to 
            // associate itemsets with cids.
            Map<Itemset, Integer> tableTemp = new HashMap<>();

            // Line 02 of the pseudocode in the article
            // We add the transaction in a temporary table
            tableTemp.put(transaction, 0); 

            // Line 03  of the pseudocode in the article
            // Create a set to store the combined cidlist of items in the transaction
            Set<Integer> cidset = new HashSet<>();
            // For each item in the transaction
            for(Integer item : transaction.getItems()){
                    // get the cid list of that item
                    List<Integer> cidlist = cidListMap.get(item);
                    if(cidlist != null){
                            // add the cid list to the combined cid list
                            cidset.addAll(cidlist);
                    }
            }

            // Line 04  of the pseudocode in the article
            // For each cid in the combined set of cids
            for(Integer cid : cidset){

                    // Get the closed itemset corresponding to this cid
                    Itemset cti = tableClosed.get(cid);
                    // create the intersection of this closed itemset
                    // and the transaction.
                    Itemset intersectionS = (Itemset) transaction.intersection(cti);

                    // Check if the intersection calculated in the previous step is in Temp
                    boolean found = false;
                    // for each entry in temp
                    for(Map.Entry<Itemset, Integer> entry : tableTemp.entrySet()){
                            // if it is equal to the intersection
                            if(entry.getKey().isEqualTo(intersectionS)){
                                    // we found it 
                                    found = true;
                                    // Get the corresponding closed itemsetitemset  
                                    Itemset ctt = tableClosed.get(entry.getValue());
                                    // if the support of cti is higher than ctt
                                    if(cti.getAbsoluteSupport() > ctt.getAbsoluteSupport()){  
                                            // set the value as "cid".
                                            entry.setValue(cid);
                                    }
                                    break;
                            }
                    }
                    // If the search was unsuccessful
                    if(found == false){ 
                            // add the instersection to the temporary table with "cid".
                            tableTemp.put(intersectionS, cid);
                    }
            }

            // For each entry in the temporary table
            for(Map.Entry<Itemset, Integer> xc : tableTemp.entrySet()){
                    // get the itemset
                    Itemset x = xc.getKey();
                    // get the cid
                    Integer c = xc.getValue();
                    // get the closed itemset for that cid
                    Itemset ctc = tableClosed.get(c);

                    // if the itemset is the same as the closed itemset
                    if(x.isEqualTo(ctc)){
                            // we have to increase its support
                            ctc.increaseTransactionCount();
                    }else{ 
                            // otherwise the itemset "x" is added to the table of closed itemsets
                            tableClosed.add(x);
                            // its support count is set to the support of ctc + 1.
                            x.setAbsoluteSupport(ctc.getAbsoluteSupport()+1);
                            // Finally, we loop over each item in the transaction again
                            for(Integer item : transaction.getItems()){
                                    // we get the cidlist of the current item
                                    List<Integer> cidlist = cidListMap.get(item);
                                    // if null
                                    if(cidlist == null){
                                            cidlist = new ArrayList<Integer>();
                                            // we  create one
                                            cidListMap.put(item, cidlist);
                                    }
                                    // then we add x to the cidlist
                                    cidlist.add(tableClosed.size()-1);
                            }
                    }
            }
    }

    /**
     * Get the current list of closed itemsets without the empty set.
     * @return a List of closed itemsets
     */
    public List<Itemset> getClosedItemsets() {
//            // if the empty set is here
//            if(tableG.get(0).size() ==0){
//                    // remove it
//                    tableClosed.remove(0); 
//            }
//            // return the remaining closed itemsets
//            return tableClosed;
        return null;
    }

    public void trainOnInstance(Example e) {
        Instance inst = (Instance)e.getData();
        int[] items = new int[inst.numValues()-1];
        int i = 0;
        for(int val = 2; val < inst.numValues(); val++){ // from val 2 because first val is groupid and second uid
            items[i++] = (int)inst.value(val);
        }
        Itemset itemset = new Itemset(items);
        if(inst.numValues() <= 1){
            return;
        }
        int groupid = (int)inst.value(0);// on index 0 there should be group id prepended before session data
        if(groupid > -1){
            groupCounters[groupid]++;
            processNewTransaction(itemset, tablesGroups.get(groupid), cidListMapGlobal);
        }else{
            globalCounter++;
            if(globalCounter % 5000 == 0){
                cleanTable(tableGlobal);
            }
            processNewTransaction(itemset, tableGlobal, cidListMapGlobal);
        }
        
    }

    void resetLearning() {
        // create the empty set with a support of 0
        Itemset emptySet = new Itemset(new int[] {});
        emptySet.setAbsoluteSupport(0);
        // add the empty set in the list of closed sets
        tableGlobal = new ArrayList<>();
        tablesGroups = new ArrayList<>();
        tableGlobal.add(emptySet);
        groupCounters = new long[config.getGC()];
        for(int i = 0; i < config.getGC(); i++){
            emptySet = new Itemset(new int[] {});
            emptySet.setAbsoluteSupport(0);
            
            tablesGroups.add(new ArrayList<Itemset>());
            tablesGroups.get(i).add(emptySet);
            groupCounters[i] = 0;
        }
    }
    
    public List<FrequentItemset> getGlobalFis() {
        List<FrequentItemset> fis = new ArrayList<>();
        for(Itemset it : this.tableGlobal){
            if(it.size() == 0){
                continue;
            }
            FrequentItemset fi = new FrequentItemset(it.getItems(), 
                    it.getAbsoluteSupport(),
                    (double)it.getAbsoluteSupport()/this.globalCounter);
            fis.add(fi);
        }
        return fis;
    }

    public List<FrequentItemset> getGroupFis(int i) {
        List<FrequentItemset> fis = new ArrayList<>();
        for(Itemset it : this.tablesGroups.get(i)){
            if(it.size() == 0){
                continue;
            }
            FrequentItemset fi = new FrequentItemset(it.itemset, 
                    it.getAbsoluteSupport(), 
                    (double)it.getAbsoluteSupport()/this.groupCounters[i]);
            fis.add(fi);
        }
        return fis;
    }

    private void cleanTable(List<Itemset> tableGlobal) {
        int index = 0;
        double supp = 10.0 * (this.globalCounter/100.0);
        for (Iterator<Itemset> iterator = tableGlobal.iterator(); iterator.hasNext();index++) {
            Itemset i = iterator.next();
            
            if(i.getAbsoluteSupport() < supp){
                iterator.remove();
//                for(Integer item : i.itemset){
//                    cidListMap.get(item).remove(new Integer(index));
//                    if(cidListMap.get(item).isEmpty()){
//                        cidListMap.remove(item);
//                    }
//                }
            }
            
        }
        cidListMapGlobal.clear();
        List<Itemset> newTable = new ArrayList<>();
        Itemset emptySet = new Itemset(new int[] {});
        emptySet.setAbsoluteSupport(0);
        // add the empty set in the list of closed sets
        newTable.add(emptySet);
        for(Itemset it: tableGlobal){
            processNewTransaction(it, newTable, cidListMapGlobal);
        }
        this.tableGlobal = newTable;
        
    }
}
