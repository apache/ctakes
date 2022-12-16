package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/24/2015
 */
final public class DefaultUmlsConceptCreator implements UmlsConceptCreator {

   static private final Logger LOGGER = Logger.getLogger( "DefaultUmlsConceptCreator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<UmlsConcept> createUmlsConcepts( final JCas jcas, final String codingScheme,
                                                      final String tui, final Concept concept ) {
      final Collection<UmlsConcept> concepts = new ArrayList<>();
      for ( String codeName : concept.getCodeNames() ) {
         if ( codeName.equals( Concept.TUI ) ) {
            continue;
         }
         final Collection<String> codes = concept.getCodes( codeName );
         if ( codes == null || codes.isEmpty() ) {
            continue;
         }
         for ( String code : codes ) {
            concepts.add( createUmlsConcept( jcas, codeName, concept.getCui(), tui,
                  concept.getPreferredText(), code ) );
         }
      }
      if ( concepts.isEmpty() ) {
         concepts.add( createUmlsConcept( jcas, codingScheme, concept.getCui(), tui,
               concept.getPreferredText(), null ) );
      }
      return concepts;
   }

   static private UmlsConcept createUmlsConcept( final JCas jcas, final String codingScheme,
                                                 final String cui, final String tui,
                                                 final String preferredText, final String code ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCodingScheme( codingScheme );
      umlsConcept.setCui( cui );
      if ( tui != null ) {
         umlsConcept.setTui( tui );
      }
      if ( preferredText != null && !preferredText.isEmpty() ) {
         umlsConcept.setPreferredText( preferredText );
      }
      if ( code != null ) {
         umlsConcept.setCode( code );
      }
      return umlsConcept;
   }

}
