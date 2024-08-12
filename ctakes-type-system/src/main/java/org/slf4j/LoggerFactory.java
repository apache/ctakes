/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.log.ObservableLogger;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Modification of slf4j 2.0.13 LoggerFactory that can use an Observable after using whatever provider slf4j
 * would normally find.
 * There were several ways to handle this versatility that really should come with slf4j,
 * creating a custom forwarding provider, playing with the classloader, using a wrapper around slf4j ...
 * In the end this approach might be the simplest to understand and modify for other developers.
 * This class simply delegates to a copy of the actual slf4j LoggerFactory (Slf4jLoggerFactory_Actual).
 *
 * The <code>LoggerFactory</code> is a utility class producing Loggers for
 * various logging APIs, e.g. logback, reload4j, log4j and JDK 1.4 logging.
 * Other implementations such as {@link org.slf4j.helpers.NOPLogger NOPLogger} and
 * SimpleLogger are also supported.
 *
 * <p><code>LoggerFactory</code>  is essentially a wrapper around an
 * {@link ILoggerFactory} instance provided by a {@link SLF4JServiceProvider}.
 *
 * <p>
 * Please note that all methods in <code>LoggerFactory</code> are static.
 *
 * @author Alexander Dorokhine
 * @author Robert Elliot
 * @author Ceki G&uuml;lc&uuml;
 *
 * Modified by SPF 8/2024
 */
public final class LoggerFactory {


   // Package access for tests
   static List<SLF4JServiceProvider> findServiceProviders() {
      return Slf4jLoggerFactory_Actual.findServiceProviders();
   }

   // private constructor prevents instantiation
   private LoggerFactory() {
   }

   /**
    * Force LoggerFactory to consider itself uninitialized.
    * <p>
    * <p>
    * This method is intended to be called by classes (in the same package) for
    * testing purposes. This method is internal. It can be modified, renamed or
    * removed at any time without notice.
    * <p>
    * <p>
    * You are strongly discouraged from calling this method in production code.
    */
   static void reset() {
      Slf4jLoggerFactory_Actual.reset();
   }


   static SLF4JServiceProvider loadExplicitlySpecified(ClassLoader classLoader) {
      return Slf4jLoggerFactory_Actual.loadExplicitlySpecified( classLoader );
   }

   static Set<URL> findPossibleStaticLoggerBinderPathSet() {
      return Slf4jLoggerFactory_Actual.findPossibleStaticLoggerBinderPathSet();
   }

   static void failedBinding(Throwable t) {
      Slf4jLoggerFactory_Actual.failedBinding( t );
   }

   /**
    * Return a logger named according to the name parameter using the
    * statically bound {@link ILoggerFactory} instance.
    *
    * @param name
    *            The name of the logger.
    * @return logger
    */
   public static Logger getLogger( String name ) {
      final Logger delegate = Slf4jLoggerFactory_Actual.getLogger( name );
      return new ObservableLogger( delegate );
   }

   /**
    * Return a logger named corresponding to the class passed as parameter,
    * using the statically bound {@link ILoggerFactory} instance.
    *
    * <p>
    * In case the <code>clazz</code> parameter differs from the name of the
    * caller as computed internally by SLF4J, a logger name mismatch warning
    * will be printed but only if the
    * <code>slf4j.detectLoggerNameMismatch</code> system property is set to
    * true. By default, this property is not set and no warnings will be
    * printed even in case of a logger name mismatch.
    *
    * @param clazz
    *            the returned logger will be named after clazz
    * @return logger
    *
    *
    * @see <a
    *      href="http://www.slf4j.org/codes.html#loggerNameMismatch">Detected
    *      logger name mismatch</a>
    */
   public static Logger getLogger(Class<?> clazz) {
      final Logger delegate = Slf4jLoggerFactory_Actual.getLogger( clazz );
      return new ObservableLogger( delegate );
   }

   /**
    * Return the {@link ILoggerFactory} instance in use.
    * <p>
    * <p>
    * ILoggerFactory instance is bound with this class at compile time.
    *
    * @return the ILoggerFactory instance in use
    */
   public static ILoggerFactory getILoggerFactory() {
      return Slf4jLoggerFactory_Actual.getILoggerFactory();
   }

   /**
    * Return the {@link SLF4JServiceProvider} in use.

    * @return provider in use
    * @since 1.8.0
    */
   static SLF4JServiceProvider getProvider() {
      return Slf4jLoggerFactory_Actual.getProvider();
   }


}