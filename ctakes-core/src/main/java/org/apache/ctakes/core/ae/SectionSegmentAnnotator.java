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

import findstruct.Section;
import findstruct.StructFinder;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.util.DocumentSection;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Creates a single segment annotation that spans the entire document. This is
 * useful for running a TAE without a CasInitializer that would normally create
 * the segment annotations.
 * 
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "Sectionizer",
      description = "Annotates Document Sections by detecting Section Headers in template.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class SectionSegmentAnnotator extends JCasAnnotator_ImplBase {
	private String segmentId;
	private StructFinder structureFinder;

	private String templateFile= null;
	Logger logger = Logger.getLogger(this.getClass());

	public HashMap<Integer, DocumentSection> sections;

   @Override
   public void initialize( UimaContext aContext ) throws ResourceInitializationException {
      super.initialize( aContext );

		try {
			templateFile = ((FileResource)aContext.getResourceObject("template")).getFile().getAbsolutePath();
			structureFinder = new StructFinder();
		}catch(Exception e ){
			logger.error("Error reading template file: " + e.getMessage());
		}

		segmentId = (String) aContext.getConfigParameterValue("SegmentID");
		if (segmentId == null) {
			segmentId = "SIMPLE_SEGMENT";
		}
	}

	/**
	 * Entry point for processing.
	 * Identify all the sections of the medical record
	 */
   @Override
   public void process( JCas jCas ) throws AnalysisEngineProcessException {

		String text = jCas.getDocumentText();

		if (text == null) {
         String docId = DocIdUtil.getDocumentID( jCas );
			throw new AnalysisEngineProcessException("text is null for docId="
					+ docId, null);
		}

		// use the API to get the list of sections.
		try{
			ArrayList<Section> foundSections = structureFinder.execute(text, new FileInputStream(templateFile));

			// iterate over the ordered sections...
			int index = 0;
			for (Section sct : foundSections) {
				String nodeName = sct.getHeader();
				String content  = sct.getContent();

				if(nodeName== null || nodeName.trim().isEmpty() || 
						content == null || content.trim().isEmpty())
					continue;

				//			String[] splitContent = content.split("\n");
				//			int endLine = startLine + splitContent.length;

				index = text.indexOf(content, index);

				Segment segment = new Segment(jCas);
				segment.setBegin(index);
				segment.setEnd(index+content.length());
				segment.setId(sct.getHeader());
				segment.addToIndexes();
				index = index + content.length();
				//			DocumentSection section = 
				//					new DocumentSection(startLine, endLine, content);
				//			section.setSectionName(nodeName);
				//			sections.put(startLine, section);
				//
				//			startLine = endLine ;
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			Segment seg = new Segment(jCas);
			seg.setBegin(0);
			seg.setEnd(text.length());
			seg.setId(segmentId);
			seg.addToIndexes();
		}
	}
}

//class StructFinder {
//
//	/** Creates a new instance of StructFinder */
//	public StructFinder() {
//	}
//
//	/**
//	 * Main method that takes in the content of a file to process
//	 * and the input stream of a template of section names 
//	 * and returns the section names found in the given file
//	 * @param wholeFile
//	 * @param templateContent
//	 * 
//	 * @return a list with the found sections
//	 */
//	public ArrayList<Section> execute(String wholeFile, 
//			InputStream templateContent) {
//		ArrayList<Section> foundSections = new ArrayList<Section>();
//
//		String templateFileName = null;
//		StructModel template = null;
//
//
//		SAXBuilder parser = new SAXBuilder();
//		try {
//			template = new StructModel(parser.build(templateContent));
//		} catch (JDOMException e) {
//			System.err.println("Error parsing template file "
//					+ templateFileName + ": " + e);
//		}
//
//		if (template!=null) {
//			if (wholeFile!=null) { 
//				Element e = template.process(wholeFile);
//
//				for(Object el : e.getContent()) {
//					// find the type of the element
//					if (el.getClass().equals(Text.class)) {
//						Section sct = new Section("root", ((Text)el).getText());
//						foundSections.add(sct);
//					}
//					else if (el.getClass().equals(Element.class)) {
//						Element foundElement = ((Element)el);
//						Section sct = new Section(foundElement.getName(), 
//								foundElement.getText());
//						foundSections.add(sct);
//					}
//				}
//			}
//		}
//
//		return foundSections;
//	}
//}
