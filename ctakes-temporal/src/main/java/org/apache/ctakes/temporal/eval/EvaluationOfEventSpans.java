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
import java.util.logging.Level;

import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.ae.feature.selection.FeatureSelection;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.transform.InstanceDataWriter;
import org.cleartk.ml.feature.transform.InstanceStream;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class EvaluationOfEventSpans extends EvaluationOfAnnotationSpans_ImplBase {

  static interface Options extends Evaluation_ImplBase.Options {

    @Option(longName = "downratio", defaultValue = "1")
    public float getProbabilityOfKeepingANegativeExample();

    @Option(longName = "featureSelectionThreshold", defaultValue = "-1")
    public float getFeatureSelectionThreshold();

    @Option(longName = "SMOTENeighborNumber", defaultValue = "0")
    public float getSMOTENeighborNumber();
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
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
    
    List<Integer> allTraining = new ArrayList<>(trainItems);
    List<Integer> allTest = null;
    if (options.getTest()) {
      allTraining.addAll(devItems);
      allTest = new ArrayList<>(testItems);
    } else {
      allTest = new ArrayList<>(devItems);
    }
    EvaluationOfEventSpans evaluation = new EvaluationOfEventSpans(
        new File("target/eval/event-spans"),
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getSubcorpus(),
        options.getXMIDirectory(),
        options.getProbabilityOfKeepingANegativeExample(),
        options.getFeatureSelectionThreshold(),
        options.getSMOTENeighborNumber());
    evaluation.prepareXMIsFor(patientSets);
    evaluation.setLogging(Level.FINE, new File("target/eval/ctakes-event-errors.log"));
    if(options.getI2B2Output()!=null) evaluation.setI2B2Output(options.getI2B2Output() + "/event-spans");
    AnnotationStatistics<String> stats = evaluation.trainAndTest(allTraining, allTest);
    System.err.println(stats);
  }

  private float probabilityOfKeepingANegativeExample;

  private float featureSelectionThreshold;

  private float smoteNeighborNumber;

  public EvaluationOfEventSpans(
      File baseDirectory,
      File rawTextDirectory,
      File xmlDirectory,
      XMLFormat xmlFormat,
      Subcorpus subcorpus,
      File xmiDirectory,
      float probabilityOfKeepingANegativeExample,
      float featureSelectionThreshold,
      float numOfSmoteNeighbors) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory, EventMention.class);
    this.probabilityOfKeepingANegativeExample = probabilityOfKeepingANegativeExample;
    this.featureSelectionThreshold = featureSelectionThreshold;
    this.smoteNeighborNumber = numOfSmoteNeighbors;
  }

  @Override
  protected AnalysisEngineDescription getDataWriterDescription(File directory)
      throws ResourceInitializationException {
    Class<?> dataWriterClass = this.featureSelectionThreshold >= 0f
        ? InstanceDataWriter.class
        : LibLinearStringOutcomeDataWriter.class;
    return EventAnnotator.createDataWriterDescription(
        dataWriterClass,
        directory,
        this.probabilityOfKeepingANegativeExample,
        this.featureSelectionThreshold,
        this.smoteNeighborNumber);
  }

  @Override
  protected void trainAndPackage(File directory) throws Exception {
    if (this.featureSelectionThreshold > 0) {
      // Extracting features and writing instances
      Iterable<Instance<String>> instances = InstanceStream.loadFromDirectory(directory);
      // Collect MinMax stats for feature normalization
      FeatureSelection<String> featureSelection = EventAnnotator.createFeatureSelection(this.featureSelectionThreshold);
      featureSelection.train(instances);
      featureSelection.save(EventAnnotator.createFeatureSelectionURI(directory));
      // now write in the libsvm format
      LibLinearStringOutcomeDataWriter dataWriter = new LibLinearStringOutcomeDataWriter(directory);
      for (Instance<String> instance : instances) {
        dataWriter.write(featureSelection.transform(instance));
      }
      dataWriter.finish();
    }

    JarClassifierBuilder.trainAndPackage(directory, "-c", "0.05");
  }

  @Override
  protected AnalysisEngineDescription getAnnotatorDescription(File directory)
      throws ResourceInitializationException {
    return EventAnnotator.createAnnotatorDescription(directory);
  }

  @Override
  protected Collection<? extends Annotation> getGoldAnnotations(JCas jCas, Segment segment) {
    return selectExact(jCas, EventMention.class, segment);
  }

  @Override
  protected Collection<? extends Annotation> getSystemAnnotations(JCas jCas, Segment segment) {
    return selectExact(jCas, EventMention.class, segment);
  }
}
