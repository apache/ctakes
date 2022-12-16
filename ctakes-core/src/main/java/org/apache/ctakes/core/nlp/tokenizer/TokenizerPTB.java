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

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.APOSTROPHE;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.COMMA;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.CR;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.HYPHEN_OR_MINUS_SIGN;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.NEWLINE;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.PERIOD;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.isPunctuation;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;

import org.apache.ctakes.core.ae.TokenizerAnnotator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ContractionToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.SymbolToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;

/**
 * A class used to break natural text into tokens following PTB rules.
 * See Supplementary Guidelines for ETTB 2.0
 * dated April 6th, 2009. 
 * https://www.ldc.upenn.edu/sites/www.ldc.upenn.edu/files/etb-supplementary-guidelines-2009-addendum.pdf
 * The token markup is external to the text and is not embedded.
 * Character offset location is used to identify the boundaries of a token.
 * 
 * @author Mayo Clinic
 */
public class TokenizerPTB {
    
    	/**
	 * Constructor
	 */
	public TokenizerPTB() {
	}

	
	static final String [] emptyStringList = new String[0];
	static final ArrayList<BaseToken> emptyTokenList = new ArrayList<BaseToken>();
	    


	/**
	 * Tokenize text that starts at offset offsetAdjustment within the complete text
	 * @param textSegment the text to tokenize
	 * @param offsetAdjustment what to add to all offsets within textSegment to make them be offsets from the start of the text for the jcas
	 * @param includeTextNotJustOffsets whether to copy the text covered by this token into the token object itself
	 * @return the list of new tokens
	 */
	public List<?> tokenizeTextSegment(JCas jcas, String textSegment, int offsetAdjustment, boolean includeTextNotJustOffsets) {
		
	    String lowerCasedText = textSegment.toLowerCase();
		ArrayList<Object> tokens = new ArrayList<Object>();
		Class<? extends BaseToken> tokenClass = null; 
		
		// if input was null or empty, return empty token list
		if (textSegment==null || textSegment.length()==0) return emptyTokenList;
		
		// find first character of a token
		int currentPosition = 0;
		currentPosition = findFirstCharOfNextToken(textSegment, currentPosition);
		// if input contained only white space but not even any newlines, return empty token list 
		if (currentPosition < 0) return emptyTokenList;
		
		while ((currentPosition = findFirstCharOfNextToken(textSegment, currentPosition))>=0) {
		
			// get current character and the one after that, which is used in making a number
			// of decisions. if at the end of the input, use '\0' to represent the non-existent 
			// character after the current one just to avoid dealing with null
			char firstCharOfToken = textSegment.charAt(currentPosition);
			int NOT_SET_INDICATOR = -999;
			int tokenLen = NOT_SET_INDICATOR; // should set it below to a real value
			Object bta;
			


			if (currentPosition+1 >= textSegment.length()) {
            			// we found the start of a token, but it was the last character in the input,
            			// so it is a 1-character token
            			tokenLen = 1;
            			tokenClass = null; // null indicates that we don't know yet what the class is

			} 
			
			// else we have at least 2 characters to consider
			
			else if (isWhitespace(textSegment.charAt(currentPosition+1))) {
			    // Since the following character is whitespace, and the current character
			    // is the first character of a token, the current character is a one-character token
			    tokenLen = 1; 
			    tokenClass = null; // null indicates that we don't know yet what the class is

			}
			
			else if (firstCharOfToken == NEWLINE) {
				tokenLen = 1; 
				tokenClass = NewlineToken.class;
			}
			
			else if (firstCharOfToken == CR) {
			
			    char peekAhead;
			    peekAhead = textSegment.charAt(currentPosition+1);
			    if (peekAhead != NEWLINE) { 
				tokenLen = 1; 
				tokenClass = NewlineToken.class;
			    }
			    else { 
				// create CR followed by LF as single end-of-line marker
				tokenLen = 2; // skip an extra one to skip both the CR and the LF
				tokenClass = NewlineToken.class;
			    }
			
			
			}
			
			else if (firstCharOfToken==PERIOD) {
			    // check if decimal number without the leading digits
			    int len = getLengthIfIsNumberThatStartsWithPeriod(currentPosition, textSegment);
			    if (len > 0) {
				tokenClass = NumToken.class;
				tokenLen = len; 
			    } 

			    else if (isEllipsis(currentPosition, textSegment)) {
				tokenLen = 3; 
				tokenClass = PunctuationToken.class;
			    } else {
				// Abbreviation does not start with period, and not part of some other token, so it is punctuation
				tokenLen = 1; 
				tokenClass = PunctuationToken.class;
			    }

			} 

			else if (firstCharOfToken==HYPHEN_OR_MINUS_SIGN) {
			    // If it's the first character of a token, then this is not a hyphenated term that
			    // was supposed to be kept as one token, or we would have included it in the previous token
			    // Also telephone numbers do not start with a dash
			    // So assume the hyphen/dash char is a one-character token like in 5-6 or in -400
			    tokenLen = 1; 
			    tokenClass = PunctuationToken.class;
			} 
			
			else if (firstCharOfToken==APOSTROPHE) { 
			    // "can't" is not part of this case because the n is the start of the second token
			    // The 've part of should've is not handled here, when something like should've or he'll
			    // is found, 2 tokens are created (elsewhere)
			    
			    // Check if start of a Name
			    int len = getLengthIfNameStartingWithApostrophe(currentPosition, textSegment);
			    if (len > 0) {
				tokenLen = len;
				tokenClass = WordToken.class;
			    } else if (ContractionsPTB.isContractionThatStartsWithApostrophe(currentPosition, lowerCasedText)) { 
				// 'tis and 'twas which get tokenized as  "'t is"  and  "'t was"
				tokenLen = 2;
				tokenClass = ContractionToken.class;
				// the "is" or "was" part will become a token on the next iteration
				// TODO potential place to add some self-checking code
			    } else { // is separate punctuation mark
				tokenLen = 1;   
				tokenClass = PunctuationToken.class;
			    }
			} 
			
			else if (isPunctuation(firstCharOfToken)) { // other than any handled above
			    // Already handled minus sign and leading period (which could be part of a decimal)
			    // Since not processing 'web-text', no need to look for things like :)
			    // so is some type of 1-character punctuation token
			    tokenLen = 1; 
			    tokenClass = PunctuationToken.class;
			} 
			
			else if (isLetterOrDigit(firstCharOfToken)) {

			    boolean obviouslyIsWord = true; // until we find a non alphanum before a whitespace
			    boolean obviouslyIsNumber = true; // until we find a non digit before a whitespace
			    int nextWhitespaceOrEndOfSegment = -1;
			    int nextNonLetterOrNonDigit = -1;
			    int nextNonLetterDigitApostrophe = -1;
			    int nextNonTelephoneOrPostalChar = -1; // digits and dash aka hyphen
			    int nextNonNumericChar = -1; // 9,876.012345  is an example with all the numeric chars 
			    int nextNonDigit = -1;
			    
			    // First check the easy case - if just letters and digits until next whitespace (or until end of segment)
			    // then that is a word or a number, can skip all the other logic to check for +hyphens
			    // or contractions etc
			    int i = currentPosition;
			    char ch;
			    do {
				ch = textSegment.charAt(i);
				if (isWhitespace(ch)) {
				    if (nextNonLetterOrNonDigit < 0) nextNonLetterOrNonDigit = i;
				    if (nextNonLetterDigitApostrophe < 0) nextNonLetterDigitApostrophe = i;
				    if (nextNonDigit < 0) nextNonDigit = i;
				    if (nextNonTelephoneOrPostalChar < 0) nextNonTelephoneOrPostalChar = i;
				    if (nextNonNumericChar < 0) nextNonNumericChar = i;
				    nextWhitespaceOrEndOfSegment = i;
				} else if (!isLetterOrDigit(ch)) {
				    obviouslyIsWord = false; // not sure if it will be word all the way to whitespace
				    obviouslyIsNumber = false; // not sure if it will be number all the way to whitespace
				    if (nextNonLetterOrNonDigit < 0) nextNonLetterOrNonDigit = i;
				    if (nextNonLetterDigitApostrophe < 0 && ch!=APOSTROPHE) {
					nextNonLetterDigitApostrophe = i;
				    }
				    if (nextNonDigit < 0) nextNonDigit = i;
				    if (nextNonTelephoneOrPostalChar < 0 && !isTelephoneNumberChar(ch)) {
					nextNonTelephoneOrPostalChar = i;
				    }
				    if (nextNonNumericChar < 0 && !isNumericChar(ch)) {
					nextNonNumericChar = i;
				    }
				    // don't break here though, keep going to set nextWhitespace correctly for other uses
				} else if (!isDigit(ch)) {
				    obviouslyIsNumber = false; // not sure if it will be number all the way to whitespace
				    // since passed nextNonLetterOrNonDigit test above, must be letter, so nextNonLetterOrNonDigit is not changed here
				    // since passed !isLetterOrDigit test above, must be letter, so nextNonLetterDigitApostrophe is not changed here
				    if (nextNonDigit < 0) nextNonDigit = i;
				    if (nextNonTelephoneOrPostalChar < 0 && !isTelephoneNumberChar(ch)) {
					nextNonTelephoneOrPostalChar = i;
				    }
				    if (nextNonNumericChar < 0 && !isNumericChar(ch)) {
					nextNonNumericChar = i;
				    }
				} else {
				    // else is a digit, none of the flags need to be set for digit characters.
				}

				i++;
			    } while (i < textSegment.length() && !isWhitespace(ch));

			    if (i>=textSegment.length()) {
				if (nextWhitespaceOrEndOfSegment < 0) nextWhitespaceOrEndOfSegment = textSegment.length();
				if (nextNonLetterOrNonDigit < 0) nextNonLetterOrNonDigit = textSegment.length();
				if (nextNonLetterDigitApostrophe < 0) nextNonLetterDigitApostrophe = textSegment.length();
				if (nextNonTelephoneOrPostalChar < 0) nextNonTelephoneOrPostalChar = textSegment.length();
				if (nextNonNumericChar < 0) nextNonNumericChar = textSegment.length();
			    }
			    //System.err.println("nextWhitespaceOrEndOfSegment = " + nextWhitespaceOrEndOfSegment);

			    
			    if (obviouslyIsNumber) {
				    tokenLen = nextWhitespaceOrEndOfSegment - currentPosition;
				    tokenClass = NumToken.class;
			    } else if (obviouslyIsWord) {
				// Check for things like "cannot" and "gonna" that appear to be one token but
				// are supposed to be more than one according to PTB rules.
				String lowerCasedSubstring = textSegment.substring(currentPosition, nextWhitespaceOrEndOfSegment).toLowerCase();
				int len = ContractionsPTB.lenOfFirstTokenInContraction(lowerCasedSubstring);
				if (len > 0) { // is a contraction that doesn't contain an apostrophe, like "gonna", create WordToken for first part, 
				    		// and create ContractionToken for other token(s)  
				    tokenLen = len;
				    tokenClass = WordToken.class;
				    bta = createToken(tokenClass, textSegment, jcas, currentPosition, currentPosition+tokenLen, offsetAdjustment);
				    tokens.add(bta);
				    currentPosition+=tokenLen; // currentPosition
				    
				    len = ContractionsPTB.lenOfSecondTokenInContraction(lowerCasedSubstring);
				    
				    tokenLen = len;
				    tokenClass = ContractionToken.class;
				    
				    len = ContractionsPTB.lenOfThirdTokenInContraction(lowerCasedSubstring);
				    if (len>0) { // if there is a 3rd, create the 2nd and set up for the 3rd to be created later
					bta = createToken(tokenClass, textSegment, jcas, currentPosition, currentPosition+tokenLen, offsetAdjustment);
					tokens.add(bta);
					currentPosition+=tokenLen; // currentPosition

					tokenLen = len;
					tokenClass = ContractionToken.class;
				    }
				} else {
				    tokenLen = nextWhitespaceOrEndOfSegment - currentPosition;
				    tokenClass = WordToken.class;
				}
			    } else { // Still within the "isLetterOrDigit(firstCharOfToken)" but not obviously number or word

				int len;

				
				ContractionResult cr;
				
				// Not sure what the token is, the token could extend to 
				// include all to the end of an email address, 
				// or include all to the end of a URL, 
				// or include all to the end of a URL, 
				// or through the next period (for an abbreviation)
				// or to the next hyphen, 
				// or beyond, 
				// or to the next whitespace (note already handle case of all alphanums to whitespace
				// or to the end of input (note already handle case of all alphanums to end of input
				// or the next apostrophe (for a most contractions) 
				// or until "n't" for such contractions
				// or the next other punctuation symbol
				// or beyond (for 80's)
				// or could include some punctuation like 3,245.51

				// Need to check for things like 80's before checking for contractions or else 80's looks like a contraction
			        if (nextNonLetterOrNonDigit < lowerCasedText.length() && lowerCasedText.charAt(nextNonLetterOrNonDigit)==APOSTROPHE) {
			            String lowerCasedSubstring = lowerCasedText.substring(currentPosition, nextWhitespaceOrEndOfSegment);
			            len = ContractionsPTB.tokenLengthCheckingForSingleQuoteWordsToKeepTogether(lowerCasedSubstring);
			            if (len > nextNonLetterOrNonDigit-currentPosition) { // if keeping the apostrophe attached
			        	tokenLen = len;
			        	tokenClass = wordTokenOrNumToken(lowerCasedText, currentPosition, tokenLen);
			            } // else let contraction checking later determine what to do

			            
			        }
			        if (tokenLen == NOT_SET_INDICATOR) { // not found yet
			            if ((cr = ContractionsPTB.getLengthIfNextApostIsMiddleOfContraction(currentPosition, nextNonLetterOrNonDigit, lowerCasedText)) != null) {
			        	len = cr.getWordTokenLen();
			        	tokenLen = len;
			        	tokenClass = WordToken.class;
			        	char c = lowerCasedText.charAt(currentPosition+len);
			        	if (c=='n' || c==APOSTROPHE) { // if a "n't" contraction or a contraction where contraction token starts with '
			        	    if (tokenLen < 0) throw new RuntimeException("c = " + c + "tokenLen = " + tokenLen + " currentPosition = " + currentPosition);
			        	    // First create the WordToken (no apostrophe)
			        	    if(tokenLen > 0){
			        	      bta = createToken(tokenClass, textSegment, jcas, currentPosition, currentPosition+tokenLen, offsetAdjustment);
			        	      //System.out.println("bta = " + bta + " class = " + bta.getClass() + " tokenLen = " + tokenLen + " currentPosition = " + currentPosition);
			        	      tokens.add(bta);
			        	      currentPosition+=tokenLen; // currentPosition
			        	    }
			        	    // Set up to create the second token, for other contractions, the next token will start with an 
			        	    // apostrophe and be handled above... but for "n't" contractions, next token won't start with apostrophe
			        	    // so just go ahead and handle it here instead of having to keep track of previous 
			        	    // and handle n't in next loop.
			        	    tokenLen = cr.getContractionTokenLen();
			        	    // if (tokenLen!=3) throw new RuntimeException("getContractionTokenLen != 3 for n't");
			        	    tokenClass = ContractionToken.class;
			        	} else {
			        	    throw new RuntimeException("ERROR: getLengthIfNextApostIsMiddleOfContraction returned " + len + " but the character (" + c +") after that is not 'n' or apostrophe ");
			        	}
				    
				    
			            } else if ((len = lenIfIsTelephoneNumber(currentPosition, lowerCasedText, nextNonTelephoneOrPostalChar)) > 0) {
			        	tokenLen = len;
			        	tokenClass = WordToken.class;
			            } else if ((len = lenIfIsPostalCode(currentPosition, lowerCasedText, nextNonTelephoneOrPostalChar)) > 0) {
			        	tokenLen = len;
			        	tokenClass = WordToken.class;
			            } else if ((len = lenIfIsUrl(currentPosition, lowerCasedText, nextWhitespaceOrEndOfSegment)) > 0) {
			        	tokenLen = len;
			        	tokenClass = WordToken.class;
			            } else if ((len = lenIfIsEmailAddress(currentPosition, lowerCasedText, nextWhitespaceOrEndOfSegment)) > 0) {
			        	tokenLen = len;
			        	tokenClass = WordToken.class;
			            } else if ((len = lenIfIsAbbreviation(currentPosition, textSegment, nextWhitespaceOrEndOfSegment)) > 0) {
			        	tokenLen = len;
			        	tokenClass = WordToken.class;
			            } else { // Still within the "isLetterOrDigit(firstCharOfToken)".
			        	// not obviously a word or number (already checked those)
			        	// and not Url, EmailAddress, or Abbreviation
			        	// There could be a hyphen before the next white space,
			        	// or a symbol before the next whitespace
			        	// or apostrophe like in 80's or P'yongyang (one token each) or James' or Ted's (2 tokens each)
			        	// Take alphanums, but consider hyphenated words and names with apostrophes 
			        	// and consider tele numbers and postal codes

			        	//				    if (true) { // TBD comment out this debug code
			        	//					System.out.println("lowerCasedSubstring = " + quoted(lowerCasedSubstring));
			        	//					System.out.println("currentPosition = " + currentPosition);
			        	//					System.out.println("nextWhitespaceOrEndOfSegment = " + nextWhitespaceOrEndOfSegment);
			        	//					System.out.println("nextNonLetterOrNonDigit = " + nextNonLetterOrNonDigit);
			        	//					System.out.println("nextNonLetterDigitApostrophe = " + nextNonLetterDigitApostrophe);
			        	//				    }

			        	if (nextNonLetterOrNonDigit<lowerCasedText.length() && lowerCasedText.charAt(nextNonLetterOrNonDigit)==HYPHEN_OR_MINUS_SIGN) {
			        	    // telephone numbers and postal codes handled above already
				            String lowerCasedSubstring = lowerCasedText.substring(currentPosition, nextWhitespaceOrEndOfSegment);
			        	    len = HyphenatedPTB.tokenLengthCheckingForHyphenatedTerms(lowerCasedSubstring);
			        	    tokenLen = len;
			        	    if (tokenLen < 0) throw new RuntimeException("tokenLen = " + tokenLen + " currentPosition = " + currentPosition + " nextNonLetterOrNonDigit = " + nextNonLetterOrNonDigit);
			        	    tokenClass = wordTokenOrNumToken(lowerCasedText, currentPosition, tokenLen);
			        	} else if (nextNonNumericChar > 0 && (len = lenIfIsNumberContainingComma(currentPosition, lowerCasedText, nextNonNumericChar)) > 0) {
			        	    tokenLen = len;
			        	    tokenClass = NumToken.class;
			        	} else if (nextNonLetterDigitApostrophe < lowerCasedText.length() && lowerCasedText.charAt(nextNonLetterDigitApostrophe)==PERIOD) {
			        	    // see if is a number with a decimal place (without commas, comma-containing numbers are handled above)
			        	    if (nextNonDigit==lowerCasedText.length()-1) {
			        		// end of sentence, don't include the period as part of the number, count it as end of sentence marker (punctuation)
			        		tokenLen = nextNonDigit - currentPosition;
			        		//if (tokenLen<1) throw new RuntimeException("Period at end of sentence " + nextNonDigit + " " + nextNonLetterDigitApostrophe+" "+tokenLen+ " " + lowerCasedText);
			        		tokenClass = NumToken.class;
			        	    } else if (nextNonLetterDigitApostrophe==nextNonDigit) {
			        		// if not end of sentence, do include period (decimal point) in the NumToken
			        		tokenLen = nextNonDigit + 1 + getLenToNextNonDigit(lowerCasedText, nextNonDigit+1) - currentPosition;
			        		tokenClass = NumToken.class;
			        	    }
			        	    else {
			        		// something like 2J3. which is not a number or 2'3.
			        		tokenLen = nextNonLetterOrNonDigit - currentPosition;
			        		tokenClass = wordTokenOrNumToken(lowerCasedText, currentPosition, tokenLen);
			        	    }
			        	} else { // breaking character is not - character and not ' character, so stop there
			        	    tokenLen = nextNonLetterOrNonDigit - currentPosition;
			        	    tokenClass = wordTokenOrNumToken(lowerCasedText, currentPosition, tokenLen);
			        	}
			        	//} else {
			        	//    throw new UnsupportedOperationException("nextNonLetterDigitApostrophe = " + nextNonLetterDigitApostrophe);
			        	//}

			            }

			        }
			    }
			    
			} else { // some other symbol or punctuation not included in isPunctuation
			    // Since not processing 'web-text', no need to look for things like :)
			    // so it is some type of 1-character symbol token
			    tokenLen = 1; 
			    tokenClass = SymbolToken.class;
			}
			
			// add the token created
			if (tokenLen < 0) throw new RuntimeException("tokenLen = " + tokenLen + " currentPosition = " + currentPosition);
			bta = createToken(tokenClass, textSegment, jcas, currentPosition, currentPosition+tokenLen, offsetAdjustment);
			//System.out.println("bta = " + bta + " class = " + bta.getClass() + " tokenLen = " + tokenLen + " currentPosition = " + currentPosition);
			tokens.add(bta);
			currentPosition+=tokenLen; // currentPosition

		} // end while loop
		
		return tokens;
		
	}

