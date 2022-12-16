package org.apache.ctakes.dictionary.cased.encoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/25/2020
 */
public enum CodeSchema {
   TUI( "int", String.class, "TUI" ),
   PREFERRED_TEXT( "text", String.class, "PREFTEXT", "PREF_TEXT", "PREFERRED_TEXT" ),
   UNKNOWN( "text", String.class, "UNKNOWN" );


   private final String _codeFormat;
   private final Class<?> _codeClass;
   private final Collection<String> _names;


   CodeSchema( final String codeFormat, final Class<?> codeClass, final String... names ) {
      _codeFormat = codeFormat;
      _codeClass = codeClass;
      _names = new HashSet<>( Arrays.asList( names ) );
   }


   public String getCodeFormat() {
      return _codeFormat;
   }

   public Class<?> getCodeClass() {
      return _codeClass;
   }

   public Collection<String> getNames() {
      return _names;
   }

   public boolean isSchema( final TermEncoding encoding ) {
      return isSchema( encoding.getSchema() );
   }

   public boolean isSchema( final String name ) {
      return _names.contains( name.toUpperCase() );
   }


   static public CodeSchema getSchema( final String name ) {
      return Arrays.stream( CodeSchema.values() )
                   .filter( c -> c.isSchema( name ) )
                   .findFirst()
                   .orElse( UNKNOWN );
   }


}
