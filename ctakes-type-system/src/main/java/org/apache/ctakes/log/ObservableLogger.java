package org.apache.ctakes.log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.DefaultLoggingEvent;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventAware;

import java.util.Collection;
import java.util.HashSet;

import static org.slf4j.event.Level.*;

/**
 * A logger that implements the Observer pattern.
 * It forwards logging to a slf4j Logger, and also sends an slf4j LoggingEvent to all registered slf4j
 * LoggingEventAware objects that have been added as listeners.
 *
 *
 * @author SPF , chip-nlp
 * @since {8/5/2024}
 */
final public class ObservableLogger implements Logger {

   private final Logger _delegate;

   // No real concerns about thread safety ...
   static private final Collection<LoggingEventAware> _listeners = new HashSet<>();

   static private final Object[] EMPTY_ARGS = new Object[]{};


   public ObservableLogger( final Logger delegate ) {
      _delegate = delegate;
   }

   public void addListener( final LoggingEventAware listener ) {
      _listeners.add( listener );
   }

   public void removeListener( final LoggingEventAware listener ) {
      _listeners.remove( listener );
   }


   @Override
   public String getName() {
      return _delegate.getName();
   }

   @Override
   public boolean isTraceEnabled() {
      return _delegate.isTraceEnabled();
   }


   @Override
   public void trace( String s ) {
      _delegate.trace( s );
      fireLogEvent( TRACE, s, EMPTY_ARGS, null );
   }

   @Override
   public void trace( String s, Object o ) {
      _delegate.trace( s, o );
      fireLogEvent( TRACE, s, new Object[]{ o }, null );
   }

