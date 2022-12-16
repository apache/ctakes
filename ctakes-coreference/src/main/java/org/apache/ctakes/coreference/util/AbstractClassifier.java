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
package org.apache.ctakes.coreference.util;

import java.io.File;
import java.io.IOException;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceAccessException;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;

public class AbstractClassifier {

	private svm_model svmCls = null;
	private int clsIndex = -1;

	public AbstractClassifier(File fn, int len) {
		try{
			svmCls = svm.svm_load_model(fn.getAbsolutePath());
			int[] labels = new int[2];
			svm.svm_get_labels(svmCls, labels);
			clsIndex = labels[0]==1 ? 0 : 1;
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public double predict(svm_node[] vec){
		return predict(vec,null);
	}

	public double predict(svm_node[] vec, TreebankNode path){
		double[] probs = new double[2];
		svm.svm_predict_probability(svmCls, vec, probs);
		return probs[clsIndex];
	}
}
