/*
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

import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.APOSTROPHE;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.HYPHEN_OR_MINUS_SIGN;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.findNextNonAlphaNum;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.startsWithWithoutBeingFollowedByLetter;

import java.util.HashMap;

/**
 * 
 * @author Mayo Clinic
 */
public class ContractionsPTB {
    
    static String[] MultiTokenWords = { "cannot", "gonna", "gotta", "lemme", "wanna", "whaddya", "whatcha", };
    static int[] MultiTokenWordLenToken1 = { 3, 3, 3, 3, 3, 3, 3 };
    static int[] MultiTokenWordLenToken2 = { 3, 2, 2, 2, 2, 2, 1 };
    static int[] MultiTokenWordLenToken3 = { 0, 0, 0, 0, 0, 2, 3 };

    static HashMap<String, Integer> MultiTokenWordsLookup = new HashMap<String, Integer>();
    static {
	for (int i = 0; i < MultiTokenWords.length; i++) {
	    MultiTokenWordsLookup.put(MultiTokenWords[i], i);
	}
    }

    // *'s
    // *'ve
    // *'re
    // *'ll
    // *'d
    // more'n
    // *n't // for can't and shouldn't etc.
    static String[] possibleContractionEndings = { "'s", "'ve", "'re", "'ll", "'d", "'n", "n't"}; // note 't is different in that n't is the contraction token
    static String lettersAfterApostropheForMiddleOfContraction = "";
    static {
	for (String s:possibleContractionEndings) {
	    int indexLetterAfter = s.indexOf(APOSTROPHE) + 1;
	    lettersAfterApostropheForMiddleOfContraction+=s.charAt(indexLetterAfter); 
	}
    }

