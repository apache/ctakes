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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Print event annotations and their context.
 *  
 * @author dmitriy dligach
 *
 */
public class EventPrintingPipeline {

  static interface Options {

    @Option(
        shortName = "i",
        description = "specify the path to the directory containing xmi files containing event annotations")
    public File getInputDirectory();
  }
  
	public static void main(String[] args) throws Exception {
		
		Options options = CliFactory.parseArguments(Options.class, args);

		SimplePipeline.runPipeline(
		    getCollectionReader(Arrays.asList(options.getInputDirectory().listFiles())), 
		    PrintEventAnnotations.getDescription());
	}
	
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

    String[] paths = new String[items.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
  
  public static class PrintEventAnnotations extends JCasAnnotator_ImplBase {

    public static AnalysisEngine getDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngine(PrintEventAnnotations.class);
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
     
      for(EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
        if(eventMention.getClass().getSimpleName().equals("EventMention")) {
          List<Sentence> sentences = JCasUtil.selectCovering(jCas, Sentence.class, eventMention.getBegin(), eventMention.getEnd());
          System.out.println(eventMention.getCoveredText() + "|" + sentences.get(0).getCoveredText());
        }
      }
    }
  }

}
