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
package org.apache.ctakes.temporal.nn.eval;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.eval.EvaluationOfTemporalRelations_ImplBase;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.temporal.eval.I2B2Data;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.temporal.keras.KerasStringOutcomeDataWriter;
import org.apache.ctakes.temporal.keras.ScriptStringFeatureDataWriter;
import org.apache.ctakes.temporal.nn.ae.EventEventTokenBasedAnnotator;
import org.apache.ctakes.temporal.nn.ae.EventTimeTokenBasedAnnotator;
import org.apache.ctakes.temporal.utils.AnnotationIdCollection;
import org.apache.ctakes.temporal.utils.TLinkTypeArray2;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.util.ViewUriUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;

//import org.apache.ctakes.temporal.ae.EventTimeSyntacticAnnotator;
//import org.apache.ctakes.temporal.ae.EventTimeRelationAnnotator;
//import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
//import org.apache.ctakes.temporal.nn.ae.EventTimeTokenAndPathBasedAnnotator;
//import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.WriteI2B2XML;
//import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat;
//import org.cleartk.ml.libsvm.tk.TkLibSvmStringOutcomeDataWriter;
//import org.cleartk.ml.libsvm.LIBSVMStringOutcomeDataWriter;
//import org.cleartk.ml.tksvmlight.TKSVMlightStringOutcomeDataWriter;

