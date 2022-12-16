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

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * <br>
* This class provides a UIMA wrapper for the CLEAR POSTagger. This pos tagger is available here:
 * <p>
 * http://code.google.com/p/clearnlp
 * <p>
 * 
 */
@TypeCapability(
		inputs = { 
				"org.apache.ctakes.typesystem.type.syntax.BaseToken:partOfSpeech",
				"org.apache.ctakes.typesystem.type.syntax.BaseToken:normalizedForm",
				"org.apache.ctakes.typesystem.type.syntax.BaseToken:tokenNumber",
				"org.apache.ctakes.typesystem.type.syntax.BaseToken:end",
				"org.apache.ctakes.typesystem.type.syntax.BaseToken:begin"
		})
@PipeBitInfo(
		name = "ClearNLP POS Tagger",
		description = "Adds Parts of Speech to Annotations.",
		role = PipeBitInfo.Role.ANNOTATOR,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class ClearNLPPOSTaggerAE extends JCasAnnotator_ImplBase {

	final String language = AbstractReader.LANG_EN;
	public Logger logger = Logger.getLogger(getClass().getName());
	
	// Default model values
	public static final String DEFAULT_MODEL_FILE_NAME = "org/apache/ctakes/postagger/models/clearnlp/mayo-en-pos-1.3.0.jar";

	
	
	// Configuration Parameters 
	public static final String PARAM_POS_MODEL_FILE_NAME = "POSModelFileName";
	@ConfigurationParameter(
			name = PARAM_POS_MODEL_FILE_NAME,
			description = "This parameter provides the file name of the Clear POS model required " +
					      "by the factory method provided by ClearNLPUtil.  If not specified, this " +
					      "analysis engine will use a default model from the resources directory")
	protected URI posModelUri;



	protected AbstractComponent postagger;


	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		try {

            	URL parserModelURL = (this.posModelUri == null)
                    ? this.getClass().getClassLoader().getResource(DEFAULT_MODEL_FILE_NAME).toURI().toURL()
                    : this.posModelUri.toURL();
                 
                    this.postagger = EngineGetter.getComponent(parserModelURL.openStream(), this.language, NLPLib.MODE_POS);

        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
			List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, sentence);
			DEPTree tree = new DEPTree();

			// Convert CAS data into structures usable by ClearNLP
			for (int i = 0; i < tokens.size(); i++) {
				BaseToken token = tokens.get(i);
				DEPNode node = new DEPNode(i+1, token.getCoveredText());
				tree.add(node);
			}

			// Run parser and convert output back to CAS friendly data types
			postagger.process(tree);
			
			for (int i = 0; i < tokens.size(); i++) {
				BaseToken token = tokens.get(i);
				DEPNode node = tree.get(i+1);
				token.setPartOfSpeech(node.pos);
			}
			
		}
		
		
	}
}
