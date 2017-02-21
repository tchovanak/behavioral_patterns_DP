/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.clustering;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import moa.cluster.Clustering;
import moa.clusterers.Clusterer;
import moa.core.dto.GroupStatsResults;

/**
 * This interface is common for all different clustering components of PPSDM method.
 * @author Tomas Chovanak
 */
public abstract class ClusteringComponent {
    
    protected Map<Integer, UserModel> usermodels;
    private ClusteringComponentConfiguration config;
    
    
    public ClusteringComponent(ClusteringComponentConfiguration config) {
        this.usermodels = new ConcurrentHashMap<>();
        usermodels.clear();
        this.config = config;
    }
    
    public void resetLearning() {
        this.usermodels.clear();
    }
    
    abstract public Clusterer getClusterer();
    abstract public Clustering getClustering();
    abstract public void performMacroclustering();
    abstract public boolean isClustering();
    abstract public int getClusteringID();
    abstract public void trainOnInstance(Instance umInstance);
    abstract public void updateGroupingInUserModel(UserModel um);
    
    public Map<Integer,UserModel>  getUserModels(){
        return usermodels;
    }
    
    public List<GroupStatsResults> extractUserModels(){
        List<GroupStatsResults> results = new ArrayList<>();
        GroupStatsResults res = new GroupStatsResults();
        res.setGroupid(-1);
        res.setClusteringId(this.getClusteringID());
        results.add(res);
        if(this.isClustering()){
            for(int i = 0; i < this.getClustering().size(); i++){
                res = new GroupStatsResults();
                res.setGroupid(i);
                res.setClusteringId(this.getClusteringID());
                res.setCentre(this.getClustering().get(i).getCenter());
                results.add(res);
                //res.setUids();
            }
        }
        for(UserModel um : this.usermodels.values()){
            if(this.getClusteringID() - um.getClusteringId() <= 1){
                results.get((int)um.getGroupid() + 1).addUid(um.getId());
            }else{
                results.get(0).addUid(um.getId());
            }
        }
        return results;
    }
    
    
    public UserModel updateUserModel(Instance inst, Map<Integer, Integer> catsToSupercats,
            Integer numMinNumberOfChangesInUserModel) {
        
        int uid = (int)inst.value(1);
        if(usermodels.containsKey(uid)){
            UserModel um = usermodels.get(uid);
            if(catsToSupercats != null && catsToSupercats.size() > 0){
                um.updateWithInstance(inst,catsToSupercats);
            }else{
                um.updateWithInstance(inst);
            }
            return um;
        }else{
            UserModel um = new UserModel((int)inst.value(1), 
                    numMinNumberOfChangesInUserModel);
            if(catsToSupercats != null && catsToSupercats.size() > 0){
                um.updateWithInstance(inst,catsToSupercats);
            }else{
                um.updateWithInstance(inst);
            }
            usermodels.put(uid, um);
            return um;
        }
    }
    
    public UserModel getUserModelFromInstance(Instance inst) {
        int uid = (int)inst.value(1);
         if(usermodels.containsKey(uid)){
            UserModel um = usermodels.get(uid);
            return um;
        }else{
            return null;
        }
    }
    
    /**
     * Removes old user models - if it has clustering id older than minimal user model updates
     */
    public void clearUserModels() {
        for(Map.Entry<Integer, UserModel> entry : this.usermodels.entrySet()){
            UserModel model = entry.getValue();
            if(getClusteringID() - model.getClusteringId() > config.getTcdiff()){
                // delete 
                this.usermodels.remove(entry.getKey());
            }
        }
    }

    
}
