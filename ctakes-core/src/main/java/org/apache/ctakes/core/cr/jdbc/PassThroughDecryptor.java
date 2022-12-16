package org.apache.ctakes.core.cr.jdbc;


import org.apache.log4j.Logger;

/**
 * Performs no decrpytion, just returns the note text
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/13/2015
 */
final public class PassThroughDecryptor implements Decryptor {

   static private final Logger LOGGER = Logger.getLogger( "PassThroughDecryptor" );

   /**
    * Performs no decryption, just returns the note text
    * {@inheritDoc}
    */
   @Override
   public String decrypt( final String key, final String note ) {
      return note;
   }


}
