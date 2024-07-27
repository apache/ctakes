package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Recursively reads a directory tree of files, sorted by level (root first),
 * creating the DocumentID from the file name and the DocumentIdPrefix by the subdirectory path between
 * the root and the leaf file
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/10/2016
 */
@PipeBitInfo(
      name = "File Tree Reader",
      description = "Reads document texts from text files in a directory tree.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX }
)
final public class FileTreeReader extends AbstractFileTreeReader {

   static private final Logger LOGGER = LoggerFactory.getLogger( "FileTreeReader" );

   /**
    * @param jCas unpopulated jcas
    * @param file file to be read
    * @throws IOException should anything bad happen
    */
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      String docText = readFile( file );
      docText = handleQuotedDoc( docText );
      docText = handleTextEol( docText );
      jCas.setDocumentText( docText );
   }


   /**
    * Reads file using a Path and stream.  Failing that it calls {@link #readByBuffer(File)}
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   public String readFile( final File file ) throws IOException {
      LOGGER.info( "Reading " + file.getPath() + " ..." );
      if ( !isKeepCrChar() ) {
         try {
            return readByPath( file );
         } catch ( IOException ioE ) {
            // This is a pretty bad way to handle a MalformedInputException, but that can be thrown by the collector
            // in the stream, and java streams and exceptions do not go well together
            LOGGER.warn( "Bad characters in " + file.getPath() );
         }
      }
      try {
         return readByStreamReader( file );
      } catch ( IOException ioE ) {
         // ignore for now, try to read by buffer.
      }
      return readByBuffer( file );
   }

   /**
    * Reads file using a Path and stream.
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByPath( final File file ) throws IOException {
      final String encoding = getValidEncoding();
      if ( encoding != null && !encoding.isEmpty() && !UNKNOWN.equals( encoding ) ) {
         final Charset charset = Charset.forName( encoding );
         try ( Stream<String> stream = Files.lines( file.toPath(), charset ) ) {
            return stream.collect( Collectors.joining( "\n" ) );
         }
      } else {
         return safeReadByPath( file );
      }
   }

   static private String safeReadByPath( final File file ) throws IOException {
      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput( CodingErrorAction.IGNORE );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( Files.newInputStream( file.toPath() ), decoder ) ) ) {
         return reader.lines().collect( Collectors.joining( "\n" ) );
      }
   }

   /**
    * Reads file using buffered input stream
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByBuffer( final File file ) throws IOException {
      final String encoding = getValidEncoding();
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( new FileInputStream( file ), buffer.length ) ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
            if ( encoding != null && !encoding.isEmpty() && !UNKNOWN.equals( encoding ) ) {
               sb.append( new String( buffer, 0, length, encoding ) );
            } else {
               sb.append( new String( buffer, 0, length ) );
            }
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }

   /**
    * Reads file using a stream reader
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByStreamReader( final File file ) throws IOException {
      final StringBuilder sb = new StringBuilder();
      final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput( CodingErrorAction.IGNORE );
      try ( BufferedReader reader
                  = new BufferedReader( new InputStreamReader( Files.newInputStream( file.toPath() ), decoder ) ) ) {
         int i = reader.read();
         while ( i != -1 ) {
            sb.append( Character.toChars( i ) );
            i = reader.read();
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }

   /**
    * Convenience method to create a reader with an input directory
    *
    * @param inputDirectory -
    * @return new reader
    * @throws ResourceInitializationException -
    */
   public static CollectionReader createReader( final String inputDirectory ) throws ResourceInitializationException {
      return CollectionReaderFactory.createReader( FileTreeReader.class,
            ConfigParameterConstants.PARAM_INPUTDIR,
            inputDirectory );
   }

}
