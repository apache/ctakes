package org.apache.ctakes.gui.pipeline.piper;

import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2017
 */
final public class PiperTextFilter extends DocumentFilter {

   static private final Logger LOGGER = Logger.getLogger( "PiperTextFilter" );

   final private TextValidator _textValidator;
   final private TextFormatter _textFormatter;


   public PiperTextFilter( final DefaultStyledDocument document ) {
      _textFormatter = new TextFormatter( document );
      _textValidator = new TextValidator( document );
      document.setDocumentFilter( this );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void remove( final FilterBypass fb, final int begin, final int length ) throws BadLocationException {
      super.remove( fb, begin, length );
      if ( shouldReformat( fb.getDocument(), begin, length ) ) {
         formatText( fb.getDocument() );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void insertString( final FilterBypass fb, final int begin, final String text, final AttributeSet attr )
         throws BadLocationException {
      super.insertString( fb, begin, text, attr );
//      if ( shouldValidate( fb.getDocument(), begin, text.length() ) ) {
//         validateText();
//      } else
      if ( shouldReformat( fb.getDocument(), begin, text.length() ) ) {
         formatText( fb.getDocument() );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void replace( final FilterBypass fb, final int begin, final int length, final String text,
                        final AttributeSet attrs )
         throws BadLocationException {
      super.replace( fb, begin, length, text, attrs );
//      if ( shouldValidate( fb.getDocument(), begin, length ) ) {
//         validateText();
//      } else
      if ( shouldReformat( fb.getDocument(), begin, length ) ) {
         formatText( fb.getDocument() );
      }
   }

   static private boolean shouldReformat( final Document document, final int begin, final int length )
         throws BadLocationException {
      final int testLength = Math.min( length + 2, document.getLength() - begin );
      final String deltaText = document.getText( begin, testLength );
      return deltaText.contains( " " ) || deltaText.contains( "\n" ) || deltaText.contains( "\t" );
   }

   private void formatText( final Document document ) {
      if ( StyledDocument.class.isInstance( document ) ) {
         SwingUtilities.invokeLater( _textFormatter );
      }
   }

   static private boolean shouldValidate( final Document document, final int begin, final int length )
         throws BadLocationException {
      final int testLength = Math.min( length + 2, document.getLength() - begin );
      final String deltaText = document.getText( begin, testLength );
      return deltaText.contains( "\n" );
   }

   public boolean validateText() {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<Boolean> validator = executor.submit( (Callable<Boolean>)_textValidator );
      try {
         boolean valid = validator.get( 1000, TimeUnit.MILLISECONDS );
         executor.shutdown();
         return valid;
      } catch ( InterruptedException | ExecutionException | TimeoutException multE ) {
         LOGGER.warn( "Piper validation timed out." );
         executor.shutdown();
         return false;
      }
   }


   static private class TextFormatter implements Runnable {
      final StyledDocument _document;
      final Map<String, Style> _styles = new HashMap<>();

      private TextFormatter( final StyledDocument document ) {
         _document = document;
         createStyles();
      }

      @Override
      public void run() {
         try {
            final String text = _document.getText( 0, _document.getLength() );
            int lineBegin = 0;
            boolean lineEnded = false;
            for ( int i = 0; i < _document.getLength(); i++ ) {
               lineEnded = false;
               if ( text.charAt( i ) == '\n' ) {
                  formatLine( lineBegin, i );
                  lineBegin = i + 1;
                  lineEnded = true;
               }
            }
            if ( !lineEnded ) {
               formatLine( lineBegin, _document.getLength() );
            }
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
         }
      }

      private void createStyles() {
         createStyle( "PLAIN", Color.BLACK, false, "PLAIN" );
         final Style comment = createStyle( "COMMENT", Color.GRAY, false, "COMMENT" );
         StyleConstants.setItalic( comment, true );
         final Style error = createStyle( "ERROR", Color.RED, false, "ERROR" );
         StyleConstants.setUnderline( error, true );
         createStyle( "PIPE_BIT", Color.BLUE, false, "PIPE_BIT" );
         createStyle( "BOLD_PIPE_BIT", Color.BLUE, "BOLD_PIPE_BIT" );
         createStyle( "PARAMETER", Color.YELLOW, "PARAMETER" );
         createStyle( "LOAD", Color.MAGENTA, "load" );
         createStyle( "PACKAGE", Color.YELLOW.darker(), "package" );
         createStyle( "SET", Color.ORANGE.darker(), "set", "cli" );
         createStyle( "READER", Color.GREEN.darker().darker(), "reader", "readFiles" );
         createStyle( "ADD", Color.CYAN.darker().darker(), "add", "addLogged", "addDescription", "addLast" );
         createStyle( "WRITE_XMI", Color.BLUE.darker(), "writeXmis", "collectCuis", "collectEntities" );
      }

      private Style createStyle( final String name, final Color color, final String... keys ) {
         return createStyle( name, color, true, keys );
      }

      private Style createStyle( final String name, final Color color, final boolean bold, final String... keys ) {
         final Style style = _document.addStyle( name, null );
         StyleConstants.setForeground( style, color );
         if ( bold ) {
            StyleConstants.setBold( style, true );
         }
         Arrays.stream( keys ).forEach( k -> _styles.put( k, style ) );
         return style;
      }

      boolean formatLine( final int begin, final int end ) throws BadLocationException {
         final int length = end - begin;
         if ( length <= 0 ) {
            return true;
         }
         final String text = _document.getText( begin, length );
         if ( text.startsWith( "#" ) || text.startsWith( "//" ) || text.startsWith( "!" ) ) {
            _document.setCharacterAttributes( begin, length, _styles.get( "COMMENT" ), true );
            return true;
         }
         int commandEnd = text.indexOf( ' ' );
         if ( commandEnd < 0 ) {
            commandEnd = length;
         }
         final String command = text.substring( 0, commandEnd );
         final Style commandStyle = getCommandStyle( command );
         _document.setCharacterAttributes( begin, commandEnd, commandStyle, true );
         if ( length > commandEnd ) {
            int styleEnd = commandEnd + 1;
            if ( command.equals( "reader" ) || command.startsWith( "add" ) || command.equals( "load" ) ) {
               final int bitStart = commandEnd + 1;
               int bitEnd = text.indexOf( ' ', bitStart );
               if ( bitEnd < 0 ) {
                  bitEnd = length;
               }
               int bitBold = bitStart;
               final String pipeBitText = text.substring( bitStart, bitEnd );
               final int dotIndex = pipeBitText.lastIndexOf( '.' );
               if ( dotIndex > 0 ) {
                  bitBold = bitStart + dotIndex + 1;
                  _document.setCharacterAttributes(
                        begin + bitStart, bitBold - bitStart, _styles.get( "PIPE_BIT" ), true );
               }
               _document
                     .setCharacterAttributes( begin + bitBold, bitEnd - bitBold, _styles.get( "BOLD_PIPE_BIT" ), true );
               styleEnd = bitEnd;
            }
            _document.setCharacterAttributes( begin + styleEnd, length - styleEnd, _styles.get( "PLAIN" ), true );
         }
         if ( commandStyle.equals( _styles.get( "ERROR" ) ) ) {
            return false;
         }
         return true;
      }

      private Style getCommandStyle( final String command ) {
         final Style style = _styles.get( command );
         if ( style == null ) {
            return _styles.get( "ERROR" );
         }
         return style;
      }

      //      private Style getParameterStyle( final String parameter ) {
//         final Style style = _styles.get( command );
//         if ( style == null ) {
//            return _styles.get( "ERROR" );
//         }
//         return style;
//      }
   }


   static private final class TextValidator extends TextFormatter implements Callable<Boolean> {
      final private PiperFileReader _reader;
      private boolean _haveReader;

      private TextValidator( final StyledDocument document ) {
         super( document );
         _reader = new PiperFileReader();
      }

      @Override
      public Boolean call() {
         _haveReader = false;
         boolean valid = true;
         try {
            final String text = _document.getText( 0, _document.getLength() );
            int lineBegin = 0;
            boolean lineEnded = false;
            for ( int i = 0; i < _document.getLength(); i++ ) {
               lineEnded = false;
               if ( text.charAt( i ) == '\n' ) {
                  if ( validateLine( lineBegin, i ) ) {
                     formatLine( lineBegin, i );
                  } else {
                     valid = false;
                  }
                  lineBegin = i + 1;
                  lineEnded = true;
               }
            }
            if ( !lineEnded ) {
               formatLine( lineBegin, _document.getLength() );
               valid = false;
            }
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
            valid = false;
         }
         _document.setCharacterAttributes( _document.getLength(), 1, _styles.get( "PLAIN" ), true );
         _reader.getBuilder().clear();
         if ( !_haveReader ) {
            LOGGER.warn( "No Reader specified" );
            return false;
         }
         return valid;
      }

      private boolean validateLine( final int begin, final int end ) throws BadLocationException {
         final int length = end - begin;
         if ( length <= 0 ) {
            return true;
         }
         final String text = _document.getText( begin, length );
         if ( text.startsWith( "#" ) || text.startsWith( "//" ) || text.startsWith( "!" ) ) {
            return true;
         } else if ( text.startsWith( "readFiles " ) || text.startsWith( "reader " ) ) {
            if ( _haveReader ) {
               LOGGER.warn( "More than one Reader specified" );
               _document.setCharacterAttributes( begin, end, _styles.get( "ERROR" ), true );
               return false;
            }
            _haveReader = true;
         }
         try {
            return _reader.parsePipelineLine( text );
         } catch ( UIMAException uE ) {
            LOGGER.warn( uE.getMessage() );
            _document.setCharacterAttributes( begin, end, _styles.get( "ERROR" ), true );
            return false;
         }
      }
   }


}
