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

//import java.io.File; //for normalization
//import java.io.IOException;//for normalization
//import java.net.URI;//for normalization

import com.google.common.collect.Lists;
import org.apache.ctakes.relationextractor.ae.features.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.util.ViewUriUtil;

import java.net.URL;
import java.util.*;

//import org.apache.ctakes.temporal.ae.feature.selection.ZscoreNormalizationExtractor;//for normalization

public abstract class TemporalRelationExtractorAnnotator extends CleartkAnnotator<String> {

	public static final String NO_RELATION_CATEGORY = "-NONE-";

	public static final String PARAM_PROB_VIEW = "ProbView";
	@ConfigurationParameter(name=PARAM_PROB_VIEW, mandatory=false)
	private String probViewname = null;

	public static final String PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE =
			"ProbabilityOfKeepingANegativeExample";

	public static Map<String, Integer> category_frequency = new LinkedHashMap<>();

	public static final String MINMAX_EXTRACTOR_KEY = "MINMAXFeatures";

	@ConfigurationParameter(
			name = PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
			mandatory = false,
			description = "probability that a negative example should be retained for training")
	protected double probabilityOfKeepingANegativeExample = 1.0;

	protected Random coin = new Random(0);

	private List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> featureExtractors;

	private Class<? extends Annotation> coveringClass = getCoveringClass();

	//private ZscoreNormalizationExtractor<String, Annotation> featureTransformExtractor;//for normalization

	//protected static URI minmaxExtractorURI;//for normalization

	//private static final String FEATURE_TRANSFORM_NAME = "TransformFeatures";//for normalization

	/**for normalization
	public static ZscoreNormalizationExtractor<String, Annotation> createMinMaxNormalizationExtractor() {
		return new ZscoreNormalizationExtractor<>(FEATURE_TRANSFORM_NAME);
	}

	public static URI createMinMaxNormalizationExtractorURI(File outputDirectoryName) {
		minmaxExtractorURI = new File(outputDirectoryName, FEATURE_TRANSFORM_NAME + "_Zscore_extractor.dat").toURI();
		return minmaxExtractorURI;
	}
	 */


	protected TemporalRelationExtractorAnnotator() {
		try {
			featureExtractors = getFeatureExtractors();
		} catch ( ResourceInitializationException riE ) {
			Logger.getLogger( "TemporalRelationExtractorAnnotator" ).error( riE.getMessage() );
		}
	}

