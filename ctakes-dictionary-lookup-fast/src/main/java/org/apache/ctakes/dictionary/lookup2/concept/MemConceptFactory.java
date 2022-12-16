package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 9/9/2014
 */
final public class MemConceptFactory extends AbstractConceptFactory {

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   final private Map<Long, Concept> _conceptMap;


   public MemConceptFactory( final String name, final Map<Long, Concept> conceptMap ) {
      super( name );
      _conceptMap = new HashMap<>( conceptMap );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      Concept concept = _conceptMap.get( cuiCode );
      if ( concept == null ) {
         concept = new DefaultConcept( CuiCodeUtil.getInstance().getAsCui( cuiCode ) );
         _conceptMap.put( cuiCode, concept );
      }
      return concept;
   }

}
