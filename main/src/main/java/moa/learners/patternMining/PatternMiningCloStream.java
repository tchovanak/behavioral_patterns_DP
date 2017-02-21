/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.patternMining;



import java.util.Iterator;
import java.util.List;

import moa.core.Example;
import moa.core.FrequentItemset;

import moa.core.dto.FIWrapper;

import moa.learners.clustering.ClusteringComponent;

import moa.core.dto.SnapshotResults;

/**
 *
 * @author Tomas
 */
public class PatternMiningCloStream implements PatternMiningComponent{

    private PersonalizedCloStream clostream;
    private int snapshotID = 0;
    
    public PatternMiningCloStream(PersonalizedCloStreamConfiguration config) {
        this.clostream = new PersonalizedCloStream(config);
        this.clostream.resetLearning();
    }

    @Override
    public void trainOnInstance(Example e) {
        this.clostream.trainOnInstance(e);
    }
    
    @Override
    public void resetLearning() {
        this.clostream.resetLearning();
    }

    @Override
    public Iterator<FrequentItemset> iteratorGlobalPatterns() {
        return this.clostream.getGlobalFis().iterator();
    }

    @Override
    public Iterator<FrequentItemset> iteratorGroupPatterns(int i) {
        return this.clostream.getGroupFis(i).iterator();
    }

    @Override
    public SnapshotResults generateSnapshot(ClusteringComponent clustererPPSDM) {
         throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    @Override
    public List<FIWrapper> extractPatterns() {
         throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSnapshotId() {
        return snapshotID;
    }
    
}
