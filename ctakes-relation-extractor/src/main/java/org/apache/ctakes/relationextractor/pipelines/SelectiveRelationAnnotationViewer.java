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
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * View relation instances in xmi files. Only display the relations
 * whose arg1s are specified in a dictionary of CUIs.
 * 
 * @author dmitriy dligach
 */
public class SelectiveRelationAnnotationViewer {

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
  
	/*
	 * Displays the relations whose arg1 is specified in a dictionary file.
	 */
  public static class RelationExtractorPrinter extends JCasAnnotator_ImplBase {
    
    // file containing one cui per line
    private String dictionaryPath = "cuis.txt";
    private Set<String> cuiDictionary;
    
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);      
      cuiDictionary = getCustomizedDictionary(dictionaryPath);
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      JCas systemView;
      try {
        systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }   
      
      for(BinaryTextRelation binaryTextRelation : JCasUtil.select(systemView, BinaryTextRelation.class)) {
        IdentifiedAnnotation entity1; // entity whose role is "Argument"
        IdentifiedAnnotation entity2; // entity whose role is "Related_to"
        
        if(binaryTextRelation.getArg1().getRole().equals("Argument")) {
          entity1 = (IdentifiedAnnotation) binaryTextRelation.getArg1().getArgument();
          entity2 = (IdentifiedAnnotation) binaryTextRelation.getArg2().getArgument();
        } else {
          entity1 = (IdentifiedAnnotation) binaryTextRelation.getArg2().getArgument();
          entity2 = (IdentifiedAnnotation) binaryTextRelation.getArg1().getArgument();
        }
        
        String category = binaryTextRelation.getCategory();
        String arg1 = entity1.getCoveredText().toLowerCase();
        String arg2 = entity2.getCoveredText().toLowerCase();
        int type1 = entity1.getTypeID();
        int type2 = entity2.getTypeID();
        
        // first argument has to be an anatomical site
        if(type1 != CONST.NE_TYPE_ID_ANATOMICAL_SITE) {
          continue;
        }
        // skip location_of(anatomical site, anatomical site)
        if(type1 == CONST.NE_TYPE_ID_ANATOMICAL_SITE && type2 == CONST.NE_TYPE_ID_ANATOMICAL_SITE) {
          continue; 
        }
        // "to" is not a valid disease/disorder
        if(type2 == CONST.NE_TYPE_ID_DISORDER && arg2.equals("to")) {
          continue;
        }

        // print relations as long as arg1 exists in the dictionary
        Set<String> codes = getOntologyConceptCodes(entity1);
        codes.retainAll(cuiDictionary);
        if(codes.size() > 0) {
          // print relation and its arguments: location_of(colon/6, colon cancer/2)
          System.out.format("%s(%s/%d, %s/%d)\n", category, arg1, type1, arg2, type2);
          List<Sentence> sentences = JCasUtil.selectCovering(systemView, Sentence.class, entity1.getBegin(), entity1.getEnd());
          System.out.println(sentences.get(0).getCoveredText());
          System.out.println();
        }
      }
    }
  }
  
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

    // convert the List<File> to a String[]
    String[] paths = new String[items.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    // return a reader that will load each of the XMI files
    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
  
  /**
   * Get the CUIs, RxNorm codes specified for this entity.
   */
  public static HashSet<String> getOntologyConceptCodes(IdentifiedAnnotation identifiedAnnotation) {
    
    HashSet<String> codes = new HashSet<String>();
    
    FSArray fsArray = identifiedAnnotation.getOntologyConceptArr();
    if(fsArray == null) {
      return codes;
    }
    
    for(FeatureStructure featureStructure : fsArray.toArray()) {
      OntologyConcept ontologyConcept = (OntologyConcept) featureStructure;
      
      if(ontologyConcept instanceof UmlsConcept) {
        UmlsConcept umlsConcept = (UmlsConcept) ontologyConcept;
        String code = umlsConcept.getCui();
        codes.add(code);
      } else { // RxNorm
        String code = ontologyConcept.getCodingScheme() + ontologyConcept.getCode();
        codes.add(code);
      }
    }
    
    return codes;
  }
  
  /**
   * Read comma separate file containing target CUIs.
   */
  public static Set<String> getCustomizedDictionary(String path) {
    
    Set<String> cuis = new HashSet<String>();
    
    File file = new File(path);
    Scanner scan = null;
    try {
      scan = new Scanner(file);
    } catch (FileNotFoundException e) {
      System.err.println("couldn't open file: " + path);
      return cuis;
    }
    
    while(scan.hasNextLine()) {
      String line = scan.nextLine();
      cuis.add(line);
    }
    
    return cuis;
  }
}
