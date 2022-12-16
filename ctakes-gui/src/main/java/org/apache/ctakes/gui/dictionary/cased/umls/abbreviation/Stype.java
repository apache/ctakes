package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

/**
 * Column name in MRCONSO.RRF with identifier to which relationship is attached
 * https://www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Stype {
   AUI,   //Atom identifier
   CODE,   //Unique Identifier or code for string in source
   CUI,   //Concept unique identifier
   SCUI,   //Source asserted concept unique identifier
   SDUI;   //Source asserted descriptor identifier

}
