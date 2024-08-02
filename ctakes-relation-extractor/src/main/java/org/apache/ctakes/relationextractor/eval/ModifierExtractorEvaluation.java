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

import com.google.common.collect.Lists;
import com.lexicalscope.jewel.cli.CliFactory;
import org.apache.ctakes.relationextractor.ae.ModifierExtractorAnnotator;
import org.apache.ctakes.typesystem.type.textsem.Modifier;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;

import java.io.File;
import java.util.*;

public class ModifierExtractorEvaluation extends RelationEvaluation_ImplBase {

  public static final ParameterSettings BEST_PARAMETERS = new ParameterSettings(
      LibLinearStringOutcomeDataWriter.class,
      new String[] { "-s", "0", "-c", "100.0" });

  public static void main(String[] args) throws Exception {
    // parse the options, validate them, and generate XMI if necessary
    final RelationExtractorEvaluation.Options options = CliFactory.parseArguments(RelationExtractorEvaluation.Options.class, args);
    CorpusXMI.validate(options);
    if(options.getGenerateXMI()) {
      boolean generateSharp = false, generateDeepPhe = false;
      if (options.getTestCorpus() == CorpusXMI.Corpus.SHARP || options.getTestCorpus() == CorpusXMI.Corpus.SHARP_RELEASE) {
        generateSharp = true;
//      } else if (options.getTestCorpus() == CorpusXMI.Corpus.DeepPhe) {
//        generateDeepPhe = true;
      }
      for(CorpusXMI.Corpus corpus : options.getTrainCorpus()){
        if(corpus == CorpusXMI.Corpus.SHARP_RELEASE || corpus == CorpusXMI.Corpus.SHARP){
          generateSharp = true;
//        }else if(corpus == CorpusXMI.Corpus.DeepPhe){
//          generateDeepPhe = true;
        }
      }

      if(generateSharp){
        SHARPXMI.generateXMI(options.getXMIDirectory(), options.getSharpCorpusDirectory(), options.getSharpBatchesDirectory());
      }
//      if(generateDeepPhe){
//        DeepPheXMI.generateXMI(options.getXMIDirectory(), options.getDeepPheAnaforaDirectory());
//      }
    }

    // determine the grid of parameters to search through
    // for the full set of LibLinear parameters, see:
    // https://github.com/bwaldvogel/liblinear-java/blob/master/src/main/java/de/bwaldvogel/liblinear/Train.java
    List<ParameterSettings> gridOfSettings = Lists.newArrayList();
    for (int solver : new int[] { 0 /* logistic regression */, 1 /* SVM */}) {
      for (double svmCost : new double[] { 0.01, 0.05, 0.1, 0.5, 1, 5, 10, 50, 100 }) {
        gridOfSettings.add(new ParameterSettings(
            LibLinearStringOutcomeDataWriter.class,
            new String[] { "-s", String.valueOf(solver), "-c", String.valueOf(svmCost) }));
      }
    }

    List<File> trainFiles = new ArrayList<>();
    for(CorpusXMI.Corpus corpus : options.getTrainCorpus()){
      File trainCorpusDirectory;
      if(corpus == CorpusXMI.Corpus.SHARP) trainCorpusDirectory = options.getSharpBatchesDirectory();
      else if(corpus == CorpusXMI.Corpus.SHARP_RELEASE) trainCorpusDirectory = options.getSharpCorpusDirectory();
//      else if(corpus == CorpusXMI.Corpus.DeepPhe) trainCorpusDirectory = options.getDeepPheAnaforaDirectory();
      else{
        throw new Exception("Train corpus not recognized: " + corpus);
      }
      trainFiles.addAll(CorpusXMI.toXMIFiles(options.getXMIDirectory(), CorpusXMI.getTrainTextFiles(corpus, options.getEvaluateOn(), trainCorpusDirectory)));
    }

    File testCorpusDirectory=null;
    if(options.getTestCorpus() == CorpusXMI.Corpus.SHARP) testCorpusDirectory = options.getSharpBatchesDirectory();
    else if(options.getTestCorpus() == CorpusXMI.Corpus.SHARP_RELEASE) testCorpusDirectory = options.getSharpCorpusDirectory();
//    else if(options.getTestCorpus() == CorpusXMI.Corpus.DeepPhe) testCorpusDirectory = options.getDeepPheAnaforaDirectory();

    List<File> testFiles = CorpusXMI.getTestTextFiles(options.getTestCorpus(), options.getEvaluateOn(), testCorpusDirectory);

    if(options.getGridSearch()){
      Map<ParameterSettings, Double> scoredParams = new HashMap<>();
      for(ParameterSettings params : gridOfSettings){
        ModifierExtractorEvaluation eval = new ModifierExtractorEvaluation(new File("target/models/modifier"), params);
        params.stats = eval.trainAndTest(trainFiles, testFiles);
        scoredParams.put(params, params.stats.f1());
      }
    }else {
      ModifierExtractorEvaluation eval = new ModifierExtractorEvaluation(new File("target/models/modifier"), BEST_PARAMETERS);
      System.err.println(eval.trainAndTest(trainFiles, testFiles));
    }
  }

