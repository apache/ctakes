package org.apache.ctakes.core.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Dot Logger Usable in try as resource blocks.  Logs a dot every 0.5 seconds until a process completes.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/18/2016
 * @deprecated use DotLogger in (sub) package log
 */
@Deprecated
final public class DotLogger implements Closeable {

   final private org.apache.ctakes.core.util.log.DotLogger _delegate;

   /**
    * Starts the Dot Logging
    */
   @Deprecated
   public DotLogger() {
      _delegate = new org.apache.ctakes.core.util.log.DotLogger();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   @Deprecated
   public void close() throws IOException {
      _delegate.close();
   }

}
