package org.apache.ctakes.gui.pipeline.bit.info;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.util.ColorFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2017
 */
public enum ProductIconFactory {
   INSTANCE;

   public static ProductIconFactory getInstance() {
      return INSTANCE;
   }


   private final Map<TypeProduct, Icon> _typeProductIcons = new EnumMap<>( TypeProduct.class );

   ProductIconFactory() {
//      SwingUtilities.invokeLater( new ProductIconMaker() );
      new ProductIconMaker().run();
   }

   public Icon getIcon( final TypeProduct typeProduct ) {
      return _typeProductIcons.get( typeProduct );
   }

   private final class ProductIconMaker implements Runnable {
      private final Stroke LINE_STROKE = new BasicStroke( 4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
      private final Font HINT_FONT = new Font( "Dialog", Font.BOLD, 12 );

      @Override
      public void run() {
         Arrays.stream( PipeBitInfo.TypeProduct.values() )
               .forEach( tp -> _typeProductIcons.put( tp, createIcon( tp ) ) );
      }

      private Icon createIcon( final PipeBitInfo.TypeProduct typeProduct ) {
         final Image image = new BufferedImage( 20, 20, BufferedImage.TYPE_3BYTE_BGR );
         final Graphics2D g2 = (Graphics2D)image.getGraphics();
         g2.setColor( UIManager.getColor( "List.background" ) );
         g2.fillRect( 0, 0, 20, 20 );

         g2.setColor( Color.LIGHT_GRAY );
         g2.drawOval( 2, 2, 17, 17 );

         final String name = typeProduct.name();
         final Color color = ColorFactory.getColor( name );
         g2.setColor( ColorFactory.addTransparency( color, 159 ) );
         g2.setFont( HINT_FONT );
         g2.drawString( typeProduct.name().charAt( 0 ) + "", 4, 15 );

         final GeneralPath linePath = new GeneralPath( GeneralPath.WIND_EVEN_ODD, 3 );
         final int[] xArray = { 11, 17, 11 };
         final int[] yArray = { 3, 9, 17 };
         linePath.moveTo( xArray[ 0 ], yArray[ 0 ] );
         for ( int i = 1; i < xArray.length; i++ ) {
            linePath.lineTo( xArray[ i ], yArray[ i ] );
         }
         g2.setStroke( LINE_STROKE );
         g2.draw( linePath );

         return new ImageIcon( image );
      }
   }

}
