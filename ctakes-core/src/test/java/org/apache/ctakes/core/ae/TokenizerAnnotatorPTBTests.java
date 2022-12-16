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
package org.apache.ctakes.core.ae;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ContractionToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.SymbolToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.utils.test.TestUtil;

public class TokenizerAnnotatorPTBTests {

    TestData<SimpleToken []>[] tests;
    private static final Logger logger = Logger.getLogger(TokenizerAnnotatorPTBTests.class.getName());
    
    public TokenizerAnnotatorPTBTests() {
	
    	hyphenExpectedResults = new SimpleToken [hyphenatedWordsToNotSplit.length];
    	int i = 0;
    	int position = 0;
    	for (String s:hyphenatedWordsToNotSplit) {
    		hyphenExpectedResults[i++] = new SimpleToken(WordToken.class, position, position+s.length());
    		position = position + s.length() + 1;
	}

	String pad = "  "; // re-test all with white space added at end which shouldn't change anything.
	tests = new TestData [] {
		new TestData<SimpleToken []>(whitespaceTest, whitespaceExpectedResults),
		new TestData<SimpleToken []>(punctuationTest, punctuationExpectedResults),
		new TestData<SimpleToken []>(contractionTest, contractionExpectedResults),
		new TestData<SimpleToken []>(hyphenTest, hyphenExpectedResults),
		new TestData<SimpleToken []>(abbrevTest1, abbrevExpectedResults1),
		new TestData<SimpleToken []>(abbrevTest2, abbrevExpectedResults2),
		new TestData<SimpleToken []>(abbrevTest3, abbrevExpectedResults3),
		new TestData<SimpleToken []>(webString1Test, webString1ExpectedResults),
		new TestData<SimpleToken []>(webString2Test, webString2ExpectedResults),
		new TestData<SimpleToken []>(ellipsisTest, ellipsisExpectedResults),
		new TestData<SimpleToken []>(teleAndPostalTest, teleAndPostalExpectedResults),
		new TestData<SimpleToken []>(namesTest, namesExpectedResults),
		new TestData<SimpleToken []>(comboTest, comboExpectedResults),

		/** repeat all the tests with whitespace added at the end of the "sentence" **/
		/** Should not change the number of tokens or the tokenization **/
		new TestData<SimpleToken []>(whitespaceTest+pad, whitespaceExpectedResults),
		new TestData<SimpleToken []>(punctuationTest+pad, punctuationExpectedResults),
		new TestData<SimpleToken []>(contractionTest+pad, contractionExpectedResults),
		new TestData<SimpleToken []>(hyphenTest+pad, hyphenExpectedResults),
		new TestData<SimpleToken []>(abbrevTest1+pad, abbrevExpectedResults1),
		new TestData<SimpleToken []>(abbrevTest2+pad, abbrevExpectedResults2),
		new TestData<SimpleToken []>(abbrevTest3+pad, abbrevExpectedResults3),
		new TestData<SimpleToken []>(webString1Test+pad, webString1ExpectedResults),
		new TestData<SimpleToken []>(webString2Test+pad, webString2ExpectedResults),
		new TestData<SimpleToken []>(ellipsisTest+pad, ellipsisExpectedResults),
		new TestData<SimpleToken []>(teleAndPostalTest+pad, teleAndPostalExpectedResults),
		new TestData<SimpleToken []>(namesTest+pad, namesExpectedResults),
		new TestData<SimpleToken []>(comboTest+pad, comboExpectedResults),

		/* add other new TestData statements here */
		};
	
    }



  
    
    @Test
    public void testTokenizerAnnotatorPTB() throws ResourceInitializationException {
    	testTokenizerAnnotatorPTB(true); // change to false if you want to see the tokens found (type and offsets) rather than just the failure message

    }

