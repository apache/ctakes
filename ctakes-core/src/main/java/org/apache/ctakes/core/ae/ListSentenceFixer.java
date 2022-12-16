package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textspan.List;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
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
 * @since 9/28/2016
 */
@PipeBitInfo(
      name = "List Sentence Splitter",
      description = "Re-annotates Sentences based upon existing List Entries, preventing a Sentence from spanning more than one List Entry.",
      dependencies = { PipeBitInfo.TypeProduct.LIST, PipeBitInfo.TypeProduct.SENTENCE }
)
final public class ListSentenceFixer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ListSentenceFixer" );

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );

   /**
    * Where Sentence annotations and List entry annotation ends overlap, Sentences are abbreviated.
    * For each List Entry with a boundary within a Sentence, a new Sentence is created
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting Sentences overlapping Lists ..." );
      final Collection<List> lists = JCasUtil.select( jcas, List.class );
      if ( lists == null || lists.isEmpty() ) {
         LOGGER.info( "Finished Processing" );
         return;
      }
      adjustListEntrySentences( jcas );
      LOGGER.info( "Finished Processing" );
   }


   static private void adjustListEntrySentences( final JCas jCas ) {
      final Collection<ListEntry> listEntries = JCasUtil.select( jCas, ListEntry.class );
      final java.util.List<Sentence> allSentences = new ArrayList<>( JCasUtil.select( jCas, Sentence.class ) );
      allSentences.sort( Comparator.comparingInt( Annotation::getBegin ) );
      // gather map of sentences that cross boundaries of list entries
      final Map<Sentence, Collection<Integer>> sentenceCrossBounds = new HashMap<>();
      for ( ListEntry entry : listEntries ) {
         for ( Sentence sentence : allSentences ) {
            if ( sentence.getBegin() > entry.getEnd() ) {
               // past the list entry
               break;
            } else if ( sentence.getEnd() < entry.getBegin() ) {
               // haven't reached the list entry
               continue;
            } else if ( sentence.getBegin() >= entry.getBegin() && sentence.getEnd() <= entry.getEnd() ) {
               // within the list entry
               continue;
            }
            // sentence overlaps but isn't contained
            Collection<Integer> crossBounds = sentenceCrossBounds.get( sentence );
            if ( crossBounds == null ) {
               crossBounds = new HashSet<>();
               sentenceCrossBounds.put( sentence, crossBounds );
               crossBounds.add( sentence.getBegin() );
               crossBounds.add( sentence.getEnd() );
            }
            crossBounds.add( Math.max( sentence.getBegin(), entry.getBegin() ) );
            crossBounds.add( Math.min( sentence.getEnd(), entry.getEnd() ) );
         }
      }
      for ( Map.Entry<Sentence, Collection<Integer>> crossBounds : sentenceCrossBounds.entrySet() ) {
         final java.util.List<Integer> sortedBounds = new ArrayList<>( crossBounds.getValue() );
         Collections.sort( sortedBounds );
         for ( int i = 0; i < sortedBounds.size() - 1; i++ ) {
            final Sentence sentence = new Sentence( jCas, sortedBounds.get( i ), sortedBounds.get( i + 1 ) );
            if ( WHITESPACE.matcher( sentence.getCoveredText() ).replaceAll( " " ).trim().length() > 0 ) {
               sentence.addToIndexes();
            }
         }
         crossBounds.getKey().removeFromIndexes();
         jCas.removeFsFromIndexes( crossBounds.getKey() );
      }
   }

}
