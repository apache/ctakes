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
package org.apache.ctakes.coreference.ae;

import libsvm.svm_node;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.coreference.type.*;
import org.apache.ctakes.coreference.util.*;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.util.*;

@PipeBitInfo(
		name = "SVM Chain Creator (MiPACQ)",
		description = "Creates Coreferences using a Simple Vector Machine.",
		dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.MARKABLE  },
		products = { PipeBitInfo.TypeProduct.COREFERENCE_RELATION }
)
public class MipacqSvmChainCreator extends JCasAnnotator_ImplBase {

  public static final String PARAM_STOPWORDS_FILENAME = "StopWords";
  @ConfigurationParameter(name = PARAM_STOPWORDS_FILENAME, mandatory=false, defaultValue="org/apache/ctakes/coreference/models/stop.txt")
  File stopwordFile = null;
  HashSet<String> stopwords;

  public static final String PARAM_FRAGS_FILENAME = "FragsFile";
  @ConfigurationParameter(name = PARAM_FRAGS_FILENAME, mandatory=false, defaultValue="org/apache/ctakes/coreference/models/frags.txt")
  File treefragFile = null;
  private ArrayList<String> treeFrags;
  
  public static final String PARAM_COREF_MODEL = "ModelFile";
  @ConfigurationParameter(name = PARAM_COREF_MODEL, mandatory=false, defaultValue="org/apache/ctakes/coreference/models/ne.mayo.rbf.model")
  File modelFile = null;
  
	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	// svm models
//	private AbstractClassifier mod_pron, mod_dem, mod_coref;
	private AbstractClassifier mod_coref;
//	private AbstractClassifier mod_pron;
//	private svm_model mod_anaphoricity;
	// positive class label index in libsvm's prob_est array
//	private int anaphoricity_idx
//	private int coref_idx;
	private SvmVectorCreator vecCreator = null;
	
