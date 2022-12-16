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
package org.apache.ctakes.assertion.medfacts;

import org.apache.ctakes.assertion.stub.CharacterOffsetToLineTokenConverter;
import org.apache.ctakes.assertion.stub.CharacterOffsetToLineTokenConverterDefaultImpl;
import java.io.File;
import java.io.FileInputStream;

import org.apache.log4j.Logger;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.xml.sax.SAXException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription; 
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS; 
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.cas.TypeSystem; 
import java.util.Collections; 
import org.apache.uima.util.XMLParser;
import org.apache.uima.UIMAFramework;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.Sofa; 
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.ctakes.assertion.stub.Annotation;
import org.apache.ctakes.assertion.stub.AssertionAnnotation;
import org.apache.ctakes.assertion.stub.ConceptType;
import org.apache.uima.cas.text.AnnotationIndex;
//import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.assertion.stub.LineAndTokenPosition;
import java.io.PrintWriter;
import java.io.IOException;
import org.apache.ctakes.assertion.stub.SingleDocumentProcessor;
import org.apache.ctakes.assertion.stub.StringHandling;
import org.apache.ctakes.assertion.stub.LineTokenToCharacterOffsetConverter;
import org.apache.ctakes.assertion.stub.AssertionFileProcessor;
import java.util.List;

import org.apache.ctakes.assertion.medfacts.types.Assertion;
import org.apache.ctakes.assertion.medfacts.types.Concept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import java.io.FileOutputStream;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.util.XMLSerializer;


/*
 * Comments on using this class:
 * This provides a utility to convert i2b2 format sentence and tokens to cTAKES Sentence and WordToken
 * annotations.  It takes as input the input text file, as well as the assertion annotations (the .ast file).
 * It also takes the pipeline descriptor (the top-level descriptor used for the entire pipeline) and an output
 * file path.  The potentially tricky part of this process involves setting up the descriptor so that 
 * it refers to the correct sub-descriptors, etc.  This probably won't be a problem with a full install
 * of the pipeline, but could be an issue in a development environment - some paths may need to be manually
 * (and temporarily) adjusted).
 */

public class Converti2b2AnnotationsToCTAKES {
	private static Logger logger = Logger.getLogger(Converti2b2AnnotationsToCTAKES.class);
	
	private static CAS getTypeSystemFromDescriptor(String descriptor) throws InvalidXMLException, IOException, ResourceInitializationException, CASException {
		XMLParser xmlParser = UIMAFramework.getXMLParser();
		AnalysisEngineDescription tsDesc = xmlParser.parseAnalysisEngineDescription(new XMLInputSource(descriptor));
		return CasCreationUtils.createCas(tsDesc);
	}
	
	/**
	   * Serialize a CAS to a file in XMI format
	   * 
	   * @param aCas
	   *          CAS to serialize
	   * @param name
	   *          output file
	   * @throws SAXException
	   * @throws Exception
	   * 
	   * @throws ResourceProcessException
	   */
	  private static void writeXmi(CAS aCas, File name) throws IOException, SAXException {
	    FileOutputStream out = null;

	    try {
	      // write XMI
	      out = new FileOutputStream(name);
	      XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
	      XMLSerializer xmlSer = new XMLSerializer(out, false);
	      ser.serialize(aCas, xmlSer.getContentHandler());
	    } finally {
	      if (out != null) {
	        out.close();
	      }
	    }
	  }
	
