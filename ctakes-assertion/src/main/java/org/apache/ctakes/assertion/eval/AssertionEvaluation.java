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
package org.apache.ctakes.assertion.eval;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.ctakes.assertion.attributes.features.selection.FeatureSelection;
import org.apache.ctakes.assertion.medfacts.ConceptConverterAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.*;
import org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine.FEATURE_CONFIG;
import org.apache.ctakes.assertion.pipelines.GoldEntityAndAttributeReaderPipelineForSeedCorpus;
import org.apache.ctakes.core.ae.DocumentIdPrinterAnalysisEngine;
import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Modifier;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.factory.*;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.FileUtils;
import org.cleartk.eval.Evaluation_ImplBase;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.transform.InstanceDataWriter;
import org.cleartk.ml.feature.transform.InstanceStream;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class AssertionEvaluation extends Evaluation_ImplBase<File, Map<String, AnnotationStatisticsCompact<String>>> {
  
private static final Logger logger = Logger.getLogger( AssertionEvaluation.class );

  private static final String YTEX_NEGATION_DESCRIPTOR = "ytex.uima.NegexAnnotator";

  enum Corpus {SHARP_SEED, SHARP_STRATIFIED, MIPACQ, I2B2, NEGEX}

  public static class Options {
    @Option(
        name = "--train-dir",
        usage = "specify the directory containing the XMI training files (for example, /NLP/Corpus/Relations/mipacq/xmi/train)",
        required = false)
    public String trainDirectory;
    
    @Option(
        name = "--test-dir",
        usage = "specify the directory containing the XMI testing files (for example, /NLP/Corpus/Relations/mipacq/xmi/test)",
        required = false)
    public File testDirectory;
    
    @Option(
            name = "--dev-dir",
            usage = "if running --preprocess, store the XMI development files here",
            required = false)
        public File devDirectory;

    @Option(
        name = "--models-dir",
        usage = "specify the directory where the models will be placed",
        required = false)
    public File modelsDirectory;
    
    @Option(
            name = "--evaluation-output-dir",
            usage = "specify the directory where the evaluation output xmi files will go",
            required = false)
    public File evaluationOutputDirectory;
        
    @Option(
            name = "--ignore-polarity",
            usage = "specify whether polarity processing should be ignored (true or false). default: false",
            required = false)
    public boolean ignorePolarity = false; // note that this is reversed from the "ignore" statement
        
    @Option(
            name = "--ignore-conditional",
            usage = "specify whether conditional processing should be ignored (true or false). default: false",
            required = false)
    public boolean ignoreConditional = false;
        
    @Option(
            name = "--ignore-uncertainty",
            usage = "specify whether uncertainty processing should be ignored (true or false). default: false",
            required = false)
    public boolean ignoreUncertainty = false;
        
    @Option(
            name = "--ignore-subject",
            usage = "specify whether subject processing should be ignored (true or false). default: false",
            required = false,
            handler=BooleanOptionHandler.class)
    public boolean ignoreSubject = false;
        
    @Option(
            name = "--ignore-generic",
            usage = "specify whether generic processing should be ignored (true or false). default: false",
            required = false)
    public boolean ignoreGeneric = false;
        
    // srh adding 2/20/13
    @Option(
            name = "--ignore-history",
            usage = "specify whether 'history of' processing should be run (true or false). default: false",
            required = false)
    public boolean ignoreHistory = false;
        
    @Option(
            name = "--cross-validation",
            usage = "ignore the test set and run n-fold cross-validation. default: n=2",
            required = false)
    public Integer crossValidationFolds;
    
    @Option(
            name = "--train-only",
            usage = "do not test a model, build one from xmi output and store in --models-dir",
            required = false)
    public boolean trainOnly = false;

    @Option(
            name = "--test-only",
            usage = "do not train a model, use the one specified in --models-dir",
            required = false)
    public boolean testOnly = false;

    @Option(
            name = "--preprocess-only",
            usage = "run preprocessing pipeline on a SHARP-style corpus, specify root directory",
            required = false)
    public File preprocessDir;

    @Option(
            name = "--no-cleartk",
            usage = "run the version of the assertion module released with cTAKES 2.5",
            required = false)
    public boolean noCleartk = false;
    
    @Option(
    		name = "--print-errors",
    		usage = "Flag to have test method print out error context for misclassified examples",
    		required = false)
    public boolean printErrors = false;

    @Option(
    		name = "--print-instances",
    		usage = "Flag to have test method print out lots of info for statistical significance testing",
    		required = false)
    public File printInstances;

    @Option(
    		name = "--eval-only",
    		usage = "Evaluate a CASes (supply the directory as an argument) with both system and gold in them.",
    		required = false)
    public boolean evalOnly;

    @Option(
    		name = "--ytex-negation",
    		usage = "Use the negation detection from ytex, which is based on a more recent NegEx than the original cTAKES used." +
    		" Note that using this requires adding the directory for YTEX_NEGATION_DESCRIPTOR to the classpath as well" +
    		" as the annotator class itself, since ytex is under a different license than Apache cTAKES.",
    		required = false)
    public boolean useYtexNegation;

    @Option(
    		name = "--feature-selection",
    		usage = "Takes an argument: the Chi^2 feature selection threshold",
    		required = false)
    public Float featureSelectionThreshold = null;

    @Option(
        name = "--kernel-params",
        usage = "Set of parameters to pass to kernel (libsvm)",
        required = false)
    public String kernelParams = null;
    
    @Option(
        name = "--use-tmp",
        usage = "Whether to put trained models into a temp directory (e.g., for a grid search)",
        required = false)
    public boolean useTmp = false;
    
    @Option(
        name = "--corpus",
        usage = "What corpus to read for pre-processing",
        required = false)
    public Corpus corpus = Corpus.SHARP_SEED;
    
    @Option(
        name = "--feats",
        usage = "What feature configuration to use",
        required = false)
    public FEATURE_CONFIG featConfig = FEATURE_CONFIG.ALL_SYN;

    @Option(
    		name = "--feda",
    		usage = "Domain adaptation -- for each semicolon-separated directory in train-dir, creates a domain-specific feature space",
    		required = false)
    public boolean feda = false;
    
    @Option(
        name = "--portion",
        usage = "Learning curve building -- what percentage of the training data to train on.",
        required = false)
    public double portionOfDataToUse = 1.0;
  }
  
  protected ArrayList<String> annotationTypes;

  private Class<? extends DataWriter<String>> dataWriterClass;
  
  private File evaluationOutputDirectory;

  static public String evaluationLogFilePath;
  static private File evaluationLogFile;
  static private BufferedWriter evaluationLogFileOut;
  static {
	  evaluationLogFilePath = "eval_"+new Date().toString().replaceAll(" ","_") + ".txt";
  }

  static public boolean useEvaluationLogFile = false;
  private boolean ignoreAnatomicalSites = false;

  protected static Options options = new Options();
  
  public static void main(String[] args) throws Exception {
	  
    System.out.println("Started assertion module at " + new Date());
    
	  resetOptions();
	  CmdLineParser parser = new CmdLineParser(options);
	  parser.parseArgument(args);

	  if (useEvaluationLogFile && evaluationLogFileOut == null) {
		  evaluationLogFile = new File(evaluationLogFilePath);
		  evaluationLogFileOut = new BufferedWriter(new FileWriter(evaluationLogFile), 32768);
	  }
	  
    printOptionsForDebugging(options);
    List<File> trainFiles = new ArrayList<>();
    if (null != options.trainDirectory) {
    	String[] dirs = options.trainDirectory.split("[;:]");
    	for (String dir : dirs) {
    		File trainDir = new File(dir);
    		if (trainDir.listFiles()!=null) {
    			for (File f : trainDir.listFiles()) {
    				trainFiles.add(f);
    			}
    		}
    	}
    }
    File modelsDir = options.modelsDirectory;
    if(options.useTmp){
      File tempModelDir = new File(options.modelsDirectory, "temp");
      tempModelDir.mkdirs();
      File curModelDir = File.createTempFile("assertion", null, tempModelDir);
      curModelDir.delete();
      curModelDir.mkdir();
      modelsDir = curModelDir;
    }
    
    File evaluationOutputDirectory = options.evaluationOutputDirectory;

    ArrayList<String> annotationTypes = new ArrayList<>();
    if (!options.ignorePolarity) { annotationTypes.add("polarity"); }
    if (!options.ignoreConditional) { annotationTypes.add("conditional"); }
    if (!options.ignoreUncertainty) { annotationTypes.add("uncertainty"); }
    if (!options.ignoreSubject) { annotationTypes.add("subject"); }
    if (!options.ignoreGeneric) { annotationTypes.add("generic"); }
    if (!options.ignoreHistory) { annotationTypes.add("historyOf"); }
    
    String[] kernelParams = null;
    if(options.kernelParams != null){
      kernelParams = options.kernelParams.split("\\s+");
    }else{
      kernelParams = new String[]{"-c", "1.0"};
    }
    Class<? extends DataWriter<String>> dw = null;
    if(options.featConfig == FEATURE_CONFIG.STK || options.featConfig == FEATURE_CONFIG.PTK){ 
//        dw = TKLibSvmStringOutcomeDataWriter.class;
      throw new UnsupportedOperationException("This requires cleartk-2.0 which");
    }
    dw = LibLinearStringOutcomeDataWriter.class;
    
    AssertionEvaluation evaluation = new AssertionEvaluation(
        modelsDir,
        evaluationOutputDirectory,
        annotationTypes,
        dw,
        kernelParams
        );
    
    // if preprocessing, don't do anything else
    if(options.preprocessDir!=null ) {
    	preprocess(options.preprocessDir);
    }
    
    // run cross-validation
    else if(options.crossValidationFolds != null) {
      // run n-fold cross-validation
      List<Map<String, AnnotationStatisticsCompact<String>>> foldStats = evaluation.crossValidation(trainFiles, options.crossValidationFolds);
      //AnnotationStatisticsCompact overallStats = AnnotationStatisticsCompact.addAll(foldStats);
      Map<String, AnnotationStatisticsCompact<String>> overallStats = new TreeMap<>();
      
      for (String currentAnnotationType : annotationTypes)
      {
    	  AnnotationStatisticsCompact<String> currentAnnotationStatisticsCompact = new AnnotationStatisticsCompact<>();
    	  overallStats.put(currentAnnotationType, currentAnnotationStatisticsCompact);
      }
      for (Map<String, AnnotationStatisticsCompact<String>> singleFoldMap : foldStats)
      {
    	  for (String currentAnnotationType : annotationTypes)
    	  {
    	    AnnotationStatisticsCompact<String> currentFoldStatistics = singleFoldMap.get(currentAnnotationType);
    	    overallStats.get(currentAnnotationType).addAll(currentFoldStatistics);
    	  }
      }
      
      AssertionEvaluation.printScore(overallStats,  "CROSS FOLD OVERALL");
      
    } 
    else if (Math.abs(options.portionOfDataToUse - 1.0) > 0.001){
      int numIters = 5;
      List<File> testFiles = Arrays.asList(options.testDirectory.listFiles());
      Map<String, Double> overallStats = new TreeMap<>();

      for(String annotationType : annotationTypes){
        overallStats.put(annotationType, 0.0);
      }
      for(int iter = 0; iter < numIters; iter++){
        Map<String,AnnotationStatisticsCompact<String>> stats = evaluation.trainAndTest(trainFiles, testFiles);
        AssertionEvaluation.printScore(stats, "Sample " + iter + " score:");
        for(String annotationType : stats.keySet()){
          overallStats.put(annotationType, overallStats.get(annotationType) + stats.get(annotationType).f1("-1"));
        }
      }
      for(String annotationType : annotationTypes){
        System.out.println("Macro-average F-score for " + annotationType + " is: " + (overallStats.get(annotationType) / numIters));
      }
//      AssertionEvaluation.printScore(overallStats, "Learning Curve Proportion Average");
    }
    
    // run train and test
    else {
      // train on the entire training set and evaluate on the test set
      List<File> testFiles;
      if (options.evalOnly) {
    	  testFiles = Arrays.asList(options.evaluationOutputDirectory.listFiles());
    	  logger.debug("evalOnly using files in directory " + evaluationOutputDirectory.getName() + " aka " + evaluationOutputDirectory.getCanonicalPath());
      } else if (options.trainOnly){
    	  testFiles = new ArrayList<>();
      } else {
    	  testFiles = Arrays.asList(options.testDirectory.listFiles());
      }
      
      if (!options.testOnly && !options.evalOnly) {
    	  CollectionReader trainCollectionReader = evaluation.getCollectionReader(trainFiles);
    	  evaluation.train(trainCollectionReader, modelsDir);
      }
      
      // run testing
      if (!options.trainOnly) {
    	  if (testFiles==null || testFiles.size()==0) {
    		  throw new RuntimeException("testFiles = " + testFiles + " testFiles.size() = " + (testFiles==null ? "null": testFiles.size())) ;
    	  }
    	  logger.debug("testFiles.size() = " + testFiles.size());
    	  CollectionReader testCollectionReader = evaluation.getCollectionReader(testFiles);
    	  Map<String, AnnotationStatisticsCompact<String>> stats = evaluation.test(testCollectionReader, modelsDir);

    	  AssertionEvaluation.printScore(stats,  modelsDir!=null? modelsDir.getAbsolutePath() : "no_model");
      }
    }
    if(options.useTmp && modelsDir != null){
      FileUtils.deleteRecursive(modelsDir);
    }
    System.out.println("Finished assertion module at " + new Date());
    
  }
  
  private static void resetOptions() {
	  options.ignoreConditional = false;
	  options.ignoreGeneric = false;
	  options.ignoreHistory = false;
	  options.ignorePolarity = false;
	  options.ignoreSubject = false;
	  options.ignoreUncertainty = false;
	  
	  options.trainOnly = false;
	  options.testOnly = false;
	  options.noCleartk = false;
	  options.printErrors = false;
	  options.printInstances = null;
	  options.evalOnly = false;
	  
	  options.evaluationOutputDirectory = null;
	  options.trainDirectory = null;
	  options.testDirectory = null;
	  options.devDirectory = null;
	  options.modelsDirectory = null;
	  options.preprocessDir = null;
	  
	  options.crossValidationFolds = null;
  }

private static void printOptionsForDebugging(Options optionsArg)
  {
	String message;
	message = String.format(
		"Printing options: %n" +
		"training dir: %s%n" +
	    "test dir: %s%n" + 
	    "model dir: %s%n" +
	    "preprocess dir: %s%n" +
	    "evaluation output dir: %s%n" +
	    "cross-validation: %d%n" +
	    "ignore polarity: %b%n" +
	    "ignore conditional: %b%n" +
	    "ignore uncertainty: %b%n" +
	    "ignore subject: %b%n" +
	    "ignore generic: %b%n" +
	    "ignore history: %b%n" +
	    "train only: %b%n" +
	    "test only: %b%n" +
	    "eval only: %b%n" +
	    //"crossValidationFolds: %s%n" +
	    "noCleartk: %b%n" +
	    "%n%n",
	    optionsArg.trainDirectory, // just a String so no need to check for null because not using getAbsolutePath()
   	    (optionsArg.testDirectory != null) ? optionsArg.testDirectory.getAbsolutePath() : "",
	    (optionsArg.modelsDirectory!=null) ? optionsArg.modelsDirectory.getAbsolutePath() : "",
   		(optionsArg.preprocessDir!=null) ? optionsArg.preprocessDir.getAbsolutePath() : "",
	    (optionsArg.evaluationOutputDirectory!=null) ? optionsArg.evaluationOutputDirectory.getAbsolutePath() : "",
	    optionsArg.crossValidationFolds,
	    optionsArg.ignorePolarity,
	    optionsArg.ignoreConditional,
	    optionsArg.ignoreUncertainty,
	    optionsArg.ignoreSubject,
	    optionsArg.ignoreGeneric,
	    optionsArg.ignoreHistory,
		optionsArg.trainOnly,
		optionsArg.testOnly,
		optionsArg.evalOnly,
		//(options.crossValidationFolds != null) ? options.crossValidationFolds.intValue()+"" : "",
		optionsArg.noCleartk
	    );
	logger.info(message);
  }

public static void printScore(Map<String, AnnotationStatisticsCompact<String>> map, String directory)
  {
      for (Map.Entry<String, AnnotationStatisticsCompact<String>> currentEntry : map.entrySet())
	  {
    	  String annotationType = currentEntry.getKey();
    	  AnnotationStatisticsCompact<String> stats = currentEntry.getValue();
    	  
    	  System.out.format("directory: \"%s\"; assertion type: %s%n%s%n%s%n%n",
    	    directory,
    	    annotationType.toUpperCase(),
    	    options.testDirectory,
    	    stats.toString());
    	  
    	  try {
    		  if (useEvaluationLogFile) {
    			  evaluationLogFileOut.write(
    					  String.format("%s\t%f\t%s\t%s\t%s",
    							  annotationType,
    							  options.featureSelectionThreshold,
    							  options.modelsDirectory.getName(),
    							  options.testDirectory.toString(),
    							  stats.toTsv())
    					  );
    			  evaluationLogFileOut.flush();
    		  }
		} catch (IOException e) {
			e.printStackTrace();
		}
	  }
  }

  private final String[] trainingArguments;

  public AssertionEvaluation(
      File modelDirectory,
      File evaluationOutputDirectory,
      ArrayList<String> annotationTypes,
      Class<? extends DataWriter<String>> dataWriterClass,
      String... trainingArguments
      ) {
    super(modelDirectory);
    
    this.annotationTypes = annotationTypes;

    this.dataWriterClass = dataWriterClass;

    this.trainingArguments = trainingArguments;
    this.evaluationOutputDirectory = evaluationOutputDirectory;
  }

  @Override
  public CollectionReader getCollectionReader(List<File> items)
      throws ResourceInitializationException {
    String[] paths = new String[items.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    return CollectionReaderFactory.createReader(
        XMIReader.class,
        TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(),
        XMIReader.PARAM_FILES,
        paths);
  }

  public static void preprocess(File rawDir ) throws ResourceInitializationException, UIMAException, IOException {
	  File preprocessedDir = null;
	  if (options.trainDirectory.split("[;]").length>1) {
		  throw new IOException("Assertion preprocess wants to write to one train directory, but you've supplied multiple: " + options.trainDirectory);
	  }
		preprocessedDir = new File(options.trainDirectory);
	  
	  if(options.corpus == Corpus.I2B2){
		  GoldEntityAndAttributeReaderPipelineForSeedCorpus.readI2B2Challenge2010(rawDir, preprocessedDir);
	  }else if(options.corpus == Corpus.MIPACQ){
		  GoldEntityAndAttributeReaderPipelineForSeedCorpus.readMiPACQ(rawDir, preprocessedDir, options.testDirectory, options.devDirectory);
	  }else if(options.corpus == Corpus.NEGEX){
		  GoldEntityAndAttributeReaderPipelineForSeedCorpus.readNegexTestSet(rawDir, preprocessedDir);
	  }else if(options.corpus == Corpus.SHARP_STRATIFIED){
	    GoldEntityAndAttributeReaderPipelineForSeedCorpus.readSharpStratifiedUmls(
	        rawDir, preprocessedDir, options.testDirectory, options.devDirectory);
	  } else if(options.corpus == Corpus.SHARP_SEED){
		  GoldEntityAndAttributeReaderPipelineForSeedCorpus.readSharpSeedUmls(
				  rawDir, preprocessedDir, options.testDirectory, options.devDirectory);
	  } else{
	    throw new ResourceInitializationException("No corpus type specified!", new Object[]{rawDir});
	  }
  }
  
  @Override
  public void train(CollectionReader collectionReader, File directory) throws Exception {
    if(options.noCleartk) return;
    AggregateBuilder builder = new AggregateBuilder();
    
    AnalysisEngineDescription goldCopierIdentifiedAnnotsAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceIdentifiedAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierIdentifiedAnnotsAnnotator);
    
    AnalysisEngineDescription goldCopierSupportingAnnotsAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceSupportingAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierSupportingAnnotsAnnotator);
    
    AnalysisEngineDescription assertionAttributeClearerAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceAnnotationsSystemAssertionClearer.class);
    builder.add(assertionAttributeClearerAnnotator);
    
    // Set up Feature Selection parameters
    Float featureSelectionThreshold = options.featureSelectionThreshold;
    Class<? extends DataWriter<String>> dataWriterClassFirstPass = getDataWriterClass(); 
    if (options.featureSelectionThreshold==null) {
    	featureSelectionThreshold = 0f;
    }
    
    // Add each assertion Analysis Engine to the pipeline!
    builder.add(AnalysisEngineFactory.createEngineDescription(AlternateCuePhraseAnnotator.class ) );
    
    if (!options.ignorePolarity)
    {
    	AnalysisEngineDescription polarityAnnotator;
    	if (options.useYtexNegation) {
    		 polarityAnnotator = AnalysisEngineFactory.createEngineDescription(YTEX_NEGATION_DESCRIPTOR);
    	} else {
    		if (options.feda) {
    			polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityFedaCleartkAnalysisEngine.class);

      			ConfigurationParameterFactory.addConfigurationParameters(
        				polarityAnnotator,
        				AssertionCleartkAnalysisEngine.FILE_TO_DOMAIN_MAP,
        				options.trainDirectory
        				);
    		} else {
    			// default: cleartk-based polarity, no domain adaptation
    			polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityCleartkAnalysisEngine.class); //,  this.additionalParamemters);
      		}
    		ConfigurationParameterFactory.addConfigurationParameters(
    				polarityAnnotator,
    				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
    				AssertionEvaluation.GOLD_VIEW_NAME,
//    				CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//    				this.dataWriterFactoryClass.getName(),
    				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
    				dataWriterClassFirstPass,
    				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
    				new File(directory, "polarity").getPath(),
    				AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_URI,
    				PolarityCleartkAnalysisEngine.createFeatureSelectionURI(new File(directory, "polarity")),
    				AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_THRESHOLD,
    				featureSelectionThreshold,
    				AssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
    				options.featConfig,
    				AssertionCleartkAnalysisEngine.PARAM_PORTION_OF_DATA_TO_USE,
    				(float) options.portionOfDataToUse
    				);
    	}
		builder.add(polarityAnnotator);
    }

    if (!options.ignoreConditional)
    {
	    AnalysisEngineDescription conditionalAnnotator = AnalysisEngineFactory.createEngineDescription(ConditionalCleartkAnalysisEngine.class); //,  this.additionalParamemters);
	    ConfigurationParameterFactory.addConfigurationParameters(
	        conditionalAnnotator,
	        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
	        AssertionEvaluation.GOLD_VIEW_NAME,
//	        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//	        this.dataWriterFactoryClass.getName(),
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
			dataWriterClassFirstPass,
	        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
	        new File(directory, "conditional").getPath(),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_URI,
			ConditionalCleartkAnalysisEngine.createFeatureSelectionURI(new File(directory, "conditional")),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_THRESHOLD,
			featureSelectionThreshold
	        );
	    builder.add(conditionalAnnotator);
    }

    if (!options.ignoreUncertainty)
    {
	    AnalysisEngineDescription uncertaintyAnnotator = AnalysisEngineFactory.createEngineDescription(UncertaintyCleartkAnalysisEngine.class); //,  this.additionalParamemters);
	    ConfigurationParameterFactory.addConfigurationParameters(
	        uncertaintyAnnotator,
	        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
	        AssertionEvaluation.GOLD_VIEW_NAME,
//	        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//	        this.dataWriterFactoryClass.getName(),
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
			dataWriterClassFirstPass,
	        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
	        new File(directory, "uncertainty").getPath(),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_URI,
			UncertaintyCleartkAnalysisEngine.createFeatureSelectionURI(new File(directory, "uncertainty")),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_THRESHOLD,
			featureSelectionThreshold,
      AssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
      options.featConfig
	        );
	    builder.add(uncertaintyAnnotator);
    }

    if (!options.ignoreSubject)
    {
	    AnalysisEngineDescription subjectAnnotator = AnalysisEngineFactory.createEngineDescription(SubjectCleartkAnalysisEngine.class); //,  this.additionalParamemters);
	    ConfigurationParameterFactory.addConfigurationParameters(
	        subjectAnnotator,
	        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
	        AssertionEvaluation.GOLD_VIEW_NAME,
//	        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//	        this.dataWriterFactoryClass.getName(),
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
			dataWriterClassFirstPass,
	        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
	        new File(directory, "subject").getPath(),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_URI,
			SubjectCleartkAnalysisEngine.createFeatureSelectionURI(new File(directory, "subject")),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_THRESHOLD,
			featureSelectionThreshold
	        );
	    builder.add(subjectAnnotator);
    }

    if (!options.ignoreGeneric)
    {
		AnalysisEngineDescription genericAnnotator = AnalysisEngineFactory.createEngineDescription(GenericCleartkAnalysisEngine.class); //,  this.additionalParamemters);
		ConfigurationParameterFactory.addConfigurationParameters(
		    genericAnnotator,
		    AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
		    AssertionEvaluation.GOLD_VIEW_NAME,
//		    CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//		    this.dataWriterFactoryClass.getName(),
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
			dataWriterClassFirstPass,
		    DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
		    new File(directory, "generic").getPath(),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_URI,
			GenericCleartkAnalysisEngine.createFeatureSelectionURI(new File(directory, "generic")),
			AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_THRESHOLD,
			featureSelectionThreshold
		    );
		builder.add(genericAnnotator);
    }
    
    // 2/20/13 srh adding
    if (!options.ignoreHistory) {
    	AnalysisEngineDescription historyAnnotator = AnalysisEngineFactory.createEngineDescription(HistoryCleartkAnalysisEngine.class);
    	ConfigurationParameterFactory.addConfigurationParameters(
    			historyAnnotator,
    			AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
    			AssertionEvaluation.GOLD_VIEW_NAME,
//    			CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//    			this.dataWriterFactoryClass.getName(),
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
			dataWriterClassFirstPass,
    			DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
    			new File(directory, "historyOf").getPath(),
				AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_URI,
				HistoryCleartkAnalysisEngine.createFeatureSelectionURI(new File(directory, "historyOf")),
				AssertionCleartkAnalysisEngine.PARAM_FEATURE_SELECTION_THRESHOLD,
				featureSelectionThreshold
    			);
    	builder.add(historyAnnotator);
    }

    SimplePipeline.runPipeline(collectionReader,  builder.createAggregateDescription());
    
    //HideOutput hider = new HideOutput();
    for (String currentAssertionAttribute : annotationTypes)
    {
    	File currentDirectory = new File(directory, currentAssertionAttribute);
    	trainAndPackage(currentAssertionAttribute, currentDirectory, trainingArguments);
    }
    //hider.restoreOutput();
  }

  @Override
  protected Map<String, AnnotationStatisticsCompact<String>> test(CollectionReader collectionReader, File directory)
      throws Exception {
    AggregateBuilder builder = new AggregateBuilder();
    
    AnalysisEngineDescription goldCopierAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceIdentifiedAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierAnnotator);
    
    AnalysisEngineDescription assertionAttributeClearerAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceAnnotationsSystemAssertionClearer.class);
    builder.add(assertionAttributeClearerAnnotator);
    
    AnalysisEngineDescription documentIdPrinterAnnotator = AnalysisEngineFactory.createEngineDescription(DocumentIdPrinterAnalysisEngine.class);
    builder.add(documentIdPrinterAnnotator);

    if ( options.noCleartk ) {
    	addExternalAttributeAnnotatorsToAggregate(builder);
    } else {
    	addCleartkAttributeAnnotatorsToAggregate(directory, builder);
    }

    if (options.evalOnly && evaluationOutputDirectory != null) {
    	// short circuit any other stuff in the pipeline
    	builder = new AggregateBuilder();
    	
    	// uimafit find available type systems on classpath
    	TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription();
    	
        AnalysisEngineDescription noOp =
    		AnalysisEngineFactory.createEngineDescription(
	            NoOpAnnotator.class,
	            typeSystemDescription);
    	builder.add(noOp);
    	
        AnalysisEngineDescription mergeGold =
    		AnalysisEngineFactory.createEngineDescription(org.apache.ctakes.assertion.eval.MergeGoldViewFromOneCasIntoInitialViewOfAnotherCas.class, typeSystemDescription);
    	builder.add(mergeGold); 	
    }
    
    AnalysisEngine aggregate = builder.createAggregate();
    
    AnnotationStatisticsCompact<String> polarityStats = new AnnotationStatisticsCompact<>();
    AnnotationStatisticsCompact<String> conditionalStats = new AnnotationStatisticsCompact<>();
    AnnotationStatisticsCompact<String> uncertaintyStats = new AnnotationStatisticsCompact<>();
    AnnotationStatisticsCompact<String> subjectStats = new AnnotationStatisticsCompact<>();
    AnnotationStatisticsCompact<String> genericStats = new AnnotationStatisticsCompact<>();
    AnnotationStatisticsCompact<String> historyStats = new AnnotationStatisticsCompact<>();	// srh 3/6/13
    
    Map<String, AnnotationStatisticsCompact<String>> map = new TreeMap<>(); 
    if (!options.ignorePolarity)
    {
      map.put("polarity",  polarityStats);
    }

    if (!options.ignoreConditional)
    {
      map.put("conditional",  conditionalStats);
    }

    if (!options.ignoreUncertainty)
    {
      map.put("uncertainty",  uncertaintyStats);
    }

    if (!options.ignoreSubject)
    {
      map.put("subject", subjectStats);
    }

    if (!options.ignoreGeneric)
    {
      map.put("generic", genericStats);
    }
    
    // srh 3/6/13
    if (!options.ignoreHistory)
    {
    	map.put("historyOf", historyStats);
    }

    // run on existing output that has both system (or instance gathering) and gold
    for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregate); casIter.hasNext();) {
      JCas jCas = casIter.next();
    	
      JCas goldView;
      try {
        goldView = jCas.getView(GOLD_VIEW_NAME);
      } catch (CASException e) {
    	logger.info("jCas.getViewName() = " + jCas.getViewName());
        throw new AnalysisEngineProcessException(e);
      }

      Collection<IdentifiedAnnotation> goldEntitiesAndEvents = new ArrayList<>(); 
      if ( !ignoreAnatomicalSites ) {
    	  Collection<EntityMention> goldEntities = JCasUtil.select(goldView, EntityMention.class);
    	  goldEntitiesAndEvents.addAll(goldEntities);
      }
      Collection<EventMention> goldEvents = JCasUtil.select(goldView, EventMention.class);
      goldEntitiesAndEvents.addAll(goldEvents);
      // System.out.format("gold entities: %d%ngold events: %d%n%n", goldEntities.size(), goldEvents.size());
      
      if (goldEntitiesAndEvents.size()==0) { 
    	  // gold annotations might have been read in as just IdentifiedAnnotation annotations
    	  // since no EventMentio or EntityMention annotations were found, ok to just try IdentifiedAnnotation
    	  // without concern for using some twice.
          Collection<IdentifiedAnnotation> identifiedAnnotations = JCasUtil.select(goldView, IdentifiedAnnotation.class);
          goldEntitiesAndEvents.addAll(identifiedAnnotations);
    	  
    	  
      }
      
      Collection<IdentifiedAnnotation> systemEntitiesAndEvents = new ArrayList<>();
      if ( !ignoreAnatomicalSites ) {
    	  Collection<EntityMention> systemEntities = JCasUtil.select(jCas, EntityMention.class);
    	  systemEntitiesAndEvents.addAll(systemEntities);
      }
      Collection<EventMention> systemEvents = JCasUtil.select(jCas, EventMention.class);
      systemEntitiesAndEvents.addAll(systemEvents);
//      System.out.format("system entities: %d%nsystem events: %d%n%n", systemEntities.size(), systemEvents.size());
      
      if (evaluationOutputDirectory != null){
         String sourceFileName = DocIdUtil.getDocumentID( jCas );
//          CasIOUtil.writeXmi(jCas, new File(evaluationOutputDirectory, sourceFileName + ".xmi"));
         new FileTreeXmiWriter().writeFile( jCas, evaluationOutputDirectory.getAbsolutePath(),
                                            sourceFileName, sourceFileName );
      }
      
      if (!options.ignorePolarity)
      {
	      polarityStats.add(goldEntitiesAndEvents, systemEntitiesAndEvents,
			  AnnotationStatisticsCompact.annotationToSpan(),
			  AnnotationStatisticsCompact.annotationToFeatureValue( "polarity" ) );
	      if(options.printErrors){
	    	  printErrors(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "polarity", CONST.NE_POLARITY_NEGATION_PRESENT, Integer.class);
	      }
	      if(options.printInstances!=null){
	    	  printInstances(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "polarity", CONST.NE_POLARITY_NEGATION_PRESENT, Integer.class, options.printInstances);
	      }
      }

      if (!options.ignoreConditional)
      {
	      conditionalStats.add(goldEntitiesAndEvents, systemEntitiesAndEvents,
			  AnnotationStatisticsCompact.<IdentifiedAnnotation>annotationToSpan(),
			  AnnotationStatisticsCompact.<IdentifiedAnnotation>annotationToFeatureValue("conditional"));
	      if(options.printErrors){
	    	  printErrors(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "conditional", CONST.NE_CONDITIONAL_TRUE, Boolean.class);
	      }
      }

      if (!options.ignoreUncertainty)
      {
	      uncertaintyStats.add(goldEntitiesAndEvents, systemEntitiesAndEvents,
			  AnnotationStatisticsCompact.<IdentifiedAnnotation>annotationToSpan(),
			  AnnotationStatisticsCompact.<IdentifiedAnnotation>annotationToFeatureValue("uncertainty"));
	      if(options.printErrors){
	    	  printErrors(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "uncertainty", CONST.NE_UNCERTAINTY_PRESENT, Integer.class);
	      }
      }

      if (!options.ignoreSubject)
      {
	      subjectStats.add(goldEntitiesAndEvents, systemEntitiesAndEvents,
			  AnnotationStatisticsCompact.<IdentifiedAnnotation>annotationToSpan(),
			  AnnotationStatisticsCompact.<IdentifiedAnnotation>annotationToFeatureValue("subject"));
	      if(options.printErrors){
	    	  printErrors(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "subject", null, CONST.ATTR_SUBJECT_PATIENT.getClass());
	      }
      }

      if (!options.ignoreGeneric)
      {
	      genericStats.add(goldEntitiesAndEvents, systemEntitiesAndEvents,
			  AnnotationStatisticsCompact.annotationToSpan(),
			  AnnotationStatisticsCompact.annotationToFeatureValue( "generic" ) );
	      if(options.printErrors){
	    	  printErrors(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "generic", CONST.NE_GENERIC_TRUE, Boolean.class);
	      }
      }
      
      // srh 3/6/13
      if (!options.ignoreHistory)
      {
    	  historyStats.add(goldEntitiesAndEvents, systemEntitiesAndEvents,
    			  AnnotationStatisticsCompact.annotationToSpan(),
    			  AnnotationStatisticsCompact.annotationToFeatureValue("historyOf"));
    	  if(options.printErrors){
    		  printErrors(jCas, goldEntitiesAndEvents, systemEntitiesAndEvents, "historyOf", CONST.NE_HISTORY_OF_PRESENT, Integer.class);
    	  }
      }
      
    }
    return map;
  }

  protected void trainAndPackage(String currentAssertionAttribute, File directory, String[] arguments) throws Exception {
	  if (options.featureSelectionThreshold!=null) {
		  // Extracting features and writing instances
		  Iterable<Instance<String>> instances = InstanceStream.loadFromDirectory(directory);

		  // Collect MinMax stats for feature normalization
		  FeatureSelection<String> featureSelection; 
		  if (currentAssertionAttribute.equals("polarity")) {
			  featureSelection = PolarityCleartkAnalysisEngine.createFeatureSelection(options.featureSelectionThreshold);
			  featureSelection.train(instances);
			  featureSelection.save(PolarityCleartkAnalysisEngine.createFeatureSelectionURI(directory));
		  }
		  else if (currentAssertionAttribute.equals("uncertainty")) {
			  featureSelection = UncertaintyCleartkAnalysisEngine.createFeatureSelection(options.featureSelectionThreshold);
			  featureSelection.train(instances);
			  featureSelection.save(UncertaintyCleartkAnalysisEngine.createFeatureSelectionURI(directory));
		  }
		  else if (currentAssertionAttribute.equals("conditional")) {
			  featureSelection = ConditionalCleartkAnalysisEngine.createFeatureSelection(options.featureSelectionThreshold);
			  featureSelection.train(instances);
			  featureSelection.save(ConditionalCleartkAnalysisEngine.createFeatureSelectionURI(directory));
		  }
		  else if (currentAssertionAttribute.equals("subject")) {
			  featureSelection = SubjectCleartkAnalysisEngine.createFeatureSelection(options.featureSelectionThreshold);
			  featureSelection.train(instances);
			  featureSelection.save(SubjectCleartkAnalysisEngine.createFeatureSelectionURI(directory));
		  }
		  else if (currentAssertionAttribute.equals("generic")) {
			  featureSelection = GenericCleartkAnalysisEngine.createFeatureSelection(options.featureSelectionThreshold);
			  featureSelection.train(instances);
			  featureSelection.save(GenericCleartkAnalysisEngine.createFeatureSelectionURI(directory));
		  }
		  else if (currentAssertionAttribute.equals("historyOf")) {
			  featureSelection = HistoryCleartkAnalysisEngine.createFeatureSelection(options.featureSelectionThreshold);
			  featureSelection.train(instances);
			  featureSelection.save(HistoryCleartkAnalysisEngine.createFeatureSelectionURI(directory));
		  }
		  else {
			  featureSelection = null;
			  throw new Exception("Feature selection is still null!");
		  }


	      // now write in the libsvm format
//	      LibLinearStringOutcomeDataWriter dataWriter = new LibLinearStringOutcomeDataWriter(directory);
		  Constructor<? extends DataWriter<String>> c = this.dataWriterClass.getConstructor(File.class);
	      DataWriter<String> dataWriter = c.newInstance(directory);
	      
	      // try filtering
	      for (Instance<String> instance : instances) {
	    	  dataWriter.write(featureSelection.transform(instance));
	      }
	      dataWriter.finish();
	  }

	  // train models based on instances
	  JarClassifierBuilder.trainAndPackage(directory, arguments);
  }
  
  protected Class<? extends DataWriter<String>> getDataWriterClass() {
    return (options.featureSelectionThreshold!=null)
        ? StringInstanceDataWriter.class
        : this.dataWriterClass;
  }

  class StringInstanceDataWriter extends InstanceDataWriter<String> {

	public StringInstanceDataWriter(File outputDirectory) throws IOException {
		super(outputDirectory);
	}
	  
  }
  
