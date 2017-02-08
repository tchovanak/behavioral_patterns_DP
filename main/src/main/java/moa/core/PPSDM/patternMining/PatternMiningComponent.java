/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.patternMining;

import java.util.Iterator;
import java.util.List;
import moa.core.Example;
import moa.core.FrequentItemset;
import moa.core.PPSDM.FciValue;
import moa.core.PPSDM.clustering.ClusteringComponent;
import moa.core.PPSDM.dto.SnapshotResults;



/**
 * This interface is common for all different clustering components of PPSDM method.
 * @author Tomas Chovanak
 */
public interface PatternMiningComponent {

    public void trainOnInstance(Example copy);
    
    public Iterator<FrequentItemset> iteratorGlobalPatterns();

    public Iterator<FrequentItemset> iteratorGroupPatterns(int i);

    public SnapshotResults generateSnapshot(ClusteringComponent clusterer);

    public List<FciValue> extractPatterns();

    public int getSnapshotId();
}