public class EvaluationOfNeuralEEAndETRelations extends
EvaluationOfTemporalRelations_ImplBase{
	static interface TempRelOptions extends Evaluation_ImplBase.Options{
		@Option
		public boolean getPrintFormattedRelations();

		@Option
		public boolean getBaseline();

		@Option
		public boolean getClosure();

		@Option
		public boolean getUseTmp();

		@Option
		public boolean getUseGoldAttributes();

		@Option
		public boolean getSkipTrain();

		@Option
		public boolean getWriteProbabilities();

		@Option
		public boolean getTestOnTrain();

		@Option
		public boolean getSkipWrite();
	}

	//  protected static boolean DEFAULT_BOTH_DIRECTIONS = false;
	//  protected static float DEFAULT_DOWNSAMPLE = 1.0f;
	//  private static double DEFAULT_SVM_C = 1.0;
	//  private static double DEFAULT_SVM_G = 1.0;
	//  private static double DEFAULT_TK = 0.5;
	//  private static double DEFAULT_LAMBDA = 0.5;

	//  defaultParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk",
	//  		  DEFAULT_SVM_C, DEFAULT_SVM_G, "polynomial", ComboOperator.SUM, DEFAULT_TK, DEFAULT_LAMBDA);
	protected static ParameterSettings flatParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "linear",
			10.0, 1.0, "linear", ComboOperator.VECTOR_ONLY, DEFAULT_TK, DEFAULT_LAMBDA);
	protected static ParameterSettings allBagsParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk", 
			100.0, 0.1, "radial basis function", ComboOperator.SUM, 0.5, 0.5);
	protected static ParameterSettings allParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk",
			10.0, 1.0, "polynomial", ComboOperator.SUM, 0.1, 0.5);  // (0.3, 0.4 for tklibsvm)
	protected static ParameterSettings ftParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk", 
			1.0, 0.1, "radial basis function", ComboOperator.SUM, 0.5, 0.5);
	private static Boolean recallModeEvaluation = true;

	// usage: train EE and ET CNN model separately. then call this evaluator with "--skipWrite" parameter
	public static void main(String[] args) throws Exception {
		TempRelOptions options = CliFactory.parseArguments(TempRelOptions.class, args);
		List<Integer> trainItems = null;
		List<Integer> devItems = null;
		List<Integer> testItems = null;

		List<Integer> patientSets = options.getPatients().getList();
		if(options.getXMLFormat() == XMLFormat.I2B2){
			trainItems = I2B2Data.getTrainPatientSets(options.getXMLDirectory());
			devItems = I2B2Data.getDevPatientSets(options.getXMLDirectory());
			testItems = I2B2Data.getTestPatientSets(options.getXMLDirectory());
		}else{
			trainItems = THYMEData.getPatientSets(patientSets, options.getTrainRemainders().getList());
			devItems = THYMEData.getPatientSets(patientSets, options.getDevRemainders().getList());
			testItems = THYMEData.getPatientSets(patientSets, options.getTestRemainders().getList());
		}
		ParameterSettings params = allParams;

		//    possibleParams.add(defaultParams);

		//    for(ParameterSettings params : possibleParams){
		try{
			File workingDir = new File("target/eval/thyme/");
			if(!workingDir.exists()) workingDir.mkdirs();
			if(options.getUseTmp()){
				File tempModelDir = File.createTempFile("temporal", null, workingDir);
				tempModelDir.delete();
				tempModelDir.mkdir();
				workingDir = tempModelDir;
			}
			EvaluationOfNeuralEEAndETRelations evaluation = new EvaluationOfNeuralEEAndETRelations(
					workingDir,
					options.getRawTextDirectory(),
					options.getXMLDirectory(),
					options.getXMLFormat(),
					options.getSubcorpus(),
					options.getXMIDirectory(),
					options.getTreebankDirectory(),
					options.getClosure(),
					options.getPrintErrors(),
					options.getPrintFormattedRelations(),
					options.getBaseline(),
					options.getUseGoldAttributes(),
					options.getKernelParams(),
					params);
			//			evaluation.prepareXMIsFor(patientSets);
			if(options.getI2B2Output()!=null) evaluation.setI2B2Output(options.getI2B2Output() + "/temporal-relations/both");
			if(options.getAnaforaOutput()!=null) evaluation.anaforaOutput = options.getAnaforaOutput();

			List<Integer> training = trainItems;
			List<Integer> testing = null;
			if(options.getTest()){
				training.addAll(devItems);
				testing = testItems;
			}else{
				testing = devItems;
			}
			//do closure on system, but not on gold, to calculate recall
			evaluation.skipTrain = options.getSkipTrain();
			evaluation.skipWrite = options.getSkipWrite();
			if(evaluation.skipTrain && options.getTest()){
				evaluation.prepareXMIsFor(testing);
			}else{
				evaluation.prepareXMIsFor(patientSets);
			}
			evaluation.writeProbabilities = options.getWriteProbabilities();

			//sort list:
			Collections.sort(training);
			Collections.sort(testing);

			//test or train or test
			evaluation.testOnTrain = options.getTestOnTrain();
			if(evaluation.testOnTrain){
				params.stats = evaluation.trainAndTest(training, training);
			}else{//test on testing set
				params.stats = evaluation.trainAndTest(training, testing);//training
			}
			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
			//			System.err.println("No closure on gold::Closure on System::Recall Mode");
			System.err.println(params.stats);

			//do closure on gold, but not on system, to calculate precision
			//			evaluation.skipTrain = true;
			//			recallModeEvaluation = false;
			//			params.stats = evaluation.trainAndTest(training, testing);//training);//
			//			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
			//			System.err.println("No closure on System::Closure on Gold::Precision Mode");
			//			System.err.println(params.stats);
			//
			//			//do closure on train, but not on test, to calculate plain results
			//			evaluation.skipTrain = true;
			//			evaluation.useClosure = false;
			//			//			evaluation.printErrors = false;
			//			params.stats = evaluation.trainAndTest(training, testing);//training);//
			//			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
			//			System.err.println("Closure on train::No closure on Test::Plain Mode");
			//			System.err.println(params.stats);

			if(options.getUseTmp()){
				// won't work because it's not empty. should we be concerned with this or is it responsibility of 
				// person invoking the tmp flag?
				FileUtils.deleteRecursive(workingDir);
			}
		}catch(ResourceInitializationException e){
			System.err.println("Error with parameter settings: " + params);
			e.printStackTrace();
		}
	}

	//  private ParameterSettings params;
	private boolean baseline;
	protected boolean useClosure;
	protected boolean useGoldAttributes;
	protected boolean skipTrain=false;
	public boolean skipWrite = false;
	//  protected boolean printRelations = false;
	private boolean writeProbabilities = false;
	protected boolean testOnTrain=false;

	public EvaluationOfNeuralEEAndETRelations(
			File baseDirectory,
			File rawTextDirectory,
			File xmlDirectory,
			XMLFormat xmlFormat,
			Subcorpus subcorpus,
			File xmiDirectory,
			File treebankDirectory,
			boolean useClosure,
			boolean printErrors,
			boolean printRelations,
			boolean baseline,
			boolean useGoldAttributes,
			String kernelParams,
			ParameterSettings params){
		super(
				baseDirectory,
				rawTextDirectory,
				xmlDirectory,
				xmlFormat,
				subcorpus,
				xmiDirectory,
				treebankDirectory,
				printErrors,
				printRelations,
				params);
		this.params = params;
		this.useClosure = useClosure;
		this.printErrors = printErrors;
		this.printRelations = printRelations;
		this.useGoldAttributes = useGoldAttributes;
		this.baseline = baseline;
		this.kernelParams = kernelParams == null ? null : kernelParams.split(" ");
	}

	//  public EvaluationOfTemporalRelations(File baseDirectory, File rawTextDirectory,
	//      File knowtatorXMLDirectory, File xmiDirectory) {
	//
	//    super(baseDirectory, rawTextDirectory, knowtatorXMLDirectory, xmiDirectory, null);
	//    this.params = defaultParams;
	//    this.printErrors = false;
	//  }

	@Override
	protected void train(CollectionReader collectionReader, File directory) throws Exception {
		//	  if(this.baseline) return;
		if(this.skipTrain) return;
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class, BinaryTextRelation.class));
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveCrossSentenceRelations.class));
		if(!this.useGoldAttributes){
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveGoldAttributes.class));
		}
		if (this.useClosure) {
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddClosure.class));//aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class));
			//			aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(AddContain2Overlap.class));
			//			aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveBeforeAndOnRelations.class));
		}
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class));
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(AddFlippedOverlap.class));//add flipped overlap instances to training data

		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonTLINKRelations.class));//remove non tlink relations, such as alinks

		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(Overlap2Contains.class));

		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PreserveEventEventRelations.class));
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(RemoveNonUMLSEvents.class));

		//add unlabeled nearby system events as potential links: 
		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddEEPotentialRelations.class));
		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddPotentialRelations.class));	

		aggregateBuilder.add(
				AnalysisEngineFactory.createEngineDescription(EventEventTokenBasedAnnotator.class,//EventEventTokenBasedAnnotator.class,EventEventPathsBasedAnnotator.class, EventEventTokenAndPosBasedAnnotator, EventEventPathsBasedAnnotator
						CleartkAnnotator.PARAM_IS_TRAINING,
						true,
						DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
						KerasStringOutcomeDataWriter.class,
						DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
						new File(directory,"event-event"),
						ScriptStringFeatureDataWriter.PARAM_SCRIPT_DIR,
						"scripts/nn/"
						) );

		aggregateBuilder.add(
				AnalysisEngineFactory.createEngineDescription(EventEventTokenBasedAnnotator.class,//EventTimeTokenAndPathBasedAnnotator.class,//
						CleartkAnnotator.PARAM_IS_TRAINING,
						true,
						DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
						KerasStringOutcomeDataWriter.class,
						DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
						new File(directory,"event-time"),
						ScriptStringFeatureDataWriter.PARAM_SCRIPT_DIR,
						"scripts/nn-et/"
						) );
		if(!this.skipWrite){
			SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());

			//    HideOutput hider = new HideOutput();
			JarClassifierBuilder.trainAndPackage(new File(directory,"event-event"));//"-c", "0.05");//"0.08","-w3","3","-w4","17","-w5","20","-w6","16","-w7","10","-w8","6", "-w9","45","-w10","30","-c", optArray[1]);//"-c", "0.05");//optArray);
			JarClassifierBuilder.trainAndPackage(new File(directory,"event-time")); //"-w3","2","-w4","19","-w5","13","-w6","22","-w7","96","-w8","18","-c", optArray[1]);//"-w4","18","-w5","14","-w6","21","-w7","100","-w8","19","-c", optArray[1]);//"0.05");//"-h","0","-c", "1000");//optArray);
		}
		//		JarClassifierBuilder.trainAndPackage(new File(directory,"event-event"), "-h","0","-c", "1000");
		//    hider.restoreOutput();
		//    hider.close();
	}

	@Override
	protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
			throws Exception {
		this.useClosure=false;
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
//		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
//				ViewCreatorAnnotator.class,
//				ViewCreatorAnnotator.PARAM_VIEW_NAME,
//				PROB_VIEW_NAME ) );

		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class));

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
				RemoveCrossSentenceRelations.class,
				RemoveCrossSentenceRelations.PARAM_SENTENCE_VIEW,
				CAS.NAME_DEFAULT_SOFA,
				RemoveCrossSentenceRelations.PARAM_RELATION_VIEW,
				GOLD_VIEW_NAME));
		
		if (!recallModeEvaluation && this.useClosure) { //closure for gold
			aggregateBuilder.add(
					AnalysisEngineFactory.createEngineDescription(AddClosure.class),//AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class),
					CAS.NAME_DEFAULT_SOFA,
					GOLD_VIEW_NAME);
		}

		//keep event event tlinks, remove the other relations
		//		aggregateBuilder.add(
		//				AnalysisEngineFactory.createEngineDescription(PreserveEventEventRelations.class),
		//				CAS.NAME_DEFAULT_SOFA,
		//				GOLD_VIEW_NAME);

		//remove non-tlink relations, such as alinks
