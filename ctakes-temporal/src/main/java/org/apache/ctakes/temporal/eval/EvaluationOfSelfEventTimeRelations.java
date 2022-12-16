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
package org.apache.ctakes.temporal.eval;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.EventTimeRelationAnnotator;
import org.apache.ctakes.temporal.ae.EventTimeSelfRelationAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.utils.AnnotationIdCollection;
import org.apache.ctakes.temporal.utils.TLinkTypeArray2;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
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
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.util.ViewUriUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

//import java.util.HashSet;
//import org.apache.ctakes.temporal.ae.EventTimeSyntacticAnnotator;
//import org.apache.ctakes.temporal.ae.EventTimeRelationAnnotator;
//import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
//import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.WriteI2B2XML;
//import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat;
//import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
//import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
//import org.cleartk.ml.tksvmlight.TkSvmLightStringOutcomeDataWriter;

public class EvaluationOfSelfEventTimeRelations extends
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
			EvaluationOfSelfEventTimeRelations evaluation = new EvaluationOfSelfEventTimeRelations(
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
			evaluation.prepareXMIsFor(patientSets);
			if(options.getI2B2Output()!=null) evaluation.setI2B2Output(options.getI2B2Output() + "/temporal-relations/event-time");
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
			params.stats = evaluation.trainAndRetrainAndTest(training, testing);//training);//
			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
			System.err.println("No closure on gold::Closure on System::Recall Mode");
			System.err.println(params.stats);

			//do closure on gold, but not on system, to calculate precision
			evaluation.skipTrain = true;
			recallModeEvaluation = false;
			params.stats = evaluation.trainAndRetrainAndTest(training, testing);//training);//
			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
			System.err.println("No closure on System::Closure on Gold::Precision Mode");
			System.err.println(params.stats);

			//do closure on train, but not on test, to calculate plain results
			evaluation.skipTrain = true;
			evaluation.useClosure = false;
			params.stats = evaluation.trainAndRetrainAndTest(training, testing);//training);//
			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
			System.err.println("Closure on train::No closure on Test::Plain Mode");
			System.err.println(params.stats);


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

	public AnnotationStatistics<String> trainAndRetrainAndTest(List<Integer> trainItems, List<Integer> testItems) 
			throws Exception {
		File trainDirectory = new File(this.baseDirectory, "train");
		File retrainDirectory = new File(this.baseDirectory, "retrain_and_test");
		trainDirectory.mkdirs();
		retrainDirectory.mkdir();
		this.train(this.getCollectionReader(trainItems), trainDirectory);
		this.preTest(this.getCollectionReader(trainItems), trainDirectory, retrainDirectory);
		return this.test(this.getCollectionReader(testItems), retrainDirectory);
	}

	//  private ParameterSettings params;
	private boolean baseline;
	protected boolean useClosure;
	protected boolean useGoldAttributes;
	protected boolean skipTrain=false;
	//  protected boolean printRelations = false;

	public EvaluationOfSelfEventTimeRelations(
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
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(RemoveNonContainsRelations.class));
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(AddFlippedOverlap.class));//add flipped overlap instances to training data

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class));
		aggregateBuilder.add(EventTimeRelationAnnotator.createDataWriterDescription(
				LibLinearStringOutcomeDataWriter.class,
				//				LIBSVMStringOutcomeDataWriter.class,
				//				TKSVMlightStringOutcomeDataWriter.class,
				//        TKLIBSVMStringOutcomeDataWriter.class,
				//        SVMlightStringOutcomeDataWriter.class,        
				new File(directory,"event-time"),
				params.probabilityOfKeepingANegativeExample));
		//		aggregateBuilder.add(EventEventRelationAnnotator.createDataWriterDescription(
		//				LIBSVMStringOutcomeDataWriter.class,
		//				new File(directory,"event-event"), 
		//				params.probabilityOfKeepingANegativeExample));
		SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
		String[] optArray;

		if(this.kernelParams == null){
			ArrayList<String> svmOptions = new ArrayList<>();
			svmOptions.add("-c"); svmOptions.add(""+params.svmCost);        // svm cost
			svmOptions.add("-t"); svmOptions.add(""+params.svmKernelIndex); // kernel index 
			svmOptions.add("-d"); svmOptions.add("3");                      // degree parameter for polynomial
			svmOptions.add("-g"); svmOptions.add(""+params.svmGamma);
			if(params.svmKernelIndex==ParameterSettings.SVM_KERNELS.indexOf("tk")){
				svmOptions.add("-S"); svmOptions.add(""+params.secondKernelIndex);   // second kernel index (similar to -t) for composite kernel
				String comboFlag = (params.comboOperator == ComboOperator.SUM ? "+" : params.comboOperator == ComboOperator.PRODUCT ? "*" : params.comboOperator == ComboOperator.TREE_ONLY ? "T" : "V");
				svmOptions.add("-C"); svmOptions.add(comboFlag);
				svmOptions.add("-L"); svmOptions.add(""+params.lambda);
				svmOptions.add("-T"); svmOptions.add(""+params.tkWeight);
				svmOptions.add("-N"); svmOptions.add("3");   // normalize trees and features
			}
			optArray = svmOptions.toArray(new String[]{});
		}else{
			optArray = this.kernelParams;
			for(int i = 0; i < optArray.length; i+=2){
				optArray[i] = "-" + optArray[i];
			}
		}

		//    HideOutput hider = new HideOutput();
		JarClassifierBuilder.trainAndPackage(new File(directory,"event-time"), "-w3","2","-w4","18","-w5","13","-w6","21","-w7","87","-w8","19","-c", optArray[1]);//"0.05");//"-h","0","-c", "1000");//optArray);
		//		JarClassifierBuilder.trainAndPackage(new File(directory,"event-event"), "-h","0","-c", "1000");
		//    hider.restoreOutput();
		//    hider.close();
	}

	@Override
	protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
			throws Exception {
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
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

		aggregateBuilder.add(
				AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class),
				CAS.NAME_DEFAULT_SOFA,
				GOLD_VIEW_NAME);

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveRelations.class));
		aggregateBuilder.add(
				EventTimeRelationAnnotator.createAnnotatorDescription(
					Paths.get(directory.getAbsolutePath(),"event-time").toAbsolutePath().toString()
				)
		);

		if(this.i2b2Output != null){
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(WriteI2B2XML.class, WriteI2B2XML.PARAM_OUTPUT_DIR, this.i2b2Output), "TimexView", CAS.NAME_DEFAULT_SOFA);
		}

		File outf = null;
		if (recallModeEvaluation && this.useClosure) {//add closure for system output
			aggregateBuilder.add(
					AnalysisEngineFactory.createEngineDescription(AddClosure.class),//AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class),
					GOLD_VIEW_NAME,
					CAS.NAME_DEFAULT_SOFA
					);
			aggregateBuilder.add(
					AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class),
					GOLD_VIEW_NAME,
					CAS.NAME_DEFAULT_SOFA
					);
			outf =  new File("target/eval/thyme/SystemError_eventTime_recall_dev.txt");
		}else if (!recallModeEvaluation && this.useClosure){
			outf =  new File("target/eval/thyme/SystemError_eventTime_precision_dev.txt");
		}else{
			outf =  new File("target/eval/thyme/SystemError_eventTime_plain_dev.txt");
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

			//newly add
			//			systemRelations = removeNonGoldRelations(systemRelations, goldRelations, getSpan);//for removing non-gold pairs
			//			systemRelations = correctArgOrder(systemRelations, goldRelations);//change the argument order of "OVERLAP" relation, if the order is flipped
			//find duplicates in gold relations:
			//			Collection<BinaryTextRelation> duplicateGoldRelations = getDuplicateRelations(goldRelations, getSpan);
			//			if(!duplicateGoldRelations.isEmpty()){
			//				System.err.println("******Duplicate gold relations in : " + ViewURIUtil.getURI(jCas).toString());
			//				for (BinaryTextRelation rel : duplicateGoldRelations){
			//					System.err.println("Duplicate : "+ formatRelation(rel));
			//				}
			//			}
			//end newly add

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

	private void preTest(CollectionReader collectionReader, File directory, File retrainDirectory) throws Exception {
		if(this.skipTrain) return;
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
//		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class,TimeMention.class));

		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class, TemporalTextRelation.class));
