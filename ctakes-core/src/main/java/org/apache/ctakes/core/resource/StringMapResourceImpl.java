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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

/**
 * Implementation for StringIntegerMapResource interface.  
 * 
 * @author Mayo Clinic
 */
public class StringMapResourceImpl
        implements StringMapResource, SharedResourceObject
{
    // LOG4J logger based on class name
    private Logger iv_logger = Logger.getLogger(getClass().getName());

    private final String DELIMITER = "|";

    Map<String, String> iv_map = new HashMap<String, String>();

    /**
     *  Loads data from a file.
     */
    public void load(DataResource dr) throws ResourceInitializationException
    {
        iv_logger.info("Loading resource: "+dr.getUrl());
        try
        {
            InputStream inStream = dr.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
            int lineCount = 0;
            String line = br.readLine();
            while (line != null)
            {
                lineCount++;
                int delimiterIndex = line.indexOf(DELIMITER);
                if (delimiterIndex > 0) // we don't want the index to be -1 or 0
                {
                    String key = line.substring(0, delimiterIndex).trim();
                    String value = line.substring(delimiterIndex+1).trim();
                    iv_map.put(key, value);
                }
                else
                {
                    iv_logger.warn("Invalid resource line, character index of '"+DELIMITER+"' was "+delimiterIndex+" at line "+lineCount);
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
    public Map getMap()
    {
        return iv_map;
    }
}