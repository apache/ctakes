/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.examples.pipeline;

import org.apache.ctakes.assertion.medfacts.cleartk.*;
import org.apache.ctakes.context.tokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.cr.FileTreeReader;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.lvg.ae.ThreadSafeLvg;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.ProcessTrace;

import java.io.IOException;

public class MultiThreadedPipeline {

  public static final int NUM_THREADS = 3;
  
  public static void main(String[] args) throws ResourceInitializationException {
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
//        FilesInDirectoryCollectionReader.class,
        FileTreeReader.class,
        ConfigParameterConstants.PARAM_INPUTDIR,
        "org/apache/ctakes/examples/annotation/anafora_annotated/" );
//        FilesInDirectoryCollectionReader.PARAM_RECURSE,
//        true);

    AnalysisEngineDescription aed = getThreadsafePipeline();
    CpeBuilder cpeBuilder = new CpeBuilder();
    try{
      cpeBuilder.setReader(reader);
      cpeBuilder.setAnalysisEngine(aed);
      cpeBuilder.setMaxProcessingUnitThreadCount(NUM_THREADS);
      cpeBuilder.getCpeDescription().getCpeCasProcessors().setPoolSize(NUM_THREADS);
      cpeBuilder.getCpeDescription().getCpeCasProcessors().setConcurrentPUCount(NUM_THREADS);
      CollectionProcessingEngine cpe = cpeBuilder.createCpe(null);
      cpe.addStatusCallbackListener(new UimaCallbackListener(cpe));
      cpe.process();
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  
  public static AnalysisEngineDescription getThreadsafePipeline() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    builder.add( SimpleSegmentAnnotator.createAnnotatorDescription() );
    builder.add( SentenceDetector.createAnnotatorDescription() );
    builder.add( TokenizerAnnotatorPTB.createAnnotatorDescription() );
    try{
      builder.add( ThreadSafeLvg.createAnnotatorDescription() );
    }catch(IOException e){
      throw new ResourceInitializationException(e);
    }
    builder.add( ContextDependentTokenizerAnnotator.createAnnotatorDescription() );
    builder.add( POSTagger.createAnnotatorDescription() );
    builder.add( DefaultJCasTermAnnotator.createAnnotatorDescription() );
    builder.add( ClearNLPDependencyParserAE.createAnnotatorDescription() );
    builder.add( PolarityCleartkAnalysisEngine.createAnnotatorDescription() );
    builder.add( UncertaintyCleartkAnalysisEngine.createAnnotatorDescription() );
    builder.add( HistoryCleartkAnalysisEngine.createAnnotatorDescription() );
    builder.add( ConditionalCleartkAnalysisEngine.createAnnotatorDescription() );
    builder.add( GenericCleartkAnalysisEngine.createAnnotatorDescription() );
    builder.add( SubjectCleartkAnalysisEngine.createAnnotatorDescription() );

    return builder.createAggregateDescription();
  }
  
  public static class UimaCallbackListener implements StatusCallbackListener {

    CollectionProcessingEngine cpe = null;
    long startTime;
    
    public UimaCallbackListener(CollectionProcessingEngine cpe) {
      this.cpe = cpe;
    }
    
    @Override
    public void initializationComplete() {
      System.out.println("CPE Initialization complete.");
      startTime = System.currentTimeMillis();
    }

    @Override
    public void batchProcessComplete() {
    }

    @Override
    public void collectionProcessComplete() {
      System.out.println("Processing complete!");
      
      ProcessTrace perf = cpe.getPerformanceReport();
      System.out.println("Performance: " + perf.toString());
      
      long duration = System.currentTimeMillis() - startTime;
      System.out.println("Total run time: " + duration + "ms");

    }

    @Override
    public void paused() {
    }

    @Override
    public void resumed() {
    }

    @Override
    public void aborted() {
    }

    @Override
    public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      try {
        JCas jcas = aCas.getJCas();
         String docId = DocIdUtil.getDeepDocumentId( jcas );
        System.out.println("Doc id for entity process complete: " + docId);
        System.out.println("Found " + JCasUtil.select(jcas, IdentifiedAnnotation.class).size() + " medical terms.");
        // The following was a bit verbose, but here's how you'd print out the text of each discovered entity span:
        /*
        for(IdentifiedAnnotation annot : JCasUtil.select(jcas, IdentifiedAnnotation.class)){
          System.out.println("Found entity: " + annot.getCoveredText());
        }
        */
      } catch (CASException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    
  }
}
