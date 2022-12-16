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
package org.apache.ctakes.coreference.factory;

import org.apache.ctakes.coreference.ae.*;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

public class CoreferenceAnnotatorFactory {

  // This method is for the use case where an outside caller has their own types they want resolved -- say for the
  // breast cancer use case you may just want tumor, cancer, anatomical sites resolved.
  public static AnalysisEngineDescription getMentionClusterResolverDescription() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    
    // annotate every markable for "salience": how important is it to the discourse in context
    builder.add(MarkableSalienceAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/salience/model.jar"));
    
    // use the mention-cluster model with default trained model:
    builder.add(MentionClusterCoreferenceAnnotator.createAnnotatorDescription("/org/apache/ctakes/coreference/models/mention-cluster/model.jar"));
    
    return builder.createAggregateDescription();
  }
  
  // This method is a one-stop shop for default coreference resolution -- uses the default
  public static AnalysisEngineDescription getMentionClusterCoreferenceDescription() throws ResourceInitializationException {
    AggregateBuilder builder = new AggregateBuilder();

     // Add markables using syntax: (nouns and pronouns)   ; requires TerminalTreeBankNodes (from ConstituencyParser)
    builder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));

    builder.add(getMentionClusterResolverDescription());
    
    return builder.createAggregateDescription();
  }
  
  public static AnalysisEngineDescription getLegacyCoreferencePipeline() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqMarkableCreator.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqMarkableExpander.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqMarkablePairGenerator.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqSvmChainCreator.class));

    return builder.createAggregateDescription();
  }
  
  // This method will point at the method we think is most likely to be useful for callers of mixed understanding
  // who may not grok the method names for the systems named for their implementation.
  public static AnalysisEngineDescription getDefaultCoreferencePipeline() throws ResourceInitializationException {
    return getMentionClusterCoreferenceDescription();
  }
}
