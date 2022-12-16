package org.apache.ctakes.gui.progress;

import org.apache.ctakes.gui.wizard.util.DialogUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/10/2019
 */
final public class NoteMarkupProgressUI extends BasicProgressBarUI {

//   private static final int ARC_EXTENT = 25;

   static private final int FRAME_COUNT = 7;

   private Image[] _images = new Image[ FRAME_COUNT ];

   private boolean _animating = false;
   private int _index = 0;

   public NoteMarkupProgressUI() {
      SwingUtilities.invokeLater( new ImageLoader() );
   }

   @Override
   protected void startAnimationTimer() {
      _animating = true;
      _index = 0;
      super.startAnimationTimer();
   }

   @Override
   protected void stopAnimationTimer() {
      _animating = false;
      _index = 0;
      super.stopAnimationTimer();
   }

   @Override
   protected void installDefaults() {
      super.installDefaults();
      progressBar.setBorder( null );
   }

   @Override
   protected void paintIndeterminate( final Graphics g, final JComponent component ) {
      final Graphics2D g2d = (Graphics2D)g;
      final Image image = _images[ _index ];
      if ( image == null ) {
         return;
      }
      g2d.drawImage( image, 0, 0, null );
      if ( _animating ) {
         _index++;
         if ( _index == FRAME_COUNT ) {
            _index = 0;
         }
      }
   }

   @Override
   protected Rectangle getBox( final Rectangle rectangle ) {
      return new Rectangle( 0, 0, 200, 200 );
//      if ( rectangle != null ) {
//         rectangle.setBounds( progressBar.getBounds() );
//         return rectangle;
//      }
//      return progressBar.getBounds();
   }

   @Override
   public Dimension getPreferredSize( final JComponent component ) {
      return new Dimension( 200, 200 );
   }

//
//   public static BufferedImage rotate( final BufferedImage image, final float angle ) {
//      float radianAngle = (float) Math.toRadians(angle) ;
//
//      float sin = (float) Math.abs(Math.sin(radianAngle));
//      float cos = (float) Math.abs(Math.cos(radianAngle));
//
//      int w = image.getWidth() ;
//      int h = image.getHeight();
//
//      int neww = Math.round( w * cos + h * sin );
//      int newh = Math.round( h * cos + w * sin );
//
//      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//      GraphicsDevice gd = ge.getDefaultScreenDevice();
//      GraphicsConfiguration gc = gd.getDefaultConfiguration();
//
//      BufferedImage result = gc.createCompatibleImage(neww, newh, Transparency.TRANSLUCENT);
//      Graphics2D g = result.createGraphics();
//
//      //-----------------------MODIFIED--------------------------------------
//      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON) ;
//      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC) ;
//      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY) ;
//
//      AffineTransform at = AffineTransform.getTranslateInstance( (neww - w) / 2, (newh - h) / 2);
//      at.rotate( radianAngle, w / 2f, h / 2f );
//      //---------------------------------------------------------------------
//
//      g.drawRenderedImage(image, at);
//      g.dispose();
//
//      return result;
//   }
//
//   private void moreRotate( final BufferedImage image ) {
//      final double rads = Math.toRadians(90);
//      final double sin = Math.abs(Math.sin(rads));
//      final double cos = Math.abs(Math.cos(rads));
//      final int w = (int) Math.floor(image.getWidth() * cos + image.getHeight() * sin);
//      final int h = (int) Math.floor(image.getHeight() * cos + image.getWidth() * sin);
//       BufferedImage rotatedImage = new BufferedImage(w, h, image.getType());
//      final AffineTransform at = new AffineTransform();
//      at.translate(w / 2, h / 2);
//      at.rotate(rads,0, 0);
//      at.translate(-image.getWidth() / 2, -image.getHeight() / 2);
//      final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
//      rotatedImage = rotateOp.filter(image, null );
//   }
//
//
//   public BufferedImage RotateImage(String imagePath,int degrees) throws IOException{
//      File file = new File(imagePath);
//      Image image = ImageIO.read(file);
//      BufferedImage img= bufferImage(image, BufferedImage.TYPE_INT_RGB);
//
//      AffineTransform tx = new AffineTransform();
//      double radians = (Math.PI / 180) * degrees;
//
//      double width = img.getWidth()/2;
//      double height = img.getHeight()/2;
//
//      if(degrees != 180){
//         tx.translate(height,width);
//         tx.rotate(radians);
//         tx.translate(-width,-height);
//      }else{
//         tx.rotate(radians,width, height);
//      }
//
//      AffineTransformOp op = new AffineTransformOp( tx, AffineTransformOp.TYPE_BILINEAR );
//      img = op.filter(img, null);
//      return img;
//   }

//   private void createCaduceus() {
//      try {
//         final InputStream imageStream = getClass().getResourceAsStream( "/org/apache/ctakes/image/ctakes_logo.jpg" );
//         if ( imageStream != null ) {
////            panel.add( new JLabel( new ImageIcon( ImageIO.read( imageStream ), "Apache cTAKES" ) ), BorderLayout.NORTH );
////         } else {
////            LOGGER.warning( "No Stream" );
//         }
//      } catch ( IOException ioE ) {
//         DialogUtil.showError( ioE.getMessage() );
//      }
//   }


   /**
    * Simple Runnable that loads an icon
    */
   private final class ImageLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "/org/apache/ctakes/image/progress/";
         final String baseName = "TextProcess_200g_";
         for ( int i = 0; i < FRAME_COUNT; i++ ) {
            _images[ i ] = loadImage( dir + baseName + i + ".gif" );
         }
      }

      private Image loadImage( final String path ) {
         try {
            final InputStream imageStream = getClass().getResourceAsStream( path );
            if ( imageStream != null ) {
               return ImageIO.read( imageStream );
               //         } else {
               //            LOGGER.warning( "No Stream" );
            }
         } catch ( IOException ioE ) {
            DialogUtil.showError( ioE.getMessage() );
         }
         return null;
      }
   }


}
