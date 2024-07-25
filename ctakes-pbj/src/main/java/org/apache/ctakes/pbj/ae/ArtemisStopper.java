package org.apache.ctakes.pbj.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.pbj.util.ArtemisController;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {5/10/2022}
 */
@PipeBitInfo(
        name = "ArtemisStopper",
        description = "Stops an Apache Artemis broker.",
        role = PipeBitInfo.Role.SPECIAL
)
public class ArtemisStopper extends ArtemisController {

    static private final Logger LOGGER = LogManager.getLogger( "ArtemisStopper" );

    private boolean _stopped = false;

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    protected String getLogSuffix() {
        return "_stop";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( final UimaContext context ) throws ResourceInitializationException {
        super.initialize( context );
        registerShutdownHook();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            runCommand();
        } catch ( IOException ioE ) {
            throw new AnalysisEngineProcessException( ioE );
        }
    }


    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    protected void runCommand() throws IOException {
        _stopped = true;
        pause();
        final SystemUtil.CommandRunner runner
              = new SystemUtil.CommandRunner( "bin" + File.separatorChar + "artemis stop" );
        final String logFile = getLogFile();
        runner.setLogFiles( logFile );
        runner.setDirectory( _artemisRoot );
        runner.wait( shouldWait() );
        runner.setSetJavaHome( false );
        LOGGER.info( "Stopping Apache Artemis ..." );
        SystemUtil.run( runner );
    }




    static public AnalysisEngineDescription createEngineDescription( final String artemisRootDir )
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription( ArtemisStopper.class,
                                                              ArtemisController.ARTEMIS_ROOT_PARAM,
                                                              artemisRootDir );
    }

    /**
     * Registers a shutdown hook for the process so that it shuts down when the VM exits.
     * This includes kill signals and user actions like "Ctrl-C".
     */
    private void registerShutdownHook() {
        Runtime.getRuntime()
               .addShutdownHook( new Thread( () -> {
                   try {
                       if ( _stopped ) {
                           return;
                       }
                       runCommand();
                   } catch ( IOException ioE ) {
                       LOGGER.error( "Could not stop Artemis.", ioE );
                   }
               } ) );
    }

}
