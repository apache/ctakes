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
package org.apache.ctakes.temporal.ae;

import com.google.common.collect.Lists;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.features.PartOfSpeechFeaturesExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.util.*;

@PipeBitInfo(
		name = "E-E I2B2 TLinker",
		description = "Creates Event - Event TLinks with I2B2 model.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.EVENT },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventI2B2RelationAnnotator extends RelationExtractorAnnotator {

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory,
					double probabilityOfKeepingANegativeExample) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventEventI2B2RelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory,
				RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
				// not sure why this has to be cast; something funny going on in uimaFIT maybe?
				(float) probabilityOfKeepingANegativeExample);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventEventI2B2RelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				modelPath);
	}

	/**
	 * @deprecated use String path instead of File.
	 * ClearTK will automatically Resolve the String to an InputStream.
	 * This will allow resources to be read within from a jar as well as File.  
	 */	  
	@SuppressWarnings("dep-ann")
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventEventI2B2RelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				//				new TokenFeaturesExtractor()
				new UnexpandedTokenFeaturesExtractor() //use unexpanded version for i2b2 data
				, new PartOfSpeechFeaturesExtractor()
				//	    		, new TemporalPETExtractor()
				, new EventArgumentPropertyExtractor()
				, new NumberOfEventTimeBetweenCandidatesExtractor()
//				, new SectionHeaderRelationExtractor()
				, new NearbyVerbTenseRelationExtractor()
				, new CheckSpecialWordRelationExtractor()
				, new UmlsFeatureExtractor()
				, new DependencyPathFeaturesExtractor()
				, new CoordinateFeaturesExtractor()
				, new OverlappedHeadFeaturesExtractor()
				, new SRLRelationFeaturesExtractor()
				, new NumberOfEventsInTheSameSentenceExtractor()
				//				, new EventPositionRelationFeaturesExtractor() //not helpful
				//				, new TimeXRelationFeaturesExtractor() //not helpful
				, new ConjunctionRelationFeaturesExtractor()
				//				, new DeterminerRelationFeaturesExtractor()
				, new EventTimeRelationFeatureExtractor()
