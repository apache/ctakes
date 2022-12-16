package org.apache.ctakes.dockhand.gui.wizard;

import org.apache.ctakes.dockhand.gui.feature.GoalPom;
import org.apache.ctakes.gui.wizard.WizardController;
import org.apache.ctakes.gui.wizard.WizardStep;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/14/2019
 */
final public class DhWizardController extends WizardController {


   public GoalPom getGoalPom() {
      return getWizardSteps().stream()
                             .filter( s -> s instanceof FeaturesStep )
                             .findAny()
                             .map( s -> (FeaturesStep)s )
                             .map( FeaturesStep::getSelectedFeatures )
                             .map( GoalPom::getAppropriatePom )
                             .get();
   }


   private String createHeader() {
      final DateFormat dateFormatter = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );
      final String date = dateFormatter.format( new Date() );
      String hostname;
      try {
         hostname = InetAddress.getLocalHost().getHostName();
      } catch ( UnknownHostException uhE ) {
         hostname = "UnknownHost";
      }
      final String userName = System.getProperty( "user.name" );
      return "// Piper File created " + date + " by " + userName + " on " + hostname;
   }


   public Collection<String> getPiperCommands() {
      final Collection<String> piperCommands = new ArrayList<>();
      piperCommands.add( createHeader() );
      for ( WizardStep wizardStep : getWizardSteps() ) {
         if ( wizardStep instanceof DhWizardStep ) {
            piperCommands.add( "" );
            piperCommands.addAll( ((DhWizardStep)wizardStep).getPiperCommands() );
         }
      }
      return piperCommands;
   }


}
