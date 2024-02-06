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
import org.apache.ctakes.assertion.zoner.types.Heading;
import org.apache.ctakes.assertion.zoner.types.Zone;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.mitre.medfacts.zoner.ZonerCli;
import org.mitre.medfacts.zoner.ZonerCli.HeadingRange;

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
	public static final String PARAM_INCLUDE_GENERICS = "IncludeGenerics";
	
  @ConfigurationParameter(
      name = PARAM_SECTION_REGEX_FILE_URI,
      description = "xml configuration file with zone regular expression values",
      mandatory = true)
  protected String sectionRegexFileUriString;
  
  @ConfigurationParameter(
	      name = PARAM_INCLUDE_GENERICS,
	      description = "include generic sections",
	      mandatory = false)
  protected Boolean includeGenerics = Boolean.FALSE;

  protected final Logger logger = Logger.getLogger(ZoneAnnotator.class.getName());
	
	@Override
	public void initialize (UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		buildZonerCli();
	}
	
	private void buildZonerCli() throws ResourceInitializationException {
		//System.out.println("sectionRegexFileUriString: " + sectionRegexFileUriString);
	    URL sectionRegexFileInClasspathUrl = 
	        getClass().getClassLoader().getResource(sectionRegexFileUriString);
	    logger.info("sectionRegexFileInClasspathUrl: " + sectionRegexFileInClasspathUrl);
	    logger.info("includeGenerics: " + includeGenerics);
	    URI sectionRegexFileInClasspathUri;
	    try
	    {
	      sectionRegexFileInClasspathUri = sectionRegexFileInClasspathUrl.toURI();
			_zonerCli = new ZonerCli(sectionRegexFileInClasspathUri);
			// NB UC custom version of the zoner
			boolean includeGenericHeadings =  includeGenerics.booleanValue();
			_zonerCli.includeGenerics(includeGenericHeadings);
			
	    } catch (URISyntaxException e1)
	    {
	      logger.error( String.format("section regex file not found [%s]", sectionRegexFileUriString), e1);
	      throw new ResourceInitializationException(e1);
	    }
	}

	private int countOfIndexOutOfBounds = 0;
	private ZonerCli _zonerCli = null;

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		_zonerCli.setEntireContents(jcas.getDocumentText());
		// initialize converter once contents are set
		_zonerCli.initialize();
		try {
			_zonerCli.execute();
		} catch (IOException e) {
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
		int remembered_begin = -1;
		List<HeadingRange> rangeList = _zonerCli.getHeadings();
		for (Iterator<HeadingRange> i = rangeList.iterator(); i.hasNext();  ) {
			HeadingRange r = i.next();
			if (validRange(r.getHeadingBegin(),r.getHeadingEnd())) {
			    if (remembered_begin < 0 || validRange(remembered_begin, r.getHeadingEnd())) {
			    	Zone zAnnot = new Zone(jcas);
			    	zAnnot.setBegin((remembered_begin < 0) ? r.getHeadingBegin() : remembered_begin);
				    zAnnot.setEnd(r.getHeadingEnd());
				    zAnnot.setLabel(r.getLabel());
				    zAnnot.addToIndexes();
				    remembered_begin = -1;
				    logger.debug(String.format("added new zone annotation [%d-%d] \"%s\"", 
				    		zAnnot.getBegin(), zAnnot.getEnd(), zAnnot.getCoveredText()));
			    }
			    else {
			    	logger.debug(String.format("unable to patch [%d-%d]", remembered_begin, r.getHeadingEnd()));
			    	remembered_begin = -1;
			    }
			} else {
				// note that range is inverted (a ZonerCli error) so range End is its sraer
				remembered_begin = r.getHeadingEnd();
				logger.debug(String.format("inverted range [%d-%d]", r.getHeadingBegin(), r.getHeadingEnd()));
			}
		    
		}
		
		// Add the heading annotations
		remembered_begin = -1;
		List<HeadingRange> headings = _zonerCli.getHeadings();
		for (Iterator<HeadingRange> i = headings.iterator(); i.hasNext();  ) {
			HeadingRange r = i.next();
			if (validRange(r.getHeadingBegin(), r.getHeadingEnd())) {
				if (remembered_begin < 0 || validRange(remembered_begin, r.getHeadingEnd())) {
				    Heading hAnnot = new Heading(jcas);	
				    hAnnot.setBegin((remembered_begin < 0) ? r.getHeadingBegin() : remembered_begin);
				    hAnnot.setEnd(r.getHeadingEnd());
				    hAnnot.setLabel(r.getLabel());
				    hAnnot.addToIndexes();
				    remembered_begin = -1;
				    logger.debug(String.format("added new headingrange annotation [%d-%d] \"%s\"", 
				    		hAnnot.getBegin(), hAnnot.getEnd(), hAnnot.getCoveredText()));
				} else {
			    	logger.debug(String.format("unable to patch [%d-%d]", remembered_begin, r.getHeadingEnd()));
			    	remembered_begin = -1;
			    }
			} else {
				// note that range is inverted (a ZonerCli error) so range End is its sraer
				remembered_begin = r.getHeadingEnd();
				logger.debug(String.format("inverted heading range [%d-%d]", r.getHeadingBegin(), r.getHeadingEnd()));
			}
		}

	}

	private boolean validRange(int begin, int end) {
		if (begin >= 0 && end > begin) 
			return true;
		return false;
	}
}
