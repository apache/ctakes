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
package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * Creates a single segment annotation that spans the entire document. This is
 * useful for running a TAE without a CasInitializer that would normally create
 * the segment annotations.
 * 
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "Tag Sectionizer",
      description = "Annotates Document Sections by detecting start and end Section Tags.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class SimpleSegmentWithTagsAnnotator extends JCasAnnotator_ImplBase {
	private String segmentId;

	static private final Logger LOGGER = LogManager.getLogger( "SimpleSegmentWithTagsAnnotator" );

   @Override
   public void initialize( UimaContext aContext )
         throws ResourceInitializationException {
      super.initialize(aContext);

		segmentId = (String) aContext.getConfigParameterValue("SegmentID");
		if (segmentId == null) {
			segmentId = "SIMPLE_SEGMENT";
		}
	}

	/**
	 * Entry point for processing.
	 */
   @Override
   public void process( JCas jcas ) throws AnalysisEngineProcessException {
		LOGGER.info( "process(JCas)" );
      // sa.setBegin(0);
		String text = jcas.getDocumentText();
		if (text == null) {
			System.out.println("text == null in Segmentannotator");
         String docID = DocIdUtil.getDocumentID( jcas );
			System.out.println(" \t\tdocID=" + docID);
		}
		if (segmentId.compareTo("parseSectionTag") != 0) {
			// If the default segment ID or a segment ID other than
			// "parseSectionTag",
			// create 1 segment using the given segment ID.
			Segment sa = new Segment(jcas);
			sa.setBegin(0);
			sa.setEnd(jcas.getDocumentText().length());
			sa.setId(segmentId);
			sa.addToIndexes();
		} else { // is "parseSectionTag", so search for the segment markers and
					// annotate
			BufferedReader fileReader = new BufferedReader(new StringReader(
					text));

			int charNum = 0;
			int charPos = 0;
			char[] sectIdArr = new char[5];
			int endStartSegment = 0;
			int beginEndSegment = 0;

			try {
				while ((charNum = fileReader.read()) != -1) {

					if (charNum == '[') {
						charPos++;
						if (((charNum = fileReader.read()) == 's')
								|| (charNum == 'e')) {
							charPos++;
							if (((charNum = fileReader.read()) == 't')
									|| (charNum == 'n')) {
								charPos++;
								if (((charNum = fileReader.read()) == 'a')
										|| (charNum == 'd')) {
									charPos++;

									if ((charNum = fileReader.read()) == 'r') {
										endStartSegment = charPos + 24;
										fileReader.skip(14);

										fileReader.read(sectIdArr, 0, 5);
										charPos = charPos + 19;

									} else if (charNum == ' ') {
										beginEndSegment = charPos - 5;
										Segment sa = new Segment(jcas);
										sa.setBegin(endStartSegment);
										sa.setEnd(beginEndSegment);
										sa.setId(String.copyValueOf(sectIdArr));
										for (int i = 0; i < sectIdArr.length; i++)
											sectIdArr[i] = ' ';
										sa.addToIndexes();
									}
									charPos++;
								}
							}
						}
					} else
						charPos++;
				}
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

	}
}