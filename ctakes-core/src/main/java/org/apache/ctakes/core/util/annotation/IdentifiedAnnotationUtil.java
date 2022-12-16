package org.apache.ctakes.core.util.annotation;


import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A one-stop shop for the most commonly requested Identified Annotation properties.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/21/2020
 */
final public class IdentifiedAnnotationUtil {

   private IdentifiedAnnotationUtil() {
   }

   /**
    * @param annotation -
    * @return true iff the annotation is not generic, not uncertain, not negated and not conditional.
    */
   static public boolean isRealAffirmed( final IdentifiedAnnotation annotation ) {
      return !isGeneric( annotation )
             && !isUncertain( annotation )
             && !isNegated( annotation )
             && !isConditional( annotation );
   }

   static public boolean isGeneric( final IdentifiedAnnotation annotation ) {
      return annotation.getGeneric();
   }

   static public boolean isUncertain( final IdentifiedAnnotation annotation ) {
      return annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT;
   }

   static public boolean isNegated( final IdentifiedAnnotation annotation ) {
      return annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT;
   }

   static public boolean isConditional( final IdentifiedAnnotation annotation ) {
      return annotation.getConditional();
   }

   static public String getText( final IdentifiedAnnotation annotation ) {
      return annotation.getCoveredText();
   }

   static public boolean isHistoric( final IdentifiedAnnotation annotation ) {
      return annotation.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT;
   }

   /**
    * @param annotation -
    * @return Semantic Groups for all Umls Concepts of the annotation
    */
   static public Collection<SemanticGroup> getSemanticGroups( final IdentifiedAnnotation annotation ) {
      return SemanticGroup.getGroups( annotation );
   }

   /**
    * @param annotation -
    * @return Semantic Tuis for all Umls Concepts of the annotation
    */
   static public Collection<SemanticTui> getSemanticTuis( final IdentifiedAnnotation annotation ) {
      return SemanticTui.getTuis( annotation );
   }

   /**
    * @param annotation -
    * @return cuis for all Umls Concepts of the annotation
    */
   static public Collection<String> getCuis( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getCuis( annotation );
   }

   /**
    * @param annotation -
    * @return a collection of schemes with codes for the given annotation.  e.g. snomed_us, rxnorm.
    */
   static public Collection<String> getCodeSchemes( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getSchemeCodes( annotation ).keySet();
   }

   /**
    * @param annotation -
    * @param schemeName the name of a coding scheme.  e.g. snomed_us, rxnorm.
    * @return all annotation codes for the given coding scheme.
    */
   static public Collection<String> getCodes( final IdentifiedAnnotation annotation, final String schemeName ) {
      return OntologyConceptUtil.getCodes( annotation, schemeName );
   }

   /**
    * @param annotation -
    * @return preferred texts for all Umls Concepts of the annotation
    */
   static public Collection<String> getPreferredTexts( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getUmlsConceptStream( annotation )
                                .map( UmlsConcept::getPreferredText )
                                .filter( Objects::nonNull )
                                .filter( t -> !t.isEmpty() )
                                .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return the confidence of the annotation
    */
   static public float getConfidence( final IdentifiedAnnotation annotation ) {
      return annotation.getConfidence();
   }

   //
   //    Some get methods that can utilize possible OntologyConcept wsd scores
   //

   /**
    * @param annotation -
    * @return the best wsd SemanticGroups
    */
   static public Collection<SemanticGroup> getBestSemanticGroups( final IdentifiedAnnotation annotation ) {
      return getBestSemanticTuis( annotation )
            .stream()
            .map( SemanticTui::getGroup )
            .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return the best wsd SemanticGroups
    */
   static public SemanticGroup getBestSemanticGroup( final IdentifiedAnnotation annotation ) {
//      return SemanticGroup.getBestGroup( getBestSemanticGroups( annotation ) );
      return SemanticGroup.getBestGroup( getSemanticGroups( annotation ) );
   }

   /**
    * @param annotation -
    * @return the best wsd SemanticTuis
    */
   static public Collection<SemanticTui> getBestSemanticTuis( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getBestUmlsConcepts( annotation )
                                .stream()
                                .map( SemanticTui::getTui )
                                .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return the best wsd cuis
    */
   static public Collection<String> getBestCuis( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getBestUmlsConcepts( annotation )
                                .stream()
                                .map( UmlsConcept::getCui )
                                .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return a collection of the best wsd schemes with codes for the given annotation.  e.g. snomed_us, rxnorm.
    */
   static public Collection<String> getBestCodeSchemes( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getBestUmlsConcepts( annotation )
                                .stream()
                                .map( UmlsConcept::getCodingScheme )
                                .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @param schemeName the name of a coding scheme.  e.g. snomed_us, rxnorm.
    * @return the best wsd annotation codes for the given coding scheme.
    */
   static public Collection<String> getBestCodes( final IdentifiedAnnotation annotation, final String schemeName ) {
      return OntologyConceptUtil.getBestUmlsConcepts( annotation )
                                .stream()
                                .filter( c -> schemeName.equals( c.getCodingScheme() ) )
                                .map( UmlsConcept::getCode )
                                .collect( Collectors.toSet() );
   }

   /**
    * @param annotation -
    * @return preferred texts for the best wsd Umls Concepts of the annotation
    */
   static public Collection<String> getBestPreferredTexts( final IdentifiedAnnotation annotation ) {
      return OntologyConceptUtil.getBestUmlsConcepts( annotation )
                                .stream()
                                .map( UmlsConcept::getPreferredText )
                                .collect( Collectors.toSet() );
   }


}
