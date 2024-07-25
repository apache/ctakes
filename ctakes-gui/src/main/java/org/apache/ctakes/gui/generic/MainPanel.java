package org.apache.ctakes.gui.generic;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {9/20/2022}
 */
public class MainPanel extends JPanel {

   static private final Logger LOGGER = LogManager.getLogger( "MainPanel" );

   private JButton _runButton;
   private JButton _stopButton;

   MainPanel() {
      super( new BorderLayout() );
      setBorder( new EmptyBorder( 2, 2, 2, 2 ) );
      add( createToolBar(), BorderLayout.NORTH );
      add( LoggerPanel.createLoggerPanel(), BorderLayout.CENTER );
      SwingUtilities.invokeLater( new ButtonIconLoader() );
   }

   public void readParameterFile( final String... args ) {
      if ( args.length != 1 ) {
         logBadArgs( args );
         return;
      }
      final File parmFile = new File( args[ 0 ] );
      if ( !parmFile.canRead() ) {
         LOGGER.error( "Cannot read parameter file: " + args[ 0 ] );
         LOGGER.info( "Please exit the application" );
         return;
      }
      String name = "";
      String startCommand = "";
      String directory = "";
      String stopCommand = "";
      try ( BufferedReader reader = new BufferedReader( new FileReader( args[ 0 ] ) ) ) {
         String line = "";
         while ( line != null ) {
            if ( !line.isEmpty() && !line.startsWith( "//" ) ) {
               if ( name.isEmpty() ) {
                  name = line;
               } else if ( startCommand.isEmpty() ) {
                  startCommand = line;
               } else if ( directory.isEmpty() ) {
                  directory = line;
               } else if ( stopCommand.isEmpty() ) {
                  stopCommand = line;
               } else {
                  LOGGER.warn( "Ignoring extra line: " + line );
               }
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      _runButton.addActionListener( new StartAction( name, startCommand, directory ) );
      _stopButton.addActionListener( new StopAction( name, stopCommand, directory ) );

   }

   static private void logBadArgs( final String... args ) {
      if ( args.length > 1 ) {
         LOGGER.error( "There are too many arguments in " + String.join( " ", args ) );
      }
      LOGGER.error( "A single argument pointing to a File containing run parameters is required." );
      LOGGER.info( "The file format is:" );
      LOGGER.info( "Application Title" );
      LOGGER.info( "Start Command" );
      LOGGER.info( "Starting Directory (optional)" );
      LOGGER.info( "Stop Command (optional)" );
      LOGGER.info( "Please exit the application" );
   }


   private JToolBar createToolBar() {
      final JToolBar toolBar = new JToolBar();
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      _runButton = addButton( toolBar, "Start " );
      _runButton.setEnabled( false );
      toolBar.addSeparator( new Dimension( 50, 0 ) );
      _stopButton = addButton( toolBar, "Stop " );
      _stopButton.setEnabled( false );

      toolBar.addSeparator( new Dimension( 50, 0 ) );
      toolBar.addSeparator( new Dimension( 10, 0 ) );

      return toolBar;
   }

   static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
//      button.setFocusable( false );
      button.setToolTipText( toolTip );
      toolBar.add( button );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return button;
   }


   private final class StartAction implements ActionListener {

      private final String _name;
      private final String _command;
      private final String _dir;

      private StartAction( final String name,
                           final String command,
                           final String dir ) {
         _name = name;
         _command = command;
         _dir = dir;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _runButton == null ) {
            return;
         }
         final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( _command );
         runner.setLogger( LOGGER );
         runner.wait( true );
         if ( _dir != null && !_dir.isEmpty() ) {
            runner.setDirectory( _dir );
         }
         LOGGER.info( "Starting " + _name + "  ..." );
         try {
            SystemUtil.run( runner );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
      }

   }


   private final class StopAction implements ActionListener {

      private final String _name;
      private final String _command;
      private final String _dir;

      private StopAction( final String name,
                          final String command,
                          final String dir ) {
         _name = name;
         _command = command;
         _dir = dir;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _runButton == null ) {
            return;
         }
         final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( _command );
         runner.setLogger( LOGGER );
         runner.wait( true );
         if ( _dir != null && !_dir.isEmpty() ) {
            runner.setDirectory( _dir );
         }
         LOGGER.info( "Stopping " + _name + "  ..." );
         try {
            SystemUtil.run( runner );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
      }

   }


   /**
    * Simple Startable that loads an icon
    * <p>
    * Some icons
    * <a href="https://www.freepik.com/free-vector/no-entry-hand-sign-isolated-white_10601278.htm#query=stop%20hand&position=1&from_view=keyword">Image by macrovector</a> on Freepik
    */
   private final class ButtonIconLoader implements Runnable {

      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String runPng = "RunPiper.png";
         final String stopPng = "StopHand.png";

         final Icon runIcon = IconLoader.loadIcon( dir + runPng );
         final Icon stopIcon = IconLoader.loadIcon( dir + stopPng );
         _runButton.setIcon( runIcon );
         _stopButton.setIcon( stopIcon );
      }

   }


}
