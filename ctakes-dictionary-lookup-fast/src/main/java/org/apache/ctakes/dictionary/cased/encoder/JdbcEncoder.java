package org.apache.ctakes.dictionary.cased.encoder;


import org.apache.ctakes.dictionary.cased.table.column.CodeType;
import org.apache.ctakes.dictionary.cased.table.column.SchemaCode;
import org.apache.ctakes.dictionary.cased.util.jdbc.JdbcUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.dictionary.cased.util.jdbc.JdbcUtil.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class JdbcEncoder implements TermEncoder {

   static public final String ENCODER_TYPE = "JDBC";

   static private final Logger LOGGER = Logger.getLogger( "JdbcEncoder" );

   private final String _name;
   private final PreparedStatement _selectCodeStatement;
   private final CodeType _codeType;


   public JdbcEncoder( final String name, final UimaContext uimaContext ) throws SQLException {
      this( name,
            getParameterValue( name, "driver", uimaContext, HSQL_DRIVER ),
            getParameterValue( name, "url", uimaContext, "" ),
            getParameterValue( name, "table", uimaContext, name.toUpperCase() ),
            getParameterValue( name, "user", uimaContext, DEFAULT_USER ),
            getParameterValue( name, "pass", uimaContext, DEFAULT_PASS ),
            getParameterValue( name, "class", uimaContext, CodeType.TEXT.name() ) );
   }

   /**
    * @param name       unique name for dictionary
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param tableName  -
    * @param jdbcUser   -
    * @param jdbcPass   -
    * @param codeType   -
    */
   public JdbcEncoder( final String name,
                       final String jdbcDriver,
                       final String jdbcUrl,
                       final String tableName,
                       final String jdbcUser,
                       final String jdbcPass,
                       final String codeType ) throws SQLException {
      _name = name;
      _selectCodeStatement = JdbcUtil.createPreparedStatement( name,
            jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, tableName, SchemaCode.CUI.name() );
      LOGGER.info( "Connected to " + name + " table " + tableName );
      _codeType = CodeType.getCodeType( codeType );
   }

   public String getName() {
      return _name;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<TermEncoding> getEncodings( final long cuiCode ) {
      switch ( _codeType ) {
         case TEXT:
            return getTextEncodings( cuiCode, SchemaCode.SCHEMA_CODE.getColumn() );
         case LONG:
            return getLongEncodings( cuiCode, SchemaCode.SCHEMA_CODE.getColumn() );
         case INT:
            return getIntEncodings( cuiCode, SchemaCode.SCHEMA_CODE.getColumn() );
         case TUI:
            return getTuiEncodings( cuiCode, SchemaCode.SCHEMA_CODE.getColumn() );
         case PREF_TEXT:
            return getPrefTextEncodings( cuiCode, SchemaCode.SCHEMA_CODE.getColumn() );
      }
      return getTextEncodings( cuiCode, SchemaCode.SCHEMA_CODE.getColumn() );
   }


   private Collection<TermEncoding> getTextEncodings( final long cuiCode, final int column ) {
      final List<TermEncoding> encodings = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectCodeStatement, cuiCode );
         final ResultSet resultSet = _selectCodeStatement.executeQuery();
         while ( resultSet.next() ) {
            encodings.add( new TermEncoding( getName(), resultSet.getString( column ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return encodings;
   }


   private Collection<TermEncoding> getLongEncodings( final long cuiCode, final int column ) {
      final List<TermEncoding> encodings = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectCodeStatement, cuiCode );
         final ResultSet resultSet = _selectCodeStatement.executeQuery();
         while ( resultSet.next() ) {
            encodings.add( new TermEncoding( getName(), resultSet.getLong( column ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return encodings;
   }


   private Collection<TermEncoding> getIntEncodings( final long cuiCode, final int column ) {
      final List<TermEncoding> encodings = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectCodeStatement, cuiCode );
         final ResultSet resultSet = _selectCodeStatement.executeQuery();
         while ( resultSet.next() ) {
            encodings.add( new TermEncoding( getName(), resultSet.getInt( column ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return encodings;
   }


   private Collection<TermEncoding> getTuiEncodings( final long cuiCode, final int column ) {
      final List<TermEncoding> encodings = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectCodeStatement, cuiCode );
         final ResultSet resultSet = _selectCodeStatement.executeQuery();
         while ( resultSet.next() ) {
            encodings.add( new TermEncoding( CodeSchema.TUI.name(), resultSet.getInt( column ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return encodings;
   }


   private Collection<TermEncoding> getPrefTextEncodings( final long cuiCode, final int column ) {
      final List<TermEncoding> encodings = new ArrayList<>();
      try {
         JdbcUtil.fillSelectCall( _selectCodeStatement, cuiCode );
         final ResultSet resultSet = _selectCodeStatement.executeQuery();
         while ( resultSet.next() ) {
            encodings.add( new TermEncoding( CodeSchema.PREFERRED_TEXT.name(), resultSet.getString( column ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return encodings;
   }


}