//		aggregateBuilder.add(
//				AnalysisEngineFactory.createEngineDescription(RemoveNonTLINKRelations.class),
//				CAS.NAME_DEFAULT_SOFA,
//				GOLD_VIEW_NAME);

		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonUMLSEvents.class));

		

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class),
				CAS.NAME_DEFAULT_SOFA,
				GOLD_VIEW_NAME);

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveRelations.class));
		AnalysisEngineDescription aed = null;
		aed=AnalysisEngineFactory.createEngineDescription(EventEventTokenBasedAnnotator.class,//EventEventTokenBasedAnnotator.class,EventEventPathsBasedAnnotator.class, EventEventTokenAndPosBasedAnnotator
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory,"event-event"), "model.jar").getPath());
		aggregateBuilder.add(aed);
		aed = AnalysisEngineFactory.createEngineDescription(EventTimeTokenBasedAnnotator.class,//EventTimeTokenAndPathBasedAnnotator.class,//EventTimeTokenBasedAnnotator.class,//
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory,"event-time"), "model.jar").getPath());
		aggregateBuilder.add(aed);

		aed = DocTimeRelAnnotator.createAnnotatorDescription(new File("target/eval/event-properties/train_and_test/docTimeRel/model.jar").getAbsolutePath());		
		aggregateBuilder.add(aed);

