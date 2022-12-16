package org.apache.ctakes.dockhand.gui.wizard;

import org.apache.ctakes.dockhand.gui.feature.Option;
import org.apache.ctakes.dockhand.gui.output.Output;
import org.apache.ctakes.dockhand.gui.output.OutputComponent;
import org.apache.ctakes.gui.wizard.AbstractWizardStep;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2019
 */
final public class OutputStep extends AbstractWizardStep implements DhWizardStep {


   private final Map<Output, OutputComponent> _outputComponents = new EnumMap<>( Output.class );


   public OutputStep() {
      super( "Pipeline Outputs",
            "Select the pipeline Outputs." );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected JComponent createPanel() {
      final JComponent panel = new JPanel();
      panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
      panel.add( Box.createVerticalStrut( 100 ) );
      for ( Output output : Output.values() ) {
         final OutputComponent outputComponent = new OutputComponent( output );
         _outputComponents.put( output, outputComponent );
         panel.add( outputComponent );
      }
      panel.add( Box.createVerticalStrut( 100 ) );
      return wrapInScrollPane( panel );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getPiperCommands() {
      if ( _outputComponents.isEmpty() ) {
         return Collections.emptyList();
      }

      final Collection<String> unfilteredCommands =
            Arrays.stream( Output.values() )
                  .map( _outputComponents::get )
                  .filter( OutputComponent::isSelected )
                  .map( OutputComponent::getOutput )
                  .map( Output::getPiperLines )
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
      for ( OutputComponent outputComponent : _outputComponents.values() ) {
         if ( !outputComponent.isSelected() ) {
            continue;
         }
         summaryLines.add( outputComponent.getOutput().getDescription() );
      }
      return "<HTML>" + String.join( "<BR>", summaryLines ) + "</HTML>";
   }

   public Collection<Output> getSelectedOutputs() {
      return _outputComponents.entrySet().stream()
                              .filter( e -> e.getValue().isSelected() )
                              .map( Map.Entry::getKey )
                              .collect( Collectors.toList() );
   }


}
