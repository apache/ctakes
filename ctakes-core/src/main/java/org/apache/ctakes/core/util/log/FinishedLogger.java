package org.apache.ctakes.core.util.log;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.ProgressManager;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2018
 */
@PipeBitInfo(
      name = "Finished Logger",
      description = "Writes a banner message COMPLETE to the log when all processing is finished.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class FinishedLogger extends JCasAnnotator_ImplBase {

//   static private final Logger LOGGER = Logger.getLogger( "FinishedLogger" );
static private final Logger LOGGER = Logger.getLogger( "ProgressDone" );

   private long _initMillis;
   private long _docCount = 0;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _initMillis = System.currentTimeMillis();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      _docCount++;
      ProgressManager.getInstance().updateProgress( Long.valueOf( _docCount ).intValue() );
   }

   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      final long endMillis = System.currentTimeMillis();
      final long instantMillis = ManagementFactory.getRuntimeMXBean().getStartTime();

      LOGGER.info( "Run Start Time:               " + getTime( instantMillis ) );
      LOGGER.info( "Processing Start Time:        " + getTime( _initMillis ) );
      LOGGER.info( "Processing End Time:          " + getTime( endMillis ) );
      LOGGER.info( "Initialization Time Elapsed:  " + getSpan( _initMillis - instantMillis ) );
      LOGGER.info( "Processing Time Elapsed:      " + getSpan( endMillis - _initMillis ) );
      LOGGER.info( "Total Run Time Elapsed:       " + getSpan( endMillis - instantMillis ) );
      LOGGER.info( "Documents Processed:          " + _docCount );
      final long millisPerNote = _docCount == 0 ? 0 : (endMillis - _initMillis) / _docCount;
      LOGGER.info( String.format( "Average Seconds per Document: %.2f", (millisPerNote / 1000f) ) );
   }

   static private String getTime( final long millis ) {
      return new Date( millis ).toString();
   }

   static private String getSpan( final long millis ) {
      long seconds = millis / 1000;
      long minutes = seconds / 60;
      seconds %= 60;
      long hours = minutes / 60;
      minutes %= 60;
      long days = hours / 24;
      hours %= 24;
      final StringBuilder sb = new StringBuilder();
      if ( days > 0 ) {
         sb.append( days ).append( " days, " );
      }
      if ( days > 0 || hours > 0 ) {
         sb.append( hours )
           .append( " hours, " );
      }
      if ( days > 0 || hours > 0 || minutes > 0 ) {
         sb.append( minutes )
           .append( " minutes, " );
      }
      sb.append( seconds )
        .append( " seconds" );
      return sb.toString();
   }


   /**
    * Example:
    * Build Version: 4.0.1-SNAPSHOT
    * Build Date: 2022-10-26 15:59
    *
    * @return build information or an empty if none is found.
    */
   static public String getBuildInfo() {
      final File libDir = FileLocator.getFileQuiet( "lib" );
      if ( libDir == null || !libDir.isDirectory() ) {
         return "";
      }
      final File[] files = libDir.listFiles();
      if ( files == null ) {
         return "";
      }
      for ( File jar : files ) {
         final String name = jar.getName();
         if ( name.contains( "ctakes-core" )
              && !name.contains( "ctakes-core-models" )
              && !name.contains( "coreference" )
              && !name.contains( "ctakes-core-res" ) ) {
            try ( JarFile jf = new JarFile( jar ) ) {
               final Manifest manifest = jf.getManifest();
               final Attributes attributes = manifest.getMainAttributes();
               return getAttribute( attributes, "Build Version", "Implementation-Version" )
                      + getAttribute( attributes, "Build Date", "Implementation-Build-Date" );
            } catch ( IOException ioE ) {
               return "";
            }
         }
      }
      return "";
   }

   static private String getAttribute( final Attributes attributes, final String name, final String attributeName ) {
      final String value = attributes.getValue( attributeName );
      return value == null ? "" : name + ": " + value + "\n";
   }


}
