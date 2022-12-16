package org.apache.ctakes.gui.component;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {10/18/2022}
 */
public class LoggerTextFilter extends DocumentFilter {

   private final Map<String, Style> _styles = new HashMap<>();


   public LoggerTextFilter( final DefaultStyledDocument document ) {
      document.setDocumentFilter( this );
      createStyles( document );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void remove( final FilterBypass fb, final int begin, final int length ) throws BadLocationException {
      super.remove( fb, begin, length );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void insertString( final FilterBypass fb, final int begin, final String text, final AttributeSet attr )
         throws BadLocationException {
      super.insertString( fb, begin, text, attr );
      formatText( fb.getDocument(), begin, text.length(), text );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void replace( final FilterBypass fb, final int begin, final int length, final String text,
                        final AttributeSet attrs )
         throws BadLocationException {
      super.replace( fb, begin, length, text, attrs );
      formatText( fb.getDocument(), begin, length, text );
   }

   private void formatText( final Document document, final int begin, final int length, final String text ) {
      if ( !( document instanceof StyledDocument ) ) {
         return;
      }
      final StyledDocument doc = (StyledDocument) document;
      SwingUtilities.invokeLater( () -> {
         if ( text.startsWith( "#" ) || text.startsWith( "//" ) ) {
            doc.setCharacterAttributes( begin, length, _styles.get( "COMMENT" ), false );
            return;
         }
         final String lower = text.toLowerCase();
         if ( lower.contains( "error" ) ) {
            doc.setCharacterAttributes( begin, length, _styles.get( "ERROR" ), false );
         } else if ( lower.contains( "warn" ) ) {
            doc.setCharacterAttributes( begin, length, _styles.get( "WARNING" ), false );
         } else if ( lower.contains( "..." ) ) {
            doc.setCharacterAttributes( begin, length, _styles.get( "ELLIPSES" ), false );
         } else {
            doc.setCharacterAttributes( begin, length, _styles.get( "PLAIN" ), false );
         }
      } );
   }

   private void createStyles( final StyledDocument doc ) {
      StyleConstants.setItalic( createStyle( doc, "COMMENT", Color.GRAY ), true );
      createStyle( doc, "ELLIPSES", Color.BLUE );
      createStyle( doc, "ERROR", Color.RED.darker() );
      createStyle( doc, "WARNING", Color.ORANGE.darker() );
      createStyle( doc, "PLAIN", Color.BLACK );
   }

   private Style createStyle( final StyledDocument doc, final String name, final Color color ) {
      final Style style = doc.addStyle( name, null );
      StyleConstants.setForeground( style, color );
      _styles.put( name, style );
      return style;
   }


}
