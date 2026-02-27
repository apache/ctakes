package org.apache.ctakes.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author SPF , chip-nlp
 * @since {1/12/2024}
 */
final public class AeParamUtil {

    static private final Logger LOGGER = LoggerFactory.getLogger( "AeParamUtil" );

    static public boolean isTrue( final String value ) {
        return value.equalsIgnoreCase( "TRUE" ) || value.equalsIgnoreCase( "YES" );
    }

    static public boolean isFalse( final String value ) {
        return value.equalsIgnoreCase( "FALSE" ) || value.equalsIgnoreCase( "NO" );
    }

    static public double parseDouble( final String value ) {
        try {
            return Double.parseDouble( value );
        } catch ( NumberFormatException nfE ) {
            LOGGER.warn( "Could not parse " + value + " as a double.  Using 0." );
        }
        return 0;
    }

    static public int parseInt( final String value ) {
        try {
            return Integer.parseInt(value);
        } catch ( NumberFormatException nfE ) {
            LOGGER.warn( "Could not parse " + value + " as an integer.  Using 0." );
        }
        return 0;
    }

}