	/**
	 * Defines the list of feature extractors used by the classifier. Subclasses
	 * may override this method to provide a different set of feature extractors.
	 * 
	 * @return The list of feature extractors to use.
	 */
	@SuppressWarnings("unchecked")
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				new TokenFeaturesExtractor(),
				new PartOfSpeechFeaturesExtractor(),
				new PhraseChunkingExtractor(),
				new NamedEntityFeaturesExtractor(),
				new DependencyTreeFeaturesExtractor(),
				new DependencyPathFeaturesExtractor());
	}

	protected Class<? extends BinaryTextRelation> getRelationClass() {
		return BinaryTextRelation.class;
	}

	/*
	 * Defines the type of annotation that the relation exists within (sentence,
	 * document, segment)
	 */
	protected abstract Class<? extends Annotation> getCoveringClass();

	/**
	 * Selects the relevant mentions/annotations within a covering annotation for
	 * relation identification/extraction.
	 */
	protected abstract List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas identifiedAnnotationView,
			Annotation coveringAnnotation);

	/**
	 * Workaround for https://code.google.com/p/cleartk/issues/detail?id=346
	 * 
	 * Not intended for external use
	 */
	static void allowClassifierModelOnClasspath(UimaContext context) {
		String modelPathParam = GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH;
		String modelPath = (String) context.getConfigParameterValue(modelPathParam);
		if (modelPath != null) {
			URL modelClasspathURL = TemporalRelationExtractorAnnotator.class.getResource(modelPath);
			if (modelClasspathURL != null) {
				UimaContextAdmin contextAdmin = (UimaContextAdmin) context;
				ConfigurationManager manager = contextAdmin.getConfigurationManager();
				String qualifiedModelPathParam = contextAdmin.getQualifiedContextName() + modelPathParam;
				manager.setConfigParameterValue(qualifiedModelPathParam, modelClasspathURL.toString());
			}
		}
	}

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {

      allowClassifierModelOnClasspath(context);
		super.initialize(context);
		//		minmaxExtractor = createMinMaxNormalizationExtractor();
		/**for normalization
		if (this.minmaxExtractorURI != null) {
			try {
				this.featureTransformExtractor = new ZscoreNormalizationExtractor<>(FEATURE_TRANSFORM_NAME);
				this.featureTransformExtractor.load(this.minmaxExtractorURI);
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}
		}*/
	}

	/*
	 * Implement the standard UIMA process method.
	 */
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		// lookup from pair of annotations to binary text relation
		// note: assumes that there will be at most one relation per pair
		Map<List<Annotation>, BinaryTextRelation> relationLookup;
		relationLookup = new HashMap<>();
		if (this.isTraining()) {
			relationLookup = new HashMap<>();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, this.getRelationClass())) {
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

		// walk through each sentence in the text
		for (Annotation coveringAnnotation : JCasUtil.select(jCas, coveringClass)) {

			// collect all relevant relation arguments from the sentence
			List<IdentifiedAnnotationPair> candidatePairs =
					this.getCandidateRelationArgumentPairs(jCas, coveringAnnotation);

			// walk through the pairs of annotations
			for (IdentifiedAnnotationPair pair : candidatePairs) {
				IdentifiedAnnotation arg1 = pair.getArg1();
				IdentifiedAnnotation arg2 = pair.getArg2();
				// apply all the feature extractors to extract the list of features
				List<Feature> features = new ArrayList<>();
				for (RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> extractor : this.featureExtractors) {
					List<Feature> feats = extractor.extract(jCas, arg1, arg2);
					if (feats != null)  features.addAll(feats);
				}

				// sanity check on feature values
				//List<Feature> transformedFeatures = new ArrayList<>();//for normalization
				for (Feature feature : features) {
					if (feature.getValue() == null) {
						feature.setValue("NULL");
						String message = String.format("Null value found in %s from %s", feature, features);
						System.err.println(message);
						//            throw new IllegalArgumentException(String.format(message, feature, features));
					}
					/**for normalization
					//transform feature:
					Object featureValue = feature.getValue();
					if (this.featureTransformExtractor != null) {
						if (featureValue instanceof Number) {
							transformedFeatures.add(featureTransformExtractor.transform(feature));
						}else{
							transformedFeatures.add(feature);
						}
					}*/
				}

				/**for normalization
				//transform features:
				if (this.featureTransformExtractor != null) {
					features = transformedFeatures;
				}*/

				// during training, feed the features to the data writer
				if (this.isTraining()) {
					String category = this.getRelationCategory(relationLookup, arg1, arg2);
					if (category == null) {
						continue;
					}

					//populate category_frequency count:
					if(category_frequency.containsKey(category)){
						category_frequency.put(category, category_frequency.get(category)+1);
					}else{
						category_frequency.put(category, 1);
					}
					// create a classification instance and write it to the training data
					this.dataWriter.write(new Instance<>(category, features));
				}

				// during classification feed the features to the classifier and create
				// annotations
				else {
					String predictedCategory = this.classify(features);
					/**
					Map<String,Double> scores = this.classifier.score(features);

					Map.Entry<String, Double> maxEntry = null;
					for( Map.Entry<String, Double> entry: scores.entrySet() ){
						if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
							maxEntry = entry;
						}
					}

					String predictedCategory = null;
					double confidence = 0d;
					if(maxEntry != null){
						predictedCategory = maxEntry.getKey();
						confidence = maxEntry.getValue().doubleValue();
					}

					// before creating the final relation (and possibly flipping the order of arguments) 
					// create the probabilistic copies in the other cas if that flag is set:
					if(probViewname != null){
						try {
							JCas probView = jCas.getView(probViewname);
							Map<String,Double> probs = SoftMaxUtil.getDistributionFromScores(scores);

							for(String label : probs.keySet()){
								createRelation(probView, arg1, arg2, label, probs.get(label));
							}
						} catch (CASException e) {
							e.printStackTrace();
							throw new AnalysisEngineProcessException(e);
						}
					}*/

					// add a relation annotation if a true relation was predicted
					if (predictedCategory != null && !predictedCategory.equals(NO_RELATION_CATEGORY)) {

						// if we predict an inverted relation, reverse the order of the
						// arguments
						if (predictedCategory.endsWith("-1")) {
							predictedCategory = predictedCategory.substring(0, predictedCategory.length() - 2);
							IdentifiedAnnotation temp = arg1;
							arg1 = arg2;
							arg2 = temp;
						}

						createRelation(jCas, arg1, arg2, predictedCategory, 0.0);
					}
				}
			} // end pair in pairs
		} // end for(Sentence)
	}

	/**
	 * Looks up the arguments in the specified lookup table and converts the
	 * relation into a label for classification
	 * 
	 * @return If this category should not be processed for training return
	 *         <i>null</i> otherwise it returns the label sent to the datawriter
	 */
	protected String getRelationCategory(
			Map<List<Annotation>, BinaryTextRelation> relationLookup,
			IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) {
		BinaryTextRelation relation = relationLookup.get(Arrays.asList(arg1, arg2));
		String category;
		if (relation != null) {
			category = relation.getCategory();
		} else if (coin.nextDouble() <= this.probabilityOfKeepingANegativeExample) {
			category = NO_RELATION_CATEGORY;
		} else {
			category = null;
		}
		return category;
	}

	/**
	 * Predict an outcome given a set of features. By default, this simply
	 * delegates to the object's <code>classifier</code>. Subclasses may override
	 * this method to implement more complex classification procedures.
	 * 
	 * @param features
	 *          The features to be classified.
	 * @return The predicted outcome (label) for the features.
	 */
	protected String classify(List<Feature> features) throws CleartkProcessingException {
		return this.classifier.classify(features);
	}

	/**
	 * Create a UIMA relation type based on arguments and the relation label. This
	 * allows subclasses to create/define their own types: e.g. coreference can
	 * create CoreferenceRelation instead of BinaryTextRelation
	 * 
	 * @param jCas
	 *          - JCas object, needed to create new UIMA types
	 * @param arg1
	 *          - First argument to relation
	 * @param arg2
	 *          - Second argument to relation
	 * @param predictedCategory
	 *          - Name of relation
	 * @param confidence 
	 * 		  - Confidence score of the relation prediction
	 */
	protected void createRelation(
			JCas jCas,
			IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2,
			String predictedCategory, 
			double confidence) {
		// add the relation to the CAS
		RelationArgument relArg1 = new RelationArgument(jCas);
		relArg1.setArgument(arg1);
		relArg1.setRole("Argument");
		relArg1.addToIndexes();
		RelationArgument relArg2 = new RelationArgument(jCas);
		relArg2.setArgument(arg2);
		relArg2.setRole("Related_to");
		relArg2.addToIndexes();
		BinaryTextRelation relation = new BinaryTextRelation(jCas);
		relation.setArg1(relArg1);
		relation.setArg2(relArg2);
		relation.setCategory(predictedCategory);
		relation.setConfidence(confidence);
		relation.addToIndexes();
	}

	public static class IdentifiedAnnotationPair {

		private final IdentifiedAnnotation arg1;
		private final IdentifiedAnnotation arg2;

		public IdentifiedAnnotationPair(IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) {
			this.arg1 = arg1;
			this.arg2 = arg2;
		}

		public final IdentifiedAnnotation getArg1() {
			return arg1;
		}

		public final IdentifiedAnnotation getArg2() {
			return arg2;
		}
	}

	// Object.finalize() was deprecated in jdk 9.  Given the manner of this code, this is a -reasonable- replacement.
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		if ( classifier instanceof AutoCloseable ) {
			try {
				((AutoCloseable)classifier).close();
			} catch ( Exception e ) {
				throw new AnalysisEngineProcessException( e );
			}
		}
	}


}
