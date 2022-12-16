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
package org.apache.ctakes.assertion.util;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class AssertionConst {
	
	/*** CHANGE THESE ***/

	// Locally-stored data models
	
	// Note that on Windows, by default, /sharp-home/assertion/ the same as C:/sharp-home/assertion/
	public static final String BASE_DIRECTORY = "/projects/data/assertion/"; // "/usr/data/work/data/assertion/";// "/Users/m081914/"; // "/usr/data/work/data/assertion/";  // "/usr/data";
	static {
		if (!BASE_DIRECTORY.endsWith("/") && !BASE_DIRECTORY.endsWith("\\")) {
			throw new RuntimeException("BASE_DIRECTORY should end with a slash");
		}
	}

	// raw and processed text, expects subdirectories for different sources, then subsubdirectories for train/test/dev
	public static final String DATA_DIR = BASE_DIRECTORY + "data/"; // + "work/data/assertion/";
	

	// expects subdirectories: "Mayo/UMLS_CEM/*batch*/Knowtator*" "Seattle Group Health/UMLS_CEM/*batch*/Knowtator*"
	public static final String SHARP_SEED_CORPUS = DATA_DIR + "gold_standard/sharp/Seed Corpus/";
	// expects subdirectories: ast, txt 
	public static final String I2B2_2010_CORPUS = DATA_DIR + "gold_standard/i2b2Challenge2010/Data/i2b2Challenge2010AllTrain/";
	// expects subdirectories: ast, txt
	public static final String I2B2_2010_TEST_CORPUS = DATA_DIR + "gold_standard/i2b2Challenge2010/Data/Test/reports/";

	// expects subdirectories called exported-xml and text
	public static final String MiPACQ_CORPUS = DATA_DIR + "gold_standard/mipacq/";

	public static final String NEGEX_CORPUS = DATA_DIR + "gold_standard/negex/Annotations-1-120-random.txt"; 
	public static final String NEGEX_CORPUS_PREPROCESSED = DATA_DIR + "preprocessed_data/negex/all"; 
	
	// Just plaintext files, which will be run through cTAKES, to generate XMI - attributes will then be judged
	// This in input for cTAKES; the output (evalOutputDir) can then be the input of the judge step.
	public static final String CORPUS_WO_GOLD_STD_TO_RUN_THROUGH_CTAKES = DATA_DIR + "ActiveLearning/plaintext";


	// specify the model to write (train/crossvalidate) or read (test/crossvalidate).
	//  please rename for different configurations of training data 
//	public static String modelDirectory = "../ctakes-assertion-res/resources/model/sharp-sprint-train";
//	public static String modelDirectory = "../ctakes-assertion-res/resources/model/sharptrain-xval-fs";
//	public static String modelDirectory = "../ctakes-assertion-res/resources/model/sharptrain";
//	public static String modelDirectory = "../ctakes-assertion-res/resources/model/sharptrain+i2b2train";
	public static String modelDirectory = "../ctakes-assertion-res/resources/model/mipacqtrain";
//	public static String modelDirectory = "../ctakes-assertion-res/resources/model/negextest";

	
	// Specify training directories for each attribute in a (semi)colon-separated list, e.g., "preprocessed_data/dev:preprocessed_data/train"
	public static HashMap<String,String> trainingDirectories = new HashMap<String,String>();
	static { 
//		trainingDirectories.put("polarity","sharp_data/train");
//		trainingDirectories.put("polarity", DATA_DIR + "preprocessed_data/i2b2/train");
		trainingDirectories.put("polarity", DATA_DIR + "preprocessed_data/mipacq/train");
//		trainingDirectories.put("polarity", DATA_DIR +  "preprocessed_data/sharp/train");
//		trainingDirectories.put("polarity", DATA_DIR +  "preprocessed_data/sharp/train"+":"
//				+DATA_DIR+"preprocessed_data/i2b2/train");
		trainingDirectories.put("conditional", DATA_DIR +  "preprocessed_data/sharp/train");
		trainingDirectories.put("uncertainty", DATA_DIR +  "preprocessed_data/sharp/train");
		trainingDirectories.put("subject", DATA_DIR +  "preprocessed_data/sharp/train");
		trainingDirectories.put("generic", DATA_DIR +  "preprocessed_data/sharp/train");
		trainingDirectories.put("historyOf", DATA_DIR +  "preprocessed_data/sharp/train");
	}
		
	public static HashMap<String,String> testDirectories = new HashMap<String,String>();
	static { 
//		testDirectories.put("polarity", DATA_DIR +  "preprocessed_data/i2b2/test");
		testDirectories.put("polarity", DATA_DIR +  "preprocessed_data/sharp/test");
//		testDirectories.put("polarity", DATA_DIR +  "preprocessed_data/sharp/dev");
//		testDirectories.put("conditional", DATA_DIR +  "preprocessed_data/sharp/test");
//		testDirectories.put("uncertainty", DATA_DIR +  "preprocessed_data/sharp/test");
//		testDirectories.put("subject", DATA_DIR +  "preprocessed_data/sharp/test");
//		testDirectories.put("generic", DATA_DIR +  "preprocessed_data/sharp/test");
//		testDirectories.put("historyOf", DATA_DIR +  "preprocessed_data/sharp/test");
	}
		
	// If you don't want to train/cross-validate everything, comment these out
	public static ArrayList<String> annotationTypes = new ArrayList<String>();
	static { 
		annotationTypes.add("polarity");
//		annotationTypes.add("conditional");
//		annotationTypes.add("uncertainty");
//		annotationTypes.add("subject");
//		annotationTypes.add("generic");
//		annotationTypes.add("historyOf");
	}
	
	
	/*** DON'T CHANGE THESE ***/ /* TODO - please comment why these should not be changed */

	// Specify input and output data locations for preprocessing.  Results will be used for model training
	public static HashMap<String,String> preprocessRootDirectory = new HashMap<String,String>();
	static { 
//		preprocessRootDirectory.put(SHARP_SEED_CORPUS + "Mayo/UMLS_CEM", DATA_DIR + "preprocessed_data/sharp/train");
//		preprocessRootDirectory.put(SHARP_SEED_CORPUS + "Seattle Group Health/UMLS_CEM", DATA_DIR + "preprocessed_data/sharp/train");
//		preprocessRootDirectory.put(I2B2_2010_CORPUS, DATA_DIR + "preprocessed_data/i2b2/train");
//		preprocessRootDirectory.put(I2B2_2010_TEST_CORPUS, DATA_DIR + "preprocessed_data/i2b2/test");
//		preprocessRootDirectory.put(MiPACQ_CORPUS, DATA_DIR + "preprocessed_data/mipacq/train");
		preprocessRootDirectory.put(DATA_DIR + "gold_standard/negex", DATA_DIR + "preprocessed_data/negex");
		
		// If one of the preprocessRootDirectory entries above is commented out, warn user with a popup
//		if (preprocessRootDirectory.keySet().size()<4) {
//			JFrame frame = new JFrame("DialogDemo");
//			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//			//Create and set up the content pane.
//			JOptionPane.showMessageDialog(frame, "Commented out one or more data dir(s) for now.. add back before using for real.");
//			frame.dispose();
//			//frame.setContentPane(newContentPane);
//
//			//Display the window.
//			//frame.pack();
//			//frame.setVisible(true);
//		}
	}
	
	
	// Specify input and output data locations for preprocessing.  Results will be used for model test
	// The map maps input dir to output dir
	public static HashMap<String,String> preprocessForTest = new HashMap<String,String>();
	static { 
		preprocessForTest.put(SHARP_SEED_CORPUS + "Mayo/UMLS_CEM", DATA_DIR + "preprocessed_data/sharp/test");
		preprocessForTest.put(SHARP_SEED_CORPUS + "Seattle Group Health/UMLS_CEM", DATA_DIR + "preprocessed_data/sharp/test");
		preprocessForTest.put(MiPACQ_CORPUS, DATA_DIR + "preprocessed_data/mipacq/test");
	}

	// Specify input and output data locations for preprocessing.  Results will be used for model dev
	// The map maps input dir to output dir
	public static HashMap<String,String> preprocessForDev = new HashMap<String,String>();
	static { 
		preprocessForDev.put(SHARP_SEED_CORPUS + "Mayo/UMLS_CEM", DATA_DIR + "preprocessed_data/sharp/dev");
		preprocessForDev.put(SHARP_SEED_CORPUS + "Seattle Group Health/UMLS_CEM", DATA_DIR + "preprocessed_data/sharp/dev");
		preprocessForDev.put(MiPACQ_CORPUS, DATA_DIR + "preprocessed_data/mipacq/dev");
	}
		
	public static String evalOutputDir =  DATA_DIR + "processing_output_aka_eval_output";
	
	public static String instanceGatheringOutputDir =  DATA_DIR + "q_output_instance_gathering";

    // If you don't want to train/cross-validate everything, comment these out
	public static ArrayList<String> allAnnotationTypes = new ArrayList<String>();
	static { 
		allAnnotationTypes.add("polarity");
		allAnnotationTypes.add("conditional");
		allAnnotationTypes.add("uncertainty");
		allAnnotationTypes.add("subject");
		allAnnotationTypes.add("generic");
		allAnnotationTypes.add("historyOf");
	}
}
