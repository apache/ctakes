package org.apache.ctakes.dockhand.gui.wizard;

import org.apache.ctakes.dockhand.gui.feature.Feature;
import org.apache.ctakes.dockhand.gui.feature.FeatureComponent;
import org.apache.ctakes.dockhand.gui.feature.Option;
import org.apache.ctakes.gui.wizard.AbstractWizardStep;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2019
 */
final public class FeaturesStep extends AbstractWizardStep implements DhWizardStep {


   private final Map<Feature, FeatureComponent> _featureComponents = new EnumMap<>( Feature.class );


   public FeaturesStep() {
      super( "Pipeline Features",
            "Select the pipeline Features." );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected JComponent createPanel() {
      final JComponent panel = new JPanel();
      panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
      final FeatureCheckController featureCheckController = new FeatureCheckController();
      for ( Feature feature : Feature.values() ) {
         final FeatureComponent featureComponent = new FeatureComponent( featureCheckController, feature );
         _featureComponents.put( feature, featureComponent );
         panel.add( featureComponent );
      }

      // Sections are always required
      _featureComponents.get( Feature.SECTIONS ).setSelected( true );

      return wrapInScrollPane( panel );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getPiperCommands() {
      if ( _featureComponents.isEmpty() ) {
         return Collections.emptyList();
      }

      final Collection<String> unfilteredCommands =
            Arrays.stream( Feature.values() )
                  .map( _featureComponents::get )
                  .filter( FeatureComponent::isSelected )
                  .map( FeatureComponent::getSelectedOption )
                  .map( Option::getPiperLines )
                  .flatMap( Collection::stream )
                  .collect( Collectors.toList() );

      final Collection<String> piperCommands = new ArrayList<>();
      for ( String command : unfilteredCommands ) {
         if ( !command.isEmpty() ) {
            if ( piperCommands.contains( command )
                 || !Option.PiperLineRequirements.hasRequirements( command, piperCommands ) ) {
               continue;
            }
         }
         piperCommands.add( command );
      }
      return piperCommands;
   }

   public String getSummaryInfo() {
      final Collection<String> summaryLines = new ArrayList<>();
      for ( FeatureComponent featureComponent : _featureComponents.values() ) {
         if ( !featureComponent.isSelected() ) {
            continue;
         }
         summaryLines.add( featureComponent.getSelectedOption().getDescription() );
      }
      return "<HTML>" + String.join( "<BR>", summaryLines ) + "</HTML>";
   }

   public Collection<Feature> getSelectedFeatures() {
      return _featureComponents.entrySet().stream()
                               .filter( e -> e.getValue().isSelected() )
                               .map( Map.Entry::getKey )
                               .collect( Collectors.toList() );
   }


   static public final class FeatureCheckController implements ActionListener {
      private boolean _activating;
      private final Map<Feature, FeatureComponent> _featureComponentMap = new EnumMap<>( Feature.class );
      private final Collection<Feature> _requiredFeatures = new HashSet<>();

      public void addFeature( final Feature feature, final FeatureComponent featureComponent ) {
         _featureComponentMap.put( feature, featureComponent );
      }

      private void forceActivate() {
         _requiredFeatures.clear();
         _featureComponentMap.entrySet().stream()
                             .filter( e -> e.getValue().isSelected() )
                             .map( Map.Entry::getKey )
                             .map( Feature::getRequirements )
                             .forEach( _requiredFeatures::addAll );
         _featureComponentMap.forEach( this::forceActivate );
      }

      private void forceActivate( final Feature feature, final FeatureComponent featureComponent ) {
         if ( _requiredFeatures.contains( feature ) ) {
            featureComponent.setSelected( true );
            featureComponent.setEnabled( false );
         } else {
            featureComponent.setEnabled( true );
         }
      }

      public void actionPerformed( final ActionEvent event ) {
         if ( _activating ) {
            return;
         }
         _activating = true;
         forceActivate();
         _activating = false;
      }
   }


}
