package org.apache.ctakes.gui.pipeline.bit.user;

import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
public class UserParameterInfoPanel extends ParameterInfoPanel {

   static private final Logger LOGGER = Logger.getLogger( "UserParameterInfoPanel" );

   @Override
   protected String getValueLabelPrefix() {
      return "User";
   }

   @Override
   protected JComponent createValuesEditor() {
      return new JTextField();
   }

   @Override
   protected void setParameterValues( final String values ) {
      ((JTextComponent)_values).setText( values );
   }

}
