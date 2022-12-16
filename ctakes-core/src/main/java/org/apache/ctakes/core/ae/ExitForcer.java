package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.ae.inert.PausableAE;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @since {7/29/2022}
 */
@PipeBitInfo(
      name = "ExitForcer",
      description = "Forcibly Exits cTAKES.  Use only at the end of a pipeline.",
      role = PipeBitInfo.Role.SPECIAL
)
public class ExitForcer extends PausableAE {

   static private final Logger LOGGER = Logger.getLogger( "ExitForcer" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      // Do nothing
   }

   /**
    * Forcibly exit.  If ctakes happens to be running within a gui then a message dialog is displayed.
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      pause( LOGGER );
      final Frame[] frames = Frame.getFrames();
      if ( frames != null && frames.length > 0 ) {
         JOptionPane.showMessageDialog( null, "Processing Complete.  Click OK to exit." );
      }
      LOGGER.info( "Exiting." );
      System.exit( 0 );
   }


}
