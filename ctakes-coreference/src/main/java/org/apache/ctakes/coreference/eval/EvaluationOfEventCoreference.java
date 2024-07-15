package org.apache.ctakes.coreference.eval;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import de.bwaldvogel.liblinear.FeatureNode;
import org.apache.commons.lang.NotImplementedException;
import org.apache.ctakes.assertion.medfacts.cleartk.*;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.patient.PatientNoteCollector;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.coreference.ae.*;
import org.apache.ctakes.coreference.factory.CoreferenceAnnotatorFactory;
import org.apache.ctakes.coreference.type.CollectionRelation;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.eval.EvaluationOfTemporalRelations_ImplBase;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.DocumentIdPrefix;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
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
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.FileUtils;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.ml.svmlight.rank.SvmLightRankDataWriter;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.util.ViewUriUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EvaluationOfEventCoreference extends EvaluationOfTemporalRelations_ImplBase {
 

  static interface CoreferenceOptions extends TempRelOptions{
    @Option
    public String getOutputDirectory();
    
    @Option
    public boolean getUseTmp();
    
    @Option
    public boolean getTestOnTrain();
    
    @Option(longName="external")
    public boolean getUseExternalScorer();
    
    @Option(shortName="t", defaultValue={"MENTION_CLUSTER"})
    public EVAL_SYSTEM getEvalSystem();
    
    @Option(shortName="c", defaultValue="default")
    public String getConfig();
    
    @Option(shortName="s")
    public String getScorerPath();
    
    @Option
    public boolean getGoldMarkables();
    
    @Option
    public boolean getSkipTest();
  }
  
  private static Logger logger = Logger.getLogger(EvaluationOfEventCoreference.class);
  public static float COREF_PAIRS_DOWNSAMPLE = 0.5f;
  public static float COREF_CLUSTER_DOWNSAMPLE=0.5f;
  private static final int NUM_SAMPLES = 0;
  private static final double DROPOUT_RATE = 0.1;
  
  protected static ParameterSettings pairwiseParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, COREF_PAIRS_DOWNSAMPLE, "linear",
      1.0, 1.0, "linear", ComboOperator.SUM, 0.1, 0.5);
  protected static ParameterSettings clusterParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, COREF_CLUSTER_DOWNSAMPLE, "linear",
      1.0, 1.0, "linear", ComboOperator.SUM, 0.1, 0.5);
  
  private static String goldOut = "";
  private static String systemOut = "";
  
  public static void main(String[] args) throws Exception {
    CoreferenceOptions options = CliFactory.parseArguments(CoreferenceOptions.class, args);

    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = getTrainItems(options);
    List<Integer> testItems = options.getTestOnTrain() ? getTrainItems(options) : getTestItems(options);

    ParameterSettings params = options.getEvalSystem() == EVAL_SYSTEM.MENTION_PAIR ? pairwiseParams : clusterParams;
    
    File workingDir = new File("target/eval/temporal-relations/coreference/" + options.getEvalSystem() + File.separator +  options.getConfig());
    if(!workingDir.exists()) workingDir.mkdirs();
    if(options.getUseTmp()){
      File tempModelDir = File.createTempFile("temporal", null, workingDir);
      tempModelDir.delete();
      tempModelDir.mkdir();
      workingDir = tempModelDir;
    }
    EvaluationOfEventCoreference eval = new EvaluationOfEventCoreference(
        workingDir,
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getSubcorpus(),
        options.getXMIDirectory(),
        options.getTreebankDirectory(),
        options.getPrintErrors(),
        options.getPrintFormattedRelations(),
        params,
        options.getKernelParams(),
        options.getOutputDirectory());

    eval.skipTrain = options.getSkipTrain();
    eval.skipWrite = options.getSkipDataWriting();
    eval.skipTest = options.getSkipTest();
    eval.goldMarkables = options.getGoldMarkables();
    eval.evalType = options.getEvalSystem();
    eval.config = options.getConfig();
    goldOut = "gold." + eval.config + ".conll";
    systemOut = "system." + eval.config + ".conll";
    
    eval.prepareXMIsFor(patientSets);
    
    params.stats = eval.trainAndTest(trainItems, testItems);//training);//

    if(options.getUseTmp()){
      FileUtils.deleteRecursive(workingDir);
    }
    
    if(options.getUseExternalScorer() && !options.getSkipTest()){
      Pattern patt = Pattern.compile("(?:Coreference|BLANC): Recall: \\([^\\)]*\\) (\\S+)%.*Precision: \\([^\\)]*\\) (\\S+)%.*F1: (\\S+)%");
      Runtime runtime = Runtime.getRuntime();
      String cmd = String.format("perl %s all %s %s none", 
          options.getScorerPath(), 
          options.getOutputDirectory() + File.separator + goldOut, 
          options.getOutputDirectory() + File.separator + systemOut);
      System.out.println("Running official scoring tool with command: " + cmd);
      Process p = runtime.exec(cmd.split(" "));
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line, metric=null;
      System.out.println(String.format("%10s%7s%7s%7s", "Metric", "Rec", "Prec", "F1"));
      Map<String,Double> scores = new HashMap<>();
      while((line = reader.readLine()) != null){
        line = line.trim();
        if(line.startsWith("METRIC")){
          metric = line.substring(7);  // everything after "METRIC"
          metric = metric.substring(0, metric.length()-1);  // remove colon from the end
        }else if(line.startsWith("Coreference")){
          Matcher m = patt.matcher(line);
          if(m.matches()){
            System.out.println(String.format("%10s%7.2f%7.2f%7.2f", metric, Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))));
            scores.put(metric, Double.parseDouble(m.group(3)));
          }
        }
      }
      
      if(scores.containsKey("muc") && scores.containsKey("bcub") && scores.containsKey("ceafe")){
        double conll = (scores.get("muc") + scores.get("bcub") + scores.get("ceafe")) / 3.0;
        System.out.println(String.format("%10s              %7.2f", "Conll", conll));
      }
    }
  }
  
  boolean skipTrain=false; 
  boolean skipWrite=false;
  boolean skipTest=false;
  boolean goldMarkables=false;
  public enum EVAL_SYSTEM { BASELINE, MENTION_PAIR, MENTION_CLUSTER, CLUSTER_RANK, PERSON_ONLY };
  EVAL_SYSTEM evalType;
  String config=null;
  
  private String outputDirectory;
  
  public EvaluationOfEventCoreference(File baseDirectory,
      File rawTextDirectory, File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat, Subcorpus subcorpus,
      File xmiDirectory, File treebankDirectory, boolean printErrors,
      boolean printRelations, ParameterSettings params, String cmdParams, String outputDirectory) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory,
        treebankDirectory, printErrors, printRelations, params);
    this.outputDirectory = outputDirectory;
    this.kernelParams = cmdParams == null ? null : cmdParams.replace("\"", "").split(" ");
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    if(skipTrain) return;
    if(this.evalType == EVAL_SYSTEM.BASELINE || this.evalType == EVAL_SYSTEM.PERSON_ONLY) return;
    if(!skipWrite){
//      ExternalResourceDescription depParserExtDesc = ExternalResourceFactory.createExternalResourceDescription(DependencySharedModel.class,
//          DependencySharedModel.DEFAULT_MODEL_FILE_NAME);

      // need this mapping for the document-aware coref module to map all gold views to system views during training.
      AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIDPrinter.class));
      aggregateBuilder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());

