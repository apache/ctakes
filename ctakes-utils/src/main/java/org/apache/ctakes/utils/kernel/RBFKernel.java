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

import libsvm.svm_node;

public class RBFKernel extends Kernel {

	double gamma = 0.0;

	public RBFKernel(double gamma){
		this.gamma = gamma;
	}

	public double eval(Object o1, Object o2){
		svm_node[] v1 = (svm_node[]) o1;
		svm_node[] v2 = (svm_node[]) o2;

		// TODO implement!	
		double squareNorm = 0;
		int i=0;
		int j=0;
		
		while(i < v1.length || j < v2.length){
			if(i < v1.length && j < v2.length && v1[i].index == v2[j].index){
				// return Math.exp(-gamma*(x_square[i]+x_square[j]-2*dot(x[i],x[j])));
				squareNorm += Math.pow(v1[i].value - v2[j].value, 2);
				i++;
				j++;
			}else if((i < v1.length && j == v2.length) || (i < v1.length && v1[i].index < v2[j].index)){
				squareNorm += Math.pow(v1[i].value, 2);
				i++;
			}else{
				squareNorm += Math.pow(v2[j].value, 2);
				j++;
			}
		}

		double sim = Math.exp(-gamma * squareNorm);
		return sim;
	}
}
