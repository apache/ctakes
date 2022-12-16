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

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMIReader;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//import javax.annotation.Nullable;

public class PrintInconsistentAnnotations {
  static interface Options {
    @Option(longName = "xmi")
    public File getXMIDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "text")
    public File getRawTextDirectory();
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    int windowSize = 50;
    
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

    int totalDocTimeRels = 0;
    int totalInconsistentDocTimeRels = 0;
    for (Iterator<JCas> casIter = new JCasIterator(reader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      JCas jCas = casIter.next();
      String text = jCas.getDocumentText();
      JCas goldView = jCas.getView("GoldView");

      // group events by their narrative container
      Multimap<Annotation, EventMention> containers = HashMultimap.create();
      for (TemporalTextRelation relation : JCasUtil.select(goldView, TemporalTextRelation.class)) {
        if (relation.getCategory().equals("CONTAINS")) {
          Annotation arg1 = relation.getArg1().getArgument();
          Annotation arg2 = relation.getArg2().getArgument();
          if (arg2 instanceof EventMention) {
            EventMention event = (EventMention) arg2;
            containers.put(arg1, event);
          }
        }
      }

      // check each container for inconsistent DocTimeRels
      for (Annotation container : containers.keySet()) {
        Set<String> docTimeRels = Sets.newHashSet();
        for (EventMention event : containers.get(container)) {
          docTimeRels.add(event.getEvent().getProperties().getDocTimeRel());
        }
        totalDocTimeRels += docTimeRels.size();
        
        boolean inconsistentDocTimeRels;
        if (container instanceof EventMention) {
          EventMention mention = ((EventMention) container);
          String containerDocTimeRel = mention.getEvent().getProperties().getDocTimeRel();
          inconsistentDocTimeRels = false;
          for (String docTimeRel : docTimeRels) {
            if (docTimeRel.equals(containerDocTimeRel)) {
              continue;
            }
            if (containerDocTimeRel.equals("BEFORE/OVERLAP")
                && (docTimeRel.equals("BEFORE") || docTimeRel.equals("OVERLAP"))) {
              continue;
            }
            inconsistentDocTimeRels = true;
            break;
          }
        } else {
          if (docTimeRels.size() == 1) {
            inconsistentDocTimeRels = false;
          } else if (docTimeRels.contains("BEFORE/OVERLAP")) {
            inconsistentDocTimeRels =
                docTimeRels.size() == 1
                    && (docTimeRels.contains("BEFORE") || docTimeRels.contains("OVERLAP"));
          } else {
            inconsistentDocTimeRels = true;
          }
        }

        // if inconsistent: print events, DocTimeRels and surrounding context
        if (inconsistentDocTimeRels) {
          totalInconsistentDocTimeRels += docTimeRels.size();
          
          List<Integer> offsets = Lists.newArrayList();
          offsets.add(container.getBegin());
          offsets.add(container.getEnd());
          for (EventMention event : containers.get(container)) {
            offsets.add(event.getBegin());
            offsets.add(event.getEnd());
          }
          Collections.sort(offsets);
          int begin = Math.max(offsets.get(0) - windowSize, 0);
          int end = Math.min(offsets.get(offsets.size() - 1) + windowSize, text.length());
          System.err.printf(
              "Inconsistent DocTimeRels in %s, ...%s...\n",
              new File(ViewUriUtil.getURI(jCas)).getName(),
              text.substring(begin, end).replaceAll("([\r\n])[\r\n]+", "$1"));
          if (container instanceof EventMention) {
            System.err.printf(
                "Container: \"%s\" (docTimeRel=%s)\n",
                container.getCoveredText(),
                ((EventMention) container).getEvent().getProperties().getDocTimeRel());
          } else {
            System.err.printf("Container: \"%s\"\n", container.getCoveredText());
          }
          Ordering<EventMention> byBegin =
              Ordering.natural().onResultOf(new Function<EventMention, Integer>() {
                @Override
                // all of guava for two @Nullable ?  temporal has about 100 you be outdated errors and warnings.
                // findbugs is the least of worries.
//                public Integer apply(@Nullable EventMention event) {
                public Integer apply( EventMention event ) {
                  return event.getBegin();
                }
              });
          for (EventMention event : byBegin.sortedCopy(containers.get(container))) {
            System.err.printf(
                "* \"%s\" (docTimeRel=%s)\n",
                event.getCoveredText(),
                event.getEvent().getProperties().getDocTimeRel());
          }
          System.err.println();
        }
      }
    }
    
    System.err.printf(
        "Inconsistent DocTimeRels: %.1f%% (%d/%d)\n",
        100.0 * totalInconsistentDocTimeRels / totalDocTimeRels,
        totalInconsistentDocTimeRels,
        totalDocTimeRels);
  }
}