//      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ViewCreatorAnnotator.class, ViewCreatorAnnotator.PARAM_VIEW_NAME, "Baseline"));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphAnnotator.class));
      //      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphVectorAnnotator.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RelationPropagator.class));
      aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
      aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/timeannotator/model.jar"));
      aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/doctimerel/model.jar"));
      if(this.goldMarkables){
        throw new NotImplementedException("Using gold markables needs to be rewritten to be compatible with patient-level annotations.");
//        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyGoldMarkablesInChains.class));
      }else{
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
        //    aggregateBuilder.add(CopyFromGold.getDescription(/*Markable.class,*/ CoreferenceRelation.class, CollectionTextRelation.class));
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemovePersonMarkables.class));
      }
      aggregateBuilder.add(CopyFromSystem.getDescription(Segment.class), GOLD_VIEW_NAME, GOLD_VIEW_NAME);

      aggregateBuilder.add(MarkableSalienceAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/salience/model.jar"));
      if(this.evalType == EVAL_SYSTEM.MENTION_PAIR){
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyCoreferenceRelations.class), CopyCoreferenceRelations.PARAM_GOLD_VIEW, GOLD_VIEW_NAME);
        aggregateBuilder.add(EventCoreferenceAnnotator.createDataWriterDescription(
            //        TKSVMlightStringOutcomeDataWriter.class,
            FlushingDataWriter.class,
            //            LibSvmStringOutcomeDataWriter.class,
            //            TkLibSvmStringOutcomeDataWriter.class,
            directory,
            params.probabilityOfKeepingANegativeExample
            ));
        Logger.getLogger(EventCoreferenceAnnotator.class).setLevel(Level.WARN);
      }else if(this.evalType == EVAL_SYSTEM.MENTION_CLUSTER){
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientNoteCollector.class));
        aggregateBuilder.add(ThymeAnaforaCrossDocCorefXmlReader.getDescription(this.xmlDirectory.getAbsolutePath(), true ) );
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyCrossDocCoreferenceRelations.class));
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
                PatientMentionClusterCoreferencer.class,
                CleartkAnnotator.PARAM_IS_TRAINING,
                true,
                MentionClusterCoreferenceAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
                params.probabilityOfKeepingANegativeExample,
                DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
                FlushingDataWriter.class,
                DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
                directory,
                MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
                false));
      }else if(this.evalType == EVAL_SYSTEM.CLUSTER_RANK){
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyCoreferenceRelations.class), CopyCoreferenceRelations.PARAM_GOLD_VIEW, GOLD_VIEW_NAME);
        aggregateBuilder.add(MentionClusterRankingCoreferenceAnnotator.createDataWriterDescription(
            SvmLightRankDataWriter.class,
            directory,
            params.probabilityOfKeepingANegativeExample));
      }else{
        logger.warn("Encountered a training configuration that does not add an annotator: " + this.evalType);
      }

      AnalysisEngineDescription aed = aggregateBuilder.createAggregateDescription();
      SimplePipeline.runPipeline(collectionReader, AnalysisEngineFactory.createEngine(aed));
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
    }
    JarClassifierBuilder.trainAndPackage(directory, optArray);
  }

  @Override
  protected AnnotationStatistics<String> test(
      CollectionReader collectionReader, File directory) throws Exception {
    AnnotationStatistics<String> corefStats = new AnnotationStatistics<>();
    AnnotationStatistics<String> mentionStats = new AnnotationStatistics<>();
    
    if(this.skipTest){
      logger.info("Skipping test");
      return corefStats;
    }
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIdFromURI.class));
    aggregateBuilder.add("Patient id printer", AnalysisEngineFactory.createEngineDescription(DocumentIDPrinter.class));
    aggregateBuilder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphAnnotator.class));
    //    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphVectorAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RelationPropagator.class));
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/timeannotator/model.jar"));
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/doctimerel/model.jar"));

    if(this.goldMarkables){
      throw new NotImplementedException("Using gold markables needs to be rewritten to be compatible with patient-level annotations.");
//      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyGoldMarkablesInChains.class)); //CopyFromGold.getDescription(Markable.class));
    }else{
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
      //    aggregateBuilder.add(CopyFromGold.getDescription(/*Markable.class,*/ CoreferenceRelation.class, CollectionTextRelation.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemovePersonMarkables.class));
    }
    aggregateBuilder.add(MarkableSalienceAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/salience/model.jar"));
    if(this.evalType == EVAL_SYSTEM.MENTION_CLUSTER) {
      // Do nothing but we still need this here so the else clause works right
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientNoteCollector.class));
      aggregateBuilder.add(ThymeAnaforaCrossDocCorefXmlReader.getDescription(this.xmlDirectory.getAbsolutePath(), false));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientMentionClusterCoreferencer.class,
              CleartkAnnotator.PARAM_IS_TRAINING,
              false,
              GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
              directory.getAbsolutePath() + File.separator + "model.jar",
              MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
              false,
              AbstractPatientConsumer.REMOVE_PATIENT,
              false));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientPersonChainAnnotator.class, AbstractPatientConsumer.REMOVE_PATIENT, false));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientScoringWriter.class,
              ConfigParameterConstants.PARAM_OUTPUTDIR,
              this.outputDirectory,
              PatientScoringWriter.PARAM_CONFIG,
              config));
    }else{
      if(!this.goldMarkables){
        aggregateBuilder.add(PersonChainAnnotator.createAnnotatorDescription());
      }
      if(this.evalType == EVAL_SYSTEM.MENTION_PAIR){
        aggregateBuilder.add(EventCoreferenceAnnotator.createAnnotatorDescription(directory.getAbsolutePath() + File.separator + "model.jar"));
      }else if(this.evalType == EVAL_SYSTEM.CLUSTER_RANK){
        aggregateBuilder.add(MentionClusterRankingCoreferenceAnnotator.createAnnotatorDescription(directory.getAbsolutePath() + File.separator + "model.jar"));
      }else if(this.evalType == EVAL_SYSTEM.BASELINE){
        aggregateBuilder.add(CoreferenceAnnotatorFactory.getLegacyCoreferencePipeline());
      }else{
        logger.info("Running an evaluation that does not add an annotator: " + this.evalType);
      }
    }

    Function<CoreferenceRelation, ?> getSpan = new Function<CoreferenceRelation, HashableArguments>() {
      public HashableArguments apply(CoreferenceRelation relation) {
        return new HashableArguments(relation);
      }
    };
    Function<CoreferenceRelation, String> getOutcome = new Function<CoreferenceRelation,String>() {
      public String apply(CoreferenceRelation relation){
        return "Coreference";
      }
    };
     
    for(Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();){
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView(GOLD_VIEW_NAME);

      Collection<CoreferenceRelation> goldRelations = JCasUtil.select(
          goldView,
          CoreferenceRelation.class);
      Collection<CoreferenceRelation> systemRelations = JCasUtil.select(
          jCas,
          CoreferenceRelation.class);
      corefStats.add(goldRelations, systemRelations, getSpan, getOutcome);
      mentionStats.add(JCasUtil.select(goldView,  Markable.class), JCasUtil.select(jCas, Markable.class));

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
    System.out.println(String.format("P=%f, R=%f, F=%f", mentionStats.precision(), mentionStats.recall(), mentionStats.f1()));
    return corefStats;
  }
  
  protected AggregateBuilder getXMIWritingPreprocessorAggregateBuilder()
      throws Exception {
    
    AggregateBuilder preprocess = new AggregateBuilder();
    
    // Then run the preprocessing engine on all views
    preprocess.add(AnalysisEngineFactory.createEngineDescription( UriToDocumentTextAnnotatorCtakes.class ));

    preprocess.add(getLinguisticProcessingDescription());
    // Mapping explanation: Grab the text from the specific document URI and write to the gold view for this document
    preprocess.add(getGoldWritingAggregate(GOLD_VIEW_NAME));
    preprocess.add(AnalysisEngineFactory.createEngineDescription(DocumentIdFromURI.class));

    // write out the CAS after all the above annotations
    preprocess.add( AnalysisEngineFactory.createEngineDescription(
        XMIWriter.class,
        XMIWriter.PARAM_XMI_DIRECTORY,
        this.xmiDirectory ) );

    return preprocess;
  }
  
  protected AggregateBuilder getXMIReadingPreprocessorAggregateBuilder() throws UIMAException {
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        XMIReader.class,
        XMIReader.PARAM_XMI_DIRECTORY,
        this.xmiDirectory ) );
    return aggregateBuilder;
  }

  public static class AnnotationComparator implements Comparator<Annotation> {

    @Override
    public int compare(Annotation o1, Annotation o2) {
      if(o1.getBegin() < o2.getBegin()){
        return -1;
      }else if(o1.getBegin() == o2.getBegin() && o1.getEnd() < o2.getEnd()){
        return -1;
      }else if(o1.getBegin() == o2.getBegin() && o1.getEnd() > o2.getEnd()){
        return 1;
      }else if(o2.getBegin() < o1.getBegin()){
        return 1;
      }else{
        return 0;
      }
    }
  }
  public static class DocumentIDPrinter extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    static Logger logger = Logger.getLogger(DocumentIDPrinter.class);
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
       String docId = DocIdUtil.getDocumentID( jCas );
       if ( docId.startsWith( DocIdUtil.NO_DOCUMENT_ID ) ) {
        docId = new File(ViewUriUtil.getURI(jCas)).getName();
      }
      logger.info(String.format("Processing %s\n", docId));
    }
    
  }

  @PipeBitInfo(
        name = "Gold Markables Copier",
        description = "Copies Markables from the Gold view to the System view.",
        role = PipeBitInfo.Role.SPECIAL,
        dependencies = { PipeBitInfo.TypeProduct.MARKABLE }
  )
  public static class CopyGoldMarkablesInChains extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas goldView, systemView;
      try {
        goldView = jCas.getView( GOLD_VIEW_NAME );
        systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
      } catch ( CASException e ) {
        throw new AnalysisEngineProcessException( e );
      }
      // first remove any system markables that snuck in
      for ( Markable annotation : Lists.newArrayList( JCasUtil.select( systemView, Markable.class ) ) ) {
        annotation.removeFromIndexes();
      }

      CasCopier copier = new CasCopier( goldView.getCas(), systemView.getCas() );
      Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA );
      HashSet<String> existingSpans = new HashSet<>();
      for ( CollectionTextRelation chain : JCasUtil.select(goldView, CollectionTextRelation.class)){
        for ( Markable markable : JCasUtil.select(chain.getMembers(), Markable.class)){
          // some spans are annotated twice erroneously in gold -- if we can't fix make sure we don't add twice
          // or else the evaluation script will explode.
          String key = markable.getBegin() + "-" + (markable.getEnd() - markable.getBegin());
          if(existingSpans.contains(key)) continue;
          
          Markable copy = (Markable)copier.copyFs( markable );
          copy.setFeatureValue( sofaFeature, systemView.getSofa() );
          copy.addToIndexes( systemView );
          existingSpans.add(key);
        }
      }
    }
      
    
  }
  /*
   * The Relation extractors all create relation objects but don't populate the objects inside of them
   * with pointers to the relation.
   */
  public static class RelationPropagator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      for(LocationOfTextRelation locRel : JCasUtil.select(jcas, LocationOfTextRelation.class)){
        IdentifiedAnnotation arg1 = (IdentifiedAnnotation) locRel.getArg1().getArgument();
        IdentifiedAnnotation arg2 = (IdentifiedAnnotation) locRel.getArg2().getArgument();
        // have to do this 3 different times because there is no intermediate class between EventMention and
        // the three types that can have locations that has that location attribute.
        // for the case where there are 2 locations, we take the one whose anatomical site argument
        // has the the longer span assuming it is more specific
        if(arg1 instanceof ProcedureMention){
          ProcedureMention p = ((ProcedureMention)arg1);
          if(p.getBodyLocation() == null){
            p.setBodyLocation(locRel);
          }else{
            Annotation a = p.getBodyLocation().getArg2().getArgument();
            int oldSize = a.getEnd() - a.getBegin();
            int newSize = arg2.getEnd() - arg2.getEnd();
            if(newSize > oldSize){
              p.setBodyLocation(locRel);
            }
          }
        }else if(arg1 instanceof DiseaseDisorderMention){
          DiseaseDisorderMention d = (DiseaseDisorderMention)arg1;
          if(d.getBodyLocation() == null){
            d.setBodyLocation(locRel);
          }else{
            Annotation a = d.getBodyLocation().getArg2().getArgument();
            int oldSize = a.getEnd() - a.getBegin();
            int newSize = arg2.getEnd() - arg2.getEnd();
            if(newSize > oldSize){
              d.setBodyLocation(locRel);
            }
          }
        }else if(arg1 instanceof SignSymptomMention){
          SignSymptomMention s = (SignSymptomMention)arg1;
          if(s.getBodyLocation() == null){
            s.setBodyLocation(locRel);
          }else{
            Annotation a = s.getBodyLocation().getArg2().getArgument();
            int oldSize = a.getEnd() - a.getBegin();
            int newSize = arg2.getEnd() - arg2.getEnd();
            if(newSize > oldSize){
              s.setBodyLocation(locRel);
            }
          }          
        }
      }
    }
    
  }
  
  public static class ParagraphAnnotator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      List<BaseToken> tokens = new ArrayList<>(JCasUtil.select(jcas, BaseToken.class));
      BaseToken lastToken = null;
      int parStart = 0;
      
      for(int i = 0; i < tokens.size(); i++){
        BaseToken token = tokens.get(i);
        if(parStart == i && token instanceof NewlineToken){
          // we've just created a pargraph ending but there were multiple newlines -- don't want to start the
          // new paragraph until we are past the newlines -- increment the parStart index and move forward
          parStart++;
        }else if(lastToken != null && token instanceof NewlineToken){
          Paragraph par = new Paragraph(jcas, tokens.get(parStart).getBegin(), lastToken.getEnd());
          par.addToIndexes();
          parStart = i+1;
        }
        lastToken = token;
      }
      
    }
    
  }
  
  
  public static class ParagraphVectorAnnotator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    WordEmbeddings words = null;

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException{
      try {
        words = WordVectorReader.getEmbeddings(FileLocator.getAsStream("org/apache/ctakes/coreference/distsem/mimic_vectors.txt"));
      } catch (IOException e) {
        e.printStackTrace();
        throw new ResourceInitializationException(e);
      }
    }
    
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
      FSArray parVecs = new FSArray(jcas, pars.size());
      for(int parNum = 0; parNum < pars.size(); parNum++){
        Paragraph par = pars.get(parNum);
        float[] parVec = new float[words.getDimensionality()];

        List<BaseToken> tokens = JCasUtil.selectCovered(BaseToken.class, par);
        for(int i = 0; i < tokens.size(); i++){
          BaseToken token = tokens.get(i);
          if(token instanceof WordToken){
            String word = token.getCoveredText().toLowerCase();
            if(words.containsKey(word)){
              WordVector wv = words.getVector(word);
              for(int j = 0; j < parVec.length; j++){
                parVec[j] += wv.getValue(j);
              }
            }          
          }
        }
        normalize(parVec);
        FloatArray vec = new FloatArray(jcas, words.getDimensionality());
        vec.copyFromArray(parVec, 0, 0, parVec.length);
        vec.addToIndexes();
        parVecs.set(parNum, vec);
      }
      parVecs.addToIndexes();
    }

    private static final void normalize(float[] vec) {
      double sum = 0.0;
      for(int i = 0; i < vec.length; i++){
        sum += (vec[i]*vec[i]);
      }
      sum = Math.sqrt(sum);
      for(int i = 0; i < vec.length; i++){
        vec[i] /= sum;
      }
    }
  }

  @PipeBitInfo(
        name = "CrossDoc Coreference Copier",
        description = "Copies markables and relations from gold to system view",
        role = PipeBitInfo.Role.SPECIAL,
        dependencies = { PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.COREFERENCE_RELATION }
  )
  public static class CopyCrossDocCoreferenceRelations extends AbstractPatientConsumer {

    public CopyCrossDocCoreferenceRelations() {
      super("CopyCrossDocCoreferenceRelations", "Copy gold coreference relations from gold cas to system cas for training");
    }

    @Override
    public String getEngineName() {
      return "CopyCrossDocCoreferenceRelations";
    }

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
      super.initialize(context);
    }

    @Override
    protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {
      Collection<JCas> docCases = PatientViewUtil.getAllViews(patientJcas).
              stream().
              filter(s -> (s.getViewName().contains(CAS.NAME_DEFAULT_SOFA) && !s.getViewName().equals(CAS.NAME_DEFAULT_SOFA))).
              collect(Collectors.toList());
      Collection<JCas> goldCases = PatientViewUtil.getAllViews(patientJcas).
              stream().
              filter(s -> s.getViewName().contains(GOLD_VIEW_NAME)).
              collect(Collectors.toList());
      Map<Markable, Markable> gold2sys = new HashMap<>();

      // Map all markables in gold cases to equivalents in docCases
      for (JCas goldCas : goldCases) {
        JCas docCas = getAlignedDocCas(docCases, goldCas);
        if (docCas == null) {
          logger.error("Could not find aligned document CAS for this gold CAS.");
          throw new AnalysisEngineProcessException();
        }
        Map<ConllDependencyNode, List<Markable>> depIndex = JCasUtil.indexCovering(docCas, ConllDependencyNode.class, Markable.class);

        for (Markable goldMarkable : JCasUtil.select(goldCas, Markable.class)) {
          ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(docCas, goldMarkable);
          // for markables that are events, the headword and the markable itself have the same span -- we need to
          // expand those in the training data to match the
          if(headNode == null){
            logger.warn(String.format("The markable %s has no head node, probably because of poorly-segmented text.", goldMarkable.getCoveredText()));
            continue;
          }

          // map the gold markable to a system markable if they have the same headword
          boolean match = CopyCoreferenceRelations.mapGoldMarkable(docCas, goldMarkable, gold2sys, depIndex);
          if (!match) {
            logger.warn(String.format("There is a gold markable %s [%d, %d] which could not map to a system markable.",
                    goldMarkable.getCoveredText(), goldMarkable.getBegin(), goldMarkable.getEnd()));
          }
        }
      }
      // now go through all gold chains:
      for (JCas goldCas : goldCases) {
        JCas docCas = getAlignedDocCas(docCases, goldCas);
        if (docCas == null) {
          logger.error("Could not find aligned document CAS for this gold CAS.");
          throw new AnalysisEngineProcessException();
        }
        // create system chains from all the mapped markables
        for (CollectionTextRelation chain : JCasUtil.select(goldCas, CollectionTextRelation.class)) {
//          ArrayList<Markable> mappedElements = new ArrayList<>();
          List<Annotation> mappedElements = new ArrayList<>();
          for (Markable goldElement : JCasUtil.select(chain.getMembers(), Markable.class)) {
            // since we have cross-doc chains, the different markables in a chain may not be in the same gold cas as the
            // chain (which will share a cas with the _earliest_ mention).
            Markable sysElement = gold2sys.get(goldElement);
            if (sysElement != null) mappedElements.add(sysElement);
          }
          if (mappedElements.size() <= 1) {
            logger.warn("Gold chain did not have enough markables map to system markables.");
          } else {
            System.out.println("Mapped a gold chain to system using system markables:");
            System.out.print("     ");
//            for(Markable m : mappedElements){
            for(Annotation m : mappedElements){
              System.out.print(" -> " + m.getCoveredText());
            }
            System.out.println();
//            CollectionTextRelation sysChain = new CollectionTextRelation(docCas);
            CollectionRelation sysChain = new CollectionRelation(docCas);
            sysChain.setMembers(FSCollectionFactory.createFSList(docCas, mappedElements));
            sysChain.addToIndexes();
          }
        }
      }
    }

    private static JCas getAlignedDocCas(Collection<JCas> docCases, JCas goldCas) {
      JCas docCas = null;

      for (JCas candidate : docCases) {
        if (goldCas.getViewName().replace(GOLD_VIEW_NAME, CAS.NAME_DEFAULT_SOFA).equals(candidate.getViewName())) {
          docCas = candidate;
          break;
        }
      }
      return docCas;
    }
  }
  
  public static class RemoveAllCoreferenceAnnotations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    // TODO - make document aware so it can run with mention-cluster as intended
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      List<CollectionTextRelation> chains = new ArrayList<>(JCasUtil.select(jcas, CollectionTextRelation.class));
      for(CollectionTextRelation chain : chains){
        NonEmptyFSList head = null;
        FSList nextHead = chain.getMembers();
        do{
          head = (NonEmptyFSList) nextHead;
          head.removeFromIndexes();
          nextHead = head.getTail();
        }while(nextHead instanceof NonEmptyFSList);
        chain.removeFromIndexes();
      }
      List<CoreferenceRelation> rels = new ArrayList<>(JCasUtil.select(jcas, CoreferenceRelation.class));
      for(CoreferenceRelation rel : rels){
        rel.getArg1().removeFromIndexes();
        rel.getArg2().removeFromIndexes();
        rel.removeFromIndexes();
      }
    }    
  }
  
  public static class RemovePersonMarkables extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
