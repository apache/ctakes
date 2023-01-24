package org.apache.ctakes.pbj.util;

import org.apache.ctakes.core.ae.PausableFileLoggerAE;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.external.SystemUtil;
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

    static public final String ARTEMIS_ROOT_PARAM = "ArtemisRoot";
    static public final String ARTEMIS_ROOT_DESC = "Your Artemis root directory.";

    @ConfigurationParameter(
            name = ARTEMIS_ROOT_PARAM,
            description = ARTEMIS_ROOT_DESC,
            mandatory = false
    )
    protected String _artemisRoot;

    static public final String WAIT_PARAM = "Wait";
    static public final String WAIT_DESC = "Wait for the launched command to finish.  Default is no.";
    @ConfigurationParameter(
          name = WAIT_PARAM,
          description = WAIT_DESC,
          defaultValue = "no",
          mandatory = false
    )
    protected String _wait;

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
        if ( logFile == null || logFile.isEmpty() ) {
            return _artemisRoot + File.separatorChar + "ctakes_artemis" + getLogSuffix() + ".log";
        }
        return logFile;
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process( final JCas jcas ) throws AnalysisEngineProcessException {
        // Implementation of the process(..) method is mandatory, even if it does nothing.
    }

}

