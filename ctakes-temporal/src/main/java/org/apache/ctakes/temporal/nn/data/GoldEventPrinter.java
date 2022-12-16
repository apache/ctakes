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
package org.apache.ctakes.temporal.nn.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Read cTAKES annotations from XMI files.
 *  
 * @author dmitriy dligach
 */
public class GoldEventPrinter {

  static interface Options {

    @Option(longName = "xmi-dir")
    public File getInputDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "output-train")
    public File getTrainOutputDirectory();

    @Option(longName = "output-test")
    public File getTestOutputDirectory();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);

    File trainFile = options.getTrainOutputDirectory();
    if(trainFile.exists()) {
      trainFile.delete();
    }
    trainFile.createNewFile();
    File devFile = options.getTestOutputDirectory();
    if(devFile.exists()) {
      devFile.delete();
    }
    devFile.createNewFile();

    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
    List<Integer> devItems = THYMEData.getPatientSets(patientSets, THYMEData.DEV_REMAINDERS);

    List<File> trainFiles = Utils.getFilesFor(trainItems, options.getInputDirectory());
    List<File> devFiles = Utils.getFilesFor(devItems, options.getInputDirectory());

    // sort training files to eliminate platform specific dir listings
    Collections.sort(trainFiles);

    // write training data to file
    CollectionReader trainCollectionReader = Utils.getCollectionReader(trainFiles);
    AnalysisEngine trainDataWriter = AnalysisEngineFactory.createEngine(
        EventPrinter.class,
        "OutputFile",
        trainFile.getAbsoluteFile());
    SimplePipeline.runPipeline(trainCollectionReader, trainDataWriter);

    // write dev data to file
    CollectionReader devCollectionReader = Utils.getCollectionReader(devFiles);
    AnalysisEngine devDataWriter = AnalysisEngineFactory.createEngine(
        EventPrinter.class,
        "OutputFile",
        devFile.getAbsolutePath());
    SimplePipeline.runPipeline(devCollectionReader, devDataWriter);
  }

  /**
   * Print events and entities.
   *  
   * @author dmitriy dligach
   */
  public static class EventPrinter extends JCasAnnotator_ImplBase {

    @ConfigurationParameter(
        name = "OutputFile",
        mandatory = true,
        description = "path to the output file")
    private String outputFile;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      // gold EventMention(s) are all in gold view
      JCas goldView;
      try {
        goldView = jCas.getView("GoldView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      // system view has sentence segmentation, tokens, and dictionary lookup events
      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      List<String> sentences = new ArrayList<>();
      for(Sentence sentence : JCasUtil.select(systemView, Sentence.class)) {
        List<String> tokensInThisSentence = new ArrayList<>();
        for(BaseToken baseToken : JCasUtil.selectCovered(systemView, BaseToken.class, sentence)) {
          List<EventMention> events = JCasUtil.selectCovering(goldView, EventMention.class, baseToken.getBegin(), baseToken.getEnd());
          String tokenText = tokenToString(baseToken);
          if(events.size() > 0) {
            tokensInThisSentence.add("[" + tokenText + "]");
          } else {
            tokensInThisSentence.add(tokenText);
          }
        }

        String sentenceAsString = String.join(" ", tokensInThisSentence).replaceAll("[\r\n]", " ");
        sentences.add(sentenceAsString);
      }

      try {
        Files.write(Paths.get(outputFile), sentences, StandardOpenOption.APPEND);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /*
   * Make sure this matches how data was pre-processed for word2vec
   */
  public static String tokenToString(BaseToken token) {

    String stringValue;
    String tokenType = token.getClass().getSimpleName();
    String tokenText = token.getCoveredText().toLowerCase();

    switch(tokenType) {
    case "ContractionToken":
      stringValue = tokenText;
      break;
    case "NewlineToken":
      // stringValue = null;
      stringValue = "";
      break;
    case "NumToken":
      stringValue = "number_token";
      break;
    case "PunctuationToken":
      stringValue = tokenText;
      break;
    case "SymbolToken":
      stringValue = tokenText;
      break;
    case "WordToken":
      stringValue = tokenText;
      break;
    default:
      throw new IllegalArgumentException("Invalid token type: " + tokenType);
    }

    return stringValue;
  }
}
