package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * All Annotation Engines should be logger their start and finish.
 * Such logging not only keeps track of what is actually in the pipeline, but it also helps with debugging and profiling
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/8/2016
 */
@PipeBitInfo(
      name = "Start or Finish Logger",
      description = "Simple Annotator to place before and after other annotators that do not Log their Start and Finish.",
      role = PipeBitInfo.Role.SPECIAL
)
public class StartFinishLogger extends JCasAnnotator_ImplBase {


   public static final String PARAM_LOGGER_NAME = "LOGGER_NAME";
   @ConfigurationParameter(
         name = PARAM_LOGGER_NAME,
         description = "provides the full name of the Annotator Engine for which start / end logging should be done.",
         defaultValue = { "StartEndProgressLogger" }
   )
   private String _loggerName;

   public static final String PARAM_LOGGER_TASK = "LOGGER_TASK";
   static private final String DEFAULT_TASK = "Processing ...";
   @ConfigurationParameter(
         name = PARAM_LOGGER_TASK,
         mandatory = false,
         description = "provides the descriptive purpose of the Annotator Engine for which start / end logging should be done.",
         defaultValue = { DEFAULT_TASK }
   )
   private String _loggerTask;

   public static final String PARAM_IS_START = "IS_START";
   @ConfigurationParameter(
         name = PARAM_IS_START,
         mandatory = false,
         description = "indicates whether this should log a start."
   )
   private Boolean _isStart;

   private Logger _logger;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context )
         throws ResourceInitializationException {
      super.initialize( context );
      _logger = Logger.getLogger( _loggerName );
      if ( _isStart != null && _isStart ) {
         if ( _loggerTask == null || _loggerTask.equals( DEFAULT_TASK ) ) {
            _logger.info( "Starting initializing" );
         } else {
            _logger.info( "Starting initializing for " + _loggerTask );
         }
      } else {
         _logger.info( "Finished initializing" );
      }

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( _isStart != null && _isStart ) {
         if ( _loggerTask == null || _loggerTask.equals( DEFAULT_TASK ) ) {
            _logger.info( "Starting processing ..." );
         } else {
            _logger.info( _loggerTask + " ..." );
         }
      } else {
         if ( _loggerTask == null || _loggerTask.equals( DEFAULT_TASK ) ) {
            _logger.info( "Finished processing" );
         } else {
            _logger.info( "Finished " + _loggerTask );
         }
      }
   }

   /**
    * This method should be avoided.  See the bottom of https://uima.apache.org/d/uimafit-current/api/index.html
    *
    * @param loggerName name for the logger
    * @param isStart    true to return an Engine that logs the Start, false to return an Engine that logs Finish
    * @return Simple Start/Finish Logger Engine
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public static AnalysisEngine createEngine( final String loggerName, final boolean isStart )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngine( StartFinishLogger.class,
            PARAM_LOGGER_NAME, loggerName,
            PARAM_IS_START, isStart );
   }

   /**
    * @param loggerName name for the logger
    * @param isStart    true to return an Engine that logs the Start, false to return an Engine that logs Finish
    * @return Simple Start/Finish Logger Engine
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public static AnalysisEngineDescription createDescription( final String loggerName, final boolean isStart )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( StartFinishLogger.class,
            PARAM_LOGGER_NAME, loggerName,
            PARAM_IS_START, isStart );
   }


   /**
    * This method should be avoided.  See the bottom of https://uima.apache.org/d/uimafit-current/api/index.html
    *
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Engine that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public static AnalysisEngine createLoggedEngine( final Class<? extends AnalysisComponent> classType,
                                                    final Object... parameters )
         throws ResourceInitializationException {
      final AnalysisEngineDescription description = createLoggedDescription( classType, parameters );
      return AnalysisEngineFactory.createEngine( description, parameters );
   }

   /**
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Description that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public static AnalysisEngineDescription createLoggedDescription( final Class<? extends AnalysisComponent> classType,
                                                                    final Object... parameters )
         throws ResourceInitializationException {
      final AnalysisEngineDescription mainDescription
            = AnalysisEngineFactory.createEngineDescription( classType, parameters );
      return createLoggedDescription( mainDescription );
   }

   /**
    * @param mainDescription main component description
    * @return Description that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public static AnalysisEngineDescription createLoggedDescription( final AnalysisEngineDescription mainDescription )
         throws ResourceInitializationException {
      final String name = mainDescription.getAnnotatorImplementationName();
      return AnalysisEngineFactory.createEngineDescription(
            createDescription( name, true ),
            mainDescription,
            createDescription( name, false ) );
   }


}
