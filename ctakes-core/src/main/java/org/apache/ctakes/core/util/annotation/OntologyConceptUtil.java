package org.apache.ctakes.core.util.annotation;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 */
final public class OntologyConceptUtil {

   static private final Logger LOGGER = Logger.getLogger( "IdentifiedAnnotationUtil" );

   static private final FeatureStructure[] EMPTY_FEATURE_ARRAY = new FeatureStructure[ 0 ];

   private OntologyConceptUtil() {
   }


   static private final Predicate<OntologyConcept> isSchemeOk
         = concept -> concept.getCodingScheme() != null && !concept.getCodingScheme().isEmpty();

   static private final Predicate<OntologyConcept> isCodeOk
         = concept -> concept.getCode() != null && !concept.getCode().isEmpty();

   static private final Function<OntologyConcept, Collection<String>> getCodeAsSet
         = concept -> new HashSet<>( Collections.singletonList( concept.getCode() ) );

   static private final BinaryOperator<Collection<String>> mergeSets
         = ( set1, set2 ) -> {
      set1.addAll( set2 );
      return set1;
   };


   /**
    * @param annotation -
    * @return array of FeatureStructure castable to array of OntologyConcept
    */
   static public FeatureStructure[] getConceptFeatureStructures( final IdentifiedAnnotation annotation ) {
      if ( annotation == null ) {
         return EMPTY_FEATURE_ARRAY;
      }
      final FSArray ontologyConcepts = annotation.getOntologyConceptArr();
      if ( ontologyConcepts == null ) {
         return EMPTY_FEATURE_ARRAY;
      }
      return ontologyConcepts.toArray();
   }

   /**
    * @param annotation -
    * @return stream of OntologyConcept
    */
   static public Stream<OntologyConcept> getOntologyConceptStream( final IdentifiedAnnotation annotation ) {
      return Arrays.stream( getConceptFeatureStructures( annotation ) )
                   .filter( OntologyConcept.class::isInstance )
                   .map( fs -> (OntologyConcept)fs )
                   .filter( isSchemeOk )
                   .filter( isCodeOk );
   }

