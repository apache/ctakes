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
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.temporary.assertion.AssertionCuePhraseAnnotation;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.TypePathExtractor;

public class CueWordTest
{
  Logger logger = Logger.getLogger(CueWordTest.class.getName());

  /**
   * @param args
   * @throws IOException 
   * @throws UIMAException 
   */
  public static void main(String[] args) throws UIMAException, IOException
  {
    CueWordTest t = new CueWordTest();
    t.execute();
  }
  
  public void execute() throws UIMAException, IOException
  {
    logger.info("starting");

    AggregateBuilder builder = new AggregateBuilder();
    
    TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription();
    String filename = "/work/medfacts/sharp/data/2013-01-11_cue_phrase_feature_test/ON03FP00037D00207__merged.txt.xmi";
    
    JCas jcas = JCasFactory.createJCas(filename, typeSystemDescription);
    
    logger.info("=====");

    Collection<BaseToken> tokens = JCasUtil.select(jcas,  BaseToken.class);
    for (BaseToken currentToken : tokens)
    {
      logger.info(String.format("token \"%s\" [%s]", currentToken.getCoveredText(), currentToken.getClass().getName()));
    }
    
    logger.info("=====");

    Map<IdentifiedAnnotation, Collection<Sentence>> entityToSentenceMap =
        JCasUtil.indexCovering(jcas, IdentifiedAnnotation.class, Sentence.class);

    Map<Sentence, Collection<AssertionCuePhraseAnnotation>>
      sentenceToCoveredCuePhraseMap =
        JCasUtil.indexCovered(jcas, Sentence.class, AssertionCuePhraseAnnotation.class);
    
    CombinedExtractor1 baseExtractorCuePhraseCategory =
        new CombinedExtractor1
          (
           new CoveredTextExtractor(),
           new TypePathExtractor(AssertionCuePhraseAnnotation.class, "cuePhrase"),
           new TypePathExtractor(AssertionCuePhraseAnnotation.class, "cuePhraseCategory"),
           new TypePathExtractor(AssertionCuePhraseAnnotation.class, "cuePhraseAssertionFamily")
          );
    
    CleartkExtractor cuePhraseInWindowExtractor =
        new CleartkExtractor(
              AssertionCuePhraseAnnotation.class,
              baseExtractorCuePhraseCategory,
              new CleartkExtractor.Bag(new CleartkExtractor.Preceding(5)),
              new CleartkExtractor.Bag(new CleartkExtractor.Following(5))
              );
//              new CleartkExtractor.Ngram(new CleartkExtractor.Preceding(5), new CleartkExtractor.Following(5)));
              //new CoveredTextExtractor(),
//              new CleartkExtractor.Covered());
//              new CleartkExtractor.Preceding(5),
//              new CleartkExtractor.Following(5));
    
    List<CleartkExtractor> extractorList = new ArrayList<CleartkExtractor>();
    extractorList.add(cuePhraseInWindowExtractor);
    
    //NamingExtractor cuePhraseInWindowNamingExtractor = new NamingExtractor("cuePhraseCategory__", cuePhraseInWindowExtractor); 

    Collection<IdentifiedAnnotation> identifiedAnnotations = JCasUtil.select(jcas,  IdentifiedAnnotation.class);
    for (IdentifiedAnnotation current : identifiedAnnotations)
    {
      if (!(current instanceof EntityMention) && !(current instanceof EventMention)) continue;
      
      // otherwise current is an entity or event mention...
      logger.info(String.format("identified annotation (event or entity) [%d-%d] \"%s\" [%s]", current.getBegin(), current.getEnd(), current.getCoveredText(), current.getClass().getName()));
      
      Collection<Sentence> coveringSentences = entityToSentenceMap.get(current);
      if (coveringSentences == null || coveringSentences.isEmpty())
      {
        logger.info("no covering sentences found!!! continuing with next entity/event...");
        continue;
      }
      logger.info(String.format("covering sentence count: %d", coveringSentences.size()));
      Sentence firstCoveringSentence = coveringSentences.iterator().next();
      
      logger.info(String.format(
          "first covering sentence: [%d-%d] \"%s\" (%s)", 
          firstCoveringSentence.getBegin(), firstCoveringSentence.getEnd(),
          firstCoveringSentence.getCoveredText(),
          firstCoveringSentence.getClass().getName()));
      
      List<Feature> cuePhraseFeatures =
          //cuePhraseInSentenceExtractor.extract(jcas, firstCoveringSentence);
          cuePhraseInWindowExtractor.extractWithin(jcas, current, firstCoveringSentence);
          //cuePhraseInWindowNamingExtractor.extract(jcas, current);
      if (cuePhraseFeatures != null && !cuePhraseFeatures.isEmpty())
      {
        String featureDebugString = (cuePhraseFeatures == null) ? "(no cue phrase features)" : cuePhraseFeatures.toString();
        logger.info("### cue phrase features: " + featureDebugString);
      }
    }
    
    logger.info("=====");
     
    logger.info("finished");
  }

}
