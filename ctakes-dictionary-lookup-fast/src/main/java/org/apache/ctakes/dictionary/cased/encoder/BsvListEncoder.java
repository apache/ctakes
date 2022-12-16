package org.apache.ctakes.dictionary.cased.encoder;


import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class BsvListEncoder implements TermEncoder {

   static public final String ENCODER_TYPE = "BSV_LIST";

   static private final Logger LOGGER = Logger.getLogger( "BsvListEncoder" );


   private final InMemoryEncoder _delegate;

   public BsvListEncoder( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_list", uimaContext ) );
   }

   public BsvListEncoder( final String name, final String bsvList ) {
      final Map<Long, Collection<TermEncoding>> encodingMap = parseList( name, bsvList );
      LOGGER.info( "Parsed " + encodingMap.size() + " encodings for " + name );
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
    * @param bsvList path to file containing term rows and bsv columns
    * @return map of all cuis and codes read from the bsv file
    */
   static private Map<Long, Collection<TermEncoding>> parseList( final String name, final String bsvList ) {
      if ( bsvList.isEmpty() ) {
         LOGGER.error( "List of term encodings is empty for " + name );
         return Collections.emptyMap();
      }
      final Map<Long, Collection<TermEncoding>> encodingMap = new HashMap<>();
      for ( String encoding : StringUtil.fastSplit( bsvList, '|' ) ) {
         final String[] keyValue = StringUtil.fastSplit( encoding, ':' );
         if ( keyValue.length != 2 ) {
            LOGGER.warn( "Improper Key : Value pair for Term Encoding " + encoding );
            continue;
         }
         final long cuiCode = CuiCodeUtil.getInstance().getCuiCode( keyValue[ 0 ] );
         final TermEncoding termEncoding = new TermEncoding( name, keyValue[ 1 ].trim() );
         encodingMap.computeIfAbsent( cuiCode, l -> new HashSet<>() ).add( termEncoding );
      }
      return encodingMap;
   }


}
