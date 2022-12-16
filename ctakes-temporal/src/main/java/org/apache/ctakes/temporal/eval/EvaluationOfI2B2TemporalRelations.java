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
import org.apache.ctakes.core.ae.CDASegmentAnnotator;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.*;
import org.apache.ctakes.temporal.ae.baselines.RecallBaselineEventTimeRelationAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventEventThymeRelations.AddEEPotentialRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.AddPotentialRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.utils.AnnotationIdCollection;
import org.apache.ctakes.temporal.utils.TLinkTypeArray2;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
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
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.util.ViewUriUtil;

import java.io.*;
import java.net.URI;
import java.util.*;

//import org.cleartk.classifier.tksvmlight.TKSVMlightStringOutcomeDataWriter;

public class EvaluationOfI2B2TemporalRelations extends
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

	private static final String EVENT_TIME = "event_time";
	private static final String EVENT_EVENT = "event_event";
	private static final String EVENT_DISCHARGE = "event_dischargeTime";
	private static final String EVENT_ADMISSION = "event_admissionTime";
	private static final String TIME_DISCHARGE = "time_dischargeTime";
	private static final String TIME_ADMISSION = "time_admissionTime";
	//	private static final String EVENT_ADMIT = "event_admission";
	private static final String TEMP_CROSSSENT = "temp_crossSentence";
	private static final String TEMPET_CROSSSENT = "eventTime_crossSentence";
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
			File workingDir = new File("target/eval/temporal-relations/");
			if(!workingDir.exists()) workingDir.mkdirs();
			if(options.getUseTmp()){
				File tempModelDir = File.createTempFile("temporal", null, workingDir);
				tempModelDir.delete();
				tempModelDir.mkdir();
				workingDir = tempModelDir;
			}
			EvaluationOfI2B2TemporalRelations evaluation = new EvaluationOfI2B2TemporalRelations(
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
			evaluation.skipTrain = options.getSkipTrain();
			params.stats = evaluation.trainAndTest(training, testing);//training);//
			//      System.err.println(options.getKernelParams() == null ? params : options.getKernelParams());
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

	//  private ParameterSettings params;
	private boolean baseline;
	protected boolean useClosure;
	protected boolean useGoldAttributes;
	protected boolean skipTrain=false;
	//  protected boolean printRelations = false;

	public EvaluationOfI2B2TemporalRelations(
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
		//add sectionizer
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
				CDASegmentAnnotator.class,
				CDASegmentAnnotator.PARAM_SECTIONS_FILE,
				"org/apache/ctakes/temporal/section/ccda_sections.txt"));
		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class, BinaryTextRelation.class));
		aggregateBuilder.add(DocTimeRelAnnotator
				.createAnnotatorDescription("/org/apache/ctakes/temporal/models/doctimerel/model.jar"));
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(RemoveCrossSentenceRelations.class));
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNullArgumentRelations.class));
		if(!this.useGoldAttributes){
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveGoldAttributes.class));
		}
		if (this.useClosure) {
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddClosure.class));//aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddTransitiveContainsRelations.class));
			//			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddContain2Overlap.class));
			//			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddTransitiveBeforeAndOnRelations.class));
		}
		//output all long distances
		//		aggregateBuilder.add(
		//				AnalysisEngineFactory.createPrimitiveDescription(FindLongDisRelations.class));//AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class),
		//				CAS.NAME_DEFAULT_SOFA,
		//				GOLD_VIEW_NAME);
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(RemoveNonContainsRelations.class));
//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddFlippedOverlap.class));//add flipped overlap instances to training data

		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(RemoveEventEventRelations.class));
		//test rules:
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(TemporalRelationRuleAnnotator.class));
		
		//add unlabeled nearby system events as potential event-time links: 
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddPotentialRelations.class));
		//add unlabeled nearby system events as potential event-event links: 
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddEEPotentialRelations.class));

		aggregateBuilder.add(EventTimeI2B2RelationAnnotator.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,
//				LibLinearStringOutcomeDataWriter.class,
				// TKSVMlightStringOutcomeDataWriter.class,
				//        TKLIBSVMStringOutcomeDataWriter.class,
				//        SVMlightStringOutcomeDataWriter.class,        
				new File(directory,EVENT_TIME),
				params.probabilityOfKeepingANegativeExample));
		aggregateBuilder.add(EventEventI2B2RelationAnnotator.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,
//				LibLinearStringOutcomeDataWriter.class,//TKSVMlightStringOutcomeDataWriter.class,//
				//				LIBLINEARStringOutcomeDataWriter.class,
				new File(directory,EVENT_EVENT), 
				params.probabilityOfKeepingANegativeExample));
		aggregateBuilder.add(EventDischargeTimeAnnotator.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,
//				LibLinearStringOutcomeDataWriter.class,
				new File(directory,EVENT_DISCHARGE)));
		aggregateBuilder.add(EventAdmissionTimeAnnotator.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,
//				LibLinearStringOutcomeDataWriter.class,
				new File(directory,EVENT_ADMISSION)));
		aggregateBuilder.add(ConsecutiveSentencesEventEventRelationAnnotator.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,
//				LibLinearStringOutcomeDataWriter.class,
				new File(directory,TEMP_CROSSSENT), 
				params.probabilityOfKeepingANegativeExample));
		aggregateBuilder.add(ConsecutiveSentencesEventTimeRelationAnnotator.createDataWriterDescription(
				LibSvmStringOutcomeDataWriter.class,
//				LibLinearStringOutcomeDataWriter.class,
				new File(directory,TEMPET_CROSSSENT), 
				params.probabilityOfKeepingANegativeExample));
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
		JarClassifierBuilder.trainAndPackage(new File(directory,EVENT_TIME), "-h","0","-c", "1000", "-w2","0.5","-w3","5","-w4","8");//"-h","0","-c", "1000");//optArray);//"-c", "0.05");//
		JarClassifierBuilder.trainAndPackage(new File(directory,EVENT_EVENT), "-h","0","-c", "1000","-w2","0.5","-w3","4","-w4","3");
		JarClassifierBuilder.trainAndPackage(new File(directory,EVENT_DISCHARGE), "-h","0","-c", "1000");//,"-w2","23","-w3","24");
		JarClassifierBuilder.trainAndPackage(new File(directory,EVENT_ADMISSION), "-h","0","-c", "1000");//,"-w2","22","-w3","5");
		//		JarClassifierBuilder.trainAndPackage(new File(directory,TIME_ADMISSION), "-h","0","-c", "1000");
		//		JarClassifierBuilder.trainAndPackage(new File(directory,TIME_DISCHARGE), "-h","0","-c", "1000");
		JarClassifierBuilder.trainAndPackage(new File(directory,TEMP_CROSSSENT), "-h","0","-c", "1000","-w2","3","-w3","0.1");
		JarClassifierBuilder.trainAndPackage(new File(directory,TEMPET_CROSSSENT), "-h","0","-c", "1000","-w2","58","-w3","63","-w4","75");
		//    hider.restoreOutput();
		//    hider.close();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
			throws Exception {
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
		//add sectionizer
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
				CDASegmentAnnotator.class,
				CDASegmentAnnotator.PARAM_SECTIONS_FILE,
				"org/apache/ctakes/temporal/section/ccda_sections.txt"));
		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class));
		aggregateBuilder.add(DocTimeRelAnnotator
				.createAnnotatorDescription("/org/apache/ctakes/temporal/models/doctimerel/model.jar"));
		//		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
		//				RemoveCrossSentenceRelations.class,
		//				RemoveCrossSentenceRelations.PARAM_SENTENCE_VIEW,
		//				CAS.NAME_DEFAULT_SOFA,
		//				RemoveCrossSentenceRelations.PARAM_RELATION_VIEW,
		//				GOLD_VIEW_NAME));
		if (this.useClosure) {
			aggregateBuilder.add(
					AnalysisEngineFactory.createEngineDescription(AddClosure.class),//AnalysisEngineFactory.createEngineDescription(AddTransitiveContainsRelations.class),
					CAS.NAME_DEFAULT_SOFA,
					GOLD_VIEW_NAME);

			//			aggregateBuilder.add(
			//					AnalysisEngineFactory.createEngineDescription(AddContain2Overlap.class),
			//					CAS.NAME_DEFAULT_SOFA,
			//					GOLD_VIEW_NAME);
			//			aggregateBuilder.add(
			//					AnalysisEngineFactory.createEngineDescription(AddTransitiveBeforeAndOnRelations.class),
			//					CAS.NAME_DEFAULT_SOFA,
			//					GOLD_VIEW_NAME);
		}

		//		aggregateBuilder.add(
		//				AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class,
		//						RemoveNonContainsRelations.PARAM_RELATION_VIEW,
		//						GOLD_VIEW_NAME));
		//		aggregateBuilder.add(
		//				AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class),
		//				CAS.NAME_DEFAULT_SOFA,
		//				GOLD_VIEW_NAME);

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveRelations.class));

		aggregateBuilder.add(EventAdmissionTimeAnnotator.createAnnotatorDescription(new File(directory,EVENT_ADMISSION)));
		aggregateBuilder.add(EventDischargeTimeAnnotator.createAnnotatorDescription(new File(directory,EVENT_DISCHARGE)));
		//		aggregateBuilder.add(TimexAdmissionTimeAnnotator.createAnnotatorDescription(new File(directory,TIME_ADMISSION)));
		//		aggregateBuilder.add(TimexDischargeTimeAnnotator.createAnnotatorDescription(new File(directory,TIME_DISCHARGE)));

		aggregateBuilder.add(this.baseline ? RecallBaselineEventTimeRelationAnnotator.createAnnotatorDescription(directory) :
			EventTimeI2B2RelationAnnotator.createEngineDescription(new File(directory,EVENT_TIME)));
		aggregateBuilder.add(EventEventI2B2RelationAnnotator.createAnnotatorDescription(new File(directory,EVENT_EVENT)));
		aggregateBuilder.add(ConsecutiveSentencesEventEventRelationAnnotator.createAnnotatorDescription(new File(directory,TEMP_CROSSSENT)));
		aggregateBuilder.add(ConsecutiveSentencesEventTimeRelationAnnotator.createAnnotatorDescription(new File(directory,TEMPET_CROSSSENT)));
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(TemporalRelationRuleAnnotator.class));


		//		if (this.useClosure) {//add closure for system output
		//			aggregateBuilder.add(
		//					AnalysisEngineFactory.createPrimitiveDescription(AddClosure.class),//AnalysisEngineFactory.createPrimitiveDescription(AddTransitiveContainsRelations.class),
		//					GOLD_VIEW_NAME,
		//					CAS.NAME_DEFAULT_SOFA
		//					);
		//		}

		if(this.i2b2Output != null){
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(WriteI2B2XML.class, WriteI2B2XML.PARAM_OUTPUT_DIR, this.i2b2Output), "TimexView", CAS.NAME_DEFAULT_SOFA);
		}
		
		//remove the null argument from relations
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNullArgumentRelations.class));
		
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
			Collection<BinaryTextRelation> goldRelations = Lists.newArrayList();
			for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))){
				if(relation.getArg1().getArgument() != null && relation.getArg2().getArgument() != null && relation.getCategory() != null){
					goldRelations.add(relation);
				}
			}

			Collection<BinaryTextRelation> systemRelations = Lists.newArrayList();
			for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(systemView, BinaryTextRelation.class))){
				if(relation.getArg1().getArgument() != null && relation.getArg2().getArgument() != null && relation.getCategory() != null){
					systemRelations.add(relation);
				}
			}
			//newly add
			//			systemRelations = removeNonGoldRelations(systemRelations, goldRelations);//for removing non-gold pairs
