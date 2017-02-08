/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.dto;

import java.util.List;

/**
 *
 * @author Tomas Chovanak
 */
public class SnapshotResults {
    
    List<GroupStatsResults> groupStats;
    private int id = 0;

    public List<GroupStatsResults> getGroupStats() {
        return groupStats;
    }

    public void setGroupStats(List<GroupStatsResults> groupStats) {
        this.groupStats = groupStats;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
}
