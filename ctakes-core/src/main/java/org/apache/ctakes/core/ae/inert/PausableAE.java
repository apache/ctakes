package org.apache.ctakes.core.ae.inert;

import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {7/29/2022}
 */
abstract public class PausableAE extends JCasAnnotator_ImplBase {

   static public final String PAUSE_PARAM = "Pause";
   static public final String PAUSE_DESC = "Pause for some seconds.  Default is 0";

   @ConfigurationParameter(
         name = PAUSE_PARAM,
         description = PAUSE_DESC,
         mandatory = false
   )
   private int _pause = 0;

   static public final String WAIT_PARAM = "Wait";
   static public final String WAIT_DESC = "Wait for the process to finish.  Default is no.";
   @ConfigurationParameter(
         name = WAIT_PARAM,
         description = WAIT_DESC,
         defaultValue = "no",
         mandatory = false
   )
   private String _wait;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
   }

   protected Logger getPauseLogger() {
      return null;
   }

   protected boolean shouldWait() {
      return _wait.equalsIgnoreCase( "yes" ) || _wait.equalsIgnoreCase( "true" );
   }

   final protected void pause() {
      if ( _pause < 1 ) {
         return;
      }
      final long pause = _pause * 1000L;
      try ( DotLogger dotter = new DotLogger( getPauseLogger(), "Pausing {} seconds ", _pause ) ) {
         Thread.sleep( pause );
      } catch ( IOException | InterruptedException multE ) {
         // do nothing
      }
   }


}
