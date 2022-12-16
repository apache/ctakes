package org.apache.ctakes.gui.dictionary.cased.term;

import org.apache.ctakes.gui.dictionary.util.TextTokenizer;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2020
 */
public interface TermLine {

   String getCui();

   String getText();

   int getPrefScore();

   String getSource();

   String getCode();

   default String getTokenizedText() {
      return getTokenizedText( getText() );
   }

   static int getMaxScore() {
      // Right now the max score is TS=P SAB=NCI STT=PF TTY=PN  + 2 for custom term.  = 50
      return 2 * 3 * 2 * 4 + 2;
   }

   static int getHalfScore() {
      return getMaxScore() / 2;
   }

   static String getTokenizedText( final String text ) {
      String tokenized = TextTokenizer.getTokenizedText( text );
      if ( tokenized.endsWith( " nos" ) ) {
         tokenized = tokenized.substring( 0, tokenized.length() - 4 );
         if ( tokenized.endsWith( " ," ) ) {
            tokenized = tokenized.substring( 0, tokenized.length() - 2 );
         }
      }
      if ( tokenized.startsWith( "[ x ] " ) ) {
         tokenized = tokenized.substring( 6 );
      } else if ( tokenized.startsWith( "[ d ] " ) ) {
         tokenized = tokenized.substring( 6 );
      }
      return tokenized;
   }

}
