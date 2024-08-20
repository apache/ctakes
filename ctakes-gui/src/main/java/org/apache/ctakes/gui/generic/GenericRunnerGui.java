package org.apache.ctakes.gui.generic;

import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.pipeline.PiperRunnerGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Can run a simple command line.
 *
 * @author SPF , chip-nlp
 * @since {9/20/2022}
 */
final public class GenericRunnerGui {

   static private final Logger LOGGER = LoggerFactory.getLogger( "GenericRunnerGui" );

   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "cTAKES Simple Program Frame" );
      frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
      // Use 1024 x 768 as the minimum required resolution (XGA)
      // iPhone 3 : 480 x 320 (3:2, HVGA)
      // iPhone 4 : 960 x 640  (3:2, unique to Apple)
      // iPhone 5 : 1136 x 640 (under 16:9, unique to Apple)
      // iPad 3&4 : 2048 x 1536 (4:3, QXGA)
      // iPad Mini: 1024 x 768 (4:3, XGA)
      final Dimension size = new Dimension( 1024, 768 );
      frame.setSize( size );
      frame.setMinimumSize( size );
      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      return frame;
   }


   static private String[] _args;

   public static void main( final String... args ) {
      // At jdk 8 this was supposedly unnecessary.  I guess that it is back ...
      _args = args;
      SwingUtilities.invokeLater( GenericRunnerGui::run );
   }


   static private void run() {
      try {
         UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
         UIManager.getDefaults()
                  .put( "SplitPane.border", BorderFactory.createEmptyBorder() );
         // Needed for MacOS, which sets gridlines to white by default
         UIManager.getDefaults()
                  .put( "Table.gridColor", Color.GRAY );
      } catch ( ClassNotFoundException | InstantiationException
            | IllegalAccessException | UnsupportedLookAndFeelException multE ) {
         LOGGER.error( multE.getLocalizedMessage() );
      }
      final JFrame frame = createFrame();
      final MainPanel mainPanel = new MainPanel();
      frame.add( mainPanel );
      frame.pack();
      frame.setVisible( true );
      DisablerPane.getInstance()
                  .initialize( frame );
      mainPanel.readParameterFile( _args );
      LOGGER.info( "To start, click the Green Circular button above." );
      LOGGER.info( "To stop, click the Red X button above." );
      // Check for -p and -c specification of piper file and cli parameter file
   }


}
