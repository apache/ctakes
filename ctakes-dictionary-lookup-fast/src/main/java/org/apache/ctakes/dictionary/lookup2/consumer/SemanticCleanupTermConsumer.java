package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.*;

/**
 * Refine a collection of dictionary terms to only contain the most specific variations:
 * "colon cancer" instead of "cancer", performed by span inclusion / complete containment, not overlap
 * Also a start at wsd by trim of overlapping terms of conflicting but related semantic group.
 * In this incarnation, any sign / symptom that is within a disease / disorder is assumed to be
 * less specific than the disease disorder and is discarded.
 * In addition, any s/s or d/d that has the same span as an anatomical site is discarded.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/24/2014
 */
public class SemanticCleanupTermConsumer extends AbstractTermConsumer {

   static private final Logger LOGGER = Logger.getLogger( "SemanticCleanupTermConsumer" );

   private final TermConsumer _idHitConsumer;

   public SemanticCleanupTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      super( uimaContext, properties );
      _idHitConsumer = new PrecisionTermConsumer( uimaContext, properties );
   }


   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "colon cancer" instead of "cancer", performed by span inclusion /complete containment, not overlap.
    * For instance:
    * "54 year old woman with left breast cancer."
    * in the above sentence, "breast" as part of "breast cancer" is an anatomical site and should not be a S/S
    * "Breast:
    * "lump, cyst"
    * in the above, breast is a list header, denoting findings on exam.
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas,
                            final RareWordDictionary dictionary,
                            final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                            final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {
      final String codingScheme = getCodingScheme();
      final Collection<Integer> usedcTakesSemantics = getUsedcTakesSemantics( cuiConcepts );
      final Map<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> groupedSemanticCuis
            = new HashMap<>();
      // The dictionary may have more than one type, create a map of types to terms and use them all
      for ( Integer cTakesSemantic : usedcTakesSemantics ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms = new HashSetMap<>();
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            for ( Long cuiCode : spanCuis.getValue() ) {
               final Collection<Concept> concepts = cuiConcepts.getCollection( cuiCode );
               if ( hascTakesSemantic( cTakesSemantic, concepts ) ) {
                  semanticTerms.placeValue( spanCuis.getKey(), cuiCode );
               }
            }
         }
         groupedSemanticCuis.put( cTakesSemantic, semanticTerms );
      }
      // Clean up sign/symptom and disease/disorder spans that are also anatomical sites
      removeUnwantedSpans( CONST.NE_TYPE_ID_ANATOMICAL_SITE, CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis );
      removeUnwantedSpans( CONST.NE_TYPE_ID_ANATOMICAL_SITE, CONST.NE_TYPE_ID_DISORDER, groupedSemanticCuis );
      // Clean up sign/symptoms that are also within disease/disorder spans
      if ( groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_FINDING )
           && groupedSemanticCuis.containsKey( CONST.NE_TYPE_ID_DISORDER ) ) {
         removeUnwantedSpans( CONST.NE_TYPE_ID_DISORDER, CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copiedTerms = new HashSetMap<>();
         copyTerms( CONST.NE_TYPE_ID_DISORDER, groupedSemanticCuis, copiedTerms );
         copyTerms( CONST.NE_TYPE_ID_FINDING, groupedSemanticCuis, copiedTerms );
         // We just created a collection with only the largest Textspans.
         // Any smaller Finding textspans are therefore within a larger d/d textspan and should be removed.
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> preciseTerms
               = PrecisionTermConsumer.createPreciseTerms( copiedTerms );
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> findingSpanCuis
               = groupedSemanticCuis.get( CONST.NE_TYPE_ID_FINDING );
         final Collection<TextSpan> findingSpans = new ArrayList<>( findingSpanCuis.keySet() );
//         findingSpans.stream()
//               .filter( fs -> !preciseTerms.containsKey( fs ) )
//               .forEach( findingSpanCuis::remove );
         for ( TextSpan span : findingSpans ) {
            if ( !preciseTerms.containsKey( span ) ) {
               findingSpanCuis.remove( span );
            }
         }
      }
      for ( Map.Entry<Integer, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> group : groupedSemanticCuis
            .entrySet() ) {
         consumeTypeIdHits( jcas, codingScheme, group.getKey(),
               PrecisionTermConsumer.createPreciseTerms( group.getValue() ), cuiConcepts );
      }
   }

   static private void removeUnwantedSpans( final int wantedTypeId, final int unwantedTypeId,
                                            final Map<Integer,
                                                  CollectionMap<TextSpan,
                                                        Long, ? extends Collection<Long>>> groupedSemanticCuis ) {
      if ( !groupedSemanticCuis.containsKey( wantedTypeId ) || !groupedSemanticCuis.containsKey( unwantedTypeId ) ) {
         return;
      }
      final Iterable<TextSpan> wantedSpans = groupedSemanticCuis.get( wantedTypeId ).keySet();
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> typeTextSpanCuis
            = groupedSemanticCuis.get( unwantedTypeId );
      for ( TextSpan wantedSpan : wantedSpans ) {
         typeTextSpanCuis.remove( wantedSpan );
      }
   }

   static private void copyTerms( final int typeId,
                                  final Map<Integer, CollectionMap<TextSpan,
                                        Long, ? extends Collection<Long>>> groupedSemanticCuis,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> copyTermsMap ) {
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> spanCuis
            = groupedSemanticCuis.get( typeId );
      for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCui : spanCuis ) {
         copyTermsMap.addAllValues( spanCui.getKey(), spanCui.getValue() );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeTypeIdHits( final JCas jcas, final String defaultScheme, final int cTakesSemantic,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticTerms,
                                  final CollectionMap<Long, Concept, ? extends Collection<Concept>> conceptMap )
         throws AnalysisEngineProcessException {
      _idHitConsumer.consumeTypeIdHits( jcas, defaultScheme, cTakesSemantic, semanticTerms, conceptMap );
   }


}