	/**
	 * Tokenize a string that is assumed to be the entire document (or at least to start at 0)
	 * @param text the String to tokenize
	 * @return the list of new tokens
	 */
	public List<?> tokenize(String text) {
	    int offsetAdjustment = 0;
	    JCas jcas = null;
	    return tokenizeTextSegment(jcas, text, offsetAdjustment, true);
	}
	
	
	private static char DASH = '-';


	/**
	 * such as -4,012.67 or 5 or 5.5 or 4,000,153
	 * @param currentPosition
	 * @param text
	 * @param nextNonNumericChar
	 * @return
	 */
	private int lenIfIsNumberContainingComma(int currentPosition, String text, int nextNonNumericChar) {
	    String s = text.substring(0, nextNonNumericChar); // use substring so don't search until end of entire document
	    int commaPosition = s.indexOf(COMMA, currentPosition);
	    if (commaPosition<0) return -1;
	    if (commaPosition>nextNonNumericChar) return -1;
	    int len = -1;
	    
	    int periodPosition = s.indexOf(PERIOD, currentPosition);
	    int endOfWholeNumberPart = periodPosition;
	    if (endOfWholeNumberPart<0) endOfWholeNumberPart = s.length();
	    // the whole number part can contain commas as long as there are exactly 3 digits after each comma
	    if (commaPosition>endOfWholeNumberPart) return -1; // if comma appears after the decimal point, then no commas in the whole-number-part
	    if (commaPosition==0) return -1; // can't start with comma

	    int position = commaPosition;  
	    
	    boolean didNotFindExactlyThreeDigitsAfterComma = false;
	    
	    while (!didNotFindExactlyThreeDigitsAfterComma) {
		len = position-currentPosition; // don't include the comma unless also can include next 3 digits
		if (position<endOfWholeNumberPart && s.charAt(position)==COMMA) {
		    position++;
		}
		for (int i=0; i<3; i++) { // 3 digits after the comma if comma is part of a number
		    if (position<endOfWholeNumberPart && isDigit(s.charAt(position))) {
			position++;
		    } else {
			didNotFindExactlyThreeDigitsAfterComma = true;
		    }
		}
		if (position<endOfWholeNumberPart && isDigit(s.charAt(position))) { // can't have 4 digits after comma like 3,4567  
		    didNotFindExactlyThreeDigitsAfterComma = true; 
		}
	    }	    
	    
	    if (len <= 0)  return -1;
	    
	    // See if there is a decimal point that can continue the number, such as  3,456.56  or 4,012.
	    // But if the sentences ends with the period that follows the whole_number_part, count it as the sentence marker 
	    // not as part of the number
	    if (periodPosition != text.length()-1 && // not the final period of a sentence
		    periodPosition == currentPosition+len) { // but the period does appear right after the whole_number_part
		len++; 
		while (len<nextNonNumericChar-currentPosition && isDigit(s.charAt(currentPosition+len))) {
		    len++;
		}
	    }
	    
	    return len;
	}

	
	private int lenIfIsPostalCode(int currentPosition, String text, int nextNonPostalCodeChar) {
	    if (nextNonPostalCodeChar < 0) return nextNonPostalCodeChar;
	    
	    int len = nextNonPostalCodeChar-currentPosition;
	    
	    String s = text.substring(currentPosition, nextNonPostalCodeChar);
	    // 55901-0000
	    
	    if (len == 10) { // 55901-0001
		if (!isDigit(s.charAt(0))) return -1;
		if (!isDigit(s.charAt(1))) return -1;
		if (!isDigit(s.charAt(2))) return -1;
		if (!isDigit(s.charAt(3))) return -1;
		if (!isDigit(s.charAt(4))) return -1;
		if (s.charAt(5)!=DASH) return -1;
		if (!isDigit(s.charAt(6))) return -1;
		if (!isDigit(s.charAt(7))) return -1;
		if (!isDigit(s.charAt(8))) return -1;
		if (!isDigit(s.charAt(9))) return -1;
		return len;
	    } else {
		return -1;
	    }
	    
	}

	
	private int lenIfIsTelephoneNumber(int currentPosition, String text, int nextNonTelephoneNumberChar) {
	    
	    if (nextNonTelephoneNumberChar < 0) return nextNonTelephoneNumberChar;
	    
	    int len = nextNonTelephoneNumberChar-currentPosition;
	    
	    String s = text.substring(currentPosition, nextNonTelephoneNumberChar);
	    // extension like 4-5555
	    // or without area code like 555-1212
	    // or with area code 507-555-1212
	    // or with 1, like 1-507-555-1212
	    // or like example in guidelines like 02-2348-2192
	    
	    if (len==6) {
		if (!isDigit(s.charAt(0))) return -1;
		if (s.charAt(1)!=DASH) return -1;
		if (!isDigit(s.charAt(2))) return -1;
		if (!isDigit(s.charAt(3))) return -1;
		if (!isDigit(s.charAt(4))) return -1;
		if (!isDigit(s.charAt(5))) return -1;
		return len;
	    } else if (len == 8) {
		if (!isDigit(s.charAt(0))) return -1;
		if (!isDigit(s.charAt(1))) return -1;
		if (!isDigit(s.charAt(2))) return -1;
		if (s.charAt(3)!=DASH) return -1;
		if (!isDigit(s.charAt(4))) return -1;
		if (!isDigit(s.charAt(5))) return -1;
		if (!isDigit(s.charAt(6))) return -1;
		if (!isDigit(s.charAt(7))) return -1;
		return len;
	    } else if (len == 12) { // two possible formats
		// first check  507-555-1212 format
		if (!isDigit(s.charAt(0))) return checkFormat2(s);
		if (!isDigit(s.charAt(1))) return checkFormat2(s);
		if (!isDigit(s.charAt(2))) return checkFormat2(s);
		if (s.charAt(3)!=DASH) return checkFormat2(s);
		if (!isDigit(s.charAt(4))) return checkFormat2(s);
		if (!isDigit(s.charAt(5))) return checkFormat2(s);
		if (!isDigit(s.charAt(6))) return checkFormat2(s);
		if (s.charAt(7)!=DASH) return checkFormat2(s);
		if (!isDigit(s.charAt(8))) return checkFormat2(s);
		if (!isDigit(s.charAt(9))) return checkFormat2(s);
		if (!isDigit(s.charAt(10))) return checkFormat2(s);
		if (!isDigit(s.charAt(11))) return checkFormat2(s);
		return len;
	    } else if (len == 14) { // 1-507-555-1212
		if (!isDigit(s.charAt(0))) return -1;
		if (s.charAt(1)!=DASH) return -1;
		if (!isDigit(s.charAt(2))) return -1;
		if (!isDigit(s.charAt(3))) return -1;
		if (!isDigit(s.charAt(4))) return -1;
		if (s.charAt(5)!=DASH) return -1;
		if (!isDigit(s.charAt(6))) return -1;
		if (!isDigit(s.charAt(7))) return -1;
		if (!isDigit(s.charAt(8))) return -1;
		if (s.charAt(9)!=DASH) return -1;
		if (!isDigit(s.charAt(10))) return -1;
		if (!isDigit(s.charAt(11))) return -1;
		if (!isDigit(s.charAt(12))) return -1;
		if (!isDigit(s.charAt(13))) return -1;
		return len;
	    } else {
		return -1;
	    }
	    
	}

