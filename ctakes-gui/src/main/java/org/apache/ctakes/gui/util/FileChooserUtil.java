package org.apache.ctakes.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.nio.file.Paths;

/**
 * @author SPF , chip-nlp
 * @since {8/21/2024}
 */
final public class FileChooserUtil {
   static private final Logger LOGGER = LoggerFactory.getLogger( "FileChooserUtil" );

   private FileChooserUtil() {}

   static public void selectWorkingDir( final JFileChooser chooser ) {
      String cwd = Paths.get( "" ).toAbsolutePath().toFile().getPath();
      if ( cwd.isEmpty() ) {
         cwd = System.getProperty( "user.dir" );
      }
      selectDir( chooser, cwd );
   }

   static public void selectDir( final JFileChooser chooser, final String dir ) {
      if ( dir == null || dir.isEmpty() ) {
         LOGGER.debug( "Selected directory is null or empty." );
         return;
      }
      SwingUtilities.invokeLater( () ->
      {
         try {
            chooser.setCurrentDirectory( new File( dir ) );
         } catch ( IndexOutOfBoundsException oobE ) {
            LOGGER.error( "FileChooser could not change directory to {}", dir );
            LOGGER.error( oobE.getMessage() );
            LOGGER.warn( "Keeping current directory {}", chooser.getCurrentDirectory() );
         }
      });
   }


}
