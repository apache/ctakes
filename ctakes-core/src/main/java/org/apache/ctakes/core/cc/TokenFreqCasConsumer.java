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
package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * This class creates a file that contains the frequencies of the word tokens found in a set
 * in a text collection.  This cas consumer could potentially be used to create a frequency
 * file for any kind of annotation but only counts Token annotations at the moment.   
 */

@PipeBitInfo(
      name = "Word Count Writer",
      description = "Writes a two-column BSV file containing Words and their total counts in a document.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class TokenFreqCasConsumer extends CasConsumer_ImplBase
{ 
	/**
	 * The name of the parameter that is specifies the path of the output file in the
	 * descriptor file.  The value is "TokenFreqFile" and should be set in the descriptor
	 * file.
	 */

	public static final String PARAM_WORD_FREQ_FILE = "TokenFreqFile";
	File wordFreqFile;
	Map<String, int[]> wordFreqs;

/**
 * This method opens/creates the file specified by "TokenFreqFile" and initializes the 
 * data structure that will keep track of frequency counts.
 * @see org.apache.uima.collection.CasConsumer_ImplBase#initialize()
 */
@Override
public void initialize() throws ResourceInitializationException {
   try
		{
			String wordFreqFileName = (String) getConfigParameterValue(PARAM_WORD_FREQ_FILE);
			wordFreqFile = new File(wordFreqFileName);
			if(!wordFreqFile.exists())
			{
				wordFreqFile.createNewFile();
			}
		}
		catch(Exception ioe)
		{
			throw new ResourceInitializationException(ioe);
		}
		wordFreqs = new HashMap<String, int[]>();
	}

	/**
	 * Iterates through all of the WordTokenAnnotation's, gets the covered text for each annotation
	 * and increments the frequency count for that text.  
	 * 
	 * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
	 */
   @Override
   public void processCas( CAS cas ) throws ResourceProcessException {
      try
		{
			JCas jcas;
			jcas = cas.getJCas();
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
	        Iterator<?> tokenItr = indexes.getAnnotationIndex(WordToken.type).iterator();
	        while (tokenItr.hasNext())
	        {
	        	WordToken token = (WordToken) tokenItr.next();
	        	String text = token.getCoveredText();
	        	if(!wordFreqs.containsKey(text))
	        	{
	        		wordFreqs.put(text, new int[1]);
	        	}
              wordFreqs.get( text )[ 0 ]++;
           }
      }
		catch(Exception exception)
		{
			throw new ResourceProcessException(exception);
		}
	}

	/**
	 * This method sorts the frequency counts and prints out the resulting frequencies in descending
	 * order to the frequency file in 'word|count' format.
	 */
   @Override
   public void collectionProcessComplete( ProcessTrace arg0 ) throws ResourceProcessException, IOException {
      //sortedFreqs will contain objects of type Object[] of length 2.  The first object in the array
		//will hold the token and the second the frequency.  We want to sort on the frequency first in 
		//descending order and token in ascending order for those tokens with the same frequency. 
		TreeSet<Object[]> sortedFreqs = new TreeSet<Object[]>(
				new Comparator<Object[]>() {
					public int compare(Object[] tokenFreq1, Object[] tokenFreq2)
					{
						Integer freq1 = (Integer)tokenFreq1[1];
						Integer freq2 = (Integer)tokenFreq2[1];
						if(!freq2.equals(freq1))
							return freq2.compareTo(freq1);
						String token1 = (String)tokenFreq1[0];
						String token2 = (String)tokenFreq2[0];
						return token1.compareTo(token2); 
					}
				});
		
		Iterator<String> words = wordFreqs.keySet().iterator();
		while(words.hasNext())
		{
			String word = words.next();
         int freq = wordFreqs.get( word )[ 0 ];
         sortedFreqs.add( new Object[] { word, new Integer( freq ) } );
      }
		
		PrintStream out = new PrintStream(new FileOutputStream(wordFreqFile));
		Iterator<Object[]> freqs = sortedFreqs.iterator(); 
		while(freqs.hasNext())
		{
         Object[] tokenFreq = freqs.next();
         String word = (String)tokenFreq[ 0 ];
         int freq = ((Integer)tokenFreq[1]).intValue();
			out.println(word+"|"+freq);
		}
		out.flush();
		out.close();
	}
}
