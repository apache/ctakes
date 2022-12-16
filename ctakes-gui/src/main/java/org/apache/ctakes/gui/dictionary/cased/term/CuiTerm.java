package org.apache.ctakes.gui.dictionary.cased.term;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.gui.dictionary.cased.Ranks;
import org.apache.ctakes.gui.dictionary.umls.VocabularyStore;
import org.apache.ctakes.gui.dictionary.util.TextTokenizer;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
@Immutable
final public class CuiTerm {

   static private final int MIN_SYNONYM_LENGTH = 2;
   static private final int MAX_SYNONYM_LENGTH = 79;
   static private final int MAX_SYNONYM_TOKENS = 5;

   private final long _cuiCode;

   private final Collection<SemanticTui> _semanticTuis = EnumSet.noneOf( SemanticTui.class );

   private final Map<String, Collection<String>> _tokenizedVocabTuis = new HashMap<>();
   private final Collection<ScoredText> _textScores = new HashSet<>();
   private final Map<String, Collection<String>> _schemaCodes = new HashMap<>();


   public CuiTerm( final long cuiCode ) {
      _cuiCode = cuiCode;
   }

//   public void addTui( final String tui ) {
//      addTui( SemanticTui.getTuiFromCode( tui ) );
//   }

   public void addTui( final SemanticTui semanticTui ) {
      _semanticTuis.add( semanticTui );
   }

   public void addSchemaCode( final String sab, final String code ) {
      if ( _schemaCodes.computeIfAbsent( sab, c -> new HashSet<>() ).add( code ) ) {
         VocabularyStore.getInstance().addVocabulary( sab, code );
      }
   }

   public Map<String, Collection<String>> getSchemaCodes() {
      return _schemaCodes;
   }


   public void addSynonym( final String text,
                           final String sab,
                           final Collection<SemanticTui> tuis,
                           final String ts,
                           final String stt,
                           final String tty ) {
      _textScores.add( new ScoredText( text, ts, stt, tty ) );
      final String tokenized = TextTokenizer.getTokenizedText( text );
      final String stripped = stripForm( tokenized );
      if ( !isDictionaryable( stripped ) ) {
         return;
      }
      final String rankCode = Ranks.getRankCode( sab, tty );
      _tokenizedVocabTuis.computeIfAbsent( maybeUncap( stripped, tuis ), s -> new HashSet<>() ).add( rankCode );
   }


   static private String stripForm( final String tokenized ) {
      return tokenized.contains( "_ _ _" ) ? "" : tokenized;
   }

   static private String replaceEnd( final String text, final String end ) {
      return text.toLowerCase().endsWith( end ) ? text.substring( 0, text.length() - end.length() ).trim() : text;
   }

   static private String replaceBegin( final String text, final String begin ) {
      return text.toLowerCase().startsWith( begin ) ? text.substring( begin.length() ).trim() : text;
   }

   static private boolean isTextValid( final String tokenized ) {
      final boolean absolutelyNot = tokenized.length() < MIN_SYNONYM_LENGTH
                                    || tokenized.length() > MAX_SYNONYM_LENGTH
                                    || StringUtil.fastSplit( tokenized, ' ' ).length > MAX_SYNONYM_TOKENS
                                    // Check for auto-created note form
//                                    || StringUtil.fastSplit( tokenized, '@' ).length > 2
                                    || tokenized.chars().noneMatch( Character::isAlphabetic )
                                    || (tokenized.length() == MIN_SYNONYM_LENGTH && tokenized.charAt( 0 ) == '(');
      return !absolutelyNot;
   }


   static private boolean isDictionaryable( final String tokenized ) {
      final boolean absolutelyNot = tokenized.length() < MIN_SYNONYM_LENGTH
                                    || tokenized.length() > MAX_SYNONYM_LENGTH
                                    || (StringUtil.fastSplit( tokenized, ' ' ).length > MAX_SYNONYM_TOKENS);
      if ( absolutelyNot ) {
         return false;
      }
      final boolean hasGarbage = tokenized.startsWith( "[" )
                                 || tokenized.contains( "#" )
                                 || tokenized.contains( "@" )
                                 || tokenized.contains( "&" )
                                 || tokenized.contains( ";" )
                                 || tokenized.contains( "\"" )
                                 || tokenized.endsWith( ")" )
                                 || tokenized.endsWith( "]" );
      return !hasGarbage;
   }