//			systemRelations = correctArgOrder(systemRelations, goldRelations);//change the argument order of "OVERLAP" relation, if the order is flipped
			//find duplicates in gold relations:
			//			Collection<BinaryTextRelation> duplicateGoldRelations = getDuplicateRelations(goldRelations, getSpan);
			//			if(!duplicateGoldRelations.isEmpty()){
			//				System.err.println("******Duplicate gold relations in : " + ViewUriUtil.getURI(jCas).toString());
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
				for (HashableArguments key : sorted) {
					BinaryTextRelation goldRelation = goldMap.get(key);
					BinaryTextRelation systemRelation = systemMap.get(key);
					if (goldRelation == null) {
						System.out.println("System added: " + formatRelation(systemRelation));
					} else if (systemRelation == null) {
						System.out.println("System dropped: " + formatRelation(goldRelation));
					} else if (!systemRelation.getCategory().equals(goldRelation.getCategory())) {
						String label = systemRelation.getCategory();
						System.out.printf("System labeled %s for %s\n", label, formatRelation(goldRelation));
					} else{
						System.out.println("Nailed it! " + formatRelation(systemRelation));
					}
				}
			}
		}
		return stats;
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

//		private static <SPAN_TYPE> Collection<BinaryTextRelation> removeNonGoldRelations(
//				Collection<BinaryTextRelation> systemRelations,
//				Collection<BinaryTextRelation> goldRelations, Function<BinaryTextRelation, ?> getSpan) {
//			//remove non-gold pairs from system relations:
//			Set<BinaryTextRelation> goodSys = Sets.newHashSet();
//			Set<SPAN_TYPE> goldspans = new HashSet<SPAN_TYPE>();
//			
//			for (BinaryTextRelation relation : goldRelations) {
//				goldspans.add(((SPAN_TYPE) getSpan.apply(relation)));			
//			}
//			
//			for (BinaryTextRelation relation : systemRelations) {
//				if (goldspans.contains(((SPAN_TYPE) getSpan.apply(relation)))) {
//					goodSys.add(relation);
//				}
//			}
//			
//			return goodSys;
//		}
	
