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
package org.mitre.medfacts.uima;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

import org.apache.ctakes.assertion.zoner.types.Heading;
import org.apache.ctakes.assertion.zoner.types.Zone;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import org.mitre.medfacts.zoner.ZonerCli;
import org.mitre.medfacts.zoner.ZonerCli.HeadingRange;
import org.mitre.medfacts.zoner.ZonerCli.Range;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;

@TypeCapability(outputs =
{
  "org.apache.ctakes.assertion.zoner.types.Zone",
  "org.apache.ctakes.assertion.zoner.types.Zone:label",
  "org.apache.ctakes.assertion.zoner.types.Subzone",
  "org.apache.ctakes.assertion.zoner.types.Subzone:label",
  "org.apache.ctakes.assertion.zoner.types.Heading",
  "org.apache.ctakes.assertion.zoner.types.Heading:label"
})

public class ZoneAnnotator extends JCasAnnotator_ImplBase {
	public static final String PARAM_SECTION_REGEX_FILE_URI = "SectionRegex";
	
  @ConfigurationParameter(
      name = PARAM_SECTION_REGEX_FILE_URI,
      description = "xml configuration file with zone regular expression values",
      mandatory = true)
  protected String sectionRegexFileUriString;

  protected final Logger logger = Logger.getLogger(ZoneAnnotator.class.getName());
	
	@Override
	public void initialize (UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}
	
	private int countOfIndexOutOfBounds = 0;

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
	  
	System.out.println("sectionRegexFileUriString: " + sectionRegexFileUriString);
    URL sectionRegexFileInClasspathUrl = 
        getClass().getClassLoader().getResource(sectionRegexFileUriString);
    System.out.println("sectionRegexFileInClasspathUrl: " + sectionRegexFileInClasspathUrl);
    URI sectionRegexFileInClasspathUri;
    try
    {
      sectionRegexFileInClasspathUri = sectionRegexFileInClasspathUrl.toURI();
    } catch (URISyntaxException e1)
    {
      logger.error( String.format("section regex file not found [%s]", sectionRegexFileUriString), e1);
      throw new AnalysisEngineProcessException(e1);
    }
	  ZonerCli zonerCli =
      new ZonerCli(sectionRegexFileInClasspathUri);
    
		zonerCli.setEntireContents(jcas.getDocumentText());
		// initialize converter once contents are set
		zonerCli.initialize();
		try {
			zonerCli.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
			//throw new AnalysisEngineProcessException(e);
		} catch (StringIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.format("string index out of bounds exception count: %d%n", ++countOfIndexOutOfBounds);
			return;
			//throw new AnalysisEngineProcessException(e);
		}
		// Add the zone annotations
		List<Range> rangeList = zonerCli.getRangeList();
		for (Iterator<Range> i = rangeList.iterator(); i.hasNext();  ) {
			Range r = i.next();
		    Zone zAnnot = new Zone(jcas);	
		    zAnnot.setBegin(r.getBegin());
		    zAnnot.setEnd(r.getEnd());
		    zAnnot.setLabel(r.getLabel());
		    zAnnot.addToIndexes();
		    logger.info(String.format("added new zone annotation [%d-%d] \"%s\"", zAnnot.getBegin(), zAnnot.getEnd(), zAnnot.getCoveredText()));
		}
		
		
		// Add the heading annotations
		List<HeadingRange> headings = zonerCli.getHeadings();
		for (Iterator<HeadingRange> i = headings.iterator(); i.hasNext();  ) {
			HeadingRange r = i.next();
		    Heading hAnnot = new Heading(jcas);	
		    hAnnot.setBegin(r.getHeadingBegin());
		    hAnnot.setEnd(r.getHeadingEnd());
		    hAnnot.setLabel(r.getLabel());
		    hAnnot.addToIndexes();
		    logger.info(String.format("added new headingrange annotation [%d-%d] \"%s\"", hAnnot.getBegin(), hAnnot.getEnd(), hAnnot.getCoveredText()));
		}

	}

}
