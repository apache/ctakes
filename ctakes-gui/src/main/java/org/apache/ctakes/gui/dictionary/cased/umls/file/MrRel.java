package org.apache.ctakes.gui.dictionary.cased.umls.file;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2018
 * <p>
 * Related Concepts in MRREL.RRF
 */
public enum MrRel {
   CUI1,    // Unique identifier for first concept
   AUI1,    // Unique identifier for first atom
   STYPE1,  // The name of the column in MRCONSO.RRF that contains the first identifier to which the relationship is attached
   REL,     // Relationship label
   CUI2,    // Unique identifier for second concept
   AUI2,    // Unique identifier for second atom
   STYPE2,  // The name of the column in MRCONSO.RRF that contains the second identifier to which the relationship is attached
   RELA,    // Additional relationship label   --- e.g. parent_of
   RUI,     // Unique identifier for relationship
   SRUI,    // Source attributed relationship identifier
   SAB,     // Source abbreviation              --- e.g. NCI
   SL,      // Source of relationship labels    --- e.g. NCI
   RG,      // Relationship group
   DIR,     // Source asserted directionality flag
   SUPPRESS,// Suppressible flag
   CVF;     // Content view flag


}
