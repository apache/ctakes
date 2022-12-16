package org.apache.ctakes.dictionary.cased.dictionary;


import org.apache.ctakes.dictionary.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.cased.lookup.LookupToken;
import org.apache.ctakes.dictionary.cased.util.jdbc.JdbcUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.apache.ctakes.dictionary.cased.table.column.Synonym.*;
import static org.apache.ctakes.dictionary.cased.util.jdbc.JdbcUtil.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
final public class JdbcDictionary implements CasedDictionary {

   static public final String DICTIONARY_TYPE = "JDBC";

   static private final Logger LOGGER = Logger.getLogger( "JdbcDictionary" );

   static private final String snomed_rxnorm_2020aa_url
         = "jdbc:hsqldb:file:resources/org/apache/ctakes/dictionary/lookup/cased/sno_rx_2020aa/sno_rx_2020aa";
   static private final String snomed_rxnorm_2020aa_driver = "org.hsqldb.jdbcDriver";
   static private final String snomed_rxnorm_2020aa_user = "sa";
   static private final String snomed_rxnorm_2020aa_pass = "";

   private final String _name;


   private final PreparedStatement _selectUpperCall;
   private final PreparedStatement _selectMixedCall;
   private final PreparedStatement _selectLowerCall;


   /**
    * @param name        unique name for dictionary
    * @param uimaContext -
    */
   public JdbcDictionary( final String name, final UimaContext uimaContext ) throws SQLException {
      this( name,
            getParameterValue( name, "driver", uimaContext, HSQL_DRIVER ),
            getParameterValue( name, "url", uimaContext, "" ),
            getParameterValue( name, "upper", uimaContext, UPPER_TABLE ),
            getParameterValue( name, "mixed", uimaContext, MIXED_TABLE ),
            getParameterValue( name, "lower", uimaContext, LOWER_TABLE ),
            getParameterValue( name, "user", uimaContext, DEFAULT_USER ),
            getParameterValue( name, "pass", uimaContext, DEFAULT_PASS ) );
   }

