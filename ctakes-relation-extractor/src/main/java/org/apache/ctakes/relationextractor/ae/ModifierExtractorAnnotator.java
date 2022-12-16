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
package org.apache.ctakes.relationextractor.ae;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.Modifier;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.chunking.Chunking;

@PipeBitInfo(
      name = "Modifier Extractor",
      description = "Annotates Modifiers and Chunks.",
      dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN, PipeBitInfo.TypeProduct.SENTENCE  },
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.CHUNK }
)
public class ModifierExtractorAnnotator extends CleartkAnnotator<String> {

  public static AnalysisEngineDescription getDescription(Object... additionalConfiguration)
      throws ResourceInitializationException {
    AnalysisEngineDescription desc = AnalysisEngineFactory.createEngineDescription(ModifierExtractorAnnotator.class);
    if (additionalConfiguration.length > 0) {
      ConfigurationParameterFactory.addConfigurationParameters(desc, additionalConfiguration);
    }
    return desc;
  }

  private Chunking<String, BaseToken, Modifier> chunking;

  private int nPreviousClassifications = 2;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    RelationExtractorAnnotator.allowClassifierModelOnClasspath(context);
    super.initialize(context);
    this.chunking = new BioChunking<BaseToken, Modifier>(BaseToken.class, Modifier.class, "typeID");
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
      List<BaseToken> tokens = new ArrayList<>();
      for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, sentence)){
        if(token instanceof NewlineToken) continue;
        tokens.add(token);
      }

      // during training, the list of all outcomes for the tokens
      List<String> outcomes;
      if (this.isTraining()) {
        List<Modifier> modifiers = JCasUtil.selectCovered(jCas, Modifier.class, sentence);
        outcomes = this.chunking.createOutcomes(jCas, tokens, modifiers);
      }

      // during prediction, the list of outcomes predicted so far
      else {
        outcomes = new ArrayList<String>();
      }

      // one classification instance per token
      int outcomeIndex = -1;
      for (BaseToken token : tokens) {
        ++outcomeIndex;

        // extract token features
        List<Feature> features = new ArrayList<Feature>();
        features.add(new Feature(token.getCoveredText()));
        features.add(new Feature("PartOfSpeech", token.getPartOfSpeech()));

        // extract previous classification features
        for (int i = this.nPreviousClassifications; i > 0; --i) {
          int index = outcomeIndex - i;
          String previousOutcome = index < 0 ? "O" : outcomes.get(index);
          features.add(new Feature("PreviousOutcome_" + i, previousOutcome));
        }

        // extract length of Modifier that is currently being created (if any)
        // int length = 0;
        // for (int i = outcomeIndex - 1; i > 0 && !"O".equals(outcomes.get(i)); --i) {
        //   ++length;
        // }
        // features.add(new Feature("CurrentModifierLength", length));

        // if training, write to data file
        if (this.isTraining()) {
          String outcome = outcomes.get(outcomeIndex);
          this.dataWriter.write(new Instance<String>(outcome, features));
        }

        // if predicting, add prediction to outcomes
        else {
          outcomes.add(this.classifier.classify(features));
        }
      }

      // convert classifications to Modifiers
      if (!this.isTraining()) {
        // TODO: don't just create Modifiers, create the XXXModifier subtypes
        this.chunking.createChunks(jCas, tokens, outcomes);
      }
    }

  }

}
