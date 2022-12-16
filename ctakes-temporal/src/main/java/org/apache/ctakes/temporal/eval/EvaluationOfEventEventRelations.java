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
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveCrossSentenceRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveRelations;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.testing.util.HideOutput;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.util.ViewUriUtil;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lexicalscope.jewel.cli.CliFactory;

public class EvaluationOfEventEventRelations extends
EvaluationOfTemporalRelations_ImplBase {

	private boolean baseline = false;
  public EvaluationOfEventEventRelations(      
      File baseDirectory,
      File rawTextDirectory,
      File xmlDirectory,
      XMLFormat xmlFormat,
      Subcorpus subcorpus,
      File xmiDirectory,
      File treebankDirectory,
      boolean printErrors,
      boolean printRelations,
      boolean baseline,
      ParameterSettings params){
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory,
        treebankDirectory, printErrors, printRelations, params);
    this.baseline = baseline;
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory) throws Exception
       {
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class, BinaryTextRelation.class));
    //	    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(MergeContainsOverlap.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveCrossSentenceRelations.class));
    // TODO -- see if this applies to this relation:
    //	    if (this.useClosure) {
    //	      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(AddTransitiveContainsRelations.class));
    //	    }
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PreserveEventEventRelations.class));
    aggregateBuilder.add(EventEventRelationAnnotator.createDataWriterDescription(
        LibSvmStringOutcomeDataWriter.class,
//        	        TKSVMlightStringOutcomeDataWriter.class,
        directory,
        1.0));
    SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());

    HideOutput hider = new HideOutput();
    // libsvm:
    JarClassifierBuilder.trainAndPackage(directory,  "-t", "0", "-c", "10");
    // tksvmlight with no tk features:
//    JarClassifierBuilder.trainAndPackage(directory, "-t", "0", "-c", "10", "-N", "0");
//    JarClassifierBuilder.trainAndPackage(directory,  "-t", "5", "-S", "0", "-N", "3", "-C", "+", "-T", "1.0");
    hider.restoreOutput();
  }

  @Override
  protected AnnotationStatistics<String> test(
      CollectionReader collectionReader, File directory) throws Exception {
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class));
    //	    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(MergeContainsOverlap.class,
    //	    		MergeContainsOverlap.PARAM_RELATION_VIEW,
    //	    		GOLD_VIEW_NAME));
    aggregateBuilder.add(
        AnalysisEngineFactory.createEngineDescription(RemoveNonContainsRelations.class),
        CAS.NAME_DEFAULT_SOFA,
        GOLD_VIEW_NAME);
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        RemoveCrossSentenceRelations.class,
        RemoveCrossSentenceRelations.PARAM_SENTENCE_VIEW,
        CAS.NAME_DEFAULT_SOFA,
        RemoveCrossSentenceRelations.PARAM_RELATION_VIEW,
        GOLD_VIEW_NAME));
    // TODO - use if relevant.
    //	    if (this.useClosure) {
    //	      aggregateBuilder.add(
    //	          AnalysisEngineFactory.createEngineDescription(AddTransitiveContainsRelations.class),
    //	          CAS.NAME_DEFAULT_SOFA,
    //	          GOLD_VIEW_NAME);
    //	    }
    aggregateBuilder.add(
        AnalysisEngineFactory.createEngineDescription(PreserveEventEventRelations.class),
        CAS.NAME_DEFAULT_SOFA,
        GOLD_VIEW_NAME);

    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemoveRelations.class));
    aggregateBuilder.add(
        EventEventRelationAnnotator.createAnnotatorDescription(directory.getAbsolutePath())
    );

    Function<BinaryTextRelation, ?> getSpan = new Function<BinaryTextRelation, HashableArguments>() {
      public HashableArguments apply(BinaryTextRelation relation) {
        return new HashableArguments(relation);
      }
    };
    Function<BinaryTextRelation, String> getOutcome = AnnotationStatistics.annotationToFeatureValue("category");

    AnnotationStatistics<String> stats = new AnnotationStatistics<String>();
    for(Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();){
      JCas jCas = casIter.next();
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

  public static void main(String[] args) throws Exception{
    TempRelOptions options = CliFactory.parseArguments(TempRelOptions.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = getTrainItems(options);
    List<Integer> testItems = getTestItems(options);

    File workingDir = new File("target/eval/temporal-relations/event-event/");
    ParameterSettings params = defaultParams;
    EvaluationOfEventEventRelations evaluation = new EvaluationOfEventEventRelations(
        workingDir,
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getSubcorpus(),
        options.getXMIDirectory(),
        options.getTreebankDirectory(),
        options.getPrintErrors(),
        options.getPrintFormattedRelations(),
        options.getBaseline(),
        params);
    evaluation.prepareXMIsFor(patientSets);

    AnnotationStatistics<String> stats = evaluation.trainAndTest(trainItems, testItems);
    System.err.println(stats);
  }

}
