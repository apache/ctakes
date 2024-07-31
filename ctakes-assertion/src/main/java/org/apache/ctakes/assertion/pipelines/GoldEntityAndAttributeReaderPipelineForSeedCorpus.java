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


import com.google.common.base.Function;
import org.apache.ctakes.assertion.cr.I2B2Challenge2010CollectionReader;
import org.apache.ctakes.assertion.cr.MiPACQKnowtatorXMLReader;
import org.apache.ctakes.assertion.cr.NegExCorpusReader;
import org.apache.ctakes.assertion.pipelines.SharpCorpusSplit.Subcorpus;
import org.apache.ctakes.core.ae.SHARPKnowtatorXMLReader;
import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.cr.FileTreeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;

/**
 * 
 * A class for testing the reader for the gold standard relation data. 
 * Currently this class runs the reader and saves the resulting annotations as xmi files.
 * 
 * @author dmitriy dligach
 * @author matt coarr
 *
 */
public class GoldEntityAndAttributeReaderPipelineForSeedCorpus {
	
	static final Logger LOGGER = LoggerFactory.getLogger(GoldEntityAndAttributeReaderPipelineForSeedCorpus.class.getName());

	public static void main(String[] args) throws UIMAException, IOException {
		
		LOGGER.warn("This should be run with one command-line argument that is the parent UMLS_CEM directory.");
		LOGGER.warn("Also, make sure each ss1_batch* directory has both a Knowtator/text directory and a Knowtator_XML directory (not the underscore in the xml directory, not a space)");
		
		if (args.length != 1)
		{
			System.out.println("Requires one parameter that is the UMLS_CEM main directory (e.g. the \"Seed_Corpus/Mayo/UMLS_CEM\" or \"Seattle Group Health/UMLS_CEM\"). The path should be fully specified.");
		}
		
		String parentDirectoryString = args[0];
		//String parentDirectoryString = "/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/Seattle Group Health/UMLS_CEM";

		File parentDirectory = new File(parentDirectoryString);
		readSharpSeedUmlsCem(parentDirectory);
		
	}

	public static void readSharpSeedUmlsCem(File parentDirectory) throws ResourceInitializationException, UIMAException, IOException {
		readSharpSeedUmlsCem(parentDirectory, null, null, null);
	}
	
	public static void readSharpSeedUmlsCem(File parentDirectory, File trainDirectory, File testDirectory, File devDirectory)
			throws ResourceInitializationException, UIMAException, IOException {
//		LOGGER.info("parent directory: " + parentDirectoryString);
//		File parentDirectory = new File(parentDirectoryString);
		if (!parentDirectory.exists())
		{
			LOGGER.error(String.format("parent directory %s does not exist! exiting!", parentDirectory.getAbsolutePath()));
			return;
		}
		
		File[] batchDirectories = parentDirectory.listFiles( new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		} );

		assert batchDirectories != null;
		for (File currentBatchDirectory : batchDirectories)
		{
			
			LOGGER.info("current batch directory: " + currentBatchDirectory.getName());
			
			if (!currentBatchDirectory.exists())
			{
				LOGGER.error(String.format("current batch directory does not exist! exiting! [\"%s\"]", currentBatchDirectory.toString()));
				continue;
			}
			
			File knowtatorDirectory = new File(currentBatchDirectory, "Knowtator");
			File textDirectory = new File(knowtatorDirectory, "text");
			// train set uses this naming convention
			File xmlDirectory = new File(currentBatchDirectory, "Knowtator_XML");
			File xmiDirectory = new File(currentBatchDirectory, "Knowtator_XMI");
			// dev and test sets use this naming convention
			if (!xmlDirectory.exists()) {
				xmlDirectory = new File(currentBatchDirectory, "Knowtator XML");
				xmiDirectory = new File(currentBatchDirectory, "Knowtator XMI");
			}
			
			if (!knowtatorDirectory.isDirectory() ||
					!textDirectory.isDirectory() ||
					!xmlDirectory.isDirectory())
			{
				LOGGER.error("one of the directories does not exist! skipping...");
				continue;
			}
			
			if (!xmiDirectory.isDirectory())
			{
				xmiDirectory.mkdir();
			}
				
			
			TypeSystemDescription typeSystemDescription = 
					// use the uimafit method of finding available type system
					// descriptor via META-INF/org.apache.uima.fit/types.txt 
					// (found in ctakes-type-system/src/main/resources)
				TypeSystemDescriptionFactory.createTypeSystemDescription();
			
			AggregateBuilder aggregate = new AggregateBuilder();
			
			CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
//					FilesInDirectoryCollectionReader.class,
//					typeSystemDescription,
//					"InputDirectory",
					FileTreeReader.class,
					ConfigParameterConstants.PARAM_INPUTDIR,
					textDirectory.toString()
					);
			
