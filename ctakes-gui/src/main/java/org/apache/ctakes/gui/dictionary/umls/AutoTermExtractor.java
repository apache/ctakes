package org.apache.ctakes.gui.dictionary.umls;


import org.apache.log4j.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/27/2016
 */
final public class AutoTermExtractor {

   private AutoTermExtractor() {
   }

   static private final Logger LOGGER = Logger.getLogger( "AutoTermExtractor" );


//   private Collection<String> autoExtractAcronyms( final String tokenizedText ) {
//      final int dashIndex = tokenizedText.indexOf( '-' );
//      if ( dashIndex > 1 ) {
//         // have text ABC - DEF, check for acronym
//         final String acronym = tokenizedText.substring( 0, dashIndex - 1 ).trim();
//         if ( acronym.isEmpty() || acronym.length() > 8 || acronym.equals( "dose" ) ) {
//            return Collections.emptyList();
//         }
//         final String[] splits = acronym.split( "\\s+" );
//         if ( (splits.length == 1 && acronym.length() > 6) || splits.length > 2 ) {
//            return Collections.emptyList();
//         }
//         final String definition = tokenizedText.substring( dashIndex + 1 ).trim();
//         if ( definition.isEmpty() ) {
//            return Collections.emptyList();
//         }
//         if ( (acronym.charAt( 0 ) != definition.charAt( 0 ) && !definition.contains( "' s" )) ) {
//            return Collections.emptyList();
//         }
//         final String[] definitionSplits = definition.split( "\\s+" );
//         if ( acronym.length() != definitionSplits.length
//               || definitionSplits[definitionSplits.length - 1].charAt( 0 ) != acronym.charAt(
//               acronym.length() - 1 ) ) {
//            return Collections.emptyList();
//         }
//         final Collection<String> extractedAbbreviations = new HashSet<>( 2 );
//         extractedAbbreviations.add( acronym );
//         extractedAbbreviations.add( definition );
//         return extractedAbbreviations;
//      }
//      return Collections.emptyList();
//   }
//
//   private Collection<String> autoExtractColonBracketTerms( final String tokenizedText ) {
//      final int colonIndex = tokenizedText.indexOf( ':' );
//      if ( colonIndex < 0 ) {
//         return Collections.emptyList();
//      }
//      final int orIndex = tokenizedText.indexOf( "] or [" );
//      final int andOrIndex = tokenizedText.indexOf( "] & / or [" );
//      if ( Math.max( orIndex, andOrIndex ) < colonIndex ) {
//         return Collections.emptyList();
//      }
//      String splitter = "\\] or \\[";
//      if ( andOrIndex > 0 ) {
//         splitter = "\\] & / or \\[";
//      }
//      final Collection<String> extractedTerms = new HashSet<>( 2 );
//      final String thing = tokenizedText.substring( 0, colonIndex - 1 ).trim();
//      final String types = tokenizedText.substring( colonIndex + 1 ).trim();
//      final String[] splits = types.split( splitter );
//      for ( String split : splits ) {
//         split = trimBracketText( split );
//         if ( split.equals( "nos" ) || split.equals( "nec" ) || split.equals( "unspecified" )
//               || split.equals( "other" ) || split.isEmpty() ) {
//            extractedTerms.addAll( getFormattedTexts( thing ) );
//         } else {
//            extractedTerms.addAll( getFormattedTexts( split + " " + thing ) );
//            extractedTerms.addAll( getFormattedTexts( thing + " " + split ) );
//         }
//      }
//      return extractedTerms;
//   }
//
//   private Collection<String> autoExtractAndBracketTerms( final String tokenizedText ) {
//      final int andIndex = tokenizedText.indexOf( "( &" );
//      if ( andIndex < 0 || tokenizedText.indexOf( "] or [" ) < andIndex ) {
//         return Collections.emptyList();
//      }
//      final Collection<String> extractedTerms = new HashSet<>( 3 );
//      final String thing = tokenizedText.substring( 0, andIndex - 1 ).trim();
//      extractedTerms.add( thing );
//      final String types = tokenizedText.substring( andIndex + 3 ).trim();
//      final String[] splits = types.split( "\\] or \\[" );
//      for ( String split : splits ) {
//         split = trimBracketText( split );
//         extractedTerms.addAll( getFormattedTexts( split + " " + thing ) );
//         extractedTerms.addAll( getFormattedTexts( thing + " " + split ) );
//      }
//      return extractedTerms;
//   }
//
//   private Collection<String> autoExtractOrBracketTerms( final String tokenizedText ) {
//      if ( !tokenizedText.contains( "] or [" ) && !tokenizedText.contains( "] & / or [" ) ) {
//         return Collections.emptyList();
//      }
//      final int lastOf = tokenizedText.lastIndexOf( " of " );
//      if ( lastOf > tokenizedText.lastIndexOf( ']' ) ) {
//         final String ofTerm = tokenizedText.substring( lastOf ).trim();
//         final Collection<String> ofExtractions = autoExtractOrBracketTerms( tokenizedText.substring( 0,
//                                                                                                      lastOf ).trim() );
//         final Collection<String> ofTexts = new HashSet<>( ofExtractions.size() );
//         for ( String ofText : ofExtractions ) {
//            ofTexts.add( ofText + " " + ofTerm );
//         }
//         return ofTexts;
//      }
//      final Collection<String> extractedTerms = new HashSet<>( 2 );
//      String splitter = "\\] or \\[";
//      if ( tokenizedText.contains( "] & / or [" ) ) {
//         splitter = "\\] & / or \\[";
//      }
//      final String[] splits = tokenizedText.split( splitter );
//      for ( String split : splits ) {
//         split = trimBracketText( split );
//         if ( !split.equals( "operation" ) && !split.equals( "therapy" ) && !split.equals( "provision of" ) ) {
//            extractedTerms.addAll( getFormattedTexts( split ) );
//         }
//      }
//      return extractedTerms;
//   }
//
//   private Collection<String> autoExtractOrParaTerms( final String tokenizedText ) {
//      if ( !tokenizedText.contains( ") or (" ) && !tokenizedText.contains( ") & / or (" ) ) {
//         return Collections.emptyList();
//      }
//      final int lastOf = tokenizedText.lastIndexOf( " of " );
//      if ( lastOf > tokenizedText.lastIndexOf( ')' ) ) {
//         final String ofTerm = tokenizedText.substring( lastOf ).trim();
//         final Collection<String> ofExtractions = autoExtractOrBracketTerms( tokenizedText.substring( 0,
//                                                                                                      lastOf ).trim() );
//         final Collection<String> ofTexts = new HashSet<>( ofExtractions.size() );
//         for ( String ofText : ofExtractions ) {
//            ofTexts.add( ofText + " " + ofTerm );
//         }
//         return ofTexts;
//      }
//      final Collection<String> extractedTerms = new HashSet<>( 2 );
//      String splitter = "\\) or \\(";
//      if ( tokenizedText.contains( ") & / or (" ) ) {
//         splitter = "\\) & / or \\(";
//      }
//      final String[] splits = tokenizedText.split( splitter );
//      for ( String split : splits ) {
//         split = trimParaText( split );
//         if ( !split.equals( "operation" ) && !split.equals( "therapy" ) && !split.equals( "provision of" ) ) {
//            extractedTerms.addAll( getFormattedTexts( split ) );
//         }
//      }
//      return extractedTerms;
//   }
//
//   private Collection<String> autoExtractColonParaTerms( final String tokenizedText ) {
//      final int colonIndex = tokenizedText.indexOf( ':' );
//      if ( colonIndex < 0 || colonIndex > tokenizedText.indexOf( '(' ) ) {
//         return Collections.emptyList();
//      }
//      final int orIndex = tokenizedText.indexOf( ") or (" );
//      final int andOrIndex = tokenizedText.indexOf( ") & / or (" );
//      if ( Math.max( orIndex, andOrIndex ) < colonIndex ) {
//         return Collections.emptyList();
//      }
//      String splitter = "\\) or \\(";
//      if ( andOrIndex > 0 ) {
//         splitter = "\\) & / or \\(";
//      }
//      final Collection<String> extractedTerms = new HashSet<>( 2 );
//      final String thing = tokenizedText.substring( 0, colonIndex - 1 ).trim();
//      final String types = tokenizedText.substring( colonIndex + 1 ).trim();
//      final String[] splits = types.split( splitter );
//      for ( String split : splits ) {
//         split = trimParaText( split );
//         if ( split.equals( "nos" ) || split.equals( "nec" ) || split.equals( "unspecified" )
//               || split.equals( "other" ) || split.isEmpty() ) {
//            extractedTerms.addAll( getFormattedTexts( thing ) );
//         } else {
//            extractedTerms.addAll( getFormattedTexts( split + " " + thing ) );
//            extractedTerms.addAll( getFormattedTexts( thing + " " + split ) );
//         }
//      }
//      return extractedTerms;
//   }
//
//   private Collection<String> autoExtractAndOrOtherTerms( final String tokenizedText ) {
//      final int otherIndex = tokenizedText.indexOf( " & / or other " );
//      if ( otherIndex < 0 ) {
//         return Collections.emptyList();
//      }
//      final Collection<String> otherTexts = new HashSet<>( 2 );
//      otherTexts.add( tokenizedText.substring( 0, otherIndex ).trim() );
//      otherTexts.add( tokenizedText.substring( otherIndex + 14 ).trim() );
//      return otherTexts;
//   }
//
//   static private String trimParaText( String paraText ) {
//      if ( paraText.startsWith( "(" ) ) {
//         paraText = paraText.substring( 1 );
//      }
//      if ( paraText.endsWith( " nos " ) || paraText.endsWith( " nec " ) ) {
//         return paraText.substring( 0, paraText.length() - 4 ).trim();
//      } else if ( paraText.endsWith( ", unspecified " ) ) {
//         return paraText.substring( 0, paraText.length() - 14 ).trim();
//      } else if ( paraText.endsWith( " nos )" ) || paraText.endsWith( " nec )" ) ) {
//         return paraText.substring( 0, paraText.length() - 5 ).trim();
//      } else if ( paraText.endsWith( ", unspecified )" ) ) {
//         return paraText.substring( 0, paraText.length() - 15 ).trim();
//      } else if ( paraText.endsWith( ")" ) ) {
//         return paraText.substring( 0, paraText.length() - 1 ).trim();
//      }
//      return paraText.trim();
//   }
//
//   static private String trimBracketText( String bracketText ) {
//      if ( bracketText.startsWith( "[" ) ) {
//         bracketText = bracketText.substring( 1 );
//      }
//      if ( bracketText.endsWith( " nos " ) || bracketText.endsWith( " nec " ) ) {
//         return bracketText.substring( 0, bracketText.length() - 4 ).trim();
//      } else if ( bracketText.endsWith( ", unspecified " ) ) {
//         return bracketText.substring( 0, bracketText.length() - 14 ).trim();
//      } else if ( bracketText.endsWith( " nos ]" ) || bracketText.endsWith( " nec ]" ) ) {
//         return bracketText.substring( 0, bracketText.length() - 5 ).trim();
//      } else if ( bracketText.endsWith( ", unspecified ]" ) ) {
//         return bracketText.substring( 0, bracketText.length() - 15 ).trim();
//      } else if ( bracketText.endsWith( "]" ) ) {
//         return bracketText.substring( 0, bracketText.length() - 1 ).trim();
//      }
//      return bracketText.trim();
//   }

}
