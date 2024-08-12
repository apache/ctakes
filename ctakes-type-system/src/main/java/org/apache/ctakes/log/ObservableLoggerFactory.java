package org.apache.ctakes.log;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this factory to retrieve a logger in ctakes.
 * It returns an implementation of a slf4j Logger that can have listeners added to it.
 * This allows a ctakes LoggerListener to receive notification of logging events and do things with them,
 * such as print them in a gui panel.
 *
 * @author SPF , chip-nlp
 * @since {8/5/2024}
 */
final public class ObservableLoggerFactory { //implements ILoggerFactory {

   private ObservableLoggerFactory() {}

   public static Logger getLogger( final String name ) {
      return new ObservableLogger( LoggerFactory.getLogger( name ) );
   }

   public static Logger getLogger( final Class<?> clazz ) {
      return new ObservableLogger( LoggerFactory.getLogger( clazz ) );
   }

   public static Logger getRootLogger() {
      return getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
   }

}