//	private static boolean matchSpan(Annotation arg1, Annotation arg2) {
//		boolean result = false;
//		result = arg1.getBegin() == arg2.getBegin() && arg1.getEnd() == arg2.getEnd();
//		return result;
//	}
	
//	protected static Collection<BinaryTextRelation> correctArgOrder(
//			Collection<BinaryTextRelation> systemRelations,
//			Collection<BinaryTextRelation> goldRelations) {
//		Set<BinaryTextRelation> goodSys = Sets.newHashSet();
//
//		for(BinaryTextRelation sysrel : systemRelations){
//			Annotation sysArg1 = sysrel.getArg1().getArgument();
//			Annotation sysArg2 = sysrel.getArg2().getArgument();
//			for(BinaryTextRelation goldrel : goldRelations){
//				Annotation goldArg1 = goldrel.getArg1().getArgument();
//				Annotation goldArg2 = goldrel.getArg2().getArgument();
//				if (matchSpan(sysArg2, goldArg1) && matchSpan(sysArg1, goldArg2)){//the order of system pair was flipped 
//					if(sysrel.getCategory().equals("OVERLAP")){ //if the relation is overlap, and the arg order was flipped, then change back the order
//						RelationArgument tempArg = (RelationArgument) sysrel.getArg1().clone();
//						sysrel.setArg1((RelationArgument) sysrel.getArg2().clone());
//						sysrel.setArg2(tempArg);
//					}//for other types of relation, still maintain the type.
//					continue;
//				}
//			}
//			goodSys.add(sysrel);
//		}
//
//		return goodSys;
//	}


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

