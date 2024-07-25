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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * @author Mayo Clinic
 */
public class FileResourceImpl implements FileResource, SharedResourceObject
{
    private File iv_file;
    static private final Logger LOGGER = LogManager.getLogger( "FileResourceImpl" );
    
    public void load(DataResource dr) throws ResourceInitializationException
    {
    	URI uri = dr.getUri();
    	if(uri != null)
    	{
    		if (uri.getScheme().equalsIgnoreCase("jar")) {
    			throw new ResourceInitializationException(new RuntimeException("Attempting to load a FileResource from a jar. The File to be loaded cannot be within a jar." + uri.toString()));
    		}
        	iv_file = new File(dr.getUri());
    	}
    	else
    	{
    		LOGGER.info("URI for data resource is null - using path from URL");
    		URL url = dr.getUrl();
    		if(url != null)
    		{
    	        String path = dr.getUrl().getPath();
	        	iv_file = new File(path);
    		}
    	}
    }

    public File getFile()
    {
        return iv_file;
    }    
}
