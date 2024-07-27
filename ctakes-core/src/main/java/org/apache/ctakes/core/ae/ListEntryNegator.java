package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.List;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.LIST;

/**
 * Sets negation for disease/disorder and sign/symptom annotations in lists.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/31/2017
 */
@PipeBitInfo(
      name = "List Entry Negator",
      description = "Checks List Entries for negation, which may be exhibited differently from unstructured negation.",
      dependencies = { LIST, IDENTIFIED_ANNOTATION }
)
public class ListEntryNegator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LoggerFactory.getLogger( "ListEntryNegator" );

   static private final Pattern NEGATIVE_PATTERN
         = Pattern
         .compile( "(?:\\s?:\\s*)(?:NEGATIVE|(?:NO\\.?\\b)|(?:N\\/A)|NONE|(?:NOT (?:APPLICABLE|INVOLVED|SEEN|PRESENT|INDICATED|FOUND|DISCOVERED)?)|DENIE[SD])",
               Pattern.CASE_INSENSITIVE );

   /**
    * Sets negation for disease/disorder and sign/symptom annotations in lists
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting attributes within Lists ..." );
      final Collection<List> lists = JCasUtil.select( jcas, List.class );
      lists.forEach( l -> processList( jcas, l ) );
   }

   static private void processList( final JCas jCas, final AnnotationFS list ) {
      final java.util.List<ListEntry> listEntries = new ArrayList<>( JCasUtil
            .selectCovered( jCas, ListEntry.class, list ) );
      if ( listEntries.isEmpty() ) {
         return;
      }
      listEntries.sort( Comparator.comparingInt( AnnotationFS::getBegin ) );
      final java.util.List<IdentifiedAnnotation> negatables = new ArrayList<>();
      negatables.addAll( JCasUtil.selectCovered( jCas, DiseaseDisorderMention.class, list ) );
      negatables.addAll( JCasUtil.selectCovered( jCas, SignSymptomMention.class, list ) );
      negatables.sort( Comparator.comparingInt( AnnotationFS::getBegin ) );
      if ( negatables.isEmpty() ) {
         return;
      }
      processNegatables( jCas.getDocumentText(), listEntries, negatables );
   }


   static private void processNegatables( final String docText, final Iterable<ListEntry> listEntries,
                                          final java.util.List<IdentifiedAnnotation> negatables ) {
      int j = 0;
      IdentifiedAnnotation negatable = negatables.get( 0 );
      for ( ListEntry listEntry : listEntries ) {
         final int entryBegin = listEntry.getBegin();
         // find next negatable in a listEntry
         while ( negatable.getEnd() < entryBegin ) {
            j++;
            if ( j >= negatables.size() ) {
               return;
            }
            negatable = negatables.get( j );
         }
         final int entryEnd = listEntry.getEnd();
         // process all negatables in the current listEntry
         while ( negatable.getBegin() >= entryBegin && negatable.getEnd() < entryEnd ) {
            final String window = docText.substring( negatable.getEnd(), entryEnd );
            final Matcher matcher = NEGATIVE_PATTERN.matcher( window );
            if ( matcher.find() ) {
               negatable.setPolarity( CONST.NE_POLARITY_NEGATION_PRESENT );
            }
            j++;
            if ( j >= negatables.size() ) {
               return;
            }
            negatable = negatables.get( j );
         }
      }
   }

}
