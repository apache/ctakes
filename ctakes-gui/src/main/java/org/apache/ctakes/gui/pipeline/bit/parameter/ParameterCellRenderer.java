package org.apache.ctakes.gui.pipeline.bit.parameter;

import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
final public class ParameterCellRenderer implements TableCellRenderer {

   static private final Logger LOGGER = Logger.getLogger( "ParameterCellRenderer" );

   private final TableCellRenderer _delegate = new DefaultTableCellRenderer();

   @Override
   public Component getTableCellRendererComponent( final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column ) {
      final Component renderer = _delegate
            .getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
      if ( renderer instanceof JLabel && value instanceof ConfigurationParameter ) {
         ((JLabel)renderer).setText( ((ConfigurationParameter)value).name() );
      }
      return renderer;
   }

}
