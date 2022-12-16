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
package org.apache.ctakes.temporal.eval;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.ClearTKLinkToTHYMELinkAnnotator;
import org.apache.ctakes.temporal.ae.EventToClearTKEventAnnotator;
import org.apache.ctakes.temporal.ae.TimexToClearTKTimexAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveCrossSentenceRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveEventEventRelations;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.opennlp.tools.ParserAnnotator;
import org.cleartk.opennlp.tools.PosTaggerAnnotator;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.snowball.DefaultSnowballStemmer;
import org.cleartk.timeml.event.EventAspectAnnotator;
import org.cleartk.timeml.event.EventClassAnnotator;
import org.cleartk.timeml.event.EventModalityAnnotator;
import org.cleartk.timeml.event.EventPolarityAnnotator;
import org.cleartk.timeml.event.EventTenseAnnotator;
import org.cleartk.timeml.time.TimeTypeAnnotator;
import org.cleartk.timeml.tlink.TemporalLinkEventToSameSentenceTimeAnnotator;
import org.cleartk.timeml.tlink.TemporalLinkEventToSubordinatedEventAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;

import com.google.common.base.Function;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class EvaluationOfClearTKRelations extends
    EvaluationOfTemporalRelations_ImplBase {
 
  /**
   * @param args
   * @throws Exception 
   */
  static interface EvalOptions extends Options{
    @Option(longName="iee") boolean getIgnoreEventEvent();
    @Option(longName="iet") boolean getIgnoreEventTime();
  }
  
  private boolean doEventEvent = true;
  private boolean doEventTime = true;

  
  public static void main(String[] args) throws Exception {
    EvalOptions options = CliFactory.parseArguments(EvalOptions.class, args);
    if(options.getIgnoreEventEvent() && options.getIgnoreEventTime()){
      System.err.println("Ignoring all relation types is not a valid configuration.");
      System.exit(-1);
    }
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = getTrainItems(options);
    List<Integer> testItems = getTestItems(options);
    
    EvaluationOfClearTKRelations evaluation = new EvaluationOfClearTKRelations(
        new File("target/eval/cleartk-event-time-links"),
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getSubcorpus(),
        options.getXMIDirectory());
    evaluation.setExtractEventEvent(!options.getIgnoreEventEvent());
    evaluation.setExtractEventTime(!options.getIgnoreEventTime());
    
    evaluation.prepareXMIsFor(patientSets);
    AnnotationStatistics<String> stats = evaluation.trainAndTest(trainItems, testItems);
    System.err.println(stats);
  }
  
  
  
  public EvaluationOfClearTKRelations(File baseDirectory, File rawTextDirectory,
      File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat,
      Subcorpus subcorpus,
      File xmiDirectory) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory, null, false, false, defaultParams);
  }
  
  private void setExtractEventTime(boolean eventTime) {
    this.doEventTime = eventTime;
  }

  private void setExtractEventEvent(boolean eventEvent) {
    this.doEventEvent = eventEvent;
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    // not training a model - just using the ClearTK one
  }
  
  @Override
  protected AnnotationStatistics<String> test(
      CollectionReader collectionReader, File directory) throws Exception {
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        RemoveCrossSentenceRelations.class,
        RemoveCrossSentenceRelations.PARAM_SENTENCE_VIEW,
        CAS.NAME_DEFAULT_SOFA,
        RemoveCrossSentenceRelations.PARAM_RELATION_VIEW,
        GOLD_VIEW_NAME));
    if(!this.doEventEvent){
      aggregateBuilder.add(
          AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class),
          CAS.NAME_DEFAULT_SOFA,
          GOLD_VIEW_NAME);
    }
    if(!this.doEventTime){
      aggregateBuilder.add(
          AnalysisEngineFactory.createEngineDescription(PreserveEventEventRelations.class),
          CAS.NAME_DEFAULT_SOFA,
          GOLD_VIEW_NAME);
    }
    aggregateBuilder.add(SentenceAnnotator.getDescription());
    aggregateBuilder.add(TokenAnnotator.getDescription());
    aggregateBuilder.add(PosTaggerAnnotator.getDescription());
    aggregateBuilder.add(DefaultSnowballStemmer.getDescription("English"));
    aggregateBuilder.add(ParserAnnotator.getDescription());
    aggregateBuilder.add(EventToClearTKEventAnnotator.getAnnotatorDescription());//for every cTakes eventMention, create a cleartk event
    aggregateBuilder.add(TimexToClearTKTimexAnnotator.getAnnotatorDescription());
//    aggregateBuilder.add(ClearTKDocumentCreationTimeAnnotator.getAnnotatorDescription());//for every jCAS create an empty DCT, and add it to index
    aggregateBuilder.add(EventTenseAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventtenseannotator/model.jar"));
    aggregateBuilder.add(EventAspectAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventaspectannotator/model.jar"));
    aggregateBuilder.add(EventClassAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventclassannotator/model.jar"));
    aggregateBuilder.add(EventPolarityAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventpolarityannotator/model.jar"));
    aggregateBuilder.add(EventModalityAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventmodalityannotator/model.jar"));
    aggregateBuilder.add(TimeTypeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/time/timetypeannotator/model.jar"));
//    aggregateBuilder.add(TemporalLinkEventToDocumentCreationTimeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtodocumentcreationtimeannotator/model.jar"));
//    aggregateBuilder.add(ClearTKDocTimeRelAnnotator.getAnnotatorDescription());// for every tlink, check if it cover and event, add the tlink type to the event's docTimeRel attribute

    if(this.doEventTime){
      aggregateBuilder.add(TemporalLinkEventToSameSentenceTimeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtosamesentencetimeannotator/model.jar"));
    }
    if(this.doEventEvent){
      aggregateBuilder.add(TemporalLinkEventToSubordinatedEventAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtosubordinatedeventannotator/model.jar"));
    }
    
    aggregateBuilder.add(ClearTKLinkToTHYMELinkAnnotator.getAnnotatorDescription());

    Function<BinaryTextRelation, ?> getSpan = new Function<BinaryTextRelation, HashableArguments>() {
      public HashableArguments apply(BinaryTextRelation relation) {
        return new HashableArguments(relation);
      }
    };
    Function<BinaryTextRelation, String> getOutcome = AnnotationStatistics.annotationToFeatureValue("category");
    AnnotationStatistics<String> stats = new AnnotationStatistics<>();
    for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView(GOLD_VIEW_NAME);
      JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      Collection<BinaryTextRelation> goldRelations = JCasUtil.select(
          goldView,
          BinaryTextRelation.class);
      Collection<BinaryTextRelation> systemRelations = JCasUtil.select(
          systemView,
          BinaryTextRelation.class);
      stats.add(goldRelations, systemRelations, getSpan, getOutcome);
    }
    
    return stats;
  }


}
