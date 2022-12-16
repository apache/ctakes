package org.apache.ctakes.dictionary.cased.encoder;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class InMemoryEncoder implements TermEncoder {

   private final String _name;

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<Long, Collection<TermEncoding>> _encodingMap;

   /**
    * @param name        unique name for dictionary
    * @param encodingMap Map with a cui code as key, and TermEncoding Collection as value
    */
   public InMemoryEncoder( final String name, final Map<Long, Collection<TermEncoding>> encodingMap ) {
      _name = name;
      _encodingMap = encodingMap;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<TermEncoding> getEncodings( final long cuiCode ) {
      return _encodingMap.getOrDefault( cuiCode, Collections.emptyList() );
   }


}
