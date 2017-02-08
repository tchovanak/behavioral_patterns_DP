/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.clustering;

/**
 *
 * @author Tomas
 */
public interface ClustreamConfiguration {

    public int getGC();

    public int getMaxNumKernels();

    public int getKernelRadiFactor();
    
    public void setGC(int gc);
    
    public void setMaxNumKernels(int gc);
    
    public void setKernelRadiFactor(int gc);
}
