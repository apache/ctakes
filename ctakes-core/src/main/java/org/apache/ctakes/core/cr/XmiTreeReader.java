package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.jcas.JCas;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/5/2019
 */
@PipeBitInfo(
      name = "XMI Tree Reader",
      description = "Reads document texts and annotations from XMI files in a directory tree.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
final public class XmiTreeReader extends AbstractFileTreeReader {

   /**
    * {@inheritDoc}
    */
   @Override
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      jCas.reset();
      try ( FileInputStream inputStream = new FileInputStream( file ) ) {
         XmiCasDeserializer.deserialize( new BufferedInputStream( inputStream ), jCas.getCas() );
      } catch ( SAXException saxE ) {
         throw new IOException( saxE );
      }
   }

}
