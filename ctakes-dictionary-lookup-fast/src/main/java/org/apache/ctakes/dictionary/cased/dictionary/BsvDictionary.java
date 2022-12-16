package org.apache.ctakes.dictionary.cased.dictionary;


import org.apache.ctakes.dictionary.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.cased.lookup.LookupToken;
import org.apache.ctakes.dictionary.cased.util.bsv.BsvFileParser;
import org.apache.ctakes.dictionary.cased.util.bsv.BsvObjectCreator;
import org.apache.ctakes.dictionary.cased.util.tokenize.TokenizedTerm;
import org.apache.ctakes.dictionary.cased.util.tokenize.TokenizedTermMapper;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
final public class BsvDictionary implements CasedDictionary {

   static public final String DICTIONARY_TYPE = "BSV";

   static private final Logger LOGGER = Logger.getLogger( "BsvDictionary" );

   final private CasedDictionary _delegateDictionary;

   /**
    * @param name        unique name for dictionary
    * @param uimaContext -
    */
   public BsvDictionary( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_file", uimaContext ) );
   }

   /**
    * @param name    unique name for dictionary
    * @param bsvPath path to bsv file containing synonyms and cuis
    */
   public BsvDictionary( final String name, final String bsvPath ) {
      final Collection<TokenizedTerm> tokenizedTerms = parseBsvFile( bsvPath );
      final Map<String, Collection<CandidateTerm>> upperWordTermMap = new HashMap<>();
      final Map<String, Collection<CandidateTerm>> mixedWordTermMap = new HashMap<>();
      final Map<String, Collection<CandidateTerm>> lowerWordTermMap = new HashMap<>();
      TokenizedTermMapper.createTermMap( tokenizedTerms, upperWordTermMap, mixedWordTermMap, lowerWordTermMap );
      _delegateDictionary = new InMemoryDictionary( name, upperWordTermMap, mixedWordTermMap, lowerWordTermMap );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateDictionary.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CandidateTerm> getCandidateTerms( final LookupToken lookupToken ) {
      return _delegateDictionary.getCandidateTerms( lookupToken );
   }

   /**
    * Create a collection of {@link TokenizedTerm} Objects
    * by parsing a bsv file.  The file can be in one of two columnar formats:
    * <p>
    * CUI|Text
    * </p>
    *
    * @param bsvFilePath path to file containing term rows and bsv columns
    * @return collection of all valid terms read from the bsv file
    */
   static private Collection<TokenizedTerm> parseBsvFile( final String bsvFilePath ) {
      try {
         return BsvFileParser.parseBsvFile( bsvFilePath, new TokenizedTermCreator() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      return Collections.emptyList();
   }


   static private class TokenizedTermCreator implements BsvObjectCreator<TokenizedTerm> {
      public TokenizedTerm createBsvObject( final String[] columns ) {
         if ( columns.length != 2 ) {
            return null;
         }
         return new TokenizedTerm( columns[ 0 ].trim(), columns[ 1 ].trim() );
      }
   }


}

