package org.apache.ctakes.gui.progress;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/30/2019
 */
public enum NoteProgressDialog {
   INSTANCE;

   static public NoteProgressDialog getInstance() {
      return INSTANCE;
   }

   private JDialog _dialog;
   private JLabel _processLabel;
   private JLabel _progressLabel;

   private void createDialog() {
      if ( _dialog != null ) {
         return;
      }
      final Frame[] frames = Frame.getFrames();
      final Frame mainFrame = frames.length > 0 ? frames[ 0 ] : null;
      _dialog = new JDialog( mainFrame, "Please wait ...", false );
      _dialog.setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
      _dialog.setSize( 700, 300 );
      _dialog.setLocationRelativeTo( mainFrame );

      final JPanel notePanel = new JPanel();
      notePanel.setLayout( new BoxLayout( notePanel, BoxLayout.Y_AXIS ) );
      notePanel.setBackground( Color.WHITE );
      notePanel.add( Box.createVerticalGlue() );
      notePanel.add( ProgressNote.getInstance().getComponent() );
      notePanel.add( Box.createVerticalGlue() );

      final JPanel labelPanel = new JPanel();
      labelPanel.setLayout( new BoxLayout( labelPanel, BoxLayout.Y_AXIS ) );
      labelPanel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
      labelPanel.setBackground( Color.WHITE );
      labelPanel.add( Box.createVerticalGlue() );
      _processLabel = new JLabel( "" );
      labelPanel.add( _processLabel );
      _progressLabel = new JLabel( "" );
      labelPanel.add( _progressLabel );
      labelPanel.add( Box.createVerticalGlue() );

      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
      panel.setBackground( Color.WHITE );
      panel.add( notePanel, BorderLayout.WEST );
      panel.add( labelPanel, BorderLayout.CENTER );

      _dialog.add( panel );
   }

   public void setProcess( final String process ) {
      createDialog();
      _processLabel.setText( "<HTML><H1>" + process + "</H1></HTML>" );
   }

   public void setProgress( final String progress ) {
      createDialog();
      _progressLabel.setText( progress );
   }

   public void setProgress( final int complete, final int total ) {
      setProgress( "<HTML><B>Progress: </B>" + complete + " / " + total + "</HTML>" );
   }


   public void startProgress() {
      createDialog();
      _dialog.setVisible( true );
      ProgressNote.getInstance().startProgress();
   }

   public void startProgress( final String process ) {
      setProcess( process );
      _dialog.setVisible( true );
      ProgressNote.getInstance().startProgress();
   }

   public void startProgress( final String process, final String progress ) {
      setProcess( process );
      setProgress( progress );
      _dialog.setVisible( true );
      ProgressNote.getInstance().startProgress();
   }

   public void stopProgress() {
      if ( _dialog == null ) {
         return;
      }
      ProgressNote.getInstance().stopProgress();
      _dialog.setVisible( false );
      _dialog.dispose();
      _dialog = null;
   }


}
