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

public abstract class Kernel{
	protected static double dotProd(svm_node[] v1, svm_node[] v2){
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
	
	public abstract double eval(Object o1, Object o2);

}


