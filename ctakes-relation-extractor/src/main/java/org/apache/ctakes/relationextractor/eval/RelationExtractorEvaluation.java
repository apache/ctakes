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
package org.apache.ctakes.relationextractor.eval;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.*;
import org.apache.ctakes.typesystem.type.relation.*;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.util.ViewUriUtil;

import java.io.*;
import java.util.*;

public class RelationExtractorEvaluation extends RelationEvaluation_ImplBase {



	public static interface Options extends RelationEvaluation_ImplBase.EvaluationOptions {

		@Option(
				longName = "relations",
				description = "determines which relations to evaluate on (separately)",
				defaultValue = { "degree_of", "location_of" })
		public List<String> getRelations();

		@Option(
				longName = "test-on-ctakes",
				description = "evaluate test performance on ctakes entities, instead of gold standard "
						+ "entities")
		public boolean getTestOnCTakes();

		@Option(
				longName = "allow-smaller-system-arguments",
				description = "for evaluation, allow system relation arguments to match gold relation "
						+ "arguments that enclose them")
		public boolean getAllowSmallerSystemArguments();

		@Option(
				longName = "ignore-impossible-gold-relations",
				description = "for evaluation, ignore gold relations that would be impossible to find "
						+ "because there are no corresponding system mentions")
		public boolean getIgnoreImpossibleGoldRelations();

		@Option(
				longName = "print-errors",
				description = "print relations that were incorrectly predicted")
		public boolean getPrintErrors();

		@Option(
				longName = "class-weights",
				description = "automatically set class-wise weights for inbalanced training data")
		public boolean getClassWeights();
		
		@Option(
				longName = "expand-events",
				description = "expand events to their covering or covered events")
		public boolean getExpandEvents();

		@Option(
				longName = "train-corpus",
				description = "Corpora to use for training (space-separated if more than one)")
		public List<CorpusXMI.Corpus> getTrainCorpus();

		@Option(
				longName = "test-corpus",
				description = "Corpus to use for testing")
		public CorpusXMI.Corpus getTestCorpus();
	}

	public static final Map<String, Class<? extends BinaryTextRelation>> RELATION_CLASSES =
			Maps.newHashMap();
	public static final Map<Class<? extends BinaryTextRelation>, Class<? extends RelationExtractorAnnotator>> ANNOTATOR_CLASSES =
			Maps.newHashMap();
	public static final Map<Class<? extends BinaryTextRelation>, ParameterSettings> BEST_PARAMETERS =
			Maps.newHashMap();

	static {
		RELATION_CLASSES.put("degree_of", DegreeOfTextRelation.class);
		ANNOTATOR_CLASSES.put(DegreeOfTextRelation.class, DegreeOfRelationExtractorAnnotator.class);
		BEST_PARAMETERS.put(DegreeOfTextRelation.class, new ParameterSettings(
				LibLinearStringOutcomeDataWriter.class,
				new Object[] { RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
						1.0f },
				new String[] { "-s", "1", "-c", "0.1" }));

		RELATION_CLASSES.put("location_of", LocationOfTextRelation.class);
		ANNOTATOR_CLASSES.put(LocationOfTextRelation.class, LocationOfRelationExtractorAnnotator.class);
		BEST_PARAMETERS.put(LocationOfTextRelation.class, new ParameterSettings(
				LibLinearStringOutcomeDataWriter.class,
				new Object[] { RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
						1.0f },//0.5f },//
				new String[] { "-s", "0", "-c", "50.0" }));//

		RELATION_CLASSES.put("manages/treats", ManagesTreatsTextRelation.class);
		ANNOTATOR_CLASSES.put(ManagesTreatsTextRelation.class, ManagesTreatsRelationExtractorAnnotator.class);
		BEST_PARAMETERS.put(ManagesTreatsTextRelation.class, new ParameterSettings(
				LibLinearStringOutcomeDataWriter.class,
				new Object[] { RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
						0.5f },
				new String[] { "-s", "0", "-c", "5.0" }));

		RELATION_CLASSES.put("causes/brings_about", CausesBringsAboutTextRelation.class);
		ANNOTATOR_CLASSES.put(CausesBringsAboutTextRelation.class, CausesBringsAboutRelationExtractorAnnotator.class);
		BEST_PARAMETERS.put(CausesBringsAboutTextRelation.class, new ParameterSettings(
				LibLinearStringOutcomeDataWriter.class,
				new Object[] { RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
						0.5f },
				new String[] { "-s", "0", "-c", "1.0" }));

		RELATION_CLASSES.put("manifestation_of", ManifestationOfTextRelation.class);
		ANNOTATOR_CLASSES.put(ManifestationOfTextRelation.class, ManifestationOfRelationExtractorAnnotator.class);
		BEST_PARAMETERS.put(ManifestationOfTextRelation.class, new ParameterSettings(
				LibLinearStringOutcomeDataWriter.class,
				new Object[] { RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
						0.5f },
				new String[] { "-s", "0", "-c", "1.0" }));
	}

