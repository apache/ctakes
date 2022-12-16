package org.apache.ctakes.core.util.regex;


import org.apache.ctakes.core.util.Pair;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * <p>
 * Proper usage is:
 * try ( RegexSpanFinder finder = new RegexSpanFinder( "\\s+" ) ) {
 * final List<Pair<Integer>> spans = finder.findSpans( "Hello World !" );
 * ...
 * } catch ( IllegalArgumentException iaE ) {
 * ...
 * }
 * </p>
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/5/2016
 */
final public class RegexSpanFinder implements Closeable {

   static private final Logger LOGGER = Logger.getLogger( "RegexSpanFinder" );

   static private final int DEFAULT_TIMEOUT_MILLIS = 1000;
   static private final int MIN_TIMEOUT_MILLIS = 100;
   static private final int MAX_TIMEOUT_MILLIS = 10000;

   private final ExecutorService _executor;
   private final Pattern _pattern;
   private final int _timeoutMillis;

   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param regex regular expression
    * @throws IllegalArgumentException if the regular expression is null or malformed
    */
   public RegexSpanFinder( final String regex ) throws IllegalArgumentException {
      this( Pattern.compile( regex ) );
   }

   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param regex         regular expression
    * @param flags         pattern flags; CASE_INSENSITIVE, etc.
    * @param timeoutMillis milliseconds at which the regex match should abort, between 100 and 10000
    * @throws IllegalArgumentException if the regular expression is null or malformed
    */
   public RegexSpanFinder( final String regex, final int flags, final int timeoutMillis ) throws IllegalArgumentException {
      this( Pattern.compile( regex, flags ), timeoutMillis );
   }

   /**
    * @param regex         regular expression
    * @param timeoutMillis milliseconds at which the regex match should abort, between 100 and 10000
    * @throws IllegalArgumentException if the regular expression is null or malformed
    */
   public RegexSpanFinder( final String regex, final int timeoutMillis ) throws IllegalArgumentException {
      this( Pattern.compile( regex ), timeoutMillis );
   }

   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param pattern Pattern compiled from a regular expression
    * @throws IllegalArgumentException if the pattern is null or malformed
    */
   public RegexSpanFinder( final Pattern pattern ) throws IllegalArgumentException {
      this( pattern, DEFAULT_TIMEOUT_MILLIS );
   }

   /**
    * Uses the default timeout of 1000 milliseconds
    *
    * @param pattern       Pattern compiled from a regular expression
    * @param timeoutMillis milliseconds at which the regex match should abort, between 100 and 10000
    * @throws IllegalArgumentException if the pattern is null or malformed
    */
   public RegexSpanFinder( final Pattern pattern, final int timeoutMillis ) throws IllegalArgumentException {
      if ( pattern == null ) {
         throw new PatternSyntaxException( "Pattern cannot be null", "", -1 );
      }
      if ( timeoutMillis < MIN_TIMEOUT_MILLIS || timeoutMillis > MAX_TIMEOUT_MILLIS ) {
         throw new IllegalArgumentException( "Timeout must be between "
                                             + MIN_TIMEOUT_MILLIS + " and " + MAX_TIMEOUT_MILLIS );
      }
      _pattern = pattern;
      _timeoutMillis = timeoutMillis;
      _executor = Executors.newSingleThreadExecutor();
   }


   /**
    * @param text text in which a find should be conducted
    * @return List of Integer Pairs representing text span begin and end offsets
    */
   public List<Pair<Integer>> findSpans( final String text ) {
      if ( text == null || text.isEmpty() ) {
         return Collections.emptyList();
      }
      final ThreadString threadText = new ThreadString( text );
      final Callable<List<Pair<Integer>>> callable = new RegexCallable( threadText, _pattern );
      final Future<List<Pair<Integer>>> future = _executor.submit( callable );
      try {
         return future.get( _timeoutMillis, TimeUnit.MILLISECONDS );
      } catch ( InterruptedException | ExecutionException | TimeoutException multE ) {
         LOGGER.debug( "Timeout for " + _pattern );
         if ( !future.cancel( true ) ) {
            LOGGER.error( "Timed out but could not be cancelled while detecting " + _pattern );
         }
      }
      if ( future.isCancelled() ) {
         LOGGER.error( "Cancelled while detecting " + _pattern );
      } else if ( !future.isDone() ) {
         LOGGER.error( "Not cancelled but didn't complete while detecting " + _pattern );
      }
      return Collections.emptyList();
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
    * Simple Callable that runs a {@link Matcher} on text to find text span begin and end offsets
    */
   static private final class RegexCallable implements Callable<List<Pair<Integer>>> {
      final private CharSequence __text;
      final private Pattern __pattern;

      private RegexCallable( final CharSequence text, final Pattern pattern ) {
         __text = text;
         __pattern = pattern;
      }

      /**
       * {@inheritDoc}
       *
       * @return text span begin and end offsets
       */
      @Override
      public List<Pair<Integer>> call() {
         final List<Pair<Integer>> listBounds = new ArrayList<>();
         final Matcher matcher = __pattern.matcher( __text );
         while ( matcher.find() && !Thread.currentThread().isInterrupted() ) {
            final Pair<Integer> bounds = new Pair<>( matcher.start(), matcher.end() );
            if ( bounds.getValue1() >= 0 && bounds.getValue2() > bounds.getValue1() &&
                 bounds.getValue2() <= __text.length() ) {
               listBounds.add( bounds );
            }
         }
         return listBounds;
      }
   }

}
