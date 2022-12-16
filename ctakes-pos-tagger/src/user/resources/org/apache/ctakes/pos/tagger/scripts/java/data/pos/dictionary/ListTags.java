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
package data.pos.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;


/**
 * From a POS corpus in OpenNLP format, create a list of the POS tags found within the corpus
 * <br>Outputs the list of tags to stdout, and for each tag, outputs one word/token that 
 * had been tagged with that tag
 * @author Mayo Clinic
 */

public class ListTags {

	/*  
	 * writes the list of tags to stdout, together with an example / a word found tagged with that tag
	 */
	private static void writeTagList(File f, HashMap<String, String> tagList) throws IOException {
		// sort them before outputting them
        ArrayList<String> list = new ArrayList<String>();
		for (Object key : tagList.keySet()) {
			list.add(key.toString());
		}
        Collections.sort(list);
        
        // output to stdout
        System.out.println("\nFor file " + f.getName() + ":");
        for (String s : list) {
    		System.out.println(s + "\t   which was a tag for '" + tagList.get(s)+ "'"); // output the tagList entry to stdout        	
        }
	}

	// Use a HashMap so we can keep an example, for each tag, of what was tagged with the tag
	private static HashMap<String, String> createTagList(BufferedReader br) throws IOException {
		HashMap<String, String> tagList;
		tagList = new HashMap<String, String> (100); // initial size is arbitrary
		String line;
		String tag;
		int pos; // position of last underscore
		String taggedThing;
		while((line = br.readLine()) != null) {
			for (String token : line.split(" ")) {
				pos = token.lastIndexOf('_');
				if (pos < 0) {
					System.err.println("ERROR: didn't find underscore within '" + token + "'");
				}
				taggedThing = token.substring(0, pos);
				tag = token.substring(pos+1);
				if (tagList.get(tag)==null) {
					tagList.put(tag, taggedThing);
				}
				else {
					//	System.out.println(tag + " already was seen for " + taggedThing);
				}	
			}
		}
		return tagList;
	}

	private static BufferedReader getBufferedReader(String filename) throws FileNotFoundException {
		File f = new File(filename);
		Reader r;
		try {
			r = new FileReader(f);
		} catch (FileNotFoundException e) {
			System.err.println("Error reading from file " + filename);
			throw e;
		}

		return new BufferedReader(r);		
	}


	/**
	 * Read a file containing POS-tagged tokens in OpenNLP format,
	 * and output to stdout the list of tags found<br>
	 *  Example input:
	 *  <br>body_NN
	 *  <br>winning_VBG<br>
	 *  <br>body_NN<br>
	 *  Example output:
	 *  <br>NN
	 *  <br>VBG
	 * @param args args[0] is required - the name of the input file containing 
	 * POS-tagged tokens in OpenNLP format.
	 * <br>E.g. data/pos/ptb-pos-training.txt
	 *  
	 */
	public static void main(String[] args) {

		if (args[0]==null || args[0].length()==0) {
			System.err.println("ERROR: corpus name required");
			return;
		}
		
		String arg0 = args[0].trim();
		if (arg0.equals("-h") || (arg0.equals("--help"))) {
			System.out.println("Usage: java ListTags <corpus-name>");
			System.out.println("  where <corpus-name> is something like   data/pos/ptb-pos-training.txt");
			System.out.println("Usage: java ListTags <directory>");
			System.out.println("  where <directory> is something like   data/pos/");
			return;
		}
		
		String inputPath = args[0];
		
		File f = new File(inputPath);
		File [] files; // list of files to process
		if (f.isDirectory()) { // directory name was input
			files = f.listFiles(); // process all within the dir
		}
		else { // name of a regular file was input
			files = new File[1];
			files[0] = f;
		}
		HashMap<String, String> tagList;
		
		try {
			for (File file : files) {
				if (file.isDirectory()) continue; // skip subdirectories
				if (file.getName().endsWith(".lnk")) continue; // skip shortcuts
				BufferedReader br = getBufferedReader(file.getAbsolutePath());
				tagList = createTagList(br);
				writeTagList(file, tagList);
			}
		} catch (IOException e) {
			System.err.println("Failed");
		}
    	
    }
	
}