    private static void test_getLengthIfNextApostIsMiddleOfContraction() {
	String before, rest;
	String text;
	int position;
	int nextNonCharDigitApostrophe;
 	int expectedLenWordToken; // as opposed to expected length of the contraction token part // "ca" part of can't or "he" part of he'll
 	int expectedLenContractionToken;
 	ContractionResult cr;

//	text =     "He should've done that";
//	position = "He ".length();
//	expectedLenAfterPosition = 6;
//	result = getLengthIfNextApostIsMiddleOfContraction(position, text);
//	System.out.println("INFO: for text = " + text + ", result = " + result);
//	assert(result==expectedLenAfterPosition);

	
	// variation 
	before  = "";
	rest = "It's a deal";
	text =  before + rest;
	position = before.length();
	nextNonCharDigitApostrophe = text.indexOf(APOSTROPHE, position); // function to use depends on string
	expectedLenWordToken = 2; // "It" 
	expectedLenContractionToken = 2; // 's 
	//
	cr = getLengthIfNextApostIsMiddleOfContraction(position, nextNonCharDigitApostrophe, text);
	System.out.println("INFO: for text = " + text + ", result = " + cr.getWordTokenLen() + " " + cr.getContractionTokenLen());
	assert(cr.getWordTokenLen()==expectedLenWordToken);
	assert(cr.getContractionTokenLen()==expectedLenContractionToken);

	
	// variation 
	before = "He ";
	rest = "can't do that";
	text =  before + rest;
	position = before.length();
	nextNonCharDigitApostrophe = text.indexOf(APOSTROPHE, position); // function to use depends on string
	expectedLenWordToken = 2; // "ca" part of can't or "he" part of he'll
	expectedLenContractionToken = 3; // n't 
	//
	cr = getLengthIfNextApostIsMiddleOfContraction(position, nextNonCharDigitApostrophe, text);
	System.out.println("INFO: for text = " + text + ", result = " + cr.getWordTokenLen() + " " + cr.getContractionTokenLen());
	assert(cr.getWordTokenLen()==expectedLenWordToken);
	assert(cr.getContractionTokenLen()==expectedLenContractionToken);
	
	// variation 
	before = "He ";
	rest = "should've done that";
	text =  before + rest;
	position = before.length();
	nextNonCharDigitApostrophe = text.indexOf(APOSTROPHE, position); // function to use depends on string
	expectedLenWordToken = 6; // "should" 
	expectedLenContractionToken = 3; // 've 
	//
	cr = getLengthIfNextApostIsMiddleOfContraction(position, nextNonCharDigitApostrophe, text);
	System.out.println("INFO: for text = " + text + ", result = " + cr.getWordTokenLen() + " " + cr.getContractionTokenLen());
	assert(cr.getWordTokenLen()==expectedLenWordToken);
	assert(cr.getContractionTokenLen()==expectedLenContractionToken);
	
	// variation 
	before = "they have ";
	rest = "more'n they can use";
	text =  before + rest;
	position = before.length();
	nextNonCharDigitApostrophe = text.indexOf(APOSTROPHE, position); // function to use depends on string
	expectedLenWordToken = 4; // "more" 
	expectedLenContractionToken = 2; // 'n 
	//
	cr = getLengthIfNextApostIsMiddleOfContraction(position, nextNonCharDigitApostrophe, text);
	System.out.println("INFO: for text = " + text + ", result = " + cr.getWordTokenLen() + " " + cr.getContractionTokenLen());
	assert(cr.getWordTokenLen()==expectedLenWordToken);
	assert(cr.getContractionTokenLen()==expectedLenContractionToken);
	
	// variation 
	before = "";
	rest = "he'll see you now";
	text =  before + rest;
	position = before.length();
	nextNonCharDigitApostrophe = text.indexOf(APOSTROPHE, position); // function to use depends on string
	expectedLenWordToken = 2; // "he" 
	expectedLenContractionToken = 3; // "'ll" 
	//
	cr = getLengthIfNextApostIsMiddleOfContraction(position, nextNonCharDigitApostrophe, text);
	System.out.println("INFO: for text = " + text + ", result = " + cr.getWordTokenLen() + " " + cr.getContractionTokenLen());
	assert(cr.getWordTokenLen()==expectedLenWordToken);
	assert(cr.getContractionTokenLen()==expectedLenContractionToken);
	
	
	System.out.println("TEST THAT ASSERT IS ENABLED:");
	boolean assertionErrorCaught = false;
	try {
	    assert(false);
	} catch (AssertionError e) {
	    assertionErrorCaught = true;
	} finally {
	    if (assertionErrorCaught) {
		    System.out.println("As expected, assertionErrorCaught = "+assertionErrorCaught);
	    } else {
		    System.out.println("ERROR: Unexpected: assertionErrorCaught = "+assertionErrorCaught);
	    }
	}
	
    }
    