	private int checkFormat2(String s) { // 02-2348-2192
		if (!isDigit(s.charAt(0))) return -1;
		if (!isDigit(s.charAt(1))) return -1;
		if (s.charAt(2)!=DASH) return -1;
		if (!isDigit(s.charAt(3))) return -1;
		if (!isDigit(s.charAt(4))) return -1;
		if (!isDigit(s.charAt(5))) return -1;
		if (!isDigit(s.charAt(6))) return -1;
		if (s.charAt(7)!=DASH) return -1;
		if (!isDigit(s.charAt(8))) return -1;
		if (!isDigit(s.charAt(9))) return -1;
		if (!isDigit(s.charAt(10))) return -1;
		if (!isDigit(s.charAt(11))) return -1;
	    
	    return -1;
	}
	
	/**
	 * "0123456789-"
	 * @param ch
	 * @return
	 */
	private boolean isTelephoneNumberChar(char ch) { 
	    return (isDigit(ch) || ch=='-');
	}

	/**
	 * ",.0123456789"
	 * @param ch
	 * @return
	 */
	private boolean isNumericChar(char ch) { // 
	    return (isDigit(ch) || ch==',' || ch=='.');
	}

	private int getLenToNextNonDigit(String s, int startingPosition) {
	    char ch;
	    int i = 0;
	    while (startingPosition+i < s.length()) {
		ch = s.charAt(startingPosition+i);
		if (!isDigit(ch)) {
		    return i; 
		}
		i++;
	    }
	    return s.length()-startingPosition;
	}

