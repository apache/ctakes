package org.apache.ctakes.gui.wizard.util;

import org.apache.ctakes.dockhand.gui.DisablerPane;
import org.apache.ctakes.gui.progress.NoteProgressDialog;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2019
 */
final public class RunnerUtil {

   static private final Logger LOGGER = Logger.getLogger( "RunnerUtil" );

   private RunnerUtil() {
   }


//   static public<T> T runWithProgress( final Callable<T> callable ) {
//      return runWithProgress( "Please Wait ...", callable );
//   }


   static public <T> T runWithProgress( final String process, final Callable<T> callable ) {
      startProgress( process );

      final ExecutorService executor = Executors.newSingleThreadExecutor();
      T result = null;
      try {
         final Future<T> future = executor.submit( callable );
         result = future.get();
      } catch ( InterruptedException | ExecutionException multE ) {
         LOGGER.warning( multE.getMessage() );
      }

      endProgress();
      return result;
   }


   static private void startProgress( final String process ) {
//      ProgressNote.getInstance().startProgress();
      DisablerPane.getInstance().setVisible( true );
//      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
//      rootPane.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
      NoteProgressDialog.getInstance().startProgress( process );
   }


   static private void endProgress() {
      DisablerPane.getInstance().setVisible( false );
//      final Component rootPane = SwingUtilities.getRoot( DisablerPane.getInstance() );
//      rootPane.setCursor( Cursor.getDefaultCursor() );
//      ProgressNote.getInstance().stopProgress();
      NoteProgressDialog.getInstance().stopProgress();
   }


}
