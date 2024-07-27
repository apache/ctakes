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
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates segment annotations based on the ccda_sections.txt file Which is
 * based on HL7/CCDA/LONIC standard headings Additional custom heading names can
 * be added to the file.
 */
@PipeBitInfo(
      name = "CCDA Sectionizer",
      description = "Annotates Document Sections by detecting Section Headers using Regular Expressions provided in a File.",
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID },
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class CDASegmentAnnotator extends JCasAnnotator_ImplBase {

	static private final Logger LOGGER = LoggerFactory.getLogger( "CDASegmentAnnotator" );
	protected static HashMap<String, Pattern> patterns = new HashMap<>();
	protected static HashMap<String, String> section_names = new HashMap<>();
	protected static final String DEFAULT_SECTION_FILE_NAME = "src/user/resources/org/apache/ctakes/core/sections"
                                                             + "/ccda_sections.txt";
	public static final String PARAM_FIELD_SEPERATOR = ",";
	public static final String PARAM_COMMENT = "#";
	public static final String SIMPLE_SEGMENT = "SIMPLE_SEGMENT";

  public static final String PARAM_SECTIONS_FILE = "sections_file";
	@ConfigurationParameter(name = PARAM_SECTIONS_FILE, 
	    description = "Path to File that contains the section header mappings", 
	    defaultValue=DEFAULT_SECTION_FILE_NAME,
	    mandatory=false)
	protected String sections_path;

	/**
	 * Init and load the sections mapping file and precompile the regex matches
	 * into a hashmap
	 */
	@Override
  public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		try {
		  BufferedReader br = new BufferedReader(new InputStreamReader(
		      FileLocator.getAsStream(sections_path)));

		  // Read in the Section Mappings File
		  // And load the RegEx Patterns into a Map
			LOGGER.info("Reading Section File " + sections_path);
		  String line = null;
		  while ((line = br.readLine()) != null) {
		    if (!line.trim().startsWith(PARAM_COMMENT)) {
		      String[] l = line.split(PARAM_FIELD_SEPERATOR);
		      // First column is the HL7 section template id
		      if (l != null && l.length > 0 && l[0] != null
		          && l[0].length() > 0
		          && !line.endsWith(PARAM_FIELD_SEPERATOR)) {
		        String id = l[0].trim();
		        // Make a giant alternator (|) regex group for each HL7
		        Pattern p = buildPattern(l);
		        patterns.put(id, p);
		        if (l.length > 2 && l[2] != null) {
		          String temp = l[2].trim();
		          section_names.put(id, temp);
		        }						

		      } else {
					LOGGER.info("Warning: Skipped reading sections config row: "
		            + Arrays.toString(l));
		      }
		    }
		  }      
		} catch (IOException e) {
		  e.printStackTrace();
		  throw new ResourceInitializationException(e);
		}
	}

	/**
	 * Build a regex pattern from a list of section names. used only during init
	 * time
	 */
	private static Pattern buildPattern(String[] line) {
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i < line.length; i++) {
			// Build the RegEx pattern for each comma delimited header name
			// Suffixed with a aggregator pipe
			sb.append("\\s*" + line[i].trim() + "(\\s\\s|\\s:|:|\\s-|-)");
			if (i != line.length - 1) {
				sb.append("|");
			}
		}
		int patternFlags = 0;
		patternFlags |= Pattern.CASE_INSENSITIVE;
		patternFlags |= Pattern.DOTALL;
		patternFlags |= Pattern.MULTILINE;
		Pattern p = Pattern.compile("^(" + sb + ")", patternFlags);
		return p;
	}

	@Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
		String text = jCas.getDocumentText();
		if (text == null) {
         String docId = DocIdUtil.getDocumentID( jCas );
			LOGGER.info("text is null for docId=" + docId);
		} else {
			ArrayList<Segment> sorted_segments = new ArrayList<>();
			for (String id : patterns.keySet()) {
				Pattern p = patterns.get(id);
				// System.out.println("Pattern" + p);
				Matcher m = p.matcher(text);
				while (m.find()) {
					Segment segment = new Segment(jCas);
					segment.setBegin(m.start());
					segment.setEnd(m.end());
					segment.setId(id);					
					sorted_segments.add(segment);
				}
			}
			// If there are non segments, create a simple one that spans the
			// entire doc
			if (sorted_segments.size() <= 0) {
				Segment header = new Segment(jCas);
				header.setBegin(0);
				header.setEnd(text.length());
				header.setId(SIMPLE_SEGMENT);
				sorted_segments.add(header);
			}			
			// TODO: this is kinda redundant, but needed the sections in sorted
			// Order to determine the end of section which is assumed to be the
			// beginning of the next section
			Collections.sort(sorted_segments, new Comparator<Segment>() {
				public int compare(Segment s1, Segment s2) {
					return s1.getBegin() - (s2.getBegin());
				}
			});
			int index = 0;
			for (Segment s : sorted_segments) {
				int prevEnd = s.getEnd();
				int nextBegin = text.length();
				if (index > 0) {
					// handle case for first section
					sorted_segments.get(index - 1).getEnd();
				}
				if (index + 1 < sorted_segments.size()) {
					// handle case for last section
					nextBegin = sorted_segments.get(index + 1).getBegin();
				}
				// Only create a segment if there is some text.
				// Handle the case where it's an empty segement
				if (nextBegin > prevEnd) {
					Segment segment = new Segment(jCas);
					segment.setBegin(prevEnd);
					segment.setEnd(nextBegin);
					segment.setId(s.getId());
					segment.addToIndexes();
					segment.setPreferredText(section_names.get(s.getId()));					
					index++;
				}
				// handle case where there is only a single SIMPLE_SEGMENT
				else if (nextBegin == prevEnd && nextBegin > 0 && index == 0) {
					Segment segment = new Segment(jCas);
					segment.setBegin(0);
					segment.setEnd(nextBegin);
					segment.setId(s.getId());
					segment.addToIndexes();
					index++;
				}
			}	
		}
	}

}
