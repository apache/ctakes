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
package org.apache.ctakes.postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * From a Part of Speech (POS) corpus in OpenNLP format, create a tagDictionary
 * <br>Outputs the dictionary to the specified file
 * @author Mayo Clinic
 */

public class TagDictionaryCreator {

	/*  
	 * Writes the dictionary entries to a PrintStream
	 * The tags are sorted for each entry, to make it easier to read, and also to make
	 * unit testing simpler
	 */
	private static void writeDictionary(Map<String, Set<String>> dict, PrintStream out) throws IOException {
		String line;
		for (Object key : dict.keySet()) {
			line = key.toString();
			// line = key.toString() + " " + dict.get(key);
			//ArrayList<String> sorted =  new ArrayList<String>(dict.get(key).size());
			String[] sorted = new String[dict.get(key).size()];
			sorted = dict.get(key).toArray(sorted);
			if (sorted!=null) java.util.Arrays.sort(sorted);
			// sorted = 
			for (String s : sorted) {
				line = line + " " + s;
			}
			out.println(line); // output the dictionary entry to the PrintStream
		}
	}

	/** 
	 * Create a tag dictionary from a POS corpus in OpenNLP format.
	 * @param br
	 * @param caseSensitive
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, Set<String>> createTagDictionary(BufferedReader br, boolean caseSensitive) throws IOException {
		HashMap<String, Set<String>> dict = new HashMap<String, Set<String>> (50000); // initial size is arbitrary
		String line;
		String word;
		String tag;
		int position; // position of last underscore
		Set<String> tagSet;
		while((line = br.readLine()) != null) {
			for (String token : line.split(" ")) {
				position = token.lastIndexOf('_');
				if (position == -1 || position == token.length()-1) {
					System.err.println("WARNING: '" + token + "' does not conform to the format WORD_TAG");
					continue;
				}
				word = token.substring(0, position);
				if(!caseSensitive)
					word = word.toLowerCase();
				tag = token.substring(position+1);
				tagSet = dict.get(word);
				if (tagSet==null) {
					tagSet = new HashSet<String>();
					tagSet.add(tag);
					dict.put(word, tagSet);
				}
				else {
					if (!tagSet.contains(tag)) { 
						tagSet.add(tag);
					}
					else {
					}
				}	
			}
		}
		return dict;
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
	 * and output a tagDictionary (OpenNLP format)<br>
	 *  Example input:
	 *  <br>winning_JJ body_NN
	 *  <br>winning_VBG<br>
	 *  Example output:
	 *  <br>body NN
	 *  <br>winning JJ VBG
	 * 
	 *  @see #printUsage()
	 */
	public static void main(String[] args) {

		boolean argumentsCorrect = true;
		if(args == null || args.length != 3)
			argumentsCorrect = false;
		for(String arg : args) {
			if(arg == null || arg.trim().length()==0)
				argumentsCorrect = false;
		}
		if (!argumentsCorrect) {
			System.err.println("ERROR: three non-empty arguments are required.");
			printUsage();
			return;
		}
		
		String arg0 = args[0].trim();
		if (arg0.equals("-h") || (arg0.equals("--help"))) {
			printUsage();
			return;
		}
		
		String trainingDataFn = args[0];
		String tagDictFn = args[1];
		String caseSensitiveArg = args[2];
		try {
			
			PrintStream out = new PrintStream(tagDictFn);
			boolean caseSensitive = Boolean.parseBoolean(caseSensitiveArg);
			Map<String, Set<String>> tagDictionary;
		
			BufferedReader br = getBufferedReader(trainingDataFn);
			tagDictionary = createTagDictionary(br, caseSensitive);
			writeDictionary(tagDictionary, out);
			out.flush();
			out.close();
			System.out.println("TagDictionary written to " + tagDictFn);
			
		} catch (IOException e) {
			System.err.println("TagDictionaryCreator Failed");
			System.err.println("training-data = " + trainingDataFn);
			System.err.println("tag-dictionary = " + tagDictFn);
			System.err.println("case-sensitive = " + caseSensitiveArg);
			System.err.flush();
			printUsage();
			File f = new File(args[0]);
			System.err.println("training-data absolute path = " + f.getAbsolutePath());
		}
    	
    }

	public static void printUsage() {

		System.out.println("Usage: java TagDictionaryCreator <training-data> <tag-dictionary> <case-sensitive>");
		System.out.println("  where <training-data> is a file prepared for training the part-of-speech tagger as described in data/pos/training/README");
		System.out.println("  where <tag-dictionary> is the output file where the tag dictionary will be written");
		System.out.println("  where <case-sensitive> is either 'true' or 'false' depending on whether the tag dictionary should be case sensitive or not.");
		
	}
	
}
