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
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.temporal.ae.*;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.File;

public class FullTemporalExtractionPipeline extends
    TemporalExtractionPipeline_ImplBase {

  static interface FullOptions extends Options {
    @Option(
        shortName = "e",
        description = "specify the path to the directory where the trained event model is located",
        defaultValue="org/apache/ctakes/temporal/models/eventannotator/")
    public String getEventModelDirectory();
    
    @Option(
        shortName = "t",
        description = "specify the path to the directory where the trained event model is located",
        defaultValue="/org/apache/ctakes/temporal/modele/timeannotator/")
    public String getTimeModelDirectory();
    
    @Option(
        shortName = "d",
        description = "specify the path to the directory where the trained event-doctime relation model is located",
        defaultValue="/org/apache/ctakes/temporal/models/doctimerel")
    public String getDoctimerelModelDirectory();
    
    @Option(
        shortName = "r",
        description = "Specify the path to the directory where the trained event-time relation model is located",
        defaultValue="target/eval/thyme/train_and_test/event-time/")
    public String getEventTimeRelationModelDirectory();

    @Option(
        shortName = "s",
        description = "Specify the path to the directory where the trained event-event relation model is located",
        defaultValue="target/eval/thyme/train_and_test/event-event/") // add in default value once we have a satisfying trained model
    public String getEventEventRelationModelDirectory();  
    
    @Option(
        shortName = "c",
        description = "Specify the path to the directory where the trained coreference model is located",
        defaultToNull=true)
    public String getCoreferenceModelDirectory();
  }

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    FullOptions options = CliFactory.parseArguments(FullOptions.class, args);
    
    CollectionReader collectionReader = CollectionReaderFactory.createReaderFromPath(
        "../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
          ConfigParameterConstants.PARAM_INPUTDIR,
        options.getInputDirectory());

    AggregateBuilder aggregateBuilder = getPreprocessorAggregateBuilder();
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyPropertiesToTemporalEventAnnotator.class));
    aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription(options.getDoctimerelModelDirectory() + File.separator + "model.jar"));
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription(options.getTimeModelDirectory() + File.separator + "model.jar"));
    aggregateBuilder.add(EventTimeRelationAnnotator.createAnnotatorDescription(options.getEventTimeRelationModelDirectory() + File.separator + "model.jar"));
    if(options.getEventEventRelationModelDirectory()!=null){
      aggregateBuilder.add(EventEventRelationAnnotator.createAnnotatorDescription(options.getEventEventRelationModelDirectory() + File.separator + "model.jar"));
    }
//    if(options.getCoreferenceModelDirectory()!=null){
//      aggregateBuilder.add(EventCoreferenceAnnotator.createAnnotatorDescription(options.getCoreferenceModelDirectory() + File.separator + "model.jar"));
//      aggregateBuilder.add(CoreferenceChainAnnotator.createAnnotatorDescription());
//    }
    
    //aggregateBuilder.createEngineDescription().toXML(new FileWriter("desc/analysis_engine/TemporalAggregateUMLSPipeline.xml"));
    AnalysisEngine xWriter = getXMIWriter(options.getOutputDirectory());
    
    SimplePipeline.runPipeline(
        collectionReader,
        aggregateBuilder.createAggregate(),
        xWriter);
  }

  public static class CopyPropertiesToTemporalEventAnnotator extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      for(EventMention mention : JCasUtil.select(jcas, EventMention.class)){
        // get temporal event mentions and not dictinoary-derived subclasses
        // find either an exact matching span, or an end-matching span with the smallest overlap
        if(mention.getClass().equals(EventMention.class)){
          EventMention bestCovering = null;
          int smallestSpan = Integer.MAX_VALUE;
          for(EventMention covering : JCasUtil.selectCovering(EventMention.class, mention)){
            if(covering.getClass().equals(EventMention.class)) continue;
            if(covering.getBegin() == mention.getBegin() && covering.getEnd() == mention.getEnd()){
              bestCovering = covering;
              break;
            }else if(covering.getEnd() == mention.getEnd()){
              int span = covering.getEnd() - covering.getBegin();
              if(span < smallestSpan){
                span = smallestSpan;
                bestCovering = covering;
              }
            }
          }
          if(bestCovering != null){
            mention.setPolarity(bestCovering.getPolarity());
//            mention.getEvent().getProperties().setPolarity(bestCovering.getPolarity());
            mention.setUncertainty(bestCovering.getUncertainty());
          }
        }
      }
    }
  }
}
