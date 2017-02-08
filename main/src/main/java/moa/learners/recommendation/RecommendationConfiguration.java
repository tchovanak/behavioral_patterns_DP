/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners.recommendation;

import moa.core.enums.RecommendStrategiesEnum;

/**
 *
 * @author Tomas
 */
public interface RecommendationConfiguration {
    
    public void setEWS(int ews);
    public int getEWS();
    public int getRC();
    public void setRC(int rc);
    public void setRecommendationStrategy(RecommendStrategiesEnum strat);
    public RecommendStrategiesEnum getRecommendationStrategy();
}
