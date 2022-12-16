package org.apache.ctakes.examples.pipeline;

import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.examples.ae.ExampleHelloWorldAnnotator;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.FileWriter;

/**
 * Build and run a pipeline using uimafit {@link AggregateBuilder}, {@link SimplePipeline} and {@link JCasUtil}
 * <p>
 * Example of a running a pipeline programatically w/o uima xml descriptor xml files
 * Adds the default Tokenization pipeline and adding the Example HelloWorld Annotator
 */

public class HelloWorldAggregatePipeline {


   public static void main( String[] args ) throws Exception {

      String note = "Hello World!";
      JCas jcas = JCasFactory.createJCas();
      jcas.setDocumentText( note );

      AggregateBuilder builder = new AggregateBuilder();
      // Get a simple pre-defined existing pipeline for Tokenization

      // You can consider using ClinicalPipelineFactory.getDefaultPipeline();
      // Which will include the UMLS Lookup, but you'll need to set your
      // -Dctakes.umlsuser and -Dctakes.umlspw in your environment
      builder.add( ClinicalPipelineFactory.getTokenProcessingPipeline() );

      //Add the new HelloWorld Example:
      builder.add( ExampleHelloWorldAnnotator.createAnnotatorDescription() );

      //Run the Aggregate Pipeline
      SimplePipeline.runPipeline( jcas, builder.createAggregateDescription() );

      //Print out the IdentifiedAnnotation objects
      for ( IdentifiedAnnotation entity : JCasUtil.select( jcas, IdentifiedAnnotation.class ) ) {
         System.out.println( "Entity: " + entity.getCoveredText() + " === Polarity: " + entity.getPolarity() );
      }

      //Example to save the Aggregate descriptor to an xml file for external
      //use such as the UIMA CVD/CPE
      if ( args.length > 0 ) {
         builder.createAggregateDescription().toXML( new FileWriter( args[ 0 ] ) );
      }
   }

}


