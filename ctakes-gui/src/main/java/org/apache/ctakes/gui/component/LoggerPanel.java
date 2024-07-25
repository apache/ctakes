package org.apache.ctakes.gui.component;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2016
 */
final public class LoggerPanel extends JScrollPane {

   static private final int MAX_DOC_LENGTH = Integer.MAX_VALUE - 5000;

   static public LoggerPanel createLoggerPanel( final Level... levels ) {
      final LoggerPanel panel = new LoggerPanel( levels );
      addAppender( LogManager.getRootLogger(), panel.getLogHandler() );
//      LogManager.getRootLogger()
//                .addAppender( panel.getLogHandler() );
      final Logger pa = LogManager.getLogger( "ProgressAppender" );
      if ( pa != null ) {
//         pa.addAppender( panel.getLogHandler() );
         addAppender( pa, panel.getLogHandler() );
      }
      final Logger pd = LogManager.getLogger( "ProgressDone" );
      if ( pd != null ) {
//         pd.addAppender( panel.getLogHandler() );
         addAppender( pd, panel.getLogHandler() );
      }
      return panel;
   }

   static private void addAppender( final Logger logger, final Appender appender ) {
      if ( logger instanceof org.apache.logging.log4j.core.Logger ) {
         ((org.apache.logging.log4j.core.Logger)logger).addAppender( appender );
      } else {
         logger.error( logger.getClass().getName() + " is not a org.apache.logging.log4j.core.Logger !" );
      }
   }

   static private final Level[] ALL_LEVELS = { Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG,
                                               Level.TRACE };


   private final Appender _appender;
   private final DefaultStyledDocument _textAreaDoc = new DefaultStyledDocument();

   /**
    * text gui that will display log4j messages
    */
   private LoggerPanel( final Level... levels ) {
      new LoggerTextFilter( _textAreaDoc );
      final JTextPane textPane = new JTextPane( _textAreaDoc );
      textPane.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 14 ) );
      textPane.setEditable( false );
      super.setViewportView( textPane );
      _appender = new LogHandler( levels );

   }

   /**
    * @return all the text in this gui
    */
   public String getText() {
      try {
         return _textAreaDoc.getText( 0, _textAreaDoc.getLength() );
      } catch ( BadLocationException blE ) {
         return "";
      }
   }

   /**
    * clear the text in this gui
    */
   public void clearText() {
      SwingUtilities.invokeLater( () -> {
         try {
            _textAreaDoc.remove( 0, _textAreaDoc.getLength() );
         } catch ( BadLocationException blE ) {
            //
         }
      } );
   }

   /**
    * @param text to append to the text displayed in this gui
    */
   public void appendText( final String text ) {
      SwingUtilities.invokeLater( () -> {
         try {
            if (_textAreaDoc.getLength() >= MAX_DOC_LENGTH ) {
               // clear log if it gets too long.  Not the best solution, but good enough for now.
               _textAreaDoc.remove( 0, _textAreaDoc.getLength() );
            }
            _textAreaDoc.insertString( _textAreaDoc.getLength(), text, null );
         } catch ( BadLocationException blE ) {
            //
         }
      } );
   }


   /**
    * @return the log4j appender that handles logging
    */
   private Appender getLogHandler() {
      return _appender;
   }

   /**
    * Handles reception of logging messages
    */
//   private class LogHandler extends AppenderSkeleton {
   private class LogHandler extends AbstractAppender {
      private final Collection<Level> _levels;

      private LogHandler( final Level... levels ) {
         super( "LoggerPanel", null, null, true, Property.EMPTY_ARRAY );
         _levels = Arrays.asList( (levels.length == 0 ? ALL_LEVELS : levels) );
      }

      /**
       * {@inheritDoc}
       */
      @Override
//      protected void append( final LogEvent event ) {
      public void append( final LogEvent event ) {
         if ( event == null ) {
            return;
         }
         final Level level = event.getLevel();
         if ( _levels.contains( level ) ) {
            final Object message = event.getMessage();
            if ( message != null ) {
               String text = message.toString();
               if ( level.equals( Level.FATAL ) || level.equals( Level.ERROR ) ) {
                  if ( !text.toLowerCase()
                            .contains( "error" ) ) {
                     text = "Error: " + text;
                  }
               } else if ( level.equals( Level.WARN ) ) {
                  if ( !text.toLowerCase()
                            .contains( "warn" ) ) {
                     text = "Warning: " + text;
                  }
               } else if ( level.equals( Level.DEBUG ) ) {
                  text = "// " + text;
               }
               if ( text.equals( "." ) ) {
                  appendText( text );
               } else {
                  appendText( text + "\n" );
               }
            }
         }
      }

//      /**
//       * {@inheritDoc}
//       *
//       * @return false
//       */
//      @Override
//      public boolean requiresLayout() {
//         return false;
//      }
//
//      /**
//       * {@inheritDoc}
//       */
//      @Override
//      public void close() {
//      }
   }


}
