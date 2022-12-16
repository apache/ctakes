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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMIReader;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class RunCorpusDiagnostics {
	static interface Options {

		@Option(longName = "text")
		public File getRawTextDirectory();

		@Option(longName = "xmi")
		public File getXMIDirectory();

		@Option(longName = "patients")
		public CommandLine.IntegerRanges getPatients();

		@Option(longName = "treebank", defaultToNull=true)
		public File getTreebankDirectory();
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
		CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(getFilesFor(options.getRawTextDirectory(), trainItems));
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add(UriToDocumentTextAnnotator.getDescription());
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
				XMIReader.class,
				XMIReader.PARAM_XMI_DIRECTORY,
				options.getXMIDirectory()));

		CounterMap<String> timeClassCounts = new CounterMap<String>();
		
		JCasIterator casIter = new JCasIterator(reader, aggregateBuilder.createAggregate());
		while(casIter.hasNext()){
			JCas jcas = casIter.next();
			JCas goldView = jcas.getView(Evaluation_ImplBase.GOLD_VIEW_NAME);
			Collection<TemporalTextRelation> rels = JCasUtil.select(goldView, TemporalTextRelation.class);
			for(BinaryTextRelation rel : rels){
				Annotation arg1 = rel.getArg1().getArgument();
				Annotation arg2 = rel.getArg2().getArgument();
				String timeClass = null;
				if(arg1 instanceof TimeMention && arg2 instanceof EventMention){
					timeClass = ((TimeMention) arg1).getTimeClass();
				}else if(arg1 instanceof EventMention && arg2 instanceof TimeMention){
					timeClass = ((TimeMention) arg2).getTimeClass();
				}
				
				if(timeClass != null){
					timeClassCounts.add(rel.getCategory()+"--"+timeClass);
				}
//				System.out.println("Found relation: " + rel.getCategory());
			}
		}
		
		for(String key : timeClassCounts.keySet()){
			System.out.println(key + " => " + timeClassCounts.get(key));
		}
	}

	private static List<File> getFilesFor(File rawTextDirectory, List<Integer> patientSets) {
		List<File> files = new ArrayList<File>();
		for (Integer set : patientSets) {
			File setTextDirectory = new File(rawTextDirectory, "doc" + set);
			for (File file : setTextDirectory.listFiles()) {
				// skip hidden files like .svn
				if (!file.isHidden()) {
					files.add(file);
				}
			}
		}
		return files;
	}

}
