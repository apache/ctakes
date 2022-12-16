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
package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.core.pipeline.StandardCliOptions;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * Used to validate UMLS license / user.
 * <p/>
 * TODO Authentication before download would be nice, or perhaps an encrypted
 * download Author: SPF Affiliation: CHIP-NLP Date: 2/19/14
 * <p/>
 * UPDATED to use the API_KEY based authentication scheme of the UMLS For
 * maximum compatibility with existing configurations we permit umls_user =
 * "umls_api_key" and umls_password = "<APIKEY>" settings or simply set the
 * ctakes.umls_apikey system property pabramowitsch (11/2020)
 */
public enum UmlsUserApprover {

	INSTANCE;

	static public UmlsUserApprover getInstance() {
		return INSTANCE;
	}

	// properties, matches new
	public final static String OLD_USER_PARAM = "ctakes.umlsuser";
	public final static String OLD_PASS_PARAM = "ctakes.umlspw";

	public final static String URL_PARAM = "umlsUrl";
	public final static String USER_PARAM = "umlsUser";
	public final static String PASS_PARAM = "umlsPass";
	public final static String KEY_PARAM = "umlsKey";

	public final static String API_KEY_LABEL = "umls_api_key";
	public final static String OLDY_KEY_PROP = "ctakes.umls_apikey";

	static final private Logger LOGGER = Logger.getLogger( "UmlsUserApprover" );

	static final private String CHANGEME = "CHANGEME";
	static final private String CHANGE_ME = "CHANGE_ME";
	// forget about copies of this URL sprinkled around the other libraries
	static private final String OLD_USER_PASS_URL = "https://uts-ws.nlm.nih.gov/restful/isValidUMLSUser";
	static final private String UTS_APIKEY_URL = "https://utslogin.nlm.nih.gov/cas/v1/api-key";

	static private final String WHERE_DEFAULT = "Default Value";
	static private final String WHERE_ENV = "User Environment or Piper Variable";
	static private final String WHERE_XML = "Property Xml";

	static private final String NEW_AUTH_MESSAGE
			= "\n\n\n"
			  + "Starting 2021 NIH is using a new method for UMLS license authentication.  \n\n"
			  + "To use the UMLS dictionary you must obtain a UMLS API Key.  \n"
			  + "After obtaining a Key, there are several methods to utilize it with Apache cTAKES.  \n\n"
			  + "You may specify the value of your Key with a single parameter: \n"
			  + OLDY_KEY_PROP + " in your Operating System, \n"
			  + KEY_PARAM + " in your Operating System, \n"
			  + KEY_PARAM + " in your Piper File, \n"
			  + KEY_PARAM + " in your Dictionary Properties XML, \n"
			  + "--" + StandardCliOptions.UMLS_KEY + " in your Piper Runner command line, \n"
			  + "--" + StandardCliOptions.UMLS_KEY + " in your Clinical Pipeline Script command line, \n"
			  + "-D" + OLDY_KEY_PROP + " in your Java command parameters, or \n"
			  + "-D" + KEY_PARAM + " in your Java command parameters.  \n\n"
			  + "The single key settings above will be preferred over the pre-2021 [UserName , Password] method.  \n"
			  + "The pre-2021 [UserName , Password] method may still be used if the username is set to \n"
			  + API_KEY_LABEL + " and the password is set to the value of your Key.  \n\n"
			  + "For more information visit \n"
			  + "https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+4.0.0.1 \n"
			  + "https://uts.nlm.nih.gov/ \n\n\n";

	// cache of valid users
	static private final Collection<String> _validUsers = new ArrayList<>();


