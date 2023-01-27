package org.apache.ctakes.pbj.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.pbj.util.ArtemisController;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {5/10/2022}
 */
@PipeBitInfo(
      name = "ArtemisStarter",
      description = "Starts an Apache Artemis broker.",
      role = PipeBitInfo.Role.SPECIAL
)
public class ArtemisStarter extends ArtemisController {

   static private final Logger LOGGER = Logger.getLogger( "ArtemisStarter" );

   /**
    * {@inheritDoc}
    * @return
    */
   @Override
   protected String getLogSuffix() {
      return "_start";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         runCommand();
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
   }

   /**
    * {@inheritDoc}
    * @return
    */
   @Override
   protected void runCommand() throws IOException {
      final SystemUtil.CommandRunner runner
            = new SystemUtil.CommandRunner( "bin" + File.separatorChar + "artemis run" );
      final String logFile = getLogFile();
      runner.setLogFiles( logFile );
      runner.setDirectory( _artemisRoot );
      if ( _wait.equalsIgnoreCase( "yes" ) || _wait.equalsIgnoreCase( "true" ) ) {
         runner.wait( true );
      }
      runner.setSetJavaHome( false );
      LOGGER.info( "Starting Apache Artemis ..." );
      SystemUtil.run( runner );
      pause();
   }


   static public AnalysisEngineDescription createEngineDescription( final String artemisRootDir )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ArtemisStarter.class,
                                                            ArtemisController.ARTEMIS_ROOT_PARAM,
                                                            artemisRootDir );

   }


}
