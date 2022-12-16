package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;

import java.net.MalformedURLException;

public class TemporalTest_ImplBase {
  public static AnalysisEngineDescription getTokenProcessingPipeline() throws ResourceInitializationException, MalformedURLException {
    AggregateBuilder builder = new AggregateBuilder();
    builder.add( SimpleSegmentAnnotator.createAnnotatorDescription() );
    builder.add( SentenceDetector.createAnnotatorDescription() );
    builder.add( TokenizerAnnotatorPTB.createAnnotatorDescription() );
//    builder.add( LvgAnnotator.createAnnotatorDescription() );
    builder.add( ContextDependentTokenizerAnnotator.createAnnotatorDescription() );
    builder.add( POSTagger.createAnnotatorDescription() );
    return builder.createAggregateDescription();
 }

}
