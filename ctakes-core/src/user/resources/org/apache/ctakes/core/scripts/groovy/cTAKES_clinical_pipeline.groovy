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
#!/usr/bin/env groovy

/**
** 	This script was not written to be run directly, although it could be if you 
**  set up your classpath to include all the things needed (see below).
**  The intent is to have something else set up a number of things first, 
**  such as the classpath for running cTAKES, and then this script actually uses cTAKES.
** 	This script assumes that 
**   - you have installed Apache cTAKES. 
**   - you have everything on your classpath needed to run cTAKES (see run_cTAKES.groovy)
**   - you have installed Groovy 
**   - the command groovy is available in your path.
**  Arguments:
**   - The name or full path of the directory containing the files to be processed by cTAKES
**   - The name or full path of the directory where cTAKES is installed 
** 	On Debian/Ubuntu systems, installing Groovy should be as easy as apt-get install groovy.
** 	You can download groovy from http://groovy.codehaus.org/
**/

import java.io.File;
import org.apache.uima.jcas.JCas;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.xwriter.XWriter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import static org.apache.uima.fit.util.JCasUtil.*;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.util.CtakesFileNamer;

import org.cleartk.util.cr.FilesCollectionReader;
import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;


def OUTPUT_DIR = "output-dir";
def DEBUG = false;


println("Starting " + this.class.getName());

// this.class.classLoader.rootLoader.URLs.each{ println it } // Print out the classpath entries for debugging


if (DEBUG) println("args.length: " + args.length);
if (DEBUG) println("args = " + args);

if (args.length!=2) {
	println "Expect exactly 2 arguments - the input dir and the path to cTAKES"
    println("args.length: " + args.length);
    println("args = " + args);
	System.exit(-1);
}

def inputDir = args[0];
def cTAKES_HOME = args[1];


println("Using cTAKES in " + cTAKES_HOME);

println("Instantiating collection reader");
CollectionReader collectionReader = FilesCollectionReader.getCollectionReader(inputDir);

println "Creating TypePriorities";
// Add first TypePriorityList
Class cl1 = org.apache.ctakes.typesystem.type.textspan.Segment;
Class cl2 = org.apache.ctakes.typesystem.type.textspan.Sentence;
Class cl3 = org.apache.ctakes.typesystem.type.syntax.BaseToken;
TypePriorities typePriorities = TypePrioritiesFactory.createTypePriorities(cl1, cl2, cl3);

// Add second TypePriorityList
TypePriorityList typePriorityList = typePriorities.addPriorityList();
typePriorityList.addType("org.apache.ctakes.typesystem.type.textspan.Sentence");
typePriorityList.addType("org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation");

//Build the pipeline to run
// we assume cTAKES' desc directory is on classpath for those 
// such as ctakes-assertion/desc/AssertionMiniPipelineAnalysisEngine that we 
// reference by descriptor (XML file) name
println("Building pipeline aggregate builder object");

AggregateBuilder aggregateBuilder = new AggregateBuilder(null, typePriorities, null);

// Here is the flow from AggregatePlaintextUMLSProcessor.xml in 3.1.1
/*
    <node>SimpleSegmentAnnotator</node>
    <node>SentenceDetectorAnnotator</node>
    <node>TokenizerAnnotator</node>
    <node>LvgAnnotator</node>
    <node>ContextDependentTokenizerAnnotator</node>
    <node>POSTagger</node>
    <node>Chunker</node>
    <node>AdjustNounPhraseToIncludeFollowingNP</node>
    <node>AdjustNounPhraseToIncludeFollowingPPNP</node>
    <node>LookupWindowAnnotator</node>
    <node>DictionaryLookupAnnotatorDB</node>
    <node>DependencyParser</node>
    <node>SemanticRoleLabeler</node>        
    <node>AssertionAnnotator</node>
    <node>ExtractionPrepAnnotator</node>
*/