	private Class<? extends BaseToken> wordTokenOrNumToken(String lowerCasedText, int currentPosition, int tokenLen) {
	    if (containsLetter(lowerCasedText, currentPosition, tokenLen)) {
		return WordToken.class;
	    } else {
		return NumToken.class;
	    }
	}

	/**
	 * 
	 * @param lowerCasedText
	 * @param currentPosition
	 * @param tokenLen
	 * @return true if at least one of the characters between currentPosition and currentPosition+tokenLen is a letter
	 */
	private boolean containsLetter(String lowerCasedText, int currentPosition, int tokenLen) {
	    for (int i=currentPosition; i<currentPosition+tokenLen; i++) {
		char c = lowerCasedText.charAt(i);
		if (isLetter(c)) {
		    return true;
		}
	    }
	    return false;
	}

	
	private static String ellipsis = "...";  
	private boolean isEllipsis(int currentPosition, String textSegment) {
	    if (textSegment.substring(currentPosition).startsWith(ellipsis)) return true;
	    return false;
	}

	static String [] nameStartingWithApostrophe = {"'assad", "'awarta", "'ashira", };
	

	private int getLengthIfNameStartingWithApostrophe(int currentPosition, String textSegment) {
	    
	    String textLowerCased = textSegment.substring(currentPosition).toLowerCase();
	    if (textLowerCased.length() == 1) return -1; // if no more chars after the apostrophe, it's a 1-char token
	    if (!isLetter(textSegment.charAt(currentPosition+1))) {
		return -1;
	    }
	    
	    // Could be the start of a quoted string like "'The boy ran', she said" or could be the start of a name like 'Assad
	    for (String s:nameStartingWithApostrophe) {
		if (s.length() == textLowerCased.length()) {
		    // exactly matches the rest of the input....
		    if (textLowerCased.startsWith(s)) return  s.length();
		} else if (s.length() > textLowerCased.length()) {
		    ; // can't be match, not long enough
		} else if (textLowerCased.startsWith(s)) {
		    return  s.length(); // already checked above that next char after ' exists and is not letter
		    
		} else {
		    
		    // don't want "'The boy ran'" to have "'The" be one token so don't assume 
		    // it's a quoted name unless it is in the specific list above
		    ; // do nothing in this case, try the next from nameStartingWithApostrophe
                    //		    int end = currentPosition+s.length();
                    //		    char next = textSegment.charAt(end);
                    //		    // ok as long as just more letters
                    //		    while (isLetter(next) && end < textSegment.length()) {
                    //			end++;			
                    //		    }
                    //		    return end-currentPosition; // is correct whether ran out of chars or found non letter 
		    
		}
	    }
		
	    
	    return -1;
	}




