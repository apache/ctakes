package org.apache.ctakes.gui.component;

import org.apache.log4j.Logger;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A Text Component that will output text that would normally go to the console for standard output or standard error
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2016
 */
final public class SystemOutPanel extends JScrollPane {

   static private final Logger LOGGER = Logger.getLogger( "SystemOutPanel" );


   private final boolean _isStandardOutRedirect;
   private final Document _textAreaDoc = new PlainDocument();
   private PrintStream _oldStandardStream;

   /**
    * new text gui that will display standard output
    */
   public SystemOutPanel() {
      this( true );
   }

   /**
    * @param isStandardOutRedirect true if this gui is to display standard output, false for standard error
    */
   public SystemOutPanel( final boolean isStandardOutRedirect ) {
      _isStandardOutRedirect = isStandardOutRedirect;
      final JTextArea textArea = new JTextArea( _textAreaDoc );
      textArea.setEditable( false );
      super.setViewportView( textArea );
   }

   /**
    * Starts redirecting standard output (or error) from the console (or other) to this gui
    */
   public void startRedirect() {
      final PrintStream newStandardStream = new PrintStream( new UiOutputStream() );
      clearText();
      if ( _isStandardOutRedirect ) {
         _oldStandardStream = System.out;
         System.setOut( newStandardStream );
      } else {
         _oldStandardStream = System.err;
         System.setErr( newStandardStream );
      }
   }

   /**
    * Stops redirecting the standard output (or error) and returns it to its former consumer
    */
   public void endRedirect() {
      if ( _isStandardOutRedirect ) {
         System.setOut( _oldStandardStream );
      } else {
         System.setErr( _oldStandardStream );
      }
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
            _textAreaDoc.insertString( _textAreaDoc.getLength(), text, null );
         } catch ( BadLocationException blE ) {
            //
         }
      } );
   }

   /**
    * stream that will accept characters until a newline, then append the line to this gui
    */
   @NotThreadSafe
   private class UiOutputStream extends OutputStream {
      private final StringBuilder __sb = new StringBuilder();

      @Override
      public void write( final int b ) throws IOException {
         __sb.append( (char)b );
         if ( (char)b == '\n' ) {
            appendText( __sb.toString() );
            __sb.setLength( 0 );
         }
      }

      @Override
      public void flush() throws IOException {
         appendText( __sb.toString() );
         __sb.setLength( 0 );
         super.flush();
      }

      @Override
      public void close() throws IOException {
         appendText( __sb.toString() );
         __sb.setLength( 0 );
         super.close();
      }
   }


}