println(" Adding segment aka section annotator");
def segmentAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.core.ae.SimpleSegmentAnnotator.class);
// ConfigurationParameterFactory.addConfigurationParameters(
//			segmentAnnotator,
//			"SegmentID", // TODO update SimpleSegmentAnnotator.java to have a constant for this
//			"20104" // 20104 is Current Medications. "SIMPLE_SEGMENT" is default. @see SimpleSegmentAnnotator.java  
//	);
aggregateBuilder.add(segmentAnnotator);

println(" Adding sentence annotator");
def sentenceAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.core.ae.SentenceDetector.class);
ConfigurationParameterFactory.addConfigurationParameters(
			sentenceAnnotator,
			org.apache.ctakes.core.ae.SentenceDetector.SD_MODEL_FILE_PARAM, 
			"org/apache/ctakes/core/sentdetect/sd-med-model.zip" 
	);
// This is where you could add SegmentsToSkip parameter values @see org.apache.ctakes.core.ae.SentenceDetector.PARAM_SEGMENTS_TO_SKIP
aggregateBuilder.add(sentenceAnnotator);

println(" Adding tokenizer annotator");
def tokenizerAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.core.ae.TokenizerAnnotatorPTB.class);
// This is where you could add SegmentsToSkip parameter values. @see TokenizerAnnotatorPTB.PARAM_SEGMENTS_TO_SKIP
aggregateBuilder.add(tokenizerAnnotator);

println(" Adding LVG annotator (word normalizer)");
//If chose to add lvg annotator by class name, the following function provides a start, but it has a TODO
//def lvgAnnotator = lvgAnnotatorByClassName();
//aggregateBuilder.add(lvgAnnotator);
// lvg annotator has many parameters, using the xml descriptor to easily use the default parameters
def lvgDescriptorLocation = "ctakes-lvg/desc/analysis_engine/LvgAnnotator"; // Note createAnalysisEngineDescription expects name to not end in .xml even though filename actually does
AnalysisEngineDescription lvgDescriptor = AnalysisEngineFactory.createAnalysisEngineDescription(lvgDescriptorLocation); // Note, do not include .xml in the name here as createAnalysisEngineDescription will append .xml
aggregateBuilder.add(lvgDescriptor);

println(" Adding context dependent tokenizer annotator");
def cdtAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator.class);
aggregateBuilder.add(cdtAnnotator);

println(" Adding part of speech (POS) annotator");
def posAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.postagger.POSTagger.class);
ConfigurationParameterFactory.addConfigurationParameters(
			posAnnotator,
			org.apache.ctakes.postagger.POSTagger.POS_MODEL_FILE_PARAM, 
			"org/apache/ctakes/postagger/models/mayo-pos.zip" 
	);
aggregateBuilder.add(posAnnotator);

println(" Adding chunker annotator");
def chunkerAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.chunker.ae.Chunker.class);
ConfigurationParameterFactory.addConfigurationParameters(
			chunkerAnnotator,
			org.apache.ctakes.chunker.ae.Chunker.CHUNKER_MODEL_FILE_PARAM, 
			"org/apache/ctakes/chunker/models/chunker-model.zip" 
	);
ConfigurationParameterFactory.addConfigurationParameters(
			chunkerAnnotator,
			org.apache.ctakes.chunker.ae.Chunker.CHUNKER_CREATOR_CLASS_PARAM, 
			"org.apache.ctakes.chunker.ae.PhraseTypeChunkCreator" 
	);
aggregateBuilder.add(chunkerAnnotator);
	
// First chunk adjuster
println(" Adding chunker adjuster annotator - NounPhraseToIncludeFollowingNP");
def chunkAdjusterNPNPAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster.class);
ConfigurationParameterFactory.addConfigurationParameters(
			chunkAdjusterNPNPAnnotator,
			org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster.PARAM_CHUNK_PATTERN, 
			(String []) ["NP", "NP"]
	);
ConfigurationParameterFactory.addConfigurationParameters(
			chunkAdjusterNPNPAnnotator,
			org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN, 
			1
	);
aggregateBuilder.add(chunkAdjusterNPNPAnnotator);