//      JCas systemView=null, goldView=null;
//      try{
//        systemView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
//        goldView = jcas.getView(GOLD_VIEW_NAME);
//      }catch(Exception e){
//        throw new AnalysisEngineProcessException(e);
//      }
      List<Markable> toRemove = new ArrayList<>();
      for(Markable markable : JCasUtil.select(jcas, Markable.class)){
        if(markable.getCoveredText().equals("I")){
          System.err.println("Unauthorized markable 'I'");
        }
        List<BaseToken> coveredTokens = JCasUtil.selectCovered(jcas, BaseToken.class, markable);
        if(coveredTokens.size() == 1 && coveredTokens.get(0).getPartOfSpeech() != null &&
            coveredTokens.get(0).getPartOfSpeech().startsWith("PRP") &&
            !markable.getCoveredText().toLowerCase().equals("it")){
          toRemove.add(markable);
        }else if(coveredTokens.size() > 0 && (coveredTokens.get(0).getCoveredText().startsWith("Mr.") || coveredTokens.get(0).getCoveredText().startsWith("Dr.") ||
                coveredTokens.get(0).getCoveredText().startsWith("Mrs.") || coveredTokens.get(0).getCoveredText().startsWith("Ms.") || coveredTokens.get(0).getCoveredText().startsWith("Miss"))){
          toRemove.add(markable);
        }else if(markable.getCoveredText().toLowerCase().endsWith("patient") || markable.getCoveredText().toLowerCase().equals("pt")){
          toRemove.add(markable);
        }
      }
      
      for(Markable markable : toRemove){
        markable.removeFromIndexes();
      }
    } 
  }

  public static class EvaluationPatientNoteCollector extends JCasAnnotator_ImplBase {

    private final Logger LOGGER = Logger.getLogger( "EvaluationPatientNoteCollector" );

    /**
     * Adds the primary view of this cas to a cache of views for patients.
     * See {@link PatientNoteStore}
     * {@inheritDoc}
     */
    @Override
    public void process( final JCas jCas ) throws AnalysisEngineProcessException {
       LOGGER.info( "Caching Document " + PatientNoteStore.getDefaultDocumentId( jCas )
             + " into Patient " + PatientNoteStore.getDefaultPatientId( jCas ) + " ..." );

       PatientNoteStore.getInstance().storeAllViews( PatientNoteStore.getDefaultPatientId( jCas ),
             PatientNoteStore.getDefaultDocumentId( jCas ),
              jCas);

      LOGGER.info( "Finished." );
    }
  }

  public static class DocumentIdFromURI extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    @Override
    public void process(JCas docCas) throws AnalysisEngineProcessException {
      try {
        for (Iterator<JCas> it = docCas.getViewIterator(); it.hasNext(); ) {

          JCas jCas = it.next();
          String uri = new File(ViewUriUtil.getURI(jCas)).getName();
          // if some default DocumentID was added earlier, delete it:

          Collection<DocumentID> oldDocIDs = JCasUtil.select(jCas, DocumentID.class);
          for(DocumentID oldDocID : oldDocIDs) {
            oldDocID.removeFromIndexes();
          }
          DocumentID docId = new DocumentID(jCas);
          if(jCas.getViewName().equals(GOLD_VIEW_NAME)){
            docId.setDocumentID(GOLD_VIEW_NAME + "_" + uri);
          }else if(jCas.getViewName().equals(CAS.NAME_DEFAULT_SOFA)){
            docId.setDocumentID(uri);
          }else{
            docId.setDocumentID(jCas.getViewName() + "_" + uri);
          }
          docId.addToIndexes();

          DocumentIdPrefix docPrefix = new DocumentIdPrefix(jCas);
          docPrefix.setDocumentIdPrefix(uri.split("_")[0]);
          docPrefix.addToIndexes();
        }
      }catch(CASException e){
        throw new AnalysisEngineProcessException(e);
      }
    }
  }

  public static class PatientPersonChainAnnotator extends AbstractPatientConsumer {
    private PatientNoteStore notes = PatientNoteStore.INSTANCE;
    private PersonChainAnnotator delegate = new PersonChainAnnotator();

    public PatientPersonChainAnnotator(){
      super("PatientPersonAnnotator", "Finds links between person mentions in a patient-based CAS.");
    }

    @Override
    protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {
      for(JCas docView : PatientViewUtil.getDocumentViews(patientJcas)){
        delegate.process(docView);
      }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      delegate.collectionProcessComplete();
    }

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
      super.initialize(context);
      delegate.initialize(context);
    }

    @Override
    public void destroy() {
      super.destroy();
      delegate.destroy();
    }
  }

  public static class FlushingDataWriter extends LibLinearStringOutcomeDataWriter {

    int numChains = 0;

    public FlushingDataWriter(File outputDirectory)
        throws FileNotFoundException {
      super(outputDirectory);
    }
    
    @Override
    protected void writeEncoded(FeatureNode[] features, Integer outcome)
        throws CleartkProcessingException {
      //        this.trainingDataWriter.println("# Writing instance:");
      super.writeEncoded(features, outcome);
      //        this.trainingDataWriter.println("# Instance written");
      this.trainingDataWriter.flush();
    }
    
    @Override
    public void write(Instance<String> instance)
        throws CleartkProcessingException {
      if(instance.getOutcome().startsWith("#DEBUG")){
        this.trainingDataWriter.println(instance.getOutcome());
        this.trainingDataWriter.flush();
      }else{
        super.write(instance);
      }
    }
  }
}
