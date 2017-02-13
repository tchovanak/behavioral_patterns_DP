/*
 *    @author Tomas Chovanak ()

 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.evaluation;

import moa.AbstractMOAObject;
import moa.core.Example;
import java.util.LinkedList;
import java.util.Queue;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.dto.GroupStatsResults;
import moa.core.dto.RecommendationResults;
import moa.core.dto.SummaryResults;
import moa.core.utils.UtilitiesPPSDM;

/**
 * Pattern mining evaluator that performs basic incremental evaluation. 
 * @author Tomas Chovanak
 */
public class RecommendationEvaluator extends AbstractMOAObject{
    
    
    private int[] numRecommendedItems = {1,2,3,4,5,10,15};
    private int[] ggSumAllHits = new int[this.numRecommendedItems.length]; // 1,2,3,4,5,10,15  recommended items
    private int[] ggSumRealRecommended = new int[this.numRecommendedItems.length];
    private int[] glSumAllHits = new int[this.numRecommendedItems.length];
    private int[] glSumRealRecommended = new int[this.numRecommendedItems.length];
    private int[] grSumAllHits = new int[this.numRecommendedItems.length];
    private int[] grSumRealRecommended = new int[this.numRecommendedItems.length];
    private int[] maxRecommendedItems = new int[this.numRecommendedItems.length];
    private int[] allTestedItems = new int[this.numRecommendedItems.length];
    
    private double[] ggSumNDCG = new double[this.numRecommendedItems.length];
    private double[] ggSumPrecision = new double[this.numRecommendedItems.length];
    private double[] ggSumRecall = new double[this.numRecommendedItems.length];
    private double[] glSumNDCG = new double[this.numRecommendedItems.length];
    private double[] glSumPrecision = new double[this.numRecommendedItems.length];
    private double[] glSumRecall = new double[this.numRecommendedItems.length];
    private double[] grSumNDCG = new double[this.numRecommendedItems.length];
    private double[] grSumPrecision = new double[this.numRecommendedItems.length];
    private double[] grSumRecall = new double[this.numRecommendedItems.length];
    
    private Map<Integer,GroupStatsResults> groupStats = new HashMap<>();
    
    private BitSet[] h000 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h001 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h010 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h100 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h011 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h101 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h110 = new BitSet[this.numRecommendedItems.length];
    private BitSet[] h111 = new BitSet[this.numRecommendedItems.length];
    
    private int[] numOfTestedTransactions = new int[this.numRecommendedItems.length];
    
    private FileWriter writer = null;
    private EvaluationConfiguration config;

