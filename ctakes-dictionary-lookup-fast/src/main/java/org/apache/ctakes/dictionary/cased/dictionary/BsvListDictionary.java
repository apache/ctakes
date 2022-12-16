package org.apache.ctakes.dictionary.cased.dictionary;


import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.dictionary.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.cased.lookup.LookupToken;
import org.apache.ctakes.dictionary.cased.util.tokenize.TokenizedTerm;
import org.apache.ctakes.dictionary.cased.util.tokenize.TokenizedTermMapper;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.util.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
final public class BsvListDictionary implements CasedDictionary {

   static public final String DICTIONARY_TYPE = "BSV_LIST";

   static private final Logger LOGGER = Logger.getLogger( "BsvListDictionary" );

   final private CasedDictionary _delegateDictionary;

   /**
    * @param name        unique name for dictionary
    * @param uimaContext -
    */
   public BsvListDictionary( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_list", uimaContext ) );
   }

   /**
    * @param name    unique name for dictionary
    * @param bsvList list containing synonyms and cuis
    */
   public BsvListDictionary( final String name, final String bsvList ) {
      final Collection<TokenizedTerm> tokenizedTerms = parseList( name, bsvList );
      LOGGER.info( "Parsed " + tokenizedTerms.size() + " terms for dictionary " + name );
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
    * @param termList list containing synonyms and cuis
    * @return collection of all valid terms read from the bsv file
    */
   static private Collection<TokenizedTerm> parseList( final String name, final String termList ) {
      if ( termList.isEmpty() ) {
         LOGGER.error( "List of terms is empty for " + name );
         return Collections.emptyList();
      }
      final Collection<TokenizedTerm> tokenizedTerms = new HashSet<>();
      for ( String term : StringUtil.fastSplit( termList, '|' ) ) {
         final String[] keyValue = StringUtil.fastSplit( term, ':' );
         if ( keyValue.length != 2 ) {
            LOGGER.warn( "Improper Key : Value pair for Dictionary Term " + term );
            continue;
         }
         tokenizedTerms.add( new TokenizedTerm( keyValue[ 0 ].trim(), keyValue[ 1 ].trim() ) );
      }
      return tokenizedTerms;
   }


}

