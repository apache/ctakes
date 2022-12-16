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
package org.apache.ctakes.temporal.ae.feature.treekernel;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.TreeFeature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.base.Function;


public class EventTimeFlatTreeFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>{

	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<Feature>();
		
		// make sure they're in the correct order
		if(arg1.getBegin() > arg2.getBegin()){
			IdentifiedAnnotation temp = arg1;
			arg1 = arg2;
			arg2 = temp;
		}
		
		SimpleTree bopTree = getTree(jcas, arg1, arg2, "BOP", new Function<BaseToken,String>(){public String apply(BaseToken t){ return t.getPartOfSpeech();}});
		SimpleTree bowTree = getTree(jcas, arg1, arg2, "BOW", new Function<BaseToken,String>(){public String apply(BaseToken t){ return t.getCoveredText();}});
		
		feats.add(new TreeFeature("TK_BOP_Tree", bopTree.toString()));
		feats.add(new TreeFeature("TK_BOW_Tree", bowTree.toString()));
		return feats;
	}

	private static SimpleTree getTree(JCas jcas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2, String label, Function<BaseToken, String> leafFun) throws AnalysisEngineProcessException{
		SimpleTree tree = new SimpleTree(label);
		SimpleTree arg1Tree = null;
		SimpleTree arg2Tree = null;
		String eventModality="";
		String timeClass="";
	
		// make sure we have the right classes
		if(arg1 instanceof EventMention){
		  EventMention mention = (EventMention) arg1;
		  if(mention.getEvent() != null && mention.getEvent().getProperties() != null){
		    eventModality = mention.getEvent().getProperties().getContextualModality();
		  }
			arg1Tree = new SimpleTree("EVENT-"+eventModality);
		}else if(arg1 instanceof TimeMention){
			timeClass = ((TimeMention)arg1).getTimeClass();
			arg1Tree = new SimpleTree("TIMEX-"+timeClass);
		}else{
		  throw new AnalysisEngineProcessException("Argument type not recognized (arg1) -- must be EventMention or TimeMention", null);
		}
		
		if(arg2 instanceof EventMention){
		  EventMention mention = (EventMention) arg2;
		  if(mention.getEvent() != null && mention.getEvent().getProperties() != null){
	      eventModality = mention.getEvent().getProperties().getContextualModality();
		  }
      arg2Tree = new SimpleTree("EVENT-"+eventModality);		  
		}else if(arg2 instanceof TimeMention){
      timeClass = ((TimeMention)arg2).getTimeClass();
      arg2Tree = new SimpleTree("TIMEX-"+timeClass);		  
    }else{
      throw new AnalysisEngineProcessException("Argument type not recognized (arg2) -- must be EventMention or TimeMention", null);
    }
		
		List<BaseToken> tokens = JCasUtil.selectCovered(jcas, BaseToken.class, arg1.getBegin(), arg2.getEnd());
		tree.addChild(arg1Tree);
		for(BaseToken token : tokens){
			SimpleTree tokenTree = new SimpleTree("TOK");
			tokenTree.addChild(new SimpleTree(leafFun.apply(token)));
			
			if(token.getEnd() <= arg1.getEnd()){
				arg1Tree.addChild(tokenTree);
			}else if(token.getBegin() >= arg2.getBegin()){
				arg2Tree.addChild(tokenTree);
			}else{
				tree.addChild(tokenTree);
			}
		}
		tree.addChild(arg2Tree);
		return tree;
	}
}