	public static void main(String[] args) throws Exception {
		// parse the options, validate them, and generate XMI if necessary
		final Options options = CliFactory.parseArguments(Options.class, args);
		CorpusXMI.validate(options);
		if(options.getGenerateXMI()) {
			boolean generateSharp = false, generateDeepPhe = false;
			if (options.getTestCorpus() == CorpusXMI.Corpus.SHARP || options.getTestCorpus() == CorpusXMI.Corpus.SHARP_RELEASE) {
				generateSharp = true;
			} else if (options.getTestCorpus() == CorpusXMI.Corpus.DeepPhe) {
				generateDeepPhe = true;
			}
			for(CorpusXMI.Corpus corpus : options.getTrainCorpus()){
				if(corpus == CorpusXMI.Corpus.SHARP_RELEASE || corpus == CorpusXMI.Corpus.SHARP){
					generateSharp = true;
				}else if(corpus == CorpusXMI.Corpus.DeepPhe){
					generateDeepPhe = true;
				}
			}

			if(generateSharp){
				SHARPXMI.generateXMI(options.getXMIDirectory(), options.getSharpCorpusDirectory(), options.getSharpBatchesDirectory());
			}
			if(generateDeepPhe){
				DeepPheXMI.generateXMI(options.getXMIDirectory(), options.getDeepPheAnaforaDirectory());
			}
		}


		// determine the grid of parameters to search through
		// for the full set of LibLinear parameters, see:
		// https://github.com/bwaldvogel/liblinear-java/blob/master/src/main/java/de/bwaldvogel/liblinear/Train.java
		List<ParameterSettings> gridOfSettings = null;
		if(options.getGridSearch()) {
			gridOfSettings = new ArrayList<>();
			for (float probabilityOfKeepingANegativeExample : new float[]{1.0f}) {//0.5f,
				for (int solver : new int[]{0 /* logistic regression */, 1 /* SVM */}) {
					for (double svmCost : new double[]{0.01, 0.05, 0.1, 0.5, 1, 5, 10, 50, 100}) {
						gridOfSettings.add(new ParameterSettings(
								LibLinearStringOutcomeDataWriter.class,
								new Object[]{
										RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
										probabilityOfKeepingANegativeExample},
								new String[]{"-s", String.valueOf(solver), "-c", String.valueOf(svmCost)}));
					}
				}
			}
		}

		// run an evaluation for each selected relation
		for (final String relationCategory : options.getRelations()) {
			// get the best parameters for the relation
			final Class<? extends BinaryTextRelation> relationClass =
					RELATION_CLASSES.get(relationCategory);

			List<File> trainFiles = new ArrayList<>();
			for(CorpusXMI.Corpus corpus : options.getTrainCorpus()){
				File trainCorpusDirectory;
				if(corpus == CorpusXMI.Corpus.SHARP) trainCorpusDirectory = options.getSharpBatchesDirectory();
				else if(corpus == CorpusXMI.Corpus.SHARP_RELEASE) trainCorpusDirectory = options.getSharpCorpusDirectory();
				else if(corpus == CorpusXMI.Corpus.DeepPhe) trainCorpusDirectory = options.getDeepPheAnaforaDirectory();
				else{
					throw new Exception("Train corpus not recognized: " + corpus);
				}
				trainFiles.addAll(CorpusXMI.toXMIFiles(options.getXMIDirectory(), CorpusXMI.getTrainTextFiles(corpus, options.getEvaluateOn(), trainCorpusDirectory)));
			}

			File testCorpusDirectory=null;

			if(options.getTestCorpus() == CorpusXMI.Corpus.SHARP) testCorpusDirectory = options.getSharpBatchesDirectory();
			else if(options.getTestCorpus() == CorpusXMI.Corpus.SHARP_RELEASE) testCorpusDirectory = options.getSharpCorpusDirectory();
			else if(options.getTestCorpus() == CorpusXMI.Corpus.DeepPhe) testCorpusDirectory = options.getDeepPheAnaforaDirectory();

			List<File> testFiles = CorpusXMI.toXMIFiles(options.getXMIDirectory(), CorpusXMI.getTestTextFiles(options.getTestCorpus(), options.getEvaluateOn(), testCorpusDirectory));

			if(gridOfSettings != null){
				// grid search:
				Map<ParameterSettings, Double> scoredParams = new HashMap<>();
				for(ParameterSettings params : gridOfSettings) {
					RelationExtractorEvaluation eval = new RelationExtractorEvaluation(
							new File("target/models/" + relationCategory),
							relationClass,
							ANNOTATOR_CLASSES.get(relationClass),
							params,
							options.getTestOnCTakes(),
							options.getAllowSmallerSystemArguments(),
							options.getIgnoreImpossibleGoldRelations(),
							options.getPrintErrors(),
							options.getClassWeights(),
							options.getExpandEvents());
					params.stats = eval.trainAndTest(trainFiles, testFiles);
					scoredParams.put(params, params.stats.f1());
				}
				// print parameters sorted by F1
				List<ParameterSettings> list = new ArrayList<>( scoredParams.keySet() );
				Function<ParameterSettings, Double> getCount = Functions.forMap( scoredParams );
				Collections.sort( list, Ordering.natural().onResultOf( getCount ) );

				// print performance of each set of parameters
				if ( list.size() > 1 ) {
					System.err.println( "Summary" );
					for ( ParameterSettings params : list ) {
						System.err.printf(
								"F1=%.3f P=%.3f R=%.3f %s\n",
								params.stats.f1(),
								params.stats.precision(),
								params.stats.recall(),
								params );
					}
					System.err.println();
				}
				// print best settings:
				if ( !list.isEmpty() ) {
					ParameterSettings lastParams = list.get( list.size() - 1 );
					System.err.println( "Best model:" );
					System.err.print( lastParams.stats );
					System.err.println( lastParams );
					System.err.println( lastParams.stats.confusions() );
					System.err.println();
				}
			}else {
				ParameterSettings bestSettings = BEST_PARAMETERS.get(relationClass);
				RelationExtractorEvaluation eval = new RelationExtractorEvaluation(new File("target/models/" + relationCategory),
						relationClass,
						ANNOTATOR_CLASSES.get(relationClass),
						bestSettings,
						options.getTestOnCTakes(),
						options.getAllowSmallerSystemArguments(),
						options.getIgnoreImpossibleGoldRelations(),
						options.getPrintErrors(),
						options.getClassWeights(),
						options.getExpandEvents());
				bestSettings.stats = eval.trainAndTest(trainFiles, testFiles);
				System.err.println( bestSettings.stats);
				System.err.println(bestSettings);
				System.err.println(bestSettings.stats.confusions());
				System.err.println();
			}
		}
	}

