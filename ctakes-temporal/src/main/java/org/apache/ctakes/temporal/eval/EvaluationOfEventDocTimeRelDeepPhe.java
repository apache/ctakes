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
import com.google.common.collect.Maps;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.eval.SHARPXMI;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.FileUtils;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.util.ViewUriUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class EvaluationOfEventDocTimeRelDeepPhe extends
Evaluation_ImplBase<Map<String, AnnotationStatistics<String>>>{
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
		public boolean getSkipWrite();
		
		@Option
		public boolean getTestOnTrain();
	}

	//	protected static ParameterSettings flatParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "linear",
	//			10.0, 1.0, "linear", ComboOperator.VECTOR_ONLY, DEFAULT_TK, DEFAULT_LAMBDA);
	//	protected static ParameterSettings allBagsParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk", 
	//			100.0, 0.1, "radial basis function", ComboOperator.SUM, 0.5, 0.5);
	//	protected static ParameterSettings ftParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk", 
	//			1.0, 0.1, "radial basis function", ComboOperator.SUM, 0.5, 0.5);
	//	private static Boolean recallModeEvaluation = true;
	protected static boolean DEFAULT_BOTH_DIRECTIONS = false;
	protected static float DEFAULT_DOWNSAMPLE = 1.0f;
	protected static ParameterSettings allParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk",
			0.0500, 1.0, "polynomial", ComboOperator.SUM, 0.1, 0.5);  // (0.3, 0.4 for tklibsvm)
	private static final String DOC_TIME_REL = "docTimeRel";
	private static final int DISCOVERTY_TYPE = 100;

	public static void main(String[] args) throws Exception {
		TempRelOptions options = CliFactory.parseArguments(TempRelOptions.class, args);
		List<Integer> trainItems = Arrays.asList(3, 11, 92, 93 );
		List<Integer> testItems = Arrays.asList(2, 21);

		//    possibleParams.add(defaultParams);
		

		ParameterSettings params = allParams;
		try{
			File workingDir = new File("target/eval/event-properties");
			if(!workingDir.exists()) workingDir.mkdirs();
			if(options.getUseTmp()){
				File tempModelDir = File.createTempFile("temporal", null, workingDir);
				tempModelDir.delete();
				tempModelDir.mkdir();
				workingDir = tempModelDir;
			}
			EvaluationOfEventDocTimeRelDeepPhe evaluation = new EvaluationOfEventDocTimeRelDeepPhe(
					workingDir,
					options.getRawTextDirectory(),
					options.getXMLDirectory(),
					options.getXMLFormat(),
					options.getSubcorpus(),
					options.getXMIDirectory(),
					options.getTreebankDirectory(),
					options.getClosure(),
					options.getPrintFormattedRelations(),
					options.getUseGoldAttributes(),
					params);
			//			evaluation.prepareXMIsFor(patientSets);
			List<Integer> training = trainItems;
			List<Integer> testing = testItems;

			evaluation.logClassificationErrors(workingDir, "deepPhe-event-property-errors");

			//do closure on system, but not on gold, to calculate recall
			evaluation.skipTrain = options.getSkipTrain();
			evaluation.skipWrite = options.getSkipWrite();
			if(!evaluation.skipTrain){
				evaluation.prepareXMIsFor(training);
			}
			evaluation.prepareXMIsFor(testing);

			Map<String, AnnotationStatistics<String>> stats = null;

			//test or train or test
			evaluation.testOnTrain = options.getTestOnTrain();
			if(evaluation.testOnTrain){
				stats = evaluation.trainAndTest(trainItems, trainItems);
			}else{//test on testing set
				stats = evaluation.trainAndTest(trainItems, testItems);//training
			}
			
			String name = DOC_TIME_REL;
			System.err.println("====================");
			System.err.println(name);
			System.err.println("--------------------");
			System.err.println(stats.get(name));


			if(options.getUseTmp()){
				// won't work because it's not empty. should we be concerned with this or is it responsibility of 
				// person invoking the tmp flag?
				FileUtils.deleteRecursive(workingDir);
			}
		}catch(ResourceInitializationException e){
			System.err.println("Error with Initialization");
			e.printStackTrace();
		}
	}

	private ParameterSettings params;
	protected boolean useClosure;
	protected boolean useGoldAttributes;
	protected boolean skipTrain=false;
	public boolean skipWrite = false;
	protected boolean testOnTrain=false;
	private Map<String, Logger> loggers = Maps.newHashMap();
	//  protected boolean printRelations = false;

	public EvaluationOfEventDocTimeRelDeepPhe(
			File baseDirectory,
			File rawTextDirectory,
			File xmlDirectory,
			XMLFormat xmlFormat,
			Subcorpus subcorpus,
			File xmiDirectory,
			File treebankDirectory,
			boolean useClosure,
			boolean printErrors,
			boolean useGoldAttributes,
			ParameterSettings params
			){
		super(
				baseDirectory,
				rawTextDirectory,
				xmlDirectory,
				xmlFormat,
				subcorpus,
				xmiDirectory,
				treebankDirectory);
		this.useClosure = useClosure;
		this.printErrors = printErrors;
		this.params = params;
		this.useGoldAttributes = useGoldAttributes;
		this.loggers.put(DOC_TIME_REL, Logger.getLogger(String.format("%s.%s", this.getClass().getName(), DOC_TIME_REL)));
	}

	@Override
	protected void train(CollectionReader collectionReader, File directory) throws Exception {
		if(this.skipTrain) return;

		if(!this.skipWrite){
			AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(KeepEventMentionsCoveredByGoldMentions.class));
			aggregateBuilder.add(DocTimeRelAnnotator.createDataWriterDescription(
					//				LibSvmStringOutcomeDataWriter.class,
					LibLinearStringOutcomeDataWriter.class,
					new File(directory, DOC_TIME_REL)));
			SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
		}
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

		//calculate class-wise weights:
		String[] weightArray=new String[8];
		weightArray[0] = "-c";
		weightArray[1] = optArray[1];
		weightArray[2] = "-s";
		weightArray[3] = "7";
		weightArray[4] = "-B";
		weightArray[5] = "0.25";
		weightArray[6] = "-e";
		weightArray[7] = "1.0";
		JarClassifierBuilder.trainAndPackage(new File(directory, DOC_TIME_REL),weightArray);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Map<String, AnnotationStatistics<String>> test(CollectionReader collectionReader, File directory)
			throws Exception {
		this.useClosure=false;//don't do closure for test
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
		//		aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class));
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ReplaceCTakesMentionsWithGoldMentions.class));

		//		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ClearEventProperties.class));

		aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription(new File(directory, DOC_TIME_REL)));

		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyHeadEventDocTimeRel2GoldEvent.class));

		Function<EventMention, ?> eventMentionToSpan = AnnotationStatistics.annotationToSpan();
		Map<String, Function<EventMention, String>> propertyGetters;
		propertyGetters = new HashMap<>();
		propertyGetters.put(DOC_TIME_REL, getPropertyGetter(DOC_TIME_REL));

		Map<String, AnnotationStatistics<String>> statsMap = new HashMap<>();

		statsMap.put(DOC_TIME_REL, new AnnotationStatistics<String>());

		for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();){
			JCas jCas = casIter.next();
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			String text = goldView.getDocumentText();

			List<EventMention> goldEvents = new ArrayList<>(JCasUtil.select(goldView, EventMention.class));
			List<EventMention> systemEvents = new ArrayList<>(JCasUtil.select(systemView, EventMention.class));
			String name = DOC_TIME_REL;
			this.loggers.get(name).fine("Errors in : " + ViewUriUtil.getURI(jCas).toString());
			Function<EventMention, String> getProperty = propertyGetters.get(name);
			statsMap.get(name).add(
					goldEvents,
					systemEvents,
					eventMentionToSpan,
					getProperty);
			for (int i = 0; i < goldEvents.size(); ++i) {
				String goldOutcome = getProperty.apply(goldEvents.get(i));
				String systemOutcome = getProperty.apply(systemEvents.get(i));
				EventMention event = goldEvents.get(i);
				int begin = event.getBegin();
				int end = event.getEnd();
				int windowBegin = Math.max(0, begin - 100);
				int windowEnd = Math.min(text.length(), end + 100);
				if (!goldOutcome.equals(systemOutcome)) {
					this.loggers.get(name).fine(String.format(
							"%s was %s but should be %s, in  ...%s[!%s!:%d-%d]%s...",
							name,
							systemOutcome,
							goldOutcome,
							text.substring(windowBegin, begin).replaceAll("[\r\n]", " "),
							text.substring(begin, end),
							begin,
							end,
							text.substring(end, windowEnd).replaceAll("[\r\n]", " ")));
				}else{//if gold outcome equals system outcome
					this.loggers.get(name).fine(String.format(
							"%s was correctly labeled as %s, in  ...%s[!%s!:%d-%d]%s...",
							name,
							goldOutcome,
							text.substring(windowBegin, begin).replaceAll("[\r\n]", " "),
							text.substring(begin, end),
							begin,
							end,
							text.substring(end, windowEnd).replaceAll("[\r\n]", " ")));
				}
			}


		}
		return statsMap;
	}

	public void logClassificationErrors(File outputDir, String outputFilePrefix) throws IOException {
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		String name = DOC_TIME_REL;
		Logger logger = this.loggers.get(name);
		logger.setLevel(Level.FINE);
		File outputFile = new File(outputDir, String.format("%s.%s.log", outputFilePrefix, name));
		FileHandler handler = new FileHandler(outputFile.getPath());
		handler.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				return record.getMessage() + '\n';
			}
		});
		logger.addHandler(handler);

	}

	private static Function<EventMention, String> getPropertyGetter(final String propertyName) {
		return new Function<EventMention, String>() {
			@Override
			public String apply(EventMention eventMention) {
				EventProperties eventProperties = eventMention.getEvent().getProperties();
				Feature feature = eventProperties.getType().getFeatureByBaseName(propertyName);
				return eventProperties.getFeatureValueAsString(feature);
			}
		};
	}

	@PipeBitInfo(
			name = "Event Property Clearer",
			description = "Clears all event properties.",
			role = PipeBitInfo.Role.SPECIAL
	)
	public static class ClearEventProperties extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (EventProperties eventProperties : JCasUtil.select(jCas, EventProperties.class)) {
				eventProperties.setAspect(null);
				eventProperties.setCategory(null);
				eventProperties.setContextualAspect(null);
				eventProperties.setContextualModality(null);
				eventProperties.setDegree(null);
				eventProperties.setDocTimeRel(null);
				eventProperties.setPermanence(null);
				eventProperties.setPolarity(0);
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
			//			List<IdentifiedAnnotation> cTakesMentions = new ArrayList<>();
			//			cTakesMentions.addAll(JCasUtil.select(systemView, EventMention.class));
			//			for (IdentifiedAnnotation cTakesMention : cTakesMentions) {
			//				cTakesMention.removeFromIndexes();
			//			}

			// copy gold Mentions and Modifiers to the system view
			List<EventMention> goldMentions = new ArrayList<>();
			goldMentions.addAll(JCasUtil.select(goldView, EventMention.class));
			CasCopier copier = new CasCopier(goldView.getCas(), systemView.getCas());
			for (EventMention goldMention : goldMentions) {
				EventMention copy = (EventMention) copier.copyFs(goldMention);
				Feature sofaFeature = copy.getType().getFeatureByBaseName("sofa");
				copy.setFeatureValue(sofaFeature, systemView.getSofa());
				copy.setDiscoveryTechnique(DISCOVERTY_TYPE);//mark copied events
				copy.addToIndexes();
			}
		}
	}

	public static class KeepEventMentionsCoveredByGoldMentions extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView(SHARPXMI.GOLD_VIEW_NAME);
				systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			// copy gold events to the system view
			List<EventMention> goldMentions = new ArrayList<>();
			goldMentions.addAll(JCasUtil.select(goldView, EventMention.class));
			CasCopier copier = new CasCopier(goldView.getCas(), systemView.getCas());
			for (EventMention goldMention : goldMentions) {
				//find system Event that is covered by goldEvent:
				boolean findCoveredSystemEvent = false;
				for(EventMention sysEvent: JCasUtil.selectCovered(systemView, EventMention.class, goldMention.getBegin(), goldMention.getEnd())){
					String goldDocTimeRel = goldMention.getEvent().getProperties().getDocTimeRel();
					sysEvent.setDiscoveryTechnique(DISCOVERTY_TYPE);//mark copied events
					findCoveredSystemEvent = true;
					if(sysEvent.getEvent()==null){
						Event event = new Event(systemView);
						EventProperties props = new EventProperties(systemView);
						props.setDocTimeRel(goldDocTimeRel);
						event.setProperties(props);
						sysEvent.setEvent(event);
					}else{
						sysEvent.getEvent().getProperties().setDocTimeRel(goldDocTimeRel);
					}
				}

				if( !findCoveredSystemEvent ){// if we didn't find covered system event for the given gold event
					EventMention copy = (EventMention) copier.copyFs(goldMention);
					Feature sofaFeature = copy.getType().getFeatureByBaseName("sofa");
					copy.setFeatureValue(sofaFeature, systemView.getSofa());
					copy.setDiscoveryTechnique(DISCOVERTY_TYPE);//mark copied events
					copy.addToIndexes();
				}
			}

			//remove non-gold events:
			List<EventMention> cTakesMentions = new ArrayList<>();
			cTakesMentions.addAll(JCasUtil.select(systemView, EventMention.class));
			for (EventMention aEvent: cTakesMentions){
				if( aEvent.getDiscoveryTechnique() != DISCOVERTY_TYPE){//if this is not an gold event
					aEvent.removeFromIndexes();
				}
			}
		}
	}

	/**
	 * copy covered event's DocTimeRel to the gold event
	 * remove non-gold eventMentions
	 */
	@PipeBitInfo(
			name = "DocTimeRel to Gold Copier",
			description = "Copies an Event's DocTimeRel from the System view to the Gold view.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.EVENT }
	)
	public static class CopyHeadEventDocTimeRel2GoldEvent extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas systemView;
			try {
				systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException(e);
			}

			//build an eventMention-eventMention covered map
			Map<EventMention, Collection<EventMention>> coveredMap =
					JCasUtil.indexCovered(jCas, EventMention.class, EventMention.class);

			// copy covered event's DocTimeRel to the gold event
			for (EventMention aEvent: JCasUtil.select(systemView, EventMention.class)){
				if( aEvent.getDiscoveryTechnique()== DISCOVERTY_TYPE){//if this is an gold event
					for(EventMention coveredEvent: coveredMap.get(aEvent)){
						String covDocTimeRel = coveredEvent.getEvent().getProperties().getDocTimeRel();
						aEvent.getEvent().getProperties().setDocTimeRel(covDocTimeRel);
						break;
					}				
				}
			}


			List<EventMention> cTakesMentions = new ArrayList<>();
			cTakesMentions.addAll(JCasUtil.select(systemView, EventMention.class));
			for (EventMention aEvent: cTakesMentions){
				if( aEvent.getDiscoveryTechnique() != DISCOVERTY_TYPE){//if this is not an gold event
					aEvent.removeFromIndexes();
				}
			}
		}
	}
}