   /**
    * @return umls cui for the term
    */
   public long getCuiCode() {
      return _cuiCode;
   }

   public Collection<Integer> getTuis() {
      return _semanticTuis.stream()
                          .map( SemanticTui::getCode )
                          .collect( Collectors.toSet() );
   }

   private Collection<String> getTokenizedSynonyms() {
      return _tokenizedVocabTuis.keySet();
   }

   static private final Predicate<String> onlyCapped
         = t -> t.substring( 1 ).equals( t.substring( 1 ).toLowerCase() );


   static private final Collection<String> UNITS = new HashSet<>( Arrays.asList(
         "MG", "MG/MG", "ML", "mL", "MG/ML", "mg/mL", "ML/ML", "GM", "MCG", "MCG/ML", "mcg/mL", "BAU/ML",
         "MEQ", "MEQ/ML", "UNT", "UNT/MG", "UNT/ML", "unt/mL", "UNT/GM", "MG/ACTUAT", "MG/HR" ) );

   static private String uncapUnits( final String text ) {
      return UNITS.contains( text ) ? text.toLowerCase() : text;
   }

   static private String uncapNumUnits( final String text ) {
      int lastNum = -1;
      for ( char c : text.toCharArray() ) {
         if ( !Character.isDigit( c ) ) {
            break;
         }
         lastNum++;
      }
      if ( lastNum < 0 || lastNum > text.length() - 2 ) {
         return text;
      }
      final String remainder = text.substring( lastNum + 1 );
      return UNITS.contains( remainder ) ? text.toLowerCase() : text;
   }

   static private final Collection<String> OTHERS = new HashSet<>( Arrays.asList( "NOS", "USP", "(USP)" ) );

   static private String uncapOther( final String text ) {
      return OTHERS.contains( text ) ? text.toLowerCase() : text;
   }

   static private String uncapitalize( final String text ) {
      final String first = text.substring( 0, 1 ).toLowerCase();
      if ( text.length() == 1 ) {
         return first;
      }
      return first + text.substring( 1 );
   }

   static private final Collection<SemanticGroup> keepSingleCapTuis
         = EnumSet.of( SemanticGroup.DEVICE, SemanticGroup.TITLE, SemanticGroup.DRUG );

   static private String maybeUncap( final String tokenized, final Collection<SemanticTui> tuis ) {
      final String[] words = StringUtil.fastSplit( tokenized, ' ' );
      final String uncapped = Arrays.stream( words )
                                    .map( CuiTerm::uncapOther )
                                    .map( CuiTerm::uncapUnits )
                                    .map( CuiTerm::uncapNumUnits )
                                    .collect( Collectors.joining( " " ) );
      if ( uncapped.equals( tokenized.toLowerCase() ) ) {
         return tokenized.toLowerCase();
      }
      final String[] words2 = StringUtil.fastSplit( uncapped, ' ' );
      final boolean removeSingleCap = tuis.stream()
                                          .map( SemanticTui::getGroup )
                                          .noneMatch( SemanticGroup.DRUG::equals );
//                                          .noneMatch( keepSingleCapTuis::contains );
      if ( words2.length > 1 || removeSingleCap ) {
         final String uncapped2 = Arrays.stream( words2 )
                                        .map( CuiTerm::uncapitalize )
                                        .collect( Collectors.joining( " " ) );
         if ( uncapped2.equals( tokenized.toLowerCase() ) ) {
            return tokenized.toLowerCase();
         }
      }
      return tokenized;
   }


