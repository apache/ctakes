package org.apache.ctakes.core.cc.jdbc.field;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
abstract public class AbstractJdbcField<FT> implements JdbcField<FT> {

   private final String _name;
   private int _index;

   public AbstractJdbcField( final String name, final int index ) {
      _name = name;
      _index = index;
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
   public int getIndex() {
      return _index;
   }

}
