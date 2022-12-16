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
package org.apache.ctakes.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;

public class ParamUtil {

	/**
	 * 
	 * @param parameterName
	 * @param uimaContext
	 * @return does not return null - but will return empty set if parameter is
	 *         optional or not set.
	 * @throws AnnotatorContextException
	 */
	public static Set<String> getStringParameterValuesSet(String parameterName, UimaContext uimaContext) {
		Set<String> returnValues = new HashSet<>();
		String[] strings = (String[]) uimaContext.getConfigParameterValue(parameterName);
		if (strings == null)
			return returnValues;

		for (int i = 0; i < strings.length; i++) {
			returnValues.add(strings[i]);
		}
		return returnValues;
	}

	public static Map<String, String> getStringParameterValuesMap(String parameterName,
			UimaContext uimaContext, String keyValueDelimiter) {
		String[] paramValues = (String[]) uimaContext.getConfigParameterValue(parameterName);
		return getStringParameterValuesMap(paramValues, keyValueDelimiter);
	}
	
	public static Map<String, String> getStringParameterValuesMap(String[] paramValues, String keyValueDelimiter){
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < paramValues.length; i++) {
      int delimiterIndex = paramValues[i].lastIndexOf(keyValueDelimiter);
      if (delimiterIndex == -1)
        continue;
      String key = paramValues[i].substring(0, delimiterIndex);
      String value = paramValues[i].substring(delimiterIndex + 1);
      map.put(key, value);
    }
    return map;
	}
}
