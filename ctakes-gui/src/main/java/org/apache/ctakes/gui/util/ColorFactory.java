package org.apache.ctakes.gui.util;


import org.apache.log4j.Logger;

import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/24/2017
 */
final public class ColorFactory {

   static private final Logger LOGGER = Logger.getLogger( "ColorFactory" );

   private ColorFactory() {
   }


   static public Color getColor( final String seed ) {
      return getColor( seed.hashCode() );
   }

   static public Color getColor( final int seed ) {
      return new Color( seed );
   }

   static public Color addTransparency( final Color color, final int transparency ) {
      return new Color( color.getRed(), color.getGreen(), color.getBlue(), transparency % 256 );
   }

}
