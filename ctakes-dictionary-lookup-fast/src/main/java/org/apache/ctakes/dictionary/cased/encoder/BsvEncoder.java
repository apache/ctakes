package org.apache.ctakes.dictionary.cased.encoder;


import org.apache.ctakes.dictionary.cased.util.bsv.BsvFileParser;
import org.apache.ctakes.dictionary.cased.util.bsv.StringArrayCreator;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.IOException;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class BsvEncoder implements TermEncoder {

   static public final String ENCODER_TYPE = "BSV";

   static private final Logger LOGGER = Logger.getLogger( "BsvEncoder" );


   private final InMemoryEncoder _delegate;

   public BsvEncoder( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_file", uimaContext ) );
   }

   public BsvEncoder( final String name, final String bsvPath ) {
      final Map<Long, Collection<TermEncoding>> encodingMap = parseBsvFile( name, bsvPath );
      _delegate = new InMemoryEncoder( name, encodingMap );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegate.getName();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<TermEncoding> getEncodings( final long cuiCode ) {
      return _delegate.getEncodings( cuiCode );
   }


   /**
    * Create a map of {@link TermEncoding} Objects
    * by parsing a bsv file.  The file should have a columnar format:
    * <p>
    * CUI|Code
    * </p>
    *
    * @param bsvFilePath path to file containing term rows and bsv columns
    * @return map of all cuis and codes read from the bsv file
    */
   static private Map<Long, Collection<TermEncoding>> parseBsvFile( final String name, final String bsvFilePath ) {
      final Collection<String[]> columnCollection = new HashSet<>();
      try {
         columnCollection.addAll( BsvFileParser.parseBsvFile( bsvFilePath, new StringArrayCreator( 2 ) ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      if ( columnCollection.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<Long, Collection<TermEncoding>> encodingMap = new HashMap<>();
      for ( String[] columns : columnCollection ) {
         final long cuiCode = CuiCodeUtil.getInstance().getCuiCode( columns[ 0 ] );
         final TermEncoding termEncoding = new TermEncoding( name, columns[ 1 ].trim() );
         encodingMap.computeIfAbsent( cuiCode, l -> new HashSet<>() ).add( termEncoding );
      }
      return encodingMap;
   }


}