// Second chunk adjuster
println(" Adding chunker adjuster annotator - NounPhraseToIncludeFollowingPPNP");
def chunkAdjusterNPPPNPAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster.class);
ConfigurationParameterFactory.addConfigurationParameters(
			chunkAdjusterNPPPNPAnnotator,
			org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster.PARAM_CHUNK_PATTERN, 
			(String []) ["NP", "PP", "NP"]
	);
ConfigurationParameterFactory.addConfigurationParameters(
			chunkAdjusterNPPPNPAnnotator,
			org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN, 
			2
	);
aggregateBuilder.add(chunkAdjusterNPPPNPAnnotator);

println(" Adding lookup window annotator");
def lwAnnotatorDescriptorLocation = "ctakes-clinical-pipeline/desc/analysis_engine/LookupWindowAnnotator"; 
AnalysisEngineDescription lookupWindowAnnotator = AnalysisEngineFactory.createAnalysisEngineDescription(lwAnnotatorDescriptorLocation); 
aggregateBuilder.add(lookupWindowAnnotator);


// TODO - this is a longer range TODO item: it would be nice to be able to set values here that would be used instead of what's in the LookupDesc*.xml files


// DictionaryLookupAnnotatorUMLS - org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator
println(" Adding dictionary lookup (UMLS) annotator");
def dictLookupAnnotatorDescriptorLocation = "ctakes-dictionary-lookup/desc/analysis_engine/DictionaryLookupAnnotatorUMLS"; 
AnalysisEngineDescription dictionaryLookupAnnotator = AnalysisEngineFactory.createAnalysisEngineDescription(dictLookupAnnotatorDescriptorLocation); 
//UmlsDictionaryLookupAnnotator will look for system properties before looking at these values
ConfigurationParameterFactory.addConfigurationParameters(
			dictionaryLookupAnnotator,
			org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator.UMLSADDR_PARAM, 
			"https://uts-ws.nlm.nih.gov/restful/isValidUMLSUser"
	);
ConfigurationParameterFactory.addConfigurationParameters(
			dictionaryLookupAnnotator,
			org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator.UMLSVENDOR_PARAM, 
			"NLM-6515182895"
	);
ConfigurationParameterFactory.addConfigurationParameters(
			dictionaryLookupAnnotator,
			org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator.UMLSUSER_PARAM, 
			"" // put your UMLS user ID here or set JAVA_OPTS ctakes_umlsuser or see user or install guide
	);
// Commenting out the setting of UMLSPW_PARAM as you probably don't want to put your password
// in this script so that if you share the script you don't share your password accidentally
//ConfigurationParameterFactory.addConfigurationParameters(
//			dictionaryLookupAnnotator,
//			org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator.UMLSPW_PARAM, 
//			""
//	);
aggregateBuilder.add(dictionaryLookupAnnotator);

// DependencyParser - see ClearNLPDependencyParserAE.xml
println " Adding dependency parser annotator"
annotatorClass = org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE.class;
def dependencyParserAnnotator = AnalysisEngineFactory.createPrimitiveDescription(annotatorClass);
ConfigurationParameterFactory.addConfigurationParameters(
				dependencyParserAnnotator,
				org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE.PARAM_PARSER_MODEL_FILE_NAME,
				org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE.DEFAULT_MODEL_FILE_NAME
		);
ConfigurationParameterFactory.addConfigurationParameters(
				dependencyParserAnnotator,
				"ParserAlgorithmName",
				"shift-pop"
		);
ConfigurationParameterFactory.addConfigurationParameters(
				dependencyParserAnnotator,
				org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE.PARAM_USE_LEMMATIZER,
				true
		);
aggregateBuilder.add(dependencyParserAnnotator);

// SemanticRoleLabeler - see ClearNLPSemanticRoleLabelerAE.xml
println " Adding semantic role labeler annotator"
annotatorClass = org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE.class;
def srlAnnotator = AnalysisEngineFactory.createPrimitiveDescription(annotatorClass);
ConfigurationParameterFactory.addConfigurationParameters(
				srlAnnotator,
				org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE.PARAM_PARSER_MODEL_FILE_NAME,
				org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE.DEFAULT_SRL_MODEL_FILE_NAME
		);