  private ParameterSettings parameterSettings;

  public ModifierExtractorEvaluation(File directory, ParameterSettings parameterSettings) {
    super(directory);
    this.parameterSettings = parameterSettings;
  }

  @Override
  public void train(CollectionReader collectionReader, File directory) throws Exception {
    System.err.printf("%s: %s:\n", this.getClass().getSimpleName(), directory.getName());
    System.err.println(this.parameterSettings);

    SimplePipeline.runPipeline(
        collectionReader,
        AnalysisEngineFactory.createEngineDescription(OnlyGoldModifiers.class),
        ModifierExtractorAnnotator.getDescription(
            DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
            this.parameterSettings.dataWriterClass,
            DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
            directory.getPath()));
    JarClassifierBuilder.trainAndPackage(directory, this.parameterSettings.trainingArguments);
  }

  @Override
  protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
      throws Exception {
    AnalysisEngine classifierAnnotator =
        AnalysisEngineFactory.createEngine(ModifierExtractorAnnotator.getDescription(
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            JarClassifierBuilder.getModelJarFile(directory)));

    AnnotationStatistics<String> stats = new AnnotationStatistics<String>();
    for (Iterator<JCas> casIter = new JCasIterator(collectionReader, classifierAnnotator); casIter.hasNext();) {
      JCas jCas = casIter.next();
      JCas goldView;
      try {
        goldView = jCas.getView(SHARPXMI.GOLD_VIEW_NAME);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }
      Collection<Modifier> goldModifiers = JCasUtil.select(goldView, Modifier.class);
      Collection<Modifier> systemModifiers = JCasUtil.select(jCas, Modifier.class);
      stats.add(goldModifiers, systemModifiers);
    }
    System.err.print(stats);
    System.err.println();
    return stats;
  }

  /**
   * Class that copies the manual {@link Modifier} annotations to the default
   * CAS.
   */
  public static class OnlyGoldModifiers extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas goldView;
      try {
        goldView = jCas.getView(SHARPXMI.GOLD_VIEW_NAME);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      // remove any automatically generated Modifiers
      for (Modifier modifier : JCasUtil.select(jCas, Modifier.class)) {
        modifier.removeFromIndexes();
      }

      // copy over the manually annotated Modifiers
      for (Modifier goldModifier : JCasUtil.select(goldView, Modifier.class)) {
        Modifier modifier = new Modifier(jCas, goldModifier.getBegin(), goldModifier.getEnd());
        modifier.setTypeID(goldModifier.getTypeID());
        modifier.setId(goldModifier.getId());
        modifier.setDiscoveryTechnique(goldModifier.getDiscoveryTechnique());
        modifier.setConfidence(goldModifier.getConfidence());
        modifier.addToIndexes();
      }
    }
  }
}
