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
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.findNextNonAlphaNum;
import static org.apache.ctakes.core.nlp.tokenizer.TokenizerHelper.startsWithWithoutBeingFollowedByLetter;

import java.util.HashMap;
import java.util.HashSet;
/**
 * 
 * @author Mayo Clinic
 */


public class HyphenatedPTB {


    static String[] MultiTokenWords = { "cannot", "gonna", "gotta", "lemme", "wanna", "whaddya", "whatcha", };
    static int[] MultiTokenWordLenToken1 = { 3, 3, 3, 3, 3, 3, 3 };
    static int[] MultiTokenWordLenToken2 = { 3, 2, 2, 2, 2, 2, 1 };

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
	    lettersAfterApostropheForMiddleOfContraction+=s.charAt(1);
	}
    }

//    /**
//     * 
//     * @see lenOfFirstTokenInContraction for handling contractions like "cannot" that don't have an apostrophe 
//     */
//    public static int getLengthIfIsMiddleOfContraction(int position, String text) {
//	if (position<1) return -1; // need at least one letter before the apostrophe for these
//	if (text.length()<position+2) return -1; // need at least one letter after the apostrophe
//	if (text.charAt(position)!=APOSTROPHE) return -1;
//	String ss = text.substring(position+1, position+2);
//	if (!lettersAfterApostropheForMiddleOfContraction.contains(ss)) return -1;
//	String rest = text.substring(position);
//
//	char prev = text.charAt(position-1);
//	
//	loop: for (String s: possibleContractionEndings) {
//
//	    if (text.length() < position+s.length()) continue loop;
//	    
//	    if (s.equals("n't")) { // different in that the contraction token includes character before the apostrophe
//		if (rest.equals(s) && (prev=='n' || prev=='N')) {
//		    return s.length()+1; 
//		} else {
//		    continue loop;
//		}
//	    } 
//	    
//	    if (rest.equals(s)) return s.length();
//	    // there's at least one character after, check that it isn't a letter or number, which would be part of the same token
//	    
//	    if (text.length() < position+s.length()+1) continue loop; // if not an exact match but has same length as exact match would, then not the right one 
//
//	    char ch =  rest.charAt(position+s.length());
//	    if (rest.startsWith(s) && Character.isLetter(prev) && !Character.isLetter(ch)) {
//		// there was at least one letter before the apostrophe and after the apostrophe
//		return s.length();
//	    }
//
//	}
//	return -1;
//	
//    }
    
    public HyphenatedPTB() {
    }

    /**
     * @param s
     * @return
     * @see isMiddleOfContraction
     */
    static int lenOfFirstTokenInContraction(String s) {

	Integer integer = MultiTokenWordsLookup.get(s); 
	if (integer==null) return -1;

	return MultiTokenWordLenToken1[integer];
    }

    //    static boolean isMultiWordToken(int currentPosition, String textSegment, int endOfInputToConsider) {
    //	
    //	return false;
    //    }

    static String [] contractionsStartingWithApostrophe = {"'tis", "'twas",};
    static boolean isContractionThatStartsWithApostrophe(int currentPosition, String textSegment) {
	    String text = textSegment.substring(currentPosition);
	    for (String s:contractionsStartingWithApostrophe) {
		if (startsWithWithoutBeingFollowedByLetter(text, s)) return true;
	    }
	    return false;
    }


    

    //    Hyphenated interjections and affixes in the following list are not split into multiple tokens.
    //    For example, uh-oh and e-mail are both single tokens: uh-oh, e-mail.
    //    e-
    //    a-
    //    u-
    //    x-
    //    agro-
    //    ante-
    //    anti-
    //    arch-
    //    be-
    //    bi-
    //    bio-
    //    co-
    //    counter-
    //    cross-
    //    cyber-
    //    de-
    //    eco-
    //    -esque
    //    -ette
    //    ex-
    //    extra-
    //    -fest
    //    -fold
    //    -gate
    //    inter-
    //    intra-
    //    -itis
    //    -less
    //    macro-
    //    mega-
    //    micro-
    //    mid-
    //    mini-
    //    mm-hm
    //    mm-mm
    //    -most
    //    multi-
    //    neo-
    //    non-
    //    o-kay
    //    -o-torium
    //    over-
    //    pan-
    //    para-
    //    peri-
    //    post-
    //    pre-
    //    pro-
    //    pseudo-
    //    quasi-
    //    -rama
    //    re-
    //    semi-
    //    sub-
    //    super-
    //    tri-
    //    uh-huh
    //    uh-oh
    //    ultra-
    //    un-
    //    uni-
    //    vice-
    //    -wise

    // hyphenated prefixes to not split
    //    e-
    //    a-
    //    u-
    //    x-
    //    agro-
    //    ante-
    //    anti-
    //    arch-
    //    be-
    //    bi-
    //    bio-
    //    co-
    //    counter-
    //    cross-
    //    cyber-
    //    de-
    //    eco-
    //    ex-
    //    extra-
    //    inter-
    //    intra-
    //    macro-
    //    mega-
    //    micro-
    //    mid-
    //    mini-
    //    multi-
    //    neo-
    //    non-
    //    over-
    //    pan-
    //    para-
    //    peri-
    //    post-
    //    pre-
    //    pro-
    //    pseudo-
    //    quasi-
    //    re-
    //    semi-
    //    sub-
    //    super-
    //    tri-
    //    ultra-
    //    un-
    //    uni-
    //    vice-
    static String [] hyphenatedPrefixes = {
	"e-",
	"a-",
	"u-",
	"x-",
	"agro-",
	"ante-",
	"anti-",
	"arch-",
	"be-",
	"bi-",
	"bio-",
	"co-",
	"counter-",
	"cross-",
	"cyber-",
	"de-",
	"eco-",
	"ex-",
	"extra-",
	"inter-",
	"intra-",
	"macro-",
	"mega-",
	"micro-",
	"mid-",
	"mini-",
	"multi-",
	"neo-",
	"non-",
	"over-",
	"pan-",
	"para-",
	"peri-",
	"post-",
	"pre-",
	"pro-",
	"pseudo-",
	"quasi-",
	"re-",
	"semi-",
	"sub-",
	"super-",
	"tri-",
	"ultra-",
	"un-",
	"uni-",
	"vice-",
	// From email from Colin Warner <colinw@ldc.upenn.edu> on 7/25/2010
	"electro-",
	"gasto-",
	"homo-",
	"hetero-",
	"ortho-",
	"phospho-",
    };
    
    
    
    // From email from Colin Warner <colinw@ldc.upenn.edu> on 7/25/2010
    // electro-
    // gasto-
    // homo-
    // hetero-
    // ortho-
    // phospho-
    
    static HashSet<String> hyphenatedPrefixesLookup = new HashSet<String>();
    static {
	for (String s: hyphenatedPrefixes) {
	    hyphenatedPrefixesLookup.add(s);
	}
	
    }
    // hyphenated suffixes to not split
    //    -esque
    //    -ette
    //    -fest
    //    -fold
    //    -gate
    //    -itis
    //    -less
    //    -most
    //    -o-torium
    //    -rama
    //    -wise

    static String [] hyphenatedSuffixes = {"-esque", "-ette", "-fest", "-fold", "-gate", "-itis", "-less", "-most", "-o-torium", "-rama", "-wise"};
    static HashSet<String> hyphenatedSuffixesLookup = new HashSet<String>();
    static {
	for (String s: hyphenatedSuffixes) {
	    hyphenatedSuffixesLookup.add(s);
	}
    }

    // complete words including hyphen
    //    mm-hm
    //    mm-mm
    //    o-kay
    //    uh-huh
    //    uh-oh
    static String [] hyphenatedWords = {"mm-hm", "mm-mm", "o-kay", "uh-huh", "uh-oh"};
    static HashSet<String> hyphenatedWordsLookup = new HashSet<String>();
    static {
	for (String s: hyphenatedWords) {
	    hyphenatedWordsLookup.add(s);
	}
    }

    public static void main(String[] args) {

	for (int i = 0; i < MultiTokenWords.length; i++) {
	    String s = MultiTokenWords[i];
	    String SPACE = " ";
	    System.out.println(s.substring(0, MultiTokenWordLenToken1[i]) + SPACE
		    + s.substring(MultiTokenWordLenToken1[i], MultiTokenWordLenToken1[i] + MultiTokenWordLenToken2[i]) + SPACE
		    + s.substring(MultiTokenWordLenToken1[i] + MultiTokenWordLenToken2[i]));

	}
	
	
    }

    static char MINUS_OR_HYPHEN = '-';
    
    /**
     * There is the fixed list of hyphenated words to not be split (hyphenatedWordsLookup)
     * 
     * And here are some made-up examples of words using affixes to keep together
     *   chronic-itis      1 suffix
     *   mega-huge         1 prefix
     *   e-game-fest       1 prefix and 1 suffix
     *   salon-o-torium    1 suffix that contains 2 hyphens
     *   urban-esque-wise  2 suffixes
     * 
     * @param lowerCasedString because of "-o-torium", input might contain more than 1 hyphen....
     * @return len to keep together, as far as we know. see hyphen hyphen hyphen case below. 
     * throws exception if there's no hyphen;
     * number of characters to keep. 
     * Does not mean to split at n+1 hyphen... need to recheck that one
     *  
     */
    public static int tokenLengthCheckingForHyphenatedTerms(String lowerCasedString) {  
	if (lowerCasedString==null) throw new UnsupportedOperationException("no hyphen found in (null)");
	int firstBreak = lowerCasedString.indexOf(MINUS_OR_HYPHEN);
	if (firstBreak<0) throw new UnsupportedOperationException("no hyphen found in '" + lowerCasedString + "'");
	if (firstBreak==0) return -1;
	if (firstBreak+1==lowerCasedString.length()) return firstBreak; // if ends with hyphen, don't include the hyphen in the token.   mega-  by itself should be mega and -
	
	// Find the 3 characters that are the next possible token breaks (look for next 3 whitespace, punctuation, but (*) count contiguous whitespace as one)
	// We are most interested in those that sometimes cause a split and sometimes don't -- apostrophes and hyphens.
	// 80's-esque should be one token according to the 2 rules. (potential break characters for that example
	// are the apostrophe, the hyphen, and the whitespace)
	// salon-o-torium should be one token, with a single contraction token and a single word token (potential
	// break characters for that example are hyphen hyphen whitespace)

	
	// Cases where first non alphanum is a hyphen:
	// 1st nonalphanum    2nd nonalphanum    3rd nonalphanum
	//   hyphen           hyphen             apostr        test for o-torium, otherwise take at most up to 2nd break
	//   hyphen           hyphen             hyphen        test for o-torium, otherwise take at most up to 2nd break, but check rest next time (TBD)
	//   hyphen           hyphen             whtspc        test for o-torium, otherwise take at most up to 2nd break
	//   hyphen           hyphen             other         test for o-torium, otherwise take at most up to 2nd break

	//   hyphen           apostr             any           hyphen and apostr not in a name together, take at most up to 2nd break
 
	//   hyphen           whtspc             any*          take at most up to 2nd break

	//   hyphen           other              any           take at most up to 2nd break
	
	int secondBreak = findNextNonAlphaNum(lowerCasedString, firstBreak+1);
	int thirdBreak = -1;
	if (secondBreak != lowerCasedString.length()) {
	    thirdBreak = findNextNonAlphaNum(lowerCasedString, secondBreak+1);
	}
	
	if (secondBreak == lowerCasedString.length()) {
	    return lenIncludingHyphensToKeep(lowerCasedString, firstBreak, 1, secondBreak, thirdBreak);  // determines if we should we split at first break or not	
	} else if (lowerCasedString.charAt(secondBreak) == MINUS_OR_HYPHEN) {
	    // test for -o-torium, otherwise take at most up to 2nd break
	    return lenIncludingHyphensToKeep(lowerCasedString, firstBreak, 2, secondBreak, thirdBreak); // take up to 2nd or 3rd break (or just to first, if not one of the exceptions)
	} else if (lowerCasedString.charAt(secondBreak) == APOSTROPHE) {
	    return lenIncludingHyphensToKeep(lowerCasedString, firstBreak, 1, secondBreak, thirdBreak);
	} else if (Character.isWhitespace(lowerCasedString.charAt(secondBreak))) {
	    return lenIncludingHyphensToKeep(lowerCasedString, firstBreak, 1, secondBreak, thirdBreak);  // take up to 2nd break (or just to first, if not one of the exceptions)
	} else { // some other symbol or punctuation 
	    return lenIncludingHyphensToKeep(lowerCasedString, firstBreak, 1, secondBreak, thirdBreak);
	}
	
	
    }
    
    // If there is 1 hyphen:  prefix, suffix, or word like uh-oh
    // If there are 2 hyphens: o-torium or prefix and suffix like mega-huge-esque
    private static int lenIncludingHyphensToKeep(String s, int indexOfFirstHyphen, int numberOfHyphensToConsiderKeeping, int secondBreak, int thirdBreak) {
		
	String possibleSuffix;
	boolean lookup;
	
	if (numberOfHyphensToConsiderKeeping>2 || numberOfHyphensToConsiderKeeping<1) {
	    throw new UnsupportedOperationException("Not ready to handle numberOfHyphensToConsiderKeeping = " + numberOfHyphensToConsiderKeeping);
	}

	// FIRST CONSIDER suffixes

	// Of the suffixes, first check those that have 2 hyphens (-o-torium)
	if (numberOfHyphensToConsiderKeeping==2) {
	    possibleSuffix = s.substring(indexOfFirstHyphen, thirdBreak);
	    lookup = hyphenatedSuffixesLookup.contains(possibleSuffix);
	    if (lookup) return thirdBreak;
	}
	    
	// Now either numberOfHyphensToConsiderKeeping==1 or was ==2 but no 2-hyphen suffix was found to match
	// Try one-hyphen suffixes, either just 1 of them or 2 of them (but not yet checking for 1 with a prefix too, see prefixes section below for that...
	possibleSuffix = s.substring(indexOfFirstHyphen, secondBreak);
	lookup = hyphenatedSuffixesLookup.contains(possibleSuffix);
	if (lookup) { // First hyphen is start of a suffix that should not be split off
	    // could be numberOfHyphensToConsiderKeeping==1 here, or could be 2 separate 1-hyphen suffixes are both
	    // used, so do need to check for a second suffix....
	    // Check if a second 1-hyphen suffix
	    if (thirdBreak > secondBreak) {
		possibleSuffix = s.substring(secondBreak, thirdBreak);
		lookup = hyphenatedSuffixesLookup.contains(possibleSuffix);
		if (lookup) return thirdBreak; // 2 1-hyphen suffixes that all should be kept together
	    }
	    return secondBreak; // just 1 1-hyphen suffix from the list of exceptions
	}
	
	// Now consider  hyphenatedWordsLookup plus a suffix   such as uh-oh-X 
	if (numberOfHyphensToConsiderKeeping>1) { 
	    String possibleHyphenatedWordsLookupMatch = s.substring(0, secondBreak);
	    possibleSuffix = s.substring(secondBreak, thirdBreak);
	    lookup = hyphenatedWordsLookup.contains(possibleHyphenatedWordsLookupMatch) && hyphenatedSuffixesLookup.contains(possibleSuffix);
	    if (lookup) return thirdBreak;
	}

	
	//  NOW CONSIDER prefixes
	
	String possiblePrefix = s.substring(0, indexOfFirstHyphen+1);
	
	lookup = hyphenatedPrefixesLookup.contains(possiblePrefix);

	// First consider prefix + one of the hyphenatedWordsLookup 
	// Do this before considering just prefix so we get both if both are present.
	if (lookup && numberOfHyphensToConsiderKeeping>1) {
	    String possibleHyphenatedWordsLookupMatch = s.substring(indexOfFirstHyphen+1, thirdBreak); // e.g. uh-oh	    
	    boolean lookup2 = hyphenatedWordsLookup.contains(possibleHyphenatedWordsLookupMatch);
	    if (lookup2) return thirdBreak;
	}
	
	if (numberOfHyphensToConsiderKeeping==1) {
	    if (lookup) return secondBreak;
	}
	
	if (numberOfHyphensToConsiderKeeping==2) {
	    if (lookup) { // a prefix was found that should not be split
		// check for a one-hyphen suffix to go with the one-hyphen prefix
		possibleSuffix = s.substring(secondBreak, thirdBreak);
		boolean lookup2 = hyphenatedSuffixesLookup.contains(possibleSuffix);
		if (lookup2) return thirdBreak; // both a prefix and a suffix that are not to be split, keep all together
		return secondBreak; // just a prefix that should not be split, split before second hyphen
	    } else { // not a prefix, or is a prefix that should be split
		// Already checked for a 2-hyphen suffix without a prefix
		// And already checked for word like uh-oh with a suffix
		// And apparently neither of those, so don't check anything else, fall through to next check 
		//String m = "This condition checked already in other if-else " + indexOfFirstHyphen + COMMA + secondBreak + COMMA + thirdBreak + COMMA + s;
		//throw new UnsupportedOperationException(m);
	    }
	
	}

	// Finally consider just hyphenatedWordsLookup, without an affix, such as "uh-oh"
	String possibleHyphenatedWordsLookupMatch = s.substring(0, secondBreak);
	lookup = hyphenatedWordsLookup.contains(possibleHyphenatedWordsLookupMatch);
	if (lookup) return secondBreak;
	
	return indexOfFirstHyphen; // if the first hyphen is not eligible to keep, keep just up to it.
    }
    
    // if character at position is a hyphen and starts a hyphenated suffix that is an exception
    // and should not be split from the rest of teh word, return length of the suffix
    // return -1 if not an exception suffix
    static int lenIfHyphenatedSuffix(String lowerCasedString, int position) {
	lowerCasedString = lowerCasedString.toLowerCase();
	int next = findNextNonAlphaNum(lowerCasedString, position+1);
	String possibleSuffix = lowerCasedString.substring(position, next);
	if (lowerCasedString.substring(position).startsWith("-o-")) { // check for -o-torium
	    next = findNextNonAlphaNum(lowerCasedString, position+3);
	    possibleSuffix = lowerCasedString.substring(position, next);
	}
	boolean lookup = hyphenatedSuffixesLookup.contains(possibleSuffix);
	
	if (lookup) return possibleSuffix.length();
	return -1;
    }
}


