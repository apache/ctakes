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
package org.apache.ctakes.dependency.parser.cr;
/*
 * Copyright: (c) 2010   Mayo Foundation for Medical Education and 
 * Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
 * triple-shield Mayo logo are trademarks and service marks of MFMER.
 *
 * Except as contained in the copyright notice above, or as used to identify 
 * MFMER as the author of this software, the trade names, trademarks, service
 * marks, or product names of the copyright holder shall not be used in
 * advertising, promotion or otherwise in connection with this software without
 * prior written authorization of the copyright holder.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * This collection reader reads in dependency tree training/test data in a tab-
 * delimited format. See note below for DEPENDENCY_FILE_PARAM for details about this
 * format. Each file will correspond to a "document" - i.e.
 * getNext() will populate the CAS with information from a file.
 */
@PipeBitInfo(
      name = "Dependency File Reader",
      description = "Reads in dependency tree training/test data in a tab-delimited format.",
      role = PipeBitInfo.Role.READER,
      products = { BASE_TOKEN, SENTENCE, DEPENDENCY_NODE }
)
public class DependencyFileCollectionReader extends CollectionReader_ImplBase {

	/**
	 * "DependencyFile" is a required, single, string parameter that specifies the
	 * location of a data file that contains dependency data in it. The
	 * format of the file should have one word per line, with tab-delimited features
	 * similar to the CONLL format.
	 */
	public static final String DEPENDENCY_FILE_PARAM = "DependencyFile";

	/**
	 * "InputFormat" is a optional, single, boolean parameter that determines
	 * whether or not the features associated with each word will be
	 * loaded into the CAS or not. The default value is false.
	 */
	public static final String INPUT_FORMAT_PARAM = "InputFormat";

	public static final String TRAINING_MODE_PARAM = "TrainingMode";

//	private static IntIntOpenHashMap formatMap = new IntIntOpenHashMap();
	
	BufferedReader input;
	String line = null;
	private int docCtr = 0;

	String inputFormat;
	boolean trainingMode = false;

	@Override
	public void initialize() throws ResourceInitializationException {
		try {
			String DependencyFile = (String) getConfigParameterValue(DEPENDENCY_FILE_PARAM);
			input = new BufferedReader(new FileReader(DependencyFile));
			String paramValue = (String) getConfigParameterValue(INPUT_FORMAT_PARAM);
			inputFormat = paramValue.toLowerCase();
			trainingMode = (Boolean) getConfigParameterValue(TRAINING_MODE_PARAM);

//			if (inputFormat.contains("choi")) {
//			    formatMap = DataLib.
//			}
			
		} catch (FileNotFoundException fnfe) {
			throw new ResourceInitializationException(fnfe);
		}
	}

