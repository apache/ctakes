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
package org.apache.ctakes.temporal.data.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Print gold standard relations and their context.
 * 
 * @author dmitriy dligach
 */
public class GoldRelationViewer {
  
  static interface Options {

    @Option(longName = "xmi-dir")
    public File getInputDirectory();
    
    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();
  }
  
	public static void main(String[] args) throws Exception {
		
		Options options = CliFactory.parseArguments(Options.class, args);
		
		List<Integer> patientSets = options.getPatients().getList();
		List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
		List<File> trainFiles = Utils.getFilesFor(trainItems, options.getInputDirectory());
    CollectionReader collectionReader = Utils.getCollectionReader(trainFiles);
		
    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(
    		RelationContextPrinter.class);
    		
		SimplePipeline.runPipeline(collectionReader, annotationConsumer);
	}

  /**
   * Print gold standard relations and their context.
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

      // can't iterate over binary text relations in a sentence, so need
      // a lookup from pair of annotations to binary text relation
      Map<List<Annotation>, BinaryTextRelation> relationLookup = new HashMap<>();
      for(BinaryTextRelation relation : JCasUtil.select(goldView, BinaryTextRelation.class)) {
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        relationLookup.put(Arrays.asList(arg1, arg2), relation);
      }

      File noteFile = new File(ViewUriUtil.getURI(jCas).toString());
      String fileName = noteFile.getName();
      
      for(Sentence sentence : JCasUtil.select(systemView, Sentence.class)) {
        List<String> formattedRelationsInSentence = new ArrayList<>();
        List<EventMention> eventMentionsInSentence = JCasUtil.selectCovered(goldView, EventMention.class, sentence);
        List<TimeMention> timeMentionsInSentence = JCasUtil.selectCovered(goldView, TimeMention.class, sentence);
        
        // retrieve event-time relations in this sentece
        for(EventMention eventMention : eventMentionsInSentence) {
          for(TimeMention timeMention : timeMentionsInSentence) {
            // there are relations where arg1=time and arg2=event
            BinaryTextRelation timeEventRel = relationLookup.get(Arrays.asList(timeMention, eventMention));
            if(timeEventRel != null) {
              String text = String.format("%s(%s, %s)", timeEventRel.getCategory(), timeMention.getCoveredText(), eventMention.getCoveredText());
              formattedRelationsInSentence.add(text);
            } 
            // and relations where arg1=event and arg2=time
            BinaryTextRelation eventTimeRel = relationLookup.get(Arrays.asList(eventMention, timeMention));
            if(eventTimeRel != null) {
              String text = String.format("%s(%s, %s)", eventTimeRel.getCategory(), eventMention.getCoveredText(), timeMention.getCoveredText());
              formattedRelationsInSentence.add(text);
            }
          }
        }
        
        // retrieve event-event relations in this sentence
        for(EventMention mention1 : eventMentionsInSentence) {
          for(EventMention mention2 : eventMentionsInSentence) {
            if(mention1 == mention2) {
              continue;
            }
            BinaryTextRelation relation = relationLookup.get(Arrays.asList(mention1, mention2));
            if(relation != null) {
              String text = String.format("%s(%s, %s)", relation.getCategory(), mention1.getCoveredText(), mention2.getCoveredText());
              formattedRelationsInSentence.add(text);
            }
          }
        }
        
        if(formattedRelationsInSentence.size() > 0) {
          System.out.println(fileName + ": " + sentence.getCoveredText());
          for(String text : formattedRelationsInSentence) {
            System.out.println(text);
          }
          System.out.println();
        }
      }
    }
  }
}
