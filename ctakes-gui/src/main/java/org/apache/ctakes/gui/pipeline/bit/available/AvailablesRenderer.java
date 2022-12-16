package org.apache.ctakes.gui.pipeline.bit.available;

import org.apache.ctakes.gui.component.CellRendererPanel;
import org.apache.ctakes.gui.pipeline.bit.PipeBitPainter;
import org.apache.ctakes.gui.util.IconLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2017
 */
final public class AvailablesRenderer implements ListCellRenderer<Object> {

   static private final Logger LOGGER = Logger.getLogger( "AvailablesRenderer" );

   static private final Border SELECTED_BORDER = new LineBorder( Color.DARK_GRAY, 1, true );
   static private final Border UNSELECTED_BORDER = new EmptyBorder( 0, 0, 0, 5 );

   private final ListCellRenderer<Object> _delegate = new DefaultListCellRenderer();
   private JLabel _arrowLabel;

   private final JPanel _focusRenderer;


   public AvailablesRenderer() {
      _focusRenderer = new CellRendererPanel( new BorderLayout() );
      _focusRenderer.setBorder( new EmptyBorder( 0, 0, 0, 5 ) );
      _arrowLabel = new JLabel();
      _focusRenderer.add( _arrowLabel, BorderLayout.EAST );
      SwingUtilities.invokeLater( new RightArrowIconLoader() );
   }

   @Override
   public Component getListCellRendererComponent(
         final JList<?> list,
         final Object value,
         final int index,
         final boolean isSelected,
         final boolean cellHasFocus ) {
      final Component renderer = _delegate.getListCellRendererComponent( list, value, index, false, false );

      PipeBitPainter.getInstance().paintObject( renderer, value, false );

      final Point p = list.getMousePosition();
      if ( p != null ) {
         final int hoverIndex = list.locationToIndex( p );
         if ( hoverIndex == index ) {
            _focusRenderer.add( renderer, BorderLayout.CENTER );
            if ( isSelected ) {
               _focusRenderer.setBorder( SELECTED_BORDER );
            } else {
               _focusRenderer.setBorder( UNSELECTED_BORDER );
            }
            return _focusRenderer;
         }
      }
      if ( isSelected && renderer instanceof JComponent ) {
         ((JComponent)renderer).setBorder( SELECTED_BORDER );
      }
      return renderer;
   }


   /**
    * Simple Runnable that loads an icon
    */
   private final class RightArrowIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String file = "BlueRightArrow.png";
         final Icon icon = IconLoader.loadIcon( dir + file );
         _arrowLabel.setIcon( icon );
      }
   }

}
