package org.apache.ctakes.core.cr.jdbc;


import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * External libraries will not implement Decryptor, but we must handle them somehow.
 * This wraps a class that can decrypt in a Decryptor.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/13/2015
 */
final public class DecryptorWrapper implements Decryptor {

   static private final Logger LOGGER = Logger.getLogger( "DecryptorWrapper" );

   private final Object _decryptorThing;
   private final Method _decryptionMethod;

   /**
    * @param decryptorThing   any object that has a proper decryption method
    * @param decryptionMethod any method that accepts a key as the first parameter and note text as the second parameter
    *                         and returns unencrypted note text
    */
   public DecryptorWrapper( final Object decryptorThing, final Method decryptionMethod ) {
      _decryptorThing = decryptorThing;
      _decryptionMethod = decryptionMethod;
   }


   /**
    * Attempts to decrypt the note using a non-Decryptor that has a decryption method
    * {@inheritDoc}
    */
   @Override
   public String decrypt( final String key, final String note ) {
      try {
         return (String)_decryptionMethod.invoke( _decryptorThing, key, note );
      } catch ( IllegalAccessException | InvocationTargetException multiE ) {
         LOGGER.error( "Could not Decrypt Note using " + _decryptorThing.getClass().getName() );
         return note;
      }
   }


}