	@Override
	public void initialize(UimaContext uc) throws ResourceInitializationException {
		super.initialize(uc);

		// Load svm models
//		mod_anaphoricity = loadModel(uc, "svmAnaphoricityModel");
		// FIXME why is there a minus one here?
//		mod_pron = new AbstractClassifier(uc, "svmPronModel", FeatureVector.getPronCorefFeatures().length + SyntaxAttributeCalculator.getNumPronFeats() - 1);
//		mod_dem = new AbstractClassifier(uc, "svmDemModel", FeatureVector.getDemCorefFeatures().length + SyntaxAttributeCalculator.getNumDemFeats() - 1);
		mod_coref = new AbstractClassifier(modelFile, FeatureVector.getNECorefFeatures().length + SyntaxAttributeCalculator.getNumNEFeats() - 1);

		// Load stop words list
		try {
			stopwords = new HashSet<>();
			try(Scanner scanner = new Scanner(stopwordFile)){
			  String l;
			  while (scanner.hasNextLine()) {
			    l = scanner.nextLine().trim();
			    if (l.length()==0) continue;
			    int i = l.indexOf('|');
			    if (i > 0)
			      stopwords.add(l.substring(0,i).trim());
			    else if (i < 0)
			      stopwords.add(l.trim());
			  }
			  logger.info("Stop words list loaded: " + stopwordFile.getAbsolutePath());
			  vecCreator = new SvmVectorCreator(stopwords);
			}
      treeFrags = new ArrayList<>();
			try(Scanner scanner = new Scanner(treefragFile)){

			  while(scanner.hasNextLine()){
			    String line = scanner.nextLine();
			    treeFrags.add(line.split(" ")[1]);
			  }
			  vecCreator.setFrags(treeFrags);
			  logger.info("Tree fragment features loaded: " + treefragFile.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error loading stop words list");
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// Convert the orderless FSIterator to List, sort by char offsets
		LinkedList<Annotation> lm = FSIteratorToList.convert(
				jcas.getJFSIndexRepository().getAnnotationIndex(Markable.type).iterator());
		Map<Markable, NonEmptyFSList> collectionRas = new HashMap<>();
      String docName = DocIdUtil.getDocumentID( jcas );
		logger.info("Classifying coreference in document: " + docName);
//		ArrayList<CollectionTextRelation> chains = new ArrayList<CollectionTextRelation>();
		int chainId = 0;
		
		EmptyFSList emptyList = new EmptyFSList(jcas);
		// Create a parent pointer tree to calculate equivalence classes
//		ppt = new ParentPtrTree(lm.size());

		// Make a data structure mapping markables to indexes so we don't lose the order if we re-arrange
		Map<Markable, Integer> m2q = new HashMap<>();
		
		for(int p = 0; p < lm.size(); p++){
			m2q.put((Markable)lm.get(p), p);
		}
			
		FSIterator<Annotation> iter = jcas.getAnnotationIndex(MarkablePairSet.type).iterator();
		while(iter.hasNext()){
			MarkablePairSet set = (MarkablePairSet) iter.next();
			Markable anaphor = set.getAnaphor();
			FSList fs = set.getAntecedentList();
			MarkableProb bestAnte = null;
			LinkedList<Markable> ll = fs2ll(fs);
			if(anaphor instanceof PronounMarkable){
				// There is not enough training data to do this reliably... the
				// classifier for this type will decrease scores
//				bestAnte = processPronoun(anaphor, ll, jcas);
				bestAnte = new MarkableProb(null, 0.0);
			}else if(anaphor instanceof NEMarkable){
				bestAnte = processNE(anaphor, ll, jcas);
			}else if(anaphor instanceof DemMarkable){
				bestAnte = processDem(anaphor, ll, jcas);
			}else{
				// should not happenn...
				continue;
			}

			if(bestAnte.prob > CorefConsts.COREF_THRESHOLD){
				// create the coref relation type
				CoreferenceRelation cr = new CoreferenceRelation(jcas);
				cr.setCategory("Coreference");
				RelationArgument ra1 = new RelationArgument(jcas);
				ra1.setId(bestAnte.m.getId());
				ra1.setArgument(bestAnte.m.getContent());
				ra1.setRole("antecedent");
				RelationArgument ra2 = new RelationArgument(jcas);
				ra2.setId(anaphor.getId());
				ra2.setArgument(anaphor.getContent());
				ra2.setRole("anaphor");
				cr.setArg1(ra1);
				cr.setArg2(ra2);
				cr.setConfidence(bestAnte.prob);
				ra1.addToIndexes();
				ra2.addToIndexes();
				cr.addToIndexes();
				
				// propagate the collection relation type
				RelationArgument anaRa = new RelationArgument(jcas);
				anaRa.setId(anaphor.getId());
				anaRa.setArgument(anaphor.getContent());
				anaRa.setRole("mention");
				NonEmptyFSList node = new NonEmptyFSList(jcas);
				node.setHead(anaRa);
				node.setTail(emptyList);
				collectionRas.put(anaphor, node);
				NonEmptyFSList anteNode = null;
				if(collectionRas.containsKey(bestAnte.m)){
					anteNode = collectionRas.get(bestAnte.m);
					// find the end of the chain of this node
					while(anteNode.getTail() instanceof NonEmptyFSList){
						anteNode = (NonEmptyFSList) anteNode.getTail();
					}
				}else{
					RelationArgument anteRa = new RelationArgument(jcas);
					anteRa.setId(bestAnte.m.getId());
					anteRa.setArgument(bestAnte.m.getContent());
					anteRa.setRole("mention");
					
					anteNode = new NonEmptyFSList(jcas);
					anteNode.setHead(anteRa);
					collectionRas.put(bestAnte.m, anteNode);
					CollectionTextRelation chain = new CollectionTextRelation(jcas);
					chain.setId(chainId++);
					chain.setCategory("CoreferenceChain");
					chain.setMembers(anteNode);
					chain.addToIndexes();
				}
				anteNode.setTail(node);				
			}
		}
		logger.info("Done classifying document: " + docName);

//		// Extract equivalence classes and save them into CAS
//		int[] ec = new int[ppt.getSize()]; // class number for each Markable
//		int n = ppt.equivCls(ec); // n holds the number of classes
//		EmptyFSList elist = new EmptyFSList(jcas); // shared tail for all chains
//		FSList[] listhds = new FSList[n]; // keep track of the heads of all chains
//		CollectionTextRelation[] chains = new CollectionTextRelation[n];

//		// Initialize n chains
//		for (int i = 0; i < n; ++i) {
//			chains[i] = null; //new CollectionTextRelation(jcas);
////			chains[i].setId(i);
////			chains[i].setCategory("CoreferenceChain");
////			chains[i].addToIndexes();
//			listhds[i] = elist;
//		}

//		// Scan from the end of the Markable list
//		// insert Markables to the head of their chains
//		for (int i = ec.length-1; i >= 0; --i) {
//			if(m2ra.containsKey(lm.get(i))){
//				NonEmptyFSList l = new NonEmptyFSList(jcas);
//				l.setHead(m2ra.get(lm.get(i)));
//				l.setTail(listhds[ec[i]]);
//				listhds[ec[i]] = l;
//				if(chains[ec[i]] == null){
//					chains[ec[i]] = new CollectionTextRelation(jcas);
//				}
//				chains[ec[i]].setMembers(l);
//			}
//		}
		
//		int j = 0;
//		for(int i = 0; i < n; i++){
//			if(chains[i] != null){
//				chains[i].setId(j++);
//				chains[i].setCategory("CoreferenceChain");
//				chains[i].addToIndexes();
//			}
//		}
	}


	private static LinkedList<Markable> fs2ll(FSList fs) {
		LinkedList<Markable> ll = new LinkedList<>();
		while(fs instanceof NonEmptyFSList){
			NonEmptyFSList node = (NonEmptyFSList) fs;
			BooleanLabeledFS feat = (BooleanLabeledFS) node.getHead();
			Markable antecedent = (Markable) feat.getFeature();
			ll.add(antecedent);
			fs = node.getTail();
		}
		return ll;
	}

	/*
	private MarkableProb processPronoun(Markable anaphor, LinkedList<Markable> anteList, JCas jcas){
		Markable ante = null;
		double bestProb = 0.0;
		List<Markable> resortedList = anteList;
		for(Markable antecedent : resortedList){
			svm_node[] nodes = vecCreator.getNodeFeatures(anaphor, antecedent, jcas);
			
			double prob = 0.0;
			prob = mod_coref.predict(nodes);
			if(prob > bestProb){
//				if(debug) indexNegativeExample(jcas, ante, anaphor, bestProb); // save former best as non-ante...
				bestProb = prob;
				ante = antecedent;
			}else{
//				if(debug){
//					indexNegativeExample(jcas, antecedent, anaphor, prob);
//				}
			}
			if(bestProb > 0.5) break;
		}
		return new MarkableProb(ante, bestProb);
	}
*/
	private MarkableProb processNE(Markable anaphor, List<Markable> anteList, JCas jcas){
		Markable ante = null;
		double bestProb = 0.0;
		for(Markable antecedent : anteList){
			svm_node[] nodes = vecCreator.getNodeFeatures(anaphor, antecedent, jcas, true);
			double prob = 0.0;
			prob = mod_coref.predict(nodes);
			if(prob > bestProb){
//				if(debug) indexNegativeExample(jcas, ante, anaphor, bestProb);
				bestProb = prob;
				ante = antecedent;
			}else{
//				if(debug) indexNegativeExample(jcas, antecedent, anaphor, prob);
			}
		}
		return new MarkableProb(ante, bestProb);
	}
	
	/*
	private static MarkableProb processNELazily(Markable anaphor, List<Markable> anteList, JCas jcas){
		if(anteList.size() > 0) return new MarkableProb(anteList.get(0), 1.0);
		
		return new MarkableProb(null,0.0);
	}
	*/
	
	private static MarkableProb processDem(Markable anaphor, List<Markable> anteList, JCas jcas){
		double bestProb = 0.0;
		TreebankNode n = MarkableTreeUtils.markableNode(jcas, anaphor.getBegin(), anaphor.getEnd());
		TreebankNode parent = (n != null ? n.getParent() : null);
		TreebankNode gparent = (parent != null ? parent.getParent() : null);
		Markable ante = null;
		for(Markable antecedent: anteList){
			if(n!=null && parent != null && gparent != null && n.getNodeType().equals("WHNP") && parent.getNodeType().equals("SBAR")
					&& gparent.getNodeType().equals("NP") && gparent.getChildren(1) == parent && gparent.getChildren(0).getNodeType().equals("NP")){
				TreebankNode anteNode = gparent.getChildren(0);
				Markable trueAnte = MarkableTreeUtils.nodeMarkable(jcas, anteNode.getBegin(), anteNode.getEnd());
				if(trueAnte == antecedent){
					bestProb = 1.0;
					ante = antecedent;
					break;
				}
			}
		}
		return new MarkableProb(ante, bestProb);
	}
	


//	private void indexNegativeExample(JCas jcas, Markable ante, Markable ana,
//			double d) {
//		if(ante == null) return;
//		// README - If needed for debugging needs to be reimplemented now that type system has changed...
//		UncoreferentRelation rel = new UncoreferentRelation(jcas);
//		RelationArgument arg1 = new RelationArgument(jcas);
//		arg1.setArgument(ante);
//		arg1.setRole("nonantecedent");
//		RelationArgument arg2 = new RelationArgument(jcas);
//		arg2.setArgument(ana);
//		arg2.setRole("nonanaphor");
//		rel.setArg1(arg1);
//		rel.setArg2(arg2);
//		rel.setConfidence(d);
//		rel.addToIndexes();
//	}
}

class MarkableProb{
	public MarkableProb(Markable m, Double p){
		this.m = m;
		this.prob = p;
	}
	Markable m;
	Double prob;
}
