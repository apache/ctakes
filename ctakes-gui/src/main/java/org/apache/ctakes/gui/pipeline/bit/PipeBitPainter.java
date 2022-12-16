package org.apache.ctakes.gui.pipeline.bit;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/13/2017
 */
public enum PipeBitPainter {
   INSTANCE;

   static public PipeBitPainter getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "PipeBitPainter" );

   static private final Color READER_COLOR = Color.GREEN.darker().darker();
   static private final Color ANNOTATOR_COLOR = Color.CYAN.darker().darker();
   static private final Color WRITER_COLOR = Color.BLUE;
   static private final Color SPECIAL_COLOR = Color.MAGENTA.darker();
   static private final String READER_ICON_FILE = "GreenArrowIn.png";
   static private final String ANNOTATOR_ICON_FILE = "BlueGear.png";
   static private final String WRITER_ICON_FILE = "BlueArrowOut.png";
   static private final String SPECIAL_ICON_FILE = "Utilities.png";

   static private final Border EMPTY_BORDER = new EmptyBorder( 0, 5, 0, 0 );


   private Icon READER_ICON = null;
   private Icon ANNOTATOR_ICON = null;
   private Icon WRITER_ICON = null;
   private Icon SPECIAL_ICON = null;
   private boolean _iconLoadAttempted;


   synchronized public void loadIcons() {
      if ( _iconLoadAttempted ) {
         return;
      }
      _iconLoadAttempted = true;
      SwingUtilities.invokeLater( new PipeBitIconLoader() );
   }

   public void paintObject( final Component renderer, final Object value, final boolean isSelected ) {
      if ( PipeBitInfo.class.isInstance( value ) ) {
         paintPipeBitInfo( renderer, (PipeBitInfo)value, isSelected );
      } else {
         LOGGER.error( value.getClass().getName() + " is not a PipeBitInfo" );
         renderer.setBackground( Color.DARK_GRAY );
         setText( renderer, "Invalid" );
         setToolTipText( renderer, "Invalid Information" );
      }
   }

   public void paintPipeBitInfo( final Component renderer, final PipeBitInfo info, final boolean isSelected ) {
      if ( !isSelected ) {
         switch ( info.role() ) {
            case READER:
               renderer.setForeground( READER_COLOR );
               break;
            case ANNOTATOR:
               renderer.setForeground( ANNOTATOR_COLOR );
               break;
            case WRITER:
               renderer.setForeground( WRITER_COLOR );
               break;
            case SPECIAL:
               renderer.setForeground( SPECIAL_COLOR );
               break;
         }
      }
      setIcon( renderer, info );
      setText( renderer, info.name() );
      setToolTipText( renderer, info.description() );
   }

   private void setIcon( final Component renderer, final PipeBitInfo info ) {
      if ( !JLabel.class.isInstance( renderer ) ) {
         return;
      }
      ((JLabel)renderer).setBorder( EMPTY_BORDER );
      switch ( info.role() ) {
         case READER:
            ((JLabel)renderer).setIcon( READER_ICON );
            break;
         case ANNOTATOR:
            ((JLabel)renderer).setIcon( ANNOTATOR_ICON );
            break;
         case WRITER:
            ((JLabel)renderer).setIcon( WRITER_ICON );
            break;
         case SPECIAL:
            ((JLabel)renderer).setIcon( SPECIAL_ICON );
            break;
      }
   }

   static private void setText( final Component renderer, final String text ) {
      if ( !JLabel.class.isInstance( renderer ) ) {
         LOGGER.error( "Renderer " + renderer.getClass().getName() + " is not a JLabel" );
         return;
      }
      ((JLabel)renderer).setText( text );
   }

   static private void setToolTipText( final Component renderer, final String text ) {
      if ( !JComponent.class.isInstance( renderer ) ) {
         LOGGER.error( "Renderer " + renderer.getClass().getName() + " is not a JComponent" );
         return;
      }
      ((JComponent)renderer).setToolTipText( text );
   }


   /**
    * Simple Callable that loads and resizes an icon
    */
   private final class PipeBitIconLoader implements Runnable {
      static private final int ICON_SIZE = 16;
      static private final String ICON_DIR = "org/apache/ctakes/gui/pipeline/icon/";

      /**
       * {@inheritDoc}
       */
      @Override
      public void run() {
         READER_ICON = IconLoader.loadIcon( ICON_DIR + READER_ICON_FILE, ICON_SIZE );
         ANNOTATOR_ICON = IconLoader.loadIcon( ICON_DIR + ANNOTATOR_ICON_FILE, ICON_SIZE );
         WRITER_ICON = IconLoader.loadIcon( ICON_DIR + WRITER_ICON_FILE, ICON_SIZE );
         SPECIAL_ICON = IconLoader.loadIcon( ICON_DIR + SPECIAL_ICON_FILE, ICON_SIZE );
      }
   }


}
