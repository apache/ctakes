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
package org.apache.ctakes.utils.kernel;

import java.util.concurrent.ConcurrentHashMap;

//import opennlp.tools.parser.Parse;
import libsvm.svm_node;

public class PolyKernel extends Kernel{

	private int degree;
	private double coef;
	private boolean norm;
	private ConcurrentHashMap<Object,Double> normalizers = new ConcurrentHashMap<Object,Double>();

	public PolyKernel(int degree, double coef, boolean norm){
		this.degree = degree;
		this.coef = coef;
		this.norm = norm;
	}

	public PolyKernel(){
		this.degree = 1;
		this.coef = 0.0;
		this.norm = false;
	}

//	public double eval(svm_node[] v1, svm_node[] v2){
	public double eval(Object o1, Object o2){
		svm_node[] v1 = (svm_node[]) o1;
		svm_node[] v2 = (svm_node[]) o2;
		
		if(norm){
			double norm1, norm2;
			if(!normalizers.containsKey(o1)){
				norm1 = sim(v1, v1);
				normalizers.put(o1, norm1);
			}else norm1 = normalizers.get(o1);
			if(!normalizers.containsKey(o2)){
				norm2 = sim(v2,v2);
				normalizers.put(o2,norm2);
			}else norm2 = normalizers.get(o2);
			return sim(v1,v2) / Math.sqrt(norm1*norm2);
		}else{
			return sim(v1,v2);
		}
		
	}

	private final double sim(svm_node[] v1, svm_node[] v2){
		double sim = dotProd(v1, v2) + coef;
		if(degree != 1){
			sim = Math.pow(sim, degree);
		}
		return sim; 
	}
/*jjj
	private static double dotProd(svm_node[] v1, svm_node[] v2){
		double sim = 0.0;
		int i = 0;
		int j = 0;
		
		while(i < v1.length && j < v2.length){
			if(v1[i].index == v2[j].index){
				sim += (v1[i].value * v2[j].value);
				i++;
				j++;
			}else if(v1[i].index < v2[j].index){
				i++;
			}else if(v1[i].index > v2[j].index){
				j++;
			}else{
				System.err.println("Don't know how this is possible!");
			}
		}
		return sim;
	}
*/
}