	private Class<? extends BinaryTextRelation> relationClass;

	private Class<? extends RelationExtractorAnnotator> classifierAnnotatorClass;

	private ParameterSettings parameterSettings;

	private boolean testOnCTakes;

	private boolean allowSmallerSystemArguments;

	private boolean ignoreImpossibleGoldRelations;

	private boolean printErrors;

	private boolean setClassWeights;

	private static PrintWriter outPrint;
	
	public static boolean expandEvent = false;

	/**
	 * An evaluation of a relation extractor.
	 * 
	 * @param baseDirectory
	 *          The directory where models, etc. should be written
	 * @param relationClass
	 *          The class of the relation to be predicted
	 * @param classifierAnnotatorClass
	 *          The CleartkAnnotator class that learns a relation extractor model
	 * @param parameterSettings
	 *          The parameters defining how to train a classifier
	 * @param testOnCTakes
	 *          During testing, use annotations from cTAKES, not from the gold
	 *          standard
	 * @param allowSmallerSystemArguments
	 *          During testing, allow system annotations to match gold annotations
	 *          that enclose them
	 * @param ignoreImpossibleGoldRelations
	 *          During testing, ignore gold relations that would be impossible to
	 *          find because there are no corresponding system mentions
	 * @param expandEventParameter
	 */
	public RelationExtractorEvaluation(
			File baseDirectory,
			Class<? extends BinaryTextRelation> relationClass,
			Class<? extends RelationExtractorAnnotator> classifierAnnotatorClass,
			ParameterSettings parameterSettings,
			boolean testOnCTakes,
			boolean allowSmallerSystemArguments,
			boolean ignoreImpossibleGoldRelations,
			boolean printErrors,
			boolean setClassWeights,
			boolean expandEventParameter) {
		super(baseDirectory);
		this.relationClass = relationClass;
		this.classifierAnnotatorClass = classifierAnnotatorClass;
		this.parameterSettings = parameterSettings;
		this.testOnCTakes = testOnCTakes;
		this.allowSmallerSystemArguments = allowSmallerSystemArguments;
		this.ignoreImpossibleGoldRelations = ignoreImpossibleGoldRelations;
		this.printErrors = printErrors;
		this.setClassWeights = setClassWeights;
		expandEvent = expandEventParameter;
	}

	public RelationExtractorEvaluation(
			File baseDirectory,
			Class<? extends BinaryTextRelation> relationClass,
			Class<? extends RelationExtractorAnnotator> classifierAnnotatorClass,
			ParameterSettings parameterSettings) {
		this(
				baseDirectory,
				relationClass,
				classifierAnnotatorClass,
				parameterSettings,
				false,
				false,
				false,
				false,
				false,
				false);
	}

