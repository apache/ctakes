package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

import java.util.Arrays;

/**
 * Relationship
 * https:// www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Rel {
   AQ,   // Allowed qualifier
   CHD,   // has child relationship in a Metathesaurus source vocabulary   --> If there is no relation name (Rela) then Rela ~ isa
   DEL( false ),   // Deleted concept
   PAR,   // has parent relationship in a Metathesaurus source vocabulary
   QB,   // can be qualified by
   RB,   // has a broader relationship
   RL( false ),   // the relationship is similar or alike. the two concepts are similar or "alike". In the current edition of the Metathesaurus, most relationships with this attribute are mappings provided by a source, named in SAB and SL; hence concepts linked by this relationship may be synonymous, i.e. self-referential: CUI1 = CUI2. In previous releases, some MeSH Supplementary Concept relationships were represented in this way.
   RN,   // has a narrower relationship
   RO,   // has relationship other than synonymous, narrower, or broader
   RQ( false ),   // related and possibly synonymous
   RU( false ),   // Related, unspecified
   SIB( false ),   // has sibling relationship in a Metathesaurus source vocabulary
   SY( false ),   // source asserted synonymy
   XR( false ),   // Not related, no mapping" );
   UNKNOWN( false );

   private final boolean _wanted;

   Rel() {
      this( true );
   }

   Rel( final boolean wanted ) {
      _wanted = wanted;
   }

   public boolean isWanted() {
      return _wanted;
   }

   static public Rel getRel( final String name ) {
      return Arrays.stream( values() )
                   .filter( s -> name.equalsIgnoreCase( s.name() ) )
                   .findFirst()
                   .orElse( UNKNOWN );
   }

   static public boolean isWanted( final String name ) {
      return getRel( name ).isWanted();
   }


}
