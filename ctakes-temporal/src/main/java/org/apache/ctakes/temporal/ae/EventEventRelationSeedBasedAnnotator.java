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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.relationextractor.ae.features.PartOfSpeechFeaturesExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.relationextractor.ae.features.TokenFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.CheckSpecialWordRelationExtractor;
import org.apache.ctakes.temporal.ae.feature.ConjunctionRelationFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.DependencyParseUtils;
import org.apache.ctakes.temporal.ae.feature.DependencyPathFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.CoordinateFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.DependingVerbsFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.EmptyFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.MultiTokenFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.NoOtherETPuntInBetweenFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.EventInBetweenPropertyExtractor;
//import org.apache.ctakes.temporal.ae.feature.EventOutsidePropertyExtractor;
import org.apache.ctakes.temporal.ae.feature.SpecialAnnotationRelationExtractor;
import org.apache.ctakes.temporal.ae.feature.TemporalPETFlatExtractor;
import org.apache.ctakes.temporal.ae.feature.TokenPropertyFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.DeterminerRelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.EventArgumentPropertyExtractor;
import org.apache.ctakes.temporal.ae.feature.EventTimeRelationFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.EventPositionRelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.NumberOfEventsInTheSameSentenceExtractor;
import org.apache.ctakes.temporal.ae.feature.NearbyVerbTenseRelationExtractor;
import org.apache.ctakes.temporal.ae.feature.NumberOfEventTimeBetweenCandidatesExtractor;
import org.apache.ctakes.temporal.ae.feature.OverlappedHeadFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.SRLRelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.TimeXRelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.SectionHeaderRelationExtractor;
//import org.apache.ctakes.temporal.ae.feature.TemporalAttributeFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.UmlsFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.UnexpandedTokenFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.UnexpandedTokenFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.TemporalPETExtractor;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
//import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

