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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.ytex.uima.model.NamedEntityRegex;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Assert;
import org.junit.Test;
import org.apache.uima.fit.factory.JCasFactory;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * @author vgarla
 *
 */
public class NamedEntityRegexTest {

	/**
	 * Verify that date parsing with a manually created date works
	 * @throws UIMAException
	 * @throws Exception 
	 */
	@Test
	public void testRegex() throws UIMAException, IOException {
		NamedEntityRegexAnnotator ner = new NamedEntityRegexAnnotator();
		NamedEntityRegex nerex = new NamedEntityRegex();
		nerex.setCode("C00TEST");
		nerex.setContext("DEFAULT");
		nerex.setRegex("(?i)COUGH");
		ner.initRegexMap(Arrays.asList(nerex));
		
	    JCas jCas = JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
	    String text = "Dr. Doolitle asked patient\nto take a deep breath\nand exhale slowly.  Patient coughed.";
	    jCas.setDocumentText(text);
	    Segment s = new Segment(jCas);
	    s.setBegin(0);
	    s.setEnd(text.length());
	    s.setId("DEFAULT");
	    s.addToIndexes();
	    ner.process(jCas);
	    // run the analysis engine
	    AnnotationIndex<Annotation> mentions = jCas.getAnnotationIndex(EntityMention.type);
		Iterator<Annotation> iter =mentions.iterator();
		int emCount = 0;
		while(iter.hasNext()) {
			EntityMention em = (EntityMention)iter.next();
			System.out.println("[" + em.getCoveredText() + "]");
			emCount ++;
		}
		Assert.assertTrue(emCount == 1);
	}
}
