package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.JCasUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.core.config.ConfigParameterConstants.PARAM_OUTPUTDIR;

/**
 * Saves the (base) tokens of each sentence on a separate line, separated by spaces
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/22/2014
 */
@PipeBitInfo(
      name = "Sentences Writer",
      description = "Writes Text files with original text from the document, sentence by sentence.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.SENTENCE,
                       PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class SentenceTokensPrinter extends CasConsumer_ImplBase {

   // LOG4J logger based on interface name
   final static private Logger LOGGER = Logger.getLogger( "SentenceTokensPrinter" );


   private String _outputDirPath;

   /**
    * Checks for parameter <code>OutputDirectory</code>.  If present then files will be saved, if not stdout is used.
    * {@inheritDoc}
    *
    * @throws ResourceInitializationException if parameter <code>OutputDirectory</code> has an invalid value
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      super.initialize();
      final String outputDirPath = (String)getConfigParameterValue( PARAM_OUTPUTDIR );
      if ( outputDirPath != null && !outputDirPath.isEmpty() ) {
         final File outputDirectory = new File( outputDirPath );
         if ( !outputDirectory.exists() && !outputDirectory.mkdirs() ) {
            throw new ResourceInitializationException(
                  new IOException( "Parameter setting " + PARAM_OUTPUTDIR
                                   + " does not point to an existing directory" +
                                   " or one that could be created." ) );
         }
         _outputDirPath = outputDirPath;
      }
   }


   /**
    * Saves the (base) tokens of each sentence on a separate line, separated by spaces
    * {@inheritDoc}
    */
   @Override
   public void processCas( final CAS cas ) throws ResourceProcessException {
      JCas jcas;
      try {
         jcas = cas.getJCas();
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
         return;
      }
      final int sentenceTypeCode = JCasUtil.getType( "org.apache.ctakes.typesystem.type.textspan.Sentence" );
      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      final AnnotationIndex<Annotation> sentences = indexes.getAnnotationIndex( sentenceTypeCode );
      if ( sentences == null ) {  // I don't trust AnnotationIndex.size(), so don't check
         return;
      }
      final Collection<String> tokenizedSentences = new ArrayList<>( sentences.size() );
      try {
         for ( Object sentence : sentences ) {
            tokenizedSentences.add( getSentenceTokens( jcas, (Annotation)sentence ) );
         }
      } catch ( ArrayIndexOutOfBoundsException iobE ) {
         // JCasHashMap will throw this every once in a while.  Assume the sentences are done and move on
         LOGGER.warn( iobE.getMessage() );
      }
      final String documentId = DocIdUtil.getDocumentID( jcas );
      outputSentenceTokens( documentId, tokenizedSentences );
   }

   /**
    * @param jcas     -
    * @param sentence -
    * @return the (base) tokens of the sentence, separated by spaces
    */
   static private String getSentenceTokens( final JCas jcas, final Annotation sentence ) {
      final StringBuilder sb = new StringBuilder();
      final List<BaseToken> allBaseTokens = org.apache.uima.fit.util.JCasUtil
            .selectCovered( jcas, BaseToken.class, sentence );
      for ( BaseToken baseToken : allBaseTokens ) {
         if ( baseToken instanceof NewlineToken ) {
            // mid-sentence newlines are ignored - this honors the newline behavior of the selected Sentence Detector
            continue;
         }
         sb.append( baseToken.getCoveredText() ).append( ' ' );
      }
      return sb.toString();
   }

   /**
    * @param documentId         id of the document, used for output identification of the analyzed document
    * @param tokenizedSentences space-separated sentence tokens
    */
   private void outputSentenceTokens( final String documentId, final Iterable<String> tokenizedSentences ) {
      if ( _outputDirPath == null ) {
         printSentenceTokens( documentId, tokenizedSentences );
      } else {
         saveSentenceTokens( _outputDirPath, documentId, tokenizedSentences );
      }
   }

   /**
    * Prints the (base) tokens of each sentence on a separate line, separated by spaces, on standard output
    *
    * @param documentId         id of the document, used for output identification of the analyzed document
    * @param tokenizedSentences space-separated sentence tokens
    */
   static private void printSentenceTokens( final String documentId, final Iterable<String> tokenizedSentences ) {
      System.out.println( "===========================   " + documentId + "   ===========================" );
      for ( String tokenizedSentence : tokenizedSentences ) {
         System.out.println( tokenizedSentence );
      }
   }

   /**
    * Saves the (base) tokens of each sentence on a separate line, separated by spaces
    *
    * @param outputDirPath      root output directory specified by parameter <code>OutputDirectory</code>
    * @param documentId         id of the document, used for the output file name of the analyzed document
    * @param tokenizedSentences space-separated sentence tokens
    */
   static private void saveSentenceTokens( final String outputDirPath,
                                           final String documentId, final Iterable<String> tokenizedSentences ) {
      // Be prepared for documentId that contains directory segments, some of which may not exist
      final File outputFile = new File( outputDirPath + File.pathSeparator + documentId );
      if ( !outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs() ) {
         LOGGER.error( outputFile.getPath() + " is an invalid output file path" );
         return;
      }
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) ) ) {
         for ( String tokenizedSentence : tokenizedSentences ) {
            writer.write( tokenizedSentence );
            writer.newLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

}
