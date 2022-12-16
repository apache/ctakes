package org.apache.ctakes.dockhand.gui;


import org.apache.ctakes.dockhand.gui.wizard.*;
import org.apache.ctakes.gui.wizard.SummaryStep;
import org.apache.ctakes.gui.wizard.WizardPanel;

import javax.swing.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2019
 */
final public class MainPanel {


   private final DhWizardController _wizardController;


   public MainPanel( final DhWizardController wizardController ) {
      _wizardController = wizardController;
      _wizardController.addStep( new InstallTypeStep( wizardController ) );
      _wizardController.addStep( new DescriptionStep() );
      _wizardController.addStep( new FeaturesStep() );
      _wizardController.addStep( new OutputStep() );
      _wizardController.addStep( new SummaryStep( wizardController ) );
   }

   public JComponent createPanel() {
      return new WizardPanel().createPanel( _wizardController );
   }


}
