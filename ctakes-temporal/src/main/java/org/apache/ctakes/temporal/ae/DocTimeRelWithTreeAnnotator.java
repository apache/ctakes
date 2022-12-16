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
///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//package org.apache.ctakes.temporal.ae;
//
//import java.io.File;
//import java.util.List;
//
//import org.apache.ctakes.temporal.ae.feature.ClosestVerbExtractor;
//import org.apache.ctakes.temporal.ae.feature.DateAndMeasurementExtractor;
//import org.apache.ctakes.temporal.ae.feature.EventPropertyExtractor;
//import org.apache.ctakes.temporal.ae.feature.NearbyVerbTenseXExtractor;
//import org.apache.ctakes.temporal.ae.feature.SectionHeaderExtractor;
//import org.apache.ctakes.temporal.ae.feature.TimeXExtractor;
//import org.apache.ctakes.temporal.ae.feature.UmlsSingleFeatureExtractor;
////import org.apache.ctakes.temporal.ae.feature.duration.DurationExpectationFeatureExtractor;
////import org.apache.ctakes.temporal.ae.feature.treekernel.DependencySingleTreeExtractor;
////import org.apache.ctakes.temporal.ae.feature.treekernel.EventVerbPETExtractor;
////import org.apache.ctakes.temporal.ae.feature.treekernel.EventVerbTreeExtractor;
//import org.apache.ctakes.temporal.ae.feature.treekernel.SyntaticSingleTreeExtractor;
//import org.apache.ctakes.typesystem.type.syntax.BaseToken;
//import org.apache.ctakes.typesystem.type.textsem.EventMention;
//import org.apache.uima.UimaContext;
//import org.apache.uima.analysis_engine.AnalysisEngineDescription;
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.resource.ResourceInitializationException;
//import org.cleartk.ml.CleartkAnnotator;
//import org.cleartk.ml.DataWriter;
//import org.cleartk.ml.Feature;
//import org.cleartk.ml.Instance;
//import org.cleartk.ml.feature.extractor.CleartkExtractor;
//import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
//import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
//import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
//import org.cleartk.ml.feature.extractor.simple.CombinedExtractor;
//import org.cleartk.ml.feature.extractor.simple.CoveredTextExtractor;
//import org.cleartk.ml.feature.extractor.simple.TypePathExtractor;
//import org.cleartk.ml.jar.DefaultDataWriterFactory;
//import org.cleartk.ml.jar.DirectoryDataWriterFactory;
//import org.cleartk.ml.jar.GenericJarClassifierFactory;
//import org.apache.uima.fit.factory.AnalysisEngineFactory;
//import org.apache.uima.fit.util.JCasUtil;
//
//public class DocTimeRelWithTreeAnnotator extends CleartkAnnotator<String> {
//
//  public static AnalysisEngineDescription createDataWriterDescription(
//      Class<? extends DataWriter<String>> dataWriterClass,
//      File outputDirectory) throws ResourceInitializationException {
//    return AnalysisEngineFactory.createEngineDescription(
//        DocTimeRelWithTreeAnnotator.class,
//        CleartkAnnotator.PARAM_IS_TRAINING,
//        true,
//        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
//        dataWriterClass,
//        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
//        outputDirectory);
//  }
//
//  public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
//      throws ResourceInitializationException {
//    return AnalysisEngineFactory.createEngineDescription(
//        DocTimeRelWithTreeAnnotator.class,
//        CleartkAnnotator.PARAM_IS_TRAINING,
//        false,
//        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
//        new File(modelDirectory, "model.jar"));
//  }
//
//  private CleartkExtractor contextExtractor;
//  private NearbyVerbTenseXExtractor verbTensePatternExtractor;
//  private SectionHeaderExtractor sectionIDExtractor;
//  private ClosestVerbExtractor closestVerbExtractor;
//  private TimeXExtractor timeXExtractor;
//  private EventPropertyExtractor genericExtractor;
//  private DateAndMeasurementExtractor dateExtractor;
//  private UmlsSingleFeatureExtractor umlsExtractor;
////  private DurationExpectationFeatureExtractor durationExtractor;
//  
////  private DependencySingleTreeExtractor depTreeEctractor;
//  private SyntaticSingleTreeExtractor singleTreeExtractor;
////  private EventVerbTreeExtractor eventVerbTreeExtractor;
////  private EventVerbPETExtractor eventVerbPETExtractor;
//
//  @Override
//  public void initialize(UimaContext context) throws ResourceInitializationException {
//    super.initialize(context);
//    CombinedExtractor1 baseExtractor = new CombinedExtractor(
//        new CoveredTextExtractor(),
//        new TypePathExtractor(BaseToken.class, "partOfSpeech"));
//    this.contextExtractor = new CleartkExtractor(
//        BaseToken.class,
//        baseExtractor,
//        new Preceding(3),
//        new Covered(),
//        new Following(3));
//    this.verbTensePatternExtractor = new NearbyVerbTenseXExtractor();
//    this.sectionIDExtractor = new SectionHeaderExtractor();
//    this.closestVerbExtractor = new ClosestVerbExtractor();
//    this.timeXExtractor = new TimeXExtractor();
//    this.genericExtractor = new EventPropertyExtractor();
//    this.dateExtractor = new DateAndMeasurementExtractor();
//    this.umlsExtractor = new UmlsSingleFeatureExtractor();
////    this.durationExtractor = new DurationExpectationFeatureExtractor();
////    this.depTreeEctractor = new DependencySingleTreeExtractor();
//    this.singleTreeExtractor = new SyntaticSingleTreeExtractor();
////    this.eventVerbTreeExtractor = new EventVerbTreeExtractor();
////    this.eventVerbPETExtractor = new EventVerbPETExtractor();
//  }
//
//  @Override
//  public void process(JCas jCas) throws AnalysisEngineProcessException {
//    for (EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
//      if (eventMention.getEvent() != null) {
//        List<Feature> features = this.contextExtractor.extract(jCas, eventMention);
//        features.addAll(this.verbTensePatternExtractor.extract(jCas, eventMention));//add nearby verb POS pattern feature
//        features.addAll(this.sectionIDExtractor.extract(jCas, eventMention)); //add section heading
//        features.addAll(this.closestVerbExtractor.extract(jCas, eventMention)); //add closest verb
//        features.addAll(this.timeXExtractor.extract(jCas, eventMention)); //add the closest time expression types
//        features.addAll(this.genericExtractor.extract(jCas, eventMention)); //add the closest time expression types
//        features.addAll(this.dateExtractor.extract(jCas, eventMention)); //add the closest NE type
//        features.addAll(this.umlsExtractor.extract(jCas, eventMention)); //add umls features
////        features.addAll(this.durationExtractor.extract(jCas, eventMention)); //add duration feature
////        features.addAll(this.depTreeEctractor.extract(jCas, eventMention));
//        features.addAll(this.singleTreeExtractor.extract(jCas, eventMention));//add the single tree that covers the event.
////        features.addAll(this.eventVerbTreeExtractor.extract(jCas, eventMention)); // add the even-verb tree.
////        features.addAll(this.eventVerbPETExtractor.extract(jCas, eventMention)); //add the event-berb PET tree
//        if (this.isTraining()) {
//          String outcome = eventMention.getEvent().getProperties().getDocTimeRel();
//          this.dataWriter.write(new Instance<String>(outcome, features));
//        } else {
//          String outcome = this.classifier.classify(features);
//          eventMention.getEvent().getProperties().setDocTimeRel(outcome);
//        }
//      }
//    }
//  }
//}
