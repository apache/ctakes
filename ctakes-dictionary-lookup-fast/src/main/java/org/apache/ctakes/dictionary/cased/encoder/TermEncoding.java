package org.apache.ctakes.dictionary.cased.encoder;

import jdk.nashorn.internal.ir.annotations.Immutable;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
@Immutable
final public class TermEncoding {

   private final String _schema;
   private final Object _schemaCode;


   public TermEncoding( final String schema,
                        final Object schemaCode ) {
      _schema = schema;
      _schemaCode = schemaCode;
   }

   public String getSchema() {
      return _schema;
   }

   public Object getSchemaCode() {
      return _schemaCode;
   }

   public boolean equals( final Object object ) {
      return object instanceof TermEncoding
             && ((TermEncoding)object).getSchema().equals( getSchema() )
             && ((TermEncoding)object).getSchemaCode().equals( getSchemaCode() );
   }

   public int hashCode() {
      return (_schema + '_' + _schemaCode).hashCode();
   }

}
