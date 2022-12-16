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
package org.apache.ctakes.chunker.ae;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Implementations of this interface are responsible for creating chunk
 * annotations. A chunk could really be anything and not necessarily a phrase
 * type as found in shallow parsing.
 * 
 * @author Philip
 * @see Chunker#CHUNKER_CREATOR_CLASS_PARAM
 */
public interface ChunkCreator {

	public void initialize(UimaContext uimaContext) throws ResourceInitializationException;

	/**
	 * 
	 * @param jCas the view to which to add the created chunk.
	 * @param begin the beginning offset of the chunk
	 * @param end the ending offset of the chunk
	 * @param chunkType a string description of the chunk type - e.g. "NP"
	 * @return the annotation created by this method.  If no annotation is created, then return null.
	 * @throws AnalysisEngineProcessException
	 */
	public Annotation createChunk(JCas jCas, int begin, int end, String chunkType) throws AnalysisEngineProcessException;

}
