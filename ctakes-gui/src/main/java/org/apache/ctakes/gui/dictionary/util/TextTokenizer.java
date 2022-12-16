package org.apache.ctakes.gui.dictionary.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/16/14
 */
final public class TextTokenizer {

   private TextTokenizer() {
   }


   // TODO : add contractions:
// The following contractions and related items are split into separate tokens.
//    // 's
//    // 've
//    // 're
//    // 'll
//    // 'd
//    // n't
//    // can not

   static private final String[] PREFIXES = {
         "e-",
         "a-",
         "u-",
         "x-",
         "agro-",
         "ante-",
         "anti-",
         "arch-",
         "be-",
         "bi-",
         "bio-",
         "co-",
         "counter-",
         "cross-",
         "cyber-",
         "de-",
         "eco-",
         "ex-",
         "extra-",
         "inter-",
         "intra-",
         "macro-",
         "mega-",
         "micro-",
         "mid-",
         "mini-",
         "multi-",
         "neo-",
         "non-",
         "over-",
         "pan-",
         "para-",
         "peri-",
         "post-",
         "pre-",
         "pro-",
         "pseudo-",
         "quasi-",
         "re-",
         "semi-",
         "sub-",
         "super-",
         "tri-",
         "ultra-",
         "un-",
         "uni-",
         "vice-",
         // From email from Colin Warner <colinw@ldc.upenn.edu> on 7/25/2010
         "electro-",
         "gasto-",
         "homo-",
         "hetero-",
         "ortho-",
         "phospho-",
         };

   static private final String[] SUFFIXES = { "-esque", "-ette", "-fest", "-fold", "-gate", "-itis", "-less", "-most",
                                              "-o-torium", "-rama", "-wise" };

   static private final Set<String> PREFIX_SET = new HashSet<>( Arrays.asList( PREFIXES ) );
   static private final Set<String> SUFFIX_SET = new HashSet<>( Arrays.asList( SUFFIXES ) );

   static private Pattern WHITESPACE = Pattern.compile( "\\s+" );

   static private String getNextCharTerm( final String word ) {
      final StringBuilder sb = new StringBuilder();
      final int count = word.length();
      for ( int i = 0; i < count; i++ ) {
         final char c = word.charAt( i );
         if ( !Character.isLetterOrDigit( c ) ) {
            return sb.toString();
         }
         sb.append( c );
      }
      return sb.toString();
   }

   static private boolean isPrefix( final String word ) {
      final String prefixQ = word + "-";
      return PREFIX_SET.contains( prefixQ.toLowerCase() );
   }

   static private boolean isSuffix( final String word, final int startIndex ) {
      if ( word.length() <= startIndex ) {
         return false;
      }
      final String nextCharTerm = getNextCharTerm( word.substring( startIndex ) );
      if ( nextCharTerm.isEmpty() ) {
         return false;
      }
      final String suffixQ = "-" + nextCharTerm;
      return SUFFIX_SET.contains( suffixQ.toLowerCase() );
   }

   static private boolean isOwnerApostrophe( final CharSequence word, final int startIndex ) {
      return word.length() == startIndex + 1 && (word.charAt( startIndex ) == 's' || word.charAt( startIndex ) == 'S');
   }

   static private boolean isNumberDecimal( final CharSequence word, final int startIndex ) {
      // Bizarre scenario in which ctakes tokenizes ".2" as a fraction, but not ".22"
      return word.length() == startIndex + 1 && Character.isDigit( word.charAt( startIndex ) );
   }

   static public List<String> getTokens( final String word ) {
      return getTokens( word, false );
   }

   static public List<String> getTokens( final String word, final boolean separateDigits ) {
      final List<String> tokens = new ArrayList<>();
      final StringBuilder sb = new StringBuilder();
      final int count = word.length();
      boolean wasDigit = false;
      for ( int i = 0; i < count; i++ ) {
         final char c = word.charAt( i );
         if ( Character.isLetterOrDigit( c ) ) {
            if ( sb.length() != 0 && separateDigits && (wasDigit && !Character.isDigit( c )) ) {
               // separating characters from digits, add the current word
               tokens.add( sb.toString() );
               sb.setLength( 0 );
            }
            wasDigit = Character.isDigit( c );
            // Appending character to current word
            sb.append( c );
            continue;
         }
         wasDigit = false;
         if ( c == '-' && (isPrefix( sb.toString() ) || isSuffix( word, i + 1 )) ) {
            // what precedes is a prefix or what follows is a suffix so append the dash to the current word and move on
            sb.append( c );
            continue;
         }
         if ( (c == '\'' && isOwnerApostrophe( word, i + 1 ))
              || (c == '.' && isNumberDecimal( word, i + 1 )) ) {
            // what follows is an 's or .# so add the preceding and move on
            if ( sb.length() != 0 ) {
               tokens.add( sb.toString() );
               sb.setLength( 0 );
            }
            sb.append( c );
            continue;
         }
         // Wasn't a special symbol for consideration, so add the previous and symbol separately
         if ( sb.length() != 0 ) {
            tokens.add( sb.toString() );
            sb.setLength( 0 );
         }
         tokens.add( "" + c );
      }
      if ( sb.length() != 0 ) {
         // add the final word
         tokens.add( sb.toString() );
      }
      return tokens;
   }

   static public String getTokenizedText( final String text ) {
      return getTokenizedText( text, false );
   }


   static public String getTokenizedText( final String text, final boolean separateDigits ) {
      if ( text.isEmpty() ) {
         return text;
      }
//      final String[] splits = WHITESPACE.split( text.toLowerCase() );
      final String[] splits = WHITESPACE.split( text );
      if ( splits.length == 0 ) {
         return "";
      }
      final String lastSplit = splits[ splits.length - 1 ];
      if ( lastSplit.endsWith( "," ) || lastSplit.endsWith( ";" ) || lastSplit.endsWith( "." ) ) {
         // get rid of last comma or semicolon or period
         splits[ splits.length - 1 ] = lastSplit.substring( 0, lastSplit.length() - 1 );
      }
      return Arrays.stream( splits )
                   .map( s -> getTokens( s, separateDigits ) )
            .flatMap( Collection::stream )
            .collect( Collectors.joining( " " ) );
   }


}
