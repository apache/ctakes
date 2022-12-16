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
package org.apache.ctakes.temporal.pipelines;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.cc.XmiWriterCasConsumerCtakes;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import java.io.File;

/**
 * Given a trained event extraction model, run the event extractor on files in a directory.
 * Save the resulting annotations in XMI files. 
 * 
 * @author dmitriy dligach
 */
public class EventAndTimeExtractionPipeline extends TemporalExtractionPipeline_ImplBase {
  
  static interface ETOptions extends Options{
    @Option(
        shortName = "e",
        description = "specify the path to the directory where the trained event model is located",
        defaultValue="target/eval/event-spans/train_and_test/")
    public String getEventModelDirectory();
    
    @Option(
        shortName = "t",
        description = "specify the path to the directory where the trained event model is located",
        defaultValue="target/eval/time-spans/train_and_test/BackwardsTimeAnnotator/")
    public String getTimeModelDirectory();
  }
  
	public static void main(String[] args) throws Exception {
		
		ETOptions options = CliFactory.parseArguments(ETOptions.class, args);

		CollectionReader collectionReader = CollectionReaderFactory.createReaderFromPath(
				"../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
            ConfigParameterConstants.PARAM_INPUTDIR,
            options.getInputDirectory());

		AggregateBuilder aggregateBuilder = getPreprocessorAggregateBuilder();
		aggregateBuilder.add(EventAnnotator.createAnnotatorDescription(new File(options.getEventModelDirectory())));
		aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription(options.getTimeModelDirectory() + "model.jar"));
    AnalysisEngine xWriter = AnalysisEngineFactory.createEngine(
        XmiWriterCasConsumerCtakes.class,
        XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
        options.getOutputDirectory());
		
    SimplePipeline.runPipeline(
        collectionReader,
        aggregateBuilder.createAggregate(),
        xWriter);
	}
}
