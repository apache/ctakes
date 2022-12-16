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
package org.apache.ctakes.coreference.cc;

import libsvm.svm_node;
import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.coreference.type.*;
import org.apache.ctakes.coreference.util.*;
import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

@PipeBitInfo(
		name = "ODIE Vector File Writer",
		description = "Write ODIE Vector File.",
		role = PipeBitInfo.Role.WRITER,
		dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.MARKABLE }
)
public class ODIEVectorFileWriter extends JCasAnnotator_ImplBase {

	private Logger log = Logger.getLogger(this.getClass());
//	private static final Integer NGRAM_THRESHOLD = 0;
	private String outputDir = null;
	private String goldStandardDir = null;
//	private PrintWriter anaphOut = null;
	private PrintWriter neOut = null;
	private PrintWriter pronOut = null;
	private PrintWriter demOut = null;
	private PrintWriter neTreeOut = null;
	private PrintWriter pronTreeOut = null;
	private PrintWriter demTreeOut = null;
	private PrintWriter debug = null;
	private boolean initialized = false;
	private int posNeInst = 0;
	private int negNeInst = 0;
	private int posDemInst = 0;
	private int negDemInst = 0;
	private int posPronInst = 0;
	private int negPronInst = 0;
	private int posAnaphInst = 0;
	private int negAnaphInst = 0;
	//	private svm_problem anaphProb = null;
//	private ArrayList<Integer> anaphLabels = new ArrayList<Integer>();
//	private ArrayList<svm_node[]> anaphNodes = new ArrayList<svm_node[]>();
//	private ArrayList<Integer> corefLabels = new ArrayList<Integer>();
//	private ArrayList<svm_node[]> corefNodes = new ArrayList<svm_node[]>();
	//	private ArrayList<TopTreebankNode> corefPathTrees = new ArrayList<TopTreebankNode>();
//	private ArrayList<String> corefTypes = new ArrayList<String>();
	//	private ArrayList<Integer> neInds = new ArrayList<Integer>();
	private PairAttributeCalculator attr = null;
	private HashSet<String> stopwords;
	private ArrayList<String> treeFrags;
	private SvmVectorCreator vecCreator = null;
//	private int maxSpanID = 0;
	private GoldStandardLabeler labeler = null; //new GoldStandardLabeler();
	

//	Vector<Span> goldSpans = null;
//	Hashtable<String,Integer> goldSpan2id = null;
//	Vector<int[]> goldPairs = null;

//	Vector<Span> sysSpans = null;
//	Vector<int[]> sysPairs = null;
	//	private boolean printModels;
	private boolean printVectors;
	private boolean printTrees;
//	private boolean anaphora;
	private boolean useFrags = true; 							// make a parameter once development is done...

	public static final String PARAM_OUTPUT_DIR = "outputDir";
	public static final String PARAM_GOLD_DIR = "goldStandardDir";
	public static final String PARAM_VECTORS = "writeVectors";
	public static final String PARAM_TREES = "writeTrees";
//	public static final String PARAM_ANAPH = "anaphora";
	public static final String PARAM_FRAGS = "treeFrags";
	public static final String PARAM_STOPS = "stopWords";
	
	@Override
	public void initialize(UimaContext aContext){
		outputDir = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR);
		goldStandardDir = (String) aContext.getConfigParameterValue(PARAM_GOLD_DIR);
		//		printModels = (Boolean) getConfigParameterValue("writeModels");
		printVectors = (Boolean) aContext.getConfigParameterValue(PARAM_VECTORS);
		printTrees = (Boolean) aContext.getConfigParameterValue(PARAM_TREES);
//		upSample = (Boolean) getConfigParameterValue("upSample");
//		anaphora = (Boolean) aContext.getConfigParameterValue(PARAM_ANAPH);

