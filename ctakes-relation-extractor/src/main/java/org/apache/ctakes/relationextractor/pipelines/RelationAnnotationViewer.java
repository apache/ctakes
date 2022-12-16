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
package org.apache.ctakes.relationextractor.pipelines;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * View the annotations obtained by running the relation extractor on clinical notes.
 * The results of running the relation extractor should to be saved in xmi files.
 * 
 * @author dmitriy dligach
 */
public class RelationAnnotationViewer {

  public static class Options {

    @Option(
        name = "--input-dir",
        usage = "specify the path to the directory containing the clinical notes to be processed",
        required = true)
    public File inputDirectory;
  }
  
	public static void main(String[] args) throws Exception {
		
		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		List<File> trainFiles = Arrays.asList(options.inputDirectory.listFiles());
    CollectionReader collectionReader = getCollectionReader(trainFiles);
		
    AnalysisEngine relationExtractorPrinter = AnalysisEngineFactory.createEngine(
    		RelationExtractorPrinter.class);
    		
		SimplePipeline.runPipeline(collectionReader, relationExtractorPrinter);
	}
  
  public static class RelationExtractorPrinter extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      
      JCas systemView;
      try {
        systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }   
            
      for(BinaryTextRelation binaryTextRelation : JCasUtil.select(systemView, BinaryTextRelation.class)) {

        IdentifiedAnnotation entity1 = (IdentifiedAnnotation) binaryTextRelation.getArg1().getArgument();
        IdentifiedAnnotation entity2 = (IdentifiedAnnotation) binaryTextRelation.getArg2().getArgument();

        String category = binaryTextRelation.getCategory();

        String arg1 = entity1.getCoveredText().toLowerCase();
        String arg2 = entity2.getCoveredText().toLowerCase();
        
        List<Sentence> enclosingSentences = JCasUtil.selectCovering(
            systemView, 
            Sentence.class,
            entity1.getBegin(), 
            entity2.getEnd());
        
        System.out.format("%s|%s|%s|%s\n", category, arg1, arg2, enclosingSentences.get(0).getCoveredText());
      }
    }
  }
  
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

    String[] paths = new String[items.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
}
