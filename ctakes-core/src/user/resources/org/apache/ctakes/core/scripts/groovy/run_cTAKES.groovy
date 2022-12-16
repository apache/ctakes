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
#!/usr/bin/env groovy

/**
** Sets the classpath before invoking the script that actually uses cTAKES.
** This includes (almost) all jars used by cTAKES as well as the resources
** and descriptors.
**
** 	This script assumes that you have installed Groovy and 
** 	that you have the command groovy available in your path.
**  This also assumes you have installed Apache cTAKES
**  from the convenience binary. 
**
**  Set the following below 
**    cTAKES_HOME (where you extracted cTAKES. This should be the parent of bin, desc, resources, etc)
**    EXTERNAL_RESOURCE_PATH (the parent of ctakes-resources-4.0.0, available from SF.net)
**
** 	On Debian/Ubuntu systems, installing Groovy should be as easy as apt-get install groovy.
** 	You can download groovy from http://groovy.codehaus.org/
**  Usage: $groovy run_cTAKES.groovy [inputDir]
**/

import java.io.File;

println("Starting " + this.class.getName());

// print out the classpath entries for debug purposes
//this.class.classLoader.rootLoader.URLs.each{ println it }

// TODO improve handling of whether user enters a trailing slash for these two constants:
def cTAKES_HOME = "/C:/Apache-cTAKES/apache-ctakes-4.0.0/";
cTAKES_HOME = "/C:/Apps/Apache-cTAKES/4.0.0-from-bin-zip/apache-ctakes-4.0.0/";
def EXTERNAL_RESOURCE_PATH = "/C:/parent-of-ctakes-resources";
EXTERNAL_RESOURCE_PATH = "/C:/usr/data/cTAKES-resources"; 

println("Using cTAKES in " + cTAKES_HOME);


// Add everything under cTAKES lib directory to classpath

File jarsDir = new File(cTAKES_HOME, "lib");
File[] files = jarsDir.listFiles();
//for (int i=files.length-1; i>=0; i--) {
for (int i=0; i<files.length; i++) {
	File f = files[i];
	if (f.getName().toLowerCase().endsWith(".jar")) {
		def path = f.getCanonicalPath();
		//println("this.class.classLoader = " + this.class.classLoader); //e.g.  this.class.classLoader = groovy.lang.GroovyClassLoader$InnerLoader@67ecd78
		//println("rootLoader = " + this.class.classLoader.rootLoader); // e.g.  rootLoader = org.codehaus.groovy.tools.RootLoader@58fe64b9
		
		// This is a total HACK: skipping the jars that start with "x".
		// Having a problem with groovy playing nice with some xml-processing jars/classes
		if (f.getName().startsWith("x")) {
			// HACK: Skip the "x" jars for now to avoid problem with xalan
		} else {
			if (path.startsWith("C:")) { // TODO generalize
				this.class.classLoader.rootLoader.addURL( new URL("file:///" + path));
			} else {
				this.class.classLoader.rootLoader.addURL( new URL("file://" + path));
			}
		}
	} else {
		println("Ignoring " + f.getName());
	}
}

//println("TODO -- consider having script download and unzip ctakes-resources-4.0.0.zip to lib");
//println("TODO -- download and unzip ctakes-resources-4.0.0.zip to lib");
// Add the ctakesresources (UMLS dictionary, LVG database) that are separately downloadable 
// from the Apache cTAKES code itself to the classpath before adding 
// the resources from within the cTAKES install directory, so these are picked up first
println("Adding ctakes-resources-4.0.0/resources to classpath");  // from ctakes-resources-4.0.0.zip
this.class.classLoader.rootLoader.addURL( new URL("file://" + EXTERNAL_RESOURCE_PATH + "/ctakes-resources-4.0.0/resources/") );


// Add cTAKES' resources directory to classpath
def subdir = "resources/";
println("Adding cTAKES subdir called " + subdir + " to classpath");
this.class.classLoader.rootLoader.addURL( new URL("file://" + cTAKES_HOME + subdir));

// Add cTAKES' desc directory to classpath
// Note, MUST end the URL with trailing slash or it doesn't seem to think it's a directory
// because AnalysisEngineFactory.createAnalysisEngineDescription won't be able to find things
// under this directory
subdir = "desc/";
println("Adding cTAKES subdir called " + subdir + " to classpath");
this.class.classLoader.rootLoader.addURL( new URL("file://" + cTAKES_HOME + subdir));


if (args.length < 1) {
	println("Please specify input directory");
	System.exit(1);
}
println("Input parm: " + args[0]);

// Run cTAKES now that the classpath has been set so that imports and resources are found
// Prepare to pass variable values to script
Binding binding = new Binding();
//binding.setVariable("cTAKES_HOME", cTAKES_HOME);
String arg0 = args[0];
String [] arguments = new String[2];
arguments[0] = arg0;
arguments[1] = cTAKES_HOME;
binding.setVariable("args", arguments);
GroovyShell shell = new GroovyShell(binding);

Object value = shell.evaluate(new File('cTAKES_clinical_pipeline.groovy'));

println("Done " + this.class.getName());
