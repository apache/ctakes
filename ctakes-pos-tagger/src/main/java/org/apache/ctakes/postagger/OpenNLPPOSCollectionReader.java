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
package org.apache.ctakes.postagger;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This collection reader reads in part-of-speech training/test data in the
 * OpenNLP format. See note below for POS_DATA_FILE_PARAM for details about this
 * format. Each line in the file will correspond to a "document" - i.e.
 * getNext() will populate the CAS with information from one line of the file.
 */
@PipeBitInfo(
      name = "OpenNLP POS Reader",
      description = "Reads in part-of-speech training/test data in the OpenNLP format.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.BASE_TOKEN, PipeBitInfo.TypeProduct.SENTENCE }
)
public class OpenNLPPOSCollectionReader extends CollectionReader_ImplBase {

	/**
	 * "PosDataFile" is a required, single, string parameter that specifies the
	 * location of a data file that contains part-of-speech data in it. The
	 * format of the file should have one sentence per line where each word is
	 * followed immediately by "_" and it pos tag followed by a space.
	 * 
	 * <pre>
	 * 	 		IL-2_NN gene_NN expression_NN and_CC ...
	 * 	 	
	 * </pre>
	 */
	public static final String POS_DATA_FILE_PARAM = "PosDataFile";

	/**
	 * "LoadWordsOnly" is a optional, single, boolean parameter that determines
	 * whether or not the part-of-speech tags associated with each word will be
	 * loaded into the CAS or not. The default value is false.
	 */
	public static final String LOAD_WORDS_ONLY_PARAM = "LoadWordsOnly";

	BufferedReader input;
	String line = null;

	boolean loadWordsOnly;

	@Override
	public void initialize() throws ResourceInitializationException {
		try {
			String posDataFile = (String) getConfigParameterValue(POS_DATA_FILE_PARAM);
			input = new BufferedReader(new FileReader(posDataFile));
			Boolean paramValue = (Boolean) getConfigParameterValue(LOAD_WORDS_ONLY_PARAM);
			loadWordsOnly = paramValue == null ? false : paramValue;

		} catch (FileNotFoundException fnfe) {
			throw new ResourceInitializationException(fnfe);
		}
	}

	/**
	 * Some of the code in this method is based loosely on
	 * opennlp.tools.postag.POSEventCollector
	 */
	public void getNext(CAS cas) throws IOException, CollectionException {
		try {
			if (hasNext()) {
				JCas jCas = cas.getJCas();
				String[] tokens = line.split(" ");
				int wordStart = 0;
				int wordEnd = 0;
				int wordNumber = 0;
				StringBuffer documentText = new StringBuffer();
				for (String token : tokens) {
					int split = token.lastIndexOf("_");
					if(split == token.length()-1) {
						split = token.substring(0, token.length()-1).lastIndexOf("_");
					}
					if (split == -1) {
						line = null;
						throw new CollectionException("There is a problem in your training data: " + token
								+ " does not conform to the format WORD_TAG.", null);
					}
					String word = token.substring(0, split);
					wordEnd = wordStart + word.length();
					// Consider creating a token similar to the way
					// TokenConverter.convert method creates BaseToken's
					BaseToken baseToken = new BaseToken(jCas, wordStart, wordEnd);
					if (!loadWordsOnly) {
						String tag = token.substring(split + 1);
						baseToken.setPartOfSpeech(tag);
					}
					baseToken.setTokenNumber(wordNumber++);
					baseToken.addToIndexes();

					documentText.append(word + " ");
					wordStart = wordEnd + 1;
				}
				Sentence sentence = new Sentence(jCas, 0, wordEnd);
				sentence.setSentenceNumber(0);
				sentence.addToIndexes();
				jCas.setDocumentText(documentText.toString());
			}
		} catch (CASException ce) {
			throw new CollectionException(ce);
		}
		line = null;
	}

	public void close() throws IOException {
		input.close();
	}

	public Progress[] getProgress() {
		return null;
	}

	public boolean hasNext() throws IOException, CollectionException {
		if (line == null) {
			line = input.readLine();
		}
		if (line == null)
			return false;
		return true;
	}

}
