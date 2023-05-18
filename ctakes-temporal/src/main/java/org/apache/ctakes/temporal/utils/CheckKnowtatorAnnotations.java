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
package org.apache.ctakes.temporal.utils;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.temporal.ae.THYMEKnowtatorXMLReader;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.SegmentsFromBracketedSectionTagsAnnotator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Checks Knowtator XML for possible annotation errors   
 */
public class CheckKnowtatorAnnotations {

  private static interface Options {

    @Option(longName = "text")
    public File getRawTextDirectory();

    @Option(longName = "xml")
    public File getKnowtatorXMLDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<File> files = new ArrayList<File>();
    for (Integer set : options.getPatients().getList()) {
      File setTextDirectory = new File(options.getRawTextDirectory(), "doc" + set);
      for (File file : setTextDirectory.listFiles()) {
        files.add(file);
      }
    }
    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(UriToDocumentTextAnnotator.getDescription());
    builder.add(AnalysisEngineFactory.createEngineDescription(
        THYMEKnowtatorXMLReader.class,
        THYMEKnowtatorXMLReader.PARAM_KNOWTATOR_XML_DIRECTORY,
        options.getKnowtatorXMLDirectory()));
    builder.add(AnalysisEngineFactory.createEngineDescription(SegmentsFromBracketedSectionTagsAnnotator.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(
            SentenceDetector.class,
            SentenceDetector.SD_MODEL_FILE_PARAM,
            "org/apache/ctakes/core/models/sentdetect/sd-med-model.zip"));
    builder.add(AnalysisEngineFactory.createEngineDescription(TokenizerAnnotatorPTB.class));
    for (Iterator<JCas> casIter = new JCasIterator(reader, builder.createAggregate()); casIter.hasNext();){
      JCas jCas = casIter.next();
      for (EventMention event : JCasUtil.select(jCas, EventMention.class)) {
        List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, event);
        if (tokens.size() > 1) {
          System.err.printf(
              "Multi-token event \"%s\" in %s\n",
              event.getCoveredText(),
              ViewUriUtil.getURI(jCas));
        }
        if (event.getCoveredText().contains(" ")) {
          System.err.printf(
              "Whitespace in event \"%s\" in %s\n",
              event.getCoveredText(),
              ViewUriUtil.getURI(jCas));
        }
      }
    }
  }
}
