/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tomas
 */
public class RecommendationResults {
    
    private List<Integer> recommendationsGG = new ArrayList<>();
    private List<Integer> recommendationsGL = new ArrayList<>();
    private List<Integer> recommendationsGR = new ArrayList<>();
    private List<Integer> testWindow = new ArrayList<>();
    private Integer numOfRecommendedItems = null;
        private int groupid = -1;

    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }
    
    public List<Integer> getRecommendationsGG() {
        return recommendationsGG;
    }

    public void setRecommendationsGG(List<Integer> recommendationsGG) {
        this.recommendationsGG = recommendationsGG;
    }

    public List<Integer> getRecommendationsGL() {
        return recommendationsGL;
    }

    public void setRecommendationsGL(List<Integer> recommendationsGL) {
        this.recommendationsGL = recommendationsGL;
    }

    public List<Integer> getRecommendationsGR() {
        return recommendationsGR;
    }

    public void setRecommendationsGR(List<Integer> recommendationsGR){
        this.recommendationsGR = recommendationsGR;
    }

    public List<Integer> getTestWindow() {
        return testWindow;
    }

    public void setTestWindow(List<Integer> testWindow) {
        this.testWindow = testWindow;
    }

    public Integer getNumOfRecommendedItems() {
        return numOfRecommendedItems;
    }

    public void setNumOfRecommendedItems(Integer numOfRecommendedItems) {
        this.numOfRecommendedItems = numOfRecommendedItems;
    }

    public List<Integer> getFirstNRecommendationsGGC(int numRec) {
       if(this.recommendationsGG.size() >= numRec){
            return this.recommendationsGG.subList(0, numRec); 
        }else{
            List<Integer> res = this.recommendationsGG.subList(0, this.recommendationsGG.size()); 
            return res;
        }
    }

    public List<Integer> getFirstNRecommendationsGL(int numRec) {
        if(this.recommendationsGL.size() >= numRec){
           return this.recommendationsGL.subList(0, numRec); 
        }else{
            List<Integer> res = this.recommendationsGL.subList(0, this.recommendationsGL.size()); 
            return res;        
        }
    }
    
    public List<Integer> getFirstNRecommendationsGR(int numRec) {
        if(this.recommendationsGR.size() >= numRec){
            return this.recommendationsGR.subList(0, numRec); 
        }else{
            List<Integer> res = this.recommendationsGR.subList(0, this.recommendationsGR.size()); 
            return res;
        }
    }
    
}
