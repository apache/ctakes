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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
//import java.util.logging.Logger;

import org.apache.ctakes.typesystem.type.syntax.NumToken;
//import org.apache.ctakes.temporal.ae.feature.treekernel.TemporalPETExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.TemporalSingleTreeExtractor;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class TimeXExtractor implements FeatureExtractor1 {

  private String name;
//  private TemporalPETExtractor path;
  private TemporalAttributeFeatureExtractor attr;
  private TimeWordTypeExtractor<IdentifiedAnnotation> timewd;
//  private TemporalSingleTreeExtractor treeExt;
  
//  private Logger logger = Logger.getLogger(this.getClass().getName());

  public TimeXExtractor() throws ResourceInitializationException {
    super();
    this.name = "TimeXFeature";
//    this.path = new TemporalPETExtractor();
    this.attr = new TemporalAttributeFeatureExtractor();
    this.timewd = new TimeWordTypeExtractor<>();
//    this.treeExt = new TemporalSingleTreeExtractor();
  }

  @Override
  public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
	  List<Feature> features = new ArrayList<>();
	  
	  //1 get covering sentence:
	  Map<EventMention, Collection<Sentence>> coveringMap =
			  JCasUtil.indexCovering(view, EventMention.class, Sentence.class);
	  EventMention targetTokenAnnotation = (EventMention)annotation;
	  Collection<Sentence> sentList = coveringMap.get(targetTokenAnnotation);
	  
	  //2 get TimeX
	  Map<Integer, IdentifiedAnnotation> timeDistMap = null;
	  
//	  List<Feature> treePath = new ArrayList<Feature>();
	  
	  //3 get Document Creation Time
//	  String sofastr = view.getSofaDataString();
//	  int start = sofastr.indexOf("meta rev_date=");
//	  int end = sofastr.indexOf(" start_date=");
//	  System.out.println(sofastr.substring(start, end));
//	  Collection<SourceData> sources = JCasUtil.select(view, SourceData.class);
//	  for(SourceData source : sources){
//			System.out.println("original date: "+source.getSourceOriginalDate());
//			System.out.println("revision date: "+source.getSourceRevisionDate());
//		}
	  
	  if (sentList != null && !sentList.isEmpty()){
		  timeDistMap = new TreeMap<>();
		  
		  //boolean hasNumberToken = false;
		  
		  for(Sentence sent : sentList) {
			  for (TimeMention time : JCasUtil.selectCovered(view, TimeMention.class, sent)) {
				  timeDistMap.put(Math.abs(time.getBegin() - annotation.getBegin()), time);
			  }
			  for (TimeAnnotation time : JCasUtil.selectCovered(view, TimeAnnotation.class, sent)) {
				  timeDistMap.put(Math.abs(time.getBegin() - annotation.getBegin()), time);
			  }
			  for (DateAnnotation time : JCasUtil.selectCovered(view, DateAnnotation.class, sent)) {
				  timeDistMap.put(Math.abs(time.getBegin() - annotation.getBegin()), time);
			  }
			  //for (NumToken number : JCasUtil.selectCovered(view, NumToken.class, sent)){
			//	  hasNumberToken = true;
			//	  int numDigit = number.getCoveredText().length();
			//	  features.add(new Feature("num_digit_numToken", numDigit));
			 // }
		  }

		  //if(hasNumberToken){
			//  features.add(new Feature("has_number_tokens_in_sentence"));
		  //}
		  
		  //get the closest Time Expression feature
		  for (Map.Entry<Integer, IdentifiedAnnotation> entry : timeDistMap.entrySet()) {
			  Feature feature = new Feature(this.name, entry.getValue().getCoveredText());
			  features.add(feature);
			  //			  logger.info("add time feature: "+ entry.getValue().getCoveredText() + entry.getValue().getTimeClass());
			  Feature indicator = new Feature("TimeXNearby", this.name);
			  features.add(indicator);
			  Feature type = new Feature("TimeXType", entry.getValue().getClass());
			  features.add(type);

			  //add PP get Heading preposition
			  for(TreebankNode treebankNode : JCasUtil.selectCovering(
					  view, 
					  TreebankNode.class, 
					  entry.getValue().getBegin(), 
					  entry.getValue().getEnd())) {

				  if(treebankNode.getNodeType().equals("PP")) {
					  Feature PPNodeType = new Feature("Timex_PPNodeType", treebankNode.getNodeType());
					  features.add(PPNodeType);
					  break;
				  }
			  }

			  //add path tree, timex attributes
			  try {
//				  treePath=this.path.extract(view, targetTokenAnnotation, entry.getValue());//add path between timex and event
				  features.addAll(this.attr.extract(view, targetTokenAnnotation, entry.getValue()));//add temporal attribute features
				  features.addAll(this.timewd.extract(view, entry.getValue()));
			  } catch (AnalysisEngineProcessException e) {
				  throw new IllegalArgumentException(String.format("error in gererating path feature:", features));
			  }
			  break;
		  }
	  }

//	  if (treePath.isEmpty()){
//		  try {
//			  features.addAll(this.treeExt.extract(view, targetTokenAnnotation));
//		  } catch (AnalysisEngineProcessException e) {
//			  throw new IllegalArgumentException(String.format("error in gererating path feature:", features));
//		  }
//	  }else{
//		  features.addAll(treePath);
//	  }


	  return features;
  }

}
