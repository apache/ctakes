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
package org.apache.ctakes.assertion.train;

import org.apache.ctakes.assertion.eval.AssertionEvaluation;
import org.apache.ctakes.assertion.util.AssertionConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
/**
 * For each assertion attribute (polarity, conditional, etc), train a model using the data
 * in the training directories for that attribute, and store the model under the models-dir
 * Note that this uses constants within {@link AssertionConst} for the directory names.
 */
public class PolarityCotrainingTrain {
	protected final static String SHARP_TRAIN = AssertionConst.DATA_DIR + "preprocessed_data/sharp/train";
	protected final static String I2B2_TRAIN  = AssertionConst.DATA_DIR + "preprocessed_data/i2b2/train";
	protected final static String MIPACQ_TRAIN = AssertionConst.DATA_DIR + "preprocessed_data/mipacq/train";
	protected final static String NEGEX_TRAIN = AssertionConst.DATA_DIR + "preprocessed_data/negex"; // actually test

	public final static String SHARP_MODEL = "../ctakes-assertion-models/resources/model/sharptrain";
	protected final static String I2B2_MODEL  = "../ctakes-assertion-models/resources/model/i2b2train";
	protected final static String MIPACQ_MODEL  = "../ctakes-assertion-models/resources/model/mipacqtrain";
	protected final static String NEGEX_MODEL  = "../ctakes-assertion-models/resources/model/negextest";
	protected final static String SHARP_I2B2_MODEL = "../ctakes-assertion-models/resources/model/sharptrain+i2b2train";
	protected final static String SHARP_MIPACQ_MODEL = "../ctakes-assertion-models/resources/model/sharptrain+mipacqtrain";
	protected final static String SHARP_NEGEX_MODEL = "../ctakes-assertion-models/resources/model/sharptrain+negextest";
	protected final static String I2B2_MIPACQ_NEGEX_MODEL = "../ctakes-assertion-models/resources/model/i2b2train+mipacqtrain+negextest";
	protected final static String SHARP_I2B2_MIPACQ_MODEL = "../ctakes-assertion-models/resources/model/sharptrain+i2b2train+mipacqtrain";
	protected final static String SHARP_MIPACQ_NEGEX_MODEL = "../ctakes-assertion-models/resources/model/sharptrain+mipacqtrain+negextest";
	protected final static String SHARP_I2B2_NEGEX_MODEL = "../ctakes-assertion-models/resources/model/sharptrain+i2b2train+negextest";
	protected final static String SHARP_I2B2_MIPACQ_NEGEX_MODEL = "../ctakes-assertion-models/resources/model/sharpi2b2mipacqnegex";

	public static void main(String[] args) throws Exception {

		String attribute = "polarity";
		
		HashMap<String,String> trainGrid = new HashMap<String,String>();
		trainGrid.put(SHARP_TRAIN, 	SHARP_MODEL);
		trainGrid.put(I2B2_TRAIN, 	I2B2_MODEL);
		trainGrid.put(MIPACQ_TRAIN,	MIPACQ_MODEL);
		trainGrid.put(NEGEX_TRAIN,	NEGEX_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+I2B2_TRAIN,	SHARP_I2B2_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+MIPACQ_TRAIN,	SHARP_MIPACQ_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+NEGEX_TRAIN,	SHARP_NEGEX_MODEL);
		trainGrid.put(I2B2_TRAIN+":"+MIPACQ_TRAIN+":"+NEGEX_TRAIN,	I2B2_MIPACQ_NEGEX_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+I2B2_TRAIN+":"+MIPACQ_TRAIN,	SHARP_I2B2_MIPACQ_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+MIPACQ_TRAIN+":"+NEGEX_TRAIN,	SHARP_MIPACQ_NEGEX_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+I2B2_TRAIN+":"+NEGEX_TRAIN,	SHARP_I2B2_NEGEX_MODEL);
		trainGrid.put(SHARP_TRAIN+":"+I2B2_TRAIN+":"+MIPACQ_TRAIN+":"+NEGEX_TRAIN,	
				SHARP_I2B2_MIPACQ_NEGEX_MODEL);

		for (Entry<String, String> oneTrain : trainGrid.entrySet()) {
			
			ArrayList<String> params = new ArrayList<String>();

			params.add("--train-dir"); 	params.add(oneTrain.getKey());
			params.add("--models-dir"); params.add(oneTrain.getValue());
			params.add("--train-only"); 
			
			// Build up an "ignore" string
			for (String ignoreAttribute : AssertionConst.allAnnotationTypes) {
				if (!ignoreAttribute.equals(attribute)) { 

					if (ignoreAttribute.equals("historyOf")) {
						ignoreAttribute = ignoreAttribute.substring(0, ignoreAttribute.length()-2);
					}

					params.add("--ignore-" + ignoreAttribute);
				}
			}
			String[] paramList = params.toArray(new String[]{});
			
			// Run the actual assertion training on just one attribute
			AssertionEvaluation.main( paramList );
		}
		
		
		
	}
}