   @Override
   public void trace( String s, Object o, Object o1 ) {
      _delegate.trace( s, o, o1 );
      fireLogEvent( TRACE, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void trace( String s, Object... objects ) {
      _delegate.trace( s, objects );
      fireLogEvent( TRACE, s, objects, null );
   }

   @Override
   public void trace( String s, Throwable throwable ) {
      _delegate.trace( s, throwable );
      fireLogEvent( TRACE, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isTraceEnabled( Marker marker ) {
      return _delegate.isTraceEnabled( marker );
   }

   @Override
   public void trace( Marker marker, String s ) {
      _delegate.trace( marker, s );
      fireLogEvent( TRACE, marker, s, EMPTY_ARGS, null );
   }

   @Override
   public void trace( Marker marker, String s, Object o ) {
      _delegate.trace( marker, s, o );
      fireLogEvent( TRACE, marker, s, new Object[]{ o }, null );
   }

   @Override
   public void trace( Marker marker, String s, Object o, Object o1 ) {
      _delegate.trace( marker, s, o, o1 );
      fireLogEvent( TRACE, marker, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void trace( Marker marker, String s, Object... objects ) {
      _delegate.trace( marker, s, objects );
      fireLogEvent( TRACE, marker, s, objects, null );
   }

   @Override
   public void trace( Marker marker, String s, Throwable throwable ) {
      _delegate.trace( marker, s, throwable );
      fireLogEvent( TRACE, marker, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isDebugEnabled() {
      return _delegate.isDebugEnabled();
   }

   @Override
   public void debug( String s ) {
      _delegate.debug( s );
      fireLogEvent( DEBUG, s, EMPTY_ARGS, null );
   }

   @Override
   public void debug( String s, Object o ) {
      _delegate.debug( s, o );
      fireLogEvent( DEBUG, s, new Object[]{ o }, null );
   }

   @Override
   public void debug( String s, Object o, Object o1 ) {
      _delegate.debug( s, o, o1 );
      fireLogEvent( DEBUG, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void debug( String s, Object... objects ) {
      _delegate.debug( s, objects );
      fireLogEvent( DEBUG, s, objects, null );
   }

   @Override
   public void debug( String s, Throwable throwable ) {
      _delegate.debug( s, throwable );
      fireLogEvent( DEBUG, s, null, throwable );
   }

   @Override
   public boolean isDebugEnabled( Marker marker ) {
      return _delegate.isDebugEnabled( marker );
   }

   @Override
   public void debug( Marker marker, String s ) {
      _delegate.debug( marker, s );
      fireLogEvent( DEBUG, marker, s, EMPTY_ARGS, null );
   }

   @Override
   public void debug( Marker marker, String s, Object o ) {
      _delegate.debug( marker, s, o );
      fireLogEvent( DEBUG, marker, s, new Object[]{ o }, null );
   }

   @Override
   public void debug( Marker marker, String s, Object o, Object o1 ) {
      _delegate.debug( marker, s, o, o1 );
      fireLogEvent( DEBUG, marker, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void debug( Marker marker, String s, Object... objects ) {
      _delegate.debug( marker, s, objects );
      fireLogEvent( DEBUG, marker, s, objects, null );
   }

   @Override
   public void debug( Marker marker, String s, Throwable throwable ) {
      _delegate.debug( marker, s, throwable );
      fireLogEvent( DEBUG, marker, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isInfoEnabled() {
      return _delegate.isInfoEnabled();
   }

   @Override
   public void info( String s ) {
      _delegate.info( s );
      fireLogEvent( INFO, s, EMPTY_ARGS, null );
   }

   @Override
   public void info( String s, Object o ) {
      _delegate.info( s, o );
      fireLogEvent( INFO, s, new Object[]{ o }, null );
   }

   @Override
   public void info( String s, Object o, Object o1 ) {
      _delegate.info( s, o, o1 );
      fireLogEvent( INFO, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void info( String s, Object... objects ) {
      _delegate.info( s, objects );
      fireLogEvent( INFO, s, objects, null );
   }

   @Override
   public void info( String s, Throwable throwable ) {
      _delegate.info( s, throwable );
      fireLogEvent( INFO, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isInfoEnabled( Marker marker ) {
      return _delegate.isInfoEnabled( marker );
   }

   @Override
   public void info( Marker marker, String s ) {
      _delegate.info( marker, s );
      fireLogEvent( INFO, marker, s, EMPTY_ARGS, null );
   }

   @Override
   public void info( Marker marker, String s, Object o ) {
      _delegate.info( marker, s, o );
      fireLogEvent( INFO, marker, s, new Object[]{ o }, null );
   }

   @Override
   public void info( Marker marker, String s, Object o, Object o1 ) {
      _delegate.info( marker, s, o, o1 );
      fireLogEvent( INFO, marker, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void info( Marker marker, String s, Object... objects ) {
      _delegate.info( marker, s, objects );
      fireLogEvent( INFO, marker, s, objects, null );
   }

   @Override
   public void info( Marker marker, String s, Throwable throwable ) {
      _delegate.info( marker, s, throwable );
      fireLogEvent( INFO, marker, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isWarnEnabled() {
      return _delegate.isWarnEnabled();
   }

   @Override
   public void warn( String s ) {
      _delegate.warn( s );
      fireLogEvent( WARN, s, EMPTY_ARGS, null );
   }

   @Override
   public void warn( String s, Object o ) {
      _delegate.warn( s, o );
      fireLogEvent( WARN, s, new Object[]{ o }, null );
   }

   @Override
   public void warn( String s, Object... objects ) {
      _delegate.warn( s, objects );
      fireLogEvent( WARN, s, objects, null );
   }

   @Override
   public void warn( String s, Object o, Object o1 ) {
      _delegate.warn( s, o, o1 );
      fireLogEvent( WARN, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void warn( String s, Throwable throwable ) {
      _delegate.warn( s, throwable );
      fireLogEvent( WARN, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isWarnEnabled( Marker marker ) {
      return _delegate.isWarnEnabled( marker );
   }

   @Override
   public void warn( Marker marker, String s ) {
      _delegate.warn( marker, s );
      fireLogEvent( WARN, marker, s, EMPTY_ARGS, null );
   }

   @Override
   public void warn( Marker marker, String s, Object o ) {
      _delegate.warn( marker, s, o );
      fireLogEvent( WARN, marker, s, new Object[]{ o }, null );
   }

   @Override
   public void warn( Marker marker, String s, Object o, Object o1 ) {
      _delegate.warn( marker, s, o, o1 );
      fireLogEvent( WARN, marker, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void warn( Marker marker, String s, Object... objects ) {
      _delegate.warn( marker, s, objects );
      fireLogEvent( WARN, marker, s, objects, null );
   }

   @Override
   public void warn( Marker marker, String s, Throwable throwable ) {
      _delegate.warn( marker, s, throwable );
      fireLogEvent( WARN, marker, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isErrorEnabled() {
      return _delegate.isErrorEnabled();
   }

   @Override
   public void error( String s ) {
      _delegate.error( s );
      fireLogEvent( ERROR, s, EMPTY_ARGS, null );
   }

   @Override
   public void error( String s, Object o ) {
      _delegate.error( s, o );
      fireLogEvent( ERROR, s, new Object[]{ o }, null );
   }

   @Override
   public void error( String s, Object o, Object o1 ) {
      _delegate.error( s, o, o1 );
      fireLogEvent( ERROR, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void error( String s, Object... objects ) {
      _delegate.error( s, objects );
      fireLogEvent( ERROR, s, objects, null );
   }

   @Override
   public void error( String s, Throwable throwable ) {
      _delegate.error( s, throwable );
      fireLogEvent( ERROR, s, EMPTY_ARGS, throwable );
   }

   @Override
   public boolean isErrorEnabled( Marker marker ) {
      return _delegate.isErrorEnabled( marker );
   }

   @Override
   public void error( Marker marker, String s ) {
      _delegate.error( marker, s );
      fireLogEvent( ERROR, marker, s, EMPTY_ARGS, null );
   }

   @Override
   public void error( Marker marker, String s, Object o ) {
      _delegate.error( marker, s, o );
      fireLogEvent( ERROR, marker, s, new Object[]{ o }, null );
   }

   @Override
   public void error( Marker marker, String s, Object o, Object o1 ) {
      _delegate.error( marker, s, o, o1 );
      fireLogEvent( ERROR, marker, s, new Object[]{ o, o1 }, null );
   }

   @Override
   public void error( Marker marker, String s, Object... objects ) {
      _delegate.error( marker, s, objects );
      fireLogEvent( ERROR, marker, s, objects, null );
   }

   @Override
   public void error( Marker marker, String s, Throwable throwable ) {
      _delegate.error( marker, s, throwable );
      fireLogEvent( ERROR, marker, s, EMPTY_ARGS, throwable );
   }

   private boolean isLevelEnabled( final Level level ) {
      return switch ( level ) {
         case ERROR -> isErrorEnabled();
         case WARN -> isWarnEnabled();
         case INFO -> isInfoEnabled();
         case DEBUG -> isDebugEnabled();
         case TRACE -> isTraceEnabled();
      };
   }

   private boolean isLevelEnabled( final Level level, final Marker marker ) {
      if ( marker == null ) {
         return isLevelEnabled( level );
      }
      return switch ( level ) {
         case ERROR -> isErrorEnabled( marker );
         case WARN -> isWarnEnabled( marker );
         case INFO -> isInfoEnabled( marker );
         case DEBUG -> isDebugEnabled( marker );
         case TRACE -> isTraceEnabled( marker );
      };
   }

   private void fireLogEvent( final Level level,
                             final String message,
                             final Object[] args,
                             final Throwable throwable ) {
     if ( _listeners.isEmpty() || !isLevelEnabled( level ) ) {
        return;
     }
     final DefaultLoggingEvent event = new DefaultLoggingEvent( level, this );
      event.setTimeStamp( System.currentTimeMillis() );
      event.setMessage( message );
      event.addArguments( args );
      event.setThrowable( throwable );
      _listeners.forEach( l -> l.log( event ) );
   }

   private void fireLogEvent( final Level level,
                              final Marker marker,
                              final String message,
                              final Object[] args,
                              final Throwable throwable ) {
      if ( _listeners.isEmpty() || !isLevelEnabled( level, marker ) ) {
         return;
      }
      final DefaultLoggingEvent event = new DefaultLoggingEvent( level, this );
      event.setTimeStamp( System.currentTimeMillis() );
      event.addMarker( marker );
      event.setMessage( message );
      event.addArguments( args );
      event.setThrowable( throwable );
      _listeners.forEach( l -> l.log( event ) );
   }

}
