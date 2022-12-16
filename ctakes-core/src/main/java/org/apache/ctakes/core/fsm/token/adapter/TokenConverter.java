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
package org.apache.ctakes.core.fsm.token.adapter;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.core.fsm.token.DecimalToken;
import org.apache.ctakes.core.fsm.token.IntegerToken;
import org.apache.ctakes.core.fsm.token.PunctuationToken;
import org.apache.ctakes.core.fsm.token.WordToken;
import org.apache.ctakes.core.nlp.tokenizer.Token;


public class TokenConverter {

	public static List<BaseToken> convertTokens(List<Token> tokens) {
		List<BaseToken> baseTokens = new ArrayList<BaseToken>();

		for (int i = 0; i < tokens.size(); i++) {
			Token t = (Token) tokens.get(i);
			switch (t.getType()) {
			case Token.TYPE_WORD:
				WordToken wt = new WordTokenAdapter(t);
				baseTokens.add(wt);
				break;
			case Token.TYPE_PUNCT:
				PunctuationToken pt = new PunctuationTokenAdapter(t);
				baseTokens.add(pt);
				break;
			case Token.TYPE_NUMBER:
				if (t.isInteger()) {
					IntegerToken it = new IntegerTokenAdapter(t);
					baseTokens.add(it);
				} else {
					DecimalToken dt = new DecimalTokenAdapter(t);
					baseTokens.add(dt);
				}
				break;
			case Token.TYPE_EOL:
				break;
			case Token.TYPE_CONTRACTION:
				break;
			}
		}

		return baseTokens;
	}

}