			// read the UMLS_CEM data from Knowtator
			AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
					SHARPKnowtatorXMLReader.class,
					typeSystemDescription,
					"TextDirectory", // 3/13/13 halgrim changed from "TextURI" trying to work with new SHARPKnowtatorXMLReader.java
					//"/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/sandbox/batch02_mayo/knowtator/"
					textDirectory.toString() + "/"
			);
			aggregate.add(goldAnnotator);

			// write just the XMI version of what's in Knowtator UMLS_CEM
			AnalysisEngineDescription xWriter = AnalysisEngineFactory.createEngineDescription(
//					XmiWriterCasConsumerCtakes.class,
//					typeSystemDescription,
//					XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
					FileTreeXmiWriter.class,
					ConfigParameterConstants.PARAM_OUTPUTDIR,
					xmiDirectory.toString()
			);
			aggregate.add(xWriter);

			// fill in other values that are necessary for preprocessing
			AnalysisEngineDescription preprocessAnnotator = AnalysisEngineFactory.createEngineDescription(
					"desc/analysis_engine/AttributeDiscoveryPreprocessor"
					);
			aggregate.add(preprocessAnnotator);
			
			if (trainDirectory!=null && testDirectory!=null && devDirectory!=null) {
				File subcorpusDirectory;
				switch (SharpCorpusSplit.splitSeed(currentBatchDirectory)) {
				case TRAIN: 
					subcorpusDirectory = trainDirectory;
					break;
				case TEST:
					subcorpusDirectory = testDirectory;
					break;
				case DEV:
					subcorpusDirectory = devDirectory;
					break;
				case CROSSVAL:
					subcorpusDirectory = trainDirectory;
					break;
				default:
					subcorpusDirectory = trainDirectory;
					break;
				}
				AnalysisEngineDescription xWriter2 = AnalysisEngineFactory.createEngineDescription(
//						XmiWriterCasConsumerCtakes.class,
//						typeSystemDescription,
//						XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
						FileTreeXmiWriter.class,
						ConfigParameterConstants.PARAM_OUTPUTDIR,
						subcorpusDirectory
				);
				aggregate.add(xWriter2);
//				SimplePipeline.runPipeline(collectionReader, goldAnnotator, xWriter, xWriter2);
			}

			SimplePipeline.runPipeline(collectionReader, aggregate.createAggregateDescription());
		}

		LOGGER.info("Finished!");
	}
	
	public static void readSharpStratifiedUmls(File releaseDirectory, File trainDirectory, File testDirectory, File devDirectory) throws UIMAException, IOException{
	  File mayoStrat = new File(releaseDirectory, "SHARP/MayoStrat/by-batch/umls");
	  // sghStrat not annotated yet... on the off chance it ever is, add it to the array below.
	  
	  readSharpUmls(new File[] {mayoStrat}, trainDirectory, testDirectory, devDirectory, 
	      new Function<File,Subcorpus>(){
	    @Override
        public Subcorpus apply(File f){
	      return SharpCorpusSplit.splitStratified(f);
	  }
	  });
	}
	
	public static void readSharpSeedUmls(File releaseDirectory, File trainDirectory, File testDirectory, File devDirectory) throws UIMAException, IOException{
	  File seed1 = new File(releaseDirectory, "SHARP/SeedSet1/by-batch/umls");
	  readSharpUmls(new File[] {seed1}, trainDirectory, testDirectory, devDirectory,
	       new Function<File,Subcorpus>(){
	      @Override
        public Subcorpus apply(File f){
	        return SharpCorpusSplit.splitSeed(f);
	    }
	    });

	}
	
	public static void readSharpUmls(File[] sections, File trainDirectory, File testDirectory, File devDirectory, Function<File,Subcorpus> splitFunction) throws UIMAException, IOException{
	  for(File section : sections){
	    File[] batches = section.listFiles(new FileFilter(){

        @Override
        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }});
		  assert batches != null;
		  for(File batchDir : batches){
	      TypeSystemDescription typeSystemDescription = 
	          // use the uimafit method of finding available type system
	          // descriptor via META-INF/org.apache.uima.fit/types.txt 
	          // (found in ctakes-type-system/src/main/resources)
	        TypeSystemDescriptionFactory.createTypeSystemDescription();
	      
	      File textDirectory = new File(batchDir, "text");
	      AggregateBuilder aggregate = new AggregateBuilder();
	      
	      CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
//	          FilesInDirectoryCollectionReader.class,
//	          typeSystemDescription,
//	          "InputDirectory",
				 FileTreeReader.class,
				 ConfigParameterConstants.PARAM_INPUTDIR,
				 textDirectory.toString()
	          );
	      
	      // read the UMLS_CEM data from Knowtator
	      AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
	          SHARPKnowtatorXMLReader.class,
	          typeSystemDescription,
	          SHARPKnowtatorXMLReader.PARAM_TEXT_DIRECTORY, // 3/13/13 halgrim changed from "TextURI" trying to work with new SHARPKnowtatorXMLReader.java
	          //"/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/sandbox/batch02_mayo/knowtator/"
	          textDirectory.toString() + "/",
	          SHARPKnowtatorXMLReader.PARAM_SET_DEFAULTS,
	          true
	      );
	      aggregate.add(goldAnnotator);

	      // fill in other values that are necessary for preprocessing
	      AnalysisEngineDescription preprocessAnnotator = AnalysisEngineFactory.createEngineDescription(
	          "desc/analysis_engine/AttributeDiscoveryPreprocessor"
	          );
	      aggregate.add(preprocessAnnotator);

	      File subcorpusDir = null;
