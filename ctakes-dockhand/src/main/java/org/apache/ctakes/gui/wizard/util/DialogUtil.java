package org.apache.ctakes.gui.wizard.util;


import org.apache.ctakes.dockhand.gui.DisablerPane;
import org.apache.ctakes.gui.progress.ProgressNote;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/10/2019
 */
final public class DialogUtil {

   private DialogUtil() {
   }

   static public File chooseSaveDir() {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      final JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
      final int decision = chooser.showSaveDialog( rootPane );
      if ( decision != JFileChooser.APPROVE_OPTION ) {
         return null;
      }
      return chooser.getSelectedFile();
   }

   static public File chooseSaveDir( final String title ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      final JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle( title );
      chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
      final int decision = chooser.showSaveDialog( rootPane );
      if ( decision != JFileChooser.APPROVE_OPTION ) {
         return null;
      }
      return chooser.getSelectedFile();
   }


   static public boolean chooseSecondaryInstall( final String name ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      final int response = JOptionPane.showConfirmDialog( rootPane,
            name + " was not found in the $PATH.  Would you like to install " + name + "?",
            "Install " + name,
            JOptionPane.YES_NO_OPTION );
      return response == JOptionPane.YES_OPTION;
   }


   static public File chooseExistingInstall( final String title ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      final JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle( title );
      chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
      final int decision = chooser.showOpenDialog( rootPane );
      if ( decision != JFileChooser.APPROVE_OPTION ) {
         return null;
      }
      return chooser.getSelectedFile();
   }


   static public void showError( final String message ) {
      final String fullMessage = "<HTML>" + message + "<BR>" + "Press OK to exit.</HTML>";
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      JOptionPane.showMessageDialog( rootPane, fullMessage, "Error", ERROR_MESSAGE );
      System.exit( 1 );
   }

   static public void initInstall() {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      rootPane.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
      DisablerPane.getInstance().setVisible( true );
      ProgressNote.getInstance().startProgress();
   }


   static public void showInstallComplete( final String name, final String installPath ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      DisablerPane.getInstance().setVisible( false );
      rootPane.setCursor( Cursor.getDefaultCursor() );
      ProgressNote.getInstance().stopProgress();
      JOptionPane.showMessageDialog( rootPane, name + " installed at " + installPath );
   }


   static public void showInstallCanceled( final String name ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      DisablerPane.getInstance().setVisible( false );
      rootPane.setCursor( Cursor.getDefaultCursor() );
      ProgressNote.getInstance().stopProgress();
      JOptionPane.showMessageDialog( rootPane, name + " Installation Cancelled." );
   }


   static public void showProgressDialog( final String title ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      JProgressBar jProgressBar = new JProgressBar();
      jProgressBar.setIndeterminate( true );
      final JOptionPane pane = new JOptionPane( jProgressBar, INFORMATION_MESSAGE );
      JDialog dialog = pane.createDialog( rootPane, title );
      //      pane.add(jProgressBar,1);
      dialog.setVisible( true );
//      dialog.dispose();
   }


   static public void showInstalledDialog( final String name, final String installPath ) {
      showMessageDialog( name + " installed at " + installPath );
   }

   static public void showCanceledDialog( final String name ) {
      showMessageDialog( name + " Installation Cancelled." );
   }

   static public boolean showStartGui() {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      final int start = JOptionPane.showConfirmDialog( rootPane,
            "Start cTAKES Piper File Submitter GUI?",
            "Start cTAKES",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE );
      return start == JOptionPane.YES_OPTION;
   }

   static public void showMessageDialog( final String message ) {
      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
      JOptionPane.showMessageDialog( rootPane, message );
   }


}
