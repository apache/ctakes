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
package org.apache.ctakes.core.resource;

import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Implementation for StringIntegerMapResource interface.  
 * 
 * @author Mayo Clinic
 */
public class StringIntegerMapResourceImpl
        implements StringIntegerMapResource, SharedResourceObject
{
    // LOG4J logger based on class name
    private Logger iv_logger = Logger.getLogger(getClass().getName());

    private final String DELIMITER = "|";

    Map<String, Integer> iv_map = new HashMap<String, Integer>();

    /**
     *  Loads data from a file.
     */
    public void load(DataResource dr) throws ResourceInitializationException
    {
        iv_logger.info("Loading resource: "+dr.getUrl());
        try
        {
            InputStream inStream = dr.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    inStream));
            int lineCount = 0;
            String line = br.readLine();
            while (line != null)
            {
                lineCount++;

                StringTokenizer st = new StringTokenizer(line, DELIMITER);
                if (st.countTokens() == 2)
                {
                    String key = st.nextToken();
                    try
                    {
                        Integer value = new Integer(st.nextToken());
                        iv_map.put(key, value);
                    }
                    catch (NumberFormatException nfe)
                    {
                        iv_logger.warn("Invalid resource line, expected integer: " + line);
                    }
                }
                else
                {
                    iv_logger.warn("Invalid resource line, expected 2 tokens only.");
                }

                line = br.readLine();
            }
            br.close();

            iv_logger.info("Loaded resource, # lines=" + lineCount);
        }
        catch (IOException ioe)
        {
            throw new ResourceInitializationException(ioe);
        }
    }

    /**
     * Gets a map of the String/Integer values.
     */
    public Map<String, Integer> getMap()
    {
        return iv_map;
    }
}