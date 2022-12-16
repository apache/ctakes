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

import org.apache.ctakes.core.nlp.tokenizer.TokenizerPTB;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * UIMA annotator that tokenizes based on Penn Treebank rules.
 * 
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "PTB Tokenizer",
      description = "Annotates Document Penn TreeBank Tokens.",
      dependencies = { SECTION, SENTENCE },
      products = { BASE_TOKEN }
)
public class TokenizerAnnotatorPTB extends JCasAnnotator_ImplBase
{
	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Value is "SegmentsToSkip".  This parameter specifies which segments to skip.  The parameter should be
	 * of type String, should be multi-valued and optional. 
	 */
	public static final String PARAM_SEGMENTS_TO_SKIP = "SegmentsToSkip";
  @ConfigurationParameter(
      name = PARAM_SEGMENTS_TO_SKIP,
      mandatory = false,
      description = "Set of segments that can be skipped"
      )
  private String[] skipSegmentsArray;
  private Set<String> skipSegmentsSet;

	private TokenizerPTB tokenizer;

	private int tokenCount = 0;

	@Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		logger.info("Initializing " + this.getClass().getName());
		tokenizer = new TokenizerPTB();
		skipSegmentsSet = new HashSet<>();
    if(skipSegmentsArray != null){
      Collections.addAll(skipSegmentsSet, skipSegmentsArray);
    }
	}

	/**
	 * Entry point for processing.
	 */
	@Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

		logger.info("process(JCas) in " + this.getClass().getName());

		tokenCount = 0;

		Collection<Segment> segments = JCasUtil.select(jcas, Segment.class);
		for(Segment sa : segments){
			String segmentID = sa.getId();
			if (!skipSegmentsSet.contains(segmentID)) { 
				annotateRange(jcas, sa.getBegin(), sa.getEnd());
			}
		}
	}


	static char CR = '\r';
	static char LF = '\n';

	/**
	 * Tokenizes a range of text, adding the tokens to the CAS
	 * Tokenizes one sentence at a time. Only tokenizes what is within Sentence annotation.
	 * There must have been Sentence annotations created beforehand in order for this method
	 * to tokenize anything.
	 * @throws AnalysisEngineProcessException 
	 */
	protected void annotateRange(JCas jcas, int rangeBegin, int rangeEnd) throws AnalysisEngineProcessException {

		// int tokenCount = 0; // can't start with tokenCount=0 here because this method can be called multiple times

	  // First look for all newlines and carriage returns (which are not contained within sentences)
		String docText = jcas.getDocumentText();
		for (int i = rangeBegin; i<rangeEnd; i++) {

			if (docText.charAt(i)==CR) {

				NewlineToken nta;
				if (i+1<rangeEnd && docText.charAt(i+1)==LF) {
					// single NewlineToken for the 2 characters
					nta = new NewlineToken(jcas, i, i+2);
					i++; // skip past the LF
				} else {
					nta = new NewlineToken(jcas, i, i+1);
				}
				nta.addToIndexes();

			} else if (docText.charAt(i)==LF) {

				NewlineToken nta = new NewlineToken(jcas, i, i+1);
				nta.addToIndexes();

			}

		}

		// Now process each sentence
		Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
		
		// Tokenize each sentence, adding the tokens to the cas index
		for(Sentence sentence : sentences){
			if (sentence.getBegin() < rangeBegin || sentence.getEnd() > rangeEnd) {
				continue;
			}
			List<?> tokens = tokenizer.tokenizeTextSegment(jcas, sentence.getCoveredText(), sentence.getBegin(), true);
			for (Object bta: tokens) {
				if (bta==null) {
					Exception e = new RuntimeException("bta==null tokenCount=" + tokenCount + " tokens.size()==" + tokens.size());
					e.printStackTrace();
				} else{
					//logger.info("Token #" + tokenCount + " len = " + bta.getCoveredText().length() + " " + bta.getCoveredText());
					// add the BaseToken to CAS index
				  if(BaseToken.class.isAssignableFrom(bta.getClass())){
				    BaseToken.class.cast(bta).addToIndexes();
				  }else{
				    throw new AnalysisEngineProcessException("Token returned cannot be cast as BaseToken", new Object[]{bta});
				  }
					//tokenCount++;
				}
			}

		}

		// Now add the tokenNumber in the order of offsets
		Collection<BaseToken> tokens = JCasUtil.select(jcas, BaseToken.class);
		for(BaseToken bta : tokens){
			if (bta.getBegin()>=rangeBegin && bta.getBegin()<rangeEnd) {
				bta.setTokenNumber(tokenCount);
				tokenCount++;
			}
		}

	}
	
	public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
	  return AnalysisEngineFactory.createEngineDescription(TokenizerAnnotatorPTB.class);
	}
}	


// pseudo code:
// ** find first non white space or first newline. this starts token 1
// if eof before next newline or next nonwhite space, done.
// if nothing but whitespace before the newline, create NewlineToken tokenNumber=0 for first
// repeat until find non white space. this starts the first BaseToken that is
// not a NewlineToken and it will have tokenNumber = (#NewlineTokens + 0)

// once found start of a token (other than NewlineToken) process as follows to find end of token:
//  if char 2 is a whitespace or no char 2 (eof), then token len = 1, and  go back to looking for next token for nonwhitespace (**)
//  if 1st char of token is "." (period) could be a number or an ellipsis CALL startsWithPeriod
//  if 1st char of token is "'" (apostrophe) could be the start of a name CALL startsWithApostrophe
//  if 1st char of token is dash/minus sign, could be a number, CALL startsWithMinusSign
//  if 1st char of token is any other punctuation, it is a one-char token.  go back to looking for nonwhitespace (**)
//  if 1st char of token is alphanum, then follow these rules
//     stop when hit a whitespace or EOF
//     include any alphanum if just alphanums
//     if a hyphen/dash/minus
//		-- if part of hyphen list, 
//			then don't stop ****
//			else stop, found end of token (and start of next token)
//				Can't be a minus sign since wasn't first character.
//				and if it's a dash, should be a separate token
//				so if wasn't part of hyphen list, stop, found end of token
//     include comma only part of a number (if all others are digits or commas or a single period)
//     include period if
//		--- part of abbreviation
//		--- part of a number (if all others are digits or commas or a single period)



// From http://www.seas.upenn.edu/~jmott/2009_addendum.pdf
// which was moved to https://www.ldc.upenn.edu/sites/www.ldc.upenn.edu/files/etb-supplementary-guidelines-2009-addendum.pdf
// modified by Task1.4.4_adoptedConventions[AL]_Feb28_2011.doc

//All strings separated by white space are treated as separate tokens. 

//Also, no token can contain white space. 

//Most hyphenated words are split into multiple tokens.
//Hyphenated interjections and affixes in the following list are not split into multiple tokens.

//All other punctuation not described above triggers a break in tokenization, with the ex-
//ceptions outlined below.
//Note that for present purposes, all non-alphanumeric characters
//are considered `punctuation'.

//The tokenization of punctuation in webtext is deter-
//mined by whitespace boundaries.



// The following is the list of punctuation exceptions that do not cause end of token
//Periods marking abbreviations.
//Punctuation in web addresses.
// - URLs or email address
//Ellipses, when encoded as a string of periods.
//Complex numerals.
//Telephone numbers and postal codes.
//Single quotation marks as parts of names.



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
//    4
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

