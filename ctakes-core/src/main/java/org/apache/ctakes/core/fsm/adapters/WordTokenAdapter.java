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
package org.apache.ctakes.core.fsm.adapters;

import org.apache.ctakes.core.ae.TokenizerAnnotator;
import org.apache.ctakes.core.fsm.token.WordToken;


/**
 * Adapts JCas token annotation to interface expected by the Context Dependent
 * Tokenizer.
 * 
 * @author Mayo Clinic
 * 
 */
public class WordTokenAdapter extends TextTokenAdapter implements WordToken
{
	private org.apache.ctakes.typesystem.type.syntax.WordToken iv_wta;

	public WordTokenAdapter(org.apache.ctakes.typesystem.type.syntax.WordToken wta)
	{
		super(wta);
		iv_wta = wta;
	}

	public byte getCaps()
	{
		int caps = iv_wta.getCapitalization();
		switch (caps)
		{
		case TokenizerAnnotator.TOKEN_CAP_ALL:
			return WordToken.CAPS_ALL;
		case TokenizerAnnotator.TOKEN_CAP_FIRST_ONLY:
			return WordToken.CAPS_FIRST_ONLY;
		case TokenizerAnnotator.TOKEN_CAP_MIXED:
			return WordToken.CAPS_MIXED;
		case TokenizerAnnotator.TOKEN_CAP_NONE:
			return WordToken.CAPS_NONE;
		default:
			return WordToken.CAPS_UNKNOWN;
		}
	}

	public byte getNumPosition()
	{
		int numPos = iv_wta.getNumPosition();
		switch (numPos)
		{
		case TokenizerAnnotator.TOKEN_NUM_POS_FIRST:
			return WordToken.NUM_FIRST;
		case TokenizerAnnotator.TOKEN_NUM_POS_LAST:
			return WordToken.NUM_LAST;
		case TokenizerAnnotator.TOKEN_NUM_POS_MIDDLE:
			return WordToken.NUM_MIDDLE;
		default:
			return WordToken.NUM_NONE;
		}
	}
}
