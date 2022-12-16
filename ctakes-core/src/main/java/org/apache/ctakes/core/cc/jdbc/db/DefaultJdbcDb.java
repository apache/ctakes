package org.apache.ctakes.core.cc.jdbc.db;


import org.apache.uima.resource.ResourceInitializationException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/18/2019
 */
final public class DefaultJdbcDb extends AbstractJdbcDb {

   public DefaultJdbcDb( final String driver,
                         final String url,
                         final String user,
                         final String pass,
                         final String keepAlive ) throws ResourceInitializationException {
      super( driver, url, user, pass, keepAlive );
   }

}