	/**
	 * Some of the code in this method is based loosely on
	 * opennlp.tools.postag.POSEventCollector
	 */
	public void getNext(CAS cas) throws IOException, CollectionException {
	    try {
	        //if (hasNext()) {
	        JCas jCas = cas.getJCas();
	        int wordNumber = 0;
	        int sentNumber = 0;
	        int wordStart  = 0;
	        int wordEnd    = 0;
	        int sentStart  = 0;
	        int sentEnd    = 0;
	        ArrayList<String> lines = new ArrayList<String>(50);
	        StringBuffer documentText = new StringBuffer();

	        // First line
	        line = input.readLine();
	        if (line==null)
	            return;
	        else
	            lines.add(line);

	        while (true) {

	            // Read the line
	            line = input.readLine();
	            String testline = "";
	            if ( lines.size()>0 ) {
	                testline = lines.get(lines.size()-1);
	            }

	            // Check if document is done
	            if (line==null && testline.matches("\\A\\s*\\Z")) { 
	                jCas.setDocumentText(documentText.toString());
	                break;
	            }

	            // Process when sentence is done
	            else if (line.matches("\\A\\s*\\Z") || (line==null && !lines.get(lines.size()).matches("^\\s*$")) ) {

	                lines.trimToSize();
	                ArrayList<ConllDependencyNode> depNodes = new ArrayList<ConllDependencyNode>(lines.size());


                    /** Input for training: many formats, creates dependency nodes */
	                sentStart = wordStart;
	                sentEnd   = sentStart;
	                for (String aline : lines) {
	                    sentEnd += aline.split("\t")[1].length()+1;
	                }
	                if ( !inputFormat.contains("tok") ) {
	                    depNodes.add( new ConllDependencyNode(jCas,sentStart,sentEnd));
	                    depNodes.get(depNodes.size()-1).setId(0);
	                    depNodes.get(depNodes.size()-1).addToIndexes(jCas);
	                }

	                /** Create tokens */
	                if ( inputFormat.contains("tok") ) {
	                    for (String aline : lines) {                    
	                        String[] tokens = aline.split("\t");
	                        wordEnd = wordStart + tokens[1].length();
	                        BaseToken btoken = new BaseToken(jCas,wordStart,wordEnd);
	                        btoken.setTokenNumber(wordNumber++);
	                        btoken.addToIndexes();
	                        documentText.append(tokens[1] + " ");
	                        wordStart = wordEnd+1;
	                    }
	                } else if ( inputFormat.contains("min") ) {
	                    for (String aline : lines) {                    
	                        String[] tokens = aline.split("\t");
	                        wordEnd = wordStart + tokens[1].length();
	                        if (trainingMode)
	                        	depNodes.add( new ConllDependencyNode(jCas,wordStart,wordEnd) );
	                        BaseToken btoken = new BaseToken(jCas,wordStart,wordEnd);
	                        btoken.setTokenNumber(wordNumber++);
	                        btoken.addToIndexes();
	                        documentText.append(tokens[1] + " ");
	                        wordStart = wordEnd+1;
	                    }
	                } else if ( inputFormat.contains("mpos")) {
	                    for (String aline : lines) {                    
	                        String[] tokens = aline.split("\t");
	                        wordEnd = wordStart + tokens[1].length();
	                        if (trainingMode)
	                        	depNodes.add( new ConllDependencyNode(jCas,wordStart,wordEnd) );
	                        BaseToken btoken = new BaseToken(jCas,wordStart,wordEnd);
	                        btoken.setTokenNumber(wordNumber++);
	                        btoken.setPartOfSpeech(tokens[2]);
	                        btoken.addToIndexes();
	                        documentText.append(tokens[1] + " ");
	                        wordStart = wordEnd+1;
	                    }
	                } else if ( inputFormat.contains("mlem")) {
	                    for (String aline : lines) {                    
	                        String[] tokens = aline.split("\t");
	                        wordEnd = wordStart + tokens[1].length();
	                        if (trainingMode)
	                        	depNodes.add( new ConllDependencyNode(jCas,wordStart,wordEnd) );
	                        BaseToken btoken = new BaseToken(jCas,wordStart,wordEnd);
	                        btoken.setTokenNumber(wordNumber++);
	                        btoken.setNormalizedForm(tokens[2]);
	                        btoken.addToIndexes();
	                        documentText.append(tokens[1] + " ");
	                        wordStart = wordEnd+1;
	                    }
	                } else if ( inputFormat.contains("dep")) {
	                    for (String aline : lines) {                    
	                        String[] tokens = aline.split("\t");
	                        wordEnd = wordStart + tokens[1].length();
	                        if (trainingMode)
	                        	depNodes.add( new ConllDependencyNode(jCas,wordStart,wordEnd) );
	                        BaseToken btoken = new BaseToken(jCas,wordStart,wordEnd);
	                        btoken.setTokenNumber(wordNumber++);
	                        btoken.setNormalizedForm(tokens[2]);
	                        btoken.setPartOfSpeech(tokens[3]);
	                        btoken.addToIndexes();
	                        documentText.append(tokens[1] + " ");
	                        wordStart = wordEnd+1;
	                    }
	                } else { // CONLL format assumed
	                    if (!inputFormat.contains("conll")) { System.err.println("Warning: Assuming CONLL-x input format"); }
	                    for (String aline : lines) {                    
	                        String[] tokens = aline.split("\t");
	                        wordEnd = wordStart + tokens[1].length();
	                        if (trainingMode)
	                        	depNodes.add( new ConllDependencyNode(jCas,wordStart,wordEnd) );
	                        BaseToken btoken = new BaseToken(jCas,wordStart,wordEnd);
	                        btoken.setTokenNumber(wordNumber++);
	                        btoken.setNormalizedForm(tokens[2]);
	                        btoken.setPartOfSpeech(tokens[4]);
	                        btoken.addToIndexes();
	                        documentText.append(tokens[1] + " ");
	                        wordStart = wordEnd+1;
	                    }
	                }


	                Sentence sentence = new Sentence(jCas, sentStart, wordEnd);
	                sentence.setSentenceNumber(sentNumber);
	                sentence.addToIndexes();
	                
	                if (!inputFormat.contains("tok") && trainingMode) 
	                    setDependencyNodesFromTabbedText(jCas, lines, documentText, depNodes);

	                if (line==null) {
	                    jCas.setDocumentText(documentText.toString());
	                    break;
	                }
	                //wordNumber = 0;
	                //wordStart = 0;
	                //wordEnd = 0;
	                sentNumber++;
	                lines = new ArrayList<String>(50);
	            } else {
	                lines.add(line);
	            }
	        }
	        
	    } catch (CASException ce) {
	        throw new CollectionException(ce);
	    }
	    line=null;

	}

