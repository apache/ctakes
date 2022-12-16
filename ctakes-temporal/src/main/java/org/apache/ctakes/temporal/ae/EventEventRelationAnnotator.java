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
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.features.PartOfSpeechFeaturesExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.ctakes.temporal.ae.feature.DependencyParseUtils;
//import org.apache.ctakes.temporal.ae.feature.EventInBetweenPropertyExtractor;
//import org.apache.ctakes.temporal.ae.feature.EventOutsidePropertyExtractor;
//import org.apache.ctakes.temporal.ae.feature.TemporalAttributeFeatureExtractor;
//import org.apache.ctakes.temporal.ae.feature.UnexpandedTokenFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.TemporalPETExtractor;
//import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;

@PipeBitInfo(
		name = "E-E TLinker",
		description = "Creates Event - Event TLinks.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.EVENT },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventRelationAnnotator extends TemporalRelationExtractorAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "EventEventRelationAnnotator" );

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory,
					double probabilityOfKeepingANegativeExample) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventEventRelationAnnotator.class,
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
				EventEventRelationAnnotator.class,
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
				EventEventRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}
	
	private RelationSyntacticETEmbeddingFeatureExtractor embedingExtractor;

   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         super.initialize( context );
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      LOGGER.info( "Finished." );
   }

	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors() {
		final String vectorFile = "org/apache/ctakes/temporal/gloveresult_3";
		try {
			this.embedingExtractor = new RelationSyntacticETEmbeddingFeatureExtractor(vectorFile);
		} catch (CleartkExtractorException e) {
			System.err.println("cannot find file: "+ vectorFile);
			e.printStackTrace();
		}
		return Lists.newArrayList(
				new UnexpandedTokenFeaturesExtractor() //new TokenFeaturesExtractor()		
//				, new EmptyFeaturesExtractor()
				,embedingExtractor
				, new PartOfSpeechFeaturesExtractor()
				, new EventArgumentPropertyExtractor()
				, new UmlsFeatureExtractor()
				, new DependencyPathFeaturesExtractor()
				, new OverlappedHeadFeaturesExtractor()

				//				, new NumberOfEventTimeBetweenCandidatesExtractor()
				//				, new NearbyVerbTenseRelationExtractor()
				//				, new CheckSpecialWordRelationExtractor()
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
				//				, new TemporalPETFlatExtractor()

				);
	}

	@Override
	protected Class<? extends Annotation> getCoveringClass() {
		return Sentence.class;
	}

	@Override
	protected List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas, Annotation sentence) {

		//		Map<List<EventMention>, TemporalTextRelation> relationLookup = new HashMap<>();
		//	    if (this.isTraining()) {
		//	      relationLookup = new HashMap<>();
		//	      for (TemporalTextRelation relation : JCasUtil.select(jCas, TemporalTextRelation.class)) {
		//	        Annotation arg1 = relation.getArg1().getArgument();
		//	        Annotation arg2 = relation.getArg2().getArgument();
		//	        // The key is a list of args so we can do bi-directional lookup
		//	        if(arg1 instanceof EventMention && arg2 instanceof EventMention){
		//	        	List<EventMention> key = Arrays.asList((EventMention)arg1, (EventMention)arg2);
		//	        	relationLookup.put(key, relation);
		//	        }
		//	      }
		//	    }

//		Map<EventMention, Collection<EventMention>> coveringMap =
//				JCasUtil.indexCovering(jCas, EventMention.class, EventMention.class);

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
			for(int j = i+1; j < eventNum; j++){
				EventMention eventA = events.get(i);
				EventMention eventB = events.get(j);

				boolean eventAMedical = false;
				for( EventMention aEve : JCasUtil.selectCovering(jCas, EventMention.class, eventA)){
					if(!aEve.getClass().equals(EventMention.class)){//this event cover a UMLS semantic type
						eventAMedical = true;
						break;
					}
				}

				boolean eventBMedical = false;
				for( EventMention bEve : JCasUtil.selectCovering(jCas, EventMention.class, eventB)){
					if(!bEve.getClass().equals(EventMention.class)){//this event cover a UMLS semantic type
						eventBMedical = true;
						break;
					}
				}

				//				List<EventMention> key = Arrays.asList(eventA, eventB);
				
//				if(this.isTraining()){
//					//pairing covering system events:
//					if(eventAMedical || eventBMedical){
//						for (EventMention event1 : coveringMap.get(eventA)){
//							if(!hasOverlap(event1,eventB)){//don't generate overlapping arguments
//								pairs.add(new IdentifiedAnnotationPair(event1, eventB));
//							}
//
//							for(EventMention event2 : coveringMap.get(eventB)){
//								if(!hasOverlap(event1,event2)){//don't generate overlapping arguments
//									pairs.add(new IdentifiedAnnotationPair(event1, event2));
//								}
//							}
//						}
//						//					}
//						//					if(eventBMedical && !eventAMedical){
//						for(EventMention event2 : coveringMap.get(eventB)){
//							if(!hasOverlap(eventA,event2)){//don't generate overlapping arguments
//								pairs.add(new IdentifiedAnnotationPair(eventA, event2));
//							}
//						}
//					}
//				}
				pairs.add(new IdentifiedAnnotationPair(eventA, eventB));

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

	private static boolean noAdditionalEventsInBetween(JCas jCas, EventMention eventA,
			EventMention eventB) {

		List<EventMention> events = new ArrayList<>(JCasUtil.selectBetween(jCas, EventMention.class, eventA, eventB));
		//filter events:
		List<EventMention> realEvents = Lists.newArrayList();
		for( EventMention event : events){
			if(event.getClass().equals(EventMention.class)){
				realEvents.add(event);
			}
		}
		events = realEvents;
		if(events==null ||events.size()==0){
			return true;
		}
		return false;
	}

	private static boolean hasOverlap(Annotation event1, Annotation event2) {
		if(event1.getEnd()>=event2.getBegin()&&event1.getEnd()<=event2.getEnd()){
			return true;
		}
		if(event2.getEnd()>=event1.getBegin()&&event2.getEnd()<=event1.getEnd()){
			return true;
		}
		return false;
	}

	@Override
	protected void createRelation(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2, String predictedCategory, double confidence) {
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
		relation.setConfidence(confidence);
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
