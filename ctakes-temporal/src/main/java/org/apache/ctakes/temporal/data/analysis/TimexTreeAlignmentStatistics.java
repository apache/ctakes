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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMIReader;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class TimexTreeAlignmentStatistics {
  static interface Options{
    @Option(longName = "xmi")
    public File getXMIDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "text")
    public File getRawTextDirectory();
  }
  
  /**
   * @param args
   * @throws IOException 
   * @throws UIMAException 
   */
  public static void main(String[] args) throws UIMAException, IOException {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
    //List<Integer> devItems = THYMEData.getDevPatientSets(patientSets);
    //List<Integer> testItems = THYMEData.getTestPatientSets(patientSets);

    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(THYMEData.getFilesFor(trainItems, options.getRawTextDirectory()));
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(UriToDocumentTextAnnotator.getDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        XMIReader.class,
        XMIReader.PARAM_XMI_DIRECTORY,
        options.getXMIDirectory()));
    AnalysisEngine ae = aggregateBuilder.createAggregate();
    int numMentions=0;
    int numMatches=0;
    
    for(Iterator<JCas> casIter = new JCasIterator(reader, ae); casIter.hasNext();){
      //      String docId = DocumentIDAnnotationUtil.getDocumentID(jCas);
      //      String docId = jCas.
      //      System.out.println("Document: " + docId);
      JCas jCas = casIter.next();
      for(Segment segment : JCasUtil.select(jCas, Segment.class)){
    	  if(THYMEData.SEGMENTS_TO_SKIP.contains(segment.getId())) continue;
        Collection<TimeMention> mentions = JCasUtil.selectCovered(jCas.getView("GoldView"), TimeMention.class, segment);
        for(TimeMention mention : mentions){
          numMentions++;
          boolean match = false;
          List<TreebankNode> nodes = JCasUtil.selectCovered(jCas, TreebankNode.class, mention);
          for(TreebankNode node : nodes){
            if(node.getBegin() == mention.getBegin() && node.getEnd() == mention.getEnd()){
              numMatches++;
              match = true;
              break;
            }
          }
          if(!match){
            List<TreebankNode> coveringNodes = JCasUtil.selectCovering(jCas, TreebankNode.class, mention.getBegin(), mention.getEnd());
            TreebankNode smallestCoveringNode = null;
            int smallestLen = Integer.MAX_VALUE;
            for(TreebankNode node : coveringNodes){
              int len = node.getEnd() - node.getBegin();
              if(len <  smallestLen){
                smallestLen = len;
                smallestCoveringNode = node;
              }
            }
            System.out.println("No alignment for: " + mention.getCoveredText());
            System.out.println("  Smallest covering treebank node is: " + (smallestCoveringNode == null ? "null" : smallestCoveringNode.getCoveredText()));
            System.out.println("  " + (smallestCoveringNode == null ? "no tree" : TreeUtils.tree2str(smallestCoveringNode)));
          }
        }
      }
    }
    System.out.printf("Found %d mentions, %d match with node spans\n", numMentions, numMatches);
  }


}
