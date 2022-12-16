package org.apache.ctakes.gui.pipeline.bit.info;


import org.apache.ctakes.gui.component.CellRendererLabel;
import org.apache.ctakes.gui.util.ColorFactory;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2017
 */
final public class TypeProductRenderer implements ListCellRenderer<Object> {

   static private final Logger LOGGER = Logger.getLogger( "TypeProductRenderer" );

   private final JLabel _delegate = new CellRendererLabel();

   public TypeProductRenderer() {
      _delegate.setBackground( UIManager.getColor( "List.background" ) );
   }

   @Override
   public Component getListCellRendererComponent(
         final JList<?> list,
         final Object value,
         final int index,
         final boolean isSelected,
         final boolean cellHasFocus ) {
      if ( !TypeProduct.class.isInstance( value ) ) {
         LOGGER.error( value.getClass().getName() + " is not a TypeProduct" );
         _delegate.setIcon( null );
         _delegate.setText( "Invalid" );
         _delegate.setToolTipText( "Invalid Information" );
         return _delegate;
      }

      final TypeProduct typeProduct = (TypeProduct)value;
      final Icon icon = ProductIconFactory.getInstance().getIcon( typeProduct );
      _delegate.setIcon( icon );
      final String name = typeProduct.name();
      final Color color = ColorFactory.getColor( name );
      _delegate.setForeground( color );
      final String prettyName = name.charAt( 0 ) + name.substring( 1, name.length() );
      _delegate.setText( prettyName );
      return _delegate;
   }

}
