package org.apache.ctakes.dockhand.gui.feature;

import org.apache.ctakes.dockhand.gui.wizard.FeaturesStep;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/8/2019
 */
final public class FeatureComponent extends JPanel {

   private final Feature _feature;
   private final JCheckBox _checkBox;
   private final Map<Option, JRadioButton> _optionButtons = new EnumMap<>( Option.class );


   public FeatureComponent( final FeaturesStep.FeatureCheckController controller, final Feature feature ) {
      super( new BorderLayout() );
      setBackground( Color.WHITE );
      _feature = feature;
      _checkBox = new JCheckBox( feature.getName() );
      _checkBox.setBackground( Color.WHITE );
      _checkBox.setToolTipText( feature.getDescription() );
      add( _checkBox, BorderLayout.NORTH );
      final java.util.List<Option> options = feature.getOptions();
      if ( options.size() > 1 ) {
         final JLabel spacer = new JLabel( "     " );
         spacer.setMinimumSize( new Dimension( 100, 10 ) );
         spacer.setSize( new Dimension( 100, 10 ) );
         add( spacer, BorderLayout.WEST );
         final JPanel optionPanel = new JPanel( new GridLayout( options.size(), 1 ) );
         final ButtonGroup optionGroup = new ButtonGroup();
         for ( Option option : options ) {
            final JRadioButton radioButton = new JRadioButton( option.getName() );
            radioButton.setBackground( Color.WHITE );
            radioButton.setToolTipText( option.getDescription() );
            optionGroup.add( radioButton );
            optionPanel.add( radioButton );
            _optionButtons.put( option, radioButton );
         }
         add( optionPanel, BorderLayout.CENTER );
         _optionButtons.get( options.get( 0 ) ).setSelected( true );
         if ( options.size() == 1 ) {
            _optionButtons.get( options.get( 0 ) ).setEnabled( false );
         }
      }
      controller.addFeature( feature, this );
      _checkBox.addActionListener( controller );
   }

   public Feature getFeature() {
      return _feature;
   }

   public void setSelected( final boolean selected ) {
      _checkBox.setSelected( selected );
   }

   public boolean isSelected() {
      return _checkBox.isSelected();
   }

   public void setEnabled( final boolean enable ) {
      _checkBox.setEnabled( enable );
      _optionButtons.values().forEach( b -> b.setEnabled( enable ) );
   }

   public Option getSelectedOption() {
      if ( _feature.getOptions().size() == 1 ) {
         return _feature.getOptions().get( 0 );
      }
      return _optionButtons.entrySet().stream()
                           .filter( e -> e.getValue().isSelected() )
                           .map( Map.Entry::getKey )
                           .findAny()
                           .orElse( null );
   }

}
