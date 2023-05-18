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
package org.apache.ctakes.core.ae;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class SentenceDetectorAnnotatorTest {

  public static final String note = "" +
      "Medications:\n" +
      "Hibernol, jamitol, triopenin, sproingo\n\n" +
      "Physical exam:\n" +
      "Patient is doing fine but probably taking too many fictional drugs. Cholesterol is acceptable. Heartrate is elevated. \n" +
      "Instructions:\n" +
      "Patient should quit smoking and taunting sharks.";

  @Test
  public void testSentenceDetectorInitialization() throws UIMAException, IOException{
    
    JCas jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    SimplePipeline.runPipeline(jcas, getSegmentingPipeline());
    
    Collection<Segment> segs = JCasUtil.select(jcas, Segment.class);
    assertEquals(segs.size(), 3);
    
    // test # sentences -- if it skips MEDS and Instructions it should be 3 from the physical exam section only.
    Collection<Sentence> sents = JCasUtil.select(jcas, Sentence.class);
    assertEquals(sents.size(), 3);
    
    jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    SimplePipeline.runPipeline(jcas, getBasicPipeline());
    segs = JCasUtil.select(jcas, Segment.class);
    assertEquals(segs.size(), 1);
    
    // test # sentences -- default should be 8.
    sents = JCasUtil.select(jcas, Sentence.class);
    assertEquals(sents.size(), 8);
    
    jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    SimplePipeline.runPipeline(jcas, getUimaFitPipeline());
    // test # sentences -- default should be 8.
    sents = JCasUtil.select(jcas, Sentence.class);
    assertEquals(sents.size(), 8);
  }
  
  private static AnalysisEngineDescription getUimaFitPipeline() throws ResourceInitializationException {
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    builder.add(SentenceDetector.createAnnotatorDescription());
    return builder.createAggregateDescription();
  }

  private static AnalysisEngine getSegmentingPipeline() throws ResourceInitializationException{
    AggregateBuilder aggregateBuilder = new AggregateBuilder();

    // identify segments; use simple segment annotator on non-mayo notes
    // aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(SegmentsFromBracketedSectionTagsAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CDASegmentAnnotator.class));

    // identify sentences
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/models/sentdetect/sd-med-model.zip",
        SentenceDetector.PARAM_SEGMENTS_TO_SKIP,
        new String[]{"2.16.840.1.113883.10.20.22.2.1.1" /*Medications*/, "2.16.840.1.113883.10.20.22.2.45" /*Instructions*/}));

    return aggregateBuilder.createAggregate();
  }
  
  private static AnalysisEngine getBasicPipeline() throws ResourceInitializationException{
    AggregateBuilder aggregateBuilder = new AggregateBuilder();

    // identify segments; use simple segment annotator on non-mayo notes
    // aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(SegmentsFromBracketedSectionTagsAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));

    // identify sentences
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/models/sentdetect/sd-med-model.zip"
        ));

    return aggregateBuilder.createAggregate();
  }
}
