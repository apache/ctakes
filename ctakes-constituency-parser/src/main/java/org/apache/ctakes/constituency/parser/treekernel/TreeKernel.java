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
package org.apache.ctakes.constituency.parser.treekernel;

import opennlp.tools.parser.Parse;

import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.utils.kernel.Kernel;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TreeKernel extends Kernel {

	public static double lambda = 0.4;
	public static double lambdaSquared = lambda * lambda;
	private ConcurrentHashMap<String,Double> normalizers = new ConcurrentHashMap<String,Double>();
	private int normalHits = 0;
	private boolean normalize = false;
	
	public TreeKernel(){
		this(false);
	}
	
	public TreeKernel(boolean norm){
		normalize = norm;
	}
	
	public double eval(Object o1, Object o2){
		Parse node1 = (Parse) o1;
		Parse node2 = (Parse) o2;
		double norm1=0.0;
		double norm2=0.0;
		if(normalize){
			StringBuffer nodeStr = new StringBuffer();
			node1.show(nodeStr);
			String node1Str = nodeStr.toString();
			if(!normalizers.containsKey(node1Str)){
				double norm = sim(node1, node1);
				System.out.println(node1Str);
//				System.out.println
				normalizers.put(node1Str, norm);
				normalHits++;
			}
			nodeStr = new StringBuffer();
			node2.show(nodeStr);
			String node2Str = nodeStr.toString();
			if(!normalizers.containsKey(node2Str)){
				double norm = sim(node2, node2);
				normalizers.put(node2Str, norm);
				normalHits++;
			}

			norm1 = normalizers.get(node1Str);
			norm2 = normalizers.get(node2Str);
		}
		if(normalize){
			return sim(node1,node2) / Math.sqrt(norm1*norm2);
		}else return sim(node1,node2);
	}

	private double sim(Parse node1, Parse node2){
		double sim = 0.0;
		//			hits++;
		List<Parse> N1 = TreeUtils.getNodeList(node1);
		List<Parse> N2 = TreeUtils.getNodeList(node2);
		for(Parse n1 : N1){
			for(Parse n2 : N2){
				double cm=0.0;
				cm = numCommonSubtrees(n1,n2);
				sim += cm;
			}
		}
		return sim;
	}
	
	private static double numCommonSubtrees(Parse n1, Parse n2){
		double retVal=1.0;
		if(n1.getChildCount() != n2.getChildCount()){
			retVal = 0;
		}else if(!n1.getType().equals(n2.getType())){
			retVal = 0;
		}else if(n1.getChildCount() == 1 && n1.getChildren()[0].getChildCount() == 0 && n2.getChildren()[0].getChildCount() == 0){
			// same productions and are both are preterminals
			retVal = lambdaSquared;
		}else{
			// At this point they have the same label and same # children. Check if children the same.
			boolean sameProd = true;
			for(int i = 0; i < n1.getChildCount(); i++){
				String l1 = n1.getChildren()[i].getType();
				String l2 = n2.getChildren()[i].getType();
				if(!l1.equals(l2)){
					sameProd = false;
					break;
				}
			}
			if(sameProd == true){
				// common = 1;
				for(int i = 0; i < n1.getChildCount(); i++){
					retVal *= (1 + numCommonSubtrees(n1.getChildren()[i], n2.getChildren()[i])); 
				}
				retVal = lambdaSquared * retVal;
			}else{
				retVal = 0;
			}
		}
		return retVal;
	}
}