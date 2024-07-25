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
package org.apache.ctakes.constituency.parser;

import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.parser.chunking.Parser;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MaxentParserWrapper implements ParserWrapper {

	Parser parser = null;
	private String parseStr = "";
   static private final Logger LOGGER = LogManager.getLogger( "MaxentParserWrapper" );
    private int maxTokens;

	public MaxentParserWrapper(InputStream in){
	    this(in, -1);
    }

	public MaxentParserWrapper(InputStream is, int maxTokens){
		try {
			if (is!=null) {
				ParserModel model = new ParserModel(is);
				parser = new Parser(model, AbstractBottomUpParser.defaultBeamSize, AbstractBottomUpParser.defaultAdvancePercentage);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.maxTokens = maxTokens;
	}

	@Override
	public String getParseString(FSIterator tokens) {
		return parseStr;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.chboston.cnlp.ctakes.parser.ParserWrapper#createAnnotations(org.apache.uima.jcas.JCas)
	 * FIXME - Does not handle the case where a sentence is only numbers. This can happen at the end of a note
	 * after "real" sentences are done where a line is just a string of numbers (looks like a ZIP code).
	 * For some reason the built-in tokenizer does not like that.
	 */
	@Override
   public void createAnnotations( final JCas jcas ) throws AnalysisEngineProcessException {
      final String docId = DocIdUtil.getDocumentID( jcas );
      LOGGER.info( "Started processing: " + docId );
      // iterate over sentences
		Parse parse = null;
//      final Collection<Sentence> allSentences = org.apache.uima.fit.util.JCasUtil.select( jcas, Sentence.class );
//      for ( Sentence sentence : allSentences ) {
      final Map<Sentence, List<BaseToken>> sentenceTokenMap = JCasUtil.indexCovered( jcas, Sentence.class, BaseToken.class );
      for ( Map.Entry<Sentence, List<BaseToken>> sentenceTokens : sentenceTokenMap.entrySet() ) {
         final Sentence sentence = sentenceTokens.getKey();
         final String text = sentence.getCoveredText();
         if ( text.isEmpty() || isBorderOnly( text ) ) {
            continue;
         }
//         final FSArray terminalArray = TreeUtils.getTerminals( jcas, sentence );
         final FSArray terminalArray = TreeUtils.getTerminals( jcas, new ArrayList<>( sentenceTokens.getValue() ) );
         if(this.maxTokens > 0 && terminalArray.size() > this.maxTokens) continue;
         final String tokenString = TreeUtils.getSplitSentence( terminalArray );
         if ( tokenString.isEmpty() ) {
            parse = null;
         } else {
            final Parse inputTokens = TreeUtils.ctakesTokensToOpennlpTokens( sentence.getBegin(), text, terminalArray );
            parse = parser.parse( inputTokens );
         }
         final TopTreebankNode top = TreeUtils.buildAlignedTree( jcas, parse, terminalArray, sentence );
         top.addToIndexes();
		}
//      LOGGER.info( "Done parsing: " + docId );
   }

   /**
    * The parser has a really tough time dealing with text lines that act as borders
    *
    * @param text text for a sentence (or other annotation)
    * @return true if the text is a candidate for a line border, e.g. "===================="
    */
   static private boolean isBorderOnly( final String text ) {
      final char[] chars = text.toCharArray();
      for ( char c : chars ) {
         // assume that a letter or digit indicates actual text of some sort
         if ( Character.isLetterOrDigit( c ) ) {
            return false;
         }
      }
      return true;
   }



}
