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
package org.apache.ctakes.temporal.nn.eval;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.baselines.RecallBaselineEventTimeRelationAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.eval.EvaluationOfTemporalRelations_ImplBase;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.temporal.eval.I2B2Data;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.temporal.keras.KerasStringOutcomeDataWriter;
import org.apache.ctakes.temporal.keras.ScriptStringFeatureDataWriter;
import org.apache.ctakes.temporal.nn.ae.EventTimePositionBasedAnnotator;
import org.apache.ctakes.temporal.utils.AnnotationIdCollection;
import org.apache.ctakes.temporal.utils.TLinkTypeArray2;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
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

public class EventTimeNeuralEvaluation extends
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
    public boolean getTestOnTrain();

    @Option
    public boolean getSkipWrite();
  }

  protected static ParameterSettings flatParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "linear",
      10.0, 1.0, "linear", ComboOperator.VECTOR_ONLY, DEFAULT_TK, DEFAULT_LAMBDA);
  protected static ParameterSettings allBagsParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk", 
      100.0, 0.1, "radial basis function", ComboOperator.SUM, 0.5, 0.5);
  protected static ParameterSettings allParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk",
      10.0, 1.0, "polynomial", ComboOperator.SUM, 0.1, 0.5);  // (0.3, 0.4 for tklibsvm)
  protected static ParameterSettings ftParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "tk", 
      1.0, 0.1, "radial basis function", ComboOperator.SUM, 0.5, 0.5);
  private static Boolean recallModeEvaluation = true;

  static int sysRelationCount;
  static int closeRelationCount;
  static int goldRelationCount;
  static int closeGoldRelationCount;

  public static void main(String[] args) throws Exception {
    sysRelationCount = 0;
    closeRelationCount = 0;
    goldRelationCount = 0;
    closeGoldRelationCount = 0;

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

    try{
      File workingDir = new File("target/eval/thyme/");
      if(!workingDir.exists()) workingDir.mkdirs();
      if(options.getUseTmp()){
        File tempModelDir = File.createTempFile("temporal", null, workingDir);
        tempModelDir.delete();
        tempModelDir.mkdir();
        workingDir = tempModelDir;
      }
      EventTimeNeuralEvaluation evaluation = new EventTimeNeuralEvaluation(
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

      if(options.getI2B2Output()!=null) evaluation.setI2B2Output(options.getI2B2Output() + "/temporal-relations/event-time");
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
      System.err.println(params.stats);

      System.err.println("System predict relations #: "+ sysRelationCount);
      System.err.println("# of system relations whose arguments are close: "+ closeRelationCount);
      System.err.println("Gold relations #: "+ goldRelationCount);
      System.err.println("# of gold relations whose arguments are close: "+ closeGoldRelationCount);

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
  protected boolean testOnTrain=false;
  //  protected boolean printRelations = false;

  public EventTimeNeuralEvaluation(
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

  @Override
  protected void train(CollectionReader collectionReader, File directory) throws Exception {

    if(this.skipTrain) return;

    if(!this.skipWrite){
      AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
      aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class, BinaryTextRelation.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveCrossSentenceRelations.class));
      if(!this.useGoldAttributes){
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveGoldAttributes.class));
      }
      if (this.useClosure) {
      }

      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(Overlap2Contains.class));

      aggregateBuilder.add(
          AnalysisEngineFactory.createEngineDescription(EventTimePositionBasedAnnotator.class,
              CleartkAnnotator.PARAM_IS_TRAINING,
              true,
              DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
              KerasStringOutcomeDataWriter.class,
              DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
              new File(directory,"event-time"),
              ScriptStringFeatureDataWriter.PARAM_SCRIPT_DIR,
              "scripts/nn/"
              ) );

      SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
    }
    
    JarClassifierBuilder.trainAndPackage(new File(directory,"event-time"));//, weightArray
  }

  @Override
  protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
      throws Exception {
    
    this.useClosure=false;//don't do closure for test
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
          AnalysisEngineFactory.createEngineDescription(AddClosure.class),
          CAS.NAME_DEFAULT_SOFA,
          GOLD_VIEW_NAME);
    }

    aggregateBuilder.add(
        AnalysisEngineFactory.createEngineDescription(RemoveEventEventRelations.class),
        CAS.NAME_DEFAULT_SOFA,
        GOLD_VIEW_NAME);

    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class),
        CAS.NAME_DEFAULT_SOFA,
        GOLD_VIEW_NAME);
    //		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonUMLSEtEvents.class));

    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveRelations.class));
    aggregateBuilder.add(this.baseline ? RecallBaselineEventTimeRelationAnnotator.createAnnotatorDescription(directory) :
      AnalysisEngineFactory.createEngineDescription(EventTimePositionBasedAnnotator.class,
          CleartkAnnotator.PARAM_IS_TRAINING,
          false,
          GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
          new File(new File(directory,"event-time"), "model.jar").getPath()));

    //count how many system predicted relations, their arguments are close to each other, without any other event in between
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CountCloseRelation.class));

    if(this.anaforaOutput != null){
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(WriteAnaforaXML.class, WriteAnaforaXML.PARAM_OUTPUT_DIR, this.anaforaOutput), "TimexView", CAS.NAME_DEFAULT_SOFA);
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
      outf =  new File("target/eval/thyme/SystemError_eventTime_recall_test.txt");
    }else if (!recallModeEvaluation && this.useClosure){
      outf =  new File("target/eval/thyme/SystemError_eventTime_precision_test.txt");
    }else{
      outf =  new File("target/eval/thyme/SystemError_eventTime_plain_test.txt");
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

  public static class RemoveNonUMLSEtEvents extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
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
        Annotation argEvent = null;
        if(arg1 instanceof EventMention && arg2 instanceof TimeMention){
          argEvent = arg1;
        }else if(arg1 instanceof TimeMention && arg2 instanceof EventMention){
          argEvent = arg2;
        }else{
          continue;
        }
        boolean eventValid = false;
        for (EventMention event : JCasUtil.selectCovered(sysView, EventMention.class, argEvent)){
          if(!event.getClass().equals(EventMention.class)){
            eventValid = true;
            break;
          }
        }
        if(eventValid){
          // these are the kind we keep.
          continue;
        }
        arg1.removeFromIndexes();
        arg2.removeFromIndexes();
        relation.removeFromIndexes();
      }
    }   
  }

  public static class RemoveEventEventRelations extends JCasAnnotator_ImplBase {
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
        RelationArgument arg1 = relation.getArg1();
        RelationArgument arg2 = relation.getArg2();
        if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention ||
            arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention){
          // these are the kind we keep.
          continue;
        }
        arg1.removeFromIndexes();
        arg2.removeFromIndexes();
        relation.removeFromIndexes();
      }
    }
  }

   @PipeBitInfo(
         name = "E-T Relation Overlapper",
         description = "Adds Event-Time temporal relations for annotations overlapping those already having relations.",
         role = PipeBitInfo.Role.SPECIAL,
         dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.TIMEX,
                          PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
   )
  public static class AddPotentialRelations extends JCasAnnotator_ImplBase {
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

      Map<EventMention, Collection<EventMention>> coveringMap =
          JCasUtil.indexCovering(relationView, EventMention.class, EventMention.class);
      for(TemporalTextRelation relation : Lists.newArrayList(JCasUtil.select(relationView, TemporalTextRelation.class))){
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        EventMention event = null;
        if(arg1 instanceof EventMention && arg2 instanceof TimeMention){
          event = (EventMention) arg1;
          Collection<EventMention> eventList = coveringMap.get(event);
          for(EventMention covEvent : eventList){
            if(!covEvent.getClass().equals(EventMention.class)){
              createRelation(relationView, covEvent, arg2, relation.getCategory());
            }
          }
        }else if(arg2 instanceof EventMention && arg1 instanceof TimeMention){
          event = (EventMention) arg2;
          Collection<EventMention> eventList = coveringMap.get(event);
          for(EventMention covEvent : eventList){
            if(!covEvent.getClass().equals(EventMention.class)){
              createRelation(relationView, arg1, covEvent, relation.getCategory());
            }
          }
        }
      }

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
      TemporalTextRelation relation = new TemporalTextRelation(jCas);
      relation.setArg1(relArg1);
      relation.setArg2(relArg2);
      relation.setCategory(category);
      relation.addToIndexes();

    }
  }

  public static class RemoveCrossSentenceRelations extends JCasAnnotator_ImplBase {

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

   @PipeBitInfo(
         name = "Closed TLink Counter",
         description = "Counts the number of TLinks that have shares Events or Times in the Gold view.",
         role = PipeBitInfo.Role.SPECIAL,
         dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
   )
  public static class CountCloseRelation extends JCasAnnotator_ImplBase {

    private String systemViewName = CAS.NAME_DEFAULT_SOFA;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas systemView, goldView;
      int sizeLimit = 6;
      try {
        systemView = jCas.getView(this.systemViewName);
        goldView = jCas.getView(GOLD_VIEW_NAME);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      //count how many sentences have timex, and how many sentences have only one timex
      for (TemporalTextRelation relation : JCasUtil.select(systemView, TemporalTextRelation.class)) {
        sysRelationCount ++;
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        if( arg1.getBegin()> arg2.getBegin()){
          Annotation temp = arg1;
          arg1 = arg2;
          arg2 = temp;
        }
        List<WordToken> words = JCasUtil.selectBetween(systemView, WordToken.class, arg1, arg2);
        if(words.size()<sizeLimit){
          closeRelationCount++;
        }
      }

      Map<List<Annotation>, TemporalTextRelation> relationLookup = new HashMap<>();
      for (TemporalTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, TemporalTextRelation.class))) {
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        // The key is a list of args so we can do bi-directional lookup
        List<Annotation> key = Arrays.asList(arg1, arg2);
        if(!relationLookup.containsKey(key)){
          relationLookup.put(key, relation);
        }
      }

      //count how many sentences have timex, and how many sentences have only one timex
      for (TemporalTextRelation relation : relationLookup.values()) {
        goldRelationCount ++;
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        if( arg1.getBegin()> arg2.getBegin()){
          Annotation temp = arg1;
          arg1 = arg2;
          arg2 = temp;
        }
        List<WordToken> words = JCasUtil.selectBetween(systemView, WordToken.class, arg1, arg2);
        if(words.size()<sizeLimit){
          closeGoldRelationCount++;
        }
      }
    }
  }

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
         name = "Transitive Contains Adder",
         description = "Adds Contains temporal relations for annotations / relations in contain other relations.",
         role = PipeBitInfo.Role.SPECIAL,
         dependencies = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
   )
  public static class AddTransitiveContainsRelations extends JCasAnnotator_ImplBase {

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
  public static class AddContain2Overlap extends JCasAnnotator_ImplBase {

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

  /**
   * For conflicting temporal relations on the same pair of arguments, if some of the relation types are "overlap", and "contains", remove "overlap" and keep "contains" 
   * @author CH151862
   *
   */
  public static class Overlap2Contains extends JCasAnnotator_ImplBase {

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
      Map<List<Annotation>, BinaryTextRelation> relationLookup;
      relationLookup = new HashMap<>();
      for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(relationView, BinaryTextRelation.class))) {
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        String relationType = relation.getCategory();
        // The key is a list of args so we can do bi-directional lookup
        List<Annotation> key = Arrays.asList(arg1, arg2);
        if(relationLookup.containsKey(key)){
          BinaryTextRelation storedRel = relationLookup.get(key);
          String reln = storedRel.getCategory();
          if(!relationType.equals(reln)){//if there is category conflicts
            if(relationType.startsWith("OVERLAP") ){//current relation is too general, remove it
              relation.getArg1().removeFromIndexes();
              relation.getArg2().removeFromIndexes();
              relation.removeFromIndexes();
            }else if( reln.startsWith("OVERLAP") ){//stored relation is too general, remove it
              //remove duplicate:
              storedRel.getArg1().removeFromIndexes();
              storedRel.getArg2().removeFromIndexes();
              storedRel.removeFromIndexes();
              relationLookup.put(key, relation);
            }else if(relationType.startsWith("CONTAINS")){//contain is dominant
              storedRel.getArg1().removeFromIndexes();
              storedRel.getArg2().removeFromIndexes();
              storedRel.removeFromIndexes(jCas);
              relationLookup.put(key, relation);
            }else if(reln.startsWith("CONTAINS")){
              relation.getArg1().removeFromIndexes();
              relation.getArg2().removeFromIndexes();
              relation.removeFromIndexes(jCas);
            }else{
              relation.getArg1().removeFromIndexes();
              relation.getArg2().removeFromIndexes();
              relation.removeFromIndexes();
            }
          }else{//if there is no conflicting, remove duplicating relations
            relation.getArg1().removeFromIndexes();
            relation.getArg2().removeFromIndexes();
            relation.removeFromIndexes(jCas);
          }
        }else{//if the relation is new, then added it to lookup
          relationLookup.put(key, relation);
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

      ArrayList<BinaryTextRelation> temporalRelation = new ArrayList<>(annotationsToRelation.values());

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
}