    /**
     * Determine if the text starting at 'position' within 'text' is the start of a 
     * contraction such as "should've" or "hasn't" or "it's" by looking at whether
     * there is a letter before the apostrophe, and the appropriate letters after the 
     * apostrophe (or in the case of "n't", verify the letter before is an 'n'
     * Note that if the text starting at 'position' is something like "n't" which 
     * isn't a complete word, returns null.
     * @param position  first char of next token 
     * @param lowerCasedText      text into which parameter position is an index into
     * @return the length of the WordToken part of the contraction.  Note this is not always the position of the 
     * apostrophe.  For example, for can't, which is tokenized as ca n't the 
     * length is 2.  For "it's", the length is also 2.
     * @see #lenOfFirstTokenInContraction for handling contractions like "cannot" that don't have an apostrophe 
     */
    public static ContractionResult getLengthIfNextApostIsMiddleOfContraction(int position, int nextNonLetterDigit, String lowerCasedText) {
	
	if (position<0) return null; 
	if (lowerCasedText.length()<position+3) return null; // need at least one letter after the apostrophe and one before ('tis and 'twas handled elsewhere)
	int apostrophePosition = lowerCasedText.indexOf(APOSTROPHE, position);

	// System.out.println("getLengthIfNextApostIsMiddleOfContraction: " + position + " " + nextNonLetterDigit + " " + lowerCasedText);

	// if a token break is found before the apostrophe or no apostrophe found
	// or there is no character after the apostrophe (out of input)
	// or no letter before the apostrophe or no letter before "n't"
	if (nextNonLetterDigit!=apostrophePosition) return null;
	if (apostrophePosition < 1 || apostrophePosition >= lowerCasedText.length()-1 || lowerCasedText.startsWith("n't")) {  
	    return null;
	}


	// First just check the one character after the apostrophe before we start checking in more detail
	// because we can rule out a lot of things this way
	String letterAfterApostrophe = lowerCasedText.substring(apostrophePosition+1, apostrophePosition+2);
	if (!lettersAfterApostropheForMiddleOfContraction.contains(letterAfterApostrophe)) return null;

	int subseqentNonAlphaNum = findNextNonAlphaNum(lowerCasedText, apostrophePosition+1);
	String restStartingWithApostrophe = lowerCasedText.substring(apostrophePosition, subseqentNonAlphaNum);  // "'n he could do" or 'n or 've or 'll or 't 

	char prev = lowerCasedText.charAt(apostrophePosition-1); // needed for checking for "n't"
	
	loop: for (String s: possibleContractionEndings) {
	    int lenAfterApostrophe = s.length()-1; // don't count the apostrophe itself
	    if (s.equals("n't")) lenAfterApostrophe--; // adjust for the "n" in "n't"
	    if (lowerCasedText.length() < apostrophePosition+lenAfterApostrophe) continue loop; // not enough text for this possibleContractionEndings to be a match

            //	    if (s.equals("'t")) { // different in that the contraction token includes character before the apostrophe
            //		if (rest.equals(s) && (prev=='n' || prev=='N')) {
            //		    throw new UnsupportedOperationException("the n't case is supposed to be handled elsewhere");
            //		    //return text.length()-3; // TBD how to tell it to go back 1 for the n't case? -- don't -- the n't case is  handled elsewhere
            //		} else {
            //		    continue loop;
            //		}
            //	    } 
	    
	    // if exact match with rest (end of sentence)
	    if (s.equals("n't") && prev=='n' && lowerCasedText.charAt(apostrophePosition+1)=='t' && lowerCasedText.length()==apostrophePosition+1+1) {
	    ContractionResult contractionResult = new ContractionResult();
	    contractionResult.setContractionTokenLen(3); // n't
		contractionResult.setWordTokenLen(apostrophePosition-1 - position);
		return contractionResult;
	    } else if (restStartingWithApostrophe.equals(s)) {
	    ContractionResult contractionResult = new ContractionResult();	    	
		contractionResult.setContractionTokenLen(s.length());
		contractionResult.setWordTokenLen(apostrophePosition - position);
		return contractionResult;

	    }
	    
	    // there's at least one character after, check that it isn't a letter or number, which would be part of the same token
	    // and would mean the apostrophe wasn't part of a contractiona after all. for example "he'dr. smith"  and "can'they" are 
	    // more likely the end of quoted sentences and teh start of a new sentence than a misspelled contraction
	    
	    // we checked exact match above.
	    // If same length as exact match but not an exact match, done with this one, go on
	    if (lowerCasedText.length() == apostrophePosition + lenAfterApostrophe+1) continue loop; // if not an exact match but has same length as exact match would, then not the right one 
	    
	    char after;
	    if (restStartingWithApostrophe.length() <= position+lenAfterApostrophe+1) {
		after = '\00';
	    } else {
		after = restStartingWithApostrophe.charAt(position+lenAfterApostrophe+1);
	    }
	    if (restStartingWithApostrophe.startsWith(s) && Character.isLetter(prev) && !Character.isLetter(after)) {
		// there was at least one letter before the apostrophe and after the apostrophe, and non letter after the contraction
	    ContractionResult contractionResult = new ContractionResult();	    	
		contractionResult.setContractionTokenLen(s.length());
		contractionResult.setWordTokenLen(apostrophePosition - position);
		return contractionResult;
	    } else if (s.equals("n't") && prev=='n' && restStartingWithApostrophe.startsWith("'t") && !Character.isLetter(after)) {
	    ContractionResult contractionResult = new ContractionResult();		
		contractionResult.setContractionTokenLen(3); // n't
		contractionResult.setWordTokenLen(apostrophePosition-1 - position);
		return contractionResult;
	    }

	}
	return null;
	
    }
    