	@Override
	public void train(CollectionReader collectionReader, File directory) throws Exception {
		System.err.printf(
				"%s: %s: %s:\n",
				this.getClass().getSimpleName(),
				this.relationClass.getSimpleName(),
				directory.getName());
		System.err.println(this.parameterSettings);

		AggregateBuilder builder = new AggregateBuilder();
		// remove cTAKES entity mentions and modifiers in the system view and copy
		// in the gold relations
		builder.add(AnalysisEngineFactory.createEngineDescription(RemoveCTakesMentionsAndCopyGoldRelations.class));

		//add potential events for training:
		if (expandEvent && this.relationClass.getSimpleName().equals("LocationOfTextRelation") )
			builder.add(AnalysisEngineFactory.createEngineDescription(AddPotentialRelations.class));

		// add the relation extractor, configured for training mode
		AnalysisEngineDescription classifierAnnotator =
				AnalysisEngineFactory.createEngineDescription(
						this.classifierAnnotatorClass,
						this.parameterSettings.configurationParameters);
		ConfigurationParameterFactory.addConfigurationParameters(
				classifierAnnotator,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				this.parameterSettings.dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				directory.getPath());
		builder.add(classifierAnnotator);

		// run the data-writing pipeline
		SimplePipeline.runPipeline(collectionReader, builder.createAggregateDescription());

		//calculate class-wise weights:
		if(this.setClassWeights){
			//calculate class-wise weights:
			String[] weightArray=new String[RelationExtractorAnnotator.category_frequency.size()*2];
			int weight_idx = 0;
			float baseFreq = RelationExtractorAnnotator.category_frequency.get(RelationExtractorAnnotator.NO_RELATION_CATEGORY);
			for( Map.Entry<String, Integer> entry: RelationExtractorAnnotator.category_frequency.entrySet()){
				weightArray[weight_idx*2] = "-w"+Integer.toString(weight_idx + 1);
				float weight = baseFreq/entry.getValue();
				weightArray[weight_idx*2+1] = Float.toString(weight);
				weight_idx ++;
				System.err.println("Category:"+entry.getKey()+"  freq:"+entry.getValue() + "   weight:"+weight);
			}

			List<String> parameters = new LinkedList<>(Arrays.asList(this.parameterSettings.trainingArguments));
			List<String> additional = Arrays.asList(weightArray);
			parameters.addAll(additional);
			
			RelationExtractorAnnotator.clearCategoryFrequency();

			// train the classifier and package it into a .jar file
			JarClassifierBuilder.trainAndPackage(directory, parameters.toArray(new String[parameters.size()]));
		}else{
			JarClassifierBuilder.trainAndPackage(directory, this.parameterSettings.trainingArguments);
		}
	}

	@PipeBitInfo(
			name = "Location Relation Overlapper",
			description = "Adds Location-Of relations for annotations overlapping those already having relations.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.LOCATION_RELATION }
	)
	public static class AddPotentialRelations extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas relationView = jCas;

			Map<EventMention, List<EventMention>> coveredMap =
					JCasUtil.indexCovered(relationView, EventMention.class, EventMention.class);
//			Map<EventMention, Collection<EventMention>> coveringMap =
//					JCasUtil.indexCovering(relationView, EventMention.class, EventMention.class);
//			Map<AnatomicalSiteMention, Collection<EventMention>> siteEventMap =
//					JCasUtil.indexCovered(relationView, AnatomicalSiteMention.class, EventMention.class);
//			Map<AnatomicalSiteMention, Collection<EntityMention>> siteEntityMap =
//					JCasUtil.indexCovering(relationView, AnatomicalSiteMention.class, EntityMention.class);
			final List<IdentifiedAnnotation> eventList = new ArrayList<>();
			for(LocationOfTextRelation relation : Lists.newArrayList(JCasUtil.select(relationView, LocationOfTextRelation.class))){
				Annotation arg1 = relation.getArg1().getArgument();
				Annotation arg2 = relation.getArg2().getArgument();
				EventMention event = null;
				if(arg1 instanceof EventMention && arg2 instanceof AnatomicalSiteMention){
					event = (EventMention) arg1;

//					eventList.addAll(coveringMap.get(event));
					eventList.addAll(coveredMap.get(event));
					for(IdentifiedAnnotation covEvent : eventList){
						if(!covEvent.getClass().equals(EventMention.class) && !hasOverlap(covEvent, arg2)){
							createRelation(relationView, covEvent, arg2, relation.getCategory());
						}
					}
					eventList.clear();
//					eventList.addAll(siteEventMap.get(arg2));
//					eventList.addAll(siteEntityMap.get(arg2));
//					for(IdentifiedAnnotation covSite : eventList){
//						if(!covSite.getClass().equals(EventMention.class) && !hasOverlap(arg1, covSite)){
//							createRelation(relationView, event, covSite, relation.getCategory());
//						}
//					}
//					eventList.clear();
				}else if(arg2 instanceof EventMention && arg1 instanceof AnatomicalSiteMention){
					event = (EventMention) arg2;
//					eventList.addAll(coveringMap.get(event));
					eventList.addAll(coveredMap.get(event));
					for(IdentifiedAnnotation covEvent : eventList){
						if(!covEvent.getClass().equals(EventMention.class)&& !hasOverlap(arg1, covEvent)){
							createRelation(relationView, arg1, covEvent, relation.getCategory());
						}
					}
					eventList.clear();
//					eventList.addAll(siteEventMap.get(arg1));
//					eventList.addAll(siteEntityMap.get(arg1));
//					for(IdentifiedAnnotation covSite : eventList){
//						if(!covSite.getClass().equals(EventMention.class) && !hasOverlap(covSite, arg2)){
//							createRelation(relationView, covSite, event, relation.getCategory());
//						}
//					}
//					eventList.clear();
				}
			}

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

