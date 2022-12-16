package org.apache.ctakes.gui.util;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2017
 */
final public class IconLoader {

   static private final Logger LOGGER = Logger.getLogger( "IconLoader" );

   private IconLoader() {
   }

   /**
    * @param filePath path to the icon file
    * @param iconSize size of the square in pixels
    * @return an icon of the specified size
    */
   static public Icon loadIcon( final String filePath, final int iconSize ) {
      try {
         final InputStream inputStream = FileLocator.getAsStream( filePath );
         final Image image = ImageIO.read( inputStream );
         final BufferedImage scaleImage = new BufferedImage( iconSize, iconSize, BufferedImage.TYPE_INT_ARGB );
         final Graphics g = scaleImage.createGraphics();
         g.drawImage( image, 0, 0, iconSize, iconSize, null );
         return new ImageIcon( scaleImage );
      } catch ( IOException ioE ) {
         LOGGER.warn( ioE.getMessage() );
      }
      return null;
   }

   /**
    * @param filePath path to the icon file
    * @return the icon stored in the given file
    */
   static public Icon loadIcon( final String filePath ) {
      try {
         final InputStream inputStream = FileLocator.getAsStream( filePath );
         final Image image = ImageIO.read( inputStream );
         return new ImageIcon( image );
      } catch ( IOException ioE ) {
         LOGGER.warn( ioE.getMessage() );
      }
      return null;
   }

}
