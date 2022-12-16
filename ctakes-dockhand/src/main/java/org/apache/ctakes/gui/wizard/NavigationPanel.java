package org.apache.ctakes.gui.wizard;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2019
 */
final class NavigationPanel {


   public JComponent createPanel( final WizardController wizardController ) {
      final TravelAction previous
            = new TravelAction( wizardController.toPrevious(),
            wizardController.hasPrevious(),
            "Previous", "Go to previous step." );
      wizardController.addListSelectionListener( previous );

      final TravelAction next
            = new TravelAction( wizardController.toNext(),
            wizardController.hasNext(),
            "Next", "Go to next step." );
      wizardController.addListSelectionListener( next );

      final FinishAction finish
            = new FinishAction( wizardController,
            "Finish", "All settings are correct.  Perform finishing move." );
      wizardController.addListSelectionListener( finish );

      final JPanel panel = new JPanel( new GridLayout( 1, 5, 5, 5 ) );
      panel.setBackground( Color.WHITE );
      panel.add( new JLabel() );
      panel.add( new JLabel() );
      panel.add( new JButton( previous ) );
      panel.add( new JButton( next ) );
      panel.add( new JButton( finish ) );
      return panel;
   }

   static private class TravelAction extends AbstractAction implements ListSelectionListener {
      private final Runnable _traveller;
      private final BooleanSupplier _travellable;

      private TravelAction( final Runnable traveller,
                            final BooleanSupplier travellable,
                            final String name, final String tip ) {
         super( name );
         putValue( Action.SHORT_DESCRIPTION, tip );
         _traveller = traveller;
         _travellable = travellable;
      }

      public void actionPerformed( final ActionEvent event ) {
         _traveller.run();
      }

      public void valueChanged( final ListSelectionEvent event ) {
         setEnabled( _travellable.getAsBoolean() );
      }
   }

   static private class FinishAction extends AbstractAction implements ListSelectionListener {
      private final WizardController _wizardController;

      private FinishAction( final WizardController wizardController,
                            final String name, final String tip ) {
         super( name );
         putValue( Action.SHORT_DESCRIPTION, tip );
         _wizardController = wizardController;
         setEnabled( _wizardController.getBuildable().getAsBoolean() );
      }

      public void actionPerformed( final ActionEvent event ) {
         final ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.execute( _wizardController.getBuildProcess() );
      }

      public void valueChanged( final ListSelectionEvent event ) {
         setEnabled( _wizardController.getBuildable().getAsBoolean() );
      }
   }


}
