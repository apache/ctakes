package org.apache.ctakes.dockhand.gui.wizard;


import org.apache.ctakes.dockhand.build.LocalInstallBuilder;
import org.apache.ctakes.dockhand.build.RestDockerBuilder;
import org.apache.ctakes.gui.wizard.AbstractWizardStep;
import org.apache.ctakes.gui.wizard.WizardController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2019
 */
final public class InstallTypeStep extends AbstractWizardStep implements DhWizardStep {


   static private final String LOCAL_BINARY = "Local Installation";
   static private final String LOCAL_BINARY_DESCRIPTION = "Create a Local Installation of the application.";

   static private final String REST_DOCKER = "Rest Server Docker Bundle";
   static private final String REST_DOCKER_DESCRIPTION = "Create a Docker File and Supporting Files for a Rest Server.";

   private final DhWizardController _wizardController;
   private Collection<AbstractButton> _radios;

   public InstallTypeStep( final DhWizardController wizardController ) {
      super( "Installation Type", "Select the installation Type." );
      _wizardController = wizardController;
   }

   public String getInstallationType() {
      if ( _radios == null ) {
         return "";
      }
      return _radios.stream()
                    .filter( AbstractButton::isSelected )
                    .map( AbstractButton::getText )
                    .findFirst()
                    .orElse( "" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected JComponent createPanel() {
      final JComponent panel = new JPanel();
      panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );

      panel.add( Box.createVerticalStrut( 100 ) );
      _radios = new ArrayList<>();
      final ButtonGroup buttonGroup = new ButtonGroup();

      final Runnable localBinaryBuilder = new LocalInstallBuilder( _wizardController );
      final JRadioButton localBinary = createRadio( _wizardController,
            LOCAL_BINARY, LOCAL_BINARY_DESCRIPTION, localBinaryBuilder );
      _wizardController.setBuildProcess( localBinaryBuilder );
      buttonGroup.add( localBinary );
      panel.add( localBinary );
      localBinary.setSelected( true );
      _radios.add( localBinary );

      final JRadioButton restDocker = createRadio( _wizardController,
            REST_DOCKER, REST_DOCKER_DESCRIPTION, new RestDockerBuilder( _wizardController ) );
      buttonGroup.add( restDocker );
      panel.add( restDocker );
      _radios.add( restDocker );


      panel.add( Box.createVerticalStrut( 100 ) );
      _radios.forEach( b -> b.setBackground( Color.WHITE ) );
      return wrapInScrollPane( panel );
   }


   static private JRadioButton createRadio( final WizardController wizardController,
                                            final String name,
                                            final String description,
                                            final Runnable builder ) {
      final JRadioButton radio = new JRadioButton( name );
      radio.setToolTipText( description );
      radio.setActionCommand( name );
      radio.addActionListener( new BuilderActioner( name, builder, wizardController ) );
      return radio;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getSummaryInfo() {
      return getInstallationType();
   }


   static private class BuilderActioner implements ActionListener {
      private final String _type;
      private final Runnable _builder;
      private final WizardController _wizardController;

      private BuilderActioner( final String type,
                               final Runnable builder,
                               final WizardController wizardController ) {
         _type = type;
         _builder = builder;
         _wizardController = wizardController;
      }

      public void actionPerformed( final ActionEvent event ) {
         if ( _type.equals( event.getActionCommand() ) ) {
            final Object source = event.getSource();
            if ( source instanceof AbstractButton && ((AbstractButton)source).isSelected() ) {
               _wizardController.setBuildProcess( _builder );
            }
         }
      }
   }


}