@PipeBitInfo(
		name = "E-E Seed TLinker",
		description = "Creates Event - Event TLinks from Seeds.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.EVENT },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventRelationSeedBasedAnnotator extends RelationExtractorAnnotator {

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory,
					double probabilityOfKeepingANegativeExample,
					boolean expandEvent) throws ResourceInitializationException {
		eventExpansion = expandEvent;

		return AnalysisEngineFactory.createEngineDescription(
				EventEventRelationSeedBasedAnnotator.class,
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
				EventEventRelationSeedBasedAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				modelPath);
	}


	public static boolean eventExpansion = false;

	/**
	 * @deprecated use String path instead of File.
	 * ClearTK will automatically Resolve the String to an InputStream.
	 * This will allow resources to be read within from a jar as well as File.  
	 */	  
	@SuppressWarnings("dep-ann")
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventEventRelationSeedBasedAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				new UnexpandedTokenFeaturesExtractor() //new TokenFeaturesExtractor()		
				//								new EmptyFeaturesExtractor()
				, new PartOfSpeechFeaturesExtractor()
				, new EventArgumentPropertyExtractor()
				, new UmlsFeatureExtractor()
				, new DependencyPathFeaturesExtractor()
				, new OverlappedHeadFeaturesExtractor()

				, new NoOtherETPuntInBetweenFeaturesExtractor()
				//				, new NumberOfEventTimeBetweenCandidatesExtractor()
				//				, new NearbyVerbTenseRelationExtractor()
				, new CheckSpecialWordRelationExtractor()
				//				, new CoordinateFeaturesExtractor()
				//				, new SRLRelationFeaturesExtractor()
				//				, new NumberOfEventsInTheSameSentenceExtractor()
				//				, new ConjunctionRelationFeaturesExtractor()
				//				, new EventTimeRelationFeatureExtractor()

				//				new MultiTokenFeaturesExtractor()
				//				new UnexpandedTokenFeaturesExtractor() //use unexpanded version for i2b2 data
				//				, new EmptyFeaturesExtractor()

				//				, new SectionHeaderRelationExtractor()
				//				, new EventPositionRelationFeaturesExtractor() //not helpful
				//				, new TimeXRelationFeaturesExtractor() //not helpful
				//				, new DeterminerRelationFeaturesExtractor()
				//				, new TokenPropertyFeaturesExtractor()
				//				, new DependingVerbsFeatureExtractor()
				//				, new SpecialAnnotationRelationExtractor() //not helpful
				//								, new TemporalPETFlatExtractor()

				);
	}

	@Override
	protected Class<? extends Annotation> getCoveringClass() {
		return Sentence.class;
	}

	@Override
	protected List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas, Annotation sentence) {
		List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();

		//find all container events:
		HashSet<EventMention> containers = new HashSet<>();
		HashSet<EventMention> involvers = new HashSet<>();
		for(TemporalTextRelation relation : JCasUtil.select(jCas, TemporalTextRelation.class)){
			Annotation arg1 = relation.getArg1().getArgument();
			Annotation arg2 = relation.getArg2().getArgument();
			String type = relation.getCategory();
			if( withinSentence(arg1,sentence) && withinSentence(arg2,sentence) ){
				List<EventMention> events = new ArrayList<>(JCasUtil.selectBetween(jCas, EventMention.class, arg1, arg2));
				//filter events:
				List<EventMention> realEvents = Lists.newArrayList();
				for( EventMention event : events){
					if(event.getClass().equals(EventMention.class)){
						realEvents.add(event);
					}
				}
				events = realEvents;
				int eventNum = events.size();
				if(eventNum==0){//find adjacent pairs
					if(type.equals("CONTAINS")&& (arg1 instanceof EventMention)){
						containers.add((EventMention) arg1);
						if(arg2 instanceof EventMention) involvers.add((EventMention)arg2);
					}else if (type.equals("CONTAINS-1") &&(arg2 instanceof EventMention)){
						containers.add((EventMention) arg2);
						if(arg1 instanceof EventMention) involvers.add((EventMention)arg1);
					}
				}
			}
		}

		int containerNum = containers.size();
		if(containerNum==0){
			return pairs;
		}

		List<EventMention> events = new ArrayList<>(JCasUtil.selectCovered(jCas, EventMention.class, sentence));
		//filter events:
		List<EventMention> realEvents = Lists.newArrayList();
		for( EventMention event : events){
			if(event.getClass().equals(EventMention.class) && !containers.contains(event) && !involvers.contains(event)){
				realEvents.add(event);
			}
		}
		events = realEvents;

		int eventNum = events.size();

		if(eventNum == 0){
			return pairs;
		}

		for(int i=0; i< eventNum; i++){
			EventMention currentEvent = events.get(i);
			if(containerNum==1){
				for(EventMention container: containers){
					pairs.add(new IdentifiedAnnotationPair(container, currentEvent));
				}
			}else{//if there are multiple containers
				for(EventMention container: containers){
					boolean noContainerInBetween = true;
					for( EventMention inbetweenPotentialContainer : JCasUtil.selectBetween(jCas, EventMention.class, container, currentEvent)){
						if(containers.contains(inbetweenPotentialContainer)){
							noContainerInBetween = false;
							break;
						}
					}
					if(noContainerInBetween){
						pairs.add(new IdentifiedAnnotationPair(container, currentEvent));
					}
				}
			}
		}		

		return pairs;
	}

	private static boolean withinSentence(Annotation arg, Annotation sentence) {
		if(arg.getBegin()>=sentence.getBegin()&&arg.getEnd()<=sentence.getEnd())
			return true;
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
					//				}else if (relation.getCategory().equals("BEFORE")){
					//					category = "AFTER";
					//				}else if (relation.getCategory().equals("AFTER")){
					//					category = "BEFORE";
					//				}
				}else{
					category = relation.getCategory() + "-1";
				}
			}
		}
		if (category == null && coin.nextDouble() <= this.probabilityOfKeepingANegativeExample) {
			category = NO_RELATION_CATEGORY;
		}
		return category;
	}
}