private static void printErrors(JCas jCas,
		  Collection<IdentifiedAnnotation> goldEntitiesAndEvents,
		  Collection<IdentifiedAnnotation> systemEntitiesAndEvents, String classifierType, Object trueCategory, Class<? extends Object> categoryClass) throws ResourceProcessException {

   String documentId = DocIdUtil.getDocumentID( jCas );
	
	  Map<HashableAnnotation, IdentifiedAnnotation> goldMap = Maps.newHashMap();
	  for (IdentifiedAnnotation mention : goldEntitiesAndEvents) {
		  goldMap.put(new HashableAnnotation(mention), mention);
	  }
	  Map<HashableAnnotation, IdentifiedAnnotation> systemMap = Maps.newHashMap();
	  for (IdentifiedAnnotation relation : systemEntitiesAndEvents) {
		  systemMap.put(new HashableAnnotation(relation), relation);
	  }
	  Set<HashableAnnotation> all = Sets.union(goldMap.keySet(), systemMap.keySet());
	  List<HashableAnnotation> sorted = Lists.newArrayList(all);
	  Collections.sort(sorted);
	  for (HashableAnnotation key : sorted) {
		  IdentifiedAnnotation goldAnnotation = goldMap.get(key);
		  IdentifiedAnnotation systemAnnotation = systemMap.get(key);
		  Object goldLabel=null;
		  Object systemLabel=null;
		  if (goldAnnotation == null) {
			  logger.debug(key + " not found in gold annotations ");
		  } else {
			  Feature feature = goldAnnotation.getType().getFeatureByBaseName(classifierType);
			  goldLabel = getFeatureValue(feature, categoryClass, goldAnnotation);
			  //  Integer goldLabel = goldAnnotation.getIntValue(feature);
		  }
		  
		  if (systemAnnotation == null) {
			  logger.info(key + " not found in system annotations ");
		  } else {
			  Feature feature = systemAnnotation.getType().getFeatureByBaseName(classifierType);
			  systemLabel = getFeatureValue(feature, categoryClass, systemAnnotation);
			  //  Integer systemLabel = systemAnnotation.getIntValue(feature);
		  }
		  
		  String typeId;
		  if (systemAnnotation!=null) {
			  typeId = systemAnnotation.getTypeID()+"";
		  } else  {
			  typeId = "X";
		  }
		  
		  if (goldLabel==null) {
			  // skip counting the attribute value since we have no gold label to compare to
			  logger.debug("Skipping annotation with label " + systemLabel + " because gold label is null");
		  } else if (systemLabel==null){
			  logger.debug("Skipping annotation with label " + systemLabel + " because system label is null");
		  } else  {
			  if(!goldLabel.equals(systemLabel)){
				  if(trueCategory == null){
					  // used for multi-class case.  Incorrect_system_label(Correct_label):
					  System.out.println(classifierType+ " "+ systemLabel + "(" + goldLabel + "): " + formatError(jCas, systemAnnotation));
				  }else if(systemLabel.equals(trueCategory)){
					  System.out.println(classifierType+" FP: " + typeId  + " " + formatError(jCas, systemAnnotation) + "| gold:|" + formatError(jCas, goldAnnotation) + " " + documentId);
				  }else{
					  System.out.println(classifierType+" FN: " + typeId + " " + formatError(jCas, goldAnnotation)+ "| system:|" + formatError(jCas, systemAnnotation) + " " + documentId);
				  }
			  }else{
			    if(trueCategory == null){
			      // multi-class case -- probably don't want to print anything?
			    	System.out.println(classifierType+ " "+ systemLabel + "(" + goldLabel + "): " + formatError(jCas, systemAnnotation));
			    }else if(systemLabel.equals(trueCategory)){
					  System.out.println(classifierType+" TP: " + typeId + " " + formatError(jCas, systemAnnotation) + "| gold:|" + formatError(jCas, goldAnnotation) + " " + documentId);
				  }else{
					  System.out.println(classifierType+" TN: " + typeId + " " + formatError(jCas, systemAnnotation) + "| gold:|" + formatError(jCas, goldAnnotation) + " " + documentId);
				  }
			  }
		  }
	  }
  }
  
