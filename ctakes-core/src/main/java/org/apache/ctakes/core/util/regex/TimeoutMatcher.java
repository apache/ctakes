package org.apache.ctakes.core.util.regex;

import org.apache.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class that can / should be used to find text spans using regular expressions.
 * It runs Matcher find {@link Matcher#find()} in a separate thread so that it may be interrupted at a set timeout.
 * This prevents infinite loop problems that can be caused by poorly-built expressions or unexpected text contents.
 * The timeout can be specified in milliseconds between 100 and 10,000.  Large timeouts are unadvised.  If a large
 * amount of text needs to be parsed then it is better to split up the text logically and use smaller timeouts.
 * The default timeout is 1000 milliseconds.
 * Extending Matcher would be better, but it is final
 * <p>
 * <p>
 * Proper usage is:
 * try ( TimeoutMatcher finder = new TimeoutMatcher( "\\s+", "Hello World !" ) ) {
 * Matcher matcher = finder.find();
 * while ( matcher != null ) {
 * ...
 * matcher = finder.find();
 * }
 * } catch ( IllegalArgumentException iaE ) {
 * ...
 * }
 * </p>
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/5/2016
 */
public class TimeoutMatcher implements Closeable {

   static private final Logger LOGGER = Logger.getLogger( "TimeoutMatcher" );

   static private final int DEFAULT_TIMEOUT_MILLIS = 1000;
   static private final int MIN_TIMEOUT_MILLIS = 100;
   static private final int MAX_TIMEOUT_MILLIS = 10000;

   private final ExecutorService _executor;
   private final int _timeoutMillis;
   private final Matcher _matcher;


   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param regex regular expression
    * @param text  text to parse
    * @throws IllegalArgumentException if the regular expression is null or malformed
    */
   public TimeoutMatcher( final String regex, final String text ) throws IllegalArgumentException {
      this( Pattern.compile( regex ), text );
   }

   /**
    * @param regex         regular expression
    * @param text          text to parse
    * @param timeoutMillis milliseconds at which the regex match should abort, between 100 and 10000
    * @throws IllegalArgumentException if the regular expression is null or malformed
    */
   public TimeoutMatcher( final String regex, final String text, final int timeoutMillis )
         throws IllegalArgumentException {
      this( Pattern.compile( regex ), text, timeoutMillis );
   }

   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param pattern Pattern compiled from a regular expression
    * @param text    text to parse
    * @throws IllegalArgumentException if the pattern is null or malformed
    */
   public TimeoutMatcher( final Pattern pattern, final String text ) throws IllegalArgumentException {
      this( pattern, text, DEFAULT_TIMEOUT_MILLIS );
   }

   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param pattern       Pattern compiled from a regular expression
    * @param text          text to parse
    * @param timeoutMillis milliseconds at which the regex match should abort, between 100 and 10000
    * @throws IllegalArgumentException if the pattern is null or malformed
    */
   public TimeoutMatcher( final Pattern pattern, final String text, final int timeoutMillis )
         throws IllegalArgumentException {
      if ( pattern == null ) {
         throw new PatternSyntaxException( "Pattern cannot be null", "", -1 );
      }
      if ( timeoutMillis < MIN_TIMEOUT_MILLIS || timeoutMillis > MAX_TIMEOUT_MILLIS ) {
         throw new IllegalArgumentException( "Timeout must be between "
                                             + MIN_TIMEOUT_MILLIS + " and " + MAX_TIMEOUT_MILLIS );
      }
      _matcher = pattern.matcher( new ThreadString( text ) );
      _timeoutMillis = timeoutMillis;
      _executor = Executors.newSingleThreadExecutor();
   }


   /**
    * @return a matcher representing the next call to {@link Matcher#find()}
    */
   public Matcher nextMatch() {
      final Callable<Matcher> callable = new RegexCallable();
      final Future<Matcher> future = _executor.submit( callable );
      try {
         return future.get( _timeoutMillis, TimeUnit.MILLISECONDS );
      } catch ( InterruptedException | ExecutionException | TimeoutException multE ) {
         LOGGER.debug( "Timeout for " + _matcher.pattern() );
         if ( !future.cancel( true ) ) {
            LOGGER.error( "Timed out but could not be cancelled while detecting " + _matcher.pattern() );
         }
      }
      if ( future.isCancelled() ) {
         LOGGER.error( "Cancelled while detecting " + _matcher.pattern() );
      } else if ( !future.isDone() ) {
         LOGGER.error( "Not cancelled but didn't complete while detecting " + _matcher.pattern() );
      }
      return null;
   }


   /**
    * shut down the executor
    * {@inheritDoc}
    */
   @Override
   public void close() {
      _executor.shutdownNow();
   }


   /**
    * Simple Callable that runs a {@link Matcher} on text
    */
   private final class RegexCallable implements Callable<Matcher> {

      private RegexCallable() {
      }

      /**
       * {@inheritDoc}
       *
       * @return matcher if there is another find, else null
       */
      @Override
      public Matcher call() {
         if ( _matcher.find() ) {
            return _matcher;
         }
         return null;
      }
   }

}
