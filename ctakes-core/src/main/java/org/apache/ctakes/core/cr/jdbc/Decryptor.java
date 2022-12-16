package org.apache.ctakes.core.cr.jdbc;

/**
 * A Decryptor is required by the CollectionReader to get parsable text from encrypted notes
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/13/2015
 */
public interface Decryptor {

   /**
    * Used to decrypt text for a note
    *
    * @param key  used for decrypting the note
    * @param note text to decrypt
    * @return unencrypted text of the note
    * //    * @throws Exception never thrown, just here to duplicate the (unspecific and poorly-formed) throw of the real class
    */
   public String decrypt( final String key, final String note );

}
