package org.apache.ctakes.dockhand.gui.output;


import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/8/2019
 */
final public class OutputComponent extends JPanel {

   private final Output _output;
   private final JCheckBox _checkBox;


   public OutputComponent( final Output output ) {
      super( new BorderLayout() );
      setBackground( Color.WHITE );
      _output = output;
      _checkBox = new JCheckBox( output.getName() );
      _checkBox.setBackground( Color.WHITE );
      _checkBox.setToolTipText( output.getDescription() );
      add( _checkBox, BorderLayout.NORTH );
   }

   public Output getOutput() {
      return _output;
   }

   public void setSelected( final boolean selected ) {
      _checkBox.setSelected( selected );
   }

   public boolean isSelected() {
      return _checkBox.isSelected();
   }

   public void setEnabled( final boolean enable ) {
      _checkBox.setEnabled( enable );
   }


}