   public Collection<String> getUpperOnly() {
      final Collection<String> lowerOnly = getLowerOnly();
      final Collection<String> lowerMixed = getMixedOnly().stream()
                                                          .map( String::toLowerCase )
                                                          .collect( Collectors.toSet() );
      return getTokenizedSynonyms()
            .stream()
            .filter( t -> t.chars().noneMatch( Character::isLowerCase ) )
            .filter( t -> !lowerOnly.contains( t.toLowerCase() ) )
            .filter( t -> !lowerMixed.contains( t.toLowerCase() ) )
            .collect( Collectors.toSet() );
   }

   public Collection<String> getMixedOnly() {
      final Collection<String> lowerOnly = getLowerOnly();
      return getTokenizedSynonyms()
            .stream()
            .filter( t -> t.chars().anyMatch( Character::isUpperCase ) )
            .filter( t -> t.chars().anyMatch( Character::isLowerCase ) )
            .filter( t -> !lowerOnly.contains( t.toLowerCase() ) )
            .collect( Collectors.toSet() );
   }

   public Collection<String> getLowerOnly() {
      return getTokenizedSynonyms()
            .stream()
            .filter( t -> t.chars().noneMatch( Character::isUpperCase ) )
            .collect( Collectors.toSet() );
   }


   public String getPreferredText() {
      return _textScores.stream()
                        .max( prefScorer )
                        .map( ScoredText::getText )
                        .orElse( "" );
   }

   public int getInstances( final String text ) {
      return _tokenizedVocabTuis.getOrDefault( text, Collections.emptyList() ).size();
   }

   public int getRank( final String text ) {
      return _tokenizedVocabTuis.getOrDefault( text, Collections.emptyList() )
                                .stream()
                                .mapToInt( Ranks.getInstance()::getCodeRank )
                                .min()
                                .orElse( 0 );
   }


   static private final class ScoredText {
      private final String _text;
      private final int _tsScore;
      private final int _sttScore;
      private final int _ttyScore;
      private final int _lengthScore;
      private final int _wordCountScore;
      private final int _uppercaseScore;
      static private final Collection<String> GOOD_STT = Arrays.asList( "PF", "VC", "VO" );
      static private final Collection<String> GREAT_TTY = Arrays.asList( "PT", "PN" );
      static private final Collection<String> GOOD_TTY = Arrays.asList( "RXN_PT", "DN" );

      private ScoredText( final String text,
                          final String ts,
                          final String stt,
                          final String tty ) {
         _text = text;
         _tsScore = ts.equals( "P" ) ? 2 : 1;
         _sttScore = GOOD_STT.contains( stt ) ? 2 : 1;
//      score = upScore( ISPREF, "Y", score );  // It usually looks reversed.
//      score = upScore( ISPREF, "N", score, 2 );
         _ttyScore = GREAT_TTY.contains( tty ) ? 3 : (GOOD_TTY.contains( tty ) ? 2 : 1);
         _lengthScore = text.length();
         // Prefer fewer-word terms - this should be last in a comparison
         _wordCountScore = 10 - StringUtil.fastSplit( text, ' ' ).length;
         _uppercaseScore = Character.isUpperCase( text.charAt( 0 ) ) ? 1 : 0;

      }

      public String getText() {
         return _text;
      }

      public int getTsScore() {
         return _tsScore;
      }

      public int getSttScore() {
         return _sttScore;
      }

      public int getTtyScore() {
         return _ttyScore;
      }

      public int getLengthScore() {
         return _lengthScore;
      }

      public int getWordCountScore() {
         return _wordCountScore;
      }

      public int getUppercaseScore() {
         return _uppercaseScore;
      }
   }


   static private final Comparator<ScoredText> prefScorer
         = Comparator.comparingInt( ScoredText::getUppercaseScore )
                     .thenComparing( ScoredText::getTtyScore )
                     .thenComparingInt( ScoredText::getSttScore )
                     .thenComparingInt( ScoredText::getTsScore )
                     .thenComparingInt( ScoredText::getWordCountScore );


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return value instanceof CuiTerm && ((CuiTerm)value).getCuiCode() == getCuiCode();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return ((Long)_cuiCode).hashCode();
   }

}
