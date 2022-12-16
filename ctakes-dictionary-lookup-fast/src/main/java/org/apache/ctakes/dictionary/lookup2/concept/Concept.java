package org.apache.ctakes.dictionary.lookup2.concept;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/21/2015
 */
public interface Concept {

   String TUI = "TUI";
   String PREFTERM = "PREFTERM";
   String PREFERRED_TERM_UNKNOWN = "Unknown Preferred Term";

   /**
    * @return umls unique identifier code (cui)
    */
   String getCui();

   /**
    * @return normalized preferred text
    */
   String getPreferredText();

   /**
    * @return secondary coding scheme names
    */
   Collection<String> getCodeNames();

   /**
    * @param codeType name of secondary coding scheme
    * @return all secondary (non-cui) codes for the named type
    */
   Collection<String> getCodes( String codeType );

   /**
    * @return the type of term that exists in the dictionary: Anatomical Site, Disease/Disorder, Drug, etc.
    */
   Collection<Integer> getCtakesSemantics();

   /**
    * @return true if this concept has no information other than cui and tui
    */
   boolean isEmpty();

}
