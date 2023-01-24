package org.apache.ctakes.gui.progress;


import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author SPF , chip-nlp
 * @since {1/12/2023}
 */
final public class InfoPanel extends JScrollPane {

   static private final int MAX_DOC_LENGTH = Integer.MAX_VALUE - 5000;

   private final OutputStream _stream;
   private final Document _textAreaDoc;

   public InfoPanel() {
      final JTextArea textArea = new JTextArea();
      textArea.setEditable( false );
      super.setViewportView( textArea );
      _textAreaDoc = textArea.getDocument();
      _stream = new InfoStream();
   }

   public OutputStream getStream() {
      return _stream;
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

   public void clearText() {
      SwingUtilities.invokeLater( () -> {
         try {
            _textAreaDoc.remove( 0, _textAreaDoc.getLength() );
         } catch ( BadLocationException blE ) {
            //
         }
      } );
   }

   private final class InfoStream extends OutputStream {
      private final StringBuilder _sb = new StringBuilder();
      @Override
      public void write( final int b ) throws IOException {
         _sb.append( b );
         if ( b == '\n' ) {
            appendText( _sb.toString() );
            _sb.setLength( 0 );
         }
      }
   }


}
