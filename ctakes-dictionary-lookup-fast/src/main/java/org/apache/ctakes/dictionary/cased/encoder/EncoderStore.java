package org.apache.ctakes.dictionary.cased.encoder;


import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public enum EncoderStore {
   INSTANCE;

   static public EncoderStore getInstance() {
      return INSTANCE;
   }


   private final Collection<TermEncoder> _encoders = new ArrayList<>();

   public boolean addEncoder( final TermEncoder encoder ) {
      final String name = encoder.getName();
      synchronized ( _encoders ) {
         final boolean present = _encoders.stream()
                                          .map( TermEncoder::getName )
                                          .anyMatch( name::equals );
         if ( present ) {
            // Encoder with given name already exists.
            return false;
         }
         _encoders.add( encoder );
         return true;
      }
   }


   public Collection<TermEncoder> getEncoders() {
      return _encoders;
   }

}