	public static void main(String [] args) throws IOException, InvalidXMLException, CASException, SAXException, AnalysisEngineProcessException {
		//File currentTextFile = new File(args[0]);
		File assertionDir = new File(args[1]);
		File dir = new File(args[0]);
		File odir = new File(args[3]);
		String desc = args[2];

		for (File file : dir.listFiles()) {
			CAS cas = null;
			try {
				cas = getTypeSystemFromDescriptor(desc);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			String contents = StringHandling.readEntireContents(file);
			String fname = file.getName();
			String outFilePath = odir + "/" + file.getName().substring(0, fname.length() - 3) + "xmi";
			File outFile = new java.io.File(outFilePath);
			String assertionFilePath = assertionDir + "/" + file.getName().substring(0, fname.length() - 3) + "ast";
			System.out.println("Assertion file path: " + assertionFilePath);
			File assertionFile = new java.io.File(assertionFilePath);
			LineTokenToCharacterOffsetConverter converter = new LineTokenToCharacterOffsetConverter(contents);
			AssertionFileProcessor assertionProcessor = new AssertionFileProcessor();
			List<Annotation> assertions = assertionProcessor.processAnnotationFile(assertionFile);

			JCas jcas = cas.getJCas();
			SingleDocumentProcessor p = new SingleDocumentProcessor();
			p.setContents(contents);
			p.preprocess();
			String [][] tokenArrays = p.getTokenArrays();
			jcas.setSofaDataString(contents, "");
			int sentNum = 0;
			int tokNum = 0;
			for (int i=0; i < tokenArrays.length; i++) {
				Sentence sent = new Sentence(jcas);
				LineAndTokenPosition sentStart = new LineAndTokenPosition();
				sentStart.setLine(i+1);
				sentStart.setTokenOffset(0);
				LineAndTokenPosition sentEnd = new LineAndTokenPosition();
				sentEnd.setLine(i+1);
				sentEnd.setTokenOffset(tokenArrays[i].length-1);
				LineTokenToCharacterOffsetConverter.BeginAndEndCharacterOffsetPair sStart = converter.convert(sentStart);
				LineTokenToCharacterOffsetConverter.BeginAndEndCharacterOffsetPair sEnd = converter.convert(sentEnd);
				if ((sStart == null) || (sEnd == null)) {
					sent.setBegin(0);
					sent.setEnd(0);
					sent.setSentenceNumber(sentNum);
				} else {
					sent.setBegin(sStart.getBegin()); // get begin of first token
					sent.setEnd(sEnd.getEnd() + 1); // get end of last token
					sent.setSentenceNumber(sentNum);
				}
				sentNum++;
				sent.addToIndexes();
				for (int j=0; j < tokenArrays[i].length; j++) {
					WordToken tok = new WordToken(jcas);
					LineAndTokenPosition word = new LineAndTokenPosition();
					word.setLine(i+1);
					word.setTokenOffset(j);
					LineTokenToCharacterOffsetConverter.BeginAndEndCharacterOffsetPair tPos = converter.convert(word);
					if (tPos == null) {
						tok.setBegin(0);
						tok.setEnd(1);
					} else {
						tok.setBegin(tPos.getBegin());
						tok.setEnd(tPos.getEnd() + 1);
					}
					tok.setTokenNumber(tokNum);
					tokNum++;	
					tok.addToIndexes();
				}
			}
			logger.info("before assertions");
			for (Annotation a : assertions) {
				logger.info("  begin assertion");
				logger.info("  assertion: " + a.toString());
				//Concept assertion = new Concept(jcas);

				AssertionAnnotation i2b2Assertion = (AssertionAnnotation)a;
				ConceptType conceptType = i2b2Assertion.getConceptType();

				IdentifiedAnnotation entityOrEventMention = null;
				if (conceptType.equals(ConceptType.TREATMENT))
				{
				  entityOrEventMention = new EventMention(jcas);
				} else
				{
				  entityOrEventMention = new EntityMention(jcas);
				}
				
				LineAndTokenPosition assertionStart = new LineAndTokenPosition();
				LineAndTokenPosition assertionEnd = new LineAndTokenPosition();
				assertionStart.setLine(a.getBegin().getLine());
				assertionStart.setTokenOffset(a.getBegin().getTokenOffset());
				assertionEnd.setLine(a.getEnd().getLine());
				assertionEnd.setTokenOffset(a.getEnd().getTokenOffset());
				
//				assertion.setBegin(converter.convert(assertionStart).getBegin());
//				assertion.setEnd(converter.convert(assertionEnd).getEnd() + 1);
//				assertion.setConceptType("PROBLEM");
//				assertion.addToIndexes();
				
				entityOrEventMention.setBegin(converter.convert(assertionStart).getBegin());
				entityOrEventMention.setEnd(converter.convert(assertionEnd).getEnd());
				entityOrEventMention.setConfidence(1.0f);
				
				FSArray ontologyConceptArray = ConceptLookup.reverseLookup(conceptType, jcas);
				entityOrEventMention.setOntologyConceptArr(ontologyConceptArray);
				
				//adjustAssertionAttributesByI2B2Convertion(entityOrEventMention, i2b2Assertion);
				AssertionAnalysisEngine.mapI2B2AssertionValueToCtakes(i2b2Assertion.getAssertionValue().toString().toLowerCase(), entityOrEventMention);
				
				entityOrEventMention.addToIndexes();
				logger.info("  end assertion");
			}
			logger.info("after assertions");
			writeXmi(cas,outFile);
		}
	}

  private static void adjustAssertionAttributesByI2B2Convertion(
      IdentifiedAnnotation entityOrEventMention,
      AssertionAnnotation i2b2Assertion)
  {
    
  }
}
