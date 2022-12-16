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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.ctakes.assertion.cc.JudgeAttributeInstances;
import org.apache.ctakes.assertion.eval.XMIReader;
import org.apache.ctakes.assertion.util.AssertionConst;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class RunJudgeAttributeInstances {
	public static class Options {
		@Option(
				name = "--input-dir",
				usage = "where to read the fully-annotated xmi data from",
				required = true)
				public String inputDir = AssertionConst.evalOutputDir;
		@Option(
				name = "--output-dir",
				usage = "where to write the 'adjudicated' instances in xmi format to",
				required = true)
				public String outputDir = AssertionConst.instanceGatheringOutputDir;
//		@Option(
//				name = "--ignore-polarity",
//				usage = "specify whether polarity processing should be ignored (true or false). default: false",
//				required = false)
//				public boolean ignorePolarity = false; // note that this is reversed from the "ignore" statement
//
//		@Option(
//				name = "--ignore-conditional",
//				usage = "specify whether conditional processing should be ignored (true or false). default: false",
//				required = false)
//				public boolean ignoreConditional = false;
//
//		@Option(
//				name = "--ignore-uncertainty",
//				usage = "specify whether uncertainty processing should be ignored (true or false). default: false",
//				required = false)
//				public boolean ignoreUncertainty = false;
//
//		@Option(
//				name = "--ignore-subject",
//				usage = "specify whether subject processing should be ignored (true or false). default: false",
//				required = false,
//				handler=BooleanOptionHandler.class)
//				public boolean ignoreSubject = false;
//
//		@Option(
//				name = "--ignore-generic",
//				usage = "specify whether generic processing should be ignored (true or false). default: false",
//				required = false)
//				public boolean ignoreGeneric = false;
//
//		// srh adding 2/20/13
//		@Option(
//				name = "--ignore-history",
//				usage = "specify whether 'history of' processing should be run (true or false). default: false",
//				required = false)
//				public boolean ignoreHistory = false;
	}

	public static Options options = new Options();

	/**
	 * @param args
	 * @throws CmdLineException 
	 */
	public static void main(String[] args) throws CmdLineException {
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		TypeSystemDescription typeSystemDescription;
		CollectionReaderDescription collectionReader = null;
		AnalysisEngineDescription userInput = null;
		
		try {
			typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription();
			
			List<File> items = Arrays.asList((new File(options.inputDir)).listFiles());
		    String[] paths = new String[items.size()];
		    for (int i = 0; i < paths.length; ++i) {
		      paths[i] = items.get(i).getAbsolutePath();
		    }
			
			collectionReader = CollectionReaderFactory.createReaderDescription(
			        XMIReader.class,
			        TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(),
			        XMIReader.PARAM_FILES,
			        paths);
			
			userInput = AnalysisEngineFactory.createEngineDescription(
					JudgeAttributeInstances.class,
					typeSystemDescription,
					JudgeAttributeInstances.PARAM_OUTPUT_DIRECTORY_NAME,
					options.outputDir
			);			
			
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			
			SimplePipeline.runPipeline(collectionReader, userInput);
			
		} catch (UIMAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
