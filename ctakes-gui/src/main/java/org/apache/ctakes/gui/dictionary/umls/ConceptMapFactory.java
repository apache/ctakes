package org.apache.ctakes.gui.dictionary.umls;


import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/11/2015
 */
final public class ConceptMapFactory {

   static private final Logger LOGGER = Logger.getLogger( "ConceptMapFactory" );

   private ConceptMapFactory() {
   }

   static public Map<Long, Concept> createInitialConceptMap( final String umlsDirPath,
                                                             final Collection<String> wantedSources,
                                                             final Collection<SemanticTui> wantedTuis ) {
      if ( wantedSources.isEmpty() ) {
         LOGGER.warn( "No source vocabularies specified" );
         return Collections.emptyMap();
      }
      if ( wantedTuis.isEmpty() ) {
         LOGGER.warn( "No TUIs specified" );
         return Collections.emptyMap();
      }
      // get the valid Cuis for all wanted Tuis
      final Map<Long, Concept> concepts = MrstyParser.createConceptsForTuis( umlsDirPath, wantedTuis );
      // filter out the Cuis that do not belong to the given sources
      final Collection<Long> validVocabularyCuis = MrconsoParser.getValidVocabularyCuis( umlsDirPath, wantedSources );
      concepts.keySet().retainAll( validVocabularyCuis );
      LOGGER.info( "Total Valid Cuis " + concepts.size() + "\t from wanted Tuis and Vocabularies" );
      return concepts;
   }

}