	/**
	 * validate the UMLS license / user
	 *
	 * @param uimaContext contains information about the UMLS license / user
	 * @param properties  possibly containing the attribs we need If not, we will
	 *                    look in the environment and sysprops.
	 * @return true if the server at umlsaddr approves of the vendor, user, password
	 * combination
	 */
	public boolean isValidUMLSUser( final UimaContext uimaContext, final Properties properties ) {
		final String apiUrl = getUrl( properties );
		final String umlsApiKey = getApiKey( uimaContext, properties );
		if ( umlsApiKey != null ) {
			return isValidUMLSUser( apiUrl, umlsApiKey );
		}
		// emulate U&P style
		final String user = getUser( uimaContext, properties );
		final String pass = getPassOrKey( uimaContext, properties );
		return isValidUMLSUser( apiUrl, null, user, pass );
	}

	/**
	 * validate the UMLS license / user Functionality overridden to deal with new
	 * UMLS API
	 *
	 * @param umlsUrl -
	 * @param vendor  IGNORED -
	 * @param user    NOW needs to be the value "umls_api_key" -
	 * @param apikey  THE API KEY -
	 * @return true if the server at umlsaddr approves of the vendor, user, password
	 * combination
	 */
	public boolean isValidUMLSUser( String umlsUrl, final String vendor, final String user, final String apikey ) {
		if ( !isValid( USER_PARAM, user ) || !user.equals( API_KEY_LABEL ) ) {
			LOGGER.error( NEW_AUTH_MESSAGE );
			return false;
		}
		if ( !isValid( PASS_PARAM, apikey ) || apikey.length() <= 24 ) {
			LOGGER.error( NEW_AUTH_MESSAGE );
			return false;
		}
		if ( _validUsers.contains( apikey ) ) {
			return true;
		}
		// last chance for an override
		if ( !isValid( umlsUrl ) ) {
			umlsUrl = getUrl();
			if ( !isValid( umlsUrl ) ) {
				umlsUrl = UTS_APIKEY_URL;
			}
		}
		return isValidUMLSUser( umlsUrl, apikey );
	}

	/**
	 * New UTS authentication method
	 *
	 * @param umlsUrl    -
	 * @param umlsApiKey -
	 * @return true if the umls api key can be authenticated
	 */
	static private boolean isValidUMLSUser( final String umlsUrl, final String umlsApiKey ) {
		if ( _validUsers.contains( umlsApiKey ) ) {
			return true;
		}
		return authenticate( umlsUrl, umlsApiKey );
	}

	/**
	 * @return the url to the authentication service
	 */
	static private String getUrl() {
		// get explicitly from the JVM in case any component
		// still has the old URL mentioned
		return getUrl( System.getProperties() );
	}

	/**
	 * @param properties properties from either the xml file or the user environment
	 * @return a user defined url or the default
	 */
	static private String getUrl( final Properties properties ) {
		String where = WHERE_ENV;
		String umlsUrl = EnvironmentVariable.getEnv( URL_PARAM );
		if ( !isValid( umlsUrl ) || umlsUrl.equals( OLD_USER_PASS_URL ) ) {
			umlsUrl = properties.getProperty( URL_PARAM );
			if ( isValid( umlsUrl ) && !umlsUrl.equals( OLD_USER_PASS_URL ) ) {
				where = WHERE_XML;
			}
		}
		if ( !isValid( umlsUrl ) || umlsUrl.equals( OLD_USER_PASS_URL ) ) {
			umlsUrl = UTS_APIKEY_URL;
			where = WHERE_DEFAULT;
		}
		LOGGER.debug( "Using umlsURL set using: " + where );
		return umlsUrl;
	}


	/**
	 * Check for parameter value in the uimacontext, environment and properties xml.
	 *
	 * @param uimaContext -
	 * @param properties  -
	 * @param name        -
	 * @param oldParam    the old environment variable style.  ctakes.umls{something}
	 * @param param       the new environment variable or piper file style.  umls{something}
	 * @return the obtained value or null
	 */
	static private String getParamValue( final UimaContext uimaContext, final Properties properties,
													 final String name,
													 final String oldParam, final String param ) {
		String where = WHERE_ENV + " " + param;
		String value = EnvironmentVariable.getEnv( param, uimaContext );
		if ( !isValid( value ) ) {
			value = properties.getProperty( param );
			if ( isValid( param, value ) ) {
				where = WHERE_XML + " " + param;
			} else {
				value = null;
			}
		}
		if ( value == null ) {
			value = EnvironmentVariable.getEnv( oldParam, uimaContext );
			if ( isValid( value ) ) {
				where = WHERE_ENV + " " + oldParam;
			}
		}
		if ( value != null ) {
			LOGGER.debug( name + " set using: " + where );
		}
		return value;
	}