//	private static Collection<BinaryTextRelation> correctArgOrder(
//			Collection<BinaryTextRelation> systemRelations,
//			Collection<BinaryTextRelation> goldRelations) {
//		Set<BinaryTextRelation> goodSys = Sets.newHashSet();
//
//		for(BinaryTextRelation sysrel : systemRelations){
//			Annotation sysArg1 = sysrel.getArg1().getArgument();
//			Annotation sysArg2 = sysrel.getArg2().getArgument();
//			for(BinaryTextRelation goldrel : goldRelations){
//				Annotation goldArg1 = goldrel.getArg1().getArgument();
//				Annotation goldArg2 = goldrel.getArg2().getArgument();
//				if (matchSpan(sysArg2, goldArg1) && matchSpan(sysArg1, goldArg2)){//the order of system pair was flipped 
//					if(sysrel.getCategory().equals("OVERLAP")){ //if the relation is overlap, and the arg order was flipped, then change back the order
//						RelationArgument tempArg = (RelationArgument) sysrel.getArg1().clone();
//						sysrel.setArg1((RelationArgument) sysrel.getArg2().clone());
//						sysrel.setArg2(tempArg);
//					}//for other types of relation, still maintain the type.
//					continue;
//				}
//			}
//			goodSys.add(sysrel);
//		}
//
//		return goodSys;
//	}


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

