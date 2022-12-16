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
package org.apache.ctakes.lvg.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

public class TestLvgAnnotator {
  public static final String note = "" +
      "Medications:\n" +
      "Hibernol, jamitol, triopenin, sproingo\n\n" +
      "Physical exam:\n" +
      "Patient is doing fine but probably taking too many fictional drugs. Cholesterol is acceptable. Heartrate is elevated. \n" +
      "Instructions:\n" +
      "Patient should quit smoking and taunting sharks.";

  @Test
  public void testLvgAnnotator() throws UIMAException, IOException {
    JCas jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    
    SimplePipeline.runPipeline(jcas, getDefaultPipeline());
    List<WordToken> tokens = new ArrayList<>(JCasUtil.select(jcas, WordToken.class));
    assertEquals("Incorrect canonical form!", "medication", tokens.get(0).getCanonicalForm());
    
    assertTrue(tokens.get(29).getCanonicalForm() == null);
    
  }
  
  public static AnalysisEngineDescription getPrerequisitePipeline() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    builder.add(SentenceDetector.createAnnotatorDescription());
    builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
    return builder.createAggregateDescription();
  }
  
  public static AnalysisEngineDescription getDefaultPipeline() throws ResourceInitializationException, MalformedURLException{
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(getPrerequisitePipeline());
    builder.add(LvgAnnotator.createAnnotatorDescription());
    return builder.createAggregateDescription();
  }
  
  
}