	private int getLengthIfIsNumberThatStartsWithPeriod(int currentPosition, String textSegment) {
	    int len = textSegment.length() -  currentPosition;	
	    if (len<2) return -1;
	    int index=currentPosition+1;
	    char ch = textSegment.charAt(index);
	    if (!isDigit(ch)) return -1;
	    index++;
	    while (index < currentPosition+len){
		ch = textSegment.charAt(index);
		if (!isDigit(ch)) return index-currentPosition;
		index++;
	    } 
	    
	    return len; // all rest were digits
	}


	/**
	 * Assumes no white space between currentPosition and endOfInputToConsider
	 * If last of a sentence is a period, then don't include the period with the abbreviation,
	 * count it as punctuation.
	 * That way we don't have to differentiate between "mg." being an abbreviation and "me." being simply
	 * the end of a sentence
	 * @param currentPosition
	 * @param mixedCaseText
	 * @param afterEndOfInputToConsider
	 * @return
	 */
	private int lenIfIsAbbreviation(int currentPosition, String mixedCaseText, int afterEndOfInputToConsider) {
		// Determine if all up to endOfInputToConsider contains at least 1 letter and ends with period
		// Note input is known to contain at least 1 letter or otherwise would have already been determined to be a number
		boolean containsLetter = false;
		// consider as single abbreviation things like e.g. but for things like 
		// www.nlm.nih.gov (without the http) count as separate tokens 
		if (afterEndOfInputToConsider-currentPosition >= 4 && mixedCaseText.substring(currentPosition, currentPosition+4).toLowerCase().equals("www.")) {
			return -1;
		}
		for (int i = currentPosition; i < afterEndOfInputToConsider; i++) { 
			char ch = mixedCaseText.charAt(i);
			char peekAhead;
			if (i+1 < afterEndOfInputToConsider) { 
				peekAhead = mixedCaseText.charAt(i+1);
			} else {
				peekAhead = ' ';
			}

			if (isLetter(ch)) {
				containsLetter = true;
			} else if (ch != PERIOD) { // if any symbol is found before the period, not considering it an abbreviation
				return -1;
			} else if (!containsLetter || (i+1 == mixedCaseText.length())) {
				return -1; // no letter, or last character of sentence is this period, in which case period is end of sentence marker, not part of abbreviation
			} else { // is a period and there was a letter before it and this period is not last char in sentence
				// If before the period there are alphanums with at least one letter, and we are 
				// not at the end of the sentence, consider the period to be part of the preceding
				// If there are more alphanums after, also terminated by period, include that too 
				// like in A.D. or e.g.
				int soFar = (i + 1 - currentPosition);
				int len = lenIfIsAbbreviation(i+1, mixedCaseText, afterEndOfInputToConsider);
				// If what's after the period satisfies abbreviation definition itself
				if (len>0) {
					return (soFar + len);
				}
				// else len<=0 and so what's after the period is not more abbreviation
				
				if (Character.isWhitespace(peekAhead) || isPossibleFinalPunctuation(peekAhead)) {
					// "e.g. edema" does have the abbreviation e.g. within it
					return soFar;
				} else if (!isLetterOrDigit(peekAhead)) { // "e.g.[1]" does have the abbreviation e.g. within it
					return soFar-1;
				}

				// "e.g.abc" is not an abbreviation because the abc follows the . immediately
				return -1; // period is end of sentence or is between alphanums

			}
		}

		// No period found - just all letters
		return -1; 

	}

