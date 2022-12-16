package org.apache.ctakes.gui.wizard;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2019
 */
final public class WizardPanel {


   public JComponent createPanel( final WizardController wizardController ) {
      final JPanel mainPanel = new JPanel( new BorderLayout( 10, 10 ) );
      mainPanel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
      mainPanel.setBackground( Color.WHITE );

      final TocPanel tocPanel = new TocPanel();
      mainPanel.add( tocPanel.createPanel( wizardController ), BorderLayout.WEST );

      final ContentsPanel contentsPanel = new ContentsPanel( wizardController );
      mainPanel.add( contentsPanel.createPanel(), BorderLayout.CENTER );

      final NavigationPanel navigationPanel = new NavigationPanel();
      mainPanel.add( navigationPanel.createPanel( wizardController ), BorderLayout.SOUTH );

      wizardController.setCurrentIndex( 0 );
      return mainPanel;
   }


}
