package org.apache.ctakes.context.tokenizer.ae;
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


import org.apache.ctakes.typesystem.type.textsem.FractionAnnotation;
import org.apache.ctakes.utils.test.TestUtil;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 
 * @author brittfitch
 *
 */
public class TestContextDependentTokenizerAnnotator {

	@Test
    public void testSimpleSegment() throws ResourceInitializationException {
		AnalysisEngine ae = TestUtil.getAE(new File("desc/analysis_engine/AggregateAE.xml"));
		JCas jCas = TestUtil.processAE(ae, "FOO 4.5 3.5-4.7 ");
		String[] expected = new String[]{"4.5", "3.5", "4.7"};
		List<FractionAnnotation> fracs = new ArrayList<>(JCasUtil.select(jCas, FractionAnnotation.class));
		
		assertEquals(3, fracs.size());
		
		for(int i=0; i<3; i++)
		{
			assertEquals(expected[i], fracs.get(i).getCoveredText());
		}
	}
}
