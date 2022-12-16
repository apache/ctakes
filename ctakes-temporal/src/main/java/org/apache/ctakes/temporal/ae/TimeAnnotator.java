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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.feature.ParseSpanFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.TimeWordTypeExtractor;
import org.apache.ctakes.temporal.ae.feature.selection.Chi2FeatureSelection;
import org.apache.ctakes.temporal.ae.feature.selection.FeatureSelection;
import org.apache.ctakes.temporal.utils.SMOTEplus;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction.PatternType;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

@PipeBitInfo(
		name = "Time Annotator",
		description = "...",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.BASE_TOKEN },
		products = { PipeBitInfo.TypeProduct.TIMEX }
)
public class TimeAnnotator extends TemporalEntityAnnotator_ImplBase {

	public static final String PARAM_FEATURE_SELECTION_THRESHOLD = "WhetherToDoFeatureSelection";

	@ConfigurationParameter(
	    name = PARAM_FEATURE_SELECTION_THRESHOLD,
	    mandatory = false,
	    description = "the Chi-squared threshold at which features should be removed")
	protected Float featureSelectionThreshold = 1f;
	
	public static final String PARAM_FEATURE_SELECTION_URI = "FeatureSelectionURI";

	@ConfigurationParameter(
			mandatory = false,
			name = PARAM_FEATURE_SELECTION_URI,
			description = "provides a URI where the feature selection data will be written")
	protected URI featureSelectionURI;
	
	public static final String PARAM_SMOTE_NUM_NEIGHBORS = "NumOfNeighborForSMOTE";

	@ConfigurationParameter(
	    name = PARAM_SMOTE_NUM_NEIGHBORS,
	    mandatory = false,
	    description = "the number of neighbors used for minority instances for SMOTE algorithm")
	protected Float smoteNumOfNeighbors = 0f;