		try{
			// need to initialize parameters to default values (except where noted)
			File neDir = new File(outputDir + "/" + CorefConsts.NE + "/vectors/");
			neDir.mkdirs();
			File proDir = new File(outputDir + "/" + CorefConsts.PRON + "/vectors/");
			proDir.mkdirs();
			File demDir = new File(outputDir + "/" + CorefConsts.DEM + "/vectors/");
			demDir.mkdirs();
//			if(printVectors){
//				if(anaphora) anaphOut = new PrintWriter(outputDir + "/anaphor.trainingvectors.libsvm");
//				neOut = new PrintWriter(outputDir + "/" + CorefConsts.NE + "/training.libsvm");
//				demOut = new PrintWriter(outputDir + "/" + CorefConsts.DEM + "/training.libsvm");
//				pronOut = new PrintWriter(outputDir + "/" + CorefConsts.PRON + "/training.libsvm");
//			}
			if(printTrees){
				neTreeOut = new PrintWriter(outputDir + "/" + CorefConsts.NE + "/trees.txt");
				demTreeOut = new PrintWriter(outputDir + "/" + CorefConsts.DEM + "/trees.txt");
				pronTreeOut = new PrintWriter(outputDir + "/" + CorefConsts.PRON + "/trees.txt");
				debug = new PrintWriter(new PrintWriter(outputDir + "/" + CorefConsts.NE + "/fulltrees_debug.txt"), true);
			}
			//			if(printModels){
			//				pathTreeOut = new PrintWriter(outputDir + "/" + CorefConsts.NE + "/matrix.out");
			//			}
			stopwords = new HashSet<String>();
//			FileResource r = (FileResource) aContext.getResourceObject("stopWords");
			File stopFile = FileLocator.getFile(((String)aContext.getConfigParameterValue(PARAM_STOPS)));
			BufferedReader br = new BufferedReader(new FileReader(stopFile));
			String l;
			while ((l = br.readLine())!=null) {
				l = l.trim();
				if (l.length()==0) continue;
				int i = l.indexOf('|');
				if (i > 0)
					stopwords.add(l.substring(0,i).trim());
				else if (i < 0)
					stopwords.add(l.trim());
			}
//			File anaphModFile = FileLocator.locateFile("anaphoricity.mayo.rbf.model");
//			svm_model anaphModel = svm.svm_load_model(anaphModFile.getAbsolutePath());
			vecCreator = new SvmVectorCreator(stopwords);
//			r = (FileResource) aContext.getResourceObject("treeFrags");
			File fragFile = FileLocator.getFile(((String)aContext.getConfigParameterValue(PARAM_FRAGS)));
			Scanner scanner = new Scanner(fragFile);
			if(useFrags){
				treeFrags = new ArrayList<String>();
				while(scanner.hasNextLine()){
					String line = scanner.nextLine();
					treeFrags.add(line.split(" ")[1]);
				}
				vecCreator.setFrags(treeFrags);
			}
			initialized = true;
		}catch(Exception e){
			System.err.println("Error initializing file writers.");
		}
	}

	@Override
	public void process(JCas jcas) {
		//		System.err.println("processCas-ing");
		if(!initialized) return;
//		JCas jcas;
//		try {
//			jcas = arg0.getCurrentView().getJCas();
//		} catch (CASException e) {
//			e.printStackTrace();
//			System.err.println("No processing done in ODIEVectoFileWriter!");
//			return;
//		}

      String docId = DocIdUtil.getDocumentID( jcas );
		docId = docId.substring(docId.lastIndexOf('/')+1, docId.length());
//		Hashtable<Integer, Integer> sysId2AlignId = new Hashtable<Integer, Integer>();
//		Hashtable<Integer, Integer> goldId2AlignId = new Hashtable<Integer, Integer>();
//		Hashtable<Integer, Integer> alignId2GoldId = new Hashtable<Integer, Integer>();
		if (docId==null) docId = "141471681_1";
		System.out.println("creating vectors for "+docId);
//		Vector<Span> goldSpans = loadGoldStandard(docId, goldSpan2id);
		int numPos = 0;

		FSIterator markIter = jcas.getAnnotationIndex(Markable.type).iterator();
		LinkedList<Annotation> lm = FSIteratorToList.convert(markIter);

//		while(markIter.hasNext()){
//			Markable m = (Markable) markIter.next();
//			String key = m.getBegin() + "-" + m.getEnd();
//			markables.put(key, m);
//		}
		
		labeler = new GoldStandardLabeler(goldStandardDir, docId, lm);

//		Vector<Span> sysSpans = loadSystemPairs(lm, docId);
		// align the spans


		FSIterator iter = null;
//		FSIterator iter = jcas.getJFSIndexRepository().getAllIndexedFS(AnaphoricityVecInstance.type);
//		int numVecs = corefNodes.size();
//		log.info(numVecs + " nodes at the start of processing...");

//		if(anaphora){
//			while(iter.hasNext()){
//				AnaphoricityVecInstance vec = (AnaphoricityVecInstance) iter.next();
//				String nodeStr = vec.getVector();
//				int label = getLabel(nodeStr);
//				if(label == 1) posAnaphInst++;
//				else if(label == 0) negAnaphInst++;
//				anaphLabels.add(label);
//				svm_node[] nodes = SvmUtils.getNodes(nodeStr);
//				anaphNodes.add(nodes);
//			}
//			return;
//		}
		
		if(printVectors){
			try {
				neOut = new PrintWriter(outputDir + "/" + CorefConsts.NE + "/vectors/" + docId + ".libsvm");
				demOut = new PrintWriter(outputDir + "/" + CorefConsts.DEM + "/vectors/" + docId + ".libsvm");
				pronOut = new PrintWriter(outputDir + "/" + CorefConsts.PRON + "/vectors/"+ docId + ".libsvm");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//		int ind = 0;
		iter = jcas.getJFSIndexRepository().getAllIndexedFS(MarkablePairSet.type);
		while(iter.hasNext()){
			//			VecInstance vec = (VecInstance) iter.next();
			MarkablePairSet pair = (MarkablePairSet) iter.next();
			Markable anaphor = pair.getAnaphor();
			String corefType = (anaphor instanceof NEMarkable ? CorefConsts.NE : (anaphor instanceof DemMarkable ? CorefConsts.DEM : CorefConsts.PRON));
			//			String nodeStr = vec.getVector();
			//			int label = getLabel(nodeStr);
			FSList pairList = pair.getAntecedentList();
			while(pairList instanceof NonEmptyFSList){
				NonEmptyFSList node = (NonEmptyFSList) pairList;
				BooleanLabeledFS labeledProb = (BooleanLabeledFS) node.getHead();
				int label = labeledProb.getLabel() ? 1 : 0;
//				if(anaphora){
//					if(label == 1) posAnaphInst++;
//					else negAnaphInst++;
//					anaphLabels.add(label);
//					svm_node[] nodes = vecCreator.createAnaphoricityVector(anaphor, jcas);
//					anaphNodes.add(nodes);
//				}
				Markable antecedent = (Markable) labeledProb.getFeature();
				label = (labeler.isGoldPair(anaphor, antecedent) ? 1 : 0);
				if(label == 1){
					numPos++;
					if(corefType.equals(CorefConsts.NE)){
						posNeInst++;
						//					neInds.add(ind);
					}else if(corefType.equals(CorefConsts.DEM)){
						posDemInst++;
					}else if(corefType.equals(CorefConsts.PRON)){
						posPronInst++;
					}
				}
				else if(label == 0){
					if(corefType.equals(CorefConsts.NE)){
						negNeInst++;
						//					neInds.add(ind);
					}else if(corefType.equals(CorefConsts.DEM)){
						negDemInst++;
					}else if(corefType.equals(CorefConsts.PRON)){
						negPronInst++;
					}
				}
//				corefLabels.add(label);
//				corefTypes.add(corefType);				// need to add it every time so the indices match...
				//			corefPathTrees.add(pathTree);

				if(printVectors){
					svm_node[] nodes = vecCreator.getNodeFeatures(anaphor, antecedent, jcas); //getNodes(nodeStr);
//					corefNodes.add(nodes);
					PrintWriter writer = null;
					if(corefType.equals(CorefConsts.NE)){
						writer = neOut;
					}else if(corefType.equals(CorefConsts.PRON)){
						writer = pronOut;
					}else if(corefType.equals(CorefConsts.DEM)){
						writer = demOut;
					}
					writer.print(label);
					for(svm_node inst : nodes){
						writer.print(" ");
						writer.print(inst.index);
						writer.print(":");
						writer.print(inst.value);
					}
					writer.println();
					writer.flush();
				}

				if(printTrees){
					//					Markable anaphor = vec.getAnaphor();
					//					Markable antecedent = vec.getAntecedent();
					TreebankNode antecedentNode = MarkableTreeUtils.markableNode(jcas, antecedent.getBegin(), antecedent.getEnd());
					TreebankNode anaphorNode = MarkableTreeUtils.markableNode(jcas, anaphor.getBegin(), anaphor.getEnd());
					debug.println(TreeUtils.tree2str(antecedentNode));
					debug.println(TreeUtils.tree2str(anaphorNode));
//					TopTreebankNode pathTree = TreeExtractor.extractPathTree(antecedentNode, anaphorNode, jcas);
					SimpleTree pathTree = TreeExtractor.extractPathTree(antecedentNode, anaphorNode);
					SimpleTree petTree = TreeExtractor.extractPathEnclosedTree(antecedentNode, anaphorNode, jcas);
//					TopTreebankNode tree = mctTree;
//					String treeStr = TreeUtils.tree2str(tree);
//					String treeStr = mctTree.toString();
					String treeStr = pathTree.toString();
					PrintWriter writer = null;
					if(corefType.equals(CorefConsts.NE)){
						writer = neTreeOut;
					}else if(corefType.equals(CorefConsts.PRON)){
						writer = pronTreeOut;
					}else if(corefType.equals(CorefConsts.DEM)){
						writer = demTreeOut;
					}
					writer.print(label == 1 ? "+1" : "-1");
					writer.print(" |BT| ");
					writer.print(treeStr.replaceAll("\\) \\(", ")("));
					writer.println(" |ET|");
				}
				pairList = node.getTail();
				// NOTE: If this is in place, then we will only output negative examples backwards until we reach
				// the actual coreferent entity.  This may have the effect of suggesting that further away markables
				// are _more_ likely to be coreferent, which is an assumption that probably does not hold up in the
				// test set configuration.  Try commenting this feature out to see if it makes the feature more useful.
//				if(label == 1) break;
			}
		}
		if(printVectors){
			neOut.close();
			demOut.close();
			pronOut.close();
		}
//		numVecs = (corefNodes.size() - numVecs);
//		log.info("Document id: " + docId + " has " + numVecs + " pairwise instances.");
	}


	private int getLabel(String nodeStr) {
		return Integer.parseInt(nodeStr.substring(0,1));
	}

	
	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		super.batchProcessComplete();

		//		System.err.println("collectionProcessComplete!");
		if(!initialized) return;

//		int numPos = 1;
//		int numNeg = 1;
//
//		if(anaphora){
//			double anaphRatio = (double) posAnaphInst / (double) negAnaphInst;
////			if(anaphRatio > 1.0) numNeg = (int) anaphRatio;
////			else numPos = (int) (1 / anaphRatio);
//			for(int i = 0; i < anaphNodes.size(); i++){
//				int label = anaphLabels.get(i);
////				int numIters = (label == 1 ? numPos : numNeg);
////				for(int j = 0; j < numIters; j++){
//					anaphOut.print(label);
//					for(svm_node node : anaphNodes.get(i)){
//						anaphOut.print(" ");
//						anaphOut.print(node.index);
//						anaphOut.print(":");
//						anaphOut.print(node.value);
//					}
//					anaphOut.println();
////				}
//			}
//			anaphOut.flush();
//			anaphOut.close();
//			return;
//		}
		if(printVectors){
			neOut.close();
			demOut.close();
			pronOut.close();
		}

		if(printTrees){
			neTreeOut.flush();
			neTreeOut.close();
			demTreeOut.flush();
			demTreeOut.close();
			pronTreeOut.flush();
			pronTreeOut.close();
		}
	}

	private double[] listToDoubleArray(ArrayList<Integer> list) {
		double[] array = new double[list.size()];
		for(int i = 0; i < list.size(); i++){
			array[i] = (double) list.get(i);
		}
		return array;
	}
	
	public static void main(String[] args){
		if(args.length < 3){
			System.err.println("Arguments: <training directory> <gold-pairs directory> <output directory>");
			System.exit(-1);
		}
		File xmiDir = new File(args[0]);
		if(!xmiDir.isDirectory()){
			System.err.println("Arg1 should be a directory! (full of xmi files)");
			System.exit(-1);
		}
		File[] files = xmiDir.listFiles();
//		ArrayList<File> fileList = new ArrayList<File>();
		String[] paths = new String[files.length];
		for(int i = 0; i < files.length; i++){
//			fileList.add(files[i]);
			paths[i] = files[i].getAbsolutePath();
		}
//		TypeSystemDescription typeSystem = 
//			TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath("../ctakes-type-system/desc/common_type_system.xml", 
//																			 "desc/type-system/CorefTypes.xml",
//																			 "../assertion/desc/medfactsTypeSystem.xml");
//		TypeSystemDescription corefTypeSystem = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath();
		try {
			CollectionReader xmiReader = CollectionReaderFactory.createReader(XMIReader.class, 
//					typeSystem, 
					XMIReader.PARAM_FILES, 
					paths);
			
			AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ODIEVectorFileWriter.class,
//					typeSystem,
					ODIEVectorFileWriter.PARAM_VECTORS, true,
					ODIEVectorFileWriter.PARAM_TREES, false,
					ODIEVectorFileWriter.PARAM_STOPS, "org/apache/ctakes/coreference/models/stop.txt",
					ODIEVectorFileWriter.PARAM_FRAGS, "org/apache/ctakes/coreference/models/frags.txt",
					ODIEVectorFileWriter.PARAM_GOLD_DIR, args[1],
					ODIEVectorFileWriter.PARAM_OUTPUT_DIR, args[2]);
					
			SimplePipeline.runPipeline(xmiReader, consumer);
		}catch(Exception e){
			System.err.println("Exception thrown!");
			e.printStackTrace();
		}
	}
}
