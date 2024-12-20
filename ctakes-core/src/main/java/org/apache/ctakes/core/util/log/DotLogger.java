package org.apache.ctakes.core.util.log;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

   static private final Logger DOT_LOGGER = LoggerFactory.getLogger( "ProgressAppender" );
   static private final Logger EOL_LOGGER = LoggerFactory.getLogger( "ProgressDone" );

   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "dd MMM yyyy hh:mm:ss" );

   private final ExecutorService _timer;

   /**
    * Starts the Dot Logging.
    */
   public DotLogger() {
      this( "" );
   }

   /**
    * @param message a message to log before dots.
    * @param arguments any arguments for substitution in the message.
    * Starts the Dot Logging with a starting message.
    */
   public DotLogger( final String message, final Object ... arguments ) {
      this( null, message, arguments );
   }

   /**
    * @param logger a specific logger to use for the message.
    * @param message a message to log before dots.
    * @param arguments any arguments for substitution in the message.
    * Starts the Dot Logging with a starting message.
    */
   public DotLogger( final Logger logger, final String message, final Object ... arguments ) {
      if ( !message.isEmpty() ) {
         DOT_LOGGER.info( createMessage( logger, message ), arguments );
      }
      _timer = Executors.newScheduledThreadPool( 1 );
      ((ScheduledExecutorService)_timer).scheduleAtFixedRate(
            new DotPlotter(), 500, 500, TimeUnit.MILLISECONDS );
   }

   static private String createMessage( final Logger logger, final String message ) {
      return DATE_FORMAT.format( System.currentTimeMillis() ) + "  INFO "
            + ((logger == null) ? "" : logger.getName()) + " - " + message;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void close() throws IOException {
      _timer.shutdownNow();
      EOL_LOGGER.info( "" );
   }

   private static class DotPlotter extends TimerTask {
      private int _count = 0;

      @Override
      public void run() {
         DOT_LOGGER.info( "." );
         _count++;
         if ( _count % 60 == 0 ) {
            if ( _count % 120 == 0 ) {
               EOL_LOGGER.info( " {}", _count / 2 );
            } else {
               DOT_LOGGER.info( " {} ", _count / 2 );
            }
         }
      }
   }

}
