package org.apache.ctakes.dictionary.cased.table.column;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
public enum CodeType {
   INT,
   LONG,
   TEXT,
   TUI,
   PREF_TEXT;
//     VARCHAR(48)  ,  BIGINT  ,  FLOAT  ,  INTEGER   ??


   /**
    * Sending a nonexistant name to enum .valueof( .. ) will throw an IllegalArgumentException.
    *
    * @param name -
    * @return -
    */
   static public CodeType getCodeType( final String name ) {
      final String upper = name.toUpperCase();
      for ( CodeType codeType : CodeType.values() ) {
         if ( codeType.name().equals( upper ) ) {
            return codeType;
         }
      }
      // Return TEXT as a default.
      return TEXT;
   }


}
