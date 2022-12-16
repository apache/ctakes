package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

/**
 * https://www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Srl {
   SRL_0( "0" ),   // No additional restrictions; general terms of the license agreement apply.
   SRL_1( "1" ),   // General terms + additional restrictions in category 12.1
   SRL_2( "2" ),   // General terms + additional restrictions in category 12.2
   SRL_3( "3" ),   // General terms + additional restrictions in category 12.3
   SRL_4( "4" ),   // General terms + additional restrictions in category 12.4
   SRL_9( "9" );   // General terms + SNOMED CT Affiliate License in Appendix 2

   private final String _name;

   Srl( String name ) {
      _name = name;
   }

   public String getName() {
      return _name;
   }


}
