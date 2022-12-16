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
package org.apache.ctakes.relationextractor.data;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * 
 * Pipeline for analyzing gold annotations.
 *  
 * @author dmitriy dligach
 *
 */
public class GoldAnnotationAnalysisPipeline {

  public static class Options {

    @Option(
        name = "--input-dir",
        usage = "specify the path to the directory containing the clinical notes to be processed",
        required = true)
    public File inputDirectory;
  }
  
	public static void main(String[] args) throws Exception {
      Options options = new Options();
      CmdLineParser parser = new CmdLineParser(options);
      parser.parseArgument(args);

      List<File> trainFiles = Arrays.asList(options.inputDirectory.listFiles());
      CollectionReader collectionReader = getCollectionReader(trainFiles);
		
      AnalysisEngine goldAnnotationStatsCalculator = AnalysisEngineFactory.createEngine(
    		GoldAnnotationStatsCalculator.class);
    		
		SimplePipeline.runPipeline(collectionReader, goldAnnotationStatsCalculator);
	}
	
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

  	// convert the List<File> to a String[]
    String[] paths = new String[items.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    // return a reader that will load each of the XMI files
    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
}
