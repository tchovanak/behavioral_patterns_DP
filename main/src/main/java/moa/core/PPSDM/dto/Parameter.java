/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.dto;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tomas Chovanak
 */
public class Parameter {
    
    private double value;
    private String name;
    private List<Double> possibleValues = new ArrayList<>();
    private double[] boundaries  = new double[3];

    public Parameter(double value,double low,double top,double step) {
        this.value = value;
        boundaries[0] = low;
        boundaries[1] = top;
        boundaries[2] = step;
        // generate possible values list 
        for(double i = boundaries[0]; i <= boundaries[1]; i+= boundaries[2]){  
            possibleValues.add(i);
        }
    }
    
    public Parameter(double value) {
        this.value = value;
    }
    
    public void addPossibleValue(double val){
        possibleValues.add(val);
    }
    
    public double[] getBoundaries(){
        return boundaries;
    }
    
    public List<Double> getPossibleValues(){
        return possibleValues;
    }
    
    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setPossibleValues(List<Double> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Parameter clone() throws CloneNotSupportedException {
        Parameter clone = new Parameter(this.value);
        clone.setPossibleValues(this.possibleValues);
        clone.setName(this.name);
        return clone;
    }
    
}
