package org.apache.ctakes.gui.dictionary.cased.term;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.gui.dictionary.cased.umls.abbreviation.Lat;
import org.apache.ctakes.gui.dictionary.cased.umls.file.MrConso;
import org.apache.ctakes.gui.dictionary.cased.umls.file.Tty;

import static org.apache.ctakes.gui.dictionary.cased.umls.file.MrConso.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/21/2019
 */
final public class ConsoLine implements TermLine {
   final private String[] _columns;

   public ConsoLine( final String line ) {
      _columns = StringUtil.fastSplit( line, '|' );
   }

   private boolean isTextOk() {
      return isWantedLat() && hasNoSpecialChars();
   }

   public boolean collect() {
      return isTextOk()
//             && hasWantedSynonyms()
             && Tty.collect( column( TTY ) );
   }

   public boolean isUnwantedDrug() {
      final String text = getTokenizedText();
      if ( text.contains( " in " ) && text.endsWith( " dosage form" ) ) {
         return true;
      }
      if ( text.endsWith( " oral tablet" ) || text.endsWith( " oral capsule" )
           || text.endsWith( "ml vial" ) || text.endsWith( "ml injection" ) ) {
         return true;
      }
//      if ( UmlsSource.getSource( column( SAB ) ) != UmlsSource.RXNORM ) {
//         return false;
//      }
      return text.contains( " " ) && !Tty.keep( column( TTY ) );
   }

   public boolean isObsolete() {
      final Tty tty = Tty.getType( column( TTY ) );
      return tty == Tty.OAP || tty == Tty.OF;
   }

   //   public String getCui() {
//      return CuiUtil.getCui( column( CUI ) );
//   }
   public String getCui() {
      return column( CUI );
   }

   public String getText() {
      return column( STR );
   }

   public int getPrefScore() {
      final String text = getText();
      if ( text.length() < 3 ) {
         return 1;
      }
      if ( text.chars().filter( Character::isAlphabetic ).count() < 3 ) {
         return 1;
      }

      int score = 1;
      score = upScore( TS, "P", score, 2 );

//      score = upScore( SAB, UmlsSource.NCI.getName(), score, 3 );
//      score = upScore( SAB, UmlsSource.FMA.getName(), score, 2 );
//      score = upScore( SAB, UmlsSource.SNOMEDCT_US.getName(), score, 2 );
//      score = upScore( SAB, UmlsSource.MTH.getName(), score, 2 );
//      score = upScore( SAB, UmlsSource.NCI_MTH.getName(), score, 3 );

      score = upScore( STT, "PF", score, 2 );
      score = upScore( STT, "VC", score, 2 );
      score = upScore( STT, "VO", score, 2 );
//      score = upScore( ISPREF, "Y", score );  // It usually looks reversed.
//      score = upScore( ISPREF, "N", score, 2 );

      score = upScore( TTY, "PT", score, 3 );
      score = upScore( TTY, "PN", score, 3 );
      score = upScore( TTY, "RXN_PT", score, 2 );
      score = upScore( TTY, "DN", score, 2 );

      if ( text.startsWith( "Entire " )
           || text.startsWith( "Structure of " )
           || text.endsWith( " structure" )
           || text.endsWith( ")" )
           || text.endsWith( "]" )
           || text.endsWith( " NOS" )
//           || text.contains( "-" )
           || text.contains( " or " )
           || !Character.isLetter( text.charAt( 0 ) ) ) {
         return score / 3;
      }
      if ( text.equals( text.toUpperCase() ) ) {
         // Should also work for numbers.
         return score / 2;
      }
      // Prefer fewer-word terms, but only slightly
      final long spaces = text.chars().filter( Character::isWhitespace ).count();
      return (int)Math.max( 1, score - spaces );
   }

   public String getSource() {
      return column( SAB );
   }

   public String getCode() {
      return column( CODE );
   }

   private String column( final MrConso conso ) {
      return _columns[ conso.ordinal() ];
   }

   private int upScore( final MrConso column, final String wanted, final int score, final int weight ) {
      if ( column( column ).equals( wanted ) ) {
         return weight * score;
      }
      return score;
   }

   public boolean isWantedLat() {
      return column( LAT ).equals( Lat.ENG.name() );
   }

//   private boolean hasWantedSynonyms() {
//      return WantedSource.hasWantedSynonyms( column( SAB ) );
//   }


   private boolean hasNoSpecialChars() {
      final String text = getText();
      // strips off all non-ASCII characters
      String txt = text.replaceAll( "[^\\x00-\\x7F]", "" );
      // erases all the ASCII control characters
      txt = txt.replaceAll( "[\\p{Cntrl}&&[^\r\n\t]]", "" );
      // removes non-printable characters from Unicode
      txt = txt.replaceAll( "\\p{C}", "" );
      return text.equals( txt );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return String.join( " | ", _columns );
   }

}
