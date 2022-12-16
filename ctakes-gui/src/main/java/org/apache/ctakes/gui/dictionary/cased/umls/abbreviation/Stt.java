package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

/**
 * String type
 * https://www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Stt {
   PF,   // Preferred form of term
   VCW,  // Case and word-order variant of the preferred form
   VC,   // Case variant of the preferred form
   VO,   // Variant of the preferred form
   VW;   // Word-order variant of the preferred form
}
