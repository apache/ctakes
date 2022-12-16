package org.apache.ctakes.dictionary.cased.dictionary;


import org.apache.ctakes.dictionary.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.cased.lookup.LookupToken;

import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
final public class InMemoryDictionary implements CasedDictionary {

   private final String _name;

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<String, Collection<CandidateTerm>> _upperTermMap;
   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<String, Collection<CandidateTerm>> _mixedTermMap;
   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<String, Collection<CandidateTerm>> _lowerTermMap;

   /**
    * @param name         unique name for dictionary
    * @param upperTermMap Map with a case-sensitive Rare Word (tokens) as key, and RareWordTerm Collection as value
    * @param mixedTermMap Map with a case-sensitive Rare Word (tokens) as key, and RareWordTerm Collection as value
    * @param lowerTermMap Map with a lowercase Rare Word (tokens) as key, and RareWordTerm Collection as value
    */
   public InMemoryDictionary( final String name,
                              final Map<String, Collection<CandidateTerm>> upperTermMap,
                              final Map<String, Collection<CandidateTerm>> mixedTermMap,
                              final Map<String, Collection<CandidateTerm>> lowerTermMap ) {
      _name = name;
      _upperTermMap = upperTermMap;
      _mixedTermMap = mixedTermMap;
      _lowerTermMap = lowerTermMap;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CandidateTerm> getCandidateTerms( final LookupToken lookupToken ) {
      if ( lookupToken.isAllUpperCase() ) {
         final Collection<CandidateTerm> cased = _upperTermMap.get( lookupToken.getText() );
         if ( cased != null ) {
            return cased;
         }
      } else if ( !lookupToken.isAllLowerCase() ) {
         final Collection<CandidateTerm> mixed = _mixedTermMap.get( lookupToken.getText() );
         if ( mixed != null ) {
            return mixed;
         }
      }
      return _lowerTermMap.get( lookupToken.getLowerText() );
   }


}
