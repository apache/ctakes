package org.apache.ctakes.dictionary.cased.encoder;

import org.apache.ctakes.dictionary.cased.lookup.DiscoveredTerm;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public interface TermEncoder {

   /**
    * The Type identifier and Name are used to maintain a collection of term encoders,
    * so the combination of Type and Name should be unique for each encoder if possible.
    *
    * @return simple name for the encoder
    */
   String getName();


   default Collection<TermEncoding> getEncodings( final DiscoveredTerm discoveredTerm ) {
      return getEncodings( discoveredTerm.getCuiCode() );
   }


   Collection<TermEncoding> getEncodings( final long cuiCode );


}