ConfigurationParameterFactory.addConfigurationParameters(
				srlAnnotator,
				"UseLemmatizer",
				true
		);
aggregateBuilder.add(srlAnnotator);
		
println(" Adding assertion annotators");
def assertionDescriptorLocation = "ctakes-assertion/desc/AssertionMiniPipelineAnalysisEngine"; // Note createAnalysisEngineDescription expects name to not end in .xml even though filename actually does
AnalysisEngineDescription assertionDescriptor = AnalysisEngineFactory.createAnalysisEngineDescription(assertionDescriptorLocation); // Note, do not include .xml in the name here as createAnalysisEngineDescription will append .xml
aggregateBuilder.add(assertionDescriptor);

println(" Adding extraction prep annotator");
def extractionPrepDescriptorLocation = "ctakes-clinical-pipeline/desc/analysis_engine/ExtractionPrepAnnotator"; 
AnalysisEngineDescription extractionPrepDescriptor = AnalysisEngineFactory.createAnalysisEngineDescription(extractionPrepDescriptorLocation); 
aggregateBuilder.add(extractionPrepDescriptor);



TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription("org.apache.ctakes.typesystem.types.TypeSystem");
		
		
// generic XMI writer
// TODO When generalize this script to run specific components instead of the whole pipeline,
// consider separate writer for each engine or a diffferent writer that produces more friendly output
println(" Adding XMI writer");
AnalysisEngineDescription xWriter = AnalysisEngineFactory.createPrimitiveDescription(
			  XWriter.class,
			  typeSystemDescription,
			  XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
			  OUTPUT_DIR,
			  XWriter.PARAM_FILE_NAMER_CLASS_NAME,
			  CtakesFileNamer.class.getName()
			  );
aggregateBuilder.add(xWriter);

//println("About to run pipeline using SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate())");
SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());

println("Done: " + this.class.getName());




def lvgAnnotatorByClassName() {
def lvgAnnotator = AnalysisEngineFactory.createPrimitiveDescription(org.apache.ctakes.lvg.ae.LvgAnnotator.class);
// This is where you could add parameter values. 
//PARAM_POST_LEMMAS defaults to false
//PARAM_USE_LEMMA_CACHE defaults to false
//PARAM_LEMMA_CACHE_FILE_LOCATION (required iff PARAM_USE_LEMMA_CACHE true)
//PARAM_LEMMA_CACHE_FREQUENCY_CUTOFF // code defaults to 20 which is same as LvgAnnotator.xml value
//"XeroxTreebankMap"
//"ExclusionSet"
// "UseSegments"
// "SegmentsToSkip"
def XeroxTreebankMapValues = (String []) [
            "adj|JJ",
            "adv|RB",
            "aux|AUX",
            "compl|CS",
            "conj|CC",
            "det|DET",
            "modal|MD",
            "noun|NN",
            "prep|IN",
            "pron|PRP",
            "verb|VB" ];
ConfigurationParameterFactory.addConfigurationParameters(
			lvgAnnotator,
			"XeroxTreebankMap", 
			XeroxTreebankMapValues 
	);
def ExclusionSetValues = (String[]) [
            "and",
            "And",
            "by",
            "By",
            "for",
            "For",
            "in",
            "In",
            "of",
            "Of",
            "on",
            "On",
            "the",
            "The",
            "to",
            "To",
            "with",
            "With" ];
ConfigurationParameterFactory.addConfigurationParameters(
			lvgAnnotator,
			"ExclusionSet", 
			ExclusionSetValues 
	);
ConfigurationParameterFactory.addConfigurationParameters(
			lvgAnnotator,
			"UseSegments", 
			false
	);
ConfigurationParameterFactory.addConfigurationParameters(
			lvgAnnotator,
			"SegmentsToSkip", 
			(String [])[]
	);
ConfigurationParameterFactory.addConfigurationParameters(
			lvgAnnotator,
			"UseCmdCache", 
			false
	);
ConfigurationParameterFactory.addConfigurationParameters(
			lvgAnnotator,
			"CmdCacheFrequencyCutoff", 
			20
	);
// TODO handle LvgCmdApi here 
return lvgAnnotator
}	
