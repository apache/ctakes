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
/*

* Copyright: (c) 2012  Children's Hospital Boston
*
* Except as contained in the copyright notice above, or as used to identify
* MFMER as the author of this software, the trade names, trademarks, service
* marks, or product names of the copyright holder shall not be used in
* advertising, promotion or otherwise in connection with this software without
* prior written authorization of the copyright holder.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
* @author Dmitriy Dligach
*/
package org.apache.ctakes.relationextractor.pipelines;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

/**
 * Run relation extraction AE on a single sentence.
 * 
 * @author dmitriy dligach
 *
 */
public class RelationExtractorPipelineSingleCas {

		public static void main(String[] args) throws UIMAException, IOException {

			String sampleSentence = "He still is not able to work because of severe pain involving his wrists.";
			
			JCas jCas = JCasFactory.createJCas();
			jCas.setDocumentText(sampleSentence);

			AnalysisEngine relationExtractor = AnalysisEngineFactory.createEngineFromPath(
					"desc/analysis_engine/RelationExtractorAggregate.xml");

		  AnalysisEngine relationConsumer = AnalysisEngineFactory.createEngine(
	    		RelationExtractorConsumer.class);

			SimplePipeline.runPipeline(jCas, relationExtractor, relationConsumer);
		}
}
