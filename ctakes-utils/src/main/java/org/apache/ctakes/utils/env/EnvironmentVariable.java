/**
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
package org.apache.ctakes.utils.env;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.uima.UimaContext;

@Immutable
final public class EnvironmentVariable {

   private EnvironmentVariable() {}

   // TODO never return null unless there is a great reason.  Refactor to non-null NOT_PRESENT
//   static public final String NOT_PRESENT = "EnvironmentVariable.NOT_PRESENT";
   static public final String NOT_PRESENT = null;


   /**
    * Get the value of some variable in the full (os, user, java, ctakes) environment.
    * Will attempt to get it from System Properties, then Environment, then uima context.
	 * @param name some variable name
	 * @param context -
	 * @return value for given name or {@link #NOT_PRESENT} (null) if name is null or empty or not found.
	 */
	public static String getEnv(final String name, final UimaContext context) {
      if ( name == null || name.trim().isEmpty() ) {
         return NOT_PRESENT;
      }
      //Attempt to get it from system properites, env variables
		String value = getEnv(name);
      if ( value == null && context != null ) {
         // Attempt to get it from UIMA Context
         value = (String) context.getConfigParameterValue(name);
      }
      return value != null ? value : NOT_PRESENT;
	}

   /**
    * Get the value of some variable in the full (os, user, java, ctakes) environment.
    * Will attempt to get it from System Properties, then Environment.
    * @param name some variable name
    * @return value for given name or {@link #NOT_PRESENT} (null) if name is null or empty or not found.
    */
	public static String getEnv(final String name) {
      if ( name == null || name.trim().isEmpty() ) {
         return NOT_PRESENT;
      }
      // Attempt to get it from System Properties
      String value = System.getProperty(name);
      if (value == null) {
         // Attempt ot get it from Env Variables
         value = System.getenv(name);
      }
      // Setting an environment variable with a dot is difficult or impossible in some shells
      // Check for an environment variable similarly named but with underscore separators
      if ( value == null && name.indexOf( '.' ) >= 0 ) {
         value = getEnv( name.replace( '.', '_' ) );
      }
      return value != null ? value : NOT_PRESENT;
	}
}
