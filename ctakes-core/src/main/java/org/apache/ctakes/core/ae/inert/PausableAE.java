package org.apache.ctakes.core.ae.inert;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {7/29/2022}
 */
@PipeBitInfo(
      name = "PausableAE",
      description = "Can be extended to add Pause capabilities to an Annotation Engine",
      role = PipeBitInfo.Role.ANNOTATOR
)
abstract public class PausableAE extends JCasAnnotator_ImplBase {

   static public final String PAUSE_PARAM = "Pause";
   static public final String PAUSE_DESC = "Pause for some seconds.  Default is 0";

   @ConfigurationParameter(
         name = PAUSE_PARAM,
         description = PAUSE_DESC,
         mandatory = false
   )
   private int _pause = 0;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
   }

   protected void pause() {
      pause( null );
   }

   protected void pause( final Logger logger ) {
      if ( _pause <= 0 ) {
         return;
      }
      final long pause = _pause * 1000L;
      if ( logger != null ) {
         logger.info( "Pausing " + _pause + " seconds ..." );
      }
      try ( DotLogger dotter = new DotLogger() ) {
         Thread.sleep( pause );
      } catch ( IOException | InterruptedException multE ) {
         // do nothing
      }
   }


}
