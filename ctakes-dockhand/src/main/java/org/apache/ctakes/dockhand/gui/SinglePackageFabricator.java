package org.apache.ctakes.dockhand.gui;


import org.apache.ctakes.dockhand.gui.wizard.DhWizardController;

import javax.swing.*;
import java.awt.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/19/2018
 */
final public class SinglePackageFabricator {


   // TODO : Copy basic elements (name, packaging, war plugin) from entity pom to other poms.


   private SinglePackageFabricator() {
   }

   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "cTAKES Single Package Fabricator" );
      frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
      // Use 1024 x 768 as the minimum required resolution (XGA)
      // iPhone 3 : 480 x 320 (3:2, HVGA)
      // iPhone 4 : 960 x 640  (3:2, unique to Apple)
      // iPhone 5 : 1136 x 640 (under 16:9, unique to Apple)
      // iPad 3&4 : 2048 x 1536 (4:3, QXGA)
      // iPad Mini: 1024 x 768 (4:3, XGA)
      final Dimension size = new Dimension( 800, 600 );
      frame.setSize( size );
      frame.setMinimumSize( size );
      // TODO - make save/load installation types?
//      final JMenuBar menuBar = new JMenuBar();
//      final JMenu fileMenu = new JMenu( "File" );
//      menuBar.add( fileMenu );
//
//      frame.setJMenuBar( menuBar );
      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
//      JFrame.setDefaultLookAndFeelDecorated( true );
      return frame;
   }

   static private JComponent createMainPanel() {
      return new MainPanel( new DhWizardController() ).createPanel();
   }


   public static void main( final String... args ) {
      try {
         UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
         UIManager.getDefaults().put( "SplitPane.border", BorderFactory.createEmptyBorder() );
      } catch ( ClassNotFoundException | InstantiationException
            | IllegalAccessException | UnsupportedLookAndFeelException multE ) {
//         LOGGER.severe( multE.getLocalizedMessage() );
      }
      final JFrame frame = createFrame();
      final JComponent mainPanel = createMainPanel();
      frame.add( mainPanel );
      frame.pack();
      frame.setVisible( true );
      DisablerPane.getInstance().initialize( frame );
   }

}
