package org.apache.ctakes.gui.dictionary.cased.umls.file;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2018
 * <p>
 * Concept names and sources in the MRCONSO.RRF
 */
public enum MrConso {
   CUI,     // Unique identifier for concept
   LAT,     // Language of Term(s)
   TS,      // Term status
   LUI,     // Unique identifier for term
   STT,     // String type
   SUI,     // Unique identifier for string
   ISPREF,  // Indicates whether AUI is preferred  --- oddly enough, N sometimes means Yes ...
   AUI,     // Unique identifier for atom
   SAUI,    // Source asserted atom identifier
   SCUI,    // Source asserted concept identifier
   SDUI,    // Source asserted descriptor identifier
   SAB,     // Source abbreviation
   TTY,     // Term type in source
   CODE,    // Unique Identifier or code for string in source
   STR,     // String
   SRL,     // Source Restriction Level
   SUPPRESS,// Suppressible flag                It looks like "O" means suppressible, "N" means not.
   CVF;     // Content view flag


}
