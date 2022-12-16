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
package org.apache.ctakes.ytex.uima.annotators;

import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * @author vgarla
 *
 */
public class DateAnnotatorTest {

	private final static Logger LOGGER = Logger.getLogger(DateAnnotatorTest.class);
	private final static String TYPESYSTEM_DESCRIPTOR_RESOURCE = "org/apache/ctakes/ytex/types/TypeSystem.xml";

	private static URL TYPESYSTEM_URL = null;

	@BeforeClass
	public static void setUp() {
		TYPESYSTEM_URL = DateAnnotatorTest.class.getClassLoader().getResource(TYPESYSTEM_DESCRIPTOR_RESOURCE);
	}

	/**
	 * Tests the URL of the TypeSystem.xml or null if no resource with this name is found
	 */
	@Test
	public void testCorrectLoadForTypeSystemResource() {
		assertNotNull("Expecting a valid resource file URL", TYPESYSTEM_URL);
		assertFalse(TYPESYSTEM_URL.getPath().isEmpty());
		assertTrue(TYPESYSTEM_URL.getPath().endsWith(TYPESYSTEM_DESCRIPTOR_RESOURCE));
	}

	@Test
	public void testLoadForDefaultYtexTypeSystemDescriptor() {
		TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
		assertNotNull("Wasn't able to create a type system", typeSystem);

		Import imp = new Import_impl();
		assertNotNull("Wasn't able to create a default org.apache.uima.resource.metadata.Import", imp);
		imp.setLocation(TYPESYSTEM_URL.getPath());
		assertEquals("Import.getLocation() is different the the one used for Import.setLocation()", TYPESYSTEM_URL.getPath(), imp.getLocation());

		typeSystem.setImports(new Import[]{ imp });
		assertEquals("Expected only 1 Import", 1, typeSystem.getImports().length);
	}

	/**
	 * Verify that we can craete a JCas. Previously we had many issues related to DateAnnotatorTest
	 *
	 * See: CTAKES-415
	 */
	@Test
	public void testCreateJCasFromPath() {
		LOGGER.info("creating JCas from: " + TYPESYSTEM_URL.getPath());
		try {
			JCas jCas = JCasFactory.createJCasFromPath(TYPESYSTEM_URL.getPath());
			assertNotNull("Expecting a valid JCas object", jCas);
		} catch (UIMAException e) {
			LOGGER.error(String.format("Could not create JCas from %s", TYPESYSTEM_URL.getPath()), e);
			assertNull(String.format("JCas couldn't be initialized from %s", TYPESYSTEM_URL.getPath()), e);
		}
	}


	// Two things:  1.  This test quite frequently fails.  2. It is not a test of ctakes/ytex code, but rather of jchronic behavior.
	// Getting Jenkins to complete a build is imo pretty important.
//	/**
//	 * Verify that date parsing with a manually created date works
//	 *
//	 * @throws UIMAException
//	 */
//	@Test
//	public void testParseDate() throws UIMAException {
//	    JCas jCas = JCasFactory.createJCasFromPath(TYPESYSTEM_URL.getPath());
//	    assertNotNull("Expecting a valid JCas object (second time loaded)", jCas);
//
//	    java.util.Date  dtExpected = new java.util.Date();
//	    String sExpected = dtExpected.toString();
//	    LOGGER.info(String.format("date to be annotated: %s", sExpected));
//
//	    jCas.setDocumentText(sExpected);
//
//	    // create the annotation
//	    DateAnnotation ctakesDate = new DateAnnotation(jCas);
//	    ctakesDate.setBegin(0);
//	    ctakesDate.setEnd(sExpected.length());
//	    ctakesDate.addToIndexes();
//	    DateAnnotator dateAnnotator = new DateAnnotator();
//	    dateAnnotator.dateType = Date.class.getName();
//	    dateAnnotator.process(jCas);
//
//	    LOGGER.info(String.format("Using org.apache.ctakes.ytex.uima.types.Date.type: %d", Date.type));
//	    AnnotationIndex<Annotation> ytexDates = jCas.getAnnotationIndex(Date.type);
//		for (FSIterator<Annotation> it = ytexDates.iterator(); it.hasNext(); ) {
//			Annotation a = it.next();
//			LOGGER.info(String.format("[%s]", a));
//		}
//		LOGGER.info(String.format("ytexDates.size: %d", ytexDates.size()));
//		assertEquals("Expecting ytexDates to have 2 elements", 2, ytexDates.size());
//
//	    // return the parsed
//	    String sParsed = ((Date)ytexDates.iterator().next()).getDate();
//		assertNotNull("Expected a parsed Date string", sParsed);
//		LOGGER.info(String.format("date from annotation: %s", sParsed));
//		java.util.Date dtParsed = null;
//	    try {
//		    dtParsed = new SimpleDateFormat(DateAnnotator.DATE_FORMAT).parse(sParsed);
//	    } catch (ParseException e) {
//		    assertFalse("Expected a real java.util.Date object", true);
//	    }
//	    // java.util.Date.equals is not advised. Comparing Date.getTime and ignoring miliseconds difference.
//	    assertNotNull("Expected a not NULL java.util.Date object", dtParsed);
//	    assertTrue("Expected what we converted .toString to be parsed",
//			       (dtExpected.getTime() - dtParsed.getTime()) < 1000 /*1 second*/);
//	}

}
