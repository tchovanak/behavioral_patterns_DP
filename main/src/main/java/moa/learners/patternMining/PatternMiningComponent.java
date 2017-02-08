/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.patternMining;

import java.util.Iterator;
import java.util.List;
import moa.core.Example;
import moa.core.PPSDM.FrequentItemset;
import moa.core.dto.FIWrapper;
import moa.learners.clustering.ClusteringComponent;
import moa.core.dto.SnapshotResults;



/**
 * This interface is common for all different clustering components of PPSDM method.
 * @author Tomas Chovanak
 */
public interface PatternMiningComponent {

    public void trainOnInstance(Example copy);
    
    public Iterator<FrequentItemset> iteratorGlobalPatterns();

    public Iterator<FrequentItemset> iteratorGroupPatterns(int i);

    public SnapshotResults generateSnapshot(ClusteringComponent clusterer);

    public List<FIWrapper> extractPatterns();

    public int getSnapshotId();

    public void resetLearning();
}