		private static void createRelation(JCas jCas, Annotation arg1,
				Annotation arg2, String category) {
			RelationArgument relArg1 = new RelationArgument(jCas);
			relArg1.setArgument(arg1);
			relArg1.setRole("Arg1");
			relArg1.addToIndexes();
			RelationArgument relArg2 = new RelationArgument(jCas);
			relArg2.setArgument(arg2);
			relArg2.setRole("Arg2");
			relArg2.addToIndexes();
			BinaryTextRelation relation = new BinaryTextRelation(jCas);
			relation.setArg1(relArg1);
			relation.setArg2(relArg2);
			relation.setCategory(category);
			relation.addToIndexes();

		}
	}

	@Override
	protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
			throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		if (this.testOnCTakes) {
			// add the modifier extractor
			File file = new File("desc/analysis_engine/ModifierExtractorAnnotator.xml");
			XMLInputSource source = new XMLInputSource(file);
			builder.add(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(source));
			// remove extraneous entity mentions
			builder.add(AnalysisEngineFactory.createEngineDescription(RemoveSmallerEventMentions.class));
		} else {
			// replace cTAKES entity mentions and modifiers in the system view with
			// the gold annotations
			builder.add(AnalysisEngineFactory.createEngineDescription(ReplaceCTakesMentionsWithGoldMentions.class));
		}
		// add the relation extractor, configured for classification mode
		AnalysisEngineDescription classifierAnnotator =
				AnalysisEngineFactory.createEngineDescription(
						this.classifierAnnotatorClass,
						this.parameterSettings.configurationParameters);
		ConfigurationParameterFactory.addConfigurationParameters(
				classifierAnnotator,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				JarClassifierBuilder.getModelJarFile(directory));
		builder.add(classifierAnnotator);

		// statistics will be based on the "category" feature of the
		// BinaryTextRelations
		AnnotationStatistics<String> stats = new AnnotationStatistics<>();
		Function<BinaryTextRelation, HashableArguments> getSpan =
				new Function<BinaryTextRelation, HashableArguments>() {
			@Override
			public HashableArguments apply(BinaryTextRelation relation) {
				return new HashableArguments(relation);
			}
		};
		Function<BinaryTextRelation, String> getOutcome =
				AnnotationStatistics.annotationToFeatureValue("category");

		// calculate statistics, iterating over the results of the classifier
		AnalysisEngine engine = builder.createAggregate();
		for (Iterator<JCas> casIter = new JCasIterator(collectionReader, engine); casIter.hasNext();) {
			JCas jCas = casIter.next();
			// get the gold view
			JCas goldView;
			try {
				goldView = jCas.getView(SHARPXMI.GOLD_VIEW_NAME);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			// get the gold and system annotations
			Collection<? extends BinaryTextRelation> goldBinaryTextRelations =
					JCasUtil.select(goldView, this.relationClass);
			Collection<? extends BinaryTextRelation> systemBinaryTextRelations =
					JCasUtil.select(jCas, this.relationClass);

			if (this.ignoreImpossibleGoldRelations) {
				// collect only relations where both arguments have some possible system
				// arguments
				List<BinaryTextRelation> relations = Lists.newArrayList();
				for (BinaryTextRelation relation : goldBinaryTextRelations) {
					boolean hasSystemArgs = true;
					for (RelationArgument relArg : Lists.newArrayList(relation.getArg1(), relation.getArg2())) {
						IdentifiedAnnotation goldArg = (IdentifiedAnnotation) relArg.getArgument();
						Class<? extends IdentifiedAnnotation> goldClass = goldArg.getClass();
						boolean noSystemArg = JCasUtil.selectCovered(jCas, goldClass, goldArg).isEmpty();
						hasSystemArgs = hasSystemArgs && !noSystemArg;
					}
					if (hasSystemArgs) {
						relations.add(relation);
					} else {
						IdentifiedAnnotation arg1 = (IdentifiedAnnotation) relation.getArg1().getArgument();
						IdentifiedAnnotation arg2 = (IdentifiedAnnotation) relation.getArg2().getArgument();
						String messageFormat =
								"removing relation between %s and %s which is impossible to "
										+ "find with system mentions";
						String message = String.format(messageFormat, format(arg1), format(arg2));
						UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, message);
					}
				}
				goldBinaryTextRelations = relations;
			}

			if (this.allowSmallerSystemArguments) {

				// collect all the arguments of the manually annotated relations
				Set<IdentifiedAnnotation> goldArgs = Sets.newHashSet();
				for (BinaryTextRelation relation : goldBinaryTextRelations) {
					for (RelationArgument relArg : Lists.newArrayList(relation.getArg1(), relation.getArg2())) {
						goldArgs.add((IdentifiedAnnotation) relArg.getArgument());
					}
				}

				// collect all the arguments of system-predicted relations that don't
				// match some gold argument
				Set<IdentifiedAnnotation> unmatchedSystemArgs = Sets.newHashSet();
				for (BinaryTextRelation relation : systemBinaryTextRelations) {
					for (RelationArgument relArg : Lists.newArrayList(relation.getArg1(), relation.getArg2())) {
						IdentifiedAnnotation systemArg = (IdentifiedAnnotation) relArg.getArgument();
						Class<? extends IdentifiedAnnotation> systemClass = systemArg.getClass();
						boolean matchesSomeGold = false;
						for (IdentifiedAnnotation goldArg : JCasUtil.selectCovered(
								goldView,
								systemClass,
								systemArg)) {
							if (goldArg.getBegin() == systemArg.getBegin()
									&& goldArg.getEnd() == systemArg.getEnd()) {
								matchesSomeGold = true;
								break;
							}
						}
						if (!matchesSomeGold) {
							unmatchedSystemArgs.add(systemArg);
						}
					}
				}

				// map each unmatched system argument to the gold argument that encloses
				// it
				Map<IdentifiedAnnotation, IdentifiedAnnotation> systemToGold = Maps.newHashMap();
				for (IdentifiedAnnotation goldArg : goldArgs) {
					Class<? extends IdentifiedAnnotation> goldClass = goldArg.getClass();
					for (IdentifiedAnnotation systemArg : JCasUtil.selectCovered(jCas, goldClass, goldArg)) {
						if (unmatchedSystemArgs.contains(systemArg)) {

							// if there's no mapping yet for this system arg, map it to the
							// enclosing gold arg
							IdentifiedAnnotation oldGoldArg = systemToGold.get(systemArg);
							if (oldGoldArg == null) {
								systemToGold.put(systemArg, goldArg);
							}

							// if there's already a mapping for this system arg, only re-map
							// it to match the type
							else {
								IdentifiedAnnotation current, other;
								if (systemArg.getTypeID() == goldArg.getTypeID()) {
									systemToGold.put(systemArg, goldArg);
									current = goldArg;
									other = oldGoldArg;
								} else {
									current = oldGoldArg;
									other = goldArg;
								}

								// issue a warning since this re-mapping procedure is imperfect
								String message =
										"system argument %s mapped to gold argument %s, but could also be mapped to %s";
								message = String.format(message, format(systemArg), format(current), format(other));
								UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, message);
							}
						}
					}
				}

				// replace system arguments with gold arguments where necessary/possible
				for (BinaryTextRelation relation : systemBinaryTextRelations) {
					for (RelationArgument relArg : Lists.newArrayList(relation.getArg1(), relation.getArg2())) {
						IdentifiedAnnotation systemArg = (IdentifiedAnnotation) relArg.getArgument();
						IdentifiedAnnotation matchingGoldArg = systemToGold.get(systemArg);
						if (matchingGoldArg != null) {
							String messageFormat = "replacing system argument %s with gold argument %s";
							String message =
									String.format(messageFormat, format(systemArg), format(matchingGoldArg));
							UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, message);
							relArg.setArgument(matchingGoldArg);
						}
					}
				}
			}

			// update the statistics based on the argument spans of the relation
			stats.add(goldBinaryTextRelations, systemBinaryTextRelations, getSpan, getOutcome);

			// print errors if requested
			if (this.printErrors) {
				printInstanceOutput(goldBinaryTextRelations, systemBinaryTextRelations, getSpan, getOutcome);

				Map<HashableArguments, BinaryTextRelation> goldMap = Maps.newHashMap();
				for (BinaryTextRelation relation : goldBinaryTextRelations) {
					goldMap.put(new HashableArguments(relation), relation);
				}
				Map<HashableArguments, BinaryTextRelation> systemMap = Maps.newHashMap();
				for (BinaryTextRelation relation : systemBinaryTextRelations) {
					systemMap.put(new HashableArguments(relation), relation);
				}
				Set<HashableArguments> all = Sets.union(goldMap.keySet(), systemMap.keySet());
				List<HashableArguments> sorted = Lists.newArrayList(all);
				Collections.sort(sorted);

				File noteFile = new File(ViewUriUtil.getURI(jCas).toString());
				String fileName = noteFile.getName();

				for (HashableArguments key : sorted) {
					BinaryTextRelation goldRelation = goldMap.get(key);
					BinaryTextRelation systemRelation = systemMap.get(key);
					if (goldRelation == null) {
						System.out.printf("[%s] System added: %s\n", fileName, formatRelation(systemRelation));
					} else if (systemRelation == null) {
						System.out.printf("[%s] System dropped: %s\n", fileName, formatRelation(goldRelation));
					} else if (!systemRelation.getCategory().equals(goldRelation.getCategory())) {
						String label = systemRelation.getCategory();
						System.out.printf("[%s] System labeled %s for %s\n", fileName, label, formatRelation(systemRelation));
					} else if (systemRelation.getCategory().equals(goldRelation.getCategory())) {
						System.out.printf("[%s] System nailed it: %s\n", fileName, formatRelation(systemRelation));
					} 
				}
			}
		}

		System.err.print(stats);
		System.err.println();
		return stats;
	}

	private static void printInstanceOutput(
			Collection<? extends BinaryTextRelation> referenceAnnotations,
			Collection<? extends BinaryTextRelation> predictedAnnotations,
			Function<BinaryTextRelation, HashableArguments> annotationToSpan,
			Function<BinaryTextRelation, String> annotationToOutcome) {
		// map gold spans to their outcomes
		Map<HashableArguments, String> referenceSpanOutcomes = new HashMap<>();
		for (BinaryTextRelation ann : referenceAnnotations) {
			referenceSpanOutcomes.put(annotationToSpan.apply(ann), annotationToOutcome.apply(ann));
		}

		// map system spans to their outcomes
		Map<HashableArguments, String> predictedSpanOutcomes = new HashMap<>();
		for (BinaryTextRelation ann : predictedAnnotations) {
			predictedSpanOutcomes.put(annotationToSpan.apply(ann), annotationToOutcome.apply(ann));
		}

		// print instance level classification types:
		Set<HashableArguments> union = new HashSet<>();
		union.addAll(referenceSpanOutcomes.keySet());
		union.addAll(predictedSpanOutcomes.keySet());

		File outf =  new File("target/locationOf_Instance_output_test.txt");
		try {
			outPrint = new PrintWriter(new BufferedWriter(new FileWriter(outf, true)));
			for (HashableArguments span : union) {
				String goldCategory = referenceSpanOutcomes.get(span);
				String predictedCategory = predictedSpanOutcomes.get(span);

				if(goldCategory==null){
					//					System.out.println("false positive: "+ predictedCategory);
					outPrint.println("fp");
				}else{
					if(predictedCategory==null){
						outPrint.println("fn");
					}else{
						outPrint.println("tp");
					}
				}

			}
			outPrint.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String formatRelation(BinaryTextRelation relation) {
		IdentifiedAnnotation arg1 = (IdentifiedAnnotation) relation.getArg1().getArgument();
		IdentifiedAnnotation arg2 = (IdentifiedAnnotation) relation.getArg2().getArgument();
		String text = arg1.getCAS().getDocumentText();
		int begin = Math.min(arg1.getBegin(), arg2.getBegin());
		int end = Math.max(arg1.getBegin(), arg2.getBegin());
		begin = Math.max(0, begin - 50);
		end = Math.min(text.length(), end + 50);
		return String.format(
				"%s(%s(type=%d), %s(type=%d)) in ...%s...",
				relation.getCategory(),
				arg1.getCoveredText(),
				arg1.getTypeID(),
				arg2.getCoveredText(),
				arg2.getTypeID(),
				text.substring(begin, end).replaceAll("[\r\n]", " "));
	}

	/**
	 * Annotator that removes cTAKES mentions in the system view and copies
	 * relations from the gold view to the system view
	 */
	public static class RemoveCTakesMentionsAndCopyGoldRelations extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView(SHARPXMI.GOLD_VIEW_NAME);
				systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			// remove cTAKES Mentions and Modifiers from system view
			List<IdentifiedAnnotation> cTakesMentions = new ArrayList<>();
			cTakesMentions.addAll(JCasUtil.select(systemView, EventMention.class));
			cTakesMentions.addAll(JCasUtil.select(systemView, EntityMention.class));
			cTakesMentions.addAll(JCasUtil.select(systemView, Modifier.class));
			for (IdentifiedAnnotation cTakesMention : cTakesMentions) {
				cTakesMention.removeFromIndexes();
			}

			// copy gold Mentions and Modifiers to the system view
			List<IdentifiedAnnotation> goldMentions = new ArrayList<>();
			goldMentions.addAll(JCasUtil.select(goldView, EventMention.class));
			goldMentions.addAll(JCasUtil.select(goldView, EntityMention.class));
			goldMentions.addAll(JCasUtil.select(goldView, Modifier.class));
			CasCopier copier = new CasCopier(goldView.getCas(), systemView.getCas());
			Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFA);
			for (IdentifiedAnnotation goldMention : goldMentions) {
				Annotation copy = (Annotation) copier.copyFs(goldMention);
				copy.setFeatureValue(sofaFeature, systemView.getSofa());
				copy.addToIndexes();
			}

			// copy gold relations to the system view
			for (BinaryTextRelation goldRelation : JCasUtil.select(goldView, BinaryTextRelation.class)) {
				BinaryTextRelation relation = (BinaryTextRelation) copier.copyFs(goldRelation);
				relation.addToIndexes(systemView);
				for (RelationArgument relArg : Lists.newArrayList(relation.getArg1(), relation.getArg2())) {
					relArg.addToIndexes(systemView);
					// relArg.getArgument() should have been added to indexes with
					// mentions above
				}
			}
		}
	}

	/**
	 * Annotator that removes cTAKES Mentions and Modifiers from the system view,
	 * and copies over the manually annotated Mentions and Modifiers from the gold
	 * view.
	 */
	public static class ReplaceCTakesMentionsWithGoldMentions extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView(SHARPXMI.GOLD_VIEW_NAME);
				systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			// remove cTAKES Mentions and Modifiers from system view
			List<IdentifiedAnnotation> cTakesMentions = new ArrayList<>();
			cTakesMentions.addAll(JCasUtil.select(systemView, EventMention.class));
			cTakesMentions.addAll(JCasUtil.select(systemView, EntityMention.class));
			cTakesMentions.addAll(JCasUtil.select(systemView, Modifier.class));
			for (IdentifiedAnnotation cTakesMention : cTakesMentions) {
				cTakesMention.removeFromIndexes();
			}

			// copy gold Mentions and Modifiers to the system view
			List<IdentifiedAnnotation> goldMentions = new ArrayList<>();
			goldMentions.addAll(JCasUtil.select(goldView, EventMention.class));
			goldMentions.addAll(JCasUtil.select(goldView, EntityMention.class));
			goldMentions.addAll(JCasUtil.select(goldView, Modifier.class));
			CasCopier copier = new CasCopier(goldView.getCas(), systemView.getCas());
			for (IdentifiedAnnotation goldMention : goldMentions) {
				Annotation copy = (Annotation) copier.copyFs(goldMention);
				Feature sofaFeature = copy.getType().getFeatureByBaseName("sofa");
				copy.setFeatureValue(sofaFeature, systemView.getSofa());
				copy.addToIndexes();
			}
		}
	}

	static String format(IdentifiedAnnotation a) {
		return a == null ? null : String.format("\"%s\"(type=%d)", a.getCoveredText(), a.getTypeID());
	}

	public static class RemoveSmallerEventMentions extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			Collection<EventMention> mentions = JCasUtil.select(jCas, EventMention.class);
			for (EventMention mention : Lists.newArrayList(mentions)) {
				int begin = mention.getBegin();
				int end = mention.getEnd();
				int typeID = mention.getTypeID();
				List<EventMention> subMentions = JCasUtil.selectCovered(jCas, EventMention.class, mention);
				for (EventMention subMention : subMentions) {
					if (subMention.getBegin() > begin || subMention.getEnd() < end) {
						if (subMention.getTypeID() == typeID) {
							String message =
									String.format("removed %s inside %s", format(subMention), format(mention));
							this.getContext().getLogger().log(Level.WARNING, message);
							subMention.removeFromIndexes();
						}
					}
				}
			}
		}
	}

	/**
	 * This class is useful for mapping the spans of relation arguments to the
	 * relation's category.
	 */
	public static class HashableArguments implements Comparable<HashableArguments> {

		protected int arg1begin;

		protected int arg1end;

		protected int arg2begin;

		protected int arg2end;

		public HashableArguments(int arg1begin, int arg1end, int arg2begin, int arg2end) {
			this.arg1begin = arg1begin;
			this.arg1end = arg1end;
			this.arg2begin = arg2begin;
			this.arg2end = arg2end;
		}

		public HashableArguments(Annotation arg1, Annotation arg2) {
			this(arg1.getBegin(), arg1.getEnd(), arg2.getBegin(), arg2.getEnd());
		}

		public HashableArguments(BinaryTextRelation relation) {
			this(relation.getArg1().getArgument(), relation.getArg2().getArgument());
		}

		@Override
		public boolean equals(Object otherObject) {
			int preArgBegin;
			int preArgEnd;
			int ltrArgBegin;
			int ltrArgEnd;
			if(this.arg1begin< this.arg2begin){
				preArgBegin = this.arg1begin;
				preArgEnd   = this.arg1end;
				ltrArgBegin = this.arg2begin;
				ltrArgEnd   = this.arg2end;
			}else{
				preArgBegin = this.arg2begin;
				preArgEnd 	= this.arg2end;
				ltrArgBegin = this.arg1begin;
				ltrArgEnd 	= this.arg1end;
			}
			boolean result = false;
			if (otherObject instanceof HashableArguments) {
				HashableArguments other = (HashableArguments) otherObject;
				int otherPreArgBegin;
				int otherPreArgEnd;
				int otherLtrArgBegin;
				int otherLtrArgEnd;
				if(other.arg1begin< other.arg2begin){
					otherPreArgBegin = other.arg1begin;
					otherPreArgEnd   = other.arg1end;
					otherLtrArgBegin = other.arg2begin;
					otherLtrArgEnd   = other.arg2end;
				}else{
					otherPreArgBegin = other.arg2begin;
					otherPreArgEnd   = other.arg2end;
					otherLtrArgBegin = other.arg1begin;
					otherLtrArgEnd   = other.arg1end;
				}
				result =
						(this.getClass() == other.getClass()
						&& preArgBegin == otherPreArgBegin
						&& preArgEnd == otherPreArgEnd
						&& ltrArgBegin == otherLtrArgBegin 
						&& ltrArgEnd == otherLtrArgEnd);
			}
			return result;
		}

		@Override
		public int hashCode() {
			int preArgBegin;
			int preArgEnd;
			int ltrArgBegin;
			int ltrArgEnd;
			if(this.arg1begin< this.arg2begin){
				preArgBegin = this.arg1begin;
				preArgEnd   = this.arg1end;
				ltrArgBegin = this.arg2begin;
				ltrArgEnd   = this.arg2end;
			}else{
				preArgBegin = this.arg2begin;
				preArgEnd 	= this.arg2end;
				ltrArgBegin = this.arg1begin;
				ltrArgEnd 	= this.arg1end;
			}
			return Objects.hashCode(preArgBegin, preArgEnd, ltrArgBegin, ltrArgEnd);
		}

		@Override
		public String toString() {
			return String.format(
					"%s(%s,%s,%s,%s)",
					this.getClass().getSimpleName(),
					this.arg1begin,
					this.arg1end,
					this.arg2begin,
					this.arg2end);
		}

		@Override
		public int compareTo(HashableArguments that) {
			int thisBegin = Math.min(this.arg1begin, this.arg2begin);
			int thatBegin = Math.min(that.arg1begin, that.arg2begin);
			int thisEnd = Math.max(this.arg1end,  this.arg2end);
			int thatEnd = Math.max(that.arg1end, that.arg2end);

			if (thisBegin < thatBegin) {
				return -1;
			} else if (thisBegin > thatBegin) {
				return +1;
			} else if (this.equals(that)) {
				return 0;
			} else if (thisEnd < thatEnd) {
				return -1;
			} else{
				return 1;
			}
		}

	}
}
