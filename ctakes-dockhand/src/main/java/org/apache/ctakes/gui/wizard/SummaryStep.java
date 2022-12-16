package org.apache.ctakes.gui.wizard;


import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/14/2019
 */
final public class SummaryStep extends AbstractWizardStep implements WizardStep, ListSelectionListener {


   private JPanel _panel;

   public SummaryStep( final WizardController wizardController ) {
      super( "Summary",
            "Summary of information for your installation." );
      wizardController.addListSelectionListener( this );
   }


   private void update( final WizardController wizardController ) {
      _panel.removeAll();
      final Collection<WizardStep> wizardSteps = wizardController.getWizardSteps();

      for ( WizardStep wizardStep : wizardSteps ) {
         if ( wizardStep.equals( this ) ) {
            continue;
         }
         _panel.add( createStepComponent( wizardStep ) );
      }
   }


   private JComponent createStepComponent( final WizardStep wizardStep ) {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
      panel.setBackground( Color.WHITE );

      final JLabel nameLabel = new JLabel( wizardStep.getName() );
//      final String description = wizardStep.getDescription();
//      nameLabel.setToolTipText( description );
      panel.add( nameLabel, BorderLayout.NORTH );

      final JLabel spacer = new JLabel( "     " );
      spacer.setMinimumSize( new Dimension( 100, 10 ) );
      spacer.setSize( new Dimension( 100, 10 ) );
      panel.add( spacer, BorderLayout.WEST );

      final JLabel textArea = new JLabel();
      textArea.setBorder( new CompoundBorder( LineBorder.createGrayLineBorder(), new EmptyBorder( 5, 5, 5, 5 ) ) );
      textArea.setVerticalAlignment( SwingConstants.TOP );
      textArea.setText( wizardStep.getSummaryInfo() );
//      textArea.setToolTipText( description );
      panel.add( textArea, BorderLayout.CENTER );

      return panel;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected JComponent createPanel() {
      _panel = new JPanel();
      _panel.setLayout( new BoxLayout( _panel, BoxLayout.Y_AXIS ) );
      return wrapInScrollPane( _panel );
   }


   public void valueChanged( final ListSelectionEvent event ) {
      final Object source = event.getSource();
      if ( source != null && source instanceof WizardController ) {
         final WizardStep step = ((WizardController)source).getCurrentStep();
         if ( this.equals( step ) ) {
            update( (WizardController)source );
         }
      }
   }


}
