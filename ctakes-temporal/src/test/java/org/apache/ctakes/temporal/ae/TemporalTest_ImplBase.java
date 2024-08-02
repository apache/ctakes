package org.apache.ctakes.temporal.ae;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

import java.net.MalformedURLException;

public class TemporalTest_ImplBase {
  public static AnalysisEngineDescription getTokenProcessingPipeline() throws ResourceInitializationException, MalformedURLException {
    throw new ResourceInitializationException(
          new Exception( "Use Piper files so that the module doesn't need to declare unnecessary dependencies." ) );
//    AggregateBuilder builder = new AggregateBuilder();
//    builder.add( SimpleSegmentAnnotator.createAnnotatorDescription() );
//    builder.add( SentenceDetector.createAnnotatorDescription() );
//    builder.add( TokenizerAnnotatorPTB.createAnnotatorDescription() );
////    builder.add( LvgAnnotator.createAnnotatorDescription() );
//    builder.add( ContextDependentTokenizerAnnotator.createAnnotatorDescription() );
//    builder.add( POSTagger.createAnnotatorDescription() );
//    return builder.createAggregateDescription();
 }

   /**
    *
    * @return true if the user has set their umls api key in the environment.
    */
 protected boolean canUseUmlsDictionary() {
    return System.getProperty( "UmlsKey" ) != null
          || System.getProperty( "umls_api_key" )  != null
          || System.getProperty( "ctakes.umls_apikey" ) != null;

 }

}
