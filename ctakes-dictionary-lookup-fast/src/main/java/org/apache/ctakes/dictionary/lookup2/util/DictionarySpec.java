package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory;
import org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;

import java.util.Collection;

/**
 * Simple Container class that holds a {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary}
 * collection, a a {@link org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory}
 * and a {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/19/2014
 */
public interface DictionarySpec {
   /**
    * @return the available pair names
    */
   Collection<String> getPairNames();

   /**
    * @return the dictionary associated with the given pair name
    */
   RareWordDictionary getDictionary( String pairName );

   /**
    * @return the concept factory for concept creation associated with the given pair name
    */
   ConceptFactory getConceptFactory( String pairName );

   /**
    * @param conceptFactoryName name of a used concept factory
    * @return all dictionaries associated with the concept factory
    */
   Collection<RareWordDictionary> getPairedDictionaries( String conceptFactoryName );

   /**
    * @param dictionaryName name of a used dictionary
    * @return all concept factories associated with the dictionary
    */
   Collection<ConceptFactory> getPairedConceptFactories( String dictionaryName );

   /**
    * @return all known dictionaries
    */
   Collection<RareWordDictionary> getDictionaries();

   /**
    * @return all known concept factories
    */
   Collection<ConceptFactory> getConceptFactories();

   /**
    * @return the consumer to add terms to the Cas
    */
   TermConsumer getConsumer();
}
