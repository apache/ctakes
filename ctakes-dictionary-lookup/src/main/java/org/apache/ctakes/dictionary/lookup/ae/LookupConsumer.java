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
package org.apache.ctakes.dictionary.lookup.ae;

import java.util.Iterator;

import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

/**
 * Defines how to consume the lookup hits produced by the LookupAnnotator.
 *
 * @author Mayo Clinic
 */
public interface LookupConsumer
{
	/**
	 * Consumes the hits produced by the LookupAnnotator. This typically means
	 * iterating over the hits and storing what's necessary to the JCas
	 * @param jcas CAS for storing data
	 * @param lookupHitItr
	 *            Iterator over LookupHit objects. These objects contain data
	 *            about the annotation span plus any associated metadata.
	 * @throws AnalysisEngineProcessException
	 */
	public void consumeHits(JCas jcas, Iterator<LookupHit> lookupHitItr) throws AnalysisEngineProcessException;
}
