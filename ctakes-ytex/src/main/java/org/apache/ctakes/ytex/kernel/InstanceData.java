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
package org.apache.ctakes.ytex.kernel;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * data structure to store instance ids, their classes, folds, runs, and labels.
 * 
 * @author vijay
 * 
 */
public class InstanceData {
	/**
	 * labels - class
	 */
	SortedMap<String, SortedSet<String>> labelToClassMap = new TreeMap<String, SortedSet<String>>();
	/**
	 * map of labels - runs - folds - train/test - instances - class for test
	 * instances
	 */
	SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> labelToInstanceMap = new TreeMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>>();

	public SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> getLabelToInstanceMap() {
		return labelToInstanceMap;
	}

	public void setLabelToInstanceMap(
			SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> labelToInstanceMap) {
		this.labelToInstanceMap = labelToInstanceMap;
	}

	public SortedMap<String, SortedSet<String>> getLabelToClassMap() {
		return labelToClassMap;
	}

	public void setLabelToClassMap(
			SortedMap<String, SortedSet<String>> labelToClassMap) {
		this.labelToClassMap = labelToClassMap;
	}

	/**
	 * get all the instance ids for the specified scope
	 * 
	 * @param label
	 *            if null, then all instance ids, else if run & fold = 0, then
	 *            all instance ids for this label.
	 * @param run
	 * @param fold
	 *            if run & fold != 0, then all instance ids for the specified
	 *            fold
	 * @return
	 */
	public SortedSet<Long> getAllInstanceIds(String label, int run, int fold) {
		SortedSet<Long> instanceIds = new TreeSet<Long>();
		if (label == null) {
			for (String labelKey : this.getLabelToInstanceMap().keySet()) {
				instanceIds.addAll(getAllInstanceIds(labelKey, 0, 0));
			}
		} else if (label != null && fold == 0 && run == 0) {
			for (int runKey : this.getLabelToInstanceMap().get(label).keySet()) {
				for (int foldKey : this.getLabelToInstanceMap().get(label)
						.get(runKey).keySet()) {
					for (SortedMap<Long, String> inst : this
							.getLabelToInstanceMap().get(label).get(runKey)
							.get(foldKey).values()) {
						instanceIds.addAll(inst.keySet());
					}
				}
			}
		}
		if (fold != 0 && run != 0) {
			for (SortedMap<Long, String> foldInst : this
					.getLabelToInstanceMap().get(label).get(run).get(fold)
					.values()) {
				instanceIds.addAll(foldInst.keySet());
			}
		}
		return instanceIds;
	}
}
