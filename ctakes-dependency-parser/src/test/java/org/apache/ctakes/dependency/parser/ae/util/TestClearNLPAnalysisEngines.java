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
package org.apache.ctakes.dependency.parser.ae.util;

import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.dependency.parser.util.SRLUtility;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.util.cr.FilesCollectionReader;
import org.junit.Test;
import org.kohsuke.args4j.Option;

/**
 * This class illustrates the pipeline needed to run the ClearNLP dependency parser and SRL systems
 * Note: This uses small, highly inaccurate model files, to keep the expense of running down.
 *       For real applications, use the model files recommended in the README.txt file, or leave the model file
 *       configuration parameter unspecified
 * @author lbecker
 *
 */
public class TestClearNLPAnalysisEngines{
	
	// Create dependency parsers analysis engine with the default models
	// The dummy models from ClearParser haven't been updated to work with ClearNLP.
	//public static final String DEP_DUMMY_MODEL_FILE = "org/apache/ctakes/dependency/parser/models/dependency/dummy.dep.mod.jar";
	//public static final String SRL_DUMMY_MODEL_FILE = "org/apache/ctakes/dependency/parser/models/srl/dummy.srl.mod.jar";
	public static String INPUT_FILE = "../ctakes-clinical-pipeline/src/test/data/plaintext/testpatient_plaintext_1.txt";
	public static class Options {
		
		@Option(name = "-d",
				aliases = "--depModelFile",
				usage = "specify the path to the dependency parser model file",
				required = false)
		public String depModelFile = null;
		
		@Option(name = "-s",
				aliases = "--srlModelFile",
				usage = "specify the path to the ClearNLP srl model file",
				required = false)
		public String srlModelFile = null;
		
		
		@Option(name = "-i",
				aliases = "--inputFile",
				usage = "specify the path to the plaintext input",
				required = false)
		public String inputFile = INPUT_FILE;
	}


	/**
	 * Simple inner class for dumping out ClearNLP output
	 * @author lbecker
	 *
	 */
	public static class DumpClearNLPOutputAE extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
				System.out.println("SOURCE SENTENCE:" + sentence.getCoveredText());
				System.out.println("Dependency Parse:");
				System.out.println(DependencyUtility.dumpDependencyGraph(sentence));
				System.out.println("Semantic Roles:");
				System.out.println(SRLUtility.dumpSRLOutput(sentence));
						
			}
		}
	}
	

	@Test
	public void TestClearNLPPipeLine() throws Exception {
		
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescription();
		
		CollectionReader reader1 = CollectionReaderFactory.createReader(
				FilesCollectionReader.class,
				typeSystem,
				FilesCollectionReader.PARAM_ROOT_FILE,
				INPUT_FILE
				);
		
		// Load preprocessing pipeline (consists of 
		AnalysisEngineDescription preprocessingAE = WriteClearNLPDescriptors.getPlaintextAggregateBuilder().createAggregateDescription();
		
		// Create dependency parsers analysis engine with the default models
		// The dummy models from ClearParser haven't been updated to work with ClearNLP.
		AnalysisEngineDescription clearNLPDepParser = AnalysisEngineFactory.createEngineDescription(
		    ClearNLPDependencyParserAE.class,
		    typeSystem);
		    
		// Create analysis engine for SRL
		AnalysisEngineDescription clearNLPSRL = AnalysisEngineFactory.createEngineDescription(
		    ClearNLPSemanticRoleLabelerAE.class,
		    typeSystem);
		
		AnalysisEngineDescription dumpClearNLPOutput = AnalysisEngineFactory.createEngineDescription(
				DumpClearNLPOutputAE.class,
				typeSystem);
		
		SimplePipeline.runPipeline(reader1, preprocessingAE, clearNLPDepParser, clearNLPSRL, dumpClearNLPOutput);	
	}
	
	 @Test
	  public void TestClearNLPPipeLineWithFactoryMethods() throws Exception {
	    
	    TypeSystemDescription typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescription();
	    
	    CollectionReader reader1 = CollectionReaderFactory.createReader(
	        FilesCollectionReader.class,
	        typeSystem,
	        FilesCollectionReader.PARAM_ROOT_FILE,
	        INPUT_FILE
	        );
	    
	    // Load preprocessing pipeline (consists of 
	    AnalysisEngineDescription preprocessingAE = WriteClearNLPDescriptors.getPlaintextAggregateBuilder().createAggregateDescription();
	    
	    // Create dependency parsers analysis engine with the default models
	    // The dummy models from ClearParser haven't been updated to work with ClearNLP.
	    AnalysisEngineDescription clearNLPDepParser = ClearNLPDependencyParserAE.createAnnotatorDescription();
	        
	    // Create analysis engine for SRL
	    AnalysisEngineDescription clearNLPSRL = 
	        ClearNLPSemanticRoleLabelerAE.createAnnotatorDescription();
	    
	    AnalysisEngineDescription dumpClearNLPOutput = AnalysisEngineFactory.createEngineDescription(
	        DumpClearNLPOutputAE.class,
	        typeSystem);
	    
	    SimplePipeline.runPipeline(reader1, preprocessingAE, clearNLPDepParser, clearNLPSRL, dumpClearNLPOutput); 
	  }
}
