package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.BASE_TOKEN;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX;

/**
 * @author SPF , chip-nlp
 * @since {3/3/2023}
 */
@PipeBitInfo(
      name = "Token Table Writer",
      description = "Writes a table of base tokens and their spans in a directory tree.",
      role = PipeBitInfo.Role.WRITER,
      usables = { DOCUMENT_ID_PREFIX, BASE_TOKEN }
)
final public class TokenTableFileWriter extends AbstractTableFileWriter {

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList( " Token Text ", " Text Span " );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final List<List<String>> dataRows = new ArrayList<>();
      final String docText = jCas.getDocumentText();
      for ( BaseToken token : JCasUtil.select( jCas, BaseToken.class ) ) {
         final int begin = token.getBegin();
         final int end = token.getEnd();
         final String text = token instanceof NewlineToken ? "<EOL>" : docText.substring( begin, end );
         dataRows.add( Arrays.asList( text, begin + "," + end ) );
      }
      return dataRows;
   }

}
