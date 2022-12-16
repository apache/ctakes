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
package org.apache.ctakes.assertion.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PipeBitInfo(
      name = "I2B2 Challenge Reader",
      description = "Reads entities and their properties from file. ",
      role = PipeBitInfo.Role.SPECIAL,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class I2B2Challenge2010CollectionReader extends CollectionReader_ImplBase {

	public final static String PARAM_INPUTDIR = "inputDir";
	public final static String PARAM_FNMATCH = "fnMatch";
	File[] docs = null;
//	private String conDir = null;
	private String astDir = null;
	private String match = null;
	private boolean fnMatch = false;
	int index = 0;
	private String mEncoding = null;
//	Pattern conPatt = Pattern.compile("c=\"(.*)\" (\\d+):(\\d+) (\\d+):(\\d+)\\|\\|t=\"(.*)\"");
	Pattern astPatt = Pattern.compile("c=\"(.*)\" (\\d+):(\\d+) (\\d+):(\\d+)\\|\\|t=\"(.*)\"\\|\\|a=\"(.*)\"");
	Logger logger = Logger.getLogger(this.getClass());
	HashMap<String,String> conLocs = new HashMap<String, String>();
	
	
	@Override
	public void initialize() throws ResourceInitializationException {
		String inputDir = (String) getConfigParameterValue(PARAM_INPUTDIR);
		File docDir = new File(inputDir + File.separator + "txt");
		match = (String) getConfigParameterValue(PARAM_FNMATCH);
		if(match != null) fnMatch = true;
		if(docDir.exists() && docDir.isDirectory()){
			docs = docDir.listFiles(new FilenameFilter(){ public boolean accept(File dir, String name){ return (name.endsWith("txt") && (!fnMatch || name.contains(match)));}});
		} else {
			throw new ResourceInitializationException(new RuntimeException("Unable to get list of files within " + docDir.getAbsolutePath()));
		}
//		conDir = new String(inputDir + File.separator + "concept");
		astDir = new String(inputDir + File.separator + "ast");
	}
	
	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		Scanner scanner = null;
		JCas jcas;
		try{
			jcas = aCAS.getJCas();
		}catch(CASException e){
			throw new CollectionException(e);
		}
		HashMap<Point,Integer> word2char = new HashMap<Point, Integer>();
//		HashSet<String> negSet = new HashSet<String>();
//		HashSet<String> hypothSet = new HashSet<String>();
//		HashSet<String> possSet = new HashSet<String>();
//		HashSet<String> condSet = new HashSet<String>();
//		HashSet<String> nasSet = new HashSet<String>();
		
		File file = docs[index];
		String fn = file.getName();
		logger.info("Reading file: " + fn);
		fn = fn.substring(0, fn.lastIndexOf('.'));
		DocumentID docId = new DocumentID(jcas);
		docId.setDocumentID(fn);
		docId.addToIndexes();
		
		scanner = new Scanner(file);
		int lineNum = 1;
		int charNum = 0;
		while(scanner.hasNextLine()){
			String line = scanner.nextLine();
			String[] tokens = line.split(" ");
			for(int i = 0; i < tokens.length; i++){
				Point pair = new Point(lineNum,i);
				word2char.put(pair, charNum);
				charNum += tokens[i].length() + 1;
			}
			if(line.length() > 0){
				for(int i = 1; i < line.length(); i++){
					if(line.charAt(line.length()-i) == ' '){
						charNum++;
						if(i > 1){
							System.err.println(fn + "contains some weird lines.");
						}
					}else{
						break;
					}
				}
			}
			lineNum++;
		}
		
		FileInputStream fis = new FileInputStream(file);
		byte[] contents = new byte[(int)file.length()];
		fis.read(contents);
		jcas.setDocumentText(new String(contents));
		
		File astFile = new File(astDir + File.separator + fn + ".ast");
		if(astFile.exists()){
			scanner = new Scanner(astFile);
			while(scanner.hasNextLine()){
				String line = scanner.nextLine().trim();
				Matcher m = astPatt.matcher(line);
				if(m.matches()){
					//1 = word, 2 = start line, 3 = start word, 4 = end line, 5 = end word, 6 = sem type, 7 = assertion status
					Point pair = new Point(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
					if(word2char.containsKey(pair)){
						int charOffset = word2char.get(pair);
						int end = charOffset + m.group(1).length();
//						Entity entity = new Entity(jcas);
						EventMention mention = new EventMention(jcas, charOffset, end);

						// set default values...
						mention.setPolarity(CONST.NE_POLARITY_NEGATION_ABSENT);
						mention.setConditional(CONST.NE_CONDITIONAL_FALSE);
						mention.setUncertainty(CONST.NE_UNCERTAINTY_ABSENT);
						mention.setGeneric(CONST.NE_GENERIC_FALSE);
						mention.setSubject(CONST.ATTR_SUBJECT_PATIENT);

						// set non-default values. mappings follow MITRE's conventions (see AssertionAnalysisEngine)
						if(m.group(7).equals("absent")){
//							negSet.add(charOffset+"-"+end);
							mention.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
						}else if(m.group(7).equals("hypothetical")){
//							hypothSet.add(charOffset+"-"+end);
							mention.setConditional(CONST.NE_CONDITIONAL_TRUE);
						}else if(m.group(7).equals("possible")){
//							possSet.add(charOffset+"-"+end);
							mention.setUncertainty(CONST.NE_UNCERTAINTY_PRESENT);
						}else if(m.group(7).equals("associated_with_someone_else")){
//							nasSet.add(charOffset+"-"+end);
							mention.setSubject(CONST.ATTR_SUBJECT_FAMILY_MEMBER); // the most common non-patient case
						}else if(m.group(7).equals("conditional")){ // no good mapping.
////							condSet.add(charOffset+"-"+end);
//							mention.setConditional(true);
////						}else if(m.group(7).equals("present")){
////							presSet.add(charOffset+"-"+end);    // NOTE: There is no "present" setting, it is an inference from other things not being set.
						}
						mention.addToIndexes();
					}
				}
			}
		}
		
		index++;
		logger.info("Done reading file: " + fn);
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		if (docs==null) {
			throw new RuntimeException("docs == null");
		}
		return (index < docs.length);
	}

	@Override
	public Progress[] getProgress() {
		Progress p = new ProgressImpl(index, docs.length, Progress.ENTITIES);
		return new Progress[]{ p};
	}

	@Override
	public void close() throws IOException {
		
	}

}

