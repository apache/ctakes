package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.regex.Pattern;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/29/2019
 */
@PipeBitInfo(
      name = "End of Line Sentence Splitter",
      description = "Re-annotates Sentences based upon short lines, preventing a Sentence from spanning over an intentional line break.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE }
)
final public class EolSentenceFixer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LoggerFactory.getLogger( "EolSentenceFixer" );

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );

   /**
    * Where Sentence annotations are short compared to the longest line, Sentences are abbreviated.
    * For each EOL boundary within a long Sentence, where the eol splits two or more lines,
    * each  much shorter than the longest line, new Sentences are created.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting Sentences for intentional line breaks ..." );
      adjustEolSentences( jcas );
      LOGGER.info( "Finished Processing" );
   }


   /**
    * @param jCas ye olde ...
    */
   static private void adjustEolSentences( final JCas jCas ) {
      // gather line breaks and lengths of first words for each line
      final char[] docChars = jCas.getDocumentText().toCharArray();
      final List<Pair<Integer>> lineBounds = new ArrayList<>();
      final List<Integer> followingWordLengths = new ArrayList<>();
      int maxLength = 0;
      int lineBegin = 0;
      boolean inBreak = false;
      boolean inFirstWord = false;
      for ( int i = 0; i < docChars.length; i++ ) {
         if ( docChars[ i ] == '\r' || docChars[ i ] == '\n' ) {
            if ( inBreak ) {
               // continue through an \r\n or multiple empty lines.
               continue;
            }
            final int length = i - lineBegin;
            if ( inFirstWord ) {
               followingWordLengths.add( length );
               inFirstWord = false;
            }
            if ( length > 0 ) {
               lineBounds.add( new Pair<>( lineBegin, i ) );
               maxLength = Math.max( length, maxLength );
            }
            inBreak = true;
            continue;
         }
         if ( inBreak ) {
            lineBegin = i;
            inBreak = false;
            inFirstWord = true;
         }
         if ( inFirstWord && (docChars[ i ] == ' ' || docChars[ i ] == '\t') && (i - lineBegin > 0) ) {
            followingWordLengths.add( i - lineBegin );
            inFirstWord = false;
         }
      }

      if ( inFirstWord ) {
         // There isn't whitespace at the end of the text
         final int length = docChars.length - lineBegin;
         followingWordLengths.add( length );
      }

      adjustEolSentences( jCas, lineBounds, followingWordLengths, maxLength );
   }

   /**
    * @param jCas                 ye olde ...
    * @param lineBounds           collection of begin, end bounds for non-empty lines in the document text
    * @param followingWordLengths collection of lengths for the first word following each line
    * @param maxLength            the length of the longest line in the document text
    */
   static private void adjustEolSentences( JCas jCas, final List<Pair<Integer>> lineBounds,
                                           final List<Integer> followingWordLengths,
                                           final int maxLength ) {
      final java.util.List<Sentence> allSentences = new ArrayList<>( JCasUtil.select( jCas, Sentence.class ) );
      allSentences.sort( Comparator.comparingInt( Annotation::getBegin ) );
      int nextLineBounds = 0;
      // gather map of sentences that cross boundaries of list entries
      final Map<Sentence, Collection<Integer>> sentenceCrossBounds = new HashMap<>();
      for ( Sentence sentence : allSentences ) {
         for ( int i = nextLineBounds; i < lineBounds.size() - 1; i++ ) {
            final Pair<Integer> lineBound = lineBounds.get( i );
            if ( lineBound.getValue2() < sentence.getBegin() ) {
               // This line ends before this sentence.
               continue;
            } else if ( lineBound.getValue2() >= sentence.getEnd() ) {
               // This line ends in the next sentence.
               nextLineBounds = i;
               break;
            }
            final int lineLength = lineBound.getValue2() - lineBound.getValue1();
            // This line ends within the current sentence.
            if ( lineLength + followingWordLengths.get( i ) < maxLength ) {
               // There is an intentional eol within the sentence.
               // Add sentence to crossBounds for removal.  Add sentence bounds and line end, plus next line begin.
               final Collection<Integer> crossBounds
                     = sentenceCrossBounds.computeIfAbsent( sentence, s -> new HashSet<>() );
               sentenceCrossBounds.put( sentence, crossBounds );
               crossBounds.add( sentence.getBegin() );
               crossBounds.add( sentence.getEnd() );
               crossBounds.add( lineBound.getValue2() );
               final Pair<Integer> nextLineBound = lineBounds.get( i + 1 );
               crossBounds.add( Math.min( sentence.getEnd(), nextLineBound.getValue1() ) );
            }
         }
      }
      for ( Map.Entry<Sentence, Collection<Integer>> crossBounds : sentenceCrossBounds.entrySet() ) {
         final java.util.List<Integer> sortedBounds = new ArrayList<>( crossBounds.getValue() );
         Collections.sort( sortedBounds );
         for ( int i = 0; i < sortedBounds.size() - 1; i++ ) {
            final String sentenceText = jCas.getDocumentText()
                                            .substring( sortedBounds.get( i ), sortedBounds.get( i + 1 ) );
            if ( WHITESPACE.matcher( sentenceText ).replaceAll( " " ).trim().length() > 0 ) {
               final Sentence sentence = new Sentence( jCas, sortedBounds.get( i ), sortedBounds.get( i + 1 ) );
               sentence.addToIndexes();
            }
         }
         crossBounds.getKey().removeFromIndexes();
         jCas.removeFsFromIndexes( crossBounds.getKey() );
      }
   }


}
