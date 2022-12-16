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
package org.apache.ctakes.assertion.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.assertion.eval.AssertionEvaluation;
import org.apache.ctakes.assertion.eval.AssertionEvaluation.ReferenceAnnotationsSystemAssertionClearer;
import org.apache.ctakes.assertion.eval.AssertionEvaluation.ReferenceIdentifiedAnnotationsSystemToGoldCopier;
import org.apache.ctakes.assertion.eval.AssertionEvaluation.ReferenceSupportingAnnotationsSystemToGoldCopier;
import org.apache.ctakes.assertion.eval.XMIReader;
import org.apache.ctakes.assertion.medfacts.cleartk.AlternateCuePhraseAnnotator;
import org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.ConditionalCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.AssertionFeatureAwareDataWriter;
import org.apache.ctakes.assertion.medfacts.cleartk.GenericCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.HistoryCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.SubjectCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

public class WriteMultipleDatasets {
  public static class Options {
    @Option(
        name = "--data-dirs",
        usage = "specify the colon-separated directories containing the XMI training files (for example, /NLP/Corpus/Relations/mipacq/xmi/train)",
        required = true)
    public String dataDirectories;

    @Option(
        name = "--models-dir",
        usage = "specify the directory where the models will be placed",
        required = false)
    public File modelsDirectory=new File("target/domain-adaptation");

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

    @Option(
        name = "--ignore-history",
        usage = "specify whether 'history of' processing should be run (true or false). default: false",
        required = false)
    public boolean ignoreHistory = false;
  }

  public static void main(String[] args) throws CmdLineException, UIMAException, IOException {
    Options options = new Options();
    CmdLineParser parser = new CmdLineParser(options);
    parser.parseArgument(args);

    List<File> paths = new ArrayList<>();
    for(String dataDirname : options.dataDirectories.split(":")){
      File dataDir = new File(dataDirname.trim());
      File[] domainFiles = dataDir.listFiles(new java.io.FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().endsWith(".xmi");
        }
      });
      paths.addAll(Arrays.asList(domainFiles));
    }

    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
        XMIReader.class,
        TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(),
        XMIReader.PARAM_FILES,
        paths);
    AnalysisEngineDescription aed = getPipeline(options, options.dataDirectories);
    SimplePipeline.runPipeline(reader, aed);
  }

  private static AnalysisEngineDescription getPipeline(Options options, String dirs) throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();

    AnalysisEngineDescription goldCopierIdentifiedAnnotsAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceIdentifiedAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierIdentifiedAnnotsAnnotator);

    AnalysisEngineDescription goldCopierSupportingAnnotsAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceSupportingAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierSupportingAnnotsAnnotator);

    AnalysisEngineDescription assertionAttributeClearerAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceAnnotationsSystemAssertionClearer.class);
    builder.add(assertionAttributeClearerAnnotator);

    // Set up Feature Selection parameters
    Class<? extends DataWriter<String>> dataWriterClassFirstPass = AssertionFeatureAwareDataWriter.class;

    // Add each assertion Analysis Engine to the pipeline!
    builder.add(AnalysisEngineFactory.createEngineDescription(AlternateCuePhraseAnnotator.class, new Object[]{}));

    if (!options.ignorePolarity)
    {
      AnalysisEngineDescription polarityAnnotator;
      polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityCleartkAnalysisEngine.class); //,  this.additionalParamemters);
      ConfigurationParameterFactory.addConfigurationParameters(
          polarityAnnotator,
          AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
          AssertionEvaluation.GOLD_VIEW_NAME,
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClassFirstPass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(options.modelsDirectory, "polarity").getPath(),
          AssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
          AssertionCleartkAnalysisEngine.FEATURE_CONFIG.ALL_SYN,
          AssertionCleartkAnalysisEngine.FILE_TO_DOMAIN_MAP,
          dirs
          );
      builder.add(polarityAnnotator);
    }

    if (!options.ignoreConditional)
    {
      AnalysisEngineDescription conditionalAnnotator = AnalysisEngineFactory.createEngineDescription(ConditionalCleartkAnalysisEngine.class); //,  this.additionalParamemters);
      ConfigurationParameterFactory.addConfigurationParameters(
          conditionalAnnotator,
          AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
          AssertionEvaluation.GOLD_VIEW_NAME,
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClassFirstPass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(options.modelsDirectory, "conditional").getPath());
      builder.add(conditionalAnnotator);
    }

    if (!options.ignoreUncertainty)
    {
      AnalysisEngineDescription uncertaintyAnnotator = AnalysisEngineFactory.createEngineDescription(UncertaintyCleartkAnalysisEngine.class); //,  this.additionalParamemters);
      ConfigurationParameterFactory.addConfigurationParameters(
          uncertaintyAnnotator,
          AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
          AssertionEvaluation.GOLD_VIEW_NAME,
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClassFirstPass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(options.modelsDirectory, "uncertainty").getPath(),
          AssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
          AssertionCleartkAnalysisEngine.FEATURE_CONFIG.ALL_SYN
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
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClassFirstPass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(options.modelsDirectory, "subject").getPath()
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
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClassFirstPass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(options.modelsDirectory, "generic").getPath()
          );
      builder.add(genericAnnotator);
    }

    if (!options.ignoreHistory) {
      AnalysisEngineDescription historyAnnotator = AnalysisEngineFactory.createEngineDescription(HistoryCleartkAnalysisEngine.class);
      ConfigurationParameterFactory.addConfigurationParameters(
          historyAnnotator,
          AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
          AssertionEvaluation.GOLD_VIEW_NAME,
          DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClassFirstPass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(options.modelsDirectory, "historyOf").getPath()
          );
      builder.add(historyAnnotator);
    }
    return builder.createAggregateDescription();
  }
}
