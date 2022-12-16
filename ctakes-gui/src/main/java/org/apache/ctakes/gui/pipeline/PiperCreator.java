package org.apache.ctakes.gui.pipeline;

import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.pipeline.bit.PipeBitPainter;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/20/2016
 */
final public class PiperCreator {

   static private final Logger LOGGER = Logger.getLogger( "PiperCreator" );

   private PiperCreator() {
   }

   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "cTAKES Simple Pipeline Fabricator" );
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
//      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      return frame;
   }

   static private MainPanel2 createMainPanel() {
      return new MainPanel2();
   }

   public static void main( final String... args ) {
      SwingUtilities.invokeLater( () -> {
         try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
            UIManager.getDefaults().put( "SplitPane.border", BorderFactory.createEmptyBorder() );
         } catch ( ClassNotFoundException | InstantiationException
               | IllegalAccessException | UnsupportedLookAndFeelException multE ) {
            LOGGER.error( multE.getLocalizedMessage() );
         }
         final JFrame frame = createFrame();
         final MainPanel2 mainPanel = createMainPanel();
         frame.add( mainPanel );
         frame.pack();
         frame.setVisible( true );
         DisablerPane.getInstance().initialize( frame );
         PipeBitPainter.getInstance().loadIcons();
         LOGGER.info( "1. Select any Pipe Bit in the list on the left." );
         LOGGER.info( "   A Pipe Bit is any part of a full Pipeline: Reader, Annotator, Writer." );
         LOGGER.info( "   Once selected, the center panel will be populated with information on the Pipe Bit." );
         LOGGER.info( "2. Edit values for Pipe Bit Parameters." );
         LOGGER.info( "   Parameters and values are displayed in the central table." );
         LOGGER.info( "   Values can be edited directly on the table." );
         LOGGER.info( "   In the rightmost column is a button to open a Filechooser if needed." );
         LOGGER.info( "3. Add the Pipe Bit to the Pipeline.  Added Pipe Bits will appear in the text panel." );
         LOGGER.info( "   When all Pipe Bit requirements are met, the Add Pipe Bit (\"+\") button will be enabled." );
         LOGGER.info( "4. Validate the Pipeline by clicking the runner at the starting line." );
         LOGGER.info( "   Any part of the pipeline that is invalid will be underlined and red." );
         LOGGER.info( "5. Run the Pipeline.  When the Pipeline is valid the Runner button will be enabled." );
         LOGGER.info( "-  You can manually edit the pipeline at any time." );

         final Object[] options = { "Scan" };
         final Icon scanIcon = IconLoader.loadIcon( "org/apache/ctakes/gui/pipeline/icon/" + "FindOnPc_48.png" );
         JOptionPane.showOptionDialog( frame, "A Scan must be performed to find available Pipe Bits.\n" +
                                              "Pipe Bits are used to assemble a cTAKES Pipeline.\n" +
                                              "***  If you are running in a developer environment and few Pipe Bits are found, " +
                                              "please run PiperCreatorGui in ctakes-examples.",
               "Find Pipe Bits", JOptionPane.YES_OPTION, JOptionPane.PLAIN_MESSAGE, scanIcon, options, options[ 0 ] );
         mainPanel.findPipeBits();
      } );

   }

}
