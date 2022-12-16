package org.apache.ctakes.gui.pipeline.bit.user;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
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
final public class UserBitRenderer implements ListCellRenderer<Object> {

   static private final Logger LOGGER = Logger.getLogger( "UsersRenderer" );

   static private final Border SELECTED_BORDER = new LineBorder( Color.DARK_GRAY, 1, true );
   static private final Border UNSELECTED_BORDER = new EmptyBorder( 0, 0, 0, 5 );

   private final ListCellRenderer<Object> _delegate = new DefaultListCellRenderer();
   private JLabel _upLabel;
   private JLabel _downLabel;
   private JLabel _removeLabel;

   private final JPanel _focusRenderer;


   public UserBitRenderer() {
      _focusRenderer = new CellRendererPanel( new BorderLayout() );
      _focusRenderer.setBorder( UNSELECTED_BORDER );
      final JPanel buttonPanel = new JPanel( new GridLayout( 1, 3 ) );
      buttonPanel.setBorder( new EmptyBorder( 0, 0, 0, 5 ) );
      buttonPanel.setBackground( null );
      _upLabel = new JLabel();
      _downLabel = new JLabel();
      _removeLabel = new JLabel();
      buttonPanel.add( _upLabel );
      buttonPanel.add( _downLabel );
      buttonPanel.add( _removeLabel );
      _focusRenderer.add( buttonPanel, BorderLayout.EAST );
      SwingUtilities.invokeLater( new ButtonIconLoader() );
   }

   static public boolean SUSPEND_BUTTONS = false;

   @Override
   public Component getListCellRendererComponent(
         final JList<?> list,
         final Object value,
         final int index,
         final boolean isSelected,
         final boolean cellHasFocus ) {
      final Component renderer = _delegate.getListCellRendererComponent( list, value, index, false, false );
      final UserBit userBit = (UserBit)value;
      PipeBitPainter.getInstance().paintObject( renderer, userBit.getPipeBitInfo(), false );
      if ( renderer instanceof JLabel ) {
         ((JLabel)renderer).setText( userBit.getBitName() );
      }
      if ( SUSPEND_BUTTONS || userBit.getPipeBitInfo().role() == PipeBitInfo.Role.READER ) {
         if ( isSelected && renderer instanceof JComponent ) {
            ((JComponent)renderer).setBorder( SELECTED_BORDER );
         }
         return renderer;
      }
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
   private final class ButtonIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String upFile = "BlueUp.png";
         final String downFile = "BlueDown.png";
         final String removeFile = "RedCircleNo.png";
         final Icon upIcon = IconLoader.loadIcon( dir + upFile, 20 );
         final Icon downIcon = IconLoader.loadIcon( dir + downFile, 20 );
         final Icon removeIcon = IconLoader.loadIcon( dir + removeFile, 20 );
         _upLabel.setIcon( upIcon );
         _downLabel.setIcon( downIcon );
         _removeLabel.setIcon( removeIcon );
      }
   }

}