    /**
     * Store the dependency information in ConllDependencyNode-s
     * @param jCas
     * @param lines
     * @param documentText
     * @param depNodes
     */
    private void setDependencyNodesFromTabbedText(JCas jCas,
            ArrayList<String> lines, StringBuffer documentText,
            ArrayList<ConllDependencyNode> depNodes) {
        /** Store the dependency information in ConllDependencyNode-s */
        int i = 1;
        if ( inputFormat.contains("min") ) {
            for (String aline : lines) {                    
                String[] tokens = aline.split("\t");
                depNodes.get(i).setId(Integer.parseInt(tokens[0]));
                depNodes.get(i).setForm(tokens[1]);
                depNodes.get(i).setHead(depNodes.get(Integer.parseInt(tokens[2])));
                depNodes.get(i).setDeprel(tokens[3]);
                depNodes.get(i).setLemma("_");
                depNodes.get(i).setCpostag("_");
                depNodes.get(i).setPostag("_");
                depNodes.get(i).setFeats("_");
                depNodes.get(i).setPhead(null);
                depNodes.get(i).setPdeprel("_");
                depNodes.get(i).addToIndexes(jCas);
                i++;
            }
        } else if ( inputFormat.contains("mpos")) {
            for (String aline : lines) {                    
                String[] tokens = aline.split("\t");
                depNodes.get(i).setId(Integer.parseInt(tokens[0]));
                depNodes.get(i).setForm(tokens[1]);
                depNodes.get(i).setPostag(tokens[2]);
                depNodes.get(i).setCpostag(tokens[2]);
                depNodes.get(i).setHead(depNodes.get(Integer.parseInt(tokens[3])));
                depNodes.get(i).setDeprel(tokens[4]);
                depNodes.get(i).setLemma("_");
                depNodes.get(i).setFeats("_");
                depNodes.get(i).setPhead(null);
                depNodes.get(i).setPdeprel("_");
                depNodes.get(i).addToIndexes(jCas);
                i++;
            }
        } else if ( inputFormat.contains("mlem")) {
            for (String aline : lines) {                    
                String[] tokens = aline.split("\t");
                depNodes.get(i).setId(Integer.parseInt(tokens[0]));
                depNodes.get(i).setForm(tokens[1]);
                depNodes.get(i).setLemma(tokens[2]);
                depNodes.get(i).setHead(depNodes.get(Integer.parseInt(tokens[3])));
                depNodes.get(i).setDeprel(tokens[4]);
                depNodes.get(i).setCpostag("_");
                depNodes.get(i).setPostag("_");
                depNodes.get(i).setFeats("_");
                depNodes.get(i).setPhead(null);
                depNodes.get(i).setPdeprel("_");
                depNodes.get(i).addToIndexes(jCas);
                i++;
            }
        } else if ( inputFormat.contains("dep")) {
            for (String aline : lines) {                    
                String[] tokens = aline.split("\t");
                depNodes.get(i).setId(Integer.parseInt(tokens[0]));
                depNodes.get(i).setForm(tokens[1]);
                depNodes.get(i).setLemma(tokens[2]);
                depNodes.get(i).setPostag(tokens[3]);
                depNodes.get(i).setCpostag(tokens[3]);
                depNodes.get(i).setHead(depNodes.get(Integer.parseInt(tokens[4])));
                depNodes.get(i).setDeprel(tokens[5]);
                depNodes.get(i).setFeats("_");
                depNodes.get(i).setPhead(null);
                depNodes.get(i).setPdeprel("_");
                depNodes.get(i).addToIndexes(jCas);
                i++;
            }
        } else { // CONLL format assumed
            if (!inputFormat.contains("conll")) { System.err.println("Warning: Assuming CONLL-x input format"); }
            for (String aline : lines) {                    
                String[] tokens = aline.split("\t");
                depNodes.get(i).setId(Integer.parseInt(tokens[0]));
                depNodes.get(i).setForm(tokens[1]);
                depNodes.get(i).setLemma(tokens[2]);
                depNodes.get(i).setCpostag(tokens[3]);
                depNodes.get(i).setPostag(tokens[4]);
                depNodes.get(i).setFeats(tokens[5]);
                depNodes.get(i).setHead(depNodes.get(Integer.parseInt(tokens[6])));
                depNodes.get(i).setDeprel(tokens[7]);
                depNodes.get(i).setPhead(depNodes.get(Integer.parseInt(tokens[8])));
                depNodes.get(i).setPdeprel(tokens[9]);
                depNodes.get(i).addToIndexes(jCas);
                i++;
            }
        }
    }

	public void close() throws IOException {
		input.close();
	}

	public Progress[] getProgress() {
		return null;
	}

	public boolean hasNext() throws IOException, CollectionException {
	    /*
	    if (line == null) {
			line = input.readLine();
		}
		if (line == null)
			return false;
	     */
	    if (docCtr==0 || line != null) {
	        docCtr++;
	        return true;
	    }
	    else
	        return false;
	}

}
