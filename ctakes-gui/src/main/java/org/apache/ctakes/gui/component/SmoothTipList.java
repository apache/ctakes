package org.apache.ctakes.gui.component;

import javax.swing.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/13/2017
 */
public class SmoothTipList<T> extends JList<T> {

   public SmoothTipList( final ListModel<T> model ) {
      super( model );
   }

   @Override
   public JToolTip createToolTip() {
      final SmoothToolTip tip = new SmoothToolTip();
      tip.setComponent( this );
      return tip;
   }


}
