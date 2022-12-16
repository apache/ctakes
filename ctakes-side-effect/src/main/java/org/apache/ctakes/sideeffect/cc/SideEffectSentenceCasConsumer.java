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
package org.apache.ctakes.sideeffect.cc;

import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.sideeffect.type.SESentence;
import org.apache.ctakes.sideeffect.util.SEUtil;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class SideEffectSentenceCasConsumer extends CasConsumer_ImplBase {
	public static final String PARAM_OUTPUT_FILE = "OutputFile";
	public static final String PARAM_DELIMITER = "Delimiter";  
	private BufferedWriter iv_bw = null;
	private String iv_delimiter;

	public void initialize() throws ResourceInitializationException {
		File outFile;

		try
		{
			String filename = (String) getConfigParameterValue(PARAM_OUTPUT_FILE);
			outFile = new File(filename);
			if (!outFile.exists())
				outFile.createNewFile();
			iv_bw = new BufferedWriter(new FileWriter(outFile));

			iv_delimiter = (String) getConfigParameterValue(PARAM_DELIMITER);

		} catch (Exception ioe)
		{
			throw new ResourceInitializationException(ioe);
		}
	}
	 
	public void processCas(CAS cas) throws ResourceProcessException {
		try {
			JCas jcas;
			jcas = SEUtil.getJCasViewWithDefault(cas, "plaintext");
         JFSIndexRepository indexes = jcas.getJFSIndexRepository();

         String docName = DocIdUtil.getDocumentID( jcas );

	        Iterator ssIter = indexes.getAnnotationIndex(SESentence.type).iterator();
	        while(ssIter.hasNext()) {
	        	SESentence ss = (SESentence) ssIter.next();
	        	
	        	String ssText = ss.getCoveredText().trim();
	        	
	        	String ssSpan = Integer.toString(ss.getBegin()) + ":" 
	        					+ Integer.toString(ss.getEnd());
	        		   
	        	String output = docName + iv_delimiter + ssText + iv_delimiter 
	        					+ ssSpan;
	        	
	        	iv_bw.write(output+"\n");
	        }

		} catch (Exception e) {
			throw new ResourceProcessException(e);
		}
	}
	
	public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException, IOException
	{
		super.collectionProcessComplete(arg0);

		try
		{
			iv_bw.flush();
			iv_bw.close();
		}
		catch(Exception e)
		{ throw new ResourceProcessException(e); }
	}
}