    static int getLenContractionToken(int currentPosition, String lowerCasedText) {
	
	return -1;
    }
    
    public ContractionsPTB() {
    }

    /**
     * @param s
     * @return
     * @see isMiddleOfContraction
     */
    static int lenOfFirstTokenInContraction(String s) {

	Integer index = MultiTokenWordsLookup.get(s); 
	if (index==null) return -1;

	return MultiTokenWordLenToken1[index];
    }

    static int lenOfSecondTokenInContraction(String s) {

	Integer index = MultiTokenWordsLookup.get(s); 
	if (index==null) return -1;

	return MultiTokenWordLenToken2[index];
    }

    static int lenOfThirdTokenInContraction(String s) {

	Integer index = MultiTokenWordsLookup.get(s); 
	if (index==null) return -1;

	return MultiTokenWordLenToken3[index];
    }

    //
    //    static boolean isMultiWordToken(int currentPosition, String textSegment, int endOfInputToConsider) {
    //	
    //	return false;
    //    }

    static String [] contractionsStartingWithApostrophe = {"'tis", "'twas",};
    static boolean isContractionThatStartsWithApostrophe(int currentPosition, String lowerCasedText) {
	    String lowerCasedSubstring = lowerCasedText.substring(currentPosition);
	    for (String s:contractionsStartingWithApostrophe) {
		if (startsWithWithoutBeingFollowedByLetter(lowerCasedSubstring, s)) return true;
	    }
	    return false;
    }

    

    // The following contractions and related items are split into separate tokens.
    // 's
    // 've
    // 're
    // 'll
    // 'd
    // n't
    // can not
    // gon na
    // got ta
    // lem me
    // more 'n
    // 't is
    // 't was
    // wan na
    // wha dd ya
    // wha t cha

    // *'s
    // *'ve
    // *'re
    // *'ll
    // *'d
    // *n't
    // cannot
    // gonna
    // gotta
    // lemme
    // more'n
    // 'tis
    // 'twas
    // wanna
    // whaddya
    // whatcha

    
    
	// Find the 3 characters that are the next possible token breaks (look for next 3 whitespace, punctuation, but (*) count contiguous whitespace as one)
	// We are most interested in those that sometimes cause a split and sometimes don't -- apostrophes and hyphens.
	// 80's-esque should be one token according to the 2 rules. (potential break characters for that example
	// are the apostrophe, the hyphen, and the whitespace)
	// salon-o-torium should be one token, with a single contraction token and a single word token (potential
	// break characters for that example are hyphen hyphen whitespace)

	
	// Cases where first non alphanum is an apostrophe:
	// 1st nonalphanum    2nd nonalphanum    3rd nonalphanum

	//   apostr           hyphen             apostr        take at most up to 3rd break  
	//   apostr           hyphen             hyphen        test for -o-torium, otherwise take at most up to 3rd break (ignore case of o-torium followed by something more meaningful)
	//   apostr           hyphen             whtspc        take at most up to 3rd break
	//   apostr           hyphen             other         take at most up to 3rd break

	//   apostr           apostr             any           take at most up to 2nd break
	
	//   apostr           whtspc             any*          take at most up to 2nd break
	
	//   apostr           other              any           take at most up to 2nd break
    
    	// Note that an exception prefix between apostrophe and 1st hyphen does not avoid the break at the hyphen
    	// so the logic is not something that can be broken down into just looking at the not splitting at the
	// apostrophe followed by normal hyphen processing.

    
    private static String [] fullWordsNotToBreakAtApostrophe = {
	"p'yongyang",
    };
    
