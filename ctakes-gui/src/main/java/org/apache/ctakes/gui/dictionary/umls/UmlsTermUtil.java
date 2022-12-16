package org.apache.ctakes.gui.dictionary.umls;

import org.apache.ctakes.gui.dictionary.util.FileUtil;
import org.apache.ctakes.gui.dictionary.util.RareWordUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Contains all the methods used to parse individual text definitions of umls terms
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/16/14
 */
final public class UmlsTermUtil {


   private enum DATA_FILE {
      REMOVAL_PREFIX_TRIGGERS( "RemovalPrefixTriggers.txt" ),
      REMOVAL_SUFFIX_TRIGGERS( "RemovalSuffixTriggers.txt" ),
      REMOVAL_FUNCTION_TRIGGERS( "RemovalFunctionTriggers.txt" ),
      REMOVAL_COLON_TRIGGERS( "RemovalColonTriggers.txt" ),
      UNWANTED_PREFIXES( "UnwantedPrefixes.txt" ),
      UNWANTED_SUFFIXES( "UnwantedSuffixes.txt" ),
      UNWANTED_TEXTS( "UnwantedTexts.txt" ),
      MODIFIER_SUFFIXES( "ModifierSuffixes.txt" ),
      RIGHT_ABBREVIATIONS( "RightAbbreviations.txt" ),
      KEEP_PREFIX_TRIGGERS( "KeepPrefixTriggers.txt" );
      final private String __name;