	private String possibleFinalPunctuation = "?!:";
	private boolean isPossibleFinalPunctuation(char c) {
		if (possibleFinalPunctuation.indexOf(c) > -1) return true;
		return false;
	}

	private String validOtherEmailAddressCharacters = "!#$%&'*+/=?^_`{|}~-"; // those that can be used without quoting or escaping them

	/**
	 * Assumes no white space between currentPosition and endOfInputToConsider
	 * @param currentPosition
	 * @param lowerCasedText
	 * @param endOfInputToConsider
	 * @return
	 */
	private int lenIfIsEmailAddress(int currentPosition, String lowerCasedText, int endOfInputToConsider) {

	    int maxLenLocalPart = 64;
	    int maxTotalLen = 320;
	    int len = -1;
	    // (?:[a-z0-9!#$%&'*+/=?^_`{|}~-]
	    // @
	    // (?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])
	    
	    char AT = '@';
	    char PERIOD = '.'; // as String not char
	    int indexOfAt = lowerCasedText.substring(currentPosition, endOfInputToConsider).indexOf(AT);
	    if (indexOfAt < 1 || currentPosition+indexOfAt+1==endOfInputToConsider || indexOfAt > maxLenLocalPart) { // '@' can't be the first character, but must be present, and can't be last char 
		// if no @ sign, or not in a valid position, don't bother doing anything more complicated, can't be an email address
		return -1;
	    }
	    
	    
	    // @see http://tools.ietf.org/html/rfc3696#section-3

	    // ignoring quoted or escape chars
	    // ignoring ability to use IP address in square brackets for domain part
	   
	    // First validate the local part (the part before the @ sign)
	    //String localPart = textSegment.substring(currentPosition, currentPosition+indexOfAt);	    
	    for (int i=currentPosition; i<currentPosition+indexOfAt; i++) {
		char ch = lowerCasedText.charAt(i);
		CharSequence cs = lowerCasedText.subSequence(i, i+1);
		if (!isLetterOrDigit(ch) && !validOtherEmailAddressCharacters.contains(cs)) {
		    return -1;
		}
		if (ch == PERIOD && (i==currentPosition || i == currentPosition+indexOfAt-1)) { // first and last of local name can't be period
		    return -1;
		}
	    }
	    
	    char prev = '@';
	    // The local part appears to be the right format for a valid email address, validate the domain part
	    for (int i=currentPosition+indexOfAt+1; i<endOfInputToConsider; i++) {
		char ch = lowerCasedText.charAt(i);
		//CharSequence cs = textSegment.subSequence(i, i+1);
		if (isLetterOrDigit(ch)) { 
		    ; // fine, continue
		} else if (ch==HYPHEN_OR_MINUS_SIGN || ch==PERIOD) {
		    // either stop one earlier, or error, or include at least one more char
		    // Is there at least one more valid character?
		    if (i+1<endOfInputToConsider && isLetterOrDigit(lowerCasedText.charAt(i+1)) ) {
			; // keep going
		    } else if (isLetterOrDigit(prev)) {
			return i-currentPosition-1;
		    } else { 
			return -1;
		    }
		} else { //something else that ends the token, like an exclamation point
		    if (isLetterOrDigit(prev)) return i-currentPosition-1;
		    else return -1;
		}
	    }
	    
	    len = endOfInputToConsider - currentPosition;
	    if (len > maxTotalLen) return -1;
	    return len;
	}


