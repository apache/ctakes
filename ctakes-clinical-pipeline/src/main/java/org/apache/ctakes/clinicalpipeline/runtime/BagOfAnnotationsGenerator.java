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
package org.apache.ctakes.clinicalpipeline.runtime;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;

public abstract class BagOfAnnotationsGenerator<T extends Annotation,K> {

	private String outputDir = null;
	private CollectionReader reader = null;
	private AnalysisEngine ae = null;
	private static final String defaultAEPath = "desc/analysis_engine/AggregatePlaintextUMLSProcessor.xml";
	private Class<T> classOfT;
	
	/**
	 * @throws IOException -
	 * @throws UIMAException  -
	 */
	public BagOfAnnotationsGenerator(String inputDir, String outputDir) throws UIMAException, IOException{
		this(inputDir, outputDir, null);
	}
	
	public BagOfAnnotationsGenerator(String inputDir, String outputDir, String aePath) throws UIMAException, IOException {
		reader = CollectionReaderFactory
				.createReaderFromPath( "../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
						ConfigParameterConstants.PARAM_INPUTDIR, inputDir );
		this.ae = AnalysisEngineFactory.createEngineFromPath( aePath == null ? defaultAEPath : aePath );
		this.outputDir = outputDir;
		this.classOfT = getClassOfT();
	}
	
	public void process() throws UIMAException, IOException{
		JCasIterator casIter = new JCasIterator(reader, ae);
		while(casIter.hasNext()){
			JCas jcas = casIter.next();
         String docId = DocIdUtil.getDocumentID( jcas );
			
			// extract info from cas
			processCas(jcas, outputDir + File.separator + docId);
		}
		ae.destroy();
	}

	private void processCas(JCas jcas, String outputFilename) throws FileNotFoundException {
		PrintStream out = new PrintStream(outputFilename);

		Collection<T> annotations = JCasUtil.select(jcas, classOfT);
		for(T annot : annotations){
			K output = extractInformation(annot);
			if(output != null) out.println(output);
		}
		out.close();
	}
	
	protected abstract K extractInformation(T t);

	@SuppressWarnings( "unchecked" )
	private Class<T> getClassOfT() {
		ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		return (Class<T>) superclass.getActualTypeArguments()[0];
	}
}
