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
package org.apache.ctakes.assertion.pipelines;

import org.apache.ctakes.assertion.eval.XMIReader;
import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.assertion.util.AssertionTreeUtils.extractAboveLeftConceptTree;

public class GenerateTreeRepresentation{

  enum ATTRIBUTE {NEG, UNC}
  
	public static class Options {

		@Option(
				name = "--train-dir",
				usage = "specify the directory containing the XMI training files (for example, /NLP/Corpus/Relations/mipacq/xmi/train)",
				required = true)
		public File trainDirectory;
		
		@Option(
				name = "--output",
				usage = "The file to which the data points be written.",
				required = true)
		public File outFile;
		
		@Option(name = "--attribute", required=false)
		public ATTRIBUTE attributeType = ATTRIBUTE.NEG;
	}
	
	protected static Options options = new Options();
	private static SemanticClasses sems = null; 
	private static PrintStream out = null;
	private static Logger log = Logger.getLogger(GenerateTreeRepresentation.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws UIMAException 
	 */
	public static void main(String[] args) throws UIMAException, IOException {
	    CmdLineParser parser = new CmdLineParser(options);
	    try {
        parser.parseArgument(args);
      } catch (CmdLineException e) {
        e.printStackTrace();
        System.exit(-1);
      }
	    
	    if(sems == null){
	      sems = new SemanticClasses(FileLocator.getAsStream("org/apache/ctakes/assertion/all_cues.txt"));
	    }
	    out = new PrintStream(options.outFile);
	    List<File> trainFiles = Arrays.asList(options.trainDirectory.listFiles());

	    String[] paths = new String[trainFiles.size()];
	    for (int i = 0; i < paths.length; ++i) {
	      paths[i] = trainFiles.get(i).getPath();
	    }
	    CollectionReader reader = CollectionReaderFactory.createReader(
	            XMIReader.class,
	            XMIReader.PARAM_FILES,
	            paths);
	       
	    JCasIterator casIter = new JCasIterator(reader);
	    while(casIter.hasNext()){
	    	JCas jcas = casIter.next();
	    	processDocument(jcas);
	    }
	    out.close();
	}

	private static void processDocument(JCas jcas) {
      log.info( "Processing document: " + DocIdUtil.getDocumentID( jcas ) );
		Collection<IdentifiedAnnotation> mentions = JCasUtil.select(jcas, IdentifiedAnnotation.class);
		for(IdentifiedAnnotation mention : mentions){
		  if(mention instanceof EventMention || mention instanceof EntityMention){
//		    TopTreebankNode orig = AnnotationTreeUtils.getAnnotationTree(jcas, mention);
//		    if(orig == null){
//		      log.warn("Tree for entity mention: " + mention.getCoveredText() + " (" + mention.getBegin() + "-" + mention.getEnd() + ") is null.");
//		      continue;
//		    }
		    SimpleTree tree = null; // extractFeatureTree(jcas, mention, sems);
//		    SimpleTree tree = extractAboveLeftConceptTree(jcas, mention, null);
//		    			SimpleTree tree = AssertionTreeUtils.extractAboveRightConceptTree(jcas, mention, sems);
		    String label = null;
		    
		    if(options.attributeType == ATTRIBUTE.NEG){ 
		      if(mention.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT) label = "+1";
		      else label = "-1";
          tree = getNegationTree(jcas, mention, sems);
		    }else if(options.attributeType == ATTRIBUTE.UNC){
		      if(mention.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT) label = "+1";
		      else label = "-1";
		      tree = getUncertaintyTree(jcas, mention, sems);
		    }else{
		      throw new IllegalArgumentException("Do not have this attribute type!");
		    }

		    out.print(label);
		    out.print(" |BT| ");
		    out.print(tree.toString());
		    out.println(" |ET|");
		    out.flush();
		  }
		}
	}

  public static SimpleTree getUncertaintyTree(JCas jcas, IdentifiedAnnotation mention, SemanticClasses sems) {
    SimpleTree tree = null;
    tree = extractAboveLeftConceptTree(jcas, mention, sems);
    String treeStr = tree.toString();
    treeStr = treeStr.replace("CONCEPT", "CONCEPT" + mention.getTypeID());
    return SimpleTree.fromString(treeStr);
  }

  public static SimpleTree getNegationTree(JCas jcas, IdentifiedAnnotation mention, SemanticClasses sems) {
    SimpleTree tree = null;
    tree = extractAboveLeftConceptTree(jcas, mention, sems);
    String treeStr = tree.toString();
//    treeStr = treeStr.replace("CONCEPT", "CONCEPT" + mention.getTypeID());
    return SimpleTree.fromString(treeStr);
  }

}