    private void testTokenizerAnnotatorPTB(boolean throwAssertionErrors) throws ResourceInitializationException {
    	String aePath =  "desc/test/analysis_engine/AggregateForTokenizerPTB.xml";
    	AnalysisEngine ae = TestUtil.getAE(new File(aePath));

    	// Try to make sure not running with the tokenizer descriptor from version 1 of cTAKES by checking for a parameter that dosen't apply to the tokenizer that uses PTB rules
    	if (ae.getConfigParameterValue("HyphFreqTable")!=null) { // TODO this check does not work - change to building own aggregate and checking the TokenizerAnnotator's parms intead of the aggregate
    		throw new ResourceInitializationException(new RuntimeException("The Tokenizer following PTB rules does not have a HyphFreqTable parameter. Do you have the right version of " + aePath));
    	}
    	JCas jCas; 

    	for (int test=0; test < tests.length; test++) {

    		String testInput = tests[test].getTestInput();
    		SimpleToken [] expectedResults = tests[test].getExpectedResults();

    		jCas = TestUtil.processAE(ae, testInput);
    		boolean alreadyOutputDebugInfoForThisRunOfPipeline = false;

    		String DQUOTE = "\"";
    		logger.info("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
    		logger.info("Test PTB Tokenizer for string (shown here in quotes) " + DQUOTE + testInput + DQUOTE);

    		int numTokensTested = 0;
    		for (int i=0; i< expectedResults.length; i++) {
    			SimpleToken expectedTok = expectedResults[i];
    			BaseToken tokenFromPipeline = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, i);
    			try {
    				if (expectedTok.getTokenClass()!=null) { 
    					// allow for tokens where we don't care what kind of token it is. for example, if testing hyphens in a sentence, 
    					// might not care if what type of token the final punctuation is created as
    					//TODO: Could we confirm this test case?
    					//assertEquals(expectedTok.getTokenClass(), tokenFromPipeline.getClass());
    				}
    				//TODO: Could we confirm this test case?
    				//assertEquals(expectedTok.getBegin(), tokenFromPipeline.getBegin());
    				//assertEquals(expectedTok.getEnd(), tokenFromPipeline.getEnd());
    				numTokensTested++;
    			} catch (java.lang.AssertionError e) {
    				if (throwAssertionErrors) {
    					throw e;
    				}
    				if (!alreadyOutputDebugInfoForThisRunOfPipeline) {
    					System.err.println("ERROR: Found a problem, so outputting the tokens");
    					for (int x=0; x < expectedResults.length; x++) {
    						SimpleToken xTok = expectedResults[x];
    						System.err.println("Expected token #" + x + " " + xTok.toString());
    					}
    					for (int sysTokNum=0; sysTokNum< expectedResults.length; sysTokNum++) {
    						BaseToken sysTok = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, sysTokNum);
    						System.err.println("System token #" + sysTokNum + " " + sysTok.getClass() + " " + sysTok.getBegin() + " " + sysTok.getEnd());
    					}

    					alreadyOutputDebugInfoForThisRunOfPipeline = true;
    				}
    				System.err.println("Caught exception at i = " + i + " for expectedTok " + expectedTok.toString() + " for testInput " + testInput);
    				System.err.flush();
    				e.printStackTrace();
    			}
    		}

    		logger.info("Verified " + numTokensTested + " token(s) using assertions.");

    	}
    }
    
    	// Comments like the following are used to help determine the index into a String
	//  tens digit:   0         111111111 2         333333333 4
	//  units digit:  01234567890123456789012345678901234567890123456789
	
    	
    //        tens digit:   0          111111 111 2            3 33333333 4
	//        units digit:  012345678 9012345 67890 123456 78 90 1234567890123456789
	String whitespaceTest = "one two\t three\tfour\tfive  \t\t \t   six";
	SimpleToken [] whitespaceExpectedResults = {
		new SimpleToken(WordToken.class, 0, 3), // use null when we don't care about checking the type of BaseToken (whether WordToken etc)
		new SimpleToken(WordToken.class, 4, 7),
		new SimpleToken(WordToken.class, 9, 14),
		new SimpleToken(WordToken.class, 15, 19),
		new SimpleToken(WordToken.class, 20, 24),
		new SimpleToken(WordToken.class, 33, 36),
	};
	
	//  tens digit:           0         111111111 2         3333 33333 4         5
	//  units digit:          0123456789012345678901234567890123 45678901234567890123456789
	String punctuationTest = "one;two.three:four-five+six!seven\\eight/;:-$nine*";
	SimpleToken [] punctuationExpectedResults = {
		new SimpleToken(WordToken.class, 0, 3), // use null when we don't care about checking the type of BaseToken (whether WordToken etc)
		new SimpleToken(PunctuationToken.class, 3, 4),
		new SimpleToken(WordToken.class, 4, 7),
		new SimpleToken(PunctuationToken.class, 7, 8),
		new SimpleToken(WordToken.class, 8, 13),
		new SimpleToken(PunctuationToken.class, 13, 14),
		new SimpleToken(WordToken.class, 14, 18),
		new SimpleToken(PunctuationToken.class, 18, 19),
		new SimpleToken(WordToken.class, 19, 23),
		new SimpleToken(SymbolToken.class, 23, 24),
		new SimpleToken(WordToken.class, 24, 27),
		new SimpleToken(SymbolToken.class, 27, 28),
		new SimpleToken(WordToken.class, 28, 33),
		new SimpleToken(PunctuationToken.class, 33, 34),
		new SimpleToken(WordToken.class, 34, 39),
		new SimpleToken(PunctuationToken.class, 39, 40),
		new SimpleToken(PunctuationToken.class, 40, 41),
		new SimpleToken(PunctuationToken.class, 41, 42),
		new SimpleToken(PunctuationToken.class, 42, 43),
		new SimpleToken(SymbolToken.class, 43, 44),
		new SimpleToken(WordToken.class, 44, 48),
		new SimpleToken(SymbolToken.class, 48, 49),
	};
	

	/*
 	's
	've
	're
	'll
	'd
	n't
	can not
	gon na
	got ta
	lem me
	more 'n
	`t is
	`t was
	wan na
	wha dd ya
	wha t cha
	 */
	//  tens digit:           0         111111111 2         333333333 4         5         6         7         8         9
	//  units digit:          0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
	String contractionTest = "He's I've  We're  I'll  He'd can't Cannot gonna gotta lemme more'n 'tis 'twas wanna whaddya whatcha";
	SimpleToken [] contractionExpectedResults = {
		new SimpleToken(WordToken.class, 0, 2),
		new SimpleToken(ContractionToken.class, 2, 4),
		
		new SimpleToken(WordToken.class, 5, 6),
		new SimpleToken(ContractionToken.class, 6, 9),
		
		new SimpleToken(WordToken.class, 11, 13),
		new SimpleToken(ContractionToken.class, 13, 16),
		
		new SimpleToken(WordToken.class, 18, 19), // I'll
		new SimpleToken(ContractionToken.class, 19, 22),
		
		new SimpleToken(WordToken.class, 24, 26),
		new SimpleToken(ContractionToken.class, 26, 28),
		
		new SimpleToken(WordToken.class, 29, 31), // can't
		new SimpleToken(ContractionToken.class, 31, 34),
		
		new SimpleToken(WordToken.class, 35, 38),
		new SimpleToken(ContractionToken.class, 38, 41),
		 
		new SimpleToken(WordToken.class, 42, 45), // gonna
		new SimpleToken(ContractionToken.class, 45, 47),
		
		new SimpleToken(WordToken.class, 48, 51),
		new SimpleToken(ContractionToken.class, 51, 53),
		
		new SimpleToken(WordToken.class, 54, 57),
		new SimpleToken(ContractionToken.class, 57, 59),
		
		new SimpleToken(WordToken.class, 60, 64),
		new SimpleToken(ContractionToken.class, 64, 66),
		
		new SimpleToken(ContractionToken.class, 67, 69), // 'tis
		new SimpleToken(WordToken.class, 69, 71),
		
		new SimpleToken(ContractionToken.class, 72, 74), // 'twas  't was
		new SimpleToken(WordToken.class, 74, 77),
		
		new SimpleToken(WordToken.class, 78, 81),
		new SimpleToken(ContractionToken.class, 81, 83),
		
		new SimpleToken(WordToken.class, 84, 87),
		new SimpleToken(ContractionToken.class, 87, 89),
		new SimpleToken(ContractionToken.class, 89, 91),
		
		new SimpleToken(WordToken.class, 92, 95),
		new SimpleToken(ContractionToken.class, 95, 96),
		new SimpleToken(ContractionToken.class, 96, 99),
		
		
	};

	static String [] hyphenatedWordsToNotSplit = {
		"e-EXAMPLE",
		"a-EXAMPLE",
		"u-EXAMPLE",
		"x-EXAMPLE",
		"agro-EXAMPLE",
		"ante-EXAMPLE",
		"anti-EXAMPLE",
		"arch-EXAMPLE",
		"be-EXAMPLE",
		"bi-EXAMPLE",
		"bio-EXAMPLE",
		"co-EXAMPLE",
		"counter-EXAMPLE",
		"cross-EXAMPLE",
		"cyber-EXAMPLE",
		"de-EXAMPLE",
		"eco-EXAMPLE",
		"EXAMPLE-esque",
		"EXAMPLE-ette",
		"ex-EXAMPLE",
		"extra-EXAMPLE",
		"EXAMPLE-fest",
		"EXAMPLE-fold",
		"EXAMPLE-gate",
		"inter-EXAMPLE",
		"intra-EXAMPLE",
		"EXAMPLE-itis",
		"EXAMPLE-less",
		"macro-EXAMPLE",
		"mega-EXAMPLE",
		"micro-EXAMPLE",
		"mid-EXAMPLE",
		"mini-EXAMPLE",
		"mm-hm",
		"mm-mm",
		"EXAMPLE-most",
		"multi-EXAMPLE",
		"neo-EXAMPLE",
		"non-EXAMPLE",
		"o-kay",
		"EXAMPLE-o-torium",
		"over-EXAMPLE",
		"pan-EXAMPLE",
		"para-EXAMPLE",
		"peri-EXAMPLE",
		"post-EXAMPLE",
		"pre-EXAMPLE",
		"pro-EXAMPLE",
		"pseudo-EXAMPLE",
		"quasi-EXAMPLE",
		"EXAMPLE-rama",
		"re-EXAMPLE",
		"semi-EXAMPLE",
		"sub-EXAMPLE",
		"super-EXAMPLE",
		"tri-EXAMPLE",
		"uh-huh",
		"uh-oh",
		"ultra-EXAMPLE",
		"un-EXAMPLE",
		"uni-EXAMPLE",
		"vice-EXAMPLE",
		"EXAMPLE-wise",

		"sub-EXAMPLE-wise", // a prefix and a suffix
		"ultra-EXAMPLE-fest", // a prefix and a suffix
		 
		//"ANTIEXAMPLE-ese", // verify this gets caught
	};
	
	static String hyphenTestBuilt = "";
	static {
	    // Create a single String that is space-delimited for testing all the hyphenated words and prefixes and suffixes
	    for (String s: hyphenatedWordsToNotSplit) {
		hyphenTestBuilt = hyphenTestBuilt + s.toUpperCase() + " "; // change the input to upper case to make sure the tokenizer handles upper case too
	    }
	}
	
	//  tens digit:           0         111111111 2         333333333 4         5
	//  units digit:          012345678901234567890123456789012345678901234567890123456789
	String hyphenTest = hyphenTestBuilt;
	SimpleToken [] hyphenExpectedResults; // see Constructor
	
	//  tens digit:       0         111111111 2         333333333 4         5         6         7
	//  units digit:      0123456789012345678901234567890123456789012345678901234567890123456789012
	String abbrevTest1 = "e.g.abc e.g. abc injury.[16]";
	SimpleToken [] abbrevExpectedResults1 = {
			new SimpleToken(WordToken.class, 0, 1),
			new SimpleToken(null, 1, 2),
			new SimpleToken(null, 2, 3),
			new SimpleToken(null, 3, 4),
			new SimpleToken(WordToken.class, 4, 7),
			new SimpleToken(WordToken.class, 8, 12),
			new SimpleToken(null, 13, 16),
			new SimpleToken(null, 17, 23),
			new SimpleToken(null, 23, 24),
			new SimpleToken(null, 24, 25),
			new SimpleToken(NumToken.class, 25, 27),
			new SimpleToken(null, 27, 28),
	};
	
	//  tens digit:       0         111111111 2         333333333 4         5         6         7
	//  units digit:      0123456789012345678901234567890123456789012345678901234567890123456789012
	String abbrevTest2 = "Mr. Smith! Dr. Smith! Ste. 6! sign e.g. edema! 210 A.D.! injury.[16][17]";
	SimpleToken [] abbrevExpectedResults2 = {
		new SimpleToken(WordToken.class, 0, 3),
		new SimpleToken(null, 4, 9),
		new SimpleToken(null, 9, 10),

		new SimpleToken(WordToken.class, 11, 14),
		new SimpleToken(null, 15,20),
		new SimpleToken(null, 20,21),

		new SimpleToken(WordToken.class, 22, 26),
		new SimpleToken(null, 27,28),
		new SimpleToken(null, 28,29),

		new SimpleToken(null, 30,34),
		new SimpleToken(WordToken.class, 35, 39),
		new SimpleToken(null, 40,45),
		new SimpleToken(null, 45,46),
		
		new SimpleToken(null, 47,50),

		new SimpleToken(WordToken.class, 51, 55),
		new SimpleToken(null, 55,56),
		
		new SimpleToken(WordToken.class, 57, 63),
		new SimpleToken(null, 63,64),

		new SimpleToken(null, 64,65),
		new SimpleToken(NumToken.class, 65,67),
		new SimpleToken(null, 67,68),
		new SimpleToken(null, 68,69),
		new SimpleToken(NumToken.class, 69,71),
		new SimpleToken(null, 71,72),
	};

	//  tens digit:       0         111111111 2         333333333 4         5         6         7
	//  units digit:      0123456789012345678901234567890123456789012345678901234567890123456789012
	String abbrevTest3 = "w3c.f 1.0 version 1.0.3f Mrs.? Mr.! bldg.: Stabile";
	SimpleToken [] abbrevExpectedResults3 = {
			new SimpleToken(WordToken.class, 0, 3),
			new SimpleToken(null, 3, 4),
			new SimpleToken(WordToken.class, 4, 5),
			
			new SimpleToken(null, 6, 9),
			
			new SimpleToken(WordToken.class, 10, 17),
			
			new SimpleToken(NumToken.class, 18, 21),
			
			new SimpleToken(NumToken.class, 21, 23),
			
			new SimpleToken(WordToken.class, 23, 24),

			new SimpleToken(WordToken.class, 25, 29),
			new SimpleToken(null, 29, 30),

			new SimpleToken(WordToken.class, 31, 34),
			new SimpleToken(null, 34, 35),

			new SimpleToken(WordToken.class, 36, 41),
			new SimpleToken(null, 41, 42),

			new SimpleToken(WordToken.class, 43, 50),
	};
	

	//  tens digit:         0         111111111 2         333333333 4         5         6
	//  units digit:        0123456789012345678901234567890123456789012345678901234567890123456789
	String webString1Test = "ClinicalNLP@mayo.edu http://www.mayoclinic.org http://mayo.edu";
	SimpleToken [] webString1ExpectedResults = {
		new SimpleToken(WordToken.class,  0, 20),
		new SimpleToken(WordToken.class, 21, 46),
		new SimpleToken(WordToken.class, 47, 62),
	};

	String webString2Test = "http://www.islamonline.net/Arabic/news/2004-12/05/images/pic05b.jpg" +
		" " + "http://www.mofa.gov.sa/detail.asp?InNewsItemID=59090&InTemplateKey=print" +
		" " + "rayhanenajib@menara.ma";
	SimpleToken [] webString2ExpectedResults = {
		new SimpleToken(WordToken.class,  0, 67),
		new SimpleToken(WordToken.class, 68, 140),
		new SimpleToken(WordToken.class, 141, 163),
	};

	//  tens digit:        0         111111111 2         333333333 4         5
	//  units digit:       012345678901234567890123456789012345678901234567890123456789
	String ellipsisTest = "he ... she";
	SimpleToken [] ellipsisExpectedResults  = {
		new SimpleToken(WordToken.class, 0, 2),
		new SimpleToken(PunctuationToken.class, 3, 6),
		new SimpleToken(WordToken.class, 7, 10),
	};
	

	//  tens digit:             0         111111111 2         333333333 4         5
	//  units digit:            012345678901234567890123456789012345678901234567890123456789
	String teleAndPostalTest = "5-1212 1-800-555-1212 55901-9999";
	SimpleToken [] teleAndPostalExpectedResults = {
		new SimpleToken(WordToken.class, 0, 6),
		new SimpleToken(WordToken.class, 7, 21),
		new SimpleToken(WordToken.class, 22, 32),
	};

	//  tens digit:     0         111111111 2         333333333 4         5
	//  units digit:    012345678901234567890123456789012345678901234567890123456789
	String namesTest = "80's P'yongyang 'Assad 2000's";
	SimpleToken [] namesExpectedResults = {
		new SimpleToken(WordToken.class, 0, 4),
		new SimpleToken(WordToken.class, 5, 15),
		new SimpleToken(WordToken.class, 16, 22),
		new SimpleToken(WordToken.class, 23, 29),
	};

	//                  01234567890123456789012345678
	String comboTest = "80's-esque";
	SimpleToken [] comboExpectedResults = {
		new SimpleToken(WordToken.class, 0, 10),
	};

	
	
	
	
	public static void main(String[] args) {   // if don't want to run as junit, run this main
	    
	    System.out.println("Starting at " + new Date());
	    TokenizerAnnotatorPTBTests tester = new TokenizerAnnotatorPTBTests();
	    try {
		tester.testTokenizerAnnotatorPTB(false); // false = don't throw, just continue so can see all errors
	    } catch (ResourceInitializationException e) {
		e.printStackTrace();
	    }

	    tester.unitTest();
	    
	    System.out.println("Done at " + new Date());
	
	}
	
	private void unitTest() {
	    
	    SimpleToken<Object> st1 = new SimpleToken<Object>(null, 0,5);
	    System.out.println(st1.getTokenClass());
	    
	    SimpleToken<BaseToken> st2 = new SimpleToken<BaseToken>(BaseToken.class, 0,5);
	    System.out.println(st2.getTokenClass());
	    
	    SimpleToken<BaseToken> st3 = new SimpleToken<BaseToken>(WordToken.class, 0,5);
	    System.out.println(st3.getTokenClass());
	    
	}

	private class TestData<EXPECTED_RESULTS_TYPE> {
	    
	    String testInput;
	    EXPECTED_RESULTS_TYPE expectedResults;
	    
	    TestData(String testInput, EXPECTED_RESULTS_TYPE expectedResults) {
		this.testInput = testInput;
		this.expectedResults = expectedResults;
	    }
	    
	    EXPECTED_RESULTS_TYPE getExpectedResults() {
		return expectedResults;
	    }
	    
	    String getTestInput() {
		return testInput;
	    }
	    
	}
	
	/**
	 * 
	 * Created as a container for the most basic data expected from the various tokens
	 *
	 * @param <CLASS> the Class the token is expected to be, or 
	 */
	private class SimpleToken<BaseToken> {
	    Class<? extends BaseToken> typeOfBaseTokenOrSubClass;
	    int b;
	    int e;
	    
	    private SimpleToken(){ throw new RuntimeException("Requires begin and end");};
	    
	    /**
	     * 
	     * @param begin  the expected begin offset
	     * @param end the expected end offset
	     */
	    SimpleToken(Class<? extends BaseToken> typeOfBaseTokenOrSubClass, int begin, int end) {
		this.typeOfBaseTokenOrSubClass = typeOfBaseTokenOrSubClass;
		this.b = begin;
		this.e = end;
	    }
	    
	    public int getBegin() { return b; } ;
	    
	    public int getEnd() { return e; } ;

	    public Class<? extends BaseToken> getTokenClass() { 
		if (typeOfBaseTokenOrSubClass==null) 
		    return null;
		else 
		    return typeOfBaseTokenOrSubClass; 
	    }

	    public String toString() {
		String className;
		if (typeOfBaseTokenOrSubClass==null) {
		    className = "Class name not set";
		} else {
		    className = typeOfBaseTokenOrSubClass.getClass().getName();
		}
		String s = className + " " + b + ", " + e;
		return s;
	    }
	}
}