//	      Subcorpus subcorpus = SharpCorpusSplit.splitStratified(Integer.parseInt(batchDir.getName()));
	      Subcorpus subcorpus = splitFunction.apply(batchDir);
	      switch(subcorpus){
	      case TRAIN:
	        subcorpusDir = trainDirectory;
	        break;
	      case DEV:
	        subcorpusDir = devDirectory;
	        break;
	      case TEST:
	        subcorpusDir = testDirectory;
	        break;
	      default:
	        subcorpusDir = trainDirectory;
	      }
	      
	       AnalysisEngineDescription xWriter = AnalysisEngineFactory.createEngineDescription(
//	            XmiWriterCasConsumerCtakes.class,
//	            typeSystemDescription,
//	            XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
					FileTreeXmiWriter.class,
					ConfigParameterConstants.PARAM_OUTPUTDIR,
	            subcorpusDir
	        );
	       aggregate.add(xWriter);
	       SimplePipeline.runPipeline(collectionReader, aggregate.createAggregateDescription());

	    }
	  }
	}

	public static void readI2B2Challenge2010(File parentDirectory, File preprocessedDirectory)
	throws ResourceInitializationException, UIMAException, IOException {

		TypeSystemDescription typeSystemDescription = 
			// use the uimafit method of finding available type system
			// descriptor via META-INF/org.apache.uima.fit/types.txt 
			// (found in ctakes-type-system/src/main/resources)
			TypeSystemDescriptionFactory.createTypeSystemDescription();

		AggregateBuilder aggregate = new AggregateBuilder();

		CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
				I2B2Challenge2010CollectionReader.class,
				typeSystemDescription,
				"inputDir",
				parentDirectory
		);

		// fill in other values that are necessary for preprocessing
		AnalysisEngineDescription preprocessAnnotator = AnalysisEngineFactory.createEngineDescription(
				"desc/analysis_engine/AttributeDiscoveryPreprocessor"
		);
		aggregate.add(preprocessAnnotator);

		if (preprocessedDirectory!=null) {
			AnalysisEngineDescription xWriter2 = AnalysisEngineFactory.createEngineDescription(
//					XmiWriterCasConsumerCtakes.class,
//					typeSystemDescription,
//					XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
					FileTreeXmiWriter.class,
					ConfigParameterConstants.PARAM_OUTPUTDIR,
					preprocessedDirectory
			);
			aggregate.add(xWriter2);
			//		SimplePipeline.runPipeline(collectionReader, goldAnnotator, xWriter, xWriter2);
		}

		SimplePipeline.runPipeline(collectionReader, aggregate.createAggregateDescription());
		LOGGER.info("Finished!");
	}

	public static void readNegexTestSet(File inputFile, File preprocessedDirectory)
	throws ResourceInitializationException, UIMAException, IOException {

		TypeSystemDescription typeSystemDescription = 
			TypeSystemDescriptionFactory.createTypeSystemDescription();

		AggregateBuilder aggregate = new AggregateBuilder();

		// input dir is hard-coded in AssertionConst
		CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
				NegExCorpusReader.class,
				typeSystemDescription
		);

		// fill in other values that are necessary for preprocessing
		AnalysisEngineDescription preprocessAnnotator = AnalysisEngineFactory.createEngineDescription(
				"desc/analysis_engine/AttributeDiscoveryPreprocessor"
		);
		aggregate.add(preprocessAnnotator);

		if (preprocessedDirectory!=null) {
			AnalysisEngineDescription xWriter2 = AnalysisEngineFactory.createEngineDescription(
//					XmiWriterCasConsumerCtakes.class,
//					typeSystemDescription,
//					XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
					FileTreeXmiWriter.class,
					ConfigParameterConstants.PARAM_OUTPUTDIR,
					preprocessedDirectory
			);
			aggregate.add(xWriter2);
			//		SimplePipeline.runPipeline(collectionReader, goldAnnotator, xWriter, xWriter2);
		}

		SimplePipeline.runPipeline(collectionReader, aggregate.createAggregateDescription());
		LOGGER.info("Finished!");
	}

	public static void readMiPACQ(File inputDirectory, File preprocessedDirectory, File testDirectory, File devDirectory)
	throws ResourceInitializationException, UIMAException, IOException {

		TypeSystemDescription typeSystemDescription = 
			TypeSystemDescriptionFactory.createTypeSystemDescription();

		HashMap<File,File> splitMipacq = new HashMap<>();
		splitMipacq.put(new File(inputDirectory+"/text/train"), preprocessedDirectory);
		splitMipacq.put(new File(inputDirectory+"/text/test"),  testDirectory);
		splitMipacq.put(new File(inputDirectory+"/text/dev"),   devDirectory);
		for (File inDir : splitMipacq.keySet() ) {
			AggregateBuilder aggregate = new AggregateBuilder();

			CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
//					FilesInDirectoryCollectionReader.class,
//					typeSystemDescription,
//					"InputDirectory",
					FileTreeReader.class,
					ConfigParameterConstants.PARAM_INPUTDIR,
					inDir
					);

			// read the UMLS_CEM data from Knowtator
			AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
					MiPACQKnowtatorXMLReader.class,
					typeSystemDescription,
					MiPACQKnowtatorXMLReader.PARAM_TEXT_DIRECTORY,
					inDir
					);

			aggregate.add(goldAnnotator);
			// fill in other values that are necessary for preprocessing
			AnalysisEngineDescription preprocessAnnotator = AnalysisEngineFactory.createEngineDescription(
					"desc/analysis_engine/AttributeDiscoveryPreprocessor"
					);
			aggregate.add(preprocessAnnotator);

			if (preprocessedDirectory!=null) {
				AnalysisEngineDescription xWriter2 = AnalysisEngineFactory.createEngineDescription(
//						XmiWriterCasConsumerCtakes.class,
//						typeSystemDescription,
//						XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
						FileTreeXmiWriter.class,
						ConfigParameterConstants.PARAM_OUTPUTDIR,
						splitMipacq.get(inDir)
						);
				aggregate.add(xWriter2);
				//		SimplePipeline.runPipeline(collectionReader, goldAnnotator, xWriter, xWriter2);
			}

			SimplePipeline.runPipeline(collectionReader, aggregate.createAggregateDescription());
		}
			
		LOGGER.info("Finished!");
	}

}