private static void printInstances(JCas jCas,
		  Collection<IdentifiedAnnotation> goldEntitiesAndEvents,
		  Collection<IdentifiedAnnotation> systemEntitiesAndEvents, String classifierType, 
		  Object trueCategory, Class<?> categoryClass,
		  File outputfile) 
				  throws ResourceProcessException, IOException {

   String documentId = DocIdUtil.getDocumentID( jCas );
	  Map<HashableAnnotation, IdentifiedAnnotation> goldMap = Maps.newHashMap();
	  for (IdentifiedAnnotation mention : goldEntitiesAndEvents) {
		  goldMap.put(new HashableAnnotation(mention), mention);
	  }
	  Map<HashableAnnotation, IdentifiedAnnotation> systemMap = Maps.newHashMap();
	  for (IdentifiedAnnotation relation : systemEntitiesAndEvents) {
		  systemMap.put(new HashableAnnotation(relation), relation);
	  }
	  Set<HashableAnnotation> all = Sets.union(goldMap.keySet(), systemMap.keySet());
	  List<HashableAnnotation> sorted = Lists.newArrayList(all);
	  Collections.sort(sorted);
	  try(
			  BufferedWriter fileOutWriter = new BufferedWriter(new FileWriter(outputfile,true), 32768);
			  ){
		  for (HashableAnnotation key : sorted) {
			  IdentifiedAnnotation goldAnnotation = goldMap.get(key);
			  IdentifiedAnnotation systemAnnotation = systemMap.get(key);
			  Object goldLabel=null;
			  Object systemLabel=null;
			  if (goldAnnotation == null) {
				  logger.debug(key + " not found in gold annotations ");
			  } else {
				  Feature feature = goldAnnotation.getType().getFeatureByBaseName(classifierType);
				  goldLabel = getFeatureValue(feature, categoryClass, goldAnnotation);
				  //  Integer goldLabel = goldAnnotation.getIntValue(feature);
			  }

			  if (systemAnnotation == null) {
				  logger.info(key + " not found in system annotations ");
			  } else {
				  Feature feature = systemAnnotation.getType().getFeatureByBaseName(classifierType);
				  systemLabel = getFeatureValue(feature, categoryClass, systemAnnotation);
				  //  Integer systemLabel = systemAnnotation.getIntValue(feature);
			  }

			  String typeId = "X";
			  String typeName = "IdentifiedAnnotation";
			  int polarity, uncertainty, historyOf;
			  boolean conditional, generic;
			  String subject = "";
			  String coveredText = "";
			  String instanceData = "";
			  if (systemAnnotation!=null && systemAnnotation.getEnd()>=0) {
				  typeId      = systemAnnotation.getTypeID()+"";
				  typeName    = systemAnnotation.getClass().getSimpleName();
				  polarity    = systemAnnotation.getPolarity();
				  uncertainty = systemAnnotation.getUncertainty();
				  conditional = systemAnnotation.getConditional();
				  generic     = systemAnnotation.getGeneric();
				  subject     = systemAnnotation.getSubject();
				  historyOf   = systemAnnotation.getHistoryOf();
				  coveredText = systemAnnotation.getCoveredText().replaceAll("\\n", " ").replaceAll(",",";");
				  instanceData = documentId+","+polarity+","+uncertainty+","+conditional+","+generic+","+subject+","+historyOf+","+
						  typeId+","+typeName+","+coveredText;
			  }

			  if (goldLabel==null) {
				  // skip counting the attribute value since we have no gold label to compare to
				  logger.debug("Skipping annotation with label " + systemLabel + " because gold label is null");
			  } else if (systemLabel == null){			  
				  logger.debug("Skipping annotation with label " + systemLabel + " because system label is null");
			  } else if (instanceData.equals("")) {
				  continue;
			  }
			  else  {
				  if(!goldLabel.equals(systemLabel)){
					  if(trueCategory == null){
						  // used for multi-class case.  Incorrect_system_label(Correct_label):
						  fileOutWriter.write(classifierType+",F,"+systemLabel+","+goldLabel+","+instanceData+"\n");
					  }else if(systemLabel.equals(trueCategory)){
						  fileOutWriter.write(classifierType+",FP,"+systemLabel+","+goldLabel+","+instanceData+"\n");
					  }else{
						  fileOutWriter.write(classifierType+",FN,"+systemLabel+","+goldLabel+","+instanceData+"\n");
					  }
				  }else{
					  if(trueCategory == null){
						  // multi-class case -- probably don't want to print anything?
						  fileOutWriter.write(classifierType+ ",T,"+systemLabel+","+goldLabel+","+instanceData+"\n");
					  }else if(systemLabel.equals(trueCategory)){
						  fileOutWriter.write(classifierType+",TP,"+systemLabel+","+goldLabel+","+instanceData+"\n");
					  }else{
						  fileOutWriter.write(classifierType+",TN,"+systemLabel+","+goldLabel+","+instanceData+"\n");
					  }
				  }
				  fileOutWriter.flush();
			  }
		  }
	  }
	}
  private static Object getFeatureValue( Feature feature,
                                         Class<?> class1, Annotation annotation ) throws ResourceProcessException {
	  if(class1 == Integer.class){
		  return annotation.getIntValue(feature);
	  }else if(class1 == String.class){
		  return annotation.getStringValue(feature);
	  }else if(class1 == Boolean.class){
		  return annotation.getBooleanValue(feature);
	  }else{
		  throw new ResourceProcessException("Received a class type that I'm not familiar with: ", new Object[]{class1});
	  }
  }

  private static String formatError(JCas jcas, IdentifiedAnnotation mention){
	  List<Sentence> context = JCasUtil.selectCovering(jcas, Sentence.class, mention.getBegin(), mention.getEnd());
	  StringBuffer buff = new StringBuffer();
	  if(context.size() > 0){
		  Sentence sent = context.get(0);
		  buff.append(sent.getCoveredText());
		  long offset = mention.getBegin() - sent.getBegin();
		  if (offset>=Integer.MAX_VALUE || offset<=Integer.MIN_VALUE) { offset=0; } // for spanless annots
		  buff.insert((int)offset, "***");
		  offset += (mention.getEnd()-mention.getBegin() + 3);
		  buff.insert((int)offset, "***");
	  }
	  return buff.toString();
  }

