/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import moa.core.PPSDM.enums.SortStrategiesEnum;
import moa.core.PPSDM.enums.RecommendStrategiesEnum;
import moa.core.PPSDM.Configuration;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.PPSDMRecommendationEvaluator;
import moa.core.PPSDM.FciValue;
import moa.core.PPSDM.dto.GroupStatsResults;
import moa.learners.PersonalizedPatternsMiner;
import moa.streams.SessionsFileStream;
import moa.core.PPSDM.dto.RecommendationResults;
import moa.core.PPSDM.dto.SummaryResults;
import moa.core.PPSDM.utils.UtilitiesPPSDM;
import moa.core.PPSDM.dto.SnapshotResults;

/**
 * Task to learn using PPSDM method and log data to REDIS DB. With input parameters defined.
 * Starts new thread performing learning and on demands logs data to 
 * @author Tomas Chovanak
 */
public class LearnAndLogPPSDM {
    
}
    
   

