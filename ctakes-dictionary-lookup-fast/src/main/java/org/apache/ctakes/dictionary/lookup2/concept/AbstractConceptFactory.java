package org.apache.ctakes.dictionary.lookup2.concept;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 9/4/2014
 */
abstract public class AbstractConceptFactory implements ConceptFactory {

   final private String _name;

   public AbstractConceptFactory( final String name ) {
      _name = name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * Only creates non-empty concepts; Cuis for which additional info does not exist don't create concepts
    * {@inheritDoc}
    */
   @Override
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
      final Map<Long, Concept> conceptMap = new HashMap<>( cuiCodes.size() );
      for ( Long cuiCode : cuiCodes ) {
         final Concept concept = createConcept( cuiCode );
         if ( concept != null && !concept.isEmpty() ) {
            conceptMap.put( cuiCode, concept );
         }
      }
      return conceptMap;
   }

}