public static class HashableAnnotation implements Comparable<HashableAnnotation> {

    protected int begin;

    protected int end;

    public HashableAnnotation(int begin, int end) {
      this.begin = begin;
      this.end = end;
    }

    public HashableAnnotation(Annotation arg1) {
      this(arg1.getBegin(), arg1.getEnd());
    }

    @Override
    public boolean equals(Object otherObject) {
      boolean result = false;
      if (otherObject instanceof HashableAnnotation) {
        HashableAnnotation other = (HashableAnnotation) otherObject;
        result = (this.getClass() == other.getClass() && this.begin == other.begin
            && this.end == other.end);
      }
      
      return result;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.begin, this.end);
    }

    @Override
    public String toString() {
      return String.format(
          "%s(%s,%s)",
          this.getClass().getSimpleName(),
          this.begin,
          this.end);
    }

    @Override
    public int compareTo(HashableAnnotation that) {
      int thisBegin = this.begin;
      int thatBegin = that.begin;
      if (thisBegin < thatBegin) {
        return -1;
      } else if (thisBegin > thatBegin) {
        return +1;
      } else if (this.equals(that)) {
        return 0;
      } else {
        return +1; // arbitrary choice for overlapping
      }
    }
  }

private static void addExternalAttributeAnnotatorsToAggregate(AggregateBuilder builder)
		throws UIMAException, IOException {
  builder.add(AnalysisEngineFactory.createEngineDescription(ConceptConverterAnalysisEngine.class));
	// RUN ALL THE OLD (non-ClearTK) CLASSIFIERS
	AnalysisEngineDescription oldAssertionAnnotator = AnalysisEngineFactory.createEngineDescription("desc/assertionAnalysisEngine"); 
	ConfigurationParameterFactory.addConfigurationParameters(
			oldAssertionAnnotator,
			AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
			AssertionEvaluation.GOLD_VIEW_NAME
	);
	builder.add(oldAssertionAnnotator);

	AnalysisEngineDescription oldConversionAnnotator = AnalysisEngineFactory.createEngineDescription("desc/conceptConverterAnalysisEngine"); 
	ConfigurationParameterFactory.addConfigurationParameters(
			oldConversionAnnotator,
			AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
			AssertionEvaluation.GOLD_VIEW_NAME
	);
	builder.add(oldConversionAnnotator);

	AnalysisEngineDescription oldSubjectAnnotator = AnalysisEngineFactory.createEngineDescription("desc/SubjectAttributeAnalysisEngine"); 
	ConfigurationParameterFactory.addConfigurationParameters(
			oldSubjectAnnotator,
			AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
			AssertionEvaluation.GOLD_VIEW_NAME
	);
	builder.add(oldSubjectAnnotator);

	AnalysisEngineDescription oldGenericAnnotator = AnalysisEngineFactory.createEngineDescription("desc/GenericAttributeAnalysisEngine"); 
	ConfigurationParameterFactory.addConfigurationParameters(
			oldGenericAnnotator,
			AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
			AssertionEvaluation.GOLD_VIEW_NAME
	);
	builder.add(oldGenericAnnotator);
}