	private static String [] urlStarters = { "http://", "https://", "ftp://", "mailto:" };

	private int lenIfIsUrl(int currentPosition, String lowerCasedText, int endOfInputToConsider) {

	    // http://host:port/path?search#fragment
	    // mailto:joe@example.com
	    //
	    String potentialUrl = lowerCasedText.substring(currentPosition, endOfInputToConsider);
	    for (String s: urlStarters) {
		if (potentialUrl.startsWith(s) && potentialUrl.length() > s.length()) {
		    return endOfInputToConsider - currentPosition; // same as  potentialUrl.length()
		}
	    }

	    return -1;
	}



	//
	private Class<? extends BaseToken> determineTokenType(String s, int begin, int end) {
		
	    if (s==null || s.length() < end || begin+1!=end) {
		Exception e = new Exception("ERROR: s not at least one char:  s= " + s + " begin, end = " + begin + "," + end);
		//System.err.println("ERROR: s.length()!=1  s=" + s);
		e.printStackTrace();
		return null; // strings longer than 1 are not supported yet
	    }		
	    
	    char ch = s.charAt(begin);
	    if (ch == NEWLINE || ch == CR) return NewlineToken.class;
	    if (isDigit(ch)) return NumToken.class;
	    if (isLetter(ch)) return WordToken.class;
	    if (isContraction(ch)) return ContractionToken.class; 
	    if (isPunctuation(ch)) return PunctuationToken.class;
	    // if none of the above, it must be a SymbolToken
	    return SymbolToken.class;
		
	}

	// classic cTAKES (1.0.5) has a single ContractionToken for the input "It's hard."
	// That token contains the two characters "'s"
	// and "It" is a BaseToken
	private boolean isContraction(char c) {
	    // single character cannot be a contraction token. need something like 't or 's or n't 
	    return false;
	}

	private boolean verify(int begin, int end, int offsetAdjustment) {
	    Exception e = new Exception(begin + " " + end + " " + offsetAdjustment);
	    if (begin < 0) {
		System.err.println("ERROR: begin = " + begin);
		e.printStackTrace();
		return false;
	    }
	    if (end < 0) {
		System.err.println("ERROR: end = " + end);
		e.printStackTrace();
		return false;
	    }
	    if (end < begin) {
		System.err.println("ERROR: end < begin " + end + " < " + begin);
		e.printStackTrace();
		return false;
	    }
	    if (offsetAdjustment < 0) {
		System.err.println("ERROR: offsetAdjustment = " + offsetAdjustment);
		e.printStackTrace();
		return false;
	    }
	    
	    return true;
	    
	}
	
	/**
	 * if clas is null, determine token class for the caller
	 * if jcas is null,  
	 * @see org.apache.ctakes.core.ae.TokenConverter#convert(org.apache.ctakes.core.nlp.tokenizer.Token, org.apache.uima.jcas.JCas, int)
	 */
	private Object createToken(Class<? extends BaseToken> clas, String s, JCas jcas, int begin, int end, int offsetAdjustment) {


	    int beginFromStartOfDocument = begin + offsetAdjustment;
	    int endFromStartOfDocument = end + offsetAdjustment;

	    Object token;

	    
	    if (true) {
		boolean ok = verify(beginFromStartOfDocument, endFromStartOfDocument, offsetAdjustment);
		if (!ok) {
		    System.err.println("ERROR: so creating a BaseToken with begin = 0 end = 0 just to avoid exception");
		    if (jcas!=null) token = new BaseToken(jcas, 0, 0); else token = new Token(0,0);
		    return token;
		}

	    }

	    if (clas == null) { // determine the type for the caller

		Class<? extends BaseToken> clss = determineTokenType(s, begin, end);
		if (clss==null) throw new RuntimeException(" still is null");
		
		if (jcas!=null) 
		    token = createToken(clss, s, jcas, begin, end, offsetAdjustment);
		else {
		    token = new Token(begin, end);
		    ((Token)token).setText(s.substring(begin, end));
		}

	    } else if (clas.equals(NewlineToken.class)) {

		if (jcas!=null) 
		    token = new NewlineToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		else { 
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}

	    } else if (clas.equals(NumToken.class)) {

		if (jcas!=null) { 
		    token = new NumToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		    String tokenText = s.substring(begin, end);
		    setNumType((NumToken)token, tokenText);
		}
		else {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}
		
	    } else if (clas.equals(WordToken.class)) {

		if (jcas!=null) { 
		    token = new WordToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		    String tokenText = s.substring(begin, end);
		    setCapitalization((WordToken)token, tokenText);
		    setNumPosition((WordToken)token, tokenText);
		}
		else  {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}

	    } else if (clas.equals(SymbolToken.class)) {

		if (jcas!=null) 
		    token = new SymbolToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		else {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}


	    } else if (clas.equals(PunctuationToken.class)) {

		if (jcas!=null) 
		    token = new PunctuationToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		else {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}
		    

	    } else if (clas.equals(ContractionToken.class)) {

		if (jcas!=null) 
		    token = new ContractionToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		else {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}

	    } else if (clas.equals(BaseToken.class)) {

		if (jcas!=null) 
		    token = new BaseToken(jcas, beginFromStartOfDocument, endFromStartOfDocument);
		else {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}

	    } else {

		if (jcas!=null) { 
		    System.err.println("clas=" + clas + " and need to add more code here to support that class");
		    token = null;
		}
		else {
		    token = new Token(beginFromStartOfDocument, endFromStartOfDocument);
		    ((Token)token).setText(s.substring(begin, end));
		}
		
	    }



            //	    if (true) { // TBD remove this debug code
            //		System.out.println(" ---------------------------------------------------- ");
            //		System.out.println("token = " + token);
            //		System.out.println("token.getCoveredText() = " + token.getCoveredText());
            //		System.out.println("token.getClass().getName() = " + token.getClass().getName());
            //		Class cl = token.getClass();
            //		Field [] fields = cl.getFields();
            //		for (Field f: fields) {
            //		    System.out.println(f.getName() + " " + f);
            //			try {
            //			    System.out.println("  " + f.getInt(token));
            //			} catch (IllegalArgumentException e) {
            //			    System.out.println("IllegalArgumentException"); //e.printStackTrace();
            //			} catch (IllegalAccessException e) {
            //			    System.out.println("IllegalAccessException"); //e.printStackTrace();
            //			}
            //		    
            //		}
            //	    }

	    return token;
	
	}

