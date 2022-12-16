package org.apache.ctakes.dictionary.cased.dictionary;

import org.apache.ctakes.dictionary.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.cased.lookup.LookupToken;

import java.util.Collection;

/**
 * Dictionary used to lookup terms by the most rare word within them.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
public interface CasedDictionary {


   /**
    * The Type identifier and Name are used to maintain a collection of dictionaries,
    * so the combination of Type and Name should be unique for each dictionary if possible.
    *
    * @return simple name for the dictionary
    */
   String getName();

   /**
    * Any single token can exist in zero or more terms in the dictionary.  It may exist as its -own- form or as an
    * alternate canonical variant.  This method will check the dictionary for both.
    *
    * @param lookupToken a single-word token
    * @return zero or more terms that contain the lookup token
    */
   Collection<CandidateTerm> getCandidateTerms( final LookupToken lookupToken );

}
