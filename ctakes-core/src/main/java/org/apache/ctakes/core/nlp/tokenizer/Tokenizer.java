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
package org.apache.ctakes.core.nlp.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class used to break natural text into tokens. The token markup is external
 * to the text and is not embedded like XML. Character offset location is used
 * to identify the boundaries of a token.
 * 
 * @author Mayo Clinic
 */
public class Tokenizer {
	private OffsetComparator iv_offsetComp = new OffsetComparator();

	// key = hypenated String obj, value = freq Integer obj
	private Map<String, Integer> iv_hyphMap;

	private int iv_freqCutoff;

	/**
	 * Constructor
	 */
	public Tokenizer() {
	}

	/**
	 * Constructor
	 * 
	 * @param hyphMap
	 *            Map where key=hyphenated string (lower cased) value=freq
	 *            Integer
	 * 
	 * @param freqCutoff
	 *            frequency cutoff
	 */
	public Tokenizer(Map<String, Integer> hyphMap, int freqCutoff) {
		iv_hyphMap = hyphMap;
		iv_freqCutoff = freqCutoff;
	}

	/**
	 * Validate the structure of the hyphen map.
	 */
	public static void validateHyphenMap(Map<String, Integer> hyphMap)
			throws Exception {
		Iterator<String> keyItr = hyphMap.keySet().iterator();
		while (keyItr.hasNext()) {
			String key = keyItr.next();
			Object val = hyphMap.get(key);
			if (val == null) {
				throw new Exception(
						"Hyphen map is missing frequency data for key=" + key);
			}
			if ((val instanceof Integer) == false) {
				throw new Exception(
						"Hyphen map has non java.lang.Integer frequency data for key=" + key);
			}
		}
	}

	/**
	 * Tokenizes a string of text and outputs a list of Token objects in sorted
	 * order.
	 * 
	 * @param text
	 *            The text to tokenize.
	 * @return A list of Token objects sorted by the order they appear in the
	 *         text.
	 * @throws Exception
	 *             Thrown if an error occurs while tokenizing.
	 */
	public List<Token> tokenizeAndSort(String text) throws Exception {
		List<Token> tokenList = tokenize(text);

		// sort tokens by offset
		Collections.sort(tokenList, iv_offsetComp);

		return tokenList;
	}

