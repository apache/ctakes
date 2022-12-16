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
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.assertion.util.NegationManualDepContextAnalyzer;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class NegationDependencyFeatureExtractor implements
		FeatureExtractor1<IdentifiedAnnotation> {

	NegationManualDepContextAnalyzer conAnal = null;

	public NegationDependencyFeatureExtractor(){
		conAnal = new NegationManualDepContextAnalyzer();
	}
	
	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation focusAnnotation)
			throws CleartkExtractorException {
		List<Feature> feats = new ArrayList<>();

		// get the dependency node for the annotation we're annotating
		ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, focusAnnotation);

		// walk up the tree to the root, which has a span of the whole sentence
		ConllDependencyNode rootNode = headNode;
		while(rootNode.getId() != 0){
			rootNode = rootNode.getHead();
		}
		// use the root node to get all the nodes for this sentence
		List<ConllDependencyNode> nodes = DependencyUtility.getDependencyNodes(jcas, rootNode);
		if(nodes.size() > 400){
			// most things with hundreds of tokens are not in fact syntactically interesting, but they take a really
			// long time to process, so we can skip them.
			return feats;
		}
		try {
			boolean[] regexFeats = conAnal.findNegationContext(nodes, headNode);
			for(int j = 0; j < regexFeats.length; j++){
				if(regexFeats[j]){
					feats.add(new Feature("DepPath_" + conAnal.getRegexName(j)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CleartkExtractorException(e);
		}
		return feats;
	}

}
