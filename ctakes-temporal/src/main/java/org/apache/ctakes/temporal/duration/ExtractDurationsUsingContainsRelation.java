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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
//import org.threeten.bp.temporal.TemporalUnit;
import java.time.temporal.TemporalUnit;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;

import scala.collection.immutable.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Use event-time CONTAINS relations to extract event duration distributions.
 * 
 * @author dmitriy dligach
 */
public class ExtractDurationsUsingContainsRelation {

  static interface Options {

    @Option(
        description = "specify the path to the directory containing the xmi files")
    public File getInputDirectory();

    @Option(
        description = "specify the path to the output file")
    public File getEventOutputFile();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);

    List<File> files = Arrays.asList(options.getInputDirectory().listFiles());
    CollectionReader collectionReader = getCollectionReader(files);

    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createEngine(
        ProcessRelations.class,
        "EventOutputFile",
        options.getEventOutputFile());

    SimplePipeline.runPipeline(collectionReader, annotationConsumer);
  }

  /**
   * Go over even-time relations and collect all times with which
   * each of the events occurs in CONTAINS relation.
   */
  public static class ProcessRelations extends JCasAnnotator_ImplBase {

    @ConfigurationParameter(
        name = "EventOutputFile",
        mandatory = true,
        description = "path to the output file that will store the events")
    private String eventOutputFile;

    // map event text to time units with counts
    private Map<String, HashMultiset<String>> eventTimeUnitCount;
    
    // file to store the output
    private File outputFile;
    
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException  {
      super.initialize(context);
      eventTimeUnitCount = new HashMap<String, HashMultiset<String>>();
      outputFile = new File(eventOutputFile);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();  
      for(String key : eventTimeUnitCount.keySet()) {
        String formatted = Utils.formatDistribution(key, eventTimeUnitCount.get(key), ", ", false);
        try {
          Files.append(formatted + "\n", outputFile, Charsets.UTF_8);
        } catch (IOException e) {
          System.out.println("couldn't open output file!");
        }
      }
    }
    
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      JCas goldView;
      try {
        goldView = jCas.getView("GoldView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) { 
        if(! relation.getCategory().equals("CONTAINS")) {
          continue;
        }

        RelationArgument arg1 = relation.getArg1();                                                                             
        RelationArgument arg2 = relation.getArg2(); 
        String eventText;
        String timeText;
        if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention) {
          timeText = arg1.getArgument().getCoveredText().toLowerCase(); 
          eventText = arg2.getArgument().getCoveredText().toLowerCase();  
        } else if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention) {
          eventText = arg1.getArgument().getCoveredText().toLowerCase(); 
          timeText = arg2.getArgument().getCoveredText().toLowerCase();  
        } else {
          continue; // not an event-time relation
        }    

        // occasionally event text has a leading eol character
        eventText = eventText.startsWith("\n") ? eventText.substring(1) : eventText; 
        
        Set<TemporalUnit> units = Utils.runTimexParser(timeText);
        if(units == null) {
          continue;
        }
        
        scala.collection.Iterator<TemporalUnit> iterator = units.iterator();
        while(iterator.hasNext()) {
          TemporalUnit unit = iterator.next();
          String coarseUnit = Utils.putInBin(unit.toString());
          if(coarseUnit != null) {
            if(eventTimeUnitCount.containsKey(eventText)) {
              eventTimeUnitCount.get(eventText).add(coarseUnit);
            } else {
              HashMultiset<String> timeUnitCount = HashMultiset.create();
              eventTimeUnitCount.put(eventText, timeUnitCount);
              eventTimeUnitCount.get(eventText).add(coarseUnit);
            }
          }
        } 
      }
    }
  }

  public static CollectionReader getCollectionReader(List<File> inputFiles) throws Exception {

    List<String> fileNames = new ArrayList<String>();
    for(File file : inputFiles) {
      if(! (file.isHidden())) {
        fileNames.add(file.getPath());
      }
    }

    String[] paths = new String[fileNames.size()];
    fileNames.toArray(paths);

    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
}

