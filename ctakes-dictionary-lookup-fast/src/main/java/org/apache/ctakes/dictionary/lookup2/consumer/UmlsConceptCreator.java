package org.apache.ctakes.dictionary.lookup2.consumer;


import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/24/2015
 */
public interface UmlsConceptCreator {


   Collection<UmlsConcept> createUmlsConcepts( final JCas jcas, final String codingScheme,
                                               final String tui, final Concept concept );

}