/**
	public static class RemoveEventEventRelations extends JCasAnnotator_ImplBase {
		public static final String PARAM_RELATION_VIEW = "RelationView";
		@ConfigurationParameter(name = PARAM_RELATION_VIEW)
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
	 */

	public static class RemoveNullArgumentRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(jCas, BinaryTextRelation.class))){
				if(relation.getArg1() == null || relation.getArg2() == null){
					relation.getArg1().removeFromIndexes();
					relation.getArg2().removeFromIndexes();
					relation.removeFromIndexes();
				}

			}

		}
	}

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
	/**
	public static class RemoveCrossSentenceRelations extends JCasAnnotator_ImplBase {

		public static final String PARAM_SENTENCE_VIEW = "SentenceView";

		@ConfigurationParameter(name = PARAM_SENTENCE_VIEW)
		private String sentenceViewName = CAS.NAME_DEFAULT_SOFA;

		public static final String PARAM_RELATION_VIEW = "RelationView";

		@ConfigurationParameter(name = PARAM_RELATION_VIEW)
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
	 */

	
	public static class RemoveRelations extends JCasAnnotator_ImplBase {
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
			name = "Reverse Overlap TLinker",
			description = "Adds Overlap temporal relations with arguments flipped.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
	)
	public static class AddFlippedOverlap extends JCasAnnotator_ImplBase {

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

	public static class FindLongDisRelations extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {

			try{
				JCas goldView = jCas.getView(GOLD_VIEW_NAME);
				JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
				File outf = new File("target/eval/temporal-relations/RelationDistance.txt");
				File outI = new File("target/eval/temporal-relations/LongRelationInstances.txt");
				File outA = new File("target/eval/temporal-relations/AdjacentRelationInstances.txt");
				BufferedWriter output   = new BufferedWriter(new FileWriter(outf, true));
				PrintWriter outIns = new PrintWriter(new BufferedWriter(new FileWriter(outI, true)));
				PrintWriter outAdj = new PrintWriter(new BufferedWriter(new FileWriter(outA, true)));

				Set<TemporalTextRelation> temporalRelations = Sets.newHashSet();
				for (TemporalTextRelation relation : JCasUtil.select(goldView, TemporalTextRelation.class)) {
					temporalRelations.add(relation);
				}

				for (BinaryTextRelation orelation : temporalRelations) {
					Annotation argA = orelation.getArg1().getArgument();
					Annotation argB = orelation.getArg2().getArgument();
					//suppose arg1 is before arg2
					int begin = argA==null? 1000000 : argA.getEnd();
					int end   = argB==null? 0: argB.getBegin();

					if(begin < end){
						int sentencesInBetween = 0;
						List<Sentence> sentences = JCasUtil.selectCovered(systemView, Sentence.class, begin, end);

						sentencesInBetween = sentences==null? 0: sentences.size();

						boolean adjacentSent = false; 						
						if(sentencesInBetween == 0){//differentiate relations in adjacent sentences
							//find the sentence after argA
							List<Sentence> follwingSentences = JCasUtil.selectFollowing(systemView, Sentence.class, argA, 1);
							if(follwingSentences !=null && follwingSentences.size()>=1){
								Sentence nextSent = follwingSentences.get(0);
								if( nextSent.getBegin()>end){
									sentencesInBetween =-1;//find within-sentence relations
								}else{//find adjacent-sentence relations
									adjacentSent = true;
								}
							}else{//if there is no following sentence
								List<Sentence> prededingSentences = JCasUtil.selectPreceding(systemView, Sentence.class, argB, 1);
								if(prededingSentences==null || prededingSentences.size()==0){
									sentencesInBetween = -1;
								}else{
									Sentence preSent = prededingSentences.get(0);
									if( preSent.getEnd()< begin){
										sentencesInBetween =-1;//find within-sentence relations
									}else{//find adjacent-sentence relations
										adjacentSent = true;
									}
								}
							}
						}

						String text = systemView.getDocumentText();
						int windowBegin = Math.max(0, argA.getBegin() - 50);
						int windowEnd = Math.min(text.length(), argB.getEnd() + 50);
						if (adjacentSent){
							outAdj.println("++++++++++Adjacent X-sentence Relation in "+ ViewUriUtil.getURI(jCas).toString() + "+++++++");
							outAdj.println(text.substring(windowBegin,argA.getBegin())+"["+argA.getCoveredText()+"] "+ text.substring(argA.getEnd(), argB.getBegin()) +" ["+argB.getCoveredText()+"]"+text.substring(argB.getEnd(),windowEnd));
						}

						output.append(sentencesInBetween+"\n");
						if (sentencesInBetween > 5){
							outIns.println("++++++++++Long Distance Relation in "+ ViewUriUtil.getURI(jCas).toString() + "+++++++");
							outIns.println(text.substring(windowBegin,argA.getBegin())+"["+argA.getCoveredText()+"] "+ text.substring(argA.getEnd(), argB.getBegin()) +" ["+argB.getCoveredText()+"]"+text.substring(argB.getEnd(),windowEnd));//"["+argA.getCoveredText()+"] "+ systemView.getDocumentText().substring(argA.getEnd(), argB.getBegin()) +" ["+argB.getCoveredText()+"]");
						}
					}
				}
				output.close();
				outIns.close();
				outAdj.close();
			}catch (IOException e) {
				//exception handling left as an exercise for the reader
			} catch (CASException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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

			String fileName = ViewUriUtil.getURI(jCas).toString();

			Multimap<List<Annotation>, BinaryTextRelation> annotationsToRelation = HashMultimap.create();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)){
				String relationType = relation.getCategory();
				if(validTemporalType(relationType)){
					Annotation arg1 = relation.getArg1().getArgument();
					Annotation arg2 = relation.getArg2().getArgument();
					if(arg1==null || arg2==null){
						System.out.println("Null argument at Doc: "+ fileName);
					}else{
						annotationsToRelation.put(Arrays.asList(arg1, arg2), relation);
					}
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
			if(relationType.equals("AFTER")||relationType.equals("OVERLAP")||relationType.equals("BEFORE"))
				return true;
			return false;
		}
	}
}
