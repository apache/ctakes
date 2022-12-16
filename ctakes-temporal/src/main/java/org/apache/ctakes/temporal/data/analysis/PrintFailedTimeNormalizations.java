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
package org.apache.ctakes.temporal.data.analysis;

import info.bethard.timenorm.DefaultTokenizer$;
import info.bethard.timenorm.Temporal;
import info.bethard.timenorm.TemporalExpressionParser;
import info.bethard.timenorm.TimeSpan;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMIReader;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import scala.util.Try;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class PrintFailedTimeNormalizations {
  static interface Options {
    @Option(longName = "xmi")
    public File getXMIDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "text")
    public File getRawTextDirectory();
  }

  private static Pattern DOC_TIME_PATTERN = Pattern.compile("rev_date=\"([^\"]+)\"");

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
    List<File> files = THYMEData.getFilesFor(trainItems, options.getRawTextDirectory());

    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(UriToDocumentTextAnnotator.getDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        XMIReader.class,
        XMIReader.PARAM_XMI_DIRECTORY,
        options.getXMIDirectory()));

    String grammarPath = "/org/apache/ctakes/temporal/timenorm.en.grammar";
    URL grammarURL = PrintFailedTimeNormalizations.class.getResource(grammarPath);
    TemporalExpressionParser parser = new TemporalExpressionParser(grammarURL, DefaultTokenizer$.MODULE$);
    for (Iterator<JCas> casIter = new JCasIterator(reader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView("GoldView");

      Matcher matcher = DOC_TIME_PATTERN.matcher(goldView.getDocumentText());
      if (!matcher.find()) {
        System.err.println(goldView.getDocumentText());
      } else {
        Try<Temporal> anchorTry = parser.parse(matcher.group(1), TimeSpan.of(1, 1, 1));
        if (anchorTry.isSuccess() && anchorTry.get() instanceof TimeSpan) {
          TimeSpan anchor = (TimeSpan)anchorTry.get();
          for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
            if (!THYMEData.SEGMENTS_TO_SKIP.contains(segment.getId())) {
              for (TimeMention time : JCasUtil.selectCovered(goldView, TimeMention.class, segment)) {
                String timeText = time.getCoveredText();
                Try<Temporal> parsedTime = parser.parse(timeText, anchor);
                if (parsedTime.isSuccess()) {
                  //System.err.printf("%s %s\n", timeText, parsedTime);
                } else {
                  System.err.println(timeText);
                  System.err.println(parsedTime);
                }
              }
            }
          }
        }
      }
    }
  }
}
