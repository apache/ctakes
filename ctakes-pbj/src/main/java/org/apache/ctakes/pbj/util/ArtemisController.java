package org.apache.ctakes.pbj.util;

import org.apache.ctakes.core.ae.PausableFileLoggerAE;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.external.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;


/**
 * @author SPF , chip-nlp
 * @since {5/10/2022}
 */
@PipeBitInfo(
        name = "ArtemisController",
        description = "Controls the Artemis broker.  Abstract.",
        role = PipeBitInfo.Role.SPECIAL
)

abstract public class ArtemisController extends PausableFileLoggerAE {

    static public final String ARTEMIS_ROOT_PARAM = "ArtemisBroker";
    static public final String ARTEMIS_ROOT_DESC = "Your Artemis broker's root directory.";

    @ConfigurationParameter(
            name = ARTEMIS_ROOT_PARAM,
            description = ARTEMIS_ROOT_DESC
    )
    protected String _artemisRoot;

    /**
     *
     * @return a suffix for the default log file.
     */
    abstract protected String getLogSuffix();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean processPerDoc() {
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected String getLogFile() {
        final String logFile = super.getLogFile();
        if ( logFile != null && !logFile.isEmpty() ) {
            return logFile;
        }
        return getLogFile( "ctakes_artemis" + getLogSuffix() + ".log" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( final UimaContext context ) throws ResourceInitializationException {
        super.initialize( context );
        _artemisRoot = SystemUtil.subVariableParameters( _artemisRoot, context );
        if ( _artemisRoot != null && !_artemisRoot.isEmpty() && !( new File( _artemisRoot ).exists() ) ) {
            throw new ResourceInitializationException(
                  new IOException( "Cannot find Artemis Root Directory " + _artemisRoot ) );
        }
        if ( new File( _artemisRoot, "LICENSE" ).exists() && new File( _artemisRoot, "NOTICE" ).exists() ) {
            final Logger LOGGER = LoggerFactory.getLogger( "ArtemisController" );
            LOGGER.error( "It looks like " + _artemisRoot + " might point to an Apache Artemis Source or binary "
                         + "distribution." );
            LOGGER.error( "You must point " + ARTEMIS_ROOT_PARAM + " to the root directory of an Artemis broker." );
            LOGGER.error( "If you have not created a broker, use the 'artemis create' command to do so." );
            LOGGER.error( "For more information, search for 'artemis create' online.  It isn't part of cTAKES." );
            LOGGER.info( "If you are certain that the directory that you've specified contains a broker, remove its "
                         + "LICENSE or NOTICE file." );
            throw new ResourceInitializationException(
                  new IOException( "Cannot find Artemis Broker in Root Directory " + _artemisRoot ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process( final JCas jcas ) throws AnalysisEngineProcessException {
        // Implementation of the process(..) method is mandatory, even if it does nothing.
    }

}

