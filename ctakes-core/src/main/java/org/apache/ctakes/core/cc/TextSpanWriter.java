package org.apache.ctakes.core.cc;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.refsem.Entity;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.Collection;

import static org.apache.ctakes.core.config.ConfigParameterConstants.DESC_OUTPUTDIR;
import static org.apache.ctakes.core.config.ConfigParameterConstants.PARAM_OUTPUTDIR;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/28/2015
 */
@PipeBitInfo(
      name = "Text Span Writer",
      description = "Writes BSV files with original text for extracted annotations and their span offsets.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class TextSpanWriter extends CasConsumer_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "TextSpanWriter" );

   static private final String FILE_EXTENSION = ".textspan.bsv";

   @ConfigurationParameter(
         name = PARAM_OUTPUTDIR,
         mandatory = false,
         description = DESC_OUTPUTDIR,
         defaultValue = ""
   )
   private String fitOutputDirectoryPath;

   private String _outputDirPath;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      try {
         if ( fitOutputDirectoryPath == null ) {
            fitOutputDirectoryPath = (String)uimaContext.getConfigParameterValue( PARAM_OUTPUTDIR );
         }
         if ( fitOutputDirectoryPath != null ) {
            setOutputDirectory( fitOutputDirectoryPath );
         }
      } catch ( IllegalArgumentException | SecurityException multE ) {
         // thrown if the path specifies a File (not Dir) or by file system access methods
         throw new ResourceInitializationException( multE );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final CAS aCAS ) throws AnalysisEngineProcessException {
      JCas jcas;
      try {
         jcas = aCAS.getJCas();
      } catch ( CASException casE ) {
         throw new AnalysisEngineProcessException( casE );
      }
      process( jcas );
   }


   /**
    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
    * @throws IllegalArgumentException if the provided path points to a File and not a Directory
    * @throws SecurityException        if the File System has issues
    */
   public void setOutputDirectory( final String outputDirectoryPath ) throws IllegalArgumentException,
                                                                             SecurityException {
      // If no outputDir is specified (null or empty) the current working directory will be used.  Else check path.
      if ( outputDirectoryPath == null || outputDirectoryPath.isEmpty() ) {
         _outputDirPath = "";
         LOGGER.debug( "No Output Directory Path specified, using current working directory "
                       + System.getProperty( "user.dir" ) );
         return;
      }
      final File outputDir = new File( outputDirectoryPath );
      if ( !outputDir.exists() ) {
         outputDir.mkdirs();
      }
      if ( !outputDir.isDirectory() ) {
         throw new IllegalArgumentException( outputDirectoryPath + " is not a valid directory path" );
      }
      _outputDirPath = outputDirectoryPath;
      LOGGER.debug( "Output Directory Path set to " + _outputDirPath );
   }


   /**
    * Process the jcas and write pretty sentences to file.  Filename is based upon the document id stored in the cas
    *
    * @param jcas ye olde ...
    */
   public void process( final JCas jcas ) {
      LOGGER.info( "Starting processing" );
      final String docId = DocIdUtil.getDocumentIdForFile( jcas );
      File outputFile;
      if ( _outputDirPath == null || _outputDirPath.isEmpty() ) {
         outputFile = new File( docId + FILE_EXTENSION );
      } else {
         outputFile = new File( _outputDirPath, docId + FILE_EXTENSION );
      }
      try ( final Writer writer = new BufferedWriter( new FileWriter( outputFile ) ) ) {
         final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jcas, IdentifiedAnnotation.class );
         for ( IdentifiedAnnotation annotation : annotations ) {
            writeAnnotation( annotation, writer );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not not write text span file " + outputFile.getPath() );
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "Finished processing" );
   }

   /**
    * Write a sentence from the document text
    *
    * @param annotation annotation containing the sentence
    * @param writer     writer to which pretty text for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static public void writeAnnotation( final AnnotationFS annotation,
                                       final Writer writer ) throws IOException {
      if ( !(annotation instanceof Event || annotation instanceof Entity) ) {
         return;
      }
      writer.write( annotation.getClass().getName()
                    + "|" + annotation.getBegin() + "," + annotation.getEnd()
                    + "|" + annotation.getCoveredText() );
      writer.write( "\n" );
   }


}
