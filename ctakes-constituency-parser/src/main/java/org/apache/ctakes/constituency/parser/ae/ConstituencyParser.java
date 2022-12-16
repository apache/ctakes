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
package org.apache.ctakes.constituency.parser.ae;

import org.apache.ctakes.constituency.parser.MaxentParserWrapper;
import org.apache.ctakes.constituency.parser.ParserWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;


@PipeBitInfo(
      name = "Constituency Parser",
		description = "Adds Terminal Treebank Nodes, necessary for Coreference Markables.",
		dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.SENTENCE },
      products = { PipeBitInfo.TypeProduct.TREE_NODE }
)
public class ConstituencyParser extends JCasAnnotator_ImplBase {
	public static final String PARAM_MODEL_FILENAME = "MODEL_FILENAME";
	
	@ConfigurationParameter(
			name = PARAM_MODEL_FILENAME,
			description = "File containing the opennlp-trained parser model",
			mandatory = false,
			defaultValue = "org/apache/ctakes/constituency/parser/models/sharpacq-3.1.bin"
	)
	private String modelFilename;

	public static final String PARAM_MAX_TOKENS = "MaxTokens";
	@ConfigurationParameter(name = PARAM_MAX_TOKENS,
				description = "The token limit for sentences we actually parse. Longer sentences will be ignored.",
				mandatory = false)
	private int maxTokens = -1;
	
	
	private ParserWrapper parser = null;
	private Logger logger = Logger.getLogger(this.getClass());

	@Override
	public void initialize( final UimaContext aContext ) throws ResourceInitializationException {
		super.initialize( aContext );
		logger.info( "Initializing ..." );
		try ( DotLogger dotter = new DotLogger() ) {
			parser = new MaxentParserWrapper( FileLocator.getAsStream( modelFilename ), this.maxTokens );
		} catch ( IOException ioE ) {
			logger.error( "Error reading parser model file/directory: " + ioE.getMessage() );
			throw new ResourceInitializationException( ioE );
		}
		logger.info( "Finished." );
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		parser.createAnnotations(jcas);
	}
	
	  public static AnalysisEngineDescription createAnnotatorDescription(
		      String modelPath) throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		    		ConstituencyParser.class,
		    		ConstituencyParser.PARAM_MODEL_FILENAME,
		        modelPath);
		  }

	  public static AnalysisEngineDescription createAnnotatorDescription(int maxTokens) throws ResourceInitializationException {
			return AnalysisEngineFactory.createEngineDescription(
					ConstituencyParser.class,
					ConstituencyParser.PARAM_MAX_TOKENS,
					maxTokens);
	  }

	  public static AnalysisEngineDescription createAnnotatorDescription() 
			  throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		    		ConstituencyParser.class);
		  }	  
}
