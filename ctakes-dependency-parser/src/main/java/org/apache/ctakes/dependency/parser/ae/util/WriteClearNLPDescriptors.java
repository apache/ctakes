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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.SAXException;



/**
 * Use this to generate UIMA xml description files for the ClearNLP analysis engines.
 *
 */
public class WriteClearNLPDescriptors {
	public static final String SIMPLE_SEGMENTER_PATH = "../ctakes-clinical-pipeline/desc/analysis_engine/SimpleSegmentAnnotator.xml";
	public static final String SENTENCE_DETECTOR_PATH="../ctakes-core/desc/analysis_engine/SentenceDetectorAnnotator.xml";
	public static final String TOKENIZER_PATH="../ctakes-core/desc/analysis_engine/TokenizerAnnotator.xml";
	public static final String LVG_BASE_TOKEN_ANNOTATOR_PATH="desc/analysis_engine/LvgBaseTokenAnnotator.xml";
	public static final String POS_TAGGER_PATH="../ctakes-pos-tagger/desc/POSTagger.xml";
	public static final String DEP_NAME="ClearNLPDependencyParser";
	public static final String SRL_NAME="ClearNLPSRL";

	public static class Options {
		@Option(name = "-o",
				aliases = "--outputRoot",
				usage = "specify the directory to write out descriptor files",
				required = false)
		public File outputRoot = new File("desc/analysis_engine");

		@Option(name = "-m",
				aliases = "--modelFile",
				usage = "specify the path to the relation extractor model jar file",
				required = false)
		public File modelFile = new File("model.jar");
	}


	/**
	 * @param args
	 * @throws IOException 
	 * @throws UIMAException 
	 * @throws SAXException 
	 * @throws CmdLineException 
	 */
	public static void main(String[] args) throws IOException, UIMAException, SAXException, CmdLineException {
		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath("../../../ctakes-type-system/src/main/resources/org/apache/ctakes/typesystem/types/TypeSystem.xml");

		AnalysisEngineDescription ClearNLPSRLDesc = AnalysisEngineFactory.createEngineDescription(
				ClearNLPSemanticRoleLabelerAE.class,
				typeSystem
				);

		AnalysisEngineDescription ClearNLPDepParserDesc = AnalysisEngineFactory.createEngineDescription(
				ClearNLPDependencyParserAE.class,
				typeSystem
				);
		

		System.out.println((new File("desc/analysis_engine")).getAbsolutePath());

		// Write standalone description files
		ClearNLPDepParserDesc.toXML(new FileOutputStream(new File(options.outputRoot, "ClearNLPDependencyParserAE.xml")));
		ClearNLPSRLDesc.toXML(new FileOutputStream(new File(options.outputRoot, "ClearNLPSemanticRoleLabelerAE.xml")));

		// Write aggregate plaintext description files
		AggregateBuilder aggregateBuilder = getPlaintextAggregateBuilder();
		writeAggregateDescriptions(aggregateBuilder, ClearNLPDepParserDesc, ClearNLPSRLDesc, options.outputRoot, "PlaintextAggregate.xml");

		// Write aggregate tokenized description files
		aggregateBuilder = getTokenizedAggregateBuilder();
		writeAggregateDescriptions(aggregateBuilder, ClearNLPDepParserDesc, ClearNLPSRLDesc, options.outputRoot, "TokenizedAggregate.xml");

		// Write aggregate tokenizedInf description files
		aggregateBuilder = getTokenizedInfPosAggregateBuilder();
		writeAggregateDescriptions(aggregateBuilder, ClearNLPDepParserDesc, ClearNLPSRLDesc, options.outputRoot, "TokenizedInfPosAggregate.xml");

	}

	/**
	 * Builds the plaintext prepreprocessing pipeline for ClearNLP
	 * @return
	 * @throws InvalidXMLException
	 * @throws IOException
	 */
	public static AggregateBuilder getPlaintextAggregateBuilder() throws InvalidXMLException, IOException {
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add(loadDescription(SIMPLE_SEGMENTER_PATH));
		aggregateBuilder.add(loadDescription(SENTENCE_DETECTOR_PATH));
		aggregateBuilder.add(loadDescription(TOKENIZER_PATH));
		//aggregateBuilder.add(loadDescription(LVG_BASE_TOKEN_ANNOTATOR_PATH));
		aggregateBuilder.add(loadDescription(POS_TAGGER_PATH));
		return aggregateBuilder;
	}


	/**
	 * Builds the tokenized preprocessing pipeline for ClearNLP
	 * @return
	 * @throws InvalidXMLException
	 * @throws IOException
	 */
	public static AggregateBuilder getTokenizedAggregateBuilder() throws InvalidXMLException, IOException {
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add(loadDescription(SIMPLE_SEGMENTER_PATH));
		aggregateBuilder.add(loadDescription(LVG_BASE_TOKEN_ANNOTATOR_PATH));
		return aggregateBuilder;
	}

	/**
	 * Builds the tokenizedInf preprocessing for ClearNLP
	 * @return
	 * @throws InvalidXMLException
	 * @throws IOException
	 */
	public static AggregateBuilder getTokenizedInfPosAggregateBuilder() throws InvalidXMLException, IOException {
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add(loadDescription(SIMPLE_SEGMENTER_PATH));
		aggregateBuilder.add(loadDescription(LVG_BASE_TOKEN_ANNOTATOR_PATH));
		aggregateBuilder.add(loadDescription(POS_TAGGER_PATH));
		return aggregateBuilder;
	}


	/**
	 * Simple method to load xml description and return an AnalysisEngineDescription object
	 * @param pathToDescription
	 * @return
	 * @throws IOException
	 * @throws InvalidXMLException
	 */
	public static AnalysisEngineDescription loadDescription(String pathToDescription) throws IOException, InvalidXMLException {
		File file = new File(pathToDescription);
		XMLParser parser = UIMAFramework.getXMLParser();
		XMLInputSource source = new XMLInputSource(file);
		AnalysisEngineDescription desc = parser.parseAnalysisEngineDescription(source);
		return desc;
	}
	
	private static void writeAggregateDescriptions(
			AggregateBuilder preprocessing, 
			AnalysisEngineDescription ClearNLPDepParserDesc, 
			AnalysisEngineDescription ClearNLPSRLDesc,
			File outputRoot,
			String aggregateSuffix) throws ResourceInitializationException, FileNotFoundException, SAXException, IOException {

		// Append Dependency Parser into aggregate and write description file
		preprocessing.add(ClearNLPDepParserDesc);
		preprocessing.createAggregateDescription().toXML(new FileOutputStream(new File(outputRoot, DEP_NAME + aggregateSuffix))); 
		// Append SRL Parser into aggregate and write description file
		preprocessing.add(ClearNLPSRLDesc);
		preprocessing.createAggregateDescription().toXML(new FileOutputStream(new File(outputRoot, SRL_NAME + aggregateSuffix))); 

	}


}
