package org.apache.ctakes.gui.dictionary.umls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/11/2015
 */
final public class TuiCellRenderer implements TableCellRenderer {

   static private final Logger LOGGER = LoggerFactory.getLogger( "TuiCellRenderer" );

   private final TuiTableModel _tuiModel;
   private final TableCellRenderer _delegate;

   public TuiCellRenderer( final TuiTableModel tuiModel, final TableCellRenderer delegate ) {
      _tuiModel = tuiModel;
      _delegate = delegate;
   }


   @Override
   public Component getTableCellRendererComponent( final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column ) {
      final Component renderer
            = _delegate.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
//      final Tui tui = _tuiModel.

      return renderer;
   }


}