	/**
	 * @see org.apache.ctakes.core.nlp.tokenizer.Tokenizer#isNumber
	 */
	private void setNumType(NumToken nta, String tokenText) {
	    if (org.apache.ctakes.core.nlp.tokenizer.Tokenizer.isNumber(tokenText) && !tokenText.contains(".")) {
		nta.setNumType(TokenizerAnnotator.TOKEN_NUM_TYPE_INTEGER);
	    } else {
		nta.setNumType(TokenizerAnnotator.TOKEN_NUM_TYPE_DECIMAL);
	    }
	}

	private void setNumPosition(WordToken wta, String tokenText) {
		if ( tokenText.isEmpty() ) {
			// was getting ioobE from tokenText.charAt(..)
			// Possibilities like this (empty, null) should always be checked
			// - but I wonder that we get (want) empty tokens at all.
			// I believe that working with zero-length words is a bug, 
			// and this is not a fix it merely avoids a crash.
			wta.setNumPosition( TokenizerAnnotator.TOKEN_NUM_POS_NONE );
			return;
		}
	    if (isDigit(tokenText.charAt(0)))  {

		wta.setNumPosition(TokenizerAnnotator.TOKEN_NUM_POS_FIRST);

	    } else if (isDigit(tokenText.charAt(tokenText.length()-1))) {

		wta.setNumPosition(TokenizerAnnotator.TOKEN_NUM_POS_LAST);

	    } else {

		boolean containsDigit = false;
		for (int i=0; i<tokenText.length(); i++) {
		    char ch = tokenText.charAt(i);
		    if (isDigit(ch)) containsDigit = true; 
		}

		if (containsDigit) { 
		    wta.setNumPosition(TokenizerAnnotator.TOKEN_NUM_POS_MIDDLE);
		} else { 
		    wta.setNumPosition(TokenizerAnnotator.TOKEN_NUM_POS_NONE);
		}

	    }

	}

	/**
	 * @see org.apache.ctakes.core.nlp.tokenizer.Tokenizer#applyCapitalizationRules
	 */
	private void setCapitalization(WordToken wta, String tokenText) {

	    int countUpperCase = 0;
	    boolean containsNonUpperCase = false;
	    for (int i=0; i<tokenText.length(); i++) {
		char ch = tokenText.charAt(i);
		if (isUpperCase(ch)) {
		    countUpperCase++;
		} else {
		    containsNonUpperCase = true;
		}
	    }

	    if (countUpperCase==0) { 
		wta.setCapitalization(TokenizerAnnotator.TOKEN_CAP_NONE); 
	    } else if (!containsNonUpperCase) {
		wta.setCapitalization(TokenizerAnnotator.TOKEN_CAP_ALL); 
	    } else if (countUpperCase==1 && isUpperCase(tokenText.charAt(0))) {
		wta.setCapitalization(TokenizerAnnotator.TOKEN_CAP_FIRST_ONLY); 
	    } else {
		wta.setCapitalization(TokenizerAnnotator.TOKEN_CAP_MIXED); 
	    }

	}


	/*
	 * Find the index of the first character of the next token, where
	 * the index is >= startPosition, and the previous token ended at
	 * startPosition-1 (or there was no previous token for the 1st time)
	 * Returns -1 if there are no more tokens (eof or all white space
	 * but no newlines)
	 */
	public int findFirstCharOfNextToken(String s, int startPosition) {

	    for (int position = startPosition; position < s.length(); position++) {

		// find a non-whitespace character or a newline
		// A newline is the start of a NewlineToken
		if (position < 0) {
		    System.out.println("position = " + position);
		}
		char c = s.charAt(position);

		if (!isWhitespace(c)) { // the only token that can start with whitespace is a NewlineToken
		    return position;
		}

		if (isEndOfLine(c)) {
		    return position;
		}

		//	    char peekAhead;
		//	    peekAhead = NEWLINE; // treat EOF like newline for tokenization purposes
		//	    if (position+1<s.length()) peekAhead = s.charAt(position+1);

	    }

	    return -1;

	}

	private boolean isEndOfLine(char c) {
	    if (c==NEWLINE || c==CR) return true;
	    return false;
	}

	static String [] testsForNumbers = {"2,000,123.For", "92,000,123.", "2,000,123.", "2,000,123.0", "2,000,13", "2", "2.", "2,", "22", "12345678901@4", "2.2.2."};

	static String [] testsForEmailAddress = {"masanz@mayo.edu", "masanz@mayo", "m@l", "m.@p", "m.n.@p", "3@4",
	    "%@f", "R@@", "MASANZ@MAYO", "jk$jk@.m", "asdf@.m$", "masanz.james-mi@ibm.com.us", ".mn@p", ".@p", "@t",  };

	public static void main(String[] args) {

	    runEmailTests();
	    runNumberTests();
	}

	static void runNumberTests() {
	    TokenizerPTB tester = new TokenizerPTB();
	    int len;
	    for (String s: testsForNumbers) {
		len = tester.lenIfIsNumberContainingComma(0, s, Math.min(s.length(),11));
		System.out.println("========== Test NumberWithComma ========== ");
		System.out.println(s);
		System.out.println(len);
		
	    }
	    
	}
	static void runEmailTests() {

	    TokenizerPTB tester = new TokenizerPTB();

	    for (String s: testsForEmailAddress) {

		int i = tester.lenIfIsEmailAddress(0, s, s.length());
		String prepend = "XYZ";
		int j = tester.lenIfIsEmailAddress(prepend.length(), prepend+s, s.length() + prepend.length());
		System.out.println("========== Test ========== ");
		System.out.println("      0123456789ABCDEF");
		System.out.println("  s = " + s + "\t  and prepend+s = " + prepend+s);
		System.out.println("  lenIfIsEmailAddress = " + i + "\t   and if prepend, len = " + j);



	    }

	}

}


// createToken(Class clas, String s, JCas jcas, int begin, int end, int offsetAdjustment) {
//	    {
//		try {
//		    System.out.println("INFO: Creating " + (clas==null? "(null class)": clas.getName()));
//		    System.out.println("INFO:   token for '" + s.substring(begin,end) + "'");
//		} catch (Exception e) {
//		    System.out.println("ERROR:  Unable to print substring for '" + s + "'" + begin + "," + end);		    
//		    e.printStackTrace(); 
//		} finally {
//		    System.out.println("INFO:                                   full string is  '" + s + "'  " + begin + "," + end + "," + offsetAdjustment); 
//		}
//		
//	    }
