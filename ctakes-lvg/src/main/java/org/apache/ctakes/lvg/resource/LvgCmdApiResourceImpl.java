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
package org.apache.ctakes.lvg.resource;

import java.io.File;

import gov.nih.nlm.nls.lvg.Api.LvgCmdApi;
import gov.nih.nlm.nls.lvg.Api.LvgLexItemApi;

import org.apache.log4j.Logger;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

/**
 * Loads NLM Lvg and Norm, reading configuration information 
 * from a config (properties) file
 * 
 * Since the value of the LVG_DIR property in the lvg properties file  
 * must be either a hard coded absolute pathname or the value AUTO_MODE, 
 * and we want to support installing this project in a directory of the
 * user's choice, we avoid using a hardcoded pathname and choose to 
 * use LVG_DIR=AUTO_MODE in the properties file. 
 * AUTO_MODE indicates to lvg to look in the current working directory for
 * its files, so using AUTO_MODE requires this class to change the current 
 * working directory temporarily so the lvg files can be found by lvg.
 *
 * @author Mayo Clinic
 */
public class LvgCmdApiResourceImpl
        implements LvgCmdApiResource, SharedResourceObject
{
    // LOG4J logger based on class name
    private Logger logger = Logger.getLogger(getClass().getName());

    private LvgCmdApi lvg;
    private LvgLexItemApi lvgLexItem;
    
    private static String CWD_PROPERTY = "user.dir"; // Name of property for current working directory
    
    public void load(DataResource dr) throws ResourceInitializationException
    {
    	String configFileName = null;
    	String cwd = null;
    	try
    	{
    		
    		File configFile = new File(dr.getUrl().toExternalForm());
	        configFileName = configFile.getPath();
	         
	        logger.info("Loading NLM Norm and Lvg with config file = " + configFileName);
	        logger.info("  config file absolute path = " + configFile.getAbsolutePath());

	    	// Set the current working directory appropriately so the lvg files
	        // will be found if the lvg properties file contains LVG_DIR=AUTO_MODE
	        // If unable to change the current working directory, continue, so that
	        // if the properties file LVG_DIR value was changed to a hardcoded path,
	        // we allow that path to be used.
	    	String lvgDir = getLvgDir(configFile);
	    	cwd = getCurrentWorkingDirectory();
	    	if (cwd!=null) {
	    		logger.info("cwd = " + cwd);
	    		changeCurrentWorkingDirectory(lvgDir);
	    	}

	    	// See http://lexsrv2.nlm.nih.gov/SPECIALIST/Projects/ctakes-lvg/2008/docs/userDoc/index.html
	        // See http://lexsrv3.nlm.nih.gov/SPECIALIST/Projects/ctakes-lvg/2008/docs/designDoc/UDF/flow/index.html        
	        // Lower-case the terms and then uninflect
	        // f = using flow components (in this order) 
	        //     l = lower case
	        //     b = uninflect a term
	        lvg = new LvgCmdApi("-f:l:b", configFileName);
	        // Generate inflectional variants and get categories as strings rather than bit vectors
	        // f = using flow components (only one (i) used here) 
	        //     i = generate inflectional variants
	        // -SC = Show category names (returns the categories as strings rather than bit vectors)
	        lvgLexItem = new LvgLexItemApi("-f:i -SC", configFileName);

    	}
    	finally {
    		// try to change the current working directory back to what it was
			if (cwd!=null) {
				changeCurrentWorkingDirectory(cwd);
			}
			
	    }
    }

    private String getCurrentWorkingDirectory() {
    	String cwd = null;
    	try {
    		cwd = System.getProperty(CWD_PROPERTY);
    	} catch (SecurityException se) {
    		se.printStackTrace();
        	return cwd;
    	}
       	return cwd;
    }
    
    private boolean changeCurrentWorkingDirectory(String s) {
    	try {
    		System.setProperty(CWD_PROPERTY, s);
    		logger.info("cd " + s);
    	} catch (SecurityException se) {
    		se.printStackTrace();
    		return false;
    	}
    	return true;
    }
    
    
    /**
     * Get the path to the root directory of the installation of 
     * NLMs lvg lexical tools.
     * Note, does not look at the LVG_DIR properties in the lvg properties
     * file because LVG_DIR might not contain a path - 
     * LVG_DIR=AUTO_MODE. 
     * Assumes the lvg properties file is in data\config\.
     * For example, if LVG project is installed into   <br>
     *   /pipeline/LVG                                 <br>
     * with the lvg.properties file therefore in       <br>
     *   /pipeline/ctakes-lvg/resources/ctakes-lvg/data/config/      <br>
     * this method will return                         <br>
     *   /pipeline/ctakes-lvg/resources/lvg                   <br>
     *  
     */
    private String getLvgDir(File configFile) {
    	
    	String pathToLvgRoot = null;
    	File configDir = configFile.getParentFile();
    	String configDirAbsPath = configDir.getPath();
    	
    	// Use the path after stripping off data/config
    	// If path is not what was expected, try cwd as a last resort
    	String dataConfigPath = "data"+File.separator+"config";
    	if (!configDirAbsPath.endsWith(dataConfigPath)) {
    		pathToLvgRoot = getCurrentWorkingDirectory();
    	}
    	else {
    		int configDirAbsPathLen = configDirAbsPath.length();
    		int pathLen = dataConfigPath.length();
    		pathToLvgRoot = configDirAbsPath.substring(0, configDirAbsPathLen-pathLen);
    	}
    	
    	return pathToLvgRoot;
    }
    
    /** 
     * An LvgCmdApi takes a term from the input and returns a vector of strings. 
     * @see org.apache.ctakes.lvg.resource.LvgCmdApiResource#getLvg()
     */
    public LvgCmdApi getLvg() {
    	return lvg;
    }

    /**
     * The thing to run through Lvg
     */
	public LvgLexItemApi getLvgLex() {
		return lvgLexItem;
	}

}
