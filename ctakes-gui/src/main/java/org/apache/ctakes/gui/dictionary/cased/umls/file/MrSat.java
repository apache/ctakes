package org.apache.ctakes.gui.dictionary.cased.umls.file;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2018
 * <p>
 * Simple Concept, Term and String Attributes in MRSAT.RRF
 */
public enum MrSat {
   CUI,     // Unique identifier for concept
   LUI,     // Unique identifier for term
   SUI,     // Unique identifier for string
   METAUI,  // Metathesaurus asserted unique identifier
   STYPE,   // The name of the column in MRCONSO.RRF or MRREL.RRF that contains the identifier to which the attribute is attached
   CODE,    // Unique Identifier or code for string in source
   ATUI,    // Unique identifier for attribute
   SATUI,   // Source asserted attribute identifier
   ATN,     // Attribute name
   SAB,     // Source abbreviation
   ATV,     // Attribute value
   SUPPRESS,// Suppressible flag
   CVF;     // Content view flag


}
