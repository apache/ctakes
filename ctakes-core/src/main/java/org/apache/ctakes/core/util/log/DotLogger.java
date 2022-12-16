package org.apache.ctakes.core.util.log;

import java.io.Closeable;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dot Logger Usable in try as resource blocks.  Logs a dot every 0.5 seconds until a process completes.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/18/2016
 */
final public class DotLogger implements Closeable {

   static private final org.apache.log4j.Logger DOT_LOGGER = org.apache.log4j.Logger.getLogger( "ProgressAppender" );
   static private final org.apache.log4j.Logger EOL_LOGGER = org.apache.log4j.Logger.getLogger( "ProgressDone" );

   private final ExecutorService _timer;

   /**
    * Starts the Dot Logging
    */
   public DotLogger() {
      _timer = Executors.newScheduledThreadPool( 1 );
      ((ScheduledExecutorService)_timer).scheduleAtFixedRate(
            new DotPlotter(), 500, 500, TimeUnit.MILLISECONDS );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void close() throws IOException {
      _timer.shutdownNow();
      EOL_LOGGER.info( "" );
   }

   private class DotPlotter extends TimerTask {
      private int _count = 0;

      @Override
      public void run() {
         DOT_LOGGER.info( "." );
         _count++;
         if ( _count % 60 == 0 ) {
            if ( _count % 120 == 0 ) {
               EOL_LOGGER.info( " " + (_count / 2) );
            } else {
               DOT_LOGGER.info( " " + (_count / 2) + " " );
            }
         }
      }
   }

}
