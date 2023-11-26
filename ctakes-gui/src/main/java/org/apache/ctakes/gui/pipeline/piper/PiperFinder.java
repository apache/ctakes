package org.apache.ctakes.gui.pipeline.piper;

import io.github.classgraph.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

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
      final ClassGraph scanner = new ClassGraph();
      LOGGER.info( "Starting Scan for Piper Filess" );
      try (ScanResult result = scanner.scan()){
    	  ResourceList list = result.getResourcesWithExtension("piper");
    	  for(Resource resource : list) {
    		  piperAdder(resource);
    	  }
      } catch ( Exception ex  ) {
         LOGGER.error( ex.getMessage() );
      }
      LOGGER.info( "Scan Finished" );
      _didScan = true;
   }

   synchronized private void piperAdder(Resource resource) {
	   String fullPath = resource.getURI().toASCIIString();
	   String simplePath = resource.getPath();
	   _piperFiles.add( new PiperInfo( fullPath, simplePath ) );
   }
   
   /**
    * unit test
    * @param argv
    */
   
   public static void main(String[] argv) {
	   PiperFinder pf = new PiperFinder();
	   Collection<PiperInfo> pi = pf.getPiperInfos();
	   pi.forEach(new Consumer<PiperInfo>() {
		@Override
		public void accept(PiperInfo t) {
			String urlPath = t.getUrlPath();
			if(urlPath.startsWith("jar:")) {
				int endpoint = urlPath.indexOf(".jar!");
				int startpoint = urlPath.lastIndexOf("/", endpoint);
				String jar = urlPath.substring(startpoint, endpoint + 4);
				System.out.println("In jar: " + jar + "  " + t.getFilePath());
			} else {
				System.out.println("\n\nFull path: " +t.getUrlPath());
			}
		}
	   });
   }

//private class PiperAdder implements FileMatchProcessorWithContext {
//      @Override
//      public void processMatch( final File classpathPrefix, final String relativePath,
//                                final InputStream inputStream, final long lengthBytes ) throws IOException {
//         final URL url = ClasspathUtils.getClasspathResourceURL( classpathPrefix, relativePath );
//         final String fullPath = url.toExternalForm();
//         final String simplePath = url.getPath();
//         _piperFiles.add( new PiperInfo( fullPath, simplePath ) );
//      }
//   }

}
