package org.apache.ctakes.gui.dictionary;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * @author SPF , chip-nlp
 * @since {12/13/2022}
 */
final public class DictionaryDownloader {

   static private final Logger LOGGER = LogManager.getLogger( "PiperRunnerGui" );
   static private final String DICTIONARY_NAME = "Fast UMLS Dictionary for Apache cTAKES";
   static private final String DICTIONARY_URL
         = "https://sourceforge.net/projects/ctakesresources/files/sno_rx_16ab.zip";
   static private final String DICTIONARY = "sno_rx_16ab";
   static private final String RESOURCE_DIR = "resources/org/apache/ctakes/dictionary/lookup/fast";

   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "cTAKES UMLS Package Fetcher" );
      frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
      // Use 1024 x 768 as the minimum required resolution (XGA)
      // iPhone 3 : 480 x 320 (3:2, HVGA)
      // iPhone 4 : 960 x 640  (3:2, unique to Apple)
      // iPhone 5 : 1136 x 640 (under 16:9, unique to Apple)
      // iPad 3&4 : 2048 x 1536 (4:3, QXGA)
      // iPad Mini: 1024 x 768 (4:3, XGA)
//      final Dimension size = new Dimension( 800, 600 );
      final Dimension size = new Dimension( 1024, 512 );
      frame.setSize( size );
      frame.setMinimumSize( size );
//      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      return frame;
   }


   public static void main( final String... args ) {
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
      frame.add( new MainPanel() );
      frame.pack();
      frame.setVisible( true );
      DisablerPane.getInstance().initialize( frame );
      LOGGER.info( "For information on the Unified Medical Terminology Services, click \"UMLS Help\"."  );
      LOGGER.info( "For information on Apache cTAKES, click the help (?) button."  );
      LOGGER.info( "To download and install the default UMLS dictionary, click the center button." );
   }

   static private final class MainPanel extends JPanel {

      private JButton _downloadButton;
      private JButton _helpButton;

      public MainPanel() {
         super( new BorderLayout() );
         add( createToolBar(), BorderLayout.NORTH );
         add( LoggerPanel.createLoggerPanel(), BorderLayout.CENTER );
         SwingUtilities.invokeLater( () -> {
            final String dir = "org/apache/ctakes/gui/pipeline/icon/";
            final Icon downloadIcon = IconLoader.loadIcon( dir + "Package.png" );
            final Icon helpIcon = IconLoader.loadIcon( dir + "Help_32.png" );
            _downloadButton.setIcon( downloadIcon );
            _helpButton.setIcon( helpIcon );
         } );
      }

      private JToolBar createToolBar() {
         final JToolBar toolBar = new JToolBar();
         toolBar.setFloatable( false );
         toolBar.setRollover( true );
         toolBar.addSeparator( new Dimension( 10, 0 ) );
         final JButton umlsButton = addButton( toolBar, "Unified Medical Terminology Services" );
         umlsButton.setFont( umlsButton.getFont().deriveFont( Font.BOLD, 20f ) );
         umlsButton.setText( "UMLS Help" );
         umlsButton.addActionListener( e -> SystemUtil.openWebPage( " https://www.nlm.nih.gov/research/umls/index.html" ) );
         toolBar.addSeparator( new Dimension( 50, 0 ) );
         _downloadButton = addButton( toolBar, "Download Dictionary" );
         _downloadButton.addActionListener( e -> {
            Executors.newSingleThreadExecutor().execute( download );
         } );
         toolBar.addSeparator( new Dimension( 50, 0 ) );
         _helpButton = addButton( toolBar, "Apache cTAKES UMLS Key Help" );
         _helpButton.addActionListener(
               e -> SystemUtil.openWebPage( "https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+4.0.0.1" ) );
         toolBar.addSeparator( new Dimension( 10, 0 ) );
         return toolBar;
      }


      static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
         final JButton button = new JButton();
         button.setFocusPainted( false );
         // prevents first button from having a painted border
         button.setFocusable( false );
         button.setToolTipText( toolTip );
         toolBar.add( button );
         toolBar.addSeparator( new Dimension( 10, 0 ) );
         return button;
      }

      static private final Runnable download = () -> {
         final SystemUtil.FileDownloader downloader
               = new SystemUtil.FileDownloader( DICTIONARY_URL, DICTIONARY, ".zip" );
         LOGGER.info( "Downloading " + DICTIONARY_NAME + " ..." );
         File tempZip;
         try ( DotLogger dotter = new DotLogger() ) {
            tempZip = downloader.call();
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            return;
         }
         LOGGER.info( "Unzipping " + DICTIONARY_NAME + " ..." );
         File tempUnzip;
         try ( DotLogger dotter = new DotLogger() ) {
            final SystemUtil.FileUnzipper unzipper
                  = new SystemUtil.FileUnzipper( tempZip, Files.createTempDirectory( DICTIONARY ).toFile() );
            tempUnzip = unzipper.call();
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            return;
         }
         LOGGER.info( "Copying to resources " + DICTIONARY_NAME + " ..." );
         try ( DotLogger dotter = new DotLogger() ) {
            SystemUtil.copyDir( tempUnzip.getAbsolutePath() + "/" + DICTIONARY, RESOURCE_DIR );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         LOGGER.info( "Done." );
         LOGGER.info( "Your cTAKES installation now contains " + DICTIONARY + " installed at " + RESOURCE_DIR );
      };

   }


}
