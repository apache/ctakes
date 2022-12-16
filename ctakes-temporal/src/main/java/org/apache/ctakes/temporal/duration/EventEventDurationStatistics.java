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
package org.apache.ctakes.temporal.duration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Analyze duration information for the relation arguments of CONTAINS relation.
 * 
 * @author dmitriy dligach
 */
public class EventEventDurationStatistics {

  static interface Options {

    @Option(longName = "xmi-dir")
    public File getInputDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "output-file")
    public File getOutputFile();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);

    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
    List<File> trainFiles = Utils.getFilesFor(trainItems, options.getInputDirectory());
    CollectionReader collectionReader = Utils.getCollectionReader(trainFiles);

    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(
        AnalyseRelationArgumentDuration.class,
        "OutputFile",
        options.getOutputFile());

    SimplePipeline.runPipeline(collectionReader, annotationConsumer);
  }

  /**
   * Look at event-event relations whose event arguments have duration data.
   */
  @PipeBitInfo(
        name = "E-E Duration Computer",
        description = "Writes a file with durations of Contains Event-Event temporal relations.",
        role = PipeBitInfo.Role.SPECIAL,
        dependencies = { PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
  )
  public static class AnalyseRelationArgumentDuration extends JCasAnnotator_ImplBase {

    @ConfigurationParameter(
        name = "OutputFile",
        mandatory = true,
        description = "path to the file that stores relation data")
    private String outputFile;

    public static final String GOLD_VIEW_NAME = "GoldView";

    @Override                                                                                                                  
    public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     

      File durationLookup = new File(Utils.durationDistributionPath);                      
      Map<String, Map<String, Float>> textToDistribution = null;                                                                 
      try {                                                                                                                      
        textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Utils.Callback());                                    
      } catch(IOException e) {                                                                                                   
        e.printStackTrace();                                                                                                     
        return;                                                                                                                  
      }  

      JCas goldView;                                                                                                           
      try {                                                                                                                    
        goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
      } catch (CASException e) {                                                                                               
        throw new AnalysisEngineProcessException(e);                                                                           
      }                                                                                                                                                                                                                                         

      // find event-time relations where both arguments have duration information
      for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {
        if(! relation.getCategory().equals("CONTAINS")) {
          continue;
        }
        
        RelationArgument arg1 = relation.getArg1();                                                                             
        RelationArgument arg2 = relation.getArg2(); 
        String event1Text;
        String event2Text;
        if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof EventMention) {
          event1Text = Utils.normalizeEventText(jCas, arg1.getArgument());
          event2Text = Utils.normalizeEventText(jCas, arg2.getArgument());
        } else {
          // this is not an event-event relation
          continue;
        }

        if(textToDistribution.containsKey(event1Text) && textToDistribution.containsKey(event2Text)) {
          // there is duration information for both arguments
          float event1ExpectedDuration = Utils.expectedDuration(textToDistribution.get(event1Text));
          float event2ExpectedDuration = Utils.expectedDuration(textToDistribution.get(event2Text));
          String context = Utils.getTextBetweenAnnotations(goldView, arg1.getArgument(), arg2.getArgument());
          String out = String.format("%s|%.5f|%s|%.5f|%s\n", 
              event1Text, event1ExpectedDuration * 3650, 
              event2Text, event2ExpectedDuration * 3650, 
              context.length() < 80 ? context : "...");
          try {
            Files.append(out, new File(outputFile), Charsets.UTF_8);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