    public RecommendationEvaluator(EvaluationConfiguration config)  {
        this.config = config;
        
        if(config.getExtractDetails()){
            try {
                this.writer = new FileWriter(config.getOutputFile());
                writer.append("TRANSACTION ID");writer.append(',');  // transaction id
                writer.append("TOTAL LENGTH");writer.append(',');  // transaction length
                writer.append("TEST LENGTH");writer.append(',');  // evaluation test length
                writer.append("H111");writer.append(',');  
                writer.append("H110");writer.append(',');  
                writer.append("H101");writer.append(',');  
                writer.append("H100");writer.append(',');  
                writer.append("H011");writer.append(',');  
                writer.append("H010");writer.append(',');  
                writer.append("H001");writer.append(','); 
                writer.append("H000");writer.append(',');  
                writer.append("PRECISION GG");writer.append(',');  
                writer.append("RECALL GG");writer.append(',');  
                writer.append("NDCG GG");writer.append(',');  
                writer.append("PRECISION GL");writer.append(',');  
                writer.append("RECALL GL");writer.append(','); 
                writer.append("NDCG GL");writer.append(','); 
                writer.append("TRANSSEC");writer.append(',');
                writer.append("GROUPS");writer.append(',');writer.append('\n');
                writer.flush();
            } catch (IOException ex) {
                Logger.getLogger(RecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void writeResultsToFile(Integer counter, Integer testSize, Integer windowSize,Double transsec) {
        try {
            if(this.writer == null)
                this.writer = new FileWriter(config.getOutputFile());
            writer.append(counter.toString());writer.append(',');  // transaction id
            writer.append(((Integer)(testSize + windowSize)).toString());writer.append(',');  // transaction length
            writer.append(testSize.toString());writer.append(',');  // test length
            for(BitSet h : this.h111){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h110){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h101){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h100){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h011){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h010){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h001){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            for(BitSet h : this.h000){
                if(h == null){ writer.append(':'); continue;} 
                writer.append(((Integer)h.cardinality()).toString()); 
                writer.append(':');
            } 
            writer.append(',');
            
            for(int i = 0; i < this.numRecommendedItems.length; i++){
                Double ggPrecision = this.ggSumPrecision[i]/this.numOfTestedTransactions[i];
                writer.append(ggPrecision.toString());
                if(i < this.numRecommendedItems.length - 1) writer.append(':');
            }
            writer.append(',');
            for(int i = 0; i < this.numRecommendedItems.length; i++){
                Double ggRecall = this.ggSumRecall[i]/this.numOfTestedTransactions[i];
                writer.append(ggRecall.toString());
                if(i < this.numRecommendedItems.length - 1) writer.append(':');
            }
            writer.append(',');
            for(int i = 0; i < this.numRecommendedItems.length; i++){
                Double ggNdcg = this.ggSumNDCG[i]/this.numOfTestedTransactions[i];
                writer.append(ggNdcg.toString());
                if(i < this.numRecommendedItems.length - 1) writer.append(':');
            }
            writer.append(',');
            
            for(int i = 0; i < this.numRecommendedItems.length; i++){
                Double goPrecision = this.glSumPrecision[i]/this.numOfTestedTransactions[i];
                writer.append(goPrecision.toString());
                if(i < this.numRecommendedItems.length - 1) writer.append(':');
            }
            writer.append(',');
            
           for(int i = 0; i < this.numRecommendedItems.length; i++){
                Double goRecall= this.grSumRecall[i]/this.numOfTestedTransactions[i];
                writer.append(goRecall.toString());
                if(i < this.numRecommendedItems.length - 1) writer.append(':');
            }
            writer.append(',');
            
            for(int i = 0; i < this.numRecommendedItems.length; i++){
                Double goNdcg = this.glSumNDCG[i]/this.numOfTestedTransactions[i];
                writer.append(goNdcg.toString());
                if(i < this.numRecommendedItems.length - 1) writer.append(':');
            }
            writer.append(',');
           
            
            // number of hits from group
            writer.append((transsec).toString());writer.append(',');
            //double pom = Configuration.GROUP_CHANGED_TIMES/Configuration.GROUP_CHANGES;
            //writer.append(((Double)pom).toString());writer.append(',');
           // writer.append(((Integer)Configuration.GROUP_COUNTER).toString());writer.append('\n');
            writer.flush(); 
        } catch (IOException ex) {
            Logger.getLogger(RecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addResult(RecommendationResults recs, int windowSize, 
            double transsec, Integer counter) {
        
        for(int i = 0; i < numRecommendedItems.length; i++){
            this.h000[i] = null;
            this.h001[i] = null;
            this.h010[i] = null;
            this.h100[i] = null;
            this.h011[i] = null;
            this.h101[i] = null;
            this.h110[i] = null;
            this.h111[i] = null;
        }
        
        int i = 0;
        for(int numRec : this.numRecommendedItems){
            int testSize = recs.getTestWindow().size();
            if(testSize >= numRec){
                addResult(i, recs.getFirstNRecommendationsGGC(numRec), 
                        recs.getFirstNRecommendationsGO(numRec),
                        recs.getFirstNRecommendationsOG(numRec), 
                        recs.getTestWindow(), recs.getGroupid());
            }
            i++;
        }
        if(config.getExtractDetails()){
            writeResultsToFile(counter, recs.getTestWindow().size(), windowSize,transsec);
        }
    }
    
    public void addResult(int ind, List<Integer> recsGGC,List<Integer> recsGO,List<Integer> recsOG, 
                            List<Integer> testWindow, int groupid){
        
        BitSet intersectGGC = UtilitiesPPSDM.computeIntersection(recsGGC,testWindow);
        BitSet intersectGO = UtilitiesPPSDM.computeIntersection(recsGO,testWindow);
        BitSet intersectOG = UtilitiesPPSDM.computeIntersection(recsOG, testWindow);
        
        h111[ind] = (BitSet)intersectGGC.clone();
        h111[ind].and(intersectGO);
        h111[ind].and(intersectOG);
        
        h110[ind] = (BitSet)intersectGO.clone();
        h110[ind].and(intersectOG);
        h110[ind].andNot(intersectGGC);
        
        h101[ind] = (BitSet)intersectGO.clone();
        h101[ind].andNot(intersectOG);
        h101[ind].and(intersectGGC);
        
        h100[ind] = (BitSet)intersectGO.clone();
        h100[ind].andNot(intersectOG);
        h100[ind].andNot(intersectGGC);
        
        h011[ind] = ((BitSet)intersectGO.clone());
        h011[ind].flip(0, this.numRecommendedItems[ind]);
        h011[ind].and(intersectOG);
        h011[ind].and(intersectGGC);
        
        h010[ind] = ((BitSet)intersectGO.clone());
        h010[ind].flip(0, this.numRecommendedItems[ind]);
        h010[ind].and(intersectOG);
        h010[ind].andNot(intersectGGC);
        
        h001[ind] = ((BitSet)intersectGO.clone());
        h001[ind].flip(0, this.numRecommendedItems[ind]);
        h001[ind].andNot(intersectOG);
        h001[ind].and(intersectGGC);
        
        h000[ind] = ((BitSet)intersectGO.clone());
        h000[ind].flip(0, this.numRecommendedItems[ind]);
        h000[ind].andNot(intersectOG);
        h000[ind].andNot(intersectGGC);
        
        double idcg = 1;
        for(int i = 1; i < this.numRecommendedItems[ind]; i++){
            idcg += 1/Math.log(i + 1);
        }
        double dcgGG = 0;
        for(int i = 0; i < intersectGGC.size();i++){
            if(i == 0 && intersectGGC.get(0)){
                dcgGG += 1;
            }else{
                if(intersectGGC.get(i)){
                    dcgGG += 1/Math.log(i + 1);
                }
            }
        }
        double dcgGO = 0;
        for(int i = 0; i < intersectGO.size();i++){
            if(i == 0 && intersectGO.get(0)){
                dcgGO += 1;
            }else{
                if(intersectGO.get(i)){
                    dcgGO += 1/Math.log(i + 1);
                }
            }
        }
        double dcgOG = 0;
        for(int i = 0; i < intersectOG.size();i++){
            if(i == 0 && intersectOG.get(0)){
                dcgOG += 1;
            }else{
                if(intersectOG.get(i)){
                    dcgOG += 1/Math.log(i + 1);
                }
            }
        }
        
        this.ggSumAllHits[ind] += intersectGGC.cardinality();
        this.ggSumRealRecommended[ind] += recsGGC.size();
        this.glSumAllHits[ind] += intersectGO.cardinality();
        this.glSumRealRecommended[ind] += recsGO.size();
        this.grSumAllHits[ind] += intersectOG.cardinality();
        this.grSumRealRecommended[ind] += recsOG.size();
        this.allTestedItems[ind] += testWindow.size();
        this.numOfTestedTransactions[ind] += 1;
        this.ggSumNDCG[ind] += dcgGG/idcg;
        this.glSumNDCG[ind] += dcgGO/idcg;
        this.grSumNDCG[ind] += dcgOG/idcg;
        double precGGC = (double)intersectGGC.cardinality()/(double)numRecommendedItems[ind];
        double precGO = (double)intersectGO.cardinality()/(double)numRecommendedItems[ind];
        double precOG = (double)intersectOG.cardinality()/(double)numRecommendedItems[ind];
        this.ggSumPrecision[ind] += precGGC;
        this.glSumPrecision[ind] += precGO;
        this.grSumPrecision[ind] += precOG;
        this.ggSumRecall[ind] += (double)intersectGGC.cardinality()/(double)testWindow.size();
        this.grSumRecall[ind] += (double)intersectGO.cardinality()/(double)testWindow.size();
        this.grSumRecall[ind] += (double)intersectOG.cardinality()/(double)testWindow.size();
        GroupStatsResults gsr = null;
        if(this.groupStats.containsKey(groupid)){
           gsr  = this.groupStats.get(groupid);
        }else{
           gsr = new GroupStatsResults();
           gsr.setGroupid(groupid);
           this.groupStats.put(groupid, gsr);
        }
        gsr.numTransactionsForGroup[ind]++;
        gsr.sumPrecisionForGroupGGC[ind] += precGGC;
        gsr.sumPrecisionForGroupGO[ind] += precGO;
        gsr.sumPrecisionForGroupOG[ind] += precOG;
        if(recsGGC.size() > 0){
            gsr.sumHitsForGroupGGC[ind] += (double)intersectGGC.cardinality();
            gsr.sumRecsForGroupGGC[ind] += recsGGC.size();
        }
        if(recsGO.size() > 0){
            gsr.sumHitsForGroupGO[ind] += (double)intersectGO.cardinality();
            gsr.sumRecsForGroupGO[ind] += recsGO.size();
        }
        if(recsOG.size() > 0){
            gsr.sumHitsForGroupOG[ind] += (double)intersectOG.cardinality();
            gsr.sumRecsForGroupOG[ind] += recsOG.size();
        }
           
    }
    
    
    public void setSnapshotResults(List<GroupStatsResults> gStats){
        for(GroupStatsResults gStat : gStats){
            GroupStatsResults gStatThis = new GroupStatsResults();
            if(this.groupStats.containsKey(gStat.getGroupid())){
                 gStatThis = this.groupStats.get(gStat.getGroupid());   
            }else{
                continue;
            }
            gStat.numTransactionsForGroup = gStatThis.numTransactionsForGroup;
            gStat.sumHitsForGroupGGC = gStatThis.sumHitsForGroupGGC;
            gStat.sumHitsForGroupGO = gStatThis.sumHitsForGroupGO;
            gStat.sumHitsForGroupOG = gStatThis.sumHitsForGroupOG;
            gStat.sumRecsForGroupGGC = gStatThis.sumRecsForGroupGGC;
            gStat.sumRecsForGroupGO = gStatThis.sumRecsForGroupGO;
            gStat.sumRecsForGroupOG = gStatThis.sumRecsForGroupOG;
            gStat.sumPrecisionForGroupGGC = gStatThis.sumPrecisionForGroupGGC;
            gStat.sumPrecisionForGroupGO = gStatThis.sumPrecisionForGroupGO;
            gStat.sumPrecisionForGroupOG = gStatThis.sumPrecisionForGroupOG;
        }
    }
    
    
    public SummaryResults getResults() {
        SummaryResults results = new SummaryResults(numRecommendedItems);
        results.setAllHitsGGC(ggSumAllHits);
        results.setAllHitsGO(glSumAllHits);
        results.setAllHitsOG(grSumAllHits);
        double[] ggprecision = new double[numRecommendedItems.length];
        double[] ggrecall = new double[numRecommendedItems.length];
        double[] ggf1 = new double[numRecommendedItems.length];
        double[] ggndcg = new double[numRecommendedItems.length];
        double[] goprecision = new double[numRecommendedItems.length];
        double[] gorecall = new double[numRecommendedItems.length];
        double[] gof1 = new double[numRecommendedItems.length];
        double[] gondcg = new double[numRecommendedItems.length];
        double[] ogprecision = new double[numRecommendedItems.length];
        double[] ogrecall = new double[numRecommendedItems.length];
        double[] ogf1 = new double[numRecommendedItems.length];
        double[] ogndcg = new double[numRecommendedItems.length];
        for(int i = 0; i < this.numRecommendedItems.length; i++){
            ggprecision[i] = this.ggSumPrecision[i]/this.numOfTestedTransactions[i];
            ggrecall[i] = this.ggSumRecall[i]/this.numOfTestedTransactions[i];
            ggndcg[i] = this.ggSumNDCG[i]/this.numOfTestedTransactions[i];
            ggf1[i] = computeF1(ggprecision[i], ggrecall[i]);
            goprecision[i] = this.glSumPrecision[i]/this.numOfTestedTransactions[i];
            gorecall[i] = this.grSumRecall[i]/this.numOfTestedTransactions[i];
            gondcg[i] = this.glSumNDCG[i]/this.numOfTestedTransactions[i];
            gof1[i] = computeF1(goprecision[i], gorecall[i]);
            ogprecision[i] = this.grSumPrecision[i]/this.numOfTestedTransactions[i];
            ogrecall[i] = this.grSumRecall[i]/this.numOfTestedTransactions[i];
            ogndcg[i] = this.grSumNDCG[i]/this.numOfTestedTransactions[i];
            ogf1[i] = computeF1(ogprecision[i], ogrecall[i]);
        }
        results.setAllTestedItems(allTestedItems);
        results.setAllTestedTransactions(numOfTestedTransactions);
        results.setF1GGC(ggf1);
        results.setF1GO(gof1);
        results.setF1OG(ogf1);
        results.setMaxRecommendedItems(maxRecommendedItems);
        results.setNdcgGGC(ggndcg);
        results.setNdcgGO(gondcg);
        results.setNdcgOG(ogndcg);
        results.setPrecisionGGC(ggprecision);
        results.setPrecisionGO(goprecision);
        results.setPrecisionOG(ogprecision);
        results.setRealRecommendedGGC(ggSumRealRecommended);
        results.setRealRecommendedGO(glSumRealRecommended);
        results.setRealRecommendedOG(grSumRealRecommended);
        results.setRecallGGC(ggrecall);
        results.setRecallGO(gorecall);
        results.setRecallOG(ogrecall);
        try {
            if(writer != null)
                writer.close();
            writer = null;
        } catch (IOException ex) {
            Logger.getLogger(RecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }
    
    private double computeF1(double p, double r) {
        double result;
        result = 2*(p*r)/(p+r);
        return result;
    }
    
    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void addResult(Example testInst, double[] recommendations, int windowSize, int numberOfRecommendedItems, double transsec) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public EvaluationConfiguration getConfiguration() {
        return config;
    }

   

    
}
