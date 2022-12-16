package org.apache.ctakes.dictionary.lookup2.concept;


import java.util.Collection;
import java.util.Map;

/**
 * Term Attribute Repository used to lookup term attributes by the cui of the term
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
public interface ConceptFactory {

   /**
    * The Type identifier and Name are used to maintain a collection of dictionaries,
    * so the combination of Type and Name should be unique for each dictionary if possible.
    *
    * @return simple name for the dictionary
    */
   public String getName();

   /**
    * @param cuiCode concept unique identifier
    * @return the information about the concept that exists in the repository.
    */
   public Concept createConcept( final Long cuiCode );

   /**
    * @param cuiCodes concept unique identifiers
    * @return the information about the concepts that exist in the repository.
    */
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes );

}
