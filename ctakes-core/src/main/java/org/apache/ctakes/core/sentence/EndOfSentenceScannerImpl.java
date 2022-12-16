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
package org.apache.ctakes.core.sentence;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.sentdetect.EndOfSentenceScanner;

/**
 * End of Sentence scanner with more candidate end-of-sentence
 * characters than the default.
 * @author Mayo Clinic
 */
public class EndOfSentenceScannerImpl implements EndOfSentenceScanner {

    private static final char[] eosCandidates =  {'.', '!', ')', ']', '>', '\"', ':', ';'}; // CTAKES-227

	public EndOfSentenceScannerImpl() {
        super();
	}

	public char[] getEndOfSentenceCharacters() {
		return eosCandidates;
		
	}
	/**
	 * @see opennlp.tools.sentdetect.EndOfSentenceScanner#getPositions(java.lang.String)
	 */
	public List<Integer> getPositions(String s) {
        return getPositions(s.toCharArray());
	}

	/**
	 * @see opennlp.tools.sentdetect.EndOfSentenceScanner#getPositions(java.lang.StringBuffer)
	 */
	public List<Integer> getPositions(StringBuffer sb) {
        return getPositions(sb.toString().toCharArray());
	}

	/**
	 * @see opennlp.tools.sentdetect.EndOfSentenceScanner#getPositions(char[])
	 */
	public List<Integer> getPositions(char[] cb) {
		List<Integer> positions = new ArrayList<Integer>();

		for (int i=0; i<cb.length; i++) { // for each character in buffer
			for (int j=0; j<eosCandidates.length; j++) { // for each eosCandidate
				if (cb[i]==eosCandidates[j]) { 
					positions.add(new Integer(i)); // TODO - don't always create new, use a pool
					break; // can't match others if it matched eosCandidates[j]
				}
			}
		}
		 
		return positions;
	}

}
