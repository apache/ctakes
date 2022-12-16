package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

/**
 * Term Status
 * https://www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Ts {
   P,  // Preferred LUI of the CUI
   S;  // Non-Preferred LUI of the CUI
//   p,  // Preferred LUI of the CUI, suppressible (only used in ORF MRCON)
//   s;  // Non-Preferred LUI of the CUI, suppressible (only used in ORF MRCON)
}
