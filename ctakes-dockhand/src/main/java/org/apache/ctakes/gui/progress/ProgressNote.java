package org.apache.ctakes.gui.progress;


import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/10/2019
 */
public enum ProgressNote {
   INSTANCE;

   static public ProgressNote getInstance() {
      return INSTANCE;
   }

   private final JProgressBar _progressBar;

   ProgressNote() {
      UIManager.put( "ProgressBar.repaintInterval", 500 );
      UIManager.put( "ProgressBar.cycleTime", 3500 );
      _progressBar = new JProgressBar();
      _progressBar.setUI( new NoteMarkupProgressUI() );
      _progressBar.setBackground( Color.WHITE );
      _progressBar.setVisible( false );
   }

   public JComponent getComponent() {
      return _progressBar;
   }

   public void startProgress() {
      _progressBar.setVisible( true );
      _progressBar.setIndeterminate( true );
   }

   public void stopProgress() {
      _progressBar.setVisible( false );
      _progressBar.setIndeterminate( false );
   }


}
