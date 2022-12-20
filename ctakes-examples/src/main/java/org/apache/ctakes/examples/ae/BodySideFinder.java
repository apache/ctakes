package org.apache.ctakes.examples.ae;

import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.util.annotation.ConceptBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.doc.TextBySentenceBuilder;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.BodySideModifier;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * This is just an example ae that will assign body sides to anatomic sites.
 * It will assign the closest preceding side to a site.
 * This is just an example ( including main() ) of how one can:
 * Create a pipeline with PipelineBuilder.
 * Add Sentences with TextBySentenceBuilder.
 * Create and add Annotations.
 * Fetch annotations with JCasUtil.
 * <p>
 * If you are unfamiliar with the builder pattern
 * or java streams or functional references then the flow may look a little strange,
 * but the essential ctakes-related building blocks are more straightforward.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/17/2020
 */
@PipeBitInfo(
      name = "BodySideFinder",
      description = "Assigns Body Side to Anatomic Sites.",
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class BodySideFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "BodySideFinder" );

   /**
    * Holds a umls CUI and some synonyms for body sides.
    * Has methods to detect a matching word token and create a BodySideModifier.
    */
   private enum Side {
      RIGHT( "C0205090", "right", "dextro" ),
      LEFT( "C0205091", "left", "levo" );
      final private Collection<String> _patterns;
      // Set up a builder for body side modifiers.
      final private IdentifiedAnnotationBuilder _sideBuilder
            = new IdentifiedAnnotationBuilder().creator( BodySideModifier::new );
      // Set up a builder for conceptual entities.
      final private ConceptBuilder _cuiBuilder
            = new ConceptBuilder().type( SemanticTui.T077 );

      Side( final String cui, final String... patterns ) {
         // Assign cui and preferred text to the concept builder for this side.
         _cuiBuilder.cui( cui ).preferredText( patterns[ 0 ] );
         _patterns = Arrays.asList( patterns );
      }

      boolean isMatch( final WordToken word ) {
         // The word token covers text in the document that matches one of the patterns, case-insensitive.
         return _patterns.stream()
                         .anyMatch( word.getCoveredText()::equalsIgnoreCase );
      }

      BodySideModifier createModifier( final JCas jCas, final WordToken word ) {
         // Set the span to match the word, set the concept (built with our concept builder), build the side modifier.
         return (BodySideModifier) _sideBuilder.span( word.getBegin(), word.getEnd() )
                                               .concept( _cuiBuilder.build( jCas ) )
                                               .build( jCas );
      }
   }

   /**
    * Process Sentence -by- Sentence.
    * If a sentence has anatomic site(s) and WordToken that match a body side synonym,
    * BodySideModifier(s) are created and attached to anatomic sites that follow in the sentence.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Body Side and Laterality ..." );

      final Map<Sentence, Collection<AnatomicalSiteMention>> sentenceSiteMap
            = JCasUtil.indexCovered( jCas, Sentence.class, AnatomicalSiteMention.class );

      final Map<Sentence, Collection<WordToken>> sentenceWordMap
            = JCasUtil.indexCovered( jCas, Sentence.class, WordToken.class );

      sentenceSiteMap.entrySet()
                     .stream()
                     .filter( e -> !e.getValue().isEmpty() )
                     .forEach( e -> assignSides( jCas, e.getValue(), sentenceWordMap.get( e.getKey() ) ) );

      LOGGER.info( "Finished." );
   }

   /**
    * Iterate through types of {@link}Side, calling {@link}findSide and {@link}setSide.
    */
   static private void assignSides( final JCas jCas,
                                    final Collection<AnatomicalSiteMention> sites,
                                    final Collection<WordToken> words ) {
      Arrays.stream( Side.values() )
            .map( s -> findSide( jCas, words, s ) )
            .flatMap( Collection::stream )
            .sorted( Comparator.comparingInt( Annotation::getBegin ) )
            .forEach( s -> setSide( s, sites ) );
   }

   /**
    * Iterate through WordTokens to find body sides and create BodySideModifiers representing them.
    */
   static private Collection<BodySideModifier> findSide( final JCas jCas,
                                                         final Collection<WordToken> words,
                                                         final Side side ) {
      return words.stream()
                  .filter( side::isMatch )
                  .map( w -> side.createModifier( jCas, w ) )
                  .collect( Collectors.toList() );
   }

   /**
    * Assign a side to all following sites.
    */
   static private void setSide( final BodySideModifier side,
                                final Collection<AnatomicalSiteMention> sites ) {
      sites.stream()
           .filter( s -> s.getEnd() > side.getEnd() )
           .forEach( s -> s.setBodySide( side ) );
   }


   /**
    * Demo
    */
   public static void main( final String... args ) {
      final String sentence = "He had a slight fracture in the proximal right fibula";
      final int index = sentence.indexOf( "fibula" );
      try {
         // TextBySentenceBuilder builds a jCas from appended sentences.
         // This does more than just setting the document text.
         final JCas jCas = new TextBySentenceBuilder()
               .addSentence( sentence )
               .build();
         // IdentifiedAnnotationBuilder creates an identified annotation and adds it to the cas.
         final AnatomicalSiteMention site
               = (AnatomicalSiteMention) new IdentifiedAnnotationBuilder().group( SemanticGroup.ANATOMY )
                                                                          .span( index, index + 6 )
                                                                          .build( jCas );
         // PipelineBuilder creates and can run pipelines.
         new PipelineBuilder()
               .add( TokenizerAnnotatorPTB.class )
               .add( BodySideFinder.class )
               .run( jCas );

         LOGGER.info( site.getCoveredText() + " has body side " + site.getBodySide().getCoveredText() );
      } catch ( IOException | UIMAException uE ) {
         LOGGER.error( uE.getMessage() );
      }
   }


}
