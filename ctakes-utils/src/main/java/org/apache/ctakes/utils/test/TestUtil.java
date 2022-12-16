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
package org.apache.ctakes.utils.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;


public class TestUtil {

	/**
	 * This method simply calls
	 * {@link #createAnalysisEngineJCas(File, File, String)} with the
	 * charSetName equal to "UTF-8".
	 * 
	 * @param analysisEngineDescriptorFile
	 *            a descriptor file for a UIMA Analysis Engine.
	 * @param textFile
	 *            a file that contains text that the analysis engine will
	 *            process.
	 * @return a JCas that represents the analysis engine's processing of the
	 *         text file.
	 * @throws ResourceInitializationException
	 * @see {@link #createAnalysisEngineJCas(File, File, String)}
	 */
	public static JCas processAE(AnalysisEngine ae, File textFile) throws ResourceInitializationException {
		return processAE(ae, textFile, "UTF-8");
	}

	/**
	 * This method reads in the text from the text file using the provided
	 * character set and then calls {@link #getJCas(File, String)}
	 * 
	 * @param analysisEngineDescriptorFile
	 *            a descriptor file for a UIMA Analysis Engine.
	 * @param textFile
	 *            a file that contains text that the analysis engine will
	 *            process.
	 * @param charSetName
	 *            the name of the character encoding of the file
	 * @return a JCas that represents the analysis engine's processing of the
	 *         text file.
	 * @throws ResourceInitializationException
	 * @see {@link #getJCas(File, String)}
	 * @see Charset
	 */
	public static JCas processAE(AnalysisEngine ae, File textFile, String charSetName)
			throws ResourceInitializationException {

		try {
			String text = FileUtils.file2String(textFile, charSetName);
			return processAE(ae, text);
		} catch (IOException ioe) {
			throw new ResourceInitializationException(ioe);
		}
	}

	/**
	 * This method creates a JCas for the provided analysis engine descriptor
	 * file and then processes the provided text and returns the JCas. This
	 * method is convenient for unit testing because the first line of a unit
	 * test can be. <br>
	 * 
	 * <pre>
	 * JCas jCas = createAnalysisEngineJCas(someAEDescriptorFile, someText);
	 * </pre>
	 * 
	 * <br>
	 * The subsequent lines can then be assertions that query the expected
	 * contents of the JCas.
	 * 
	 * @param analysisEngineDescriptorFile
	 *            a descriptor file for a UIMA Analysis Engine.
	 * @param text
	 *            text that the analysis engine will process.
	 * 
	 * @return a JCas that represents the analysis engine's processing of the
	 *         text file.
	 * @throws ResourceInitializationException
	 * 
	 * The code for this method was found here:
	 * http://article.gmane.org/gmane.comp.apache.uima.general/880/match=push+documents+into+cpe+collectionreader
	 * 
	 */

	public static JCas processAE(AnalysisEngine ae, String text) throws ResourceInitializationException {
		try {
			JCas jCas = ae.newJCas();
			jCas.setDocumentText(text);
			ae.process(jCas);
			return jCas;
		} catch (AnalysisEngineProcessException aepe) {
			throw new ResourceInitializationException(aepe);
		}
	}

	public static AnalysisEngine getAE(File analysisEngineDescriptorFile) throws ResourceInitializationException {
		try {
			XMLInputSource xmlInputSource = new XMLInputSource(analysisEngineDescriptorFile);
			XMLParser xmlParser = UIMAFramework.getXMLParser();
			ResourceSpecifier resourceSpecifier = xmlParser.parseResourceSpecifier(xmlInputSource);
			AnalysisEngine analysisEngine = UIMAFramework.produceAnalysisEngine(resourceSpecifier);
			return analysisEngine;
		} catch (IOException ioe) {
			throw new ResourceInitializationException(ioe);
		} catch (InvalidXMLException ixe) {
			throw new ResourceInitializationException(ixe);
		}

	}

	public static CollectionReader getCR(File collectionReaderDescriptorFile) throws ResourceInitializationException {
		try {
			XMLInputSource xmlInputSource = new XMLInputSource(collectionReaderDescriptorFile);
			XMLParser xmlParser = UIMAFramework.getXMLParser();
			ResourceSpecifier resourceSpecifier = xmlParser.parseResourceSpecifier(xmlInputSource);
			CollectionReader collectionReader = UIMAFramework.produceCollectionReader(resourceSpecifier);
			return collectionReader;
		} catch (IOException ioe) {
			throw new ResourceInitializationException(ioe);
		} catch (InvalidXMLException ixe) {
			throw new ResourceInitializationException(ixe);
		}

	}

	public static <T extends TOP> int getFeatureStructureSize(JCas jCas, Class<T> cls) {

		try {
		
			int type = (Integer) cls.getField("type").get(null);
			FSIndex fsIndex = jCas.getAnnotationIndex(type);

			FSIterator iterator = fsIndex.iterator();
			int size = 0;
			while (iterator.hasNext()) {
				iterator.next();
				size++;
			}
			return size; 
			
		} catch (IllegalAccessException iae) {
			throw new IllegalArgumentException("class passed in caused an exception: class=" + cls.getCanonicalName(), iae);
		} catch (NoSuchFieldException nsfe) {
			throw new IllegalArgumentException("class passed in caused an exception: class=" + cls.getCanonicalName(), nsfe);
		}
		
	}

	public static <T extends TOP> T getFeatureStructureAtIndex(JCas jCas, Class<T> cls, int index) {

		try {
			int type = (Integer) cls.getField("type").get(null);
			FSIndex fsIndex = jCas.getAnnotationIndex(type);
			if (index < 0)
				throw new IllegalArgumentException("index less than zero: index=" + index);
			if (index >= fsIndex.size())
				throw new IllegalArgumentException("index greater than or equal to fsIndex.size(): index=" + index);

			FSIterator iterator = fsIndex.iterator();
			Object obj = null;
			for (int i = 0; i <= index; i++) {
				obj = iterator.next();
			}
			return cls.cast(obj);
		} catch (IllegalAccessException iae) {
			throw new IllegalArgumentException("class passed in caused an exception: class=" + cls.getCanonicalName(),
					iae);
		} catch (NoSuchFieldException nsfe) {
			throw new IllegalArgumentException("class passed in caused an exception: class=" + cls.getCanonicalName(),
					nsfe);
		}
	}

	static String unexpectedParamValueMsg = "unexpected parameter value for descriptor file %1$s for param: %2$s";

	public static void testConfigParam(UimaContext uimaContext, String descriptorFile, String paramName, Object paramValue) {
		testConfigParam(uimaContext, descriptorFile, paramName, paramValue, null);
	}
	public static void testConfigParam(UimaContext uimaContext, String descriptorFile, String paramName, Object paramValue, Integer arrayIndex) {
		if(arrayIndex == null) {
			assertEquals(String.format(unexpectedParamValueMsg, descriptorFile, paramName), paramValue, uimaContext
				.getConfigParameterValue(paramName));
		} else
			assertEquals(String.format(unexpectedParamValueMsg, descriptorFile, paramName), paramValue, ((Object[])uimaContext
					.getConfigParameterValue(paramName))[arrayIndex]);
	}
}
