package org.apache.ctakes.dockhand.gui.wizard;

import org.apache.ctakes.gui.wizard.WizardStep;

import java.util.Collection;
import java.util.Collections;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/11/2019
 */
public interface DhWizardStep extends WizardStep {

   default Collection<String> getPiperCommands() {
      return Collections.emptyList();
   }


}
