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
package org.apache.ctakes.relationextractor.data.analysis;

import java.io.File;
import java.util.List;

import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Print gold entities and events (potential relation arguments).
 */
public class EventAndEntityViewer {
  
  static interface Options {

    @Option(
        longName = "xmi-dir",
        description = "path to xmi files containing gold annotations")
    public File getInputDirectory();
  }
  
	public static void main(String[] args) throws Exception {
		
		Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(RelationContextPrinter.class);
		SimplePipeline.runPipeline(collectionReader, annotationConsumer);
	}

  /**
   * Print gold standard entities and events.
   * 
   * @author dmitriy dligach
   */
  public static class RelationContextPrinter extends JCasAnnotator_ImplBase {
    
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      
      JCas goldView;
      try {
        goldView = jCas.getView("GoldView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }
      
      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      // iterate over sentences in case the context is needed
      for(Sentence sentence : JCasUtil.select(systemView, Sentence.class)) {
        List<EventMention> events = JCasUtil.selectCovered(goldView, EventMention.class, sentence);
        List<EntityMention> entities = JCasUtil.selectCovered(goldView, EntityMention.class, sentence);
                
      }
    }
  }
}