      DATA_FILE( final String name ) {
         __name = name;
      }
   }

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );
   static private final Pattern AUTO_NOTE = Pattern.compile( "@" );

   static private String getDataPath( final String dataDir, final DATA_FILE dataFile ) {
      return dataDir + '/' + dataFile.__name;
   }

   final private Collection<String> _removalPrefixTriggers;
   final private Collection<String> _removalSuffixTriggers;
   final private Collection<String> _removalColonTriggers;
   final private Collection<String> _removalFunctionTriggers;
   final private Collection<String> _unwantedPrefixes;
   final private Collection<String> _unwantedSuffixes;
   final private Collection<String> _unwantedTexts;
   final private Collection<String> _modifierSuffixes;
   final private Collection<String> _abbreviations;
   final private Collection<String> _unwantedPosTexts;
   final private Collection<String> _keepPrefixTriggers;

   public UmlsTermUtil( final String dataDir ) {
      this( getDataPath( dataDir, DATA_FILE.REMOVAL_PREFIX_TRIGGERS ),
            getDataPath( dataDir, DATA_FILE.REMOVAL_SUFFIX_TRIGGERS ),
            getDataPath( dataDir, DATA_FILE.REMOVAL_COLON_TRIGGERS ),
            getDataPath( dataDir, DATA_FILE.REMOVAL_FUNCTION_TRIGGERS ),
            getDataPath( dataDir, DATA_FILE.UNWANTED_PREFIXES ),
            getDataPath( dataDir, DATA_FILE.UNWANTED_SUFFIXES ),
            getDataPath( dataDir, DATA_FILE.UNWANTED_TEXTS ),
            getDataPath( dataDir, DATA_FILE.MODIFIER_SUFFIXES ),
            getDataPath( dataDir, DATA_FILE.RIGHT_ABBREVIATIONS ),
            getDataPath( dataDir, DATA_FILE.KEEP_PREFIX_TRIGGERS ) );
   }

   public UmlsTermUtil( final String removalPrefixTriggersPath, final String removalSuffixTriggersPath,
                        final String removalColonTriggersPath, final String removalFunctionTriggersPath,
                        final String unwantedPrefixesPath, final String unwantedSuffixesPath,
                        final String unwantedTextsPath,
                        final String modifierSuffixesPath, final String abbreviationsPath,
                        final String keepPrefixTriggersPath ) {
      _removalPrefixTriggers = FileUtil.readOneColumn( removalPrefixTriggersPath, "term removal Prefix Triggers" );
      _removalSuffixTriggers = FileUtil.readOneColumn( removalSuffixTriggersPath, "term removal Suffix Triggers" );
      _removalColonTriggers = FileUtil.readOneColumn( removalColonTriggersPath, "term removal Colon Triggers" );
      _removalFunctionTriggers = FileUtil
            .readOneColumn( removalFunctionTriggersPath, "term removal Function Triggers" );
      _unwantedPrefixes = FileUtil.readOneColumn( unwantedPrefixesPath, "unwanted Prefixes" );
      _unwantedSuffixes = FileUtil.readOneColumn( unwantedSuffixesPath, "unwanted Suffixes" );
      _unwantedTexts = FileUtil.readOneColumn( unwantedTextsPath, "unwanted Texts" );
      _modifierSuffixes = FileUtil.readOneColumn( modifierSuffixesPath, "modifier Suffixes" );
      _abbreviations = FileUtil.readOneColumn( abbreviationsPath, "Abbreviations to expand" );
      _keepPrefixTriggers = FileUtil.readOneColumn( keepPrefixTriggersPath, "term keep Prefix Triggers" );
      _unwantedPosTexts = RareWordUtil.getUnwantedPosTexts();
   }

   public boolean isTextValid( final String text ) {
      if ( text.length() > 255 ) {
         return false;
      }
      if ( _keepPrefixTriggers.stream().anyMatch( text::startsWith ) ) {
         return true;
      }

      if ( text.startsWith( "fh " ) || text.startsWith( "no fh " )
           || text.startsWith( "family " ) || text.startsWith( "history " ) ) {
         return true;
      }
      // Check for illegal characters
      boolean haveChar = false;
      for ( int i = 0; i < text.length(); i++ ) {
         if ( text.charAt( i ) < ' ' || text.charAt( i ) > '~' ) {
            return false;
         }
         if ( !haveChar && Character.isAlphabetic( text.charAt( i ) ) ) {
            haveChar = true;
         }
      }
      if ( !haveChar ) {
         return false;
      }
      if ( text.length() == 3 && text.charAt( 0 ) == '(' ) {
         return false;
      }
      // Check for auto-created note form
      if ( AUTO_NOTE.split( text ).length > 2 ) {
         return false;
      }
      if ( _unwantedTexts.contains( text ) ) {
         return false;
      }
      if ( _unwantedPosTexts.contains( text ) ) {
         return false;
      }
      if ( _removalPrefixTriggers.stream().anyMatch( text::startsWith ) ) {
         return false;
      }
      if ( _removalSuffixTriggers.stream().anyMatch( text::endsWith ) ) {
         return false;
      }
      if ( _removalColonTriggers.stream().anyMatch( text::contains ) ) {
         return false;
      }
      return !_removalFunctionTriggers.stream().anyMatch( text::contains );
   }

   static public boolean isTextTooShort( final String text, final int minCharLength ) {
      return text.length() < minCharLength;
   }


   static public boolean isTextTooLong( final String text, final int maxCharLength,
                                        final int maxWordCount, final int maxSymCount ) {
      if ( text.length() > 255 ) {
         return true;
      }
      final String[] splits = WHITESPACE.split( text );
      int wordCount = 0;
      int symCount = 0;
      for ( String split : splits ) {
         if ( split.length() > maxCharLength ) {
            return true;
         }
         if ( split.length() > 2 ) {
            wordCount++;
         } else {
            symCount++;
         }
      }
      return wordCount > maxWordCount || symCount > maxSymCount;
   }


   public Collection<String> getFormattedTexts( final String strippedText, final boolean extractAbbreviations,
                                                final int minCharLength, final int maxCharLength,
                                                final int maxWordCount, final int maxSymCount ) {
      Collection<String> extractedTerms = Collections.emptySet();
      if ( extractAbbreviations ) {
         // add embedded abbreviations
         extractedTerms = extractAbbreviations( strippedText );
      }
      if ( extractedTerms.isEmpty() ) {
         extractedTerms = extractModifiers( strippedText );
      }
      if ( !extractedTerms.isEmpty() ) {
         extractedTerms.add( strippedText );
         return getFormattedTexts( getPluralTerms( getStrippedTexts( extractedTerms ) ), minCharLength, maxCharLength, maxWordCount, maxSymCount );
      }
      Collection<String> texts = new HashSet<>( 1 );
      texts.add( strippedText );
      return getFormattedTexts( getPluralTerms( getStrippedTexts( texts ) ), minCharLength, maxCharLength, maxWordCount, maxSymCount );
   }


   static private Collection<String> getFormattedTexts( final Collection<String> extractedTerms,
                                                        final int minCharLength, final int maxCharLength,
                                                        final int maxWordCount, final int maxSymCount ) {
      return extractedTerms.stream()
            .filter( t -> !isTextTooShort( t, minCharLength ) )
            .filter( t -> !isTextTooLong( t, maxCharLength, maxWordCount, maxSymCount ) )
            .collect( Collectors.toList() );
   }

   static private Collection<String> getPluralTerms( final Collection<String> texts ) {
      final Collection<String> plurals = texts.stream()
            .filter( t -> t.endsWith( "( s )" ) )
            .collect( Collectors.toList() );
      if ( plurals.isEmpty() ) {
         return texts;
      }
      texts.removeAll( plurals );
      final Consumer<String> addPlural = t -> {
         texts.add( t );
         texts.add( t + "s" );
      };
      plurals.stream()
            .map( t -> t.substring( 0, t.length() - 5 ) )
            .forEach( addPlural );
      return texts;
   }

   private Collection<String> getStrippedTexts( final Collection<String> texts ) {
      return texts.stream()
            .map( this::getStrippedText )
            .filter( t -> !t.isEmpty() )
            .collect( Collectors.toSet() );
   }

   public String getStrippedText( final String text ) {
      // remove form underlines
//      if ( text.contains( "_ _ _" ) ) {
//         final int lastParen = text.lastIndexOf( '(' );
//         final int lastDash = text.indexOf( "_ _ _" );
//         final int deleteIndex = Math.max( 0, Math.min( lastParen, lastDash ) );
//         if ( deleteIndex > 0 ) {
//            return getStrippedText( text.substring( 0, deleteIndex - 1 ).trim() );
//         }
//      }
      // remove unmatched parentheses, brackets, etc.
      //      if ( text.startsWith( "(" ) && !text.contains( ")" ) ) {
      //         return getStrippedText( text.substring( 1 ).trim() );
      //      }
      //      if ( text.startsWith( "[" ) && !text.contains( "]" ) ) {
      //         return getStrippedText( text.substring( 1 ).trim() );
      //      }
      //      if ( text.startsWith( "(" ) && text.endsWith( ") or" ) ) {
      //         return getStrippedText( text.substring( 1, text.length() - 4 ).trim() );
      //      }
      //      if ( text.startsWith( "or (" ) ) {
      //         return getStrippedText( text.substring( 2 ).trim() );
      //      }
      //      if ( text.startsWith( "\"" ) && text.endsWith( "\"" ) ) {
      //         return getStrippedText( text.substring( 1 ).trim() );
      //      }
      //      if ( text.startsWith( "(" ) && text.endsWith( ")" ) ) {
      //         return getStrippedText( text.substring( 1, text.length() - 2 ).trim() );
      //      }
      //      if ( text.startsWith( "[" ) && text.endsWith( "]" ) ) {
      //         return getStrippedText( text.substring( 1, text.length() - 2 ).trim() );
      //      }
      //      if ( text.startsWith( "&" ) ) {
      //         return getStrippedText( text.substring( 1 ).trim() );
      //      }
      //      if ( text.endsWith( "]" ) && !text.contains( "[" ) ) {
      //         return getStrippedText( text.substring( 0, text.length() - 2 ).trim() );
      //      }
      //      if ( text.endsWith( ")" ) && !text.contains( "(" ) ) {
      //         return getStrippedText( text.substring( 0, text.length() - 2 ).trim() );
      //      }
      String strippedText = text.trim();
      // Text in umls can have multiple suffixes and/or prefixes.  Stripping just once doesn't do the trick
      int lastLength = Integer.MAX_VALUE;
      while ( lastLength != strippedText.length() ) {
         lastLength = strippedText.length();
         for ( String prefix : _unwantedPrefixes ) {
            if ( strippedText.startsWith( prefix ) ) {
               strippedText = strippedText.substring( prefix.length() ).trim();
            }
         }
         for ( String suffix : _unwantedSuffixes ) {
            if ( strippedText.endsWith( suffix ) ) {
               strippedText = strippedText.substring( 0, strippedText.length() - suffix.length() ).trim();
            }
         }
         if ( !isTextValid( strippedText ) ) {
            return "";
         }
      }
      if ( strippedText.contains( "(" ) && strippedText.contains( "[" ) ) {
         return "";
      }
      return strippedText;
   }


   private Collection<String> extractAbbreviations( final String tokenizedText ) {
      for ( String abbreviation : _abbreviations ) {
         if ( tokenizedText.endsWith( abbreviation )
              && !tokenizedText.contains( ":" ) && !tokenizedText.contains( " of " )
              && !tokenizedText.contains( " for " ) ) {
            final String noAbbrTerm
                  = tokenizedText.substring( 0, tokenizedText.length() - abbreviation.length() ).trim();
            final String abbrTerm
                  = abbreviation.replace( ":", "" ).replace( "(", "" ).replace( ")", "" ).replace( "-", "" )
                  .replace( "[", "" ).replace( "]", "" ).replace( "&", "" ).trim();
            final Collection<String> extractedAbbreviations = new HashSet<>( 2 );
            if ( noAbbrTerm.length() < 255 ) {
               extractedAbbreviations.add( noAbbrTerm );
            }
            if ( abbrTerm.length() < 255 ) {
               extractedAbbreviations.add( abbrTerm );
            }
            return extractedAbbreviations;
         }
      }
      return Collections.emptyList();
   }

   private Collection<String> extractModifiers( final String tokenizedText ) {
      for ( String modifier : _modifierSuffixes ) {
         if ( tokenizedText.endsWith( modifier ) ) {
            final String mainText = tokenizedText.substring( 0, tokenizedText.length() - modifier.length() ).trim();
            final String modifierText
                  = modifier.replace( "(", "" ).replace( ")", "" ).replace( "-", "" ).replace( ",", "" ).trim();
            final Collection<String> modifiedTexts = new HashSet<>( 2 );
            modifiedTexts.add( tokenizedText );
            modifiedTexts.add( modifierText + " " + mainText );
            return modifiedTexts;
         }
      }
      return Collections.emptyList();
   }


}
