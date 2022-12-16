package org.apache.ctakes.gui.pipeline.piper;


import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.ScanInterruptedException;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessorWithContext;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import io.github.lukehutch.fastclasspathscanner.utils.ClasspathUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/24/2017
 */
public class PiperFinder {

   static private final Logger LOGGER = Logger.getLogger( "PiperFinder" );


   private final Collection<PiperInfo> _piperFiles = new ArrayList<>();
   private boolean _didScan = false;

   synchronized public void reset() {
      _piperFiles.clear();
      _didScan = false;
   }

   synchronized public Collection<PiperInfo> getPiperInfos() {
      scan();
      return _piperFiles;
   }

   synchronized public void scan() {
      if ( _didScan ) {
         return;
      }
      final FastClasspathScanner scanner = new FastClasspathScanner();
      LOGGER.info( "Starting Scan for Piper Filess" );
      try {
         scanner.matchFilenameExtension( "piper", new PiperAdder() );
         final ScanResult result = scanner.scan();
      } catch ( ScanInterruptedException siE ) {
         LOGGER.error( siE.getMessage() );
      }
      LOGGER.info( "Scan Finished" );
      _didScan = true;
   }

   private class PiperAdder implements FileMatchProcessorWithContext {
      @Override
      public void processMatch( final File classpathPrefix, final String relativePath,
                                final InputStream inputStream, final long lengthBytes ) throws IOException {
         final URL url = ClasspathUtils.getClasspathResourceURL( classpathPrefix, relativePath );
         final String fullPath = url.toExternalForm();
         final String simplePath = url.getPath();
         _piperFiles.add( new PiperInfo( fullPath, simplePath ) );
      }
   }

}