   /**
    * @param name       unique name for dictionary
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param upperName  Name of table containing uppercase-only terms
    * @param mixedName  Name of table containing mixed case terms
    * @param lowerName  Name of table containing lowercase-only terms
    * @param jdbcUser   -
    * @param jdbcPass   -
    */
   public JdbcDictionary( final String name,
                          final String jdbcDriver,
                          final String jdbcUrl,
                          final String upperName,
                          final String mixedName,
                          final String lowerName,
                          final String jdbcUser,
                          final String jdbcPass ) throws SQLException {
      _name = name;
      _selectUpperCall = JdbcUtil.createPreparedStatement( name,
            jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, upperName, INDEX_WORD.name() );
      LOGGER.info( "Connected to " + name + " table " + upperName );
      _selectMixedCall = JdbcUtil.createPreparedStatement( name,
            jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, mixedName, INDEX_WORD.name() );
      LOGGER.info( "Connected to " + name + " table " + mixedName );
      _selectLowerCall = JdbcUtil.createPreparedStatement( name,
            jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, lowerName, INDEX_WORD.name() );
      LOGGER.info( "Connected to " + name + " table " + lowerName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<CandidateTerm> getCandidateTerms( final LookupToken lookupToken ) {
      final Collection<CandidateTerm> candidates = new HashSet<>();
      if ( lookupToken.isAllUpperCase() ) {
         candidates.addAll( getUpperTerms( lookupToken.getText() ) );
//         final Collection<CandidateTerm> cased = getUpperTerms( lookupToken.getText() );
//         if ( !cased.isEmpty() ) {
//            LOGGER.info( "Token " + lookupToken.getText() + " UPPER " + cased.stream()
//                                                                             .map( CandidateTerm::getTokens )
//                                                                             .map( t -> String.join( " ", t ) )
//                                                                             .collect( Collectors.joining( " ; " ) ) );
//            return cased;
//         }
      }
      if ( !lookupToken.isAllLowerCase() ) {
         candidates.addAll( getMixedTerms( lookupToken.getText() ) );
//         final Collection<CandidateTerm> mixed = getMixedTerms( lookupToken.getText() );
//         if ( !mixed.isEmpty() ) {
//            LOGGER.info( "Token " + lookupToken.getText() + " MIXED " + mixed.stream()
//                                                                             .map( CandidateTerm::getTokens )
//                                                                             .map( t -> String.join( " ", t ) )
//                                                                             .collect( Collectors.joining( " ; " ) ) );
//            return mixed;
//         }
      }
      candidates.addAll( getLowerTerms( lookupToken.getLowerText() ) );
//      final Collection<CandidateTerm> lower = getLowerTerms( lookupToken.getLowerText() );
//      if ( !lower.isEmpty() ) {
//         LOGGER.info( "Token " + lookupToken.getText() + " LOWER " + lower.stream()
//                                                                          .map( CandidateTerm::getTokens )
//                                                                          .map( t -> String.join( " ", t ) )
//                                                                          .collect( Collectors.joining( " ; " ) ) );
//         return lower;
//      }
//      LOGGER.info( "Token " + lookupToken.getText() + " NOTHING " );

      //      return getLowerTerms( lookupToken.getLowerText() );
      return candidates;
   }


   /**
    * @param text to lookup
    * @return uppercase candidate terms
    */
   public Collection<CandidateTerm> getUpperTerms( final String text ) {
      final List<CandidateTerm> candidateTerms = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectUpperCall, text );
         final ResultSet resultSet = _selectUpperCall.executeQuery();
         while ( resultSet.next() ) {
            final CandidateTerm candidateTerm = new CandidateTerm(
                  resultSet.getLong( CUI.getColumn() ),
                  resultSet.getString( PREFIX.getColumn() ),
                  resultSet.getString( INDEX_WORD.getColumn() ),
                  resultSet.getString( SUFFIX.getColumn() ),
                  true,
                  false,
                  resultSet.getInt( RANK.getColumn() ),
                  resultSet.getInt( INSTANCES.getColumn() ) );
            candidateTerms.add( candidateTerm );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return candidateTerms;
   }

   /**
    * @param text to lookup
    * @return mixed case candidate terms
    */
   public Collection<CandidateTerm> getMixedTerms( final String text ) {
      final List<CandidateTerm> candidateTerms = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectMixedCall, text );
         final ResultSet resultSet = _selectMixedCall.executeQuery();
         while ( resultSet.next() ) {
            final CandidateTerm candidateTerm = new CandidateTerm(
                  resultSet.getLong( CUI.getColumn() ),
                  resultSet.getString( PREFIX.getColumn() ),
                  resultSet.getString( INDEX_WORD.getColumn() ),
                  resultSet.getString( SUFFIX.getColumn() ),
                  false,
                  false,
                  resultSet.getInt( RANK.getColumn() ),
                  resultSet.getInt( INSTANCES.getColumn() ) );
            candidateTerms.add( candidateTerm );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return candidateTerms;
   }


   /**
    * @param text to lookup
    * @return lowercase candidate terms
    */
   public Collection<CandidateTerm> getLowerTerms( final String text ) {
      final List<CandidateTerm> candidateTerms = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectLowerCall, text );
         final ResultSet resultSet = _selectLowerCall.executeQuery();
         while ( resultSet.next() ) {
            final CandidateTerm candidateTerm = new CandidateTerm(
                  resultSet.getLong( CUI.getColumn() ),
                  resultSet.getString( PREFIX.getColumn() ),
                  resultSet.getString( INDEX_WORD.getColumn() ),
                  resultSet.getString( SUFFIX.getColumn() ),
                  false,
                  true,
                  resultSet.getInt( RANK.getColumn() ),
                  resultSet.getInt( INSTANCES.getColumn() ) );
            candidateTerms.add( candidateTerm );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return candidateTerms;
   }


}
