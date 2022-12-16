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

import org.apache.ctakes.core.fsm.token.CharacterToken;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;

/**
 * Adapts JCas token annotation to interface expected by the Context Dependent
 * Tokenizer.
 * 
 * @author Mayo Clinic
 * 
 */
public class CharacterTokenAdapter extends BaseTokenAdapter implements
		CharacterToken
{
	private char iv_char;
	
	public CharacterTokenAdapter(BaseToken bta)
	{
		super(bta);
		iv_char = bta.getCoveredText().charAt(0);
	}
	
	public char getChar()
	{
		return iv_char;
	}
}
