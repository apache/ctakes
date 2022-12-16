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
package org.apache.ctakes.temporal.ae.feature;

import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.feature.extractor.CleartkExtractor.*;
import org.cleartk.util.ViewUriUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * for every UMLS entities covering the annotation, extracted related token features
 * @author CH151862
 *
 */
public class MultiTokenFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private FeatureExtractor1 coveredText = new CoveredTextExtractor();

	/**
	 * First word of the mention, last word of the mention, all words of the mention as a bag, the
	 * preceding 3 words, the following 3 words
	 */
	private FeatureExtractor1 tokenContext = new CleartkExtractor(
			BaseToken.class,
			coveredText,
			new FirstCovered(1),
			new LastCovered(1),
			new Bag(new Covered()),
			new Preceding(3),
			new Following(3));

	/**
	 * All extractors for mention 1, with features named to distinguish them from mention 2
	 */
	private FeatureExtractor1 mention1FeaturesExtractor = new NamingExtractor1(
			"mention1",
			new CombinedExtractor1(coveredText, tokenContext));

	/**
	 * All extractors for mention 2, with features named to distinguish them from mention 1
	 */
	private FeatureExtractor1 mention2FeaturesExtractor = new NamingExtractor1(
			"mention2",
			new CombinedExtractor1(coveredText, tokenContext));

	/**
	 * First word, last word, and all words between the mentions
	 */
	private CleartkExtractor tokensBetween = new CleartkExtractor(
			BaseToken.class,
			new NamingExtractor1("BetweenMentions", coveredText),
			new FirstCovered(1),
			new LastCovered(1),
			new Bag(new Covered()));

	private String cachedDocID = null;
	private Map<EventMention, Collection<EventMention>> coveringMap;

	/**
	 * Number of words between the mentions
	 */
	private DistanceExtractor nTokensBetween = new DistanceExtractor(null, BaseToken.class);

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation mention1, IdentifiedAnnotation mention2)
			throws AnalysisEngineProcessException {

		String docId=null;
		try{
			docId = ViewUriUtil.getURI(jCas).toString();// get docID
		}catch(Exception e){
         docId = DocIdUtil.getDocumentID( jCas );
		}
		if(!docId.equals(cachedDocID)){
			// rebuild event-event map
			cachedDocID = docId;
			coveringMap = JCasUtil.indexCovering(jCas, EventMention.class, EventMention.class);
		}

		List<Feature> features = new ArrayList<>();
		Annotation arg1 = mention1;
		Annotation arg2 = mention2;

		List<Annotation> arg1s = new ArrayList<>();		
		if(arg1 instanceof EventMention){
			for (EventMention event1 : coveringMap.get(arg1)){
				Annotation tempAnn =  getExpandedEvent(jCas, event1);
				if(tempAnn == null) tempAnn = event1;
				arg1s.add(tempAnn);
			}
			arg1 = getExpandedEvent(jCas, mention1);
			if(arg1 == null) arg1 = mention1;			
		}
		arg1s.add(arg1);

		List<Annotation> arg2s = new ArrayList<>();		
		if(arg2 instanceof EventMention){
			for (EventMention event2 : coveringMap.get(arg2)){
				Annotation tempAnn =  getExpandedEvent(jCas, event2);
				if(tempAnn == null) tempAnn = event2;
				arg2s.add(tempAnn);
			}
			arg2 = getExpandedEvent(jCas, mention2);
			if(arg2 == null) arg2 = mention2;			
		}
		arg2s.add(arg2);

		for (Annotation ann1 : arg1s){
			features.addAll(this.mention1FeaturesExtractor.extract(jCas, ann1));
			for (Annotation ann2: arg2s){
				features.addAll(this.tokensBetween.extractBetween(jCas, ann1, ann2));
				features.addAll(this.nTokensBetween.extract(jCas, ann1, ann2));
			}
		}
		for (Annotation ann2: arg2s){
			features.addAll(this.mention2FeaturesExtractor.extract(jCas, ann2));
		}
		
		return features;
	}

	private static TreebankNode getExpandedEvent(JCas jCas, IdentifiedAnnotation mention){
		// since events are single words, we are at a terminal node:
		List<TerminalTreebankNode> terms = JCasUtil.selectCovered(TerminalTreebankNode.class, mention);
		if(terms == null || terms.size() == 0){
			return null;
		}

		TreebankNode coveringNode = AnnotationTreeUtils.annotationNode(jCas, mention);
		if(coveringNode == null) return terms.get(0);

		String pos =terms.get(0).getNodeType(); 
		// do not expand Verbs
		if(pos.startsWith("V")) return coveringNode;

		if(pos.startsWith("N")){
			// get first NP node:
			while(coveringNode != null && !coveringNode.getNodeType().equals("NP")){
				coveringNode = coveringNode.getParent();
			}
		}else if(pos.startsWith("J")){
			while(coveringNode != null && !coveringNode.getNodeType().equals("ADJP")){
				coveringNode = coveringNode.getParent();
			}
		}
		if(coveringNode == null) coveringNode = terms.get(0);
		return coveringNode;    
	}
}
