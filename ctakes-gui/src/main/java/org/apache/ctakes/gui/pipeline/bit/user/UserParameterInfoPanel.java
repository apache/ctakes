package org.apache.ctakes.gui.pipeline.bit.user;

import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
public class UserParameterInfoPanel extends ParameterInfoPanel {

   static private final Logger LOGGER = LogManager.getLogger( "UserParameterInfoPanel" );

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
