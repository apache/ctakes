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
package org.apache.ctakes.temporal.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.CRFTimeAnnotator;
import org.apache.ctakes.temporal.ae.ConstituencyBasedTimeAnnotator;
import org.apache.ctakes.temporal.ae.MetaTimeAnnotator;
import org.apache.ctakes.temporal.ae.TimeAnnotator;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.crfsuite.CrfSuiteStringOutcomeDataWriter;
import org.cleartk.ml.jar.JarClassifierBuilder;

import com.google.common.collect.Maps;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class EvaluationOfMetaTimeExpressionExtractor extends EvaluationOfAnnotationSpans_ImplBase {
  public static int nFolds = 5;
  private List<Integer> allTrain = null;
  private boolean skipTrainComponents = false;
  
  interface MetaOptions extends Options {
    @Option
    boolean getSkipTrainComponents();  
  }

  public EvaluationOfMetaTimeExpressionExtractor(File baseDirectory,
      File rawTextDirectory, File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat,
      Subcorpus subcorpus, File xmiDirectory, File treebankDirectory,
      List<Integer> allTrain, Class<? extends Annotation> annotationClass) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory,
        treebankDirectory, annotationClass);
    this.allTrain = allTrain;
  }

  public static void main(String[] args) throws Exception {
    MetaOptions options = CliFactory.parseArguments(MetaOptions.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = null;
    List<Integer> devItems = null;
    List<Integer> testItems = null;
    if(options.getXMLFormat() == XMLFormat.I2B2){
      trainItems = I2B2Data.getTrainPatientSets(options.getXMLDirectory());
      devItems = I2B2Data.getDevPatientSets(options.getXMLDirectory());
      testItems = I2B2Data.getTestPatientSets(options.getXMLDirectory());
    }else{
      trainItems = THYMEData.getPatientSets(patientSets, options.getTrainRemainders().getList());
      devItems = THYMEData.getPatientSets(patientSets, options.getDevRemainders().getList());
      testItems = THYMEData.getPatientSets(patientSets, options.getTestRemainders().getList());
    }
    List<Integer> allTrain = new ArrayList<>(trainItems);
    List<Integer> allTest = null;
    
    if(options.getTest()){
      allTrain.addAll(devItems);
      allTest = new ArrayList<>(testItems);
    }else{
      allTest = new ArrayList<>(devItems);
    }

    EvaluationOfMetaTimeExpressionExtractor eval = new
        EvaluationOfMetaTimeExpressionExtractor(
            new File("target/eval/time-spans"), 
            options.getRawTextDirectory(), 
            options.getXMLDirectory(), 
            options.getXMLFormat(),
            options.getSubcorpus(),
            options.getXMIDirectory(), 
            options.getTreebankDirectory(),
            allTrain,
            TimeMention.class);
    if(options.getSkipTrainComponents()) eval.setSkipTrainComponents(true);
    if(options.getI2B2Output()!=null) eval.setI2B2Output(options.getI2B2Output());
    if(options.getPrintOverlappingSpans()) eval.printOverlapping = true;
    AnnotationStatistics<String> stats = eval.trainAndTest(allTrain, allTest);
    System.out.println(stats.toString());
  }

  private void setSkipTrainComponents(boolean skip) {
    this.skipTrainComponents = skip;
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    
    if(!this.skipTrainComponents){
      Class<? extends JCasAnnotator_ImplBase>[] annotatorClasses = MetaTimeAnnotator.getComponents();

      // add more annotator types?
      Map<Class<? extends JCasAnnotator_ImplBase>, String[]> annotatorTrainingArguments = Maps.newHashMap();
      annotatorTrainingArguments.put(BackwardsTimeAnnotator.class, new String[]{"-c", "0.1"});
      annotatorTrainingArguments.put(TimeAnnotator.class, new String[]{"-c", "0.1"});
      annotatorTrainingArguments.put(ConstituencyBasedTimeAnnotator.class, new String[]{"-c", "0.3"});
      annotatorTrainingArguments.put(CRFTimeAnnotator.class, new String[]{"-p", "c2=0.3"});

      JCasIterator[] casIters = new JCasIterator[nFolds];
      for (int fold = 0; fold < nFolds; ++fold) {
        List<Integer> xfoldTrain = selectTrainItems(allTrain, nFolds, fold);
        List<Integer> xfoldTest = selectTestItems(allTrain, nFolds, fold);
        AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
        File modelDirectory = getModelDirectory(new File("target/eval/time-spans/fold_"+fold));
        for (Class<? extends JCasAnnotator_ImplBase> annotatorClass : annotatorClasses) {
          EvaluationOfTimeSpans evaluation = new EvaluationOfTimeSpans(
              new File("target/eval/time-spans/" ),
              this.rawTextDirectory,
              this.xmlDirectory,
              this.xmlFormat,
              this.subcorpus,
              this.xmiDirectory,
              this.treebankDirectory,
              1,
              0,
              annotatorClass,
              false,
              annotatorTrainingArguments.get(annotatorClass));
          evaluation.prepareXMIsFor(allTrain);
          String name = String.format("%s.errors", annotatorClass.getSimpleName());
          evaluation.setLogging(Level.FINE, new File("target/eval", name));

          // train on 4 of the folds of the training data:
          evaluation.train(evaluation.getCollectionReader(xfoldTrain), modelDirectory);
          if(fold == 0){
            // train the main model as well:
            evaluation.train(evaluation.getCollectionReader(allTrain), directory);
          }

        }
        casIters[fold] = new JCasIterator(getCollectionReader(xfoldTest), aggregateBuilder.createAggregate());
      }
      // run meta data-writer for this fold:
      AggregateBuilder writerBuilder = new AggregateBuilder();
      writerBuilder.add(CopyFromGold.getDescription(TimeMention.class));
      writerBuilder.add(this.getDataWriterDescription(directory));
      AnalysisEngine writer = writerBuilder.createAggregate();
      for(JCasIterator casIter : casIters){
        while(casIter.hasNext()){
          JCas jcas = casIter.next();
          SimplePipeline.runPipeline(jcas, writer);
        }
      }
      writer.collectionProcessComplete();
    }
    JarClassifierBuilder.trainAndPackage(getModelDirectory(directory), new String[]{"-p", "c2=3.0"});
  }
  
  private static List<Integer> selectTrainItems(List<Integer> items, int numFolds, int fold) {
    List<Integer> trainItems = new ArrayList<>();
    for (int i = 0; i < items.size(); ++i) {
      if (i % numFolds != fold) {
        trainItems.add(items.get(i));
      }
    }
    return trainItems;
  }
  
  private static List<Integer> selectTestItems(List<Integer> items, int numFolds, int fold) {
    List<Integer> trainItems = new ArrayList<>();
    for (int i = 0; i < items.size(); ++i) {
      if (i % numFolds == fold) {
        trainItems.add(items.get(i));
      }
    }
    return trainItems;
  }
  

  @Override
  protected AnalysisEngineDescription getDataWriterDescription(File directory)
      throws ResourceInitializationException {
    return MetaTimeAnnotator.getDataWriterDescription(CrfSuiteStringOutcomeDataWriter.class, directory);          
  }

  @Override
  protected void trainAndPackage(File directory) throws Exception {
    System.err.println("\n\n***\n\n\nChanging the classifier setup\n\n\n");
    JarClassifierBuilder.trainAndPackage(getModelDirectory(directory), "-p", "c2=0.3");
  }

  @Override
  protected AnalysisEngineDescription getAnnotatorDescription(File directory)
      throws ResourceInitializationException {
    return MetaTimeAnnotator.getAnnotatorDescription(directory);
  }

  @Override
  protected Collection<? extends Annotation> getGoldAnnotations(JCas jCas,
      Segment segment) {
    return selectExact(jCas, TimeMention.class, segment);
  }

  @Override
  protected Collection<? extends Annotation> getSystemAnnotations(JCas jCas,
      Segment segment) {
    return selectExact(jCas, TimeMention.class, segment);
  }

  private static File getModelDirectory(File directory) {
    return new File(directory, "MetaTimeAnnotator");
  }

}
