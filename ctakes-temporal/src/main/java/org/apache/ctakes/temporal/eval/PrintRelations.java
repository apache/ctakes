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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ctakes.temporal.ae.THYMEKnowtatorXMLReader;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class PrintRelations {

  interface Options {

    @Option(longName = "text")
    public File getRawTextDirectory();

    @Option(longName = "xml")
    public File getKnowtatorXMLDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();
  }

  public static void main(String[] args) throws Exception {

    // parse command line options
    Options options = CliFactory.parseArguments(Options.class, args);
    File rawTextDirectory = options.getRawTextDirectory();
    File knowtatorXMLDirectory = options.getKnowtatorXMLDirectory();
    List<Integer> patientSets = options.getPatients().getList();

    // collect the files for all the patients
    List<File> files = new ArrayList<File>();
    for (Integer set : patientSets) {
      File subDir = new File(rawTextDirectory, "doc" + set);
      files.addAll(Arrays.asList(subDir.listFiles()));
    }

    // construct reader and Knowtator XML parser
    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(UriToDocumentTextAnnotator.getDescription());
    aggregateBuilder.add(THYMEKnowtatorXMLReader.getDescription(knowtatorXMLDirectory));

    // walk through each document in the collection
    for (Iterator<JCas> casIter = new JCasIterator(reader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      JCas jCas = casIter.next();
      System.err.println(ViewUriUtil.getURI(jCas));

      // collect all relations and sort them by the order they appear in the text
      Collection<BinaryTextRelation> relations = JCasUtil.select(jCas, BinaryTextRelation.class);
      List<BinaryTextRelation> relationList = new ArrayList<BinaryTextRelation>(relations);
      Collections.sort(relationList, BY_RELATION_OFFSETS);

      for (IdentifiedAnnotation identifiedAnnotation : JCasUtil.select(jCas, IdentifiedAnnotation.class)) {
    	if (identifiedAnnotation instanceof EventMention || identifiedAnnotation instanceof EntityMention) {
    		System.err.printf("%s (%s)\n", identifiedAnnotation.getCoveredText(), identifiedAnnotation.getTypeID());
    	}
      }

      // print out the relations for visual inspection
      // for (BinaryTextRelation relation : relationList) {
      // Annotation source = relation.getArg1().getArgument();
      // Annotation target = relation.getArg2().getArgument();
      // String type = relation.getCategory();
      // System.err.printf("%s(%s,%s)\n", type, source.getCoveredText(), target.getCoveredText());
      // }
      System.err.println();
    }
  }

  /**
   * Orders relations to match their order in the text (as defined by the spans of their arguments)
   */
  private static final Ordering<BinaryTextRelation> BY_RELATION_OFFSETS = Ordering.<Integer> natural().lexicographical().onResultOf(
      new Function<BinaryTextRelation, Set<Integer>>() {
        @Override
        public Set<Integer> apply(BinaryTextRelation relation) {
          Annotation arg1 = relation.getArg1().getArgument();
          Annotation arg2 = relation.getArg2().getArgument();
          return new TreeSet<Integer>(Arrays.asList(arg1.getBegin(), arg2.getBegin()));
        }
      });
}
