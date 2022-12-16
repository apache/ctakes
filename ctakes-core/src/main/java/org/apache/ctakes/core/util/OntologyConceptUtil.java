package org.apache.ctakes.core.util;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 * @deprecated use OntologyConceptUtil in (sub) package annotation
 */
@Deprecated
final public class OntologyConceptUtil {

   private OntologyConceptUtil() {
   }

   /**
    * @param annotation -
    * @return array of FeatureStructure castable to array of OntologyConcept
    */
   @Deprecated
   static public FeatureStructure[] getConceptFeatureStructures( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getConceptFeatureStructures( annotation );
   }

   /**
    * @param annotation -
    * @return stream of OntologyConcept
    */
   @Deprecated
   static public Stream<OntologyConcept> getOntologyConceptStream( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getOntologyConceptStream( annotation );
   }

   /**
    * @param annotation -
    * @return stream of OntologyConcept
    */
   @Deprecated
   static public Collection<OntologyConcept> getOntologyConcepts( final IdentifiedAnnotation annotation ) {
      return getOntologyConceptStream( annotation ).collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return stream of all Umls Concepts associated with the annotation
    */
   @Deprecated
   static public Stream<UmlsConcept> getUmlsConceptStream( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getUmlsConceptStream( annotation );
   }

   /**
    * @param annotation -
    * @return set of all Umls Concepts associated with the annotation
    */
   @Deprecated
   static public Collection<UmlsConcept> getUmlsConcepts( final IdentifiedAnnotation annotation ) {
      return getUmlsConceptStream( annotation ).collect( Collectors.toSet() );
   }


   //
   //   Get cuis, tuis, or codes for a single IdentifiedAnnotation
   //

   /**
    * @param annotation -
    * @return set of all Umls cuis associated with the annotation
    */
   @Deprecated
   static public Collection<String> getCuis( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCuis( annotation );
   }

   /**
    * @param annotation -
    * @return set of all Umls tuis associated with the annotation
    */
   @Deprecated
   static public Collection<String> getTuis( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getTuis( annotation );
   }

   /**
    * @param annotation -
    * @return map of ontology scheme names to a set of ontology codes associated each scheme
    */
   @Deprecated
   static public Map<String, Collection<String>> getSchemeCodes( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getSchemeCodes( annotation );
   }

   /**
    * @param annotation -
    * @return set of ontology codes associated with all schemes
    */
   @Deprecated
   static public Collection<String> getCodes( final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCodes( annotation );
   }

   /**
    * @param annotation -
    * @param schemeName name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   @Deprecated
   static public Collection<String> getCodes( final IdentifiedAnnotation annotation,
                                              final String schemeName ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCodes( annotation, schemeName );
   }


   //
   //   Get cuis, tuis, or codes for all IdentifiedAnnotations in a jcas
   //

   /**
    * @param jcas -
    * @return set of all cuis in jcas
    */
   @Deprecated
   static public Collection<String> getCuis( final JCas jcas ) {
      return getCuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return map of all cuis in the jcas and their counts
    */
   @Deprecated
   static public Map<String, Long> getCuiCounts( final JCas jcas ) {
      return getCuiCounts( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all tuis in jcas
    */
   @Deprecated
   static public Collection<String> getTuis( final JCas jcas ) {
      return getTuis( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all ontology codes in jcas
    */
   @Deprecated
   static public Map<String, Collection<String>> getSchemeCodes( final JCas jcas ) {
      return getSchemeCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas -
    * @return set of all ontology codes in jcas
    */
   @Deprecated
   static public Collection<String> getCodes( final JCas jcas ) {
      return getCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ) );
   }

   /**
    * @param jcas       -
    * @param schemeName name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   @Deprecated
   static public Collection<String> getCodes( final JCas jcas,
                                              final String schemeName ) {
      return getCodes( JCasUtil.select( jcas, IdentifiedAnnotation.class ), schemeName );
   }


   //
   //   Get cuis, tuis, or codes for all IdentifiedAnnotations in a lookup window
   //

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return set of all cuis in lookupWindow
    */
   @Deprecated
   static public <T extends Annotation> Collection<String> getCuis( final JCas jcas, final T lookupWindow ) {
      return getCuis( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return set of all tuis in lookupWindow
    */
   @Deprecated
   static public <T extends Annotation> Collection<String> getTuis( final JCas jcas, final T lookupWindow ) {
      return getTuis( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return map of all schemes and their codes in lookupWindow
    */
   @Deprecated
   static public <T extends Annotation> Map<String, Collection<String>> getSchemeCodes( final JCas jcas,
                                                                                        final T lookupWindow ) {
      return getSchemeCodes( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @return set of all codes in lookupWindow
    */
   @Deprecated
   static public <T extends Annotation> Collection<String> getCodes( final JCas jcas, final T lookupWindow ) {
      return getCodes( JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, lookupWindow ) );
   }

   /**
    * @param jcas         -
    * @param lookupWindow -
    * @param schemeName   name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   @Deprecated
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
   @Deprecated
   static public Collection<String> getCuis( final Collection<IdentifiedAnnotation> annotations ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCuis( annotations );
   }

   /**
    * @param annotations -
    * @return map of all Umls cuis associated with the annotations and the counts of those cuis
    */
   @Deprecated
   static public Map<String, Long> getCuiCounts( final Collection<IdentifiedAnnotation> annotations ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCuiCounts( annotations );
   }

   /**
    * @param annotations -
    * @return set of all Umls tuis associated with the annotation
    */
   @Deprecated
   static public Collection<String> getTuis( final Collection<IdentifiedAnnotation> annotations ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getTuis( annotations );

   }

   /**
    * @param annotations -
    * @return map of ontology scheme names to a set of ontology codes associated each scheme
    */
   @Deprecated
   static public Map<String, Collection<String>> getSchemeCodes( final Collection<IdentifiedAnnotation> annotations ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getSchemeCodes( annotations );
   }

   /**
    * @param annotations -
    * @return set of ontology codes associated with all schemes
    */
   @Deprecated
   static public Collection<String> getCodes( final Collection<IdentifiedAnnotation> annotations ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCodes( annotations );
   }

   /**
    * @param annotations -
    * @param schemeName  name of the scheme of interest
    * @return set of ontology codes associated the named scheme
    */
   @Deprecated
   static public Collection<String> getCodes( final Collection<IdentifiedAnnotation> annotations,
                                              final String schemeName ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getCodes( annotations, schemeName );
   }


   //
   //   Get all IdentifiedAnnotations in jcas with given cui, tui, or code
   //

   /**
    * @param jcas -
    * @param cui  cui of interest
    * @return all IdentifiedAnnotations that have the given cui
    */
   @Deprecated
   static public Collection<IdentifiedAnnotation> getAnnotationsByCui( final JCas jcas,
                                                                       final String cui ) {
      return getAnnotationsByCui( JCasUtil.select( jcas, IdentifiedAnnotation.class ), cui );
   }

   /**
    * @param jcas -
    * @param tui  tui of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   @Deprecated
   static public Collection<IdentifiedAnnotation> getAnnotationsByTui( final JCas jcas,
                                                                       final String tui ) {
      return getAnnotationsByTui( JCasUtil.select( jcas, IdentifiedAnnotation.class ), tui );
   }

   /**
    * @param jcas -
    * @param code code of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   @Deprecated
   static public Collection<IdentifiedAnnotation> getAnnotationsByCode( final JCas jcas,
                                                                        final String code ) {
      return getAnnotationsByCode( JCasUtil.select( jcas, IdentifiedAnnotation.class ), code );
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
   @Deprecated
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
   @Deprecated
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
   @Deprecated
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
   @Deprecated
   static public Collection<IdentifiedAnnotation> getAnnotationsByCui(
         final Collection<IdentifiedAnnotation> annotations,
         final String cui ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getAnnotationsByCui( annotations, cui );
   }


   /**
    * @param annotations annotations for which codes should be found
    * @param tui         tui of interest
    * @return all IdentifiedAnnotations that have the given tui
    */
   @Deprecated
   static public Collection<IdentifiedAnnotation> getAnnotationsByTui(
         final Collection<IdentifiedAnnotation> annotations,
         final String tui ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getAnnotationsByTui( annotations, tui );
   }


   /**
    * @param annotations annotations for which codes should be found
    * @param code        code of interest
    * @return all IdentifiedAnnotations that have the given code
    */
   @Deprecated
   static public Collection<IdentifiedAnnotation> getAnnotationsByCode(
         final Collection<IdentifiedAnnotation> annotations,
         final String code ) {
      return org.apache.ctakes.core.util.annotation.OntologyConceptUtil.getAnnotationsByCode( annotations, code );
   }

}
