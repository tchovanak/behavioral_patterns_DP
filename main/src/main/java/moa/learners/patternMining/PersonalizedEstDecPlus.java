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

import ca.pfv.spmf.algorithms.frequentpatterns.estDec.CPTree;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;


import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.tools.MemoryLogger;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.IOException;
import java.util.Hashtable;

import java.util.Iterator;
import moa.core.BehavioralPattern;
import moa.core.Example;
import moa.core.FrequentItemset;
import moa.core.utils.UtilitiesPPSDM;

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
public class PersonalizedEstDecPlus {
	  
    PersonalizedEstDecPlusConfiguration config;

    // the Compressible Prefix tree
    CPTree treeGlobal; 
    List<CPTree> treesGroups = new ArrayList<>();
    
    // the number of transactions
    int transactionCountGlobal = 0; 
    int[] transactionsCountGroup;
    int transactionCounter =0;
    
    // the total time for mining (for stats)
    private long miningTime = 0;

    // the total time for transaction insertion (for stats)
    double sumTransactionInsertionTime = 0; 
    
   
    /**
     * Constructor that also initialize the algorithm
     */
    public PersonalizedEstDecPlus(PersonalizedEstDecPlusConfiguration config) { 
            this.config = config;
            resetLearning();
    }
    
//    /**
//    * A method to set the decay rate "d" using the "b" and "h" parameter (see the
//    * EstDec article)
//    * @param b  decay base 
//    * @param h decay-base life
//    */
//    public void setDecayRate(double b, double h) {
//       tree.setDecayRate(b, h);
//    }


    /**
     * Process a transaction (add it to the tree and update itemsets
     * 
     * @param transaction an ArrayList of integers
     */
    public void processTransaction(int[] transaction, CPTree tree, int group) {
            
            // record st
            double startCTimestamp = System.currentTimeMillis();

            // phase 1) Parameter updating
            tree.updateParams();

            // phase 2) Node restructuring
            for (int i = 0; i < tree.root.children.size(); ++i)
                    tree.traverse(tree.root.children.get(i), tree.root, (short) -1,	transaction);
            
            int transactionCount = (group < 0)? ++transactionCountGlobal : ++transactionsCountGroup[group];
            // phase 3) Itemset Insertion
            tree.insertItemset(transaction, transactionCountGlobal, this.config.getStreamStartTime(), this.config.getMTS());
            if(transactionCount % 1000 == 0){
                tree.forcePruning(tree.root);
                tree.N = 0;
            }
            sumTransactionInsertionTime += (System.currentTimeMillis() - startCTimestamp);
    }

    public void trainOnInstance(Example e) {
        // Perform mining
        // SPEED REGULATION PART
        this.transactionCounter++;
        double[] time = UtilitiesPPSDM.getActualTransSec(this.transactionCounter,
                config.getStreamStartTime());
        System.out.println(time[0] + " " + time[1]);
        if(time[1] > 10 && time[0] < config.getMTS()){
            return;
        }
        Instance inst = (Instance)e.getData();
        int[] items = new int[inst.numValues()-1];
        int i = 0;
        for(int val = 2; val < inst.numValues(); val++){ // from val 2 because first val is groupid and second uid
            items[i++] = (int)inst.value(val);
        }
        if(inst.numValues() <= 1){
            return;
        }
        int groupid = (int)inst.value(0);// on index 0 there should be group id prepended before session data
        
        this.processTransaction(items, treeGlobal, -1);
        if(groupid >= 0){
           this.processTransaction(items, treesGroups.get(groupid), groupid);
        }
    }

    void resetLearning() {
        // Reset memory logger
        MemoryLogger.getInstance().reset();

        // create the "Monitoring Lattice" tree
        treeGlobal = new CPTree(config.getD(), config.getMS(), config.getMinSigValue(), 
                config.getDeltaValue(), config.getMinMergeValue());
       
        transactionsCountGroup = new int[config.getGC()];
        treesGroups = new ArrayList<>();
        
        for(int i = 0; i < config.getGC(); i++){
            treesGroups.add(new CPTree(config.getD(), config.getMS(), config.getMinSigValue(), 
                config.getDeltaValue(), config.getMinMergeValue()));
            
            transactionsCountGroup[i] = 0;
        }
    }
    
    public List<BehavioralPattern> getGlobalFis() throws IOException {
        List<BehavioralPattern> fis = new ArrayList<>();
        // Perform mining
        // SPEED REGULATION PART
        double[] time = UtilitiesPPSDM.getActualTransSec(this.transactionCountGlobal,
                config.getStreamStartTime());
        Hashtable<int[], Double> patterns;
        if(time[0] < config.getMTS()){
            patterns = treeGlobal.getPatterns();
        }else{
            patterns = treeGlobal.patternMining_saveToMemory();
        }
	
        for (Iterator<Map.Entry<int[], Double>> it = patterns.entrySet().iterator(); it.hasNext();) {
            Map.Entry<int[], Double> e = it.next();
            int[] itemset = e.getKey();
            double support = e.getValue();
            if(itemset.length == 0){
                continue;
            }
            FrequentItemset fi = new FrequentItemset(itemset,support);
            fis.add(fi);
        }
        return fis;
    }

    public List<BehavioralPattern> getGroupFis(int i) throws IOException {
       List<BehavioralPattern> fis = new ArrayList<>();
       // Perform mining
        // SPEED REGULATION PART
        double[] time = UtilitiesPPSDM.getActualTransSec(this.transactionCountGlobal,
                config.getStreamStartTime());
        Hashtable<int[], Double> patterns;
        if(time[0] < config.getMTS()){
            patterns = treesGroups.get(i).getPatterns();
        }else{
            patterns = treesGroups.get(i).patternMining_saveToMemory();
        }
        for (Iterator<Map.Entry<int[], Double>> it = patterns.entrySet().iterator(); it.hasNext();) {
            Map.Entry<int[], Double> e = it.next();
            int[] itemset = e.getKey();
            double support = e.getValue();
            if(itemset.length == 0){
                continue;
            }
            FrequentItemset fi = new FrequentItemset(itemset,support);
            fis.add(fi);
        }
        return fis;
    }

    
}
