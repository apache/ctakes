package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/6/2016
 */
@PipeBitInfo(
      name = "Paragraph Sentence Splitter",
      description = "Re-annotates Sentences based upon existing Paragraphs, preventing a Sentence from spanning more than one Paragraph.",
      dependencies = { PipeBitInfo.TypeProduct.PARAGRAPH, PipeBitInfo.TypeProduct.SENTENCE }
)
final public class ParagraphSentenceFixer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ParagraphSentenceFixer" );


   /**
    * Where Sentence annotations and paragraph annotations overlap, Sentences are abbreviated or removed.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting Sentences overlapping Paragraphs ..." );
      final Collection<Paragraph> paragraphs = JCasUtil.select( jcas, Paragraph.class );
      if ( paragraphs == null || paragraphs.isEmpty() ) {
         LOGGER.info( "Finished Processing" );
         return;
      }
      adjustParagraphSentences( jcas, paragraphs );
      LOGGER.info( "Finished Processing" );
   }

   static private void adjustParagraphSentences( final JCas jCas, final Collection<Paragraph> paragraphs ) {
      final Collection<Sentence> allSentences = JCasUtil.select( jCas, Sentence.class );
      final Collection<Pair<Integer>> newBounds = new HashSet<>();
      // gather map of sentences that cross boundaries of lists; add list entry sentences
      final Map<Sentence, Collection<Paragraph>> boundarySentences = new HashMap<>();
      for ( Paragraph paragraph : paragraphs ) {
         for ( Sentence sentence : allSentences ) {
            if ( (sentence.getBegin() < paragraph.getBegin() && sentence.getEnd() > paragraph.getBegin())
                 || (sentence.getEnd() > paragraph.getEnd() && sentence.getBegin() < paragraph.getEnd()) ) {
               // sentence overlaps but isn't contained
               boundarySentences.computeIfAbsent( sentence, p -> new HashSet<>() ).add( paragraph );
            }
         }
      }
      // cut up the boundary sentences, paying attention to sentences that span two or more paragraphs
      for ( Map.Entry<Sentence, Collection<Paragraph>> boundarySentence : boundarySentences.entrySet() ) {
         final int sentenceBegin = boundarySentence.getKey().getBegin();
         final int sentenceEnd = boundarySentence.getKey().getEnd();
         final List<Paragraph> sorted
               = boundarySentence.getValue().stream()
                                 .sorted( Comparator.comparingInt( Paragraph::getBegin ) )
                                 .collect( Collectors.toList() );
         final Paragraph first = sorted.get( 0 );
         if ( sentenceBegin < first.getBegin() && sentenceEnd > first.getBegin() ) {
            // sentence starts before but ends in or after paragraph
            newBounds.add( new Pair<>( sentenceBegin, first.getBegin() ) );
            final int end = Math.min( sentenceEnd, first.getEnd() );
            newBounds.add( new Pair<>( first.getBegin(), end ) );
         }
         for ( int i = 0; i < sorted.size() - 1; i++ ) {
            if ( sentenceBegin > sorted.get( i ).getBegin() && sentenceEnd >= sorted.get( i ).getEnd() ) {
               // sentence starts in, ends after
               newBounds.add( new Pair<>( sentenceBegin, sorted.get( i ).getEnd() ) );
            }
            if ( sentenceBegin < sorted.get( i + 1 ).getBegin() && sentenceEnd <= sorted.get( i + 1 ).getEnd() ) {
               // sentence starts in, ends after
               newBounds.add( new Pair<>( sorted.get( i + 1 ).getBegin(), sentenceEnd ) );
            }
            if ( sorted.get( i + 1 ).getBegin() >= sorted.get( i ).getEnd() ) {
               // sentence extends between two paragraphs
               newBounds.add( new Pair<>( sorted.get( i ).getEnd(), sorted.get( i + 1 ).getBegin() ) );
            }
         }
         final Paragraph last = sorted.get( sorted.size() - 1 );
         if ( sentenceEnd >= last.getEnd() && sentenceBegin < last.getEnd() ) {
            // sentence ends after but begins in or before the paragraph
            final int begin = Math.max( last.getBegin(), sentenceBegin );
            newBounds.add( new Pair<>( begin, last.getEnd() ) );
            if ( last.getEnd() < sentenceEnd ) {
               newBounds.add( new Pair<>( last.getEnd(), sentenceEnd ) );
            }
         }
      }
      // adjust the cas
      boundarySentences.keySet().forEach( Sentence::removeFromIndexes );
      boundarySentences.keySet().forEach( jCas::removeFsFromIndexes );
      newBounds.stream()
            .filter( p -> p.getValue2() - p.getValue1() > 0 )
            .map( p -> new Sentence( jCas, p.getValue1(), p.getValue2() ) )
            .filter( notEmpty )
            .forEach( Sentence::addToIndexes );
   }

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );
   static private final Predicate<Sentence> notEmpty
         = s -> WHITESPACE.matcher( s.getCoveredText() ).replaceAll( " " ).trim().length() > 0;


}
