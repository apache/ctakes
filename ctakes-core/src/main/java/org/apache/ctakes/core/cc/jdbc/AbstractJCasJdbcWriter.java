package org.apache.ctakes.core.cc.jdbc;

import org.apache.uima.jcas.JCas;


/**
 * Writes information pulled directly from the jcas sent to the standard process( JCas ) method.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
abstract public class AbstractJCasJdbcWriter extends AbstractJdbcWriter<JCas> {

   private JCas _jCas;

   /**
    * Sets data to be written to the jcas.
    *
    * @param jCas ye olde
    */
   @Override
   protected void createData( final JCas jCas ) {
      _jCas = jCas;
   }

   /**
    * @return the JCas.
    */
   @Override
   protected JCas getData() {
      return _jCas;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final JCas data ) {
   }

}