private static void addCleartkAttributeAnnotatorsToAggregate(File directory,
		AggregateBuilder builder) throws UIMAException, IOException,
		ResourceInitializationException {
    builder.add(AnalysisEngineFactory.createEngineDescription(AlternateCuePhraseAnnotator.class, new Object[]{}));

	// Add the ClearTk or the ytex negation (polarity) classifier
	if (!options.ignorePolarity)
	{
		AnalysisEngineDescription polarityAnnotator;
    	if (options.useYtexNegation) {
    		polarityAnnotator = AnalysisEngineFactory.createEngineDescription(YTEX_NEGATION_DESCRIPTOR);
    		builder.add(polarityAnnotator);
    	} else {
    		if (options.feda) {
    			polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityFedaCleartkAnalysisEngine.class);

      			ConfigurationParameterFactory.addConfigurationParameters(
        				polarityAnnotator,
        				AssertionCleartkAnalysisEngine.FILE_TO_DOMAIN_MAP,
        				options.testDirectory
        				);
    		} else {
    			// default: cleartk-based polarity, no domain adaptation
    			polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityCleartkAnalysisEngine.class); //,  this.additionalParamemters);
      		}
    		ConfigurationParameterFactory.addConfigurationParameters(
    				polarityAnnotator,
    				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
    				AssertionEvaluation.GOLD_VIEW_NAME,
    				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
    				new File(new File(directory, "polarity"), "model.jar").getPath(),
            AssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
            options.featConfig
    				);
    		builder.add(polarityAnnotator);
    	}
	}

	// Add the rest of the ClearTk classifiers
	if (!options.ignoreConditional)
	{
		AnalysisEngineDescription conditionalAnnotator = AnalysisEngineFactory.createEngineDescription(ConditionalCleartkAnalysisEngine.class); //,  this.additionalParamemters);
		ConfigurationParameterFactory.addConfigurationParameters(
				conditionalAnnotator,
				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
				AssertionEvaluation.GOLD_VIEW_NAME,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory, "conditional"), "model.jar").getPath()
		);
		builder.add(conditionalAnnotator);
	}

	if (!options.ignoreUncertainty)
	{
		AnalysisEngineDescription uncertaintyAnnotator = AnalysisEngineFactory.createEngineDescription(UncertaintyCleartkAnalysisEngine.class); //,  this.additionalParamemters);
		ConfigurationParameterFactory.addConfigurationParameters(
				uncertaintyAnnotator,
				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
				AssertionEvaluation.GOLD_VIEW_NAME,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory, "uncertainty"), "model.jar").getPath()
		);
		builder.add(uncertaintyAnnotator);
	}

	if (!options.ignoreSubject)
	{
		AnalysisEngineDescription subjectAnnotator = AnalysisEngineFactory.createEngineDescription(SubjectCleartkAnalysisEngine.class); //,  this.additionalParamemters);
		ConfigurationParameterFactory.addConfigurationParameters(
				subjectAnnotator,
				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
				AssertionEvaluation.GOLD_VIEW_NAME,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory, "subject"), "model.jar").getPath()
		);
		builder.add(subjectAnnotator);
	}

	if (!options.ignoreGeneric)
	{
		AnalysisEngineDescription genericAnnotator = AnalysisEngineFactory.createEngineDescription(GenericCleartkAnalysisEngine.class); //,  this.additionalParamemters);
		ConfigurationParameterFactory.addConfigurationParameters(
				genericAnnotator,
				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
				AssertionEvaluation.GOLD_VIEW_NAME,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory, "generic"), "model.jar").getPath()
		);
		builder.add(genericAnnotator);
	}
	
	if(!options.ignoreHistory){
		AnalysisEngineDescription historyAnnotator = AnalysisEngineFactory.createEngineDescription(HistoryCleartkAnalysisEngine.class);
		ConfigurationParameterFactory.addConfigurationParameters(
				historyAnnotator,
				AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
				AssertionEvaluation.GOLD_VIEW_NAME,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(new File(directory, "historyOf"), "model.jar").getPath()
				);
		builder.add(historyAnnotator);
	}
}

  public static final String GOLD_VIEW_NAME = "GoldView";

  /**
   * Class that copies the manual {@link Modifier} annotations to the default CAS.
   */
  public static class OnlyGoldAssertions extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas goldView;
      try {
        goldView = jCas.getView(GOLD_VIEW_NAME);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      // remove any automatically generated Modifiers
      for (EntityMention entityMention : JCasUtil.select(jCas, EntityMention.class)) {
        entityMention.removeFromIndexes();
      }

      // copy over the manually annotated Modifiers
      for (EntityMention entityMention : JCasUtil.select(goldView, EntityMention.class)) {
        EntityMention newEntityMention = new EntityMention(jCas, entityMention.getBegin(), entityMention.getEnd());
        newEntityMention.setTypeID(entityMention.getTypeID());
        newEntityMention.setId(entityMention.getId());
        newEntityMention.setDiscoveryTechnique(entityMention.getDiscoveryTechnique());
        newEntityMention.setConfidence(entityMention.getConfidence());
        newEntityMention.addToIndexes();
      }
    }
  }
  

  /**
   * Annotator that removes cTAKES EntityMentions and Modifiers from the system
   * view, and copies over the manually annotated EntityMentions and Modifiers
   * from the gold view.
   * 
   */
  public static class ReplaceCTakesEntityMentionsAndModifiersWithGold extends
      JCasAnnotator_ImplBase
  {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException
    {
      JCas goldView, systemView;
      try
      {
        goldView = jCas.getView(GOLD_VIEW_NAME);
        systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      } catch (CASException e)
      {
        throw new AnalysisEngineProcessException(e);
      }

      // remove cTAKES EntityMentions and Modifiers from system view
      List<IdentifiedAnnotation> cTakesMentions = new ArrayList<>();
      cTakesMentions.addAll(JCasUtil.select(systemView, EntityMention.class));
      cTakesMentions.addAll(JCasUtil.select(systemView, Modifier.class));
      for (IdentifiedAnnotation cTakesMention : cTakesMentions)
      {
        cTakesMention.removeFromIndexes();
      }

      // copy gold EntityMentions and Modifiers to the system view
      List<IdentifiedAnnotation> goldMentions = new ArrayList<>();
      goldMentions.addAll(JCasUtil.select(goldView, EntityMention.class));
      goldMentions.addAll(JCasUtil.select(goldView, Modifier.class));
      CasCopier copier = new CasCopier(goldView.getCas(), systemView.getCas());
      for (IdentifiedAnnotation goldMention : goldMentions)
      {
        Annotation copy = (Annotation) copier.copyFs(goldMention);
        Feature sofaFeature = copy.getType().getFeatureByBaseName("sofa");
        copy.setFeatureValue(sofaFeature, systemView.getSofa());
        copy.addToIndexes();
      }
    }
  }
 
  
  /**
   * Class that copies the manual {@link Modifier} annotations to the default CAS.
   */
  public static class ReferenceIdentifiedAnnotationsSystemToGoldCopier extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas goldView;
      try {
        goldView = jCas.createView(GOLD_VIEW_NAME);
        goldView.setSofaDataString(jCas.getSofaDataString(), jCas.getSofaMimeType());
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      for (EntityMention oldSystemEntityMention : JCasUtil.select(jCas, EntityMention.class))
      {
        EntityMention newGoldEntityMention = new EntityMention(goldView, oldSystemEntityMention.getBegin(), oldSystemEntityMention.getEnd());
        
        // copying assertion fields
        newGoldEntityMention.setDiscoveryTechnique(oldSystemEntityMention.getDiscoveryTechnique());
        newGoldEntityMention.setUncertainty(oldSystemEntityMention.getUncertainty());
        newGoldEntityMention.setConditional(oldSystemEntityMention.getConditional());
        newGoldEntityMention.setGeneric(oldSystemEntityMention.getGeneric());
        newGoldEntityMention.setPolarity(oldSystemEntityMention.getPolarity());
        newGoldEntityMention.setSubject(oldSystemEntityMention.getSubject());
        newGoldEntityMention.setHistoryOf(oldSystemEntityMention.getHistoryOf());

        // copying non-assertion fields
        newGoldEntityMention.setConfidence(oldSystemEntityMention.getConfidence());
        newGoldEntityMention.setTypeID(oldSystemEntityMention.getTypeID());
        
        newGoldEntityMention.addToIndexes();
      }

      for (EventMention oldSystemEventMention : JCasUtil.select(jCas, EventMention.class))
      {
        EventMention newGoldEventMention = new EventMention(goldView, oldSystemEventMention.getBegin(), oldSystemEventMention.getEnd());
        
        // copying assertion fields
        newGoldEventMention.setDiscoveryTechnique(oldSystemEventMention.getDiscoveryTechnique());
        newGoldEventMention.setUncertainty(oldSystemEventMention.getUncertainty());
        newGoldEventMention.setConditional(oldSystemEventMention.getConditional());
        newGoldEventMention.setGeneric(oldSystemEventMention.getGeneric());
        newGoldEventMention.setPolarity(oldSystemEventMention.getPolarity());
        newGoldEventMention.setSubject(oldSystemEventMention.getSubject());
        newGoldEventMention.setHistoryOf(oldSystemEventMention.getHistoryOf());

        // copying non-assertion fields
        newGoldEventMention.setConfidence(oldSystemEventMention.getConfidence());
        newGoldEventMention.setTypeID(oldSystemEventMention.getTypeID());

        newGoldEventMention.addToIndexes();
      }
    } // end of method ReferenceIdentifiedAnnotationsSystemToGoldCopier.process()
  } // end of class ReferenceIdentifiedAnnotationsSystemToGoldCopier

  /**
   * Class that copies the manual {@link Modifier} annotations to the default CAS.
   */
  public static class ReferenceSupportingAnnotationsSystemToGoldCopier extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas goldView;
      try {
        goldView = jCas.getView(GOLD_VIEW_NAME);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      for (Sentence oldSystemSentence : JCasUtil.select(jCas, Sentence.class))
      {
        Sentence newGoldSentence = new Sentence(goldView, oldSystemSentence.getBegin(), oldSystemSentence.getEnd());
        
        newGoldSentence.addToIndexes();
      }

      for (BaseToken oldSystemToken : JCasUtil.select(jCas, BaseToken.class))
      {
        BaseToken newGoldToken = null; //new BaseToken(goldView, oldSystemEventMention.getBegin(), oldSystemEventMention.getEnd());

        String oldSystemTokenClass = oldSystemToken.getClass().getName();
        if (oldSystemTokenClass.equals(WordToken.class.getName()))
        {
          newGoldToken = new WordToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else if (oldSystemTokenClass.equals(ContractionToken.class.getName()))
        {
          newGoldToken = new ContractionToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else if (oldSystemTokenClass.equals(NewlineToken.class.getName()))
        {
          newGoldToken = new NewlineToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else if (oldSystemTokenClass.equals(NumToken.class.getName()))
        {
          newGoldToken = new NumToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else if (oldSystemTokenClass.equals(PunctuationToken.class.getName()))
        {
          newGoldToken = new PunctuationToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else if (oldSystemTokenClass.equals(SymbolToken.class.getName()))
        {
          newGoldToken = new SymbolToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else if (oldSystemTokenClass.equals(BaseToken.class.getName()))
        {
          newGoldToken = new BaseToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        } else
        {
          newGoldToken = new BaseToken(goldView, oldSystemToken.getBegin(), oldSystemToken.getEnd());
        }
        
        newGoldToken.setPartOfSpeech(oldSystemToken.getPartOfSpeech());
        newGoldToken.setTokenNumber(oldSystemToken.getTokenNumber());
        
        newGoldToken.addToIndexes();
      }

    } // end of method ReferenceSupportingAnnotationsSystemToGoldCopier.process()
  } // end of class ReferenceSupportingAnnotationsSystemToGoldCopier

  /**
   * Class that copies the manual {@link Modifier} annotations to the default CAS.
   */
  public static class ReferenceAnnotationsSystemAssertionClearer extends JCasAnnotator_ImplBase
  {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException
    {
      for (EntityMention entityMention : JCasUtil.select(jCas,  EntityMention.class))
      {
        entityMention.setPolarity(1);
      }
      for (EventMention eventMention : JCasUtil.select(jCas,  EventMention.class))
      {
        eventMention.setPolarity(1);
      }
    } // end method ReferenceAnnotationsSystemAssertionClearer.process()
  } // end class ReferenceAnnotationsSystemAssertionClearer
  
} // end of class AssertionEvalBasedOnModifier
