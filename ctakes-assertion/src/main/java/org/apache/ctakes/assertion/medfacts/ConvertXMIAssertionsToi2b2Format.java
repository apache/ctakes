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
package org.apache.ctakes.assertion.medfacts;

import org.apache.ctakes.assertion.stub.CharacterOffsetToLineTokenConverter;
import org.apache.ctakes.assertion.stub.CharacterOffsetToLineTokenConverterDefaultImpl;
import java.io.File;
import java.io.FileInputStream;

import org.apache.ctakes.assertion.medfacts.types.Assertion;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.xml.sax.SAXException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription; 
import org.apache.uima.cas.CAS; 
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.cas.TypeSystem; 
import java.util.Collections; 
import org.apache.uima.util.XMLParser;
import org.apache.uima.UIMAFramework;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.jcas.cas.Sofa; 
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.ctakes.assertion.stub.LineAndTokenPosition;
import java.io.PrintWriter;
import java.io.IOException;

public class ConvertXMIAssertionsToi2b2Format {

	private static CAS getTypeSystemFromDescriptor(String descriptor) throws InvalidXMLException, IOException, ResourceInitializationException, CASException {
		XMLParser xmlParser = UIMAFramework.getXMLParser();
		AnalysisEngineDescription tsDesc = xmlParser.parseAnalysisEngineDescription(new XMLInputSource(descriptor));
		return CasCreationUtils.createCas(tsDesc);
	}
	
	
	public static void main(String [] args) throws IOException, InvalidXMLException, CASException {
		File dir = new File(args[0]);
		File odir = new File(args[2]);
		String desc = args[1];
		FileInputStream inputStream = null;
		int assertionType = Assertion.type;
		CAS cas = null;
		try {
			cas = getTypeSystemFromDescriptor(desc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (cas != null) {
			for (File file : dir.listFiles()) {
				try {
					inputStream = new FileInputStream(file);
					PrintWriter writer = new PrintWriter(new java.io.File(odir + "/" + file.getName()));
				    XmiCasDeserializer.deserialize(inputStream, cas);
				    JCas jcas = cas.getJCas();
				    String sofaString = jcas.getDocumentText();
				    System.err.println("Converting text string: " + sofaString);
				    CharacterOffsetToLineTokenConverterDefaultImpl converter = new CharacterOffsetToLineTokenConverterDefaultImpl(sofaString);
					AnnotationIndex<Annotation> aIndex = jcas.getAnnotationIndex(assertionType);
					for (Annotation a : aIndex) {
						Assertion ai = (Assertion) a;
						int begin = ai.getBegin();
						int end = ai.getEnd();
						LineAndTokenPosition begPos = converter.convert(begin);
						LineAndTokenPosition endPos = converter.convert(end);
						writer.println("c=\"" + sofaString.substring(begin,end) + "\" " + begPos.getLine() + ":" + begPos.getTokenOffset() +
								" " + endPos.getLine() + ":" + endPos.getTokenOffset() + "||t=\"problem\"||a=\"" + ai.getAssertionType() + "\"");
					}
					writer.close();
				} catch (Exception e) { 
					throw new RuntimeException(e); 
					} finally {
					 if (inputStream != null)
						 inputStream.close(); 
					}
			}
		}
	}
}
