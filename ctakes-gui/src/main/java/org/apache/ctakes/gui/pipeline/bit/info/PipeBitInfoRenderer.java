package org.apache.ctakes.gui.pipeline.bit.info;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipeBitInfoUtil;
import org.apache.ctakes.gui.component.CellRendererLabel;
import org.apache.ctakes.gui.component.CellRendererPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/24/2017
 */
final public class PipeBitInfoRenderer implements ListCellRenderer<Object> {

   static private final Logger LOGGER = Logger.getLogger( "PipeBitInfoRenderer" );

   static private final Color READER_COLOR = Color.GREEN.darker().darker();
   static private final Color ANNOTATOR_COLOR = Color.CYAN.darker().darker();
   static private final Color WRITER_COLOR = Color.BLUE;
   static private final Color SPECIAL_COLOR = Color.MAGENTA.darker();

   static private final LayoutManager EMPTY_LAYOUT = new GridLayout( 1, 1 );

   private final JPanel _renderer = new CellRendererPanel( new BorderLayout( 5, 0 ) );
   private final JPanel _dependencies = new CellRendererPanel( EMPTY_LAYOUT );
   private final JLabel _textLabel = new CellRendererLabel();
   private final JPanel _products = new CellRendererPanel( EMPTY_LAYOUT );


   public PipeBitInfoRenderer() {
      _dependencies.setBackground( null );
      _textLabel.setBackground( null );
      _products.setBackground( null );
      _renderer.add( _dependencies, BorderLayout.WEST );
      _renderer.add( _textLabel, BorderLayout.CENTER );
      _renderer.add( _products, BorderLayout.EAST );
   }

   @Override
   public Component getListCellRendererComponent(
         final JList<?> list,
         final Object value,
         final int index,
         final boolean isSelected,
         final boolean cellHasFocus ) {
      _dependencies.removeAll();
      _products.removeAll();

      if ( isSelected ) {
         _renderer.setBackground( list.getSelectionBackground() );
         _textLabel.setForeground( list.getSelectionForeground() );
      } else {
         _renderer.setBackground( list.getBackground() );
      }
      if ( !PipeBitInfo.class.isInstance( value ) ) {
         LOGGER.error( value.getClass().getName() + " is not a PipeBitInfo" );
         _dependencies.setLayout( EMPTY_LAYOUT );
         _products.setLayout( EMPTY_LAYOUT );
         _textLabel.setText( "Invalid" );
         _renderer.setBackground( Color.DARK_GRAY );
         _renderer.setToolTipText( "Invalid Information" );
         return _renderer;
      }
      final PipeBitInfo info = (PipeBitInfo)value;

      if ( PipeBitInfoUtil.isUnknown( info ) ) {
         final Color bg = isSelected ? Color.GRAY : Color.LIGHT_GRAY;
         _renderer.setBackground( bg );
      }

      if ( !isSelected ) {
         final Color color = getColor( info.role() );
         _textLabel.setForeground( color );
      }
      _textLabel.setText( info.name() );

      createTypeIcons( _dependencies, info.dependencies() );
      createTypeIcons( _products, info.products() );

      return _renderer;
   }

   static private Color getColor( final PipeBitInfo.Role role ) {
      switch ( role ) {
         case READER:
            return READER_COLOR;
         case ANNOTATOR:
            return ANNOTATOR_COLOR;
         case WRITER:
            return WRITER_COLOR;
         case SPECIAL:
            return SPECIAL_COLOR;
      }
      return Color.GRAY;
   }

   static private void createTypeIcons( final JPanel panel, final TypeProduct... types ) {
      panel.setLayout( new GridLayout( 1, types.length ) );
      Arrays.stream( types )
            .map( ProductIconFactory.getInstance()::getIcon )
            .filter( Objects::nonNull )
            .map( CellRendererLabel::new )
            .forEach( panel::add );
   }


}
