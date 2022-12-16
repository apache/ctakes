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
package org.apache.ctakes.smokingstatus.ae;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;


import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.smokingstatus.Const;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.smokingstatus.type.libsvm.NominalAttributeValue;

public class PcsClassifierAnnotator_libsvm extends JCasAnnotator_ImplBase {
	Set<String> stopWords;
	List<String> goWords;
	boolean caseSensitive = true;
	Map<?, ?> tokenCounts;
	svm_model model; // trained libsvm model

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		tokenCounts = new HashMap();
		stopWords = new HashSet<String>();
		goWords = new ArrayList<String>();

		try {
			Object paramValue = aContext
					.getConfigParameterValue("CaseSensitive");
			if (paramValue != null)
				caseSensitive = ((Boolean) paramValue).booleanValue();
			FileResource stopWordsFileName = (FileResource) aContext
					.getResourceObject("StopWordsFile");
			stopWords = readLinesFromFile(stopWordsFileName.getFile()
					.getAbsolutePath());
			FileResource pcsKeyWordFile = (FileResource) aContext
					.getResourceObject("PCSKeyWordFile");
			goWords = readOrderedLinesFromFile(pcsKeyWordFile.getFile()
					.getAbsolutePath());
			FileResource pathOfTrainedModel = (FileResource) aContext
					.getResourceObject("PathOfModel");

			model = svm.svm_load_model(pathOfTrainedModel.getFile()
					.getAbsolutePath());
		} catch (Exception ace) {
			ace.printStackTrace();
		}
	}

	public void process(JCas jcas) {
		List<Double> feature = new ArrayList<Double>();

		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> wordTokenItr = indexes.getAnnotationIndex(WordToken.type)
				.iterator();
		List<String> unigrams = new ArrayList<String>();
		List bigrams = new ArrayList();

		while (wordTokenItr.hasNext()) {
			WordToken token = (WordToken) wordTokenItr.next();
			String tok = token.getCoveredText();

			if (tok == null)
				continue;
			if (!caseSensitive)
				tok = tok.toLowerCase();
			// if(!stopWords.contains(tok)) unigrams.add(tok);
			// -- this is the replace of the above line
			// Since the model was trained on words without non-word characters
			tok = tok.toLowerCase().replaceAll("-{2,}", " ").trim(); // To deal
			// with
			// the
			// cases
			// like:
			// Tobacco--quit
			// in
			// 1980.
			String[] toks = tok.split("\\s");
			for (int i = 0; i < toks.length; i++)
				if (!stopWords.contains(toks[i]))
					unigrams.add(toks[i]);
		}

		for (int i = 0; i < unigrams.size() - 1; i++)
			bigrams.add((String) unigrams.get(i) + "_"
					+ (String) unigrams.get(i + 1));

		// unigram & bigram keywords
		Iterator<String> itr = goWords.iterator();
		while (itr.hasNext()) {
			String k = (String) itr.next();
			double val = 0.0;

			if (k.indexOf("_") != -1) {
				for (int i = 0; i < bigrams.size(); i++) {
					if (k.equalsIgnoreCase((String) bigrams.get(i))) {
						val = 1.0;
						break;
					}
				}
			} else {
				for (int i = 0; i < unigrams.size(); i++) {
					if (k.equalsIgnoreCase((String) unigrams.get(i))) {
						val = 1.0;
						break;
					}
				}
			}

			feature.add(new Double(val));
		}

		// date information
		double dateInfo = 0.0;

		// Cannot access sentence by SentenceAnnotator or RecordSentence
		String sen = jcas.getDocumentText(); // this is sentence!!
		sen = sen.replaceAll("[.?!:;()',\"{}<>#+]", " ").trim();
		String[] strTokens = sen.split("\\s");

		for (int i = 0; i < strTokens.length; i++) {
			String s = strTokens[i];
			if (s.matches("19\\d\\d") || s.matches("19\\d\\ds")
					|| s.matches("20\\d\\d") || s.matches("20\\d\\ds")
					|| s.matches("[1-9]0s")
					|| s.matches("\\d{1,2}[/-]\\d{1,2}")
					|| s.matches("\\d{1,2}[/-]\\d{4}")
					|| s.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2}")
					|| s.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}")) {
				dateInfo = 1.0;
				// System.out.println("***dateInfo|"+s+"|"+dateInfo);
				break;
			}
		}

		feature.add(new Double(dateInfo));

		// set the libSVM feature vector
		svm_node[] x = new svm_node[feature.size()];
		for (int j = 0; j < feature.size(); j++) {
			x[j] = new svm_node();
			x[j].index = j + 1;
			x[j].value = ((Double) feature.get(j)).doubleValue();
		}

		double clsLabel; // 1:CURRENT_SMOKER, 2:PAST_SMOKER, 3:SMOKER
		clsLabel = svm.svm_predict(model, x); // predict class label using
		// libSVM
		String clsVal; // string value
		if (clsLabel == Const.CLASS_CURR_SMOKER_INT)
			clsVal = Const.CLASS_CURR_SMOKER;
		else if (clsLabel == Const.CLASS_PAST_SMOKER_INT)
			clsVal = Const.CLASS_PAST_SMOKER;
		else if (clsLabel == Const.CLASS_SMOKER_INT)
			clsVal = Const.CLASS_SMOKER;
		else
			clsVal = null;

		// System.out.println("clsLabel="+clsLabel+" clsVal="+clsVal);

		NominalAttributeValue nominalAttributeValue = new NominalAttributeValue(
				jcas);
		nominalAttributeValue.setAttributeName("smoking_status");
		nominalAttributeValue.setNominalValue(clsVal);
		nominalAttributeValue.addToIndexes();
	}

	private Set<String> readLinesFromFile(String fileName) throws IOException {
		Set<String> returnValues = new HashSet<String>();
		File file = new File(fileName);
		BufferedReader fileReader = new BufferedReader(new FileReader(file));

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (!caseSensitive)
				line = line.toLowerCase();
			returnValues.add(line);

		}
		return returnValues;
	}
	
	private List<String> readOrderedLinesFromFile(String fileName) throws IOException
	{
		List<String> returnValues = new ArrayList<String>();
		File file = new File(fileName);
	    BufferedReader fileReader = new BufferedReader(new FileReader(file));
		
		String line;
		while((line = fileReader.readLine()) != null)
		{
			if(line.length()==0) continue;
    		if(!caseSensitive) line = line.toLowerCase();
        		returnValues.add(line);

		}
		return returnValues;
	}

}
