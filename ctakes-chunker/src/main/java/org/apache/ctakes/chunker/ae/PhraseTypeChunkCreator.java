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

import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * This chunker creator creates annotations of type org.apache.ctakes.typesystem.type.* and
 * sets the chunkType feature of the annotation to the passed in parameter chunkType. 
 * @author Philip
 * @see org.apache.ctakes.chunker.ae.type
 */

public class PhraseTypeChunkCreator implements ChunkCreator {

	
	public void initialize(UimaContext uimaContext) throws ResourceInitializationException {
		/* no initialization required */
	}

	public Annotation createChunk(JCas jCas, int start, int end, String chunkType) {
		Chunk chunk;
		if(chunkType.equals("ADJP")) {
			chunk = new ADJP(jCas, start, end);
		} else if(chunkType.equals("ADVP")) {
			chunk = new ADVP(jCas, start, end);
		} else if(chunkType.equals("CONJP")) {
			chunk = new CONJP(jCas, start, end);
		} else if(chunkType.equals("INTJ")) {
			chunk = new INTJ(jCas, start, end);
		} else if(chunkType.equals("LST")) {
			chunk = new LST(jCas, start, end);
		} else if(chunkType.equals("NP")) {
			chunk = new NP(jCas, start, end);
		} else if(chunkType.equals("PP")) {
			chunk = new PP(jCas, start, end);
		} else if(chunkType.equals("PRT")) {
			chunk = new PRT(jCas, start, end);
		} else if(chunkType.equals("SBAR")) {
			chunk = new SBAR(jCas, start, end);
		} else if(chunkType.equals("UCP")) {
			chunk = new UCP(jCas, start, end);
		} else if(chunkType.equals("VP")) {
			chunk = new VP(jCas, start, end);
		} else if(chunkType.equals("O")) {
			chunk = new O(jCas, start, end);
		} else {
			chunk = new Chunk(jCas, start, end);
		}
				
		chunk.setChunkType(chunkType);
		chunk.addToIndexes();
		return chunk;
	}


}
