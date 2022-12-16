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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Print gold standard relations and their context.
 * Full set of relations annotated in SHARP is the following:
 * 
 * affects, causes/brings_about, complicates/disrupts, contraindicates, degree_of, diagnoses,
 * indicates, is_indicated_for, location_of, manages/treats, manifestation_of, result_of
 * 
 * @author dmitriy dligach
 */
public class GoldRelationViewer {
  
  static interface Options {

    @Option(
        longName = "xmi-dir",
        description = "path to xmi files containing gold annotations")
    public File getInputDirectory();
    
    @Option(
        longName = "relation-type",
        description = "determines which relation to view (e.g. location_of)",
        defaultValue = "all")
    public String getRelation();
    
    @Option(
        longName = "print-context",
        description = "output relation context")
    public boolean getPrintContext();
  }
  
	public static void main(String[] args) throws Exception {
		  
		Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(
        RelationContextPrinter.class,
        "RelationType",
        options.getRelation(),
        "PrintContext",
        options.getPrintContext());
		SimplePipeline.runPipeline(collectionReader, annotationConsumer);
	}

  /**
   * Print gold standard relations and their context.
   * 
   * @author dmitriy dligach
   */
  public static class RelationContextPrinter extends JCasAnnotator_ImplBase {
    
    @ConfigurationParameter(
        name = "RelationType",
        mandatory = true,
        description = "relation type whose instances will be displayed (e.g. location_of)")
    private String relationType;
    
    @ConfigurationParameter(
        name = "PrintContext",
        mandatory = true,
        description = "print relation context")
    private boolean printContext;
    
    
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
        List<Annotation> annotationsInSentence = JCasUtil.selectCovered(goldView, Annotation.class, sentence);
        for(Annotation annot1 : annotationsInSentence) {
          for(Annotation annot2 : annotationsInSentence) {
            if(annot1 == annot2) {
              continue;
            }
            BinaryTextRelation relation = relationLookup.get(Arrays.asList(annot1, annot2));
            if(relation != null) {
              if(relationType.equals("all") || relation.getCategory().equals(relationType)) {
                String text;
                if(printContext) {
                  text = String.format("%s(%s, %s)", relation.getCategory(), annot1.getCoveredText(), annot2.getCoveredText());
                } else {
                  String type1 = annot1.getClass().getSimpleName();
                  String type2 = annot2.getClass().getSimpleName();
                  String text1 = annot1.getCoveredText();
                  String text2 = annot2.getCoveredText();
                  text = String.format("%s|%s|%s|%s|%s|%s", fileName, relation.getCategory(), text1, type1, text2, type2);
                }                
                formattedRelationsInSentence.add(text);
              }
            }
          }
        }
        
        if(formattedRelationsInSentence.size() > 0) {
          if(printContext) {
            System.out.println();
            System.out.println(fileName + ": " + sentence.getCoveredText());
          }
          for(String text : formattedRelationsInSentence) {
            System.out.println(text);
          }
        }
      }
    }
  }
}
