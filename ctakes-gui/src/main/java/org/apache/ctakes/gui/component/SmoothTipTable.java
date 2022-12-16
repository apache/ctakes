package org.apache.ctakes.gui.component;

import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/23/2016
 */
public class SmoothTipTable extends JTable {

   public SmoothTipTable( final TableModel model ) {
      super( model );
   }

   @Override
   public JToolTip createToolTip() {
      final SmoothToolTip tip = new SmoothToolTip();
      tip.setComponent( this );
      return tip;
   }

}