	static private String getApiKey( final UimaContext uimaContext, final Properties properties ) {
		return getParamValue( uimaContext, properties, "UMLS API Key", OLDY_KEY_PROP, KEY_PARAM );
	}

	static private String getPassOrKey( final UimaContext uimaContext, final Properties properties ) {
		return getParamValue( uimaContext, properties, "UMLS API Key", OLD_PASS_PARAM, PASS_PARAM );
	}

	static private String getUser( final UimaContext uimaContext, final Properties properties ) {
		return getParamValue( uimaContext, properties, "UMLS User", OLD_USER_PARAM, USER_PARAM );
	}

	/**
	 * @param umlsUrl -
	 * @param apiKey  -
	 * @return true if the api key is valid according to the server at umlsUrl
	 */
	static private boolean authenticate( final String umlsUrl, String apiKey ) {
		try ( DotLogger dotter = new DotLogger() ) {
			apiKey = apiKey.trim();
			LOGGER.info( "Checking UMLS Account at " + umlsUrl + ":" );
			String data = "apikey=" + apiKey;
			final URL url = new URL( umlsUrl );
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod( "POST" );
			connection.setDoOutput( true );
			final OutputStreamWriter writer = new OutputStreamWriter( connection.getOutputStream() );
			writer.write( data );
			writer.flush();
			boolean isValidUser = false;
			final BufferedReader reader = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
			String line;
			while ( ( line = reader.readLine() ) != null ) {
				final String trimline = line.trim();
				if ( trimline.isEmpty() ) {
					break;
				}
			}
			// not used, but in case of problems we may want to see what is returned
			LOGGER.debug( "UTS response: " + line );
			writer.close();
			reader.close();
			// This method gets a ticket getting token. If it's successful, thats all we
			// need to know
			isValidUser = ( connection.getResponseCode() == HttpURLConnection.HTTP_CREATED );
			if ( isValidUser ) {
				LOGGER.info( "  UMLS Account has been validated" );
				_validUsers.add( apiKey );
			} else {
				LOGGER.error( "  UMLS Account at " + umlsUrl + " is not valid." );
				LOGGER.error( NEW_AUTH_MESSAGE );
			}
			return isValidUser;
		} catch ( IOException ioE ) {
			LOGGER.error( ioE.getMessage() );
			return false;
		}
	}

	/**
	 * used for unit testing
	 */
	public void resetUserCache() {
		_validUsers.clear();
	}

	/**
	 * @param value -
	 * @return true if the value is not null, not empty and not a "NOT_PRESENT" constant
	 */
	static private boolean isValid( final String value ) {
		return value != null
				 && !value.trim()
							 .isEmpty()
				 && !value.equals( EnvironmentVariable.NOT_PRESENT );
	}

	/**
	 * If the value is equal to "CHANGEME" or "CHANGE_ME" a message is logged telling the user to set a value.
	 *
	 * @param name  -
	 * @param value -
	 * @return true if the value is not null, not empty and not a "NOT_PRESENT" constant
	 */
	static private boolean isValid( final String name, final String value ) {
		if ( isValid( value ) ) {
			if ( value.trim()
						 .equals( CHANGEME ) || value.trim()
															  .equals( CHANGE_ME ) ) {
				// Potentially someone could have a user ID of CHANGEME or a password of CHANGEME but don't allow those
				// to make it easy for us to detect that the user or password was not set correctly.
				LOGGER.error( "  " + name + " " + value + " not allowed.  It is a placeholder reminder." );
				return false;
			}
			return true;
		}
		return false;
	}


}
