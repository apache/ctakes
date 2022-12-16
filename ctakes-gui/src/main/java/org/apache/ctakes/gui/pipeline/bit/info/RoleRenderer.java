package org.apache.ctakes.gui.pipeline.bit.info;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.component.CellRendererLabel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/24/2017
 */
final public class RoleRenderer implements ListCellRenderer<Object> {

   static private final Logger LOGGER = Logger.getLogger( "RoleRenderer" );

   static private final String READER_ICON_FILE = "GreenArrowIn.png";
   static private final String ANNOTATOR_ICON_FILE = "BlueGear.png";
   static private final String WRITER_ICON_FILE = "BlueArrowOut.png";
   static private final String SPECIAL_ICON_FILE = "Utilities.png";

   private final JLabel _reader = new CellRendererLabel();
   private final JLabel _annotator = new CellRendererLabel();
   private final JLabel _writer = new CellRendererLabel();
   private final JLabel _special = new CellRendererLabel();

   public RoleRenderer() {
      decorate( _reader );
      decorate( _annotator );
      decorate( _writer );
      decorate( _special );
      SwingUtilities.invokeLater( new RoleIconLoader() );
   }

   static private void decorate( final JLabel renderer ) {
      final Color background = UIManager.getColor( "List.background" );
      renderer.setBackground( background );
      renderer.setBorder( new EmptyBorder( 0, 2, 0, 2 ) );
   }

   @Override
   public Component getListCellRendererComponent(
         final JList<?> list,
         final Object value,
         final int index,
         final boolean isSelected,
         final boolean cellHasFocus ) {
      if ( !PipeBitInfo.class.isInstance( value ) ) {
         LOGGER.error( value.getClass().getName() + " is not a PipeBitInfo" );
         final JLabel renderer = new JLabel( "Invalid" );
         renderer.setBackground( Color.DARK_GRAY );
         renderer.setToolTipText( "Invalid Information" );
         return renderer;
      }
      final PipeBitInfo info = (PipeBitInfo)value;
      switch ( info.role() ) {
         case READER:
            return _reader;
         case ANNOTATOR:
            return _annotator;
         case WRITER:
            return _writer;
         case SPECIAL:
            return _special;
      }
      return new JLabel();
   }

   /**
    * Simple Callable that loads and resizes an icon
    */
   private final class RoleIconLoader implements Runnable {
      static private final int ICON_SIZE = 16;
      static private final String ICON_DIR = "org/apache/ctakes/gui/pipeline/icon/";

      /**
       * {@inheritDoc}
       */
      @Override
      public void run() {
         final Icon reader = IconLoader.loadIcon( ICON_DIR + READER_ICON_FILE, ICON_SIZE );
         _reader.setIcon( reader );
         final Icon annotator = IconLoader.loadIcon( ICON_DIR + ANNOTATOR_ICON_FILE, ICON_SIZE );
         _annotator.setIcon( annotator );
         final Icon writer = IconLoader.loadIcon( ICON_DIR + WRITER_ICON_FILE, ICON_SIZE );
         _writer.setIcon( writer );
         final Icon special = IconLoader.loadIcon( ICON_DIR + SPECIAL_ICON_FILE, ICON_SIZE );
         _special.setIcon( special );
      }
   }


}
