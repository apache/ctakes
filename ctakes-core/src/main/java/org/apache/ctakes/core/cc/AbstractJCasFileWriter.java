package org.apache.ctakes.core.cc;


import org.apache.uima.jcas.JCas;

/**
 * Writes information pulled directly from the jcas sent to the standard process( JCas ) method.
 * Replaces poorly named AbstractOutputFileWriter.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/16/2016
 */
abstract public class AbstractJCasFileWriter extends AbstractFileWriter<JCas> {

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
