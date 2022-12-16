package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

/**
 * https://www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Lat {
   BAQ( "Basque" ),
   CHI( "Chinese" ),
   CZE( "Czech" ),
   DAN( "Danish" ),
   DUT( "Dutch" ),
   ENG( "English" ),
   EST( "Estonian" ),
   FIN( "Finnish" ),
   FRE( "French" ),
   GER( "German" ),
   GRE( "Greek" ),
   HEB( "Hebrew" ),
   HUN( "Hungarian" ),
   ITA( "Italian" ),
   JPN( "Japanese" ),
   KOR( "Korean" ),
   LAV( "Latvian" ),
   NOR( "Norwegian" ),
   POL( "Polish" ),
   POR( "Portuguese" ),
   RUS( "Russian" ),
   SCR( "Croatian" ),
   SPA( "Spanish" ),
   SWE( "Swedish" ),
   TUR( "Turkish" );


   private final String _name;

   Lat( String name ) {
      _name = name;
   }

   public String getName() {
      return _name;
   }

}