	/**
	 * Tokenizes a string of text and outputs a list of Token objects. The list
	 * is not guaranteed to be sorted.
	 * 
	 * @param text The text to tokenize.
	 * @return A list of Token objects.
	 */
	public List<Token> tokenize(String text) throws Exception {
		try {
			List<Token> eolTokens = getEndOfLineTokens(text);

			// Break text into raw tokens (whitespace-delimited text)
			List<Token> tokens = getRawTokens(text);

			// Detect punctuation and symbols inside the raw tokens
			applyPunctSymbolRules(tokens, text);

			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				String tokenText = text.substring(token.getStartOffset(), token
						.getEndOffset());
				if (token.getType() != Token.TYPE_PUNCT) {
					if (isNumber(tokenText)) {
						token.setType(Token.TYPE_NUMBER);

						token.setIsInteger(isInteger(tokenText));
					}

					if (token.getType() == Token.TYPE_UNKNOWN) {
						// token must be a word if it's not classified yet
						token.setType(Token.TYPE_WORD);
					}

					if (token.getType() == Token.TYPE_WORD) {
						applyCapitalizationRules(token, tokenText);
						applyWordNumRules(token, tokenText);
					}
				}
			}
			tokens.addAll(eolTokens);

			// set text for tokens
			for (int i = 0; i < tokens.size(); i++) {
				Token t = tokens.get(i);
				t.setText(text.substring(t.getStartOffset(), t.getEndOffset()));
			}

			return tokens;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Internal Error with Tokenizer.");
		}
	}

	/**
	 * Applies punctuation/symbol rules to the given list of tokens. Tokens that
	 * are punctuation/symbols are marked as such. Tokens that contain
	 * punctuation/symbols inside them are split into multiple tokens, one of
	 * which is the inner punctuation/symbol token.
	 * 
	 * @param tokens List of tokens to apply rules to.
	 * @param text The original text.
	 */
	private void applyPunctSymbolRules(List<Token> tokens, String text) {
		List<Token> newTokenList = new ArrayList<Token>();
		List<Token> removeTokenList = new ArrayList<Token>();

		for (int tIndex = 0; tIndex < tokens.size(); tIndex++) {
			Token token = tokens.get(tIndex);
			String tokenText = text.substring(token.getStartOffset(), token
					.getEndOffset());

			if (tokenText.length() == 1) {
				char currentChar = tokenText.charAt(0);
				// token is only 1 character long, check if it's a symbol
				if (!isAlphabetLetterOrDigit(currentChar)) {
					if (isPunctuation(currentChar)) {
						token.setType(Token.TYPE_PUNCT);
					} 
					else {
						token.setType(Token.TYPE_SYMBOL);
					}
				}
				continue;
			}

			// punctuation/symbol at start of token
			int startCnt = processStartPunctSymbol(newTokenList, token,
					tokenText);
			// adjust original token to no longer include the punctuation/symbol
			token.setStartOffset(token.getStartOffset() + startCnt);

			// punctuation at end of token
			tokenText = text.substring(token.getStartOffset(), token
					.getEndOffset());
			int endCnt = processEndPunctSymbol(newTokenList, token, tokenText);
			// adjust original token to no longer include the punctuation/symbol
			token.setEndOffset(token.getEndOffset() - endCnt);

			// If the original token was only a punctuation or symbol,
			// and the start and end punctuation/symbol
			// has been stripped off, it's possible to now have an empty token
			// In that case, remove the empty token
			if (token.getStartOffset() == token.getEndOffset()) {
				removeTokenList.add(token);
			}

			// contractions
			tokenText = text.substring(token.getStartOffset(), token
					.getEndOffset());
			int aposIndex = tokenText.indexOf('\'');
			if (aposIndex != -1) {
				Token cpToken = null;
				String afterAposStr = tokenText.substring(aposIndex + 1,
						tokenText.length());
				if (afterAposStr.length() == 1) {
					// handle xxx'd (e.g. we'd)
					// handle xxx'm (e.g. I'm)
					// handle xxx's (e.g. it's)
					if (afterAposStr.equalsIgnoreCase("d")
							|| afterAposStr.equalsIgnoreCase("m")
							|| afterAposStr.equalsIgnoreCase("s")) {
						cpToken = new Token(token.getStartOffset() + aposIndex,
								token.getEndOffset());
					}
					// handle xxxn't (e.g. won't don't)
					else if (afterAposStr.equalsIgnoreCase("t")) {
						String beforeAposChar = tokenText.substring(
								aposIndex - 1, aposIndex);
						if (beforeAposChar.equalsIgnoreCase("n")) {
							cpToken = new Token(token.getStartOffset()
									+ aposIndex - 1, token.getEndOffset());
						}
					}
				} else if (afterAposStr.length() == 2) {
					// handle xxx're (e.g. they're)
					// handle xxx've (e.g. they've)
					// handle xxx'll (e.g. they'll)
					if (afterAposStr.equalsIgnoreCase("re")
							|| afterAposStr.equalsIgnoreCase("ve")
							|| afterAposStr.equalsIgnoreCase("ll")) {
						cpToken = new Token(token.getStartOffset() + aposIndex,
								token.getEndOffset());
					}
				}
				if (cpToken != null) {
					cpToken.setType(Token.TYPE_CONTRACTION);
					newTokenList.add(cpToken);
					// adjust original token to no longer include the
					// contraction
					// or possessive
					token.setEndOffset(cpToken.getStartOffset());
				}
			} else if (tokenText.equalsIgnoreCase("cannot")) {
				// special case where cannot needs to be split into can & not
				Token notToken = new Token(token.getStartOffset() + 3, token
						.getEndOffset());
				notToken.setType(Token.TYPE_WORD);
				newTokenList.add(notToken);
				// adjust original token to no longer include "not"
				token.setEndOffset(token.getStartOffset() + 3);
			}

			// punctuation inside the token
			tokenText = text.substring(token.getStartOffset(), token
					.getEndOffset());
			boolean foundSomethingInside = findPunctSymbolInsideToken(tokens,
					token, tokenText);
			// sourceforge bug tracker #3072902
			// if nothing left after remove the contraction, such as the line " n't "
			// then all of token was turned into a contraction token
			if (token.getEndOffset()== token.getStartOffset()) foundSomethingInside = true;
			if (foundSomethingInside) {
				removeTokenList.add(token);
			}
		}
		tokens.addAll(newTokenList);
		for (int i = 0; i < removeTokenList.size(); i++) {
			Token tokenToBeRemoved = removeTokenList.get(i);
			tokens.remove(tokenToBeRemoved);
		}
	}

	private int processStartPunctSymbol(List<Token> newTokenList, Token token,
			String tokenText) {
		int count = 0;
		for (int i = 0; i < tokenText.length(); i++) {
			char currentChar = tokenText.charAt(i);
			if (!isAlphabetLetterOrDigit(currentChar)) {
				Token t = new Token(token.getStartOffset() + i, token
						.getStartOffset()
						+ i + 1);

				if (isPunctuation(currentChar)) {
					t.setType(Token.TYPE_PUNCT);
				} else {
					t.setType(Token.TYPE_SYMBOL);
				}
				newTokenList.add(t);
				count++;
			} else { // encountered a letter or digit, stop
				return count;
			}
		}
		return count;
	}

	private int processEndPunctSymbol(List<Token> newTokenList, Token token,
			String tokenText) {
		int count = 0;
		for (int i = tokenText.length() - 1; i >= 0; i--) {
			char currentChar = tokenText.charAt(i);
			if (!isAlphabetLetterOrDigit(currentChar)) {
				Token t = new Token(token.getStartOffset() + i, token
						.getStartOffset()
						+ i + 1);

				if (isPunctuation(currentChar)) {
					t.setType(Token.TYPE_PUNCT);
				} else {
					t.setType(Token.TYPE_SYMBOL);
				}

				newTokenList.add(t);
				count++;
			} else { // encountered a letter or digit, stop
				return count;
			}
		}
		return count;
	}

	private int getFirstInsidePunctSymbol(String tokenText) {
		for (int i = 0; i < tokenText.length(); i++) {
			char currentChar = tokenText.charAt(i);
			
			if (currentChar == ',' && !isNumber(tokenText)) {
				return i;
			}
			if (currentChar == '.' && !isNumber(tokenText)) {
				return i;
			}
			

			if ((isAlphabetLetterOrDigit(currentChar) == false)
					&& (currentChar != '.') && (currentChar != ',')
					&& (currentChar != ':') && (currentChar != ';')) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Finds punctuation/symbols located inside a token. If found, the token is
	 * split into multiple Tokens. Note that the method is recursive.
	 * 
	 * @param tokens
	 * @param token
	 * @param tokenText
	 * @return
	 */
	private boolean findPunctSymbolInsideToken(List<Token> tokens, Token token,
			String tokenText) {
		int startOffset = token.getStartOffset();
		int punctSymbolOffset = getFirstInsidePunctSymbol(tokenText);
		if (punctSymbolOffset != -1) {
			char c = tokenText.charAt(punctSymbolOffset);

			// logic for hypenation
			if (c == '-') {
				if ((iv_hyphMap != null)
						&& iv_hyphMap.containsKey(tokenText.toLowerCase())) {
					int freq = ((Integer) iv_hyphMap.get(tokenText
							.toLowerCase())).intValue();
					if (freq > iv_freqCutoff) {
						if (!tokens.contains(token)) {
							tokens.add(token);
							return true;
						}
						return false;
					}
				}
			}

			Token t = new Token(startOffset + punctSymbolOffset, startOffset
					+ punctSymbolOffset + 1);
			if (isPunctuation(c)) {
				t.setType(Token.TYPE_PUNCT);
			} else {
				t.setType(Token.TYPE_SYMBOL);
			}

			tokens.add(t);
			if (startOffset != t.getStartOffset()) {
				Token leftToken = new Token(startOffset, t.getStartOffset());
				tokens.add(leftToken);
			}

			Token rightToken = new Token(t.getEndOffset(), token.getEndOffset());
			String rightTokenText = tokenText.substring(punctSymbolOffset + 1,
					tokenText.length());
			// recurse
			return findPunctSymbolInsideToken(tokens, rightToken,
					rightTokenText);
		} else {
			if (!tokens.contains(token)) {
				tokens.add(token);
				return true;
			}
			return false;
		}
	}

	private boolean isPunctuation(char c) {
		if ((c == ';') || (c == ':') || (c == ',') || (c == '.') || (c == '(')
				|| (c == ')') || (c == '[') || (c == ']') || (c == '{')
				|| (c == '}') || (c == '<') || (c == '>') || (c == '\'')
				|| (c == '"') || (c == '/') || (c == '\\') || (c == '-')) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isAlphabetLetterOrDigit(char c) {
		if (isAlphabetLetter(c))
			return true;
		if (isDigit(c))
			return true; // otherwise
		return false;
	}

	public boolean isAlphabetLetter(char c) {
		int unicode = Character.getNumericValue(c);
		if ((unicode >= 10) && (unicode <= 35))
			return true;
		else
			return false;
	}

	private boolean isDigit(char c) {
		int unicode = Character.getNumericValue(c);
		if ((unicode >= 0) && (unicode <= 9))
			return true;
		else
			return false;
	}

	/**
	 * Applies number rules to the given token.
	 * 
	 * @return True if the token is a number, false otherwise.
	 */
	public static boolean isNumber(String tokenText) {
		final char decimalPoint = '.';
		boolean foundDecimalPoint = false;
		int charsBeforeDecimal = 0;
		for (int i = tokenText.length() - 1; i >= 0; i--) {
			char currentChar = tokenText.charAt(i);
			if (Character.isDigit(currentChar) == false) {
				if ((currentChar == decimalPoint)
						&& (foundDecimalPoint == false)) {
					foundDecimalPoint = true;
					charsBeforeDecimal = 0;
					continue;
				} else if (currentChar == ',') { // commas are valid only
													// every 3 digits
					if (charsBeforeDecimal % 3 == 0) {
						continue;
					} else {
						return false;
					}
				} // otherwise it's a letter or punct
				return false;
			}
			charsBeforeDecimal++;
		}
		return true;
	}

	/**
	 * Given that the token text is a number, this method will determine if the
	 * number is an integer or not.
	 * 
	 * @param tokenText
	 * @return
	 */
	private boolean isInteger(String tokenText) {
		if (tokenText.indexOf('.') != -1) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Applies capitalization rules to the given token. This should normally
	 * only be used for tokens containing strictly text, but mixtures of
	 * letters, numbers, and symbols are allowed too.
	 * 
	 * @param token
	 * @param tokenText
	 */
	private void applyCapitalizationRules(Token token, String tokenText) {
		// true = upper case, false = lower case
		boolean[] uppercaseMask = new boolean[tokenText.length()];
		boolean isAllUppercase = true;
		boolean isAllLowercase = true;
		for (int i = 0; i < tokenText.length(); i++) {
			char currentChar = tokenText.charAt(i);
			uppercaseMask[i] = Character.isUpperCase(currentChar);
			if (uppercaseMask[i] == false)
				isAllUppercase = false;
			else
				isAllLowercase = false;
		}

		if (isAllLowercase) {
			token.setCaps(Token.CAPS_NONE);
		} else if (isAllUppercase) {
			token.setCaps(Token.CAPS_ALL);
		} else if (uppercaseMask[0] == true) {
			if (uppercaseMask.length == 1) {
				token.setCaps(Token.CAPS_FIRST_ONLY);
				return;
			}
			boolean isRestLowercase = true;
			for (int i = 1; i < uppercaseMask.length; i++) {
				if (uppercaseMask[i] == true)
					isRestLowercase = false;
			}
			if (isRestLowercase) {
				token.setCaps(Token.CAPS_FIRST_ONLY);
			} else {
				token.setCaps(Token.CAPS_MIXED);
			}
		} else {
			token.setCaps(Token.CAPS_MIXED);
		}
		return;
	}

	private void applyWordNumRules(Token token, String tokenText) {
		boolean[] digitMask = new boolean[tokenText.length()];
		boolean isAllLetters = true;
		for (int i = 0; i < tokenText.length(); i++) {
			char currentChar = tokenText.charAt(i);
			digitMask[i] = Character.isDigit(currentChar);
			if (digitMask[i] == true) {
				isAllLetters = false;
			}
		}

		if (isAllLetters) {
			token.setNumPosition(Token.NUM_NONE);
		} else if (digitMask[0] == true) {
			token.setNumPosition(Token.NUM_FIRST);
		} else if (digitMask[tokenText.length() - 1]) {
			token.setNumPosition(Token.NUM_LAST);
		} else {
			token.setNumPosition(Token.NUM_MIDDLE);
		}
		return;
	}

	/**
	 * Gets a list of tokens that mark end of a line.
	 * 
	 * @param text
	 * @return
	 */
	private List<Token> getEndOfLineTokens(String text) {
		final char crChar = '\r';
		final char nlChar = '\n';
		List<Token> eolTokens = new ArrayList<Token>();
		for (int i = 0; i < text.length(); i++) {
			char currentChar = text.charAt(i);
			/**
			 * Fixed: ID: 3307765 to handle windows CRLF \r\n new lines in Docs
			 */
			Token t = null;
			if (currentChar == nlChar) {
				t = new Token(i, i + 1);
			} else if (currentChar == crChar) {
				if ((i + 1) < text.length()) {
					char nextChar = text.charAt(i + 1);
					if (nextChar == nlChar) {
						t = new Token(i, i + 2);
						i++;
					} else {
						t = new Token(i, i + 1);
					}
				} else {
					t = new Token(i, i + 1);
				}
			}

			if (t != null) {
				t.setType(Token.TYPE_EOL);
				eolTokens.add(t);
			}

		}
		return eolTokens;
	}

	/**
	 * Text is split based on whitespace into raw tokens. A raw token is defined
	 * as a span of text with no identified type.
	 * 
	 * @param text
	 * @return
	 */
	private List<Token> getRawTokens(String text) {
		final char wsChar = ' ';
		final char tabChar = '\t';
		final char newlineChar = '\n';
		final char crChar = '\r';		
		boolean insideText = false;
		int startIndex = 0;
		int endIndex = 0;
		List<Token> rawTokens = new ArrayList<Token>();
		for (int i = 0; i < text.length(); i++) {
			char currentChar = text.charAt(i);
			if ((currentChar != wsChar) && (currentChar != tabChar)
					&& (currentChar != newlineChar) && (currentChar != crChar)) {
				if (insideText == false) {
					insideText = true;
					startIndex = i;
				}
			} else {
				if (insideText == true) {
					insideText = false;
					endIndex = i;
					Token t = new Token(startIndex, endIndex);
					rawTokens.add(t);
				}
			}
		} // capture last token, that may not have whitespace after it
		if (insideText) {
			insideText = false;
			endIndex = text.length();
			Token t = new Token(startIndex, endIndex);
			rawTokens.add(t);
		}

		return rawTokens;
	}

}
