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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.ctakes.smokingstatus.type.UnknownSmokerNamedEntityAnnotation;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.smokingstatus.Const;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.smokingstatus.type.libsvm.NominalAttributeValue;

public class KuRuleBasedClassifierAnnotator extends JCasAnnotator_ImplBase {
	Set<String> smokingWords; // smoking related words
	Set<String> unknownWords; // if this word/phrase appears, treat the sentence
								// as UNKNOWN (eg: smoke detector)
	String classAttributeName;
	boolean caseSensitive = true;

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		super.initialize(aContext);

		smokingWords = new HashSet<String>();
		unknownWords = new HashSet<String>();
		try {

			Object paramValue = aContext
					.getConfigParameterValue("CaseSensitive");
			if (paramValue != null)
				caseSensitive = ((Boolean) paramValue).booleanValue();

			classAttributeName = (String) aContext
					.getConfigParameterValue("classAttribute");
			String smokingWordsFileName = (String) aContext
					.getConfigParameterValue("SmokingWordsFile");

			smokingWords = readLinesFromFile(FileLocator.getFile(
					smokingWordsFileName).getAbsolutePath());
			String unknownWordsFileName = (String) aContext
					.getConfigParameterValue("UnknownWordsFile");
			unknownWords = readLinesFromFile(FileLocator.getFile(
					unknownWordsFileName).getAbsolutePath());
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

	}

	public void process(JCas jcas) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> tokenItr = indexes.getAnnotationIndex(WordToken.type)
				.iterator();

		/*
		 * some cases there is no word token for a given jcas So, if initial
		 * classVal is "null" this null is assigned for class value and cause
		 * error.
		 */
		String classVal = Const.CLASS_UNKNOWN;
		while (tokenItr.hasNext()) {
			WordToken token = (WordToken) tokenItr.next();
			String strToken = token.getCoveredText();

			// System.out.println("***word:" + strToken);
			if (strToken == null)
				continue;
			if (!caseSensitive)
				strToken = strToken.toLowerCase();

			if (smokingWords.contains(strToken)) {
				classVal = Const.CLASS_KNOWN;
				// System.out.println("***smoking word found");
				break;
			}
		}

		// This is to check if there is unknown indication words when found
		// smoking related word
		// e.g.: Smoke Detector in-Home------ Yes
		if (classVal.equals(Const.CLASS_KNOWN)) {
			String sen = jcas.getDocumentText(); // This is sentence here!!

			Iterator<String> itr = unknownWords.iterator();
			while (itr.hasNext()) {
				String s = (String) itr.next();
				if (sen.toLowerCase().trim().matches(".*" + s + ".*")) { // need
																			// trim()!!
					// System.out.println("***UnknownWords|"+s+"|");
					classVal = Const.CLASS_UNKNOWN;
					break;
				}
			}
		}

		// ---
		// 
		Iterator<?> neItr = indexes.getAnnotationIndex(
				UnknownSmokerNamedEntityAnnotation.type).iterator();
		while (neItr.hasNext()) {
			UnknownSmokerNamedEntityAnnotation neAnn = (UnknownSmokerNamedEntityAnnotation) neItr
					.next();
			System.out.println("***UnknownNamedEntity|"
					+ neAnn.getCoveredText());
			classVal = Const.CLASS_UNKNOWN;
		}
		// ---

		// System.out.println("***" + classVal + " for " + classAttributeName +
		// "***");

		NominalAttributeValue nominalAttributeValue = new NominalAttributeValue(
				jcas);
		nominalAttributeValue.setAttributeName(classAttributeName);
		nominalAttributeValue.setNominalValue(classVal);
		nominalAttributeValue.addToIndexes();
	}

	private Set<String> readLinesFromFile(String fileName) throws IOException {
		Set<String> returnValues = new HashSet<String>();
		File file = new File(fileName);
		BufferedReader fileReader = new BufferedReader(new FileReader(file));

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (!caseSensitive)
				line = line.toLowerCase().trim();
			returnValues.add(line);

		}
		return returnValues;
	}
}
