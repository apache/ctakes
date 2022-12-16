/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.temporal.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

/**
 * A simple implementation of SMOTE algorithm. 
 * Nitesh V. Shawla et. al. SMOTE: Synthetic Minority Over-sampling Technique, 06/02
 * Find K nearest neighbor for each minority instance.
 *  
 * @author Chen Lin
 * @DCT: 12/28/2012
 * Modified on 1/4/2013
 */
public class SMOTEplus {

	protected List<Instance<String>> minorityInsts;
	protected Table<Instance<String>, String, Integer> instanceFeatureCount;
	protected Table<Instance<String>, Instance<String>, Double> interInstanceDistance;
	protected List<Instance<String>> syntheticInsts;
	protected final int numOfNearestNeighbors;
	
	public SMOTEplus(int numNeighbors) {
		this.minorityInsts 	= Lists.newArrayList();
		this.syntheticInsts = Lists.newArrayList();
		this.instanceFeatureCount 	= HashBasedTable.<Instance<String>, String, Integer> create();
		this.interInstanceDistance	= HashBasedTable.<Instance<String>, Instance<String>, Double> create();
		this.numOfNearestNeighbors	= numNeighbors;
	}
	
	public Iterable<Instance<String>> populateMinorityClass() {
		//1. populate Minority instance-Feature matrix
		for (Instance<String> instance : this.minorityInsts) {
		      for (Feature feature : instance.getFeatures()) {
		    	  this.instanceFeatureCount.put(instance, getFeatureName(feature), 1);
		      }
		}
		
		//2. Iterate through all minority instances:
		for (Instance<String> aMinorityInst : this.minorityInsts) {
			//3. find its nearest neighbor minority instance:
			List<Object[]> distToMe = new LinkedList<Object[]>();
			for ( Instance<String> bInst : this.instanceFeatureCount.rowKeySet()){
				double distance = calculateDistance(aMinorityInst, bInst);
				distToMe.add(new Object[] {distance, bInst});
			}
			
			//sort list and find nearest neighbors:
			Collections.sort(distToMe, new Comparator<Object>(){
				public int compare(Object o1, Object o2){
					double dist1 = (Double) ((Object[])o1)[0];
					double dist2 = (Double) ((Object[])o2)[0];
					return (int) Math.ceil(dist1 - dist2);
				}
			});
			
			//populate the nearest neighbor, create synthetic data:
			Iterator<Object[]> neighborIter = distToMe.iterator();
			int idx = 0;
			while( neighborIter.hasNext() && idx < this.numOfNearestNeighbors){
				@SuppressWarnings("unchecked")
				Instance<String> nearestNeighbor = ((Instance<String>) neighborIter.next()[1]);
				Instance<String> sytheticInst = generateInstance(aMinorityInst, nearestNeighbor);
				this.syntheticInsts.add(sytheticInst);
				idx ++;
			}
		}
		
		return this.syntheticInsts;
	}

	private Instance<String> generateInstance(Instance<String> aMinorityInst,
			Instance<String> nearestNeighbor) {
		List<Feature> features = new ArrayList<Feature>();
		//iterate through all features:
		for(Feature feature: aMinorityInst.getFeatures()){
			String featureName = getFeatureName(feature);
			Integer valB = this.instanceFeatureCount.get(nearestNeighbor, featureName);
			if(valB != null){
				features.add(feature);
			}
		}
		String outcome = nearestNeighbor.getOutcome();
		Instance<String> syntheticInst = new Instance<String>(outcome, features);
		return syntheticInst;
	}

	private double calculateDistance(Instance<String> instA,
			Instance<String> instB) {
		double distance = 0;
		Double dis1 = this.interInstanceDistance.get(instA, instB);
		Double dis2 = this.interInstanceDistance.get(instB, instA);

		if (dis1 == null && dis2 == null){ //if this pair's distance hasn't been calculated, then calculate it.
			//iterate through all features:
			for(Feature feature: instA.getFeatures()){
				String featureName = getFeatureName(feature);
				Integer valB = this.instanceFeatureCount.get(instB, featureName);
				if ( valB == null ){
					distance ++;
				}
			}
			distance = Math.pow(distance, .5);
			this.interInstanceDistance.put(instA, instB, distance);
		}else{
			distance = dis1 == null?  dis2 : dis1;
		}

		return distance;
	}

	public String getFeatureName(Feature feature) {
	    String featureName = feature.getName();
	    Object featureValue = feature.getValue();
	    //return featureValue instanceof Number ? featureName : featureName + ":" + featureValue;
	    return featureName + ":" + featureValue;
	  }

	public void addInstance(Instance<String> minorityInst) {
		this.minorityInsts.add(minorityInst);
	}
}
