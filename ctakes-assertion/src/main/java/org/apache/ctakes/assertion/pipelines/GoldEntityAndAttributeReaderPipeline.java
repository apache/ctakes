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


import java.io.IOException;
import java.util.Collections;

import org.apache.ctakes.core.ae.SHARPKnowtatorXMLReader;
import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.uima.UIMAException;

/**
 * 
 * A class for testing the reader for the gold standard relation data. 
 * Currently this class runs the reader and saves the resulting annotations as xmi files.
 * 
 * @author dmitriy dligach
 *
 */
public class GoldEntityAndAttributeReaderPipeline {

	public static void main(String[] args) throws UIMAException, IOException {
		
//		TypeSystemDescription typeSystemDescription =
//				// use the uimafit method of finding available type system
//				// descriptor via META-INF/org.apache.uima.fit/types.txt
//				// (found in ctakes-type-system/src/main/resources)
//			TypeSystemDescriptionFactory.createTypeSystemDescription();
//
//		CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
//				FilesInDirectoryCollectionReader.class,
//				typeSystemDescription,
//				"InputDirectory",
////				"/Users/m081914/work/data/sharp/Seed_Corpus/Mayo/UMLS_CEM/ss1_batch10/Knowtator/text"
//				"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator/text"
//				//"/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/sandbox/batch02_mayo/text"
//				);
//
////		AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
////				GoldEntityAndAttributeReader.class,
////				typeSystemDescription,
////				"InputDirectory",
//////				"/Users/m081914/work/data/sharp/Seed_Corpus/Mayo/UMLS_CEM/ss1_batch10/Knowtator XML/");
////				"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/xml/");
//////				"/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/sandbox/batch02_mayo/knowtator/");
//
//		AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
//				SHARPKnowtatorXMLReader.class,
//				typeSystemDescription,
//				"KnowtatorURI",
//				"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator_XML",
//				"TextURI",
//				"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator/text");
//
//		AnalysisEngineDescription xWriter = AnalysisEngineFactory.createEngineDescription(
//        XmiWriterCasConsumerCtakes.class,
//        typeSystemDescription,
//        XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
////        "/Users/m081914/work/data/sharp/Seed_Corpus/Mayo/UMLS_CEM/ss1_batch10/Knowtator XMI/",
//    	"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator_XMI/"
//        // "/work/medfacts/sharp/data/2012-10-09_full_data_set/batch02",
////        "/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/sandbox/batch02_mayo/xmi",
//        );
//
//		SimplePipeline.runPipeline(collectionReader, goldAnnotator, xWriter);

		// This is much simpler than the dozen or so lines above.
		new PipelineBuilder().set( ConfigParameterConstants.PARAM_INPUTDIR,
											"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator/text" )
									.readFiles()
									.add( SHARPKnowtatorXMLReader.class,
											Collections.emptyList(),
											"KnowtatorURI",
											"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator_XML",
											"TextURI",
											"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator/text" )
									.set( ConfigParameterConstants.PARAM_OUTPUTDIR,
											"/Users/m081914/work/sharpattr/ctakes/ctakes-assertion/sharp_data/one/Knowtator_XMI" )
									.add( FileTreeXmiWriter.class )
									.run();
	}
}