//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class));
		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
		//				RemoveCrossSentenceRelations.class,
		//				RemoveCrossSentenceRelations.PARAM_SENTENCE_VIEW,
		//				CAS.NAME_DEFAULT_SOFA,
		//				RemoveCrossSentenceRelations.PARAM_RELATION_VIEW,
		//				GOLD_VIEW_NAME));


//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveRelations.class));
		//annotate on system-generated events, for event-time relations
		aggregateBuilder.add(EventTimeSelfRelationAnnotator.createEngineDescription(
				Paths.get(directory.getAbsolutePath(),"event-time").toAbsolutePath().toString())
		);

		//		//re-train:
		//overwrite system-generated relations by gold temporal relations:
//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(OverwriteTemporalRelationToSystem.class));

		//add closure for system output
		aggregateBuilder.add(
				AnalysisEngineFactory.createEngineDescription(AddClosure.class),//AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class),
				GOLD_VIEW_NAME,
				CAS.NAME_DEFAULT_SOFA
				);
		aggregateBuilder.add(
				AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class),
				GOLD_VIEW_NAME,
				CAS.NAME_DEFAULT_SOFA
				);

		//		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class, BinaryTextRelation.class));//could not copy over, because it will remove all system generated relations
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveCrossSentenceRelations.class));
		
		if(!this.useGoldAttributes){
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveGoldAttributes.class));
		}

		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class));
		aggregateBuilder.add(EventTimeSelfRelationAnnotator.createDataWriterDescription(
				LibLinearStringOutcomeDataWriter.class,
				//				LIBSVMStringOutcomeDataWriter.class,
				//				TKSVMlightStringOutcomeDataWriter.class,
				//        TKLIBSVMStringOutcomeDataWriter.class,
				//        SVMlightStringOutcomeDataWriter.class,        
				new File(retrainDirectory,"event-time"),
				params.probabilityOfKeepingANegativeExample));
		//		aggregateBuilder.add(EventEventRelationAnnotator.createDataWriterDescription(
		//				LIBSVMStringOutcomeDataWriter.class,
		//				new File(directory,"event-event"), 
		//				params.probabilityOfKeepingANegativeExample));
		SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
		String[] optArray;

		if(this.kernelParams == null){
			ArrayList<String> svmOptions = new ArrayList<>();
			svmOptions.add("-c"); svmOptions.add(""+params.svmCost);        // svm cost
			svmOptions.add("-t"); svmOptions.add(""+params.svmKernelIndex); // kernel index 
			svmOptions.add("-d"); svmOptions.add("3");                      // degree parameter for polynomial
			svmOptions.add("-g"); svmOptions.add(""+params.svmGamma);
			if(params.svmKernelIndex==ParameterSettings.SVM_KERNELS.indexOf("tk")){
				svmOptions.add("-S"); svmOptions.add(""+params.secondKernelIndex);   // second kernel index (similar to -t) for composite kernel
				String comboFlag = (params.comboOperator == ComboOperator.SUM ? "+" : params.comboOperator == ComboOperator.PRODUCT ? "*" : params.comboOperator == ComboOperator.TREE_ONLY ? "T" : "V");
				svmOptions.add("-C"); svmOptions.add(comboFlag);
				svmOptions.add("-L"); svmOptions.add(""+params.lambda);
				svmOptions.add("-T"); svmOptions.add(""+params.tkWeight);
				svmOptions.add("-N"); svmOptions.add("3");   // normalize trees and features
			}
			optArray = svmOptions.toArray(new String[]{});
		}else{
			optArray = this.kernelParams;
			for(int i = 0; i < optArray.length; i+=2){
				optArray[i] = "-" + optArray[i];
			}
		}

		//    HideOutput hider = new HideOutput();
		JarClassifierBuilder.trainAndPackage(new File(retrainDirectory,"event-time"), "-w1","0.6","-c", optArray[1]);//"-w1","0.6","-w3","28","-w4","8","-w5","2","-w6","9","-w7","9","-w8","12","-c", optArray[1]);//"0.05");//"-h","0","-c", "1000");//optArray);
		//		JarClassifierBuilder.trainAndPackage(new File(directory,"event-event"), "-h","0","-c", "1000");
		//    hider.restoreOutput();
		//    hider.close();
		// TODO Auto-generated method stub

	}


	public static class RemoveEventEventRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		public static final String PARAM_RELATION_VIEW = "RelationView";
		@ConfigurationParameter(name = PARAM_RELATION_VIEW,mandatory=false)
		private String relationViewName = CAS.NAME_DEFAULT_SOFA;

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas relationView;
			try {
				relationView = jCas.getView(this.relationViewName);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(relationView, BinaryTextRelation.class))){
				//	    	  if(relation.getCategory().equals("CONTAINS")){
				RelationArgument arg1 = relation.getArg1();
				RelationArgument arg2 = relation.getArg2();
				if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention ||
						arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention){
					// these are the kind we keep.
					continue;
				}
				//	    		  if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof EventMention){
				arg1.removeFromIndexes();
				arg2.removeFromIndexes();
				relation.removeFromIndexes();
			}
			//	      }

		}
	}

	public static class OverwriteTemporalRelationToSystem extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas systemView;
			JCas goldView;
			try {
				systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
				goldView = jCas.getView(GOLD_VIEW_NAME);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}
			
			Map<List<Integer>, TemporalTextRelation> goldRelationLookup = new HashMap<>();
			
			for(TemporalTextRelation relation: JCasUtil.select(goldView, TemporalTextRelation.class)){
				Annotation arg1 = relation.getArg1().getArgument();
				Annotation arg2 = relation.getArg2().getArgument();
				
				//populate gold relation hashmap:
				goldRelationLookup.put(Arrays.asList(arg1.getBegin(),arg1.getEnd(),arg2.getBegin(),arg2.getEnd()), relation);
			}
			
			//iterate system relations to update any conflicts with gold
			for(TemporalTextRelation relation: JCasUtil.select(systemView, TemporalTextRelation.class)){
				Annotation arg1 = relation.getArg1().getArgument();
				Annotation arg2 = relation.getArg2().getArgument();
				String sysLabel = relation.getCategory();
				int arg1Beg = arg1.getBegin();
				int arg1End = arg1.getEnd();
				int arg2Beg = arg2.getBegin();
				int arg2End = arg2.getEnd();
				TemporalTextRelation goldRel = goldRelationLookup.get(Arrays.asList(arg1Beg,arg1End,arg2Beg,arg2End));
				String goldLabel = null;
				if(goldRel != null){
					goldLabel = goldRel.getCategory();
					if(!goldLabel.equals(sysLabel)){
						relation.setCategory(goldLabel);
					}
				}else{//gold relation is not found
					goldRel = goldRelationLookup.get(Arrays.asList(arg2Beg,arg2End, arg1Beg,arg1End));
					if(goldRel != null){
						RelationArgument tempArg = (RelationArgument) relation.getArg1().clone();
						relation.setArg1((RelationArgument) relation.getArg2().clone());
						relation.setArg2(tempArg);
						goldLabel = goldRel.getCategory();
						relation.setCategory(goldLabel);
					}
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
	public static class AddClosure extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

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
