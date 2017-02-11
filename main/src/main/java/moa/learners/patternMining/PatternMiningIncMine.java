/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.patternMining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import moa.core.Example;
import moa.core.PPSDM.FrequentItemset;
import moa.core.FCITablePPSDM;
import moa.core.dto.FIWrapper;
import moa.learners.clustering.UserModelPPSDM;
import moa.learners.clustering.ClusteringComponent;
import moa.core.dto.GroupStatsResults;
import moa.core.dto.SnapshotResults;

/**
 *
 * @author Tomas
 */
public class PatternMiningIncMine implements PatternMiningComponent{

    private PersonalizedIncMine incMine;
    private int snapshotID = 0;
    
    public PatternMiningIncMine(PersonalizedIncMineConfiguration config) {
        this.incMine = new PersonalizedIncMine(config);
        this.incMine.resetLearning();
    }

    @Override
    public void trainOnInstance(Example e) {
        this.incMine.trainOnInstance(e);
    }
    
    @Override
    public void resetLearning() {
        this.incMine.resetLearning();
    }

    @Override
    public Iterator<FrequentItemset> iteratorGlobalPatterns() {
        return this.incMine.fciTableGlobal.getSemiFcis().iterator();
    }

    @Override
    public Iterator<FrequentItemset> iteratorGroupPatterns(int i) {
        return this.incMine.fciTablesGroups.get(i).getSemiFcis().iterator();
    }

    @Override
    public SnapshotResults generateSnapshot(ClusteringComponent clustererPPSDM) {
        SnapshotResults snap = new SnapshotResults();
        snap.setId(snapshotID++);
        List<GroupStatsResults> listGStats = new ArrayList<>();
        Map<List<Integer>, Integer> frequencyOfPatterns = new HashMap<>();
        FCITablePPSDM fciTableGlobal = this.incMine.fciTableGlobal;
        List<FCITablePPSDM> fciTablesGroup = this.incMine.fciTablesGroups;
        fciTableGlobal.computeFcis(this.incMine.getMinSupport(),
                this.incMine.getFixedSegmentLength());
        GroupStatsResults globRes = new GroupStatsResults();
        globRes.setGroupid(-1);
        globRes.setClusteringId(clustererPPSDM.getClusteringID());
        Iterator<FrequentItemset> itGlob = fciTableGlobal.getFcis().iterator();
        List<FIWrapper> globPatterns = new ArrayList<>();
        while(itGlob.hasNext()){
            FrequentItemset sfci = itGlob.next();
            if(frequencyOfPatterns.containsKey(sfci.getItems())){
                frequencyOfPatterns.put(sfci.getItems(), frequencyOfPatterns.get(sfci.getItems()));
            }else{
                frequencyOfPatterns.put(sfci.getItems(),1);
            }
            FIWrapper fciVal = new FIWrapper();
            fciVal.setItems(sfci.getItems());
            fciVal.setSupport(sfci.getSupportDouble());
            fciVal.setGroupid(-1);
            globPatterns.add(fciVal);
        }
        globRes.setFcis(globPatterns);
        listGStats.add(globRes);
        int groupid = 0;
        for(FCITablePPSDM gTable: fciTablesGroup){
            GroupStatsResults gRes = new GroupStatsResults();
            gRes.setClusteringId(clustererPPSDM.getClusteringID());
            gRes.setGroupid(groupid);
            gTable.computeFcis(incMine.getMinSupport(),incMine.getFixedSegmentLength());
            Iterator<FrequentItemset> itGr = gTable.getFcis().iterator();
            List<FIWrapper> groupPatterns = new ArrayList<>(); 
            while(itGr.hasNext()){
                FrequentItemset sfci = itGr.next();
                if(frequencyOfPatterns.containsKey(sfci.getItems())){
                    frequencyOfPatterns.put(sfci.getItems(), frequencyOfPatterns.get(sfci.getItems()) + 1);
                }else{
                    frequencyOfPatterns.put(sfci.getItems(),1);
                }
                FIWrapper fciVal = new FIWrapper();
                fciVal.setItems(sfci.getItems());
                fciVal.setSupport(sfci.getSupportDouble());
                fciVal.setGroupid(groupid);
                groupPatterns.add(fciVal);
            }
            groupid++;
            gRes.setFcis(groupPatterns);
            listGStats.add(gRes);
        }
        
        setUserModels(listGStats, clustererPPSDM);
        for(GroupStatsResults gStat : listGStats){
            // COMPUTE NUM OF UNIQUE PATTERNS
            int numOfUniquePatterns = 0;
            double sumSupportUniquePatterns = 0;
            double sumSupportAllPatterns = 0;
            for(FIWrapper fci : gStat.getFcis()){
                List<Integer> items = fci.getItems();
                int freq = frequencyOfPatterns.get(items);
                if(freq == 1){
                    numOfUniquePatterns++;
                    sumSupportUniquePatterns += fci.getSupport();
                }
                sumSupportAllPatterns += fci.getSupport();
            }
            gStat.setNumOfUniquePatterns(numOfUniquePatterns);
            gStat.setAverageSupportOfUniquePatterns(
                    sumSupportUniquePatterns/numOfUniquePatterns
            );
            gStat.setAverageSupport(sumSupportAllPatterns/gStat.getFcis().size());
        }
        snap.setGroupStats(listGStats);
        return snap;
    }
    
