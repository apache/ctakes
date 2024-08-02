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
package org.apache.ctakes.assertion.pipelines;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

import java.net.MalformedURLException;

//import org.apache.ctakes.lvg.ae.LvgAnnotator;

/**
 * @deprecated Use a piper or a PipelineBuilder within your own code.  This required unnecessary dependencies.
 * Meant to replace the XML descriptors in the preprocessing pipeline, making it easier
 * to access with API calls from outside projects that want to have the same preprocessing
 * workflow as the standard training scripts.
 */
@Deprecated
public class PreprocessingPipeline {
    // This method gives a UimaFit version of the preprocessing pipeline that
    // is otherwise in an xml descriptor file:
    public static AnalysisEngineDescription getPreprocessingDescription() throws ResourceInitializationException, MalformedURLException {
        throw new ResourceInitializationException( new Exception( "This class has been deprecated." ) );
//        AggregateBuilder builder = new AggregateBuilder();
//        builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
//        builder.add(SentenceDetector.createAnnotatorDescription());
//        builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
////        builder.add(LvgAnnotator.createAnnotatorDescription());
//        builder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
//        builder.add(POSTagger.createAnnotatorDescription());
//        builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
//        builder.add(ConstituencyParser.createAnnotatorDescription());

//        return builder.createAggregateDescription();
    }

    // This method is more appropriate for preprocessing of texts for which
    // higher-level syntactic features won't be used -- those that differ
    // from the domains which trained those models, or perhaps have another
    // language.
    public static AnalysisEngineDescription getTokenPreprocessingDescription() throws ResourceInitializationException, MalformedURLException {
        throw new ResourceInitializationException( new Exception( "This class has been deprecated." ) );
//        AggregateBuilder builder = new AggregateBuilder();
//        builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
//        builder.add(SentenceDetector.createAnnotatorDescription());
//        builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
//        builder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
//        builder.add(POSTagger.createAnnotatorDescription());
//        return builder.createAggregateDescription();
    }
}
