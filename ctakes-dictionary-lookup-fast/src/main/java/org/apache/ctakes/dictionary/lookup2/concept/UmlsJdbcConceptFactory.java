package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.dictionary.lookup2.util.UmlsUserApprover;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class UmlsJdbcConceptFactory implements ConceptFactory {

   static private final Logger LOGGER = Logger.getLogger( "UmlsJdbcConceptFactory" );

   final private ConceptFactory _delegateConceptFactory;


   public UmlsJdbcConceptFactory( final String name, final UimaContext uimaContext, final Properties properties )
         throws SQLException {
      final boolean isValidUser = UmlsUserApprover.getInstance().isValidUMLSUser( uimaContext, properties );
      if ( !isValidUser ) {
         throw new SQLException( "Invalid User for UMLS Concept Factory " + name );
      }
      _delegateConceptFactory = new JdbcConceptFactory( name, uimaContext, properties );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateConceptFactory.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      return _delegateConceptFactory.createConcept( cuiCode );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
      return _delegateConceptFactory.createConcepts( cuiCodes );
   }

}
