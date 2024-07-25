package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textspan.List;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
      name = "List Paragraph Fixer",
      description = "Re-annotates Paragraphs based upon existing Lists, preventing a Paragraph from spanning more than one List.",
      dependencies = { PipeBitInfo.TypeProduct.LIST, PipeBitInfo.TypeProduct.SENTENCE }
)
final public class ListParagraphFixer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LogManager.getLogger( "ListSentenceFixer" );

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
      adjustListParagraphs( jcas );
      LOGGER.info( "Finished Processing" );
   }


   static private void adjustListParagraphs( final JCas jCas ) {
      final Collection<List> lists = JCasUtil.select( jCas, List.class );
      final java.util.List<Paragraph> allParagraphs = new ArrayList<>( JCasUtil.select( jCas, Paragraph.class ) );
      allParagraphs.sort( Comparator.comparingInt( Annotation::getBegin ) );
      // gather map of paragraphs that cross boundaries of lists
      final Map<Paragraph, Collection<Integer>> paragraphCrossBounds = new HashMap<>();
      for ( List list : lists ) {
         for ( Paragraph paragraph : allParagraphs ) {
            if ( paragraph.getBegin() > list.getEnd() ) {
               // past the list entry
               break;
            } else if ( paragraph.getEnd() < list.getBegin() ) {
               // haven't reached the list entry
               continue;
            } else if ( paragraph.getBegin() >= list.getBegin() && paragraph.getEnd() <= list.getEnd() ) {
               // within the list entry
               continue;
            }
            // sentence overlaps but isn't contained
            Collection<Integer> crossBounds = paragraphCrossBounds.get( paragraph );
            if ( crossBounds == null ) {
               crossBounds = new HashSet<>();
               paragraphCrossBounds.put( paragraph, crossBounds );
               crossBounds.add( paragraph.getBegin() );
               crossBounds.add( paragraph.getEnd() );
            }
            crossBounds.add( Math.max( paragraph.getBegin(), list.getBegin() ) );
            crossBounds.add( Math.min( paragraph.getEnd(), list.getEnd() ) );
         }
      }
      for ( Map.Entry<Paragraph, Collection<Integer>> crossBounds : paragraphCrossBounds.entrySet() ) {
         final java.util.List<Integer> sortedBounds = new ArrayList<>( crossBounds.getValue() );
         Collections.sort( sortedBounds );
         for ( int i = 0; i < sortedBounds.size() - 1; i++ ) {
            final Paragraph paragraph = new Paragraph( jCas, sortedBounds.get( i ), sortedBounds.get( i + 1 ) );
            if ( WHITESPACE.matcher( paragraph.getCoveredText() ).replaceAll( " " ).trim().length() > 0 ) {
               paragraph.addToIndexes();
            }
         }
         crossBounds.getKey().removeFromIndexes();
         jCas.removeFsFromIndexes( crossBounds.getKey() );
      }
   }


}
