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
import org.apache.ctakes.relationextractor.ae.features.TokenFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.ctakes.temporal.ae.feature.EventIndexOfSameSentenceRelationFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.EventPositionRelationFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.EventTimeRelationFeatureExtractor;
//import org.apache.ctakes.temporal.ae.feature.SRLRelationFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.SectionHeaderRelationExtractor;
//import org.apache.ctakes.temporal.ae.feature.TimeWordTypeRelationExtractor;
//import org.apache.ctakes.temporal.ae.feature.UnexpandedTokenFeaturesExtractor;
//import org.apache.ctakes.temporal.ae.feature.TemporalAttributeFeatureExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.EventTimeFlatTreeFeatureExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.EventVerbRelationTreeExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.TemporalPETExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.TemporalPathExtractor;

@PipeBitInfo(
		name = "E-T TLinker",
		description = "Creates Event - Time TLinks.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TIMEX },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventTimeRelationAnnotator extends RelationExtractorAnnotator {

   static private final Logger LOGGER = LoggerFactory.getLogger( "EventTimeRelationAnnotator" );

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory,
					double probabilityOfKeepingANegativeExample) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventTimeRelationAnnotator.class,
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
				EventTimeRelationAnnotator.class,
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
	@Deprecated
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventTimeRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         super.initialize( context );
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
//      LOGGER.info( "Finished." );
   }

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		LOGGER.info( "Finding Event-Time Relations ..." );
		super.process( jCas );
	}

	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				new TokenFeaturesExtractor()
				//				new UnexpandedTokenFeaturesExtractor() //use unexpanded version for i2b2 data
				, new PartOfSpeechFeaturesExtractor()
				, new TemporalAttributeFeatureExtractor()
				//				, new EventTimeFlatTreeFeatureExtractor()
				//				, new TemporalPETExtractor()
				//, new TemporalPathExtractor()
				//				, new EventVerbRelationTreeExtractor()
				, new NumberOfEventTimeBetweenCandidatesExtractor()
				//				, new SectionHeaderRelationExtractor()
				, new NearbyVerbTenseRelationExtractor()
				, new CheckSpecialWordRelationExtractor()
				, new NearestFlagFeatureExtractor()
				, new DependencyPathFeaturesExtractor()
				, new DependencyFeatureExtractor()
				//				, new SRLRelationFeaturesExtractor()// tried, but not helpful
				, new EventArgumentPropertyExtractor()
				, new OverlappedHeadFeaturesExtractor()
				//				, new EventTimeRelationFeatureExtractor()
				, new ConjunctionRelationFeaturesExtractor()
				//				, new EventPositionRelationFeaturesExtractor() //tried, but not helpful
				, new TimeXRelationFeaturesExtractor()
				, new TemporalPETFlatExtractor()
				, new TimeXPropertyRelationFeaturesExtractor()
				//				, new TimeWordTypeRelationExtractor() //tried, but not helpful
				//				, new EventIndexOfSameSentenceRelationFeaturesExtractor() //tried, but not helpful
				);
	}

	@Override
	protected Class<? extends Annotation> getCoveringClass() {
		return Sentence.class;
	}

	@Override
	public List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas,
			Annotation sentence) {
		List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();
		for (EventMention event : JCasUtil.selectCovered(jCas, EventMention.class, sentence)) {
			// ignore subclasses like Procedure and Disease/Disorder
			if (event.getClass().equals(EventMention.class)) {
//				boolean eventValid = false;
//				for (EventMention ev : JCasUtil.selectCovered(jCas, EventMention.class, event)){
//					if(!ev.getClass().equals(EventMention.class)){// if there is an valid UMLS type in the same span, then true
//						eventValid = true;
//						break;
//					}
//				}
//				if(eventValid){
					for (TimeMention time : JCasUtil.selectCovered(jCas, TimeMention.class, sentence)) {
						pairs.add(new IdentifiedAnnotationPair(event, time));
					}
//				}
			}
		}

		//only use gold pairs:
		//		for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
		//			Annotation arg1 = relation.getArg1().getArgument();
		//			Annotation arg2 = relation.getArg2().getArgument();
		//			EventMention event = null;
		//			TimeMention time = null;
		//			if(arg1 instanceof EventMention){
		//				 event = (EventMention) arg1;
		//			}else if(arg1 instanceof TimeMention){
		//				time = (TimeMention) arg1;
		//			}
		//			if(arg2 instanceof EventMention){
		//				 event = (EventMention) arg2;
		//			}else if(arg2 instanceof TimeMention){
		//				time = (TimeMention) arg2;
		//			}
		//			if(event != null && time != null){
		//				pairs.add(new IdentifiedAnnotationPair(event, time));
		//			}
		//		}

		return pairs;
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
		if (relation != null) {
			category = relation.getCategory();
		} else {
			relation = relationLookup.get(Arrays.asList(arg2, arg1));
			if (relation != null) {
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