   /**
    * @param annotation -
    * @return stream of OntologyConcept
    */
   static public Collection<OntologyConcept> getOntologyConcepts( final IdentifiedAnnotation annotation ) {
      return getOntologyConceptStream( annotation ).collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return stream of all Umls Concepts associated with the annotation
    */
   static public Stream<UmlsConcept> getUmlsConceptStream( final IdentifiedAnnotation annotation ) {
      return Arrays.stream( getConceptFeatureStructures( annotation ) )
                   .filter( UmlsConcept.class::isInstance )
                   .map( fs -> (UmlsConcept)fs );
   }

   /**
    * @param annotation -
    * @return set of all Umls Concepts associated with the annotation
    */
   static public Collection<UmlsConcept> getUmlsConcepts( final IdentifiedAnnotation annotation ) {
      return getUmlsConceptStream( annotation ).collect( Collectors.toSet() );
   }


   static private final Predicate<OntologyConcept> isDisambiguated = c -> {
      try {
         return c.getDisambiguated();
      } catch ( Exception e ) {
         return false;
      }
   };

   static private double getWsdScore( final OntologyConcept concept ) {
      try {
         return concept.getScore();
      } catch ( Exception e ) {
         return 0.5;
      }
   }


   /**
    * @param annotation -
    * @return set of all Umls Concepts associated with the annotation with the highest wsd score
    */
   static public Collection<UmlsConcept> getBestUmlsConcepts( final IdentifiedAnnotation annotation ) {
      final Stream<UmlsConcept> umlsConceptStream = getUmlsConceptStream( annotation );
      List<UmlsConcept> wsdConcepts = umlsConceptStream
            .filter( isDisambiguated )
            .collect( Collectors.toList() );
      if ( wsdConcepts.size() == 1 ) {
         return wsdConcepts;
      }
      if ( wsdConcepts.isEmpty() ) {
         wsdConcepts = umlsConceptStream
               .sorted( Comparator.comparingDouble( OntologyConceptUtil::getWsdScore ) )
               .collect( Collectors.toList() );
      } else {
         wsdConcepts = wsdConcepts.stream()
                                  .sorted( Comparator.comparingDouble( OntologyConceptUtil::getWsdScore ) )
                                  .collect( Collectors.toList() );
      }
      if ( wsdConcepts.size() <= 1 ) {
         return wsdConcepts;
      }
      final double max = getWsdScore( wsdConcepts.get( wsdConcepts.size() - 1 ) );
      return wsdConcepts.stream()
                        .filter( c -> getWsdScore( c ) == max )
                        .collect( Collectors.toSet() );
   }


   //
   //   Get cuis, tuis, or codes for a single IdentifiedAnnotation
   //

   /**
    * @param annotation -
    * @return set of all Umls cuis associated with the annotation
    */
   static public Collection<String> getCuis( final IdentifiedAnnotation annotation ) {
      return getUmlsConceptStream( annotation )
            .map( UmlsConcept::getCui )
            .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return set of all Umls tuis associated with the annotation
    */
   static public Collection<String> getTuis( final IdentifiedAnnotation annotation ) {
      return getUmlsConceptStream( annotation )
            .map( UmlsConcept::getTui )
            .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return map of ontology scheme names to a set of ontology codes associated each scheme
    */
   static public Map<String, Collection<String>> getSchemeCodes( final IdentifiedAnnotation annotation ) {
      return getOntologyConceptStream( annotation )
            .collect( Collectors.toMap( OntologyConcept::getCodingScheme, getCodeAsSet, mergeSets ) );
   }

   /**
    * @param annotation -
    * @return set of ontology codes associated with all schemes
    */
   static public Collection<String> getCodes( final IdentifiedAnnotation annotation ) {
      return getOntologyConceptStream( annotation )
            .map( OntologyConcept::getCode )
            .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @param schemeName name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   static public Collection<String> getCodes( final IdentifiedAnnotation annotation,
                                              final String schemeName ) {
      return getOntologyConceptStream( annotation )
            .filter( concept -> schemeName.equalsIgnoreCase( concept.getCodingScheme() ) )
            .map( OntologyConcept::getCode )
            .collect( Collectors.toSet() );
   }


   //
   //   Get cuis, tuis, or codes for all IdentifiedAnnotations in a jcas
   //

   /**
    * @param jcas -
    * @return set of all cuis in jcas
    */
   static public Collection<String> getCuis( final JCas jcas ) {
      return getCuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return map of all cuis in the jcas and their counts
    */
   static public Map<String, Long> getCuiCounts( final JCas jcas ) {
      return getCuiCounts( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all tuis in jcas
    */
   static public Collection<String> getTuis( final JCas jcas ) {
      return getTuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all ontology codes in jcas
    */
   static public Map<String, Collection<String>> getSchemeCodes( final JCas jcas ) {
      return getSchemeCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all ontology codes in jcas
    */
   static public Collection<String> getCodes( final JCas jcas ) {
      return getCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas       -
    * @param schemeName name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   static public Collection<String> getCodes( final JCas jcas,
                                              final String schemeName ) {
      return getCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ), schemeName );
   }

   /**
    * @param jcas -
    * @return set of all semantic Types in jcas
    */
   static public Collection<SemanticTui> getSemanticTuis( final JCas jcas ) {
      return getSemanticTuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all semantic groups in jcas
    */
   static public Collection<SemanticGroup> getSemanticGroups( final JCas jcas ) {
      return getSemanticGroups( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   //
   //   Get cuis, tuis, or codes for all IdentifiedAnnotations in a lookup window
   //

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return set of all cuis in lookupWindow
    */
   static public <T extends Annotation> Collection<String> getCuis( final JCas jcas, final T lookupWindow ) {
      return getCuis( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return set of all tuis in lookupWindow
    */
   static public <T extends Annotation> Collection<String> getTuis( final JCas jcas, final T lookupWindow ) {
      return getTuis( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return map of all schemes and their codes in lookupWindow
    */
   static public <T extends Annotation> Map<String, Collection<String>> getSchemeCodes( final JCas jcas,
                                                                                        final T lookupWindow ) {
      return getSchemeCodes( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return set of all codes in lookupWindow
    */
   static public <T extends Annotation> Collection<String> getCodes( final JCas jcas, final T lookupWindow ) {
      return getCodes( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param schemeName   name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   static public <T extends Annotation> Collection<String> getCodes( final JCas jcas, final T lookupWindow,
                                                                     final String schemeName ) {
      return getCodes( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ), schemeName );
   }


   //
   //   Get cuis, tuis, or codes for a collections of IdentifiedAnnotations
   //

   /**
    * @param annotations -
    * @return set of all Umls cuis associated with the annotations
    */
   static public Collection<String> getCuis( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( OntologyConceptUtil::getCuis )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations -
    * @return map of all Umls cuis associated with the annotations and the counts of those cuis
    */
   static public Map<String, Long> getCuiCounts( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( OntologyConceptUtil::getCuis )
                        .flatMap( Collection::stream )
                        .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   /**
    * @param annotations -
    * @return set of all Umls tuis associated with the annotation
    */
   static public Collection<String> getTuis( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( OntologyConceptUtil::getTuis )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );

   }

   /**
    * @param annotations -
    * @return map of ontology scheme names to a set of ontology codes associated each scheme
    */
   static public Map<String, Collection<String>> getSchemeCodes( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( OntologyConceptUtil::getSchemeCodes )
                        .map( Map::entrySet )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, mergeSets ) );
   }

   /**
    * @param annotations -
    * @return set of ontology codes associated with all schemes
    */
   static public Collection<String> getCodes( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( OntologyConceptUtil::getCodes )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations -
    * @param schemeName  name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   static public Collection<String> getCodes( final Collection<IdentifiedAnnotation> annotations,
                                              final String schemeName ) {
      return annotations.stream()
                        .map( annotation -> getCodes( annotation, schemeName ) )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }


   /**
    * @param annotations -
    * @return set of Semantic Types associated the named scheme
    */
   static public Collection<SemanticTui> getSemanticTuis( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( SemanticTui::getTuis )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations -
    * @return set of ontology codes associated the named scheme
    */
   static public Collection<SemanticGroup> getSemanticGroups( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .map( SemanticGroup::getGroups )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toSet() );
   }

   //
   //   Get all IdentifiedAnnotations in jcas with given cui, tui, or code
   //

   /**
    * @param jcas -
    * @param cui  cui of interest
    * @return all IdentifiedAnnotations that have the given cui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCui( final JCas jcas,
                                                                       final String cui ) {
      return getAnnotationsByCui( JCasUtil.select( jcas, IdentifiedAnnotation.class ), cui );
   }

   /**
    * @param jcas -
    * @param tui  tui of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByTui( final JCas jcas,
                                                                       final String tui ) {
      return getAnnotationsByTui( JCasUtil.select( jcas, IdentifiedAnnotation.class ), tui );
   }

   /**
    * @param jcas -
    * @param code code of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCode( final JCas jcas,
                                                                        final String code ) {
      return getAnnotationsByCode( JCasUtil.select( jcas, IdentifiedAnnotation.class ), code );
   }

   /**
    * @param jcas -
    * @param tui  semantic type of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticTui( final JCas jcas,
                                                                               final SemanticTui tui ) {
      return getAnnotationsBySemanticTui( JCasUtil.select( jcas, IdentifiedAnnotation.class ), tui );
   }

   /**
    * @param jcas  -
    * @param group semantic group of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticGroup( final JCas jcas,
                                                                                 final SemanticGroup group ) {
      return getAnnotationsBySemanticGroup( JCasUtil.select( jcas, IdentifiedAnnotation.class ), group );
   }

   /**
    * @param jcas -
    * @param cuis cuis of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCuis( final JCas jcas,
                                                                        final String... cuis ) {
      return getAnnotationsByCuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ), cuis );
   }

   /**
    * @param jcas -
    * @param tuis tuis of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByTuis( final JCas jcas,
                                                                        final String... tuis ) {
      return getAnnotationsByTuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ), tuis );
   }

   /**
    * @param jcas  -
    * @param codes codes of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCodes( final JCas jcas,
                                                                         final String... codes ) {
      return getAnnotationsByCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ), codes );
   }

   /**
    * @param jcas -
    * @param tuis tuis of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticTuis( final JCas jcas,
                                                                                final SemanticTui... tuis ) {
      return getAnnotationsBySemanticTuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ), tuis );
   }

   /**
    * @param jcas   -
    * @param groups tuis of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticGroups( final JCas jcas,
                                                                                  final SemanticGroup... groups ) {
      return getAnnotationsBySemanticGroups( JCasUtil.select( jcas, IdentifiedAnnotation.class ), groups );
   }


   //
   //   Get all IdentifiedAnnotations in lookup window with given cui, tui, or code
   //

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param cui          cui of interest
    * @return all IdentifiedAnnotations that have the given cui
    */
   static public <T extends Annotation> Collection<IdentifiedAnnotation> getAnnotationsByCui( final JCas jcas,
                                                                                              final T lookupWindow,
                                                                                              final String cui ) {
      return getAnnotationsByCui( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ), cui );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param tui          tui of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public <T extends Annotation> Collection<IdentifiedAnnotation> getAnnotationsByTui( final JCas jcas,
                                                                                              final T lookupWindow,
                                                                                              final String tui ) {
      return getAnnotationsByTui( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ), tui );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param code         code of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   static public <T extends Annotation> Collection<IdentifiedAnnotation> getAnnotationsByCode( final JCas jcas,
                                                                                               final T lookupWindow,
                                                                                               final String code ) {
      return getAnnotationsByCode( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ), code );
   }


   //
   //   Get all IdentifiedAnnotations in a collection of annotations with given cui, tui, or code
   //

   /**
    * @param annotations annotations for which codes should be found
    * @param cui         cui of interest
    * @return all IdentifiedAnnotations that have the given cui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCui(
         final Collection<IdentifiedAnnotation> annotations,
         final String cui ) {
      return annotations.stream()
                        .filter( annotation -> getCuis( annotation ).contains( cui ) )
                        .collect( Collectors.toSet() );
   }


   /**
    * @param annotations annotations for which codes should be found
    * @param tui         tui of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByTui(
         final Collection<IdentifiedAnnotation> annotations,
         final String tui ) {
      return annotations.stream()
                        .filter( annotation -> getTuis( annotation ).contains( tui ) )
                        .collect( Collectors.toSet() );
   }


   /**
    * @param annotations annotations for which codes should be found
    * @param code        code of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCode(
         final Collection<IdentifiedAnnotation> annotations,
         final String code ) {
      return annotations.stream()
                        .filter( annotation -> getCodes( annotation ).contains( code ) )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param tui         semantic type of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticTui(
         final Collection<IdentifiedAnnotation> annotations,
         final SemanticTui tui ) {
      return annotations.stream()
                        .filter( annotation -> SemanticTui.getTuis( annotation ).contains( tui ) )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param group       semantic group of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticGroup(
         final Collection<IdentifiedAnnotation> annotations,
         final SemanticGroup group ) {
      return annotations.stream()
                        .filter( annotation -> SemanticGroup.getGroups( annotation ).contains( group ) )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param cuis        cuis of interest
    * @return all IdentifiedAnnotations that have the given cui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCuis(
         final Collection<IdentifiedAnnotation> annotations,
         final String... cuis ) {
      final Collection<String> cuiSet = new HashSet<>( Arrays.asList( cuis ) );
      final Predicate<IdentifiedAnnotation> cuiMatch
            = a -> getCuis( a ).stream().anyMatch( cuiSet::contains );
      return annotations.stream()
                        .filter( cuiMatch )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param tuis        tuis of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByTuis(
         final Collection<IdentifiedAnnotation> annotations,
         final String... tuis ) {
      final Collection<String> tuiSet = new HashSet<>( Arrays.asList( tuis ) );
      final Predicate<IdentifiedAnnotation> tuiMatch
            = a -> getTuis( a ).stream().anyMatch( tuiSet::contains );
      return annotations.stream()
                        .filter( tuiMatch )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param codes       codes of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsByCodes(
         final Collection<IdentifiedAnnotation> annotations,
         final String... codes ) {
      final Collection<String> codeSet = new HashSet<>( Arrays.asList( codes ) );
      final Predicate<IdentifiedAnnotation> codeMatch
            = a -> getCodes( a ).stream().anyMatch( codeSet::contains );
      return annotations.stream()
                        .filter( codeMatch )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param tuis        semantic types of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticTuis(
         final Collection<IdentifiedAnnotation> annotations,
         final SemanticTui... tuis ) {
      final Collection<SemanticTui> tuiSet = new HashSet<>( Arrays.asList( tuis ) );
      final Predicate<IdentifiedAnnotation> tuiMatch
            = a -> SemanticTui.getTuis( a ).stream().anyMatch( tuiSet::contains );
      return annotations.stream()
                        .filter( tuiMatch )
                        .collect( Collectors.toSet() );
   }

   /**
    * @param annotations annotations for which codes should be found
    * @param groups      semantic groups of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   static public Collection<IdentifiedAnnotation> getAnnotationsBySemanticGroups(
         final Collection<IdentifiedAnnotation> annotations,
         final SemanticGroup... groups ) {
      final Collection<SemanticGroup> groupSet = new HashSet<>( Arrays.asList( groups ) );
      final Predicate<IdentifiedAnnotation> groupMatch
            = a -> SemanticGroup.getGroups( a ).stream().anyMatch( groupSet::contains );
      return annotations.stream()
                        .filter( groupMatch )
                        .collect( Collectors.toSet() );
   }

}
