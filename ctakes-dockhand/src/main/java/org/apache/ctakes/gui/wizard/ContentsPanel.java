package org.apache.ctakes.gui.wizard;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2019
 */
final class ContentsPanel {


   private final JLabel _mainLabel;
   private final JScrollPane _scrollPane;
   private final WizardController _wizardController;

   ContentsPanel( final WizardController wizardController ) {
      _mainLabel = new JLabel();
      final Font font = _mainLabel.getFont();
      final int fontSize = font.getSize();
      _mainLabel.setFont( font.deriveFont( Font.BOLD ).deriveFont( fontSize * 1.5f ) );
      _scrollPane = new JScrollPane();
      _scrollPane.setBorder( null );
      _wizardController = wizardController;
      wizardController.addListSelectionListener( new WizardControllerListener() );
   }

   public JComponent createPanel() {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBackground( Color.WHITE );
      panel.add( _mainLabel, BorderLayout.NORTH );
      panel.add( _scrollPane, BorderLayout.CENTER );
      return panel;
   }


   private class WizardControllerListener implements ListSelectionListener {
      public void valueChanged( final ListSelectionEvent event ) {
         _mainLabel.setText( _wizardController.getCurrentStep().getDescription() );
         _scrollPane.setViewportView( _wizardController.getCurrentStep().getPanel() );
      }
   }


}
