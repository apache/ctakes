package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @since {3/3/2023}
 */
@PipeBitInfo(
      name = "TokenSpanWriter",
      description = "Writes files listing base tokens and their spans in a directory tree.",
      role = PipeBitInfo.Role.WRITER,
      usables = { DOCUMENT_ID_PREFIX, BASE_TOKEN }
)
public class TokenSpanWriter extends AbstractJCasFileWriter {

   // If you do not need to utilize the entire cas, or need more than the doc cas, consider AbstractFileWriter<T>.
   static private final Logger LOGGER = Logger.getLogger( "TokenSpanWriter" );
   // to add a configuration parameter, type "param" and hit tab.

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      final File file = new File( outputDir, documentId + "_tokenSpans.txt" );
      final String docText = jCas.getDocumentText();
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( BaseToken token : JCasUtil.select( jCas, BaseToken.class ) ) {
            final int begin = token.getBegin();
            final int end = token.getEnd();
            final String text = token instanceof NewlineToken ? "<EOL>" : docText.substring( begin, end );
            writer.write( text + "|" + begin + "," + end + "\n" );
         }
      }
   }

}