    /**
     * Assumes apostrophe is not first character.... that case is handled elsewhere
     * Assumes <code>s</code> is lower case.
     */
    static boolean breakAtApostrophe(String s, int positionOfApostropheToTest) {
	
	if (s.length()==positionOfApostropheToTest+1) return true; // James'
	
	if (positionOfApostropheToTest==0) {
	    throw new UnsupportedOperationException("positionOfApostropheToTest==0");
	}
	
	// First check for things like 80's that are all digits followed by 's and immediately
	// after the s there can't be an alphanum 
	if (allDigits(s.substring(0, positionOfApostropheToTest)) && s.charAt(positionOfApostropheToTest+1)=='s') {
	    if (s.length() < positionOfApostropheToTest+3) {
		return false; // 80's<end_of_input>
	    }
	    // Check that after the 's there aren't more letters or digits which would be unknown like 'st or 's2 
	    // and therefore don't want to assume ' should be kept together with rest.
	    char after = s.charAt(positionOfApostropheToTest+2);
	    if (Character.isLetterOrDigit(after)) {
		return true;
	    }
	    return false;
	} else {
	    for (String comparison: fullWordsNotToBreakAtApostrophe) {
		if (comparison.equals(s)) {
		    return false; // keep the apostrophe and more attached
		}
	    }
	}
	
	
	return true; // if not one of the exceptions above, break at the apostrophe
    }
    
    // If at least 1 char long and all chars are digits
    static boolean allDigits(String s) {
	if (s==null || s.length()<1) return false;
	for (int i=1;i<s.length(); i++) {
	    if (!Character.isDigit(s.charAt(i))) return false;
	}
	return true;
    }

	

    /**
     * for a word like 80's or P'yongyang or James' or Sean's or 80's-like or 80's-esque 
     * (or can't or haven't, which are to be split)
     * determine whether the singlequote(apostrophe)
     * needs to be kept with the surrounding letters/numbers 
     * and what to do about hyphenated afterwards if there is a hyphen after....
     * For possessives, do split.
     * Note that things that start with an apostrophe like 'Assad were handled elsewhere
     * @return len of how much to keep: len to apostrophe, or to next breaking char (the space after s for "80's ") or end of hyphenated suffix that should also remain attached, or -1
     */
    static int tokenLengthCheckingForSingleQuoteWordsToKeepTogether(String lowerCasedText) {
	
	if (lowerCasedText==null) throw new UnsupportedOperationException("no quote/apostrophe char found in (null)");
	int firstBreak = lowerCasedText.indexOf(APOSTROPHE);
	if (firstBreak<0) throw new UnsupportedOperationException("no quote/apostrophe char found in '" + lowerCasedText + "'");
	if (firstBreak==0) return -1;
	if (firstBreak+1==lowerCasedText.length()) return firstBreak;
	
	int secondBreak = findNextNonAlphaNum(lowerCasedText, firstBreak+1);
	//char secondBreak = -1;

	if (breakAtApostrophe(lowerCasedText, firstBreak)) {
	    return firstBreak;
	}
	// else going to keep at least past the apostrophe, but if there's a hyphenated word or a hyphenated suffix that should not be split,
	// keep that much too
	
	
	if (secondBreak == lowerCasedText.length()) return secondBreak; // no more text, must stop here
	
	// See if there are hyphenated suffix(es) that should also remain attached
	if (lowerCasedText.charAt(secondBreak) != HYPHEN_OR_MINUS_SIGN) {
	    return secondBreak;
	} else { // have to determine whether to keep the hyphen and how many hyphens
	    // 80's-esque
	    int len = HyphenatedPTB.lenIfHyphenatedSuffix(lowerCasedText, secondBreak);
	    if (len > 0) return secondBreak+len;
	    return secondBreak; 
	}

    }

    public static void main(String[] args) {

	test_getLengthIfNextApostIsMiddleOfContraction() ;
	
	for (int i = 0; i < MultiTokenWords.length; i++) {
	    String s = MultiTokenWords[i];
	    String SPACE = " ";
	    System.out.println(s.substring(0, MultiTokenWordLenToken1[i]) + SPACE
		    + s.substring(MultiTokenWordLenToken1[i], MultiTokenWordLenToken1[i] + MultiTokenWordLenToken2[i]) + SPACE
		    + s.substring(MultiTokenWordLenToken1[i] + MultiTokenWordLenToken2[i]) + SPACE
		    + s.substring(MultiTokenWordLenToken3[i] + MultiTokenWordLenToken3[i])
		    );

	}
	
	
    }

}
