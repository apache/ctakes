package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/15/2020
 */
@PipeBitInfo(
      name = "MrsDrSentenceJoiner",
      description = "Joins Sentences with person titles Mr. Mrs. Dr. that have been split by SentenceDetectorBIO.",
      dependencies = PipeBitInfo.TypeProduct.SENTENCE,
      role = PipeBitInfo.Role.SPECIAL
)
final public class MrsDrSentenceJoiner extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LoggerFactory.getLogger( "MrsDrSentenceJoiner" );

   /**
    * Joins Sentences with person titles Mr. Mrs. Dr. that have been split by SentenceDetectorBIO.
    * Note : this does not take into account Sections or Paragraphs.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Joining Sentences ending with person titles (Mr. Mrs. Dr.) ..." );
      // Obtain a list of Sentences, sorted by character offset.
      final List<Sentence> sentences
            = JCasUtil.select( jCas, Sentence.class ).stream()
                      .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                      .collect( Collectors.toList() );
      // Account for (intentional?) EOL after person title.
      final Collection<Integer> newlines = new HashSet<>();
      final char[] chars = jCas.getDocumentText().toCharArray();
      for ( int i = 0; i < chars.length; i++ ) {
         if ( chars[ i ] == '\r' || chars[ i ] == '\n' ) {
            newlines.add( i );
         }
      }
      final int sentenceCount = sentences.size();
      final Collection<Sentence> removalSentences = new HashSet<>();
      boolean appendNextSentence = false;
      int appendSentenceBegin = 0;
      for ( int i = 0; i < sentenceCount; i++ ) {
         final Sentence sentence = sentences.get( i );
         final String text = sentence.getCoveredText();
         if ( ( text.endsWith( " Mr." ) || text.endsWith( " Mrs." ) || text.endsWith( " Dr." )
                || text.endsWith( " St." )
                || text.endsWith( " a.m." ) || text.endsWith( " p.m." )
                || text.endsWith( "\nMr." ) || text.endsWith( "\nMrs." ) || text.endsWith( "\nDr." )
                || text.endsWith( "\na.m." ) || text.endsWith( "\np.m." )
                || text.equals( "Mr." ) || text.equals( "Mrs." ) || text.equals( "Dr." )
                || text.equals( "a.m." ) || text.equals( "p.m." ) )
              && i < sentenceCount - 1
              && !newlines.contains( sentence.getEnd() ) ) {
            // Sentence ends with a person title AND is not the last sentence in the document.
            if ( !appendNextSentence ) {
               appendSentenceBegin = sentence.getBegin();
            }
            removalSentences.add( sentence );
            appendNextSentence = true;
            continue;
         }
         if ( appendNextSentence ) {
            // Create a new sentence that spans this sentence plus the preceding sentence(s) ending with a title.
            final Sentence newSentence = new Sentence( jCas, appendSentenceBegin, sentence.getEnd() );
            newSentence.addToIndexes();
            removalSentences.add( sentence );
            appendNextSentence = false;
         }
      }
      // Get rid of all of the original sentences that are now covered by new sentences.
      removalSentences.forEach( Sentence::removeFromIndexes );
      // Just in case, set sentence numbers.
      final AtomicInteger index = new AtomicInteger( 0 );
      JCasUtil.select( jCas, Sentence.class ).stream()
              .sorted( Comparator.comparingInt( Annotation::getBegin ) )
              .forEach( s -> s.setSentenceNumber( index.incrementAndGet() ) );
   }

   static private boolean isCM( final String text ) {
      if ( text.length() > 4 && (text.endsWith( "CM." ) || text.endsWith( "cm." ) ) ) {
         if ( Character.isDigit( text.charAt( text.length()-4 ) ) ) {
            return true;
         }
         return Character.isDigit( text.charAt( text.length()-5 ) );
      }
      return false;
   }


}
