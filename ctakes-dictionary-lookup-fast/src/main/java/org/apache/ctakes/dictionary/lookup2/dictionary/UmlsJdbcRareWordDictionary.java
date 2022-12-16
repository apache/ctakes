package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.ctakes.dictionary.lookup2.util.UmlsUserApprover;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class UmlsJdbcRareWordDictionary implements RareWordDictionary {

   static private final Logger LOGGER = Logger.getLogger( "UmlsJdbcRareWordDictionary" );

   final private RareWordDictionary _delegateDictionary;


   public UmlsJdbcRareWordDictionary( final String name, final UimaContext uimaContext, final Properties properties )
         throws SQLException {
      final boolean isValidUser = UmlsUserApprover.getInstance().isValidUMLSUser( uimaContext, properties );
      if ( !isValidUser ) {
         throw new SQLException( "Invalid User for UMLS dictionary " + name );
      }
      _delegateDictionary = new JdbcRareWordDictionary( name, uimaContext, properties );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateDictionary.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
      return _delegateDictionary.getRareWordHits( fastLookupToken );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      return _delegateDictionary.getRareWordHits( rareWordText );
   }


}
