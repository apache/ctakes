/*
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
package org.apache.ctakes.assertion.pipelines;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/*
 * See here for training/dev/test split information:
 * http://informatics.mayo.edu/sharp/index.php/Annotation#Training.2FDevelopment.2FTest_Split
 */

public class SharpCorpusSplit {
	public enum Subcorpus { TRAIN, TEST, DEV, CROSSVAL }
	
	private static Map<String,Subcorpus> map = new HashMap<String,Subcorpus>();
	static {
		map.put("ss1_batch02", Subcorpus.TRAIN); 
		map.put("ss1_batch03", Subcorpus.TRAIN); 
		map.put("ss1_batch04", Subcorpus.TRAIN); 
		map.put("ss1_batch05", Subcorpus.TRAIN); 
		map.put("ss1_batch06", Subcorpus.TRAIN); 
		map.put("ss1_batch07", Subcorpus.TRAIN); 
		map.put("ss1_batch08", Subcorpus.TRAIN); 
		map.put("ss1_batch09", Subcorpus.TRAIN); 
		map.put("ss1_batch10", Subcorpus.DEV); 
		map.put("ss1_batch11", Subcorpus.TEST); 
		map.put("ss1_batch12", Subcorpus.TEST); 
		map.put("ss1_batch13", Subcorpus.TRAIN); 
		map.put("ss1_batch14", Subcorpus.TRAIN); 
		map.put("ss1_batch15", Subcorpus.TRAIN); 
		map.put("ss1_batch16", Subcorpus.TRAIN); 
		map.put("ss1_batch17", Subcorpus.DEV); 
		map.put("ss1_batch18", Subcorpus.TRAIN); 
		map.put("ss1_batch19", Subcorpus.TRAIN); 
	}
	
	public static Subcorpus splitSeed( File directory ) {
//		if (map.containsKey(directory.getName())) {
////			System.out.println(directory.toString());
//			return map.get(directory.getName());
//		}
//		return Subcorpus.TRAIN;
		int batchNum = Integer.parseInt(directory.getName());
		if(batchNum == 10 || batchNum == 17) return Subcorpus.DEV;
		else if(batchNum == 11 || batchNum == 12) return Subcorpus.TEST;
		else return Subcorpus.TRAIN;
	}
	
	public static Subcorpus splitStratified(int batchNum){
	  if(batchNum % 5 < 3) return Subcorpus.TRAIN;
	  else if(batchNum % 5 == 3) return Subcorpus.DEV;
	  else return Subcorpus.TEST;
	}
	
	public static Subcorpus splitStratified(File file){
	  return splitStratified(Integer.parseInt(file.getName()));
	}
}