//				, new TokenPropertyFeaturesExtractor()
//				, new DependingVerbsFeatureExtractor()
//				, new SpecialAnnotationRelationExtractor() //not helpful
//				, new TemporalPETFlatExtractor()
				//				, new EventInBetweenPropertyExtractor()
				//				, new EventOutsidePropertyExtractor()
				);
	}

	@Override
	protected Class<? extends Annotation> getCoveringClass() {
		return Sentence.class;
	}

	@Override
	protected List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas, Annotation sentence) {

		Map<EventMention, List<EventMention>> coveringMap =
				JCasUtil.indexCovering(jCas, EventMention.class, EventMention.class);

		List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();
		List<EventMention> events = new ArrayList<>(JCasUtil.selectCovered(jCas, EventMention.class, sentence));
		//filter events:
		List<EventMention> realEvents = Lists.newArrayList();
		for( EventMention event : events){
			if(event.getClass().equals(EventMention.class)){
				realEvents.add(event);
			}
		}
		events = realEvents;

		int eventNum = events.size();

		for (int i = 0; i < eventNum-1; i++){
			EventMention eventB = events.get(i);
			for(int j = i+1; j < eventNum; j++){
				EventMention eventA = events.get(j);

				if(j-i==1 || j-i==eventNum-1||ifDependent(jCas, eventA, eventB)){
//					if(this.isTraining()){
//						//pairing covering system events:
//						for (EventMention event1 : coveringMap.get(eventA)){
//							for(EventMention event2 : coveringMap.get(eventB)){
//								pairs.add(new IdentifiedAnnotationPair(event1, event2));							
//							}
//							pairs.add(new IdentifiedAnnotationPair(event1, eventB));
//						}
//						for(EventMention event2 : coveringMap.get(eventB)){
//							pairs.add(new IdentifiedAnnotationPair(eventA, event2));							
//						}
//						//pairing covered system events:
//						for(EventMention event1 : JCasUtil.selectCovered(jCas, EventMention.class, eventA)){
//							for(EventMention event2 : JCasUtil.selectCovered(jCas, EventMention.class, eventB)){
//								pairs.add(new IdentifiedAnnotationPair(event1, event2));
//							}
//							pairs.add(new IdentifiedAnnotationPair(event1, eventB));
//						}
//						for(EventMention event2 : JCasUtil.selectCovered(jCas, EventMention.class, eventB)){
//							pairs.add(new IdentifiedAnnotationPair(eventA, event2));
//						}
//					}
					pairs.add(new IdentifiedAnnotationPair(eventA, eventB));
				}
			}
		}


		//		if(eventNum >= 2){
		//			for ( int i = 0; i< eventNum -1 ; i ++){
		//				EventMention evI = events.get(i);
		//				for(int j = i+1; j< eventNum; j++){
		//					EventMention evJ = events.get(j);
		//					if(j-i==1 || j-i==eventNum-1){//if two events are consecutive, or major events
		//						pairs.add(new IdentifiedAnnotationPair(evJ, evI));
		//					}else if(ifDependent(jCas, evI, evJ)){//if the event pairs are dependent// eventNum < 7 && 
		//						pairs.add(new IdentifiedAnnotationPair(evJ, evI));
		//					}else{// if the 
		//						continue;
		//					}
		//				}
		//			}
		//		}

		return pairs;
	}
	
	private static boolean ifDependent(JCas jCas, EventMention ev1, EventMention ev2) {
		for (ConllDependencyNode firstNode : JCasUtil.selectCovered(jCas, ConllDependencyNode.class, ev1)) {//get the covered conll nodes within the first event
			String pos = firstNode.getPostag();
			if(pos.startsWith("NN")||pos.startsWith("VB")){//get the head node
				for(ConllDependencyNode nextNode : JCasUtil.selectCovered(jCas, ConllDependencyNode.class, ev2)){//get the covered conll nodes within the next event
					pos = nextNode.getPostag();
					if(pos.startsWith("NN")||pos.startsWith("VB")){//get the head node
						ConllDependencyNode ancestor = DependencyParseUtils.getCommonAncestor(firstNode, nextNode);
						if(ancestor==firstNode || ancestor==nextNode){
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void createRelation(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2, String predictedCategory) {
		RelationArgument relArg1 = new RelationArgument(jCas);
		relArg1.setArgument(arg1);
		relArg1.setRole("Arg1");
		relArg1.addToIndexes();
		RelationArgument relArg2 = new RelationArgument(jCas);
		relArg2.setArgument(arg2);
		relArg2.setRole("Arg2");
		relArg2.addToIndexes();
		TemporalTextRelation relation = new TemporalTextRelation(jCas);
		relation.setArg1(relArg1);
		relation.setArg2(relArg2);
		relation.setCategory(predictedCategory);
		relation.addToIndexes();
	}

	@Override
	protected String getRelationCategory(
			Map<List<Annotation>, BinaryTextRelation> relationLookup,
			IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) {
		BinaryTextRelation relation = relationLookup.get(Arrays.asList(arg1, arg2));
		String category = null;
		if (relation != null && relation instanceof TemporalTextRelation) {
			category = relation.getCategory();
		} else {
			relation = relationLookup.get(Arrays.asList(arg2, arg1));
			if (relation != null && relation instanceof TemporalTextRelation) {
				if(relation.getCategory().equals("OVERLAP")){
					category = relation.getCategory();
				}else if (relation.getCategory().equals("BEFORE")){
					category = "AFTER";
				}else if (relation.getCategory().equals("AFTER")){
					category = "BEFORE";
				}
			}
		}
		if (category == null && coin.nextDouble() <= this.probabilityOfKeepingANegativeExample) {
			category = NO_RELATION_CATEGORY;
		}
		return category;
	}
}