//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CrossSentenceTemporalRelationAnnotator.class));
		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(WithinSentenceBeforeRelationAnnotator.class));

		if(this.anaforaOutput != null){
			aed = AnalysisEngineFactory.createEngineDescription(WriteAnaforaXML.class, WriteAnaforaXML.PARAM_OUTPUT_DIR, this.anaforaOutput);
			aggregateBuilder.add(aed, "TimexView", CAS.NAME_DEFAULT_SOFA);
		}

		File outf = null;
		if (recallModeEvaluation && this.useClosure) {//add closure for system output
			aggregateBuilder.add(
					AnalysisEngineFactory.createEngineDescription(AddClosure.class),//AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class),
					GOLD_VIEW_NAME,
					CAS.NAME_DEFAULT_SOFA
					);
			outf =  new File("target/eval/thyme/SystemError_eventEvent_recall_test.txt");
		}else if (!recallModeEvaluation && this.useClosure){
			outf =  new File("target/eval/thyme/SystemError_eventEvent_precision_test.txt");
		}else{
			outf =  new File("target/eval/thyme/SystemError_eventEvent_plain_test.txt");
		}

		PrintWriter outDrop =null;
		outDrop = new PrintWriter(new BufferedWriter(new FileWriter(outf, false)));

		Function<BinaryTextRelation, ?> getSpan = new Function<BinaryTextRelation, HashableArguments>() {
			public HashableArguments apply(BinaryTextRelation relation) {
				return new HashableArguments(relation);
			}
		};
		Function<BinaryTextRelation, String> getOutcome = AnnotationStatistics.annotationToFeatureValue("category");

		AnnotationStatistics<String> stats = new AnnotationStatistics<>();
		JCasIterator jcasIter =new JCasIterator(collectionReader, aggregateBuilder.createAggregate());
		JCas jCas = null;
		while(jcasIter.hasNext()) {
			jCas = jcasIter.next();
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			Collection<BinaryTextRelation> goldRelations = JCasUtil.select(
					goldView,
					BinaryTextRelation.class);
			Collection<BinaryTextRelation> systemRelations = JCasUtil.select(
					systemView,
					BinaryTextRelation.class);

			stats.add(goldRelations, systemRelations, getSpan, getOutcome);
			if(this.printRelations){
				URI uri = ViewUriUtil.getURI(jCas);
				String[] path = uri.getPath().split("/");
				printRelationAnnotations(path[path.length - 1], systemRelations);
			}
			if(this.printErrors){
				Map<HashableArguments, BinaryTextRelation> goldMap = Maps.newHashMap();
				for (BinaryTextRelation relation : goldRelations) {
					goldMap.put(new HashableArguments(relation), relation);
				}
				Map<HashableArguments, BinaryTextRelation> systemMap = Maps.newHashMap();
				for (BinaryTextRelation relation : systemRelations) {
					systemMap.put(new HashableArguments(relation), relation);
				}
				Set<HashableArguments> all = Sets.union(goldMap.keySet(), systemMap.keySet());
				List<HashableArguments> sorted = Lists.newArrayList(all);
				Collections.sort(sorted);
				outDrop.println("Doc id: " + ViewUriUtil.getURI(jCas).toString());
				for (HashableArguments key : sorted) {
					BinaryTextRelation goldRelation = goldMap.get(key);
					BinaryTextRelation systemRelation = systemMap.get(key);
					if (goldRelation == null) {
						outDrop.println("System added: " + formatRelation(systemRelation));
					} else if (systemRelation == null) {
						outDrop.println("System dropped: " + formatRelation(goldRelation));
					} else if (!systemRelation.getCategory().equals(goldRelation.getCategory())) {
						String label = systemRelation.getCategory();
						outDrop.printf("System labeled %s for %s\n", label, formatRelation(goldRelation));
					} else{
						outDrop.println("Nailed it! " + formatRelation(systemRelation));
					}
				}
			}
		}
		outDrop.close();
		return stats;
	}

	public static class RemoveNonUMLSEvents extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		public static final String PARAM_GOLD_VIEW = "GoldView";

		@ConfigurationParameter(name = PARAM_GOLD_VIEW,mandatory=false)
		private String goldViewName = CAS.NAME_DEFAULT_SOFA;

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas sysView;
			JCas goldView;
			try {
				sysView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
				goldView = jCas.getView(PARAM_GOLD_VIEW);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}
			for(TemporalTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, TemporalTextRelation.class))){
				Annotation arg1 = relation.getArg1().getArgument();
				Annotation arg2 = relation.getArg2().getArgument();
				boolean arg1Valid = false;
				boolean arg2Valid = false;
				for (EventMention event : JCasUtil.selectCovered(sysView, EventMention.class, arg1)){
					if(!event.getClass().equals(EventMention.class)){
						arg1Valid = true;
						break;
					}
				}
				for (EventMention event : JCasUtil.selectCovered(sysView, EventMention.class, arg2)){
					if(!event.getClass().equals(EventMention.class)){
						arg2Valid = true;
						break;
					}
				}
				if(arg1Valid && arg2Valid){
					// these are the kind we keep.
					continue;
				}
				arg1.removeFromIndexes();
				arg2.removeFromIndexes();
				relation.removeFromIndexes();
			}
		}   
	}


	static void createRelation(JCas jCas, Annotation arg1,
			Annotation arg2, String category) {
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
		relation.setCategory(category);
		relation.addToIndexes();

	}


	/*
  private static String formatRelation(BinaryTextRelation relation) {
	  IdentifiedAnnotation arg1 = (IdentifiedAnnotation)relation.getArg1().getArgument();
	  IdentifiedAnnotation arg2 = (IdentifiedAnnotation)relation.getArg2().getArgument();
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

  private static void printRelationAnnotations(String fileName, Collection<BinaryTextRelation> relations) {

	  for(BinaryTextRelation binaryTextRelation : relations) {

		  Annotation arg1 = binaryTextRelation.getArg1().getArgument();
		  Annotation arg2 = binaryTextRelation.getArg2().getArgument();

		  String arg1Type = arg1.getClass().getSimpleName();
		  String arg2Type = arg2.getClass().getSimpleName();

		  int arg1Begin = arg1.getBegin();
		  int arg1End = arg1.getEnd();
		  int arg2Begin = arg2.getBegin();
		  int arg2End = arg2.getEnd();

		  String category = binaryTextRelation.getCategory();

		  System.out.format("%s\t%s\t%s\t%d\t%d\t%s\t%d\t%d\n", 
				  fileName, category, arg1Type, arg1Begin, arg1End, arg2Type, arg2Begin, arg2End);
	  }
  }
	 */



	//	@SuppressWarnings("unchecked")
	//	private static <SPAN> Collection<BinaryTextRelation> getDuplicateRelations(
	//			Collection<BinaryTextRelation> goldRelations,
	//			Function<BinaryTextRelation, ?> getSpan) {
	//		Set<BinaryTextRelation> duplicateRelations = Sets.newHashSet();
	//		//build a multimap that map gold span to gold relation
	//		Multimap<SPAN, BinaryTextRelation> spanToRelation = HashMultimap.create();
	//		for (BinaryTextRelation relation : goldRelations) {
	//			spanToRelation.put((SPAN) getSpan.apply(relation), relation);			
	//		}
	//		for (SPAN span: spanToRelation.keySet()){
	//			Collection<BinaryTextRelation> relations = spanToRelation.get(span);
	//			if(relations.size()>1){//if same span maps to multiple relations
	//				duplicateRelations.addAll(relations);
	//			}
	//		}
	//		return duplicateRelations;
	//	}

	//	private static Collection<BinaryTextRelation> removeNonGoldRelations(
	//			Collection<BinaryTextRelation> systemRelations, Collection<BinaryTextRelation> goldRelations) {
	//		//remove non-gold pairs from system relations:
	//		Set<BinaryTextRelation> goodSys = Sets.newHashSet();
	//
	//		for(BinaryTextRelation sysrel : systemRelations){
	//			Annotation sysArg1 = sysrel.getArg1().getArgument();
	//			Annotation sysArg2 = sysrel.getArg2().getArgument();
	//			for(BinaryTextRelation goldrel : goldRelations){
	//				Annotation goldArg1 = goldrel.getArg1().getArgument();
	//				Annotation goldArg2 = goldrel.getArg2().getArgument();
	//				if(matchSpan(sysArg1, goldArg1) && matchSpan(sysArg2, goldArg2)){
	//					goodSys.add(sysrel);
	//					continue;
	//				}else if (matchSpan(sysArg2, goldArg1) && matchSpan(sysArg1, goldArg2)){//the order of system pair was flipped 
	//					if(sysrel.getCategory().equals("OVERLAP")){ //if the relation is overlap, and the arg order was flipped, then change back the order
	//						RelationArgument tempArg = (RelationArgument) sysrel.getArg1().clone();
	//						sysrel.setArg1((RelationArgument) sysrel.getArg2().clone());
	//						sysrel.setArg2(tempArg);
	//					}//for other types of relation, still maintain the type.
	//					goodSys.add(sysrel);
	//					continue;
	//				}
	//			}
	//		}
	//
	//		return goodSys;
	//	}




	public static class RemoveNonTLINKRelations extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(
					jCas,
					BinaryTextRelation.class))) {
				if (!(relation instanceof TemporalTextRelation)) {
					relation.getArg1().removeFromIndexes();
					relation.getArg2().removeFromIndexes();
					relation.removeFromIndexes();
				}
			}
		}
	}

	public static class RemoveCrossSentenceRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

		public static final String PARAM_SENTENCE_VIEW = "SentenceView";

		@ConfigurationParameter(name = PARAM_SENTENCE_VIEW,mandatory=false)
		private String sentenceViewName = CAS.NAME_DEFAULT_SOFA;

		public static final String PARAM_RELATION_VIEW = "RelationView";

		@ConfigurationParameter(name = PARAM_RELATION_VIEW,mandatory=false)
		private String relationViewName = CAS.NAME_DEFAULT_SOFA;

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas sentenceView, relationView;
			try {
				sentenceView = jCas.getView(this.sentenceViewName);
				relationView = jCas.getView(this.relationViewName);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			// map events and times to the sentences that contain them
			Map<IdentifiedAnnotation, Integer> sentenceIndex = Maps.newHashMap();
			int index = -1;
			for (Sentence sentence : JCasUtil.select(sentenceView, Sentence.class)) {
				++index;
				for (EventMention event : JCasUtil.selectCovered(relationView, EventMention.class, sentence)) {
					sentenceIndex.put(event, index);
				}
				for (TimeMention time : JCasUtil.selectCovered(relationView, TimeMention.class, sentence)) {
					sentenceIndex.put(time, index);
				}
			}

			// remove any relations that are in different sentences.
			for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(
					relationView,
					BinaryTextRelation.class))) {
				Integer sent1 = sentenceIndex.get(relation.getArg1().getArgument());
				Integer sent2 = sentenceIndex.get(relation.getArg2().getArgument());
				if (sent1 == null || sent2 == null || !sent1.equals(sent2)) {
					relation.getArg1().removeFromIndexes();
					relation.getArg2().removeFromIndexes();
					relation.removeFromIndexes();
				}
			}
		}
	}


	public static class RemoveRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(
					jCas,
					BinaryTextRelation.class))) {
				relation.getArg1().removeFromIndexes();
				relation.getArg2().removeFromIndexes();
				relation.removeFromIndexes();
			}
		}
	}


	@PipeBitInfo(
			name = "Transitive Contains Adder",
			description = "Adds Contains temporal relations for annotations / relations in contain other relations.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
	)
	public static class AddTransitiveContainsRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {

			// collect many-to-many mappings of containment relations 
			Multimap<Annotation, Annotation> isContainedIn = HashMultimap.create();
			Multimap<Annotation, Annotation> contains = HashMultimap.create();
			Set<BinaryTextRelation> containsRelations = Sets.newHashSet();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
				if (relation.getCategory().equals("CONTAINS")) {
					containsRelations.add(relation);
					Annotation arg1 = relation.getArg1().getArgument();
					Annotation arg2 = relation.getArg2().getArgument();
					contains.put(arg1, arg2);
					isContainedIn.put(arg2, arg1);
				}
			}

			// look for X -> Y -> Z containment chains and add X -> Z relations
			Deque<Annotation> todo = new ArrayDeque<>(isContainedIn.keySet());
			while (!todo.isEmpty()) {
				Annotation next = todo.removeFirst();
				for (Annotation parent : Lists.newArrayList(isContainedIn.get(next))) {
					for (Annotation grandParent : Lists.newArrayList(isContainedIn.get(parent))) {
						if (!isContainedIn.containsEntry(next, grandParent)) {
							isContainedIn.put(next, grandParent);
							contains.put(grandParent, next);

							// once X -> Z has been added, we need to re-do all W where W -> X
							for (Annotation child : contains.get(next)) {
								todo.add(child);
							}
						}
					}
				}
			}

			// remove old relations
			for (BinaryTextRelation relation : containsRelations) {
				relation.getArg1().removeFromIndexes();
				relation.getArg2().removeFromIndexes();
				relation.removeFromIndexes();
			}

			// add new, transitive relations
			for (Annotation contained : isContainedIn.keySet()) {
				for (Annotation container : isContainedIn.get(contained)) {
					RelationArgument arg1 = new RelationArgument(jCas);
					arg1.setArgument(container);
					RelationArgument arg2 = new RelationArgument(jCas);
					arg2.setArgument(contained);
					BinaryTextRelation relation = new BinaryTextRelation(jCas);
					relation.setArg1(arg1);
					relation.setArg2(arg2);
					relation.setCategory("CONTAINS");
					arg1.addToIndexes();
					arg2.addToIndexes();
					relation.addToIndexes();
				}
			}
		}

	}

	@PipeBitInfo(
			name = "TLink Overlap Adder",
			description = "Adds an Overlap temporal relation for each Contains temporal relation.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
	)
	public static class AddContain2Overlap extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {

			Set<BinaryTextRelation> containsRelations = Sets.newHashSet();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
				if (relation.getCategory().equals("CONTAINS")) {
					containsRelations.add(relation);
				}
			}

			for (BinaryTextRelation relation : containsRelations) {
				RelationArgument arg1 = (RelationArgument) relation.getArg1().clone();
				RelationArgument arg2 = (RelationArgument) relation.getArg2().clone();
				BinaryTextRelation newrelation = new BinaryTextRelation(jCas);
				newrelation.setArg1(arg1);
				newrelation.setArg2(arg2);
				newrelation.setCategory("OVERLAP");
				arg1.addToIndexes();
				arg2.addToIndexes();
				newrelation.addToIndexes();
			}
		}
	}

	@PipeBitInfo(
			name = "Reverse Overlap TLinker",
			description = "Adds Overlap temporal relations with arguments flipped.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
	)
	public static class AddFlippedOverlap extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {

			Set<BinaryTextRelation> overlapRelations = Sets.newHashSet();
			Multimap<Annotation, Annotation> overlaps = HashMultimap.create();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
				if (relation.getCategory().equals("OVERLAP")) {
					overlapRelations.add(relation);
					Annotation arg1 = relation.getArg1().getArgument();
					Annotation arg2 = relation.getArg2().getArgument();
					overlaps.put(arg1, arg2);
				}
			}

			for (BinaryTextRelation orelation : overlapRelations) {
				Annotation argA = orelation.getArg1().getArgument();
				Annotation argB = orelation.getArg2().getArgument();
				//add overlap 
				if (!overlaps.containsEntry(argB, argA)) {
					//create a new flipped relation:
					RelationArgument arg1 = new RelationArgument(jCas);
					arg1.setArgument(argB);
					RelationArgument arg2 = new RelationArgument(jCas);
					arg2.setArgument(argA);
					BinaryTextRelation relation = new BinaryTextRelation(jCas);
					relation.setArg1(arg1);
					relation.setArg2(arg2);
					relation.setCategory("OVERLAP");
					arg1.addToIndexes();
					arg2.addToIndexes();
					relation.addToIndexes();
					overlaps.put(argB, argA);
				}

			}
		}
	}

	@PipeBitInfo(
			name = "TLink Closure Engine",
			description = "Performs closure on Temporal Relations",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
	)
	public static class AddClosure extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {

			Multimap<List<Annotation>, BinaryTextRelation> annotationsToRelation = HashMultimap.create();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)){
				String relationType = relation.getCategory();
				if(validTemporalType(relationType)){
					Annotation arg1 = relation.getArg1().getArgument();
					Annotation arg2 = relation.getArg2().getArgument();
					annotationsToRelation.put(Arrays.asList(arg1, arg2), relation);
				}
			}
			for (List<Annotation> span: Lists.newArrayList(annotationsToRelation.keySet())){
				Collection<BinaryTextRelation> relations = annotationsToRelation.get(span);
				if(relations.size()>1){//if same span maps to multiple relations
					Set<String> types = Sets.newHashSet();
					for(BinaryTextRelation relation: relations){
						types.add(relation.getCategory());
					}
					if(types.size()>1){
						for(BinaryTextRelation relation: Lists.newArrayList(relations)){
							annotationsToRelation.remove(span, relation);
							relation.getArg1().removeFromIndexes();
							relation.getArg2().removeFromIndexes();
							relation.removeFromIndexes();
						}
					}else if(types.size()==1){
						for (int i =1; i< relations.size(); i++){
							BinaryTextRelation relation = (BinaryTextRelation) relations.toArray()[i];
							annotationsToRelation.remove(span, relation);
							relation.getArg1().removeFromIndexes();
							relation.getArg2().removeFromIndexes();
							relation.removeFromIndexes();
						}
					}
				}
			}

			ArrayList<BinaryTextRelation> temporalRelation = new ArrayList<>(annotationsToRelation.values());//new ArrayList<BinaryTextRelation>();
			//			Map<List<Annotation>, BinaryTextRelation> temporalRelationLookup = new HashMap<List<Annotation>, BinaryTextRelation>();
			//
			//			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)){
			//				String relationType = relation.getCategory();
			//				if(validTemporalType(relationType)){
			//					Annotation arg1 = relation.getArg1().getArgument();
			//			        Annotation arg2 = relation.getArg2().getArgument();
			//			        BinaryTextRelation tempRelation = temporalRelationLookup.get(Arrays.asList(arg1, arg2));
			//					if( tempRelation == null){
			//						temporalRelation.add(relation);					
			//				        temporalRelationLookup.put(Arrays.asList(arg1, arg2), relation);
			//					}else{//if there is duplicate
			//						relation.getArg1().removeFromIndexes();
			//						relation.getArg2().removeFromIndexes();
			//						relation.removeFromIndexes();
			//					}
			//					
			//				}
			//			}

			if (!temporalRelation.isEmpty()){
				TLinkTypeArray2 relationArray = new TLinkTypeArray2(temporalRelation, new AnnotationIdCollection(temporalRelation));

				int addedCount = 0;
				for (BinaryTextRelation relation : relationArray.getClosedTlinks(jCas)) {
					RelationArgument arg1 = relation.getArg1();
					RelationArgument arg2 = relation.getArg2();
					String relationType = relation.getCategory();
					if(relationType.equals("CONTAINED-BY")||relationType.equals("AFTER")){//ignore these two categories, because their reciprocal already exist.
						continue;
					}
					//check if the inferred relation new:
					Collection<BinaryTextRelation> relations = annotationsToRelation.get(Arrays.asList(arg1.getArgument(), arg2.getArgument()));
					if(relations.isEmpty()){ //if haven't seen this inferred relation before, then add this relation
						arg1.addToIndexes();
						arg2.addToIndexes();
						relation.addToIndexes();
						addedCount++;
					}		
				}

				System.out.println( "**************************************************************");
				System.out.println( "Finally added closure relations: " + addedCount );
				System.out.println( "**************************************************************");
			}			

		}

		private static boolean validTemporalType(String relationType) {
			if(relationType.equals("CONTAINS")||relationType.equals("OVERLAP")||relationType.equals("BEFORE")||relationType.equals("ENDS-ON")||relationType.equals("BEGINS-ON"))
				return true;
			return false;
		}
	}

	//	public static class AddTransitiveBeforeAndOnRelations extends JCasAnnotator_ImplBase {
	//
	//		@Override
	//		public void process(JCas jCas) throws AnalysisEngineProcessException {
	//
	//			// collect many-to-many mappings of containment relations 
	//			Multimap<Annotation, Annotation> contains = HashMultimap.create();
	//			Multimap<Annotation, Annotation> before = HashMultimap.create();
	//			Multimap<Annotation, Annotation> endson = HashMultimap.create();
	//			Multimap<Annotation, Annotation> beginson = HashMultimap.create();
	//			Set<BinaryTextRelation> beforeRel = Sets.newHashSet();
	//
	//			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
	//				if (relation.getCategory().equals("CONTAINS")) {
	//					Annotation arg1 = relation.getArg1().getArgument();
	//					Annotation arg2 = relation.getArg2().getArgument();
	//					contains.put(arg1, arg2);
	//				}else if (relation.getCategory().equals("BEFORE")) {
	//					Annotation arg1 = relation.getArg1().getArgument();
	//					Annotation arg2 = relation.getArg2().getArgument();
	//					before.put(arg1, arg2);
	//					beforeRel.add(relation);
	//				}else if (relation.getCategory().equals("ENDS-ON")) {
	//					Annotation arg1 = relation.getArg1().getArgument();
	//					Annotation arg2 = relation.getArg2().getArgument();
	//					endson.put(arg1, arg2);
	//					if (!endson.containsEntry(arg2, arg1)) {
	//						endson.put(arg2, arg1);
	//					}
	//				}else if (relation.getCategory().equals("BEGINS-ON")) {
	//					Annotation arg1 = relation.getArg1().getArgument();
	//					Annotation arg2 = relation.getArg2().getArgument();
	//					beginson.put(arg1, arg2);
	//					if (!beginson.containsEntry(arg2, arg1)) {
	//						beginson.put(arg2, arg1);
	//					}
	//				}
	//			}
	//
	//			// for A BEFORE B, check if A and B Contain anything
	//			for (BinaryTextRelation brelation : beforeRel) {
	//				Annotation argA = brelation.getArg1().getArgument();
	//				Annotation argB = brelation.getArg2().getArgument();
	//				//add contained before
	//				for (Annotation childA : contains.get(argA)) {
	//					for (Annotation childB : contains.get(argB)) {
	//						if (!before.containsEntry(childA, childB)) {
	//							//create a new before relation:
	//							RelationArgument arg1 = new RelationArgument(jCas);
	//							arg1.setArgument(childA);
	//							RelationArgument arg2 = new RelationArgument(jCas);
	//							arg2.setArgument(childB);
	//							BinaryTextRelation relation = new BinaryTextRelation(jCas);
	//							relation.setArg1(arg1);
	//							relation.setArg2(arg2);
	//							relation.setCategory("BEFORE");
	//							arg1.addToIndexes();
	//							arg2.addToIndexes();
	//							relation.addToIndexes();
	//							before.put(childA, childB);
	//						}
	//					}
	//				}
	//				//add ends-on A
	//				for (Annotation endsOnA : endson.get(argA)) {
	//					if (!before.containsEntry(endsOnA, argB)) {
	//						//create a new before relation:
	//						RelationArgument arg1 = new RelationArgument(jCas);
	//						arg1.setArgument(endsOnA);
	//						RelationArgument arg2 = new RelationArgument(jCas);
	//						arg2.setArgument(argB);
	//						BinaryTextRelation relation = new BinaryTextRelation(jCas);
	//						relation.setArg1(arg1);
	//						relation.setArg2(arg2);
	//						relation.setCategory("BEFORE");
	//						arg1.addToIndexes();
	//						arg2.addToIndexes();
	//						relation.addToIndexes();
	//						before.put(endsOnA, argB);
	//					}
	//				}
	//				//add begins-on B
	//				for (Annotation beginsOnB : beginson.get(argB)) {
	//					if (!before.containsEntry(argA, beginsOnB)) {
	//						//create a new before relation:
	//						RelationArgument arg1 = new RelationArgument(jCas);
	//						arg1.setArgument(argA);
	//						RelationArgument arg2 = new RelationArgument(jCas);
	//						arg2.setArgument(beginsOnB);
	//						BinaryTextRelation relation = new BinaryTextRelation(jCas);
	//						relation.setArg1(arg1);
	//						relation.setArg2(arg2);
	//						relation.setCategory("BEFORE");
	//						arg1.addToIndexes();
	//						arg2.addToIndexes();
	//						relation.addToIndexes();
	//						before.put(argA, beginsOnB);
	//					}
	//				}
	//			}
	//		}
	//
	//	}
}
