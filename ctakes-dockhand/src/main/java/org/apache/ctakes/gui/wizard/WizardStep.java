package org.apache.ctakes.gui.wizard;

import javax.swing.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2019
 */
public interface WizardStep {

   String getName();

   String getDescription();

   JComponent getPanel();

   default boolean finished() {
      return true;
   }

   default String getSummaryInfo() {
      return "";
   }

}
