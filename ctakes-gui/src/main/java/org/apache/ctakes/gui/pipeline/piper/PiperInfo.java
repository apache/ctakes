package org.apache.ctakes.gui.pipeline.piper;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/24/2017
 */
final public class PiperInfo {

   static private final Logger LOGGER = LogManager.getLogger( "PiperInfo" );

   private final String _urlPath;
   private final String _filePath;
   private final boolean _readOnly;

   public PiperInfo( final String urlPath, final String filePath ) {
      this( urlPath, filePath, urlPath.startsWith( "jar:" ) );
   }

   public PiperInfo( final String urlPath, final String filePath, final boolean readOnly ) {
      _urlPath = urlPath;
      _filePath = filePath;
      _readOnly = readOnly;
   }

   public String getUrlPath() {
      return _urlPath;
   }

   public String getFilePath() {
      return _filePath;
   }

   public boolean isReadOnly() {
      return _readOnly;
   }

}
