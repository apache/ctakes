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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.relationextractor.ae.features.PartOfSpeechFeaturesExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.CheckSpecialWordRelationExtractor;
import org.apache.ctakes.temporal.ae.feature.DependencyPathFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.EventArgumentPropertyExtractor;
import org.apache.ctakes.temporal.ae.feature.OverlappedHeadFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.UmlsFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.UnexpandedTokenFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instances;
import org.cleartk.ml.crfsuite.CrfSuiteStringOutcomeDataWriter;
import org.cleartk.ml.jar.DefaultSequenceDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

@PipeBitInfo(
		name = "E-E CRF TLinker",
		description = "Creates Event - Event TLinks with Conditional Random Field.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.EVENT },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventCRFRelationAnnotator extends TemporalSequenceAnnotator_ImplBase {

	public static final String NO_RELATION_CATEGORY = "-NONE-";
	public static final String PARAM_TIMEX_VIEW = "TimexView";
	@ConfigurationParameter(
			name = PARAM_TIMEX_VIEW,
			mandatory = false,
			description = "View to write timexes to (used for ensemble methods)")
	protected String timexView = CAS.NAME_DEFAULT_SOFA;

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<CrfSuiteStringOutcomeDataWriter> dataWriterClass,
			File outputDirectory,
			double probabilityOfKeepingANegativeExample,
			boolean expandEvent) throws ResourceInitializationException {
		eventExpansion = expandEvent;

		return AnalysisEngineFactory.createEngineDescription(
				EventEventCRFRelationAnnotator.class,
				CleartkSequenceAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
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
				EventEventCRFRelationAnnotator.class,
				CleartkSequenceAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				modelPath);
	}


	public static boolean eventExpansion = false;
	private List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> featureExtractors;

	/**
	 * @deprecated use String path instead of File.
	 * ClearTK will automatically Resolve the String to an InputStream.
	 * This will allow resources to be read within from a jar as well as File.  
	 */	  
	@SuppressWarnings("dep-ann")
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				EventEventCRFRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

	protected EventEventCRFRelationAnnotator() {
		try {
			featureExtractors = getFeatureExtractors();
		} catch ( ResourceInitializationException riE ) {
			Logger.getLogger( "EventEventCRFRelationAnnotator" ).error( riE.getMessage() );
		}

	}

	@SuppressWarnings( "unchecked" )
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				new UnexpandedTokenFeaturesExtractor() //new TokenFeaturesExtractor()		
				, new PartOfSpeechFeaturesExtractor()
				, new EventArgumentPropertyExtractor()
				, new UmlsFeatureExtractor()
				, new DependencyPathFeaturesExtractor()
				, new OverlappedHeadFeaturesExtractor()
				, new CheckSpecialWordRelationExtractor()
				);
	}


	@Override
	public void process(JCas jCas, Segment segment)
			throws AnalysisEngineProcessException {
		Map<List<Annotation>, BinaryTextRelation> relationLookup;
		relationLookup = new HashMap<>();
		if (this.isTraining()) {
			relationLookup = new HashMap<>();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, TemporalTextRelation.class)) {
				Annotation arg1 = relation.getArg1().getArgument();
				Annotation arg2 = relation.getArg2().getArgument();
				// The key is a list of args so we can do bi-directional lookup
				List<Annotation> key = Arrays.asList(arg1, arg2);
				if(relationLookup.containsKey(key)){
					String reln = relationLookup.get(key).getCategory();
					System.err.println("Error in: "+ ViewUriUtil.getURI(jCas).toString());
					System.err.println("Error! This attempted relation " + relation.getCategory() + " already has a relation " + reln + " at this span: " + arg1.getCoveredText() + " -- " + arg2.getCoveredText());
				}
				relationLookup.put(key, relation);
			}
		}

		for (Sentence sentence : JCasUtil.selectCovered(jCas, Sentence.class, segment)) {
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
				EventMention eventA = events.get(i);
				if(i+1 <= eventNum){
					EventMention eventB = events.get(i+1);
					pairs.add(new IdentifiedAnnotationPair(eventA, eventB));
				}
			}
			
			// walk through the pairs of annotations
			List<String> outcomes = new ArrayList<>();
			List<List<Feature>> allFeatures = new ArrayList<>();
			for (IdentifiedAnnotationPair pair : pairs) {
				IdentifiedAnnotation arg1 = pair.getArg1();
				IdentifiedAnnotation arg2 = pair.getArg2();
				// apply all the feature extractors to extract the list of features
				List<Feature> features = new ArrayList<>();
				for ( RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> extractor : featureExtractors ) {
					List<Feature> feats = extractor.extract(jCas, arg1, arg2);
					if (feats != null)  features.addAll(feats);
				}

				allFeatures.add(features);

				// during training, feed the features to the data writer
				if (this.isTraining()) {
					String category = this.getRelationCategory(relationLookup, arg1, arg2);
					outcomes.add(category);
				}
			} // end pair in pairs
			// during training, the list of all outcomes for the relations

			if (this.isTraining()) {
				this.dataWriter.write(Instances.toInstances(outcomes, allFeatures));        
			}else{
				outcomes = this.classifier.classify(allFeatures);
				JCas timexCas;
				try {
					timexCas = jCas.getView(timexView);
				} catch (CASException e) {
					throw new AnalysisEngineProcessException(e);
				}
				int idx = 0;
				for (IdentifiedAnnotationPair pair : pairs) {
					IdentifiedAnnotation arg1 = pair.getArg1();
					IdentifiedAnnotation arg2 = pair.getArg2();
					String predictedCategory = outcomes.get(idx);
					if (!predictedCategory.equals(NO_RELATION_CATEGORY)) {
						createRelation(timexCas, arg1, arg2, predictedCategory);
					}
					idx++;
				}
			}//end if/else : training/testing
		}//end iterate sentence
	}



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
				}else{
					category = relation.getCategory() + "-1";
				}
			}
		}
		if (category == null) {
			category = NO_RELATION_CATEGORY;
		}
		return category;
	}
}
