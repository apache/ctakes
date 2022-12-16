package org.apache.ctakes.gui.wizard;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/11/2019
 */
abstract public class AbstractWizardStep implements WizardStep {


   private final String _name;
   private final String _description;
   private JComponent _panel;

   protected AbstractWizardStep( final String name, final String description ) {
      _name = name;
      _description = description;
   }

   abstract protected JComponent createPanel();

   protected JScrollPane wrapInScrollPane( final JComponent component ) {
      component.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
      component.setBackground( Color.WHITE );
      final JScrollPane scrollPane = new JScrollPane( component );
      scrollPane.setBorder( null );
      return scrollPane;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getDescription() {
      return _description;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public JComponent getPanel() {
      if ( _panel == null ) {
         _panel = createPanel();
      }
      return _panel;
   }


   /**
    * @return the name of the step.
    */
   @Override
   public String toString() {
      return getName();
   }

}
