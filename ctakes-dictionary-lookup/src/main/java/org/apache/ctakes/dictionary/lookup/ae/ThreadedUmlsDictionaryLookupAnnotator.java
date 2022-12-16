/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.core.ae.UmlsEnvironmentConfiguration;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * UIMA annotator that identified entities based on lookup.
 * Special implementation to pre bundle the UMLS SnowmedCT/RxNorm dictionaries
 * Performs a check for user's UMLS licence at init time via their RESTful API
 * User's will need to configure their UMLS username/password in their config
 * Date: 1/9/13
 */
public class ThreadedUmlsDictionaryLookupAnnotator extends ThreadedDictionaryLookupAnnotator {

   // TODO: use consistent variable names (_logger vs LOGGER vs logger)
   static final private Logger _logger = Logger.getLogger( ThreadedUmlsDictionaryLookupAnnotator.class );

   @Override
   public void initialize( final UimaContext aContext ) throws ResourceInitializationException {
      super.initialize( aContext );
      final String umlsAddress = EnvironmentVariable.getEnv(UmlsEnvironmentConfiguration.URL.toString(), aContext );
      final String umlsVendor = EnvironmentVariable.getEnv( UmlsEnvironmentConfiguration.VENDOR.toString(), aContext );
      final String umlsUser = EnvironmentVariable.getEnv( UmlsEnvironmentConfiguration.USER.toString(), aContext );
      final String umlsPassword = EnvironmentVariable.getEnv( UmlsEnvironmentConfiguration.PASSWORD.toString(), aContext );
      _logger.info( "Using " + UmlsEnvironmentConfiguration.URL + ": " + umlsAddress + ": " + umlsUser );
      if ( !isValidUMLSUser( umlsAddress, umlsVendor, umlsUser, umlsPassword ) ) {
         _logger.error( "Error: Invalid UMLS License.  " +
                        "A UMLS License is required to use the UMLS dictionary lookup. \n" +
                        "Error: You may request one at: https://uts.nlm.nih.gov/license.html \n" +
                        "Please verify your UMLS license settings in the " +
                        "DictionaryLookupAnnotatorUMLS.xml configuration." );
         throw new ResourceInitializationException( new Exception( "Failed to initilize.  Invalid UMLS License" ) );
      }
   }

   public static boolean isValidUMLSUser( final String umlsaddr, final String vendor,
                                          final String username, final String password ) {
      String data;
      try {
         data = URLEncoder.encode( "licenseCode", "UTF-8" ) + "=" + URLEncoder.encode( vendor, "UTF-8" );
         data += "&" + URLEncoder.encode( "user", "UTF-8" ) + "=" + URLEncoder.encode( username, "UTF-8" );
         data += "&" + URLEncoder.encode( "password", "UTF-8" ) + "=" + URLEncoder.encode( password, "UTF-8" );
      } catch ( UnsupportedEncodingException unseE ) {
         _logger.error( "Could not encode URL for " + username + " with vendor license " + vendor );
         return false;
      }
      try {
         final URL url = new URL( umlsaddr );
         final URLConnection connection = url.openConnection();
         connection.setDoOutput( true );
         try ( final OutputStreamWriter writer = new OutputStreamWriter( connection.getOutputStream() );
               final BufferedReader reader = new BufferedReader( new InputStreamReader( connection
                     .getInputStream() ) ) ) {

            writer.write( data );
            writer.flush();
            boolean result = false;

            String line;
            while ( (line = reader.readLine()) != null ) {
               final String trimline = line.trim();
               if ( trimline.isEmpty() ) {
                  break;
               }
               result = trimline.equalsIgnoreCase( "<Result>true</Result>" )
                        || trimline.equalsIgnoreCase( "<?xml version='1.0' encoding='UTF-8'?><Result>true</Result>" );
            }
            return result;
         }
      } catch ( IOException ioE ) {
         _logger.error( ioE.getMessage() );
         return false;
      }
   }

}
