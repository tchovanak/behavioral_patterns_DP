/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.patternMining;



import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import moa.core.Example;
import moa.core.FrequentItemset;

import moa.core.dto.FIWrapper;

import moa.learners.clustering.ClusteringComponent;

import moa.core.dto.SnapshotResults;

/**
 *
 * @author Tomas
 */
public class PatternMiningEstDecPlus implements PatternMiningComponent{

    private PersonalizedEstDecPlus estDec;
    private int snapshotID = 0;
    
    public PatternMiningEstDecPlus(PersonalizedEstDecPlusConfiguration config) {
        this.estDec = new PersonalizedEstDecPlus(config);
        this.estDec.resetLearning();
    }

    @Override
    public void trainOnInstance(Example e) {
        this.estDec.trainOnInstance(e);
    }
    
    @Override
    public void resetLearning() {
        this.estDec.resetLearning();
    }

    @Override
    public Iterator<FrequentItemset> iteratorGlobalPatterns() {
        try {
            return this.estDec.getGlobalFis().iterator();
        } catch (IOException ex) {
            Logger.getLogger(PatternMiningEstDecPlus.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public Iterator<FrequentItemset> iteratorGroupPatterns(int i) {
        try {
            return this.estDec.getGroupFis(i).iterator();
        } catch (IOException ex) {
            Logger.getLogger(PatternMiningEstDecPlus.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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
