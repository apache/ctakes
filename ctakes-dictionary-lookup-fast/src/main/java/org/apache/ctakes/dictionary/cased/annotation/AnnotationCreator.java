package org.apache.ctakes.dictionary.cased.annotation;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.dictionary.cased.encoder.TermEncoding;
import org.apache.ctakes.dictionary.cased.lookup.DiscoveredTerm;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
public interface AnnotationCreator {

   void createAnnotations( final JCas jCas,
                           final Map<Pair<Integer>, Collection<DiscoveredTerm>> allDiscoveredTermsMap,
                           final Map<DiscoveredTerm, Collection<TermEncoding>> termEncodingMap,
                           final Map<SemanticTui, SemanticGroup> reassignSemantics );

}