    private void setUserModels(List<GroupStatsResults> results, ClusteringComponent clustererPPSDM){
        if(clustererPPSDM.isClustering()){
            for(int i = 0; i < clustererPPSDM.getClustering().getClustering().size(); i++){
                GroupStatsResults res = results.get(i);
                res.setCentre(clustererPPSDM.getClustering().getClustering().get(i).getCenter());
            }
        }
        for(UserModelPPSDM um : clustererPPSDM.getUserModels().values()){
            if(clustererPPSDM.getClusteringID() - um.getClusteringId() == 0){
                results.get((int)um.getGroupid() + 1).addUid(um.getId());
            }else{
                //results.get(0).addUid(um.getId());
            }
        }
    }

    @Override
    public List<FIWrapper> extractPatterns() {
        FCITablePPSDM fciTableGlobal = this.incMine.fciTableGlobal;
        List<FCITablePPSDM> fciTablesGroup = this.incMine.fciTablesGroups;
        List<FIWrapper> allPatterns = new ArrayList<>();
        
        fciTableGlobal.computeFcis(this.incMine.getMinSupport(), 
                this.incMine.getFixedSegmentLength());
        Iterator<FrequentItemset> itGlob = fciTableGlobal.getFcis().iterator();
        while(itGlob.hasNext()){
            FrequentItemset sfci = itGlob.next();
            //double support = this.calculateSupport(sfci);
            //if(support >= this.minSupportOption.getValue()){
                FIWrapper fciVal = new FIWrapper();
                fciVal.setItems(sfci.getItems());
                fciVal.setSupport(sfci.getSupportDouble());
                fciVal.setGroupid(-1);
                allPatterns.add(fciVal);
            //}
        }
        int groupid = 0;
        for(FCITablePPSDM gTable: fciTablesGroup){
            gTable.computeFcis(this.incMine.getMinSupport(), 
                this.incMine.getFixedSegmentLength());
            Iterator<FrequentItemset> itGr = gTable.getFcis().iterator();
            while(itGr.hasNext()){
                FrequentItemset sfci = itGr.next();
                //double support = this.calculateSupport(sfci);
                //if(support >= this.minSupportOption.getValue()){
                    FIWrapper fciVal = new FIWrapper();
                    fciVal.setItems(sfci.getItems());
                    //fciVal.setFci(sfci);
                    fciVal.setSupport(sfci.getSupportDouble());
                    fciVal.setGroupid(groupid);
                    allPatterns.add(fciVal);
                //}
            }
            groupid++;
        }
        return allPatterns;
    }

    @Override
    public int getSnapshotId() {
        return snapshotID;
    }
    
}
