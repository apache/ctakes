package org.apache.ctakes.gui.component;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
final public class FileChooserPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "FileChooserPanel" );

   public FileChooserPanel( final String name, final String defaultDirectory,
                            final ActionListener fileChangeListener ) {
      this( name, defaultDirectory, false, fileChangeListener );
   }


   public FileChooserPanel( final String name, final String defaultDirectory, final boolean selectDir,
                            final ActionListener fileChangeListener ) {
      super( new BorderLayout( 10, 10 ) );
      setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( name );
      label.setPreferredSize( new Dimension( 150, 0 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final JTextField textField = new JTextField( defaultDirectory );
      textField.setEditable( false );
      final JButton openChooserButton
            = new JButton( new OpenFileChooserAction( textField, selectDir, fileChangeListener ) );
      add( label, BorderLayout.WEST );
      add( textField, BorderLayout.CENTER );
      add( openChooserButton, BorderLayout.EAST );

      textField.setDropTarget( new FileDropTarget( textField, selectDir, fileChangeListener ) );
      textField.addActionListener( fileChangeListener );
   }

   /**
    * Opens the JFileChooser
    */
   static private class OpenFileChooserAction extends AbstractAction {
      private final JFileChooser __chooser;
      private final JTextComponent __textComponent;
      private final ActionListener __fileChangeListener;

      private OpenFileChooserAction( final JTextComponent textComponent, final boolean selectDir,
                                     final ActionListener dirChangeListener ) {
         super( "Select " + (selectDir ? "Directory" : "File") );
         __textComponent = textComponent;
         __chooser = new JFileChooser();
         String cwdPath = Paths.get( "" ).toAbsolutePath().toFile().getPath();
         if ( cwdPath.isEmpty() ) {
            cwdPath = System.getProperty( "user.dir" );
         }
         if ( cwdPath != null && !cwdPath.isEmpty() ) {
            __chooser.setCurrentDirectory( new File( cwdPath ) );
         }
         __chooser.setFileSelectionMode( (selectDir ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY) );
         __fileChangeListener = dirChangeListener;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         final String startDirPath = __textComponent.getText();
         if ( startDirPath != null && !startDirPath.isEmpty() ) {
            final File startingDir = new File( startDirPath );
            if ( startingDir.exists() ) {
               __chooser.setCurrentDirectory( startingDir );
            }
         }
         final int option = __chooser.showOpenDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = __chooser.getSelectedFile();
         __textComponent.setText( file.getAbsolutePath() );
         final ActionEvent fileEvent = new ActionEvent( this, ActionEvent.ACTION_FIRST, file.getAbsolutePath() );
         __fileChangeListener.actionPerformed( fileEvent );
      }
   }


   static private class FileDropTarget extends DropTarget {
      private final JTextComponent __textComponent;
      private final ActionListener __fileChangeListener;
      private final boolean __selectDir;

      private FileDropTarget( final JTextComponent textComponent, final boolean selectDir,
                              final ActionListener dirChangeListener ) {
         __textComponent = textComponent;
         __selectDir = selectDir;
         __fileChangeListener = dirChangeListener;
      }

      @Override
      public synchronized void drop( final DropTargetDropEvent event ) {
         event.acceptDrop( DnDConstants.ACTION_COPY );
         try {
            final Object values = event.getTransferable().getTransferData( DataFlavor.javaFileListFlavor );
            if ( !(values instanceof Iterable) ) {
               return;
            }
            for ( Object value : (Iterable)values ) {
               if ( !(value instanceof File) ) {
                  continue;
               }
               final File file = (File)value;
               if ( file.isDirectory() != __selectDir ) {
                  continue;
               }
               __textComponent.setText( file.getAbsolutePath() );
               final ActionEvent fileEvent
                     = new ActionEvent( this, ActionEvent.ACTION_FIRST, file.getAbsolutePath() );
               __fileChangeListener.actionPerformed( fileEvent );
               return;
            }
         } catch ( UnsupportedFlavorException | IOException multE ) {
            LOGGER.warn( multE.getMessage() );
         }
      }
   }


}
