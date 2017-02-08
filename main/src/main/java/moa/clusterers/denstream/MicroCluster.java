/*
 *    MicroCluster.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Wels (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.clusterers.denstream;

import moa.cluster.CFCluster;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import moa.core.PPSDM.Configuration;
import moa.core.enums.DistanceMetricsEnum;
import moa.core.utils.UtilitiesPPSDM;

public class MicroCluster extends CFCluster {

    
    class InstanceInside{
        private Instance inside;
        private long timestamp;

        public InstanceInside(Instance inside, long timestamp) {
            this.inside = inside;
            this.timestamp = timestamp;
        } 

        public Instance getInside() {
            return inside;
        }

        public void setInside(Instance inside) {
            this.inside = inside;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
    }
    
    private long lastEditT = -1;
    private long creationTimestamp = -1;
    private double lambda;
    private Timestamp currentTimestamp;
    private BlockingQueue<InstanceInside> instances = 
            new ArrayBlockingQueue<>(100); // queue of last n user sessions

    
    public MicroCluster(double[] center, int dimensions, long creationTimestamp, 
            double lambda, Timestamp currentTimestamp) {
        super(center, dimensions);
        this.creationTimestamp = creationTimestamp;
        this.lastEditT = creationTimestamp;
        this.lambda = lambda;
        this.currentTimestamp = currentTimestamp;
        this.radiusFactor = 1.8;
    }

    public MicroCluster(Instance instance, int dimensions, long timestamp, double lambda, Timestamp currentTimestamp) {
        this(instance.toDoubleArray(), dimensions, timestamp, lambda, currentTimestamp);
    }

    public void insert(Instance instance, long timestamp) {
        N++;
        super.setWeight(super.getWeight() + 1);
        this.lastEditT = timestamp;
        for (int i = 0; i < instance.numValues(); i++) {
            LS[i] += instance.value(i);
            SS[i] += instance.value(i) * instance.value(i);
        }
        
        /*
            Change to really contain inserted instances
        */
        InstanceInside ins = new InstanceInside(instance,timestamp);
        if(!instances.offer(ins)){
            instances.poll();
            instances.offer(ins);
        }
    }

    public long getLastEditTimestamp() {
        return lastEditT;
    }

    private double[] calcCF2(long timestamp) {
//        double[] cf2 = new double[SS.length];
//        for (int i = 0; i < SS.length; i++) {
//            cf2[i] = Math.pow(2, -lambda * dt) * SS[i];
//        }
//        return cf2;
//        
        double[] cf2 = new double[SS.length];
        for (int i = 0; i < SS.length; i++) {
            cf2[i] = 0.0;
        }
        for(InstanceInside point : instances){
            Instance inst  = point.getInside();
            long dt = timestamp - point.getTimestamp();
            for (int i = 0; i < inst.numValues(); i++) {
                cf2[inst.index(i)] =  Math.pow(2, -lambda * dt) * inst.value(i);
            }
        }
        return cf2;
        
    }

    private double[] calcCF1(long timestamp) {
//        double[] cf1 = new double[LS.length];
//        for (int i = 0; i < LS.length; i++) {
//            cf1[i] = Math.pow(2, -lambda * dt) * LS[i];
//        }
//        return cf1;
        
        double[] cf1 = new double[SS.length];
        for (int i = 0; i < SS.length; i++) {
            cf1[i] = 0.0;
        }
        for(InstanceInside point : instances){
            Instance inst  = point.getInside();
            long dt = timestamp - point.getTimestamp();
            for (int i = 0; i < inst.numValues(); i++) {
                cf1[inst.index(i)] +=  Math.pow(2, -lambda * dt) * inst.value(inst.index(i)) * inst.value(inst.index(i));
            }
        }
        return cf1;
    }

    @Override
    public double getWeight() {
        return getWeight(currentTimestamp.getTimestamp());
    }

    private double getWeight(long timestamp) {
        long dt = timestamp - lastEditT;
        return (N * Math.pow(2, -lambda * dt));
    }

    public long getCreationTime() {
        return creationTimestamp;
    }

    @Override
    public double[] getCenter() {
        //return getCenter(currentTimestamp.getTimestamp());
        double[] cf1 = calcCF1(currentTimestamp.getTimestamp());
        double w = getWeight();
        double[] center = new double[cf1.length];
        for(int i = 0; i < center.length; i++){
            center[i] = cf1[i]/w;
        }
        return center;
    }

    private double[] getCenter(long timestamp) {
        long dt = timestamp - lastEditT;
        double w = getWeight(timestamp);
        double[] res = new double[LS.length];
        for (int i = 0; i < LS.length; i++) {
            res[i] = LS[i];
            res[i] *= Math.pow(2, -lambda * dt);
            res[i] /= w;
        }
        return res;
    }
    
    private double[] getCenterSqrt(long timestamp) {
        long dt = timestamp - lastEditT;
        double w = getWeight(timestamp);
        double[] res = new double[SS.length];
        for (int i = 0; i < SS.length; i++) {
            res[i] = SS[i];
            res[i] *= Math.pow(2, -lambda * dt);
            res[i] /= w;
        }
        return res;
    }

    @Override
    public double getRadius() {
        return getRadius(currentTimestamp.getTimestamp())*radiusFactor;
    }

    public double getRadius(long timestamp) {
        // AS AVERAGE COSINE DISTANCE 
        double[] cf1 = calcCF1(timestamp);
        double w = getWeight();
        double[] center = new double[cf1.length];
        for(int i = 0; i < center.length; i++){
            center[i] = cf1[i]/w;
        }
        
        double sumDist = 0.0;
        for(InstanceInside inst: instances){
            sumDist += UtilitiesPPSDM.distanceBetweenVectors(inst.getInside().toDoubleArray(), center);
        }
        return sumDist/instances.size();
        
//        double[] cf2w = getCenterSqrt(timestamp);
//        double[] cf1w = getCenter(timestamp);
//        double w = getWeight(timestamp);
//        double max = 0;
//        double sum = 0;
//        double sumx1 = 0;
//        double sumx2 = 0;
//        //double dist = UtilitiesPPSDM.distanceBetweenVectors(cf2w, cf1w);
//        for (int i = 0; i < SS.length; i++) {
////            sumx1 += cf2[i];
////            sumx2 += cf1[i];
//            double x1 = cf2[i]/w;
//            double x2 = Math.pow(cf1[i]/w, 2);
//            sum += (x1 - x2);
////            if (Math.sqrt(x1 - x2) > max) {
////                max = Math.sqrt(x1 - x2);
////            }
//        }
////        double r = Math.sqrt(sumx1/w - Math.pow(sumx2/w,2));
////        double r = Math.sqrt(getCenterSqrt(timestamp),Math.pow(getCenter(timestamp),2));
//        double r = Math.sqrt(sum);
//        max = max * radiusFactor;
       // return r;
    }

    @Override
    public MicroCluster copy() {
        MicroCluster copy = new MicroCluster(this.LS.clone(), this.LS.length, this.getCreationTime(), this.lambda, this.currentTimestamp);
        copy.setWeight(this.N + 1);
        copy.N = this.N;
        for(InstanceInside i : instances){
           copy.instances.offer(i);
        }
        copy.SS = this.SS.clone();
        copy.LS = this.LS.clone();
        copy.lastEditT = this.lastEditT;
        return copy;
    }

    @Override
    public double getInclusionProbability(Instance instance) {
        double[] center = getCenter();
        double[] items = instance.toDoubleArray();
        double d =  UtilitiesPPSDM.distanceBetweenVectors(center,items);
        double radius = getRadius();///Configuration.MAX_USER_SESSIONS_HISTORY_IN_USER_MODEL;
        if(d <= radius*1.0){
            return 1.0;
        }else{
            return 0.0;
        }
    }
    
    
    public double getDistanceToCenter(Instance instance) {
        double[] center = getCenter();
        double[] items = instance.toDoubleArray();
        double d =  UtilitiesPPSDM.distanceBetweenVectors(center,items);
        return d;
    }

    @Override
    public CFCluster getCF(){
        CFCluster cf = copy();
        double w = getWeight();
        cf.setN(w);
        return cf;
    }
}
