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
    
    private List<Integer> recommendations = new ArrayList<>();
    private List<Integer> testWindow = new ArrayList<>();
    private Integer numOfRecommendedItems = null;
      
    public List<Integer> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Integer> recommendations) {
        this.recommendations = recommendations;
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

}
