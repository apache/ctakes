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
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Creates a single segment annotation that spans the entire document. This is
 * useful for running a TAE without a CasInitializer that would normally create
 * the segment annotations.
 * 
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "Single Sectionizer",
      description = "Annotates Document as a single Section.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class SimpleSegmentAnnotator extends JCasAnnotator_ImplBase {
	
  public static final String PARAM_SEGMENT_ID = "SegmentID";
	@ConfigurationParameter(
	    name = PARAM_SEGMENT_ID,
	    mandatory = false,
	    defaultValue = "SIMPLE_SEGMENT",
	    description = "Name to give to all segments"
	)
	private String segmentId;

	/**
	 * Entry point for processing.
	 */
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		Segment segment = new Segment(jCas);
		segment.setBegin(0);
		String text = jCas.getDocumentText();
		if (text == null) {
         String docId = DocIdUtil.getDocumentID( jCas );
			throw new AnalysisEngineProcessException("text is null for docId="
					+ docId, null);
		}
		segment.setEnd(jCas.getDocumentText().length());
		segment.setId(segmentId);
      segment.setPreferredText( segmentId );
		segment.addToIndexes();
	}
	
	public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
	  return AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class);
	}
	
	public static AnalysisEngineDescription createAnnotatorDescription(String segmentID) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class,
        SimpleSegmentAnnotator.PARAM_SEGMENT_ID,
        segmentID);
	}
}
