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
package org.apache.ctakes.relationextractor.metastasis;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.relationextractor.data.analysis.Utils;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
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

import com.google.common.collect.Lists;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Pipeline for detecting very simple lists of anatomical sites.
 * 
 * @author dmitriy dligach
 */
public class AnatomicalSiteListExtractorPipeline {
  
  static interface Options {

    @Option(
        longName = "xmi-dir",
        description = "path to xmi files containing gold annotations")
    public File getInputDirectory();
  }
  
	public static void main(String[] args) throws Exception {
		  
		Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    AnalysisEngine listAndConjunctionAnnotator = AnalysisEngineFactory.createEngine(ListAndConjunctionAe.class);
		SimplePipeline.runPipeline(collectionReader, listAndConjunctionAnnotator);
	}

  /**
   * Implements a finate state machine for detecting 
   * extremely simple lists and conjunctions of anatomical sites.
   * 
   * E.g. CT chest, abdomen and pelvis.
   * 
   * FSA:
   * 
   * start -{anatomical site}-> anatsite1
   * start -{any other input}-> start
   * annatsite1 -{list connector}-> listconn
   * annatsite1 -{any other input}-> start 
   * listconn -{anatomical site}-> anatsite2
   * listconn -{any other input}-> start
   * anatsite2 -{list connector}-> listconn
   * anatsite2 -{any other input}-> accept
   */
  public static class ListAndConjunctionAe extends JCasAnnotator_ImplBase {

    public enum State {
      START, ANATSITE1, LISTCONN, ANATSITE2, ACCEPT
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      
      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }
      
      for(Sentence sentence : JCasUtil.select(systemView, Sentence.class)) {
        int beginOffset = -1;
        int endOffset = -1;
        State state = State.START;
        for(BaseToken input : JCasUtil.selectCovered(systemView, BaseToken.class, sentence)) {
          state = getNextState(systemView, state, input);
          if(state == State.ANATSITE1) {
            beginOffset = input.getBegin();
          } else if(state == State.ANATSITE2) {
            endOffset = input.getEnd();
          } else if(state == State.ACCEPT) {
            state = State.START;

            int begin = beginOffset - sentence.getBegin();
            int end = endOffset - sentence.getBegin();
            System.out.println(sentence.getCoveredText());
            System.out.println(sentence.getCoveredText().substring(begin, end));
            System.out.println();
          } 
        }
      }
    }

    /*
     * Compute the transition given current state and an input token.
     */
    public State getNextState(JCas systemView, State currentState, BaseToken inputToken) {

      // tokens that connect list elements
      Set<String> listConnectors = new HashSet<>(Lists.newArrayList("and", "or", ","));
      
      State nextState;
      int tokenSemType = getSemanticType(systemView, inputToken);
      String tokenText = inputToken.getCoveredText().toLowerCase();
      
      if(currentState == State.START) {
        if(tokenSemType == CONST.NE_TYPE_ID_ANATOMICAL_SITE) {
          nextState = State.ANATSITE1;
        } else {
          nextState = State.START;
        }
      } else if(currentState == State.ANATSITE1) {
        if(listConnectors.contains(tokenText)) {
          nextState = State.LISTCONN;
        } else {
          nextState = State.START;
        }
      } else if(currentState == State.LISTCONN) {
        if(tokenSemType == CONST.NE_TYPE_ID_ANATOMICAL_SITE) {
          nextState = State.ANATSITE2;
        } else {
          nextState = State.START;
        } 
      } else if(currentState == State.ANATSITE2) {
        if(listConnectors.contains(tokenText)) {
          nextState = State.LISTCONN;
        } else {
          nextState = State.ACCEPT;
        }
      } else {
        System.out.println("\nThis shouldn't happen!\n");
        nextState = State.START;
      }
        
      return nextState;
    }
    
    public int getSemanticType(JCas systemView, BaseToken baseToken) {
      
      List<IdentifiedAnnotation> coveredIdentifiedAnnotations = 
          JCasUtil.selectCovered(
              systemView, 
              IdentifiedAnnotation.class, 
              baseToken.getBegin(), 
              baseToken.getEnd());
            if(coveredIdentifiedAnnotations.size() < 1) {
        return CONST.NE_TYPE_ID_UNKNOWN; // no type
      } 
            
      return coveredIdentifiedAnnotations.get(0).getTypeID();
    }
  }
}