	public static final String PARAM_TIMEX_VIEW = "TimexView";
	@ConfigurationParameter(
	    name = PARAM_TIMEX_VIEW,
	    mandatory = false,
	    description = "View to write timexes to (used for ensemble methods)")
	protected String timexView = CAS.NAME_DEFAULT_SOFA;

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<?> dataWriterClass,
					File outputDirectory,
					float featureSelect,
					float smoteNeighborNumber) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				TimeAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory,
				TimeAnnotator.PARAM_FEATURE_SELECTION_THRESHOLD,
		        featureSelect,
		        EventAnnotator.PARAM_SMOTE_NUM_NEIGHBORS,
		        smoteNeighborNumber);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				TimeAnnotator.class,
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
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				TimeAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"),
				TimeAnnotator.PARAM_FEATURE_SELECTION_URI,
				TimeAnnotator.createFeatureSelectionURI(modelDirectory));
	}

	public static AnalysisEngineDescription createEnsembleDescription(File modelDirectory, String mappedView)
	    throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        TimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(modelDirectory, "model.jar"),
        TimeAnnotator.PARAM_TIMEX_VIEW,
        mappedView,
        TimeAnnotator.PARAM_FEATURE_SELECTION_URI,
        TimeAnnotator.createFeatureSelectionURI(modelDirectory));	  
	}
	
	protected List<FeatureExtractor1> tokenFeatureExtractors;

	protected List<CleartkExtractor> contextFeatureExtractors;

	//  protected List<FeatureExtractor1> parseFeatureExtractors;
	protected ParseSpanFeatureExtractor parseExtractor;

	private BioChunking<BaseToken, TimeMention> timeChunking;
	
	private FeatureSelection<String> featureSelection;

	private static final String FEATURE_SELECTION_NAME = "SelectNeighborFeatures";

	public static FeatureSelection<String> createFeatureSelection(double threshold) {
		return new Chi2FeatureSelection<String>(TimeAnnotator.FEATURE_SELECTION_NAME, threshold, true);
	}
	
	public static URI createFeatureSelectionURI(File outputDirectoryName) {
		return new File(outputDirectoryName, FEATURE_SELECTION_NAME + "_Chi2_extractor.dat").toURI();
	}

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		// define chunking
		this.timeChunking = new BioChunking<BaseToken, TimeMention>(BaseToken.class, TimeMention.class);

		CombinedExtractor1 allExtractors = new CombinedExtractor1(
				new CoveredTextExtractor(),
		        CharacterCategoryPatternFunction.<BaseToken>createExtractor(PatternType.REPEATS_MERGED),
		        CharacterCategoryPatternFunction.<BaseToken>createExtractor(PatternType.ONE_PER_CHAR),
				new TypePathExtractor(BaseToken.class, "partOfSpeech"),
				new TimeWordTypeExtractor());

		//    CombinedExtractor1 parseExtractors = new CombinedExtractor(
		//        new ParseSpanFeatureExtractor()
		//        );
		this.tokenFeatureExtractors = new ArrayList<FeatureExtractor1>();
		this.tokenFeatureExtractors.add(allExtractors);

		this.contextFeatureExtractors = new ArrayList<CleartkExtractor>();
		this.contextFeatureExtractors.add(new CleartkExtractor(
				BaseToken.class,
				allExtractors,
				new Preceding(3),
				new Following(3)));
		//    this.parseFeatureExtractors = new ArrayList<ParseSpanFeatureExtractor>();
		//    this.parseFeatureExtractors.add(new ParseSpanFeatureExtractor());
		parseExtractor = new ParseSpanFeatureExtractor();

		//initialize feature selection
		if (featureSelectionThreshold == 1) {
			this.featureSelection = null;
		} else {
			this.featureSelection = TimeAnnotator.createFeatureSelection(this.featureSelectionThreshold);

			if (this.featureSelectionURI != null) {
				try {
					this.featureSelection.load(this.featureSelectionURI);
				} catch (IOException e) {
					throw new ResourceInitializationException(e);
				}
			}
		}
	}

	@Override
	public void process(JCas jCas, Segment segment) throws AnalysisEngineProcessException {
		//TRY SMOTE algorithm here to generate more minority class samples
	    SMOTEplus smote = new SMOTEplus((int)Math.ceil(this.smoteNumOfNeighbors));
	    
		// classify tokens within each sentence
		for (Sentence sentence : JCasUtil.selectCovered(jCas, Sentence.class, segment)) {
			List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, sentence);
			
			// during training, the list of all outcomes for the tokens
			List<String> outcomes;
			if (this.isTraining()) {
				List<TimeMention> times = JCasUtil.selectCovered(jCas, TimeMention.class, sentence);
				outcomes = this.timeChunking.createOutcomes(jCas, tokens, times);
			}
			// during prediction, the list of outcomes predicted so far
			else {
				outcomes = new ArrayList<String>();
			}

			// extract features for all tokens
			int tokenIndex = -1;
			for (BaseToken token : tokens) {
				++tokenIndex;

				List<Feature> features = new ArrayList<Feature>();
				// features from token attributes
				for (FeatureExtractor1 extractor : this.tokenFeatureExtractors) {
					features.addAll(extractor.extract(jCas, token));
				}
				// features from surrounding tokens
				for (CleartkExtractor extractor : this.contextFeatureExtractors) {
					features.addAll(extractor.extractWithin(jCas, token, sentence));
				}
				// features from previous classifications
				int nPreviousClassifications = 2;
				for (int i = nPreviousClassifications; i > 0; --i) {
					int index = tokenIndex - i;
					String previousOutcome = index < 0 ? "O" : outcomes.get(index);
					features.add(new Feature("PreviousOutcome_" + i, previousOutcome));
				}
				//add segment ID as a features:
				features.add(new Feature("SegmentID", segment.getId()));

				// features from dominating parse tree
				//        for(FeatureExtractor1 extractor : this.parseFeatureExtractors){
				BaseToken startToken = token;
				for(int i = tokenIndex-1; i >= 0; --i){
					String outcome = outcomes.get(i);
					if(outcome.equals("O")){
						break;
					}
					startToken = tokens.get(i);
				}
				features.addAll(parseExtractor.extract(jCas, startToken.getBegin(), token.getEnd()));
				//        }
				
				// apply feature selection, if necessary
		        if (this.featureSelection != null) {
		          features = this.featureSelection.transform(features);
		        }
				
				// if training, write to data file
		        if (this.isTraining()) {
		        	String outcome = outcomes.get(tokenIndex);
		        	// if it is an "O" down-sample it
		        	if (outcome.equals("O")) {
		        		this.dataWriter.write(new Instance<String>(outcome, features));

		        	}else{//for minority instances:
		        		Instance<String> minorityInst = new Instance<String>(outcome, features);
		        		this.dataWriter.write(minorityInst);
		        		smote.addInstance(minorityInst);//add minority instances to SMOTE algorithm
		        	}
		        }else {// if predicting, add prediction to outcomes
		        	outcomes.add(this.classifier.classify(features));
		        }
			}

			// during prediction, convert chunk labels to times and add them to the CAS
			if (!this.isTraining()) {
				JCas timexCas;
				try {
				  timexCas = jCas.getView(timexView);
				} catch (CASException e) {
				  throw new AnalysisEngineProcessException(e);
				}
				this.timeChunking.createChunks(timexCas, tokens, outcomes);
			}
		}
		if(this.isTraining() && this.smoteNumOfNeighbors >= 1){ //add synthetic instances to datawriter, if smote is selected
	    	Iterable<Instance<String>> syntheticInsts = smote.populateMinorityClass();
	    	for( Instance<String> sytheticInst: syntheticInsts){
	    		this.dataWriter.write(sytheticInst);
	    	}
	    }
	}
}
