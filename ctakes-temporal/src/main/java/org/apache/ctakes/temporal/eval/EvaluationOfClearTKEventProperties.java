/**
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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.lexicalscope.jewel.cli.CliFactory;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.ClearTKDocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.ClearTKDocumentCreationTimeAnnotator;
import org.apache.ctakes.temporal.ae.EventToClearTKEventAnnotator;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.opennlp.tools.ParserAnnotator;
import org.cleartk.opennlp.tools.PosTaggerAnnotator;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.snowball.DefaultSnowballStemmer;
import org.cleartk.timeml.event.*;
import org.cleartk.timeml.tlink.TemporalLinkEventToDocumentCreationTimeAnnotator;
import org.cleartk.token.tokenizer.TokenAnnotator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class EvaluationOfClearTKEventProperties extends
    Evaluation_ImplBase<Map<String, AnnotationStatistics<String>>> {

  private static final String DOC_TIME_REL = "docTimeRel";

  private static final List<String> PROPERTY_NAMES = Arrays.asList(DOC_TIME_REL);

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = getTrainItems(options);
    List<Integer> testItems = getTestItems(options);
    
    EvaluationOfClearTKEventProperties evaluation = new EvaluationOfClearTKEventProperties(
        new File("target/eval/event-properties"),
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getSubcorpus(),
        options.getXMIDirectory());
    evaluation.prepareXMIsFor(patientSets);
    evaluation.logClassificationErrors(new File("target/eval"), "ctakes-event-property-errors");
    Map<String, AnnotationStatistics<String>> stats = evaluation.trainAndTest(trainItems, testItems);
    for (String name : PROPERTY_NAMES) {
      System.err.println("====================");
      System.err.println(name);
      System.err.println("--------------------");
      System.err.println(stats.get(name));
    }
  }

  private Map<String, Logger> loggers = Maps.newHashMap();
  
  public EvaluationOfClearTKEventProperties(
      File baseDirectory,
      File rawTextDirectory,
      File xmlDirectory,
      XMLFormat xmlFormat,
      Subcorpus subcorpus,
      File xmiDirectory) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory, null);
    for (String name : PROPERTY_NAMES) {
      this.loggers.put(name, Logger.getLogger(String.format("%s.%s", this.getClass().getName(), name)));
    }
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory) throws Exception {
	  AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
	  aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class));
	  aggregateBuilder.add(CopyFromGold.getDescription(EventProperties.class));
	  SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
  }

  @Override
  protected Map<String, AnnotationStatistics<String>> test(
		  CollectionReader collectionReader,
		  File directory) throws Exception {
	  AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
	  aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class));
	  aggregateBuilder.add(CopyFromGold.getDescription(TimeMention.class));
	  aggregateBuilder.add(SentenceAnnotator.getDescription());
	  aggregateBuilder.add(TokenAnnotator.getDescription());
	  aggregateBuilder.add(PosTaggerAnnotator.getDescription());
	  aggregateBuilder.add(DefaultSnowballStemmer.getDescription("English"));
	  aggregateBuilder.add(ParserAnnotator.getDescription());
	  aggregateBuilder.add(EventToClearTKEventAnnotator.getAnnotatorDescription());//for every cTakes eventMention, create a cleartk event
	  aggregateBuilder.add(ClearTKDocumentCreationTimeAnnotator.getAnnotatorDescription());//for every jCAS create an empty DCT, and add it to index
	  aggregateBuilder.add(EventTenseAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventtenseannotator/model.jar"));
	  aggregateBuilder.add(EventAspectAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventaspectannotator/model.jar"));
	  aggregateBuilder.add(EventClassAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventclassannotator/model.jar"));
	  aggregateBuilder.add(EventPolarityAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventpolarityannotator/model.jar"));
	  aggregateBuilder.add(EventModalityAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventmodalityannotator/model.jar"));
	  aggregateBuilder.add(TemporalLinkEventToDocumentCreationTimeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtodocumentcreationtimeannotator/model.jar"));
	  aggregateBuilder.add(ClearTKDocTimeRelAnnotator.getAnnotatorDescription());// for every tlink, check if it cover and event, add the tlink type to the event's docTimeRel attribute

    Function<EventMention, ?> eventMentionToSpan = AnnotationStatistics.annotationToSpan();
    Map<String, Function<EventMention, String>> propertyGetters;
    propertyGetters = new HashMap<>();
    for (String name : PROPERTY_NAMES) {
      propertyGetters.put(name, getPropertyGetter(name));
    }

    Map<String, AnnotationStatistics<String>> statsMap = new HashMap<>();
    statsMap.put(DOC_TIME_REL, new AnnotationStatistics<String>());
    for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView(GOLD_VIEW_NAME);
      JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      String text = goldView.getDocumentText();
      for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
        if (!THYMEData.SEGMENTS_TO_SKIP.contains(segment.getId())) {
          List<EventMention> goldEvents = selectExact(goldView, EventMention.class, segment);
          List<EventMention> systemEvents = selectExact(systemView, EventMention.class, segment);
          for (String name : PROPERTY_NAMES) {
            Function<EventMention, String> getProperty = propertyGetters.get(name);
            statsMap.get(name).add(
                goldEvents,
                systemEvents,
                eventMentionToSpan,
                getProperty);
            for (int i = 0; i < goldEvents.size(); ++i) {
              String goldOutcome = getProperty.apply(goldEvents.get(i));
              if ( i == systemEvents.size()){
            	  break;
              }
              String systemOutcome = getProperty.apply(systemEvents.get(i));
              if (!goldOutcome.equals(systemOutcome)) {
                EventMention event = goldEvents.get(i);
                int begin = event.getBegin();
                int end = event.getEnd();
                int windowBegin = Math.max(0, begin - 50);
                int windowEnd = Math.min(text.length(), end + 50);
                this.loggers.get(name).fine(String.format(
                    "%s was %s but should be %s, in  ...%s[!%s!]%s...",
                    name,
                    systemOutcome,
                    goldOutcome,
                    text.substring(windowBegin, begin).replaceAll("[\r\n]", " "),
                    text.substring(begin, end),
                    text.substring(end, windowEnd).replaceAll("[\r\n]", " ")));
              }
            }
          }
        }
      }
    }
    return statsMap;
  }
  
  public void logClassificationErrors(File outputDir, String outputFilePrefix) throws IOException {
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    for (String name : PROPERTY_NAMES) {
      Logger logger = this.loggers.get(name);
      logger.setLevel(Level.FINE);
      File outputFile = new File(outputDir, String.format("%s.%s.log", outputFilePrefix, name));
      FileHandler handler = new FileHandler(outputFile.getPath());
      handler.setFormatter(new Formatter() {
        @Override
        public String format(LogRecord record) {
          return record.getMessage() + '\n';
        }
      });
      logger.addHandler(handler);
    }
  }

  private static Function<EventMention, String> getPropertyGetter(final String propertyName) {
    return new Function<EventMention, String>() {
      @Override
      public String apply(EventMention eventMention) {
        EventProperties eventProperties = eventMention.getEvent().getProperties();
        Feature feature = eventProperties.getType().getFeatureByBaseName(propertyName);
        return eventProperties.getFeatureValueAsString(feature);
      }
    };
  }

   @PipeBitInfo(
         name = "Event Property Clearer",
         description = "Clears all event properties.",
         role = PipeBitInfo.Role.SPECIAL
   )
  public static class ClearEventProperties extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      for (EventProperties eventProperties : JCasUtil.select(jCas, EventProperties.class)) {
        eventProperties.setAspect(null);
        eventProperties.setCategory(null);
        eventProperties.setContextualAspect(null);
        eventProperties.setContextualModality(null);
        eventProperties.setDegree(null);
        eventProperties.setDocTimeRel(null);
        eventProperties.setPermanence(null);
        eventProperties.setPolarity(0);
      }
    }

  }
}
