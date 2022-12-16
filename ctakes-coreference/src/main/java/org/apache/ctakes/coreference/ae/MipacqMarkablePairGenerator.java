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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.coreference.type.BooleanLabeledFS;
import org.apache.ctakes.coreference.type.DemMarkable;
import org.apache.ctakes.coreference.type.Markable;
import org.apache.ctakes.coreference.type.MarkablePairSet;
import org.apache.ctakes.coreference.type.NEMarkable;
import org.apache.ctakes.coreference.type.PronounMarkable;
import org.apache.ctakes.coreference.util.CorefConsts;
import org.apache.ctakes.coreference.util.FSIteratorToList;
import org.apache.ctakes.coreference.util.PairAttributeCalculator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

@PipeBitInfo(
		name = "Markable Pair Creator (MiPACQ)",
		description = "Pairs Markables using a stop word list.",
		role = PipeBitInfo.Role.SPECIAL,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.CHUNK  },
		usables = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class MipacqMarkablePairGenerator extends JCasAnnotator_ImplBase {

  public static final String PARAM_STOPWORDS_FILE = "StopFile";
  @ConfigurationParameter(name=PARAM_STOPWORDS_FILE, mandatory=false, defaultValue="org/apache/ctakes/coreference/models/stop.txt")
  File stopwordFile = null;
  HashSet<String> stopwords;
  
	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());
	int numVecs = 0;
	
	@Override
	public void initialize(UimaContext uc) throws org.apache.uima.resource.ResourceInitializationException {
		super.initialize(uc);
		
		// Load stop words list
		try {
			stopwords = new HashSet<>();
			try(BufferedReader br = new BufferedReader(new FileReader(stopwordFile))){
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
			}
			logger.info("Stop words list loaded: " + stopwordFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error loading stop words list");
		}
		
	}
	
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// read the gold standard
		numVecs = 0;
//		String docName = DocumentIDAnnotationUtil.getDocumentID(jcas);

		// Convert the orderless FSIterator to List, sort by char offsets
		LinkedList<Annotation> lm = FSIteratorToList.convert(
				jcas.getJFSIndexRepository().getAnnotationIndex(Markable.type).iterator());
		
		// now iterate over system markables and add the ones that match gold standard as
		// true, otherwise false
		for (int p = 1; p < lm.size(); ++p) {
			Markable m = (Markable) lm.get(p);
			Annotation mc = m.getContent();

			// if m is a pronoun 
			if (m instanceof PronounMarkable &&
				((BaseToken) mc).getPartOfSpeech().startsWith("PRP")){
					createPronPairs(lm, p, jcas);
			}

			// if m is a demonstrative or relative pronoun
			else if (m instanceof DemMarkable &&
					((Chunk) mc).getChunkType().equals("NP")){
				String s = mc.getCoveredText().toLowerCase();
				if (s.startsWith("this") ||
					s.startsWith("that") ||
					s.startsWith("these") ||
					s.startsWith("those") ||
					s.equalsIgnoreCase("which")){
					createDemPairs(lm, p, jcas);
				}
			}

			// if m is a regular NE
			else if (m instanceof NEMarkable) {
				createCorefPairs(lm, p, jcas);
			}

			else {
				System.err.println("Unknown type of Markable " + mc.getClass().getName() + " " + mc.getCoveredText());
			}
		}
		System.out.println("   ....ended with " + numVecs + " vectors.");
//		maxSpanID += sa.getMaxID();
	}

	private void createCorefPairs(LinkedList<Annotation> lm, int p, JCas jcas) {
		NEMarkable m = (NEMarkable) lm.get(p); // Current markable under consideration
		MarkablePairSet pairList = new MarkablePairSet(jcas);
		pairList.setBegin(m.getBegin());
		pairList.setEnd(m.getEnd());
		pairList.setAnaphor(m);
		NonEmptyFSList head = new NonEmptyFSList(jcas);
		pairList.setAntecedentList(head);
		NonEmptyFSList tail = null;
		for (int q = p-1; q>=0; --q) {
			Markable a = (Markable) lm.get(q); // Candidate antecedent

			// Don't link to a expletive
//			if (dnr.contains(m)) continue;
			// Look no more than 10 sentences
			int sentdist = sentDist(jcas, a, m);
			if (sentdist>CorefConsts.NEDIST) break;
//			else if (sentdist>PRODIST && m instanceof PronounMarkable) continue;
			// filter out if both are NEs but of diff types
			if (m.getContent() instanceof IdentifiedAnnotation &&
					a.getContent() instanceof IdentifiedAnnotation &&
					((IdentifiedAnnotation)m.getContent()).getTypeID() != ((IdentifiedAnnotation)a.getContent()).getTypeID())
				continue;

			// filter out "which" that crosses sentence boundary
			if (a.getCoveredText().equalsIgnoreCase("which") &&
					sentDist(jcas, a, m)>=1)
				continue;
			// ban pairs that one markable is a sub/superspan of the other
			if ((a.getBegin()<=m.getBegin() && a.getEnd()>=m.getEnd()) ||
					m.getBegin()<=a.getBegin() && m.getEnd()>=a.getEnd())
				continue;
			// Create a vector
			BooleanLabeledFS labeledAntecedent = new BooleanLabeledFS(jcas);
			labeledAntecedent.setFeature(a);
			if(tail == null){
				tail = head;
			}else{
				tail.setTail(new NonEmptyFSList(jcas));
				tail = (NonEmptyFSList) tail.getTail();
			}
			tail.setHead(labeledAntecedent);
		}
		if(tail == null) pairList.setAntecedentList(new EmptyFSList(jcas));
		else tail.setTail(new EmptyFSList(jcas));
		numVecs++;
		pairList.addToIndexes();		
	}

	private void createDemPairs(LinkedList<Annotation> lm, int p, JCas jcas) {
		DemMarkable m = (DemMarkable) lm.get(p); // Current markable under consideration
		MarkablePairSet pairList = new MarkablePairSet(jcas);
		pairList.setAnaphor(m);
		NonEmptyFSList head = new NonEmptyFSList(jcas);
		pairList.setAntecedentList(head);
		NonEmptyFSList tail = null;

		for (int q = p-1; q>=0; --q) {
			Markable a = (Markable) lm.get(q); // Candidate antecedent
			if (sentDist(jcas, a, m)>CorefConsts.PRODIST) break; // Look no more than 3 sentences

			// Create a vector
			BooleanLabeledFS labeledAntecedent = new BooleanLabeledFS(jcas);
			labeledAntecedent.setFeature(a);
			if(tail == null){
				tail = head;
			}else{
				tail.setTail(new NonEmptyFSList(jcas));
				tail = (NonEmptyFSList) tail.getTail();
			}
			tail.setHead(labeledAntecedent);
		}
		if(tail == null) pairList.setAntecedentList(new EmptyFSList(jcas));
		else tail.setTail(new EmptyFSList(jcas));
		numVecs++;
		pairList.addToIndexes();
	}

	private void createPronPairs(LinkedList<Annotation> lm, int p, JCas jcas) {
		PronounMarkable m = (PronounMarkable) lm.get(p); // Current markable under consideration
		MarkablePairSet pairList = new MarkablePairSet(jcas);
		pairList.setAnaphor(m);
		NonEmptyFSList head = new NonEmptyFSList(jcas);
		pairList.setAntecedentList(head);
		NonEmptyFSList tail = null;
		
		for (int q = p-1; q>=0; --q) {
			Markable a = (Markable) lm.get(q); // Candidate antecedent
			if (sentDist(jcas, a, m)>CorefConsts.PRODIST) break;  // Look no more than 3 sentences

			if ((a.getBegin()<=m.getBegin() && a.getEnd()>=m.getEnd()) ||
					m.getBegin()<=a.getBegin() && m.getEnd()>=a.getEnd())
				continue;

			// Create a pair
			BooleanLabeledFS labeledAntecedent = new BooleanLabeledFS(jcas);
			labeledAntecedent.setFeature(a);
			if(tail == null){
				tail = head;
			}else{
				tail.setTail(new NonEmptyFSList(jcas));
				tail = (NonEmptyFSList) tail.getTail();
			}
			tail.setHead(labeledAntecedent);
		}
		if(tail == null) pairList.setAntecedentList(new EmptyFSList(jcas));
		else tail.setTail(new EmptyFSList(jcas));
		numVecs++;
		pairList.addToIndexes();
	}


//	private void loadGoldStandard(String docName) {
//		File f = new File("/home/tmill/mnt/rc-pub/odie/gold-pairs/"+docName);
//		goldSpan2id = new Hashtable<String, Integer>();
//		goldPairs = new Vector<int[]>();
//		goldSpans = new Vector<Span>();
//		int id = 0;
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(f));
//			String l;
////			ArrayList<int[]> ppt_arr = new ArrayList<int[]>();
//			while ((l = br.readLine())!=null) {
//				String[] p = l.split("\\t");
//				if (!goldSpan2id.containsKey(p[0])){
//					goldSpan2id.put(p[0], ++id);
//					String[] s = p[0].split("[-:]");
//					int[] a = new int[s.length];
//					for (int i = 0; i < s.length; i++)
//						a[i] = Integer.parseInt(s[i]);
//					goldSpans.add(new Span(a));
//				}
//				if (!goldSpan2id.containsKey(p[1])){
//					goldSpan2id.put(p[1], ++id);
//					String[] s = p[1].split("[-:]");
//					int[] a = new int[s.length];
//					for (int i = 0; i < s.length; i++)
//						a[i] = Integer.parseInt(s[i]);
//					goldSpans.add(new Span(a));					
//				}
//				goldPairs.add(new int[]{goldSpan2id.get(p[0]), goldSpan2id.get(p[1])});
////				ppt_arr.add(new int[]{span2id.get(p[0]), span2id.get(p[1])});
//			}
//			br.close();
//			java.util.Collections.sort(goldSpans, new SpanOffsetComparator());
//			ppt = new ParentPtrTree(goldPairs.toArray(new int[][] {}));
//			goldEqvCls = new int[ppt.getSize()];
//			ppt.equivCls(goldEqvCls);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	private void loadSystemPairs(LinkedList<Annotation> lm){
//		sysPairs = new Vector<int[]>();
//		sysSpans = new Vector<Span>();
//		sysSpan2id = new Hashtable<String, Integer>();
//		// build system chains...
//		for (int p = 1; p < lm.size(); ++p) {
//			Markable m1 = (Markable) lm.get(p);
//			int id1 = m1.getId();
//			if (!sysSpan2id.containsKey(m1.getBegin()+"-"+m1.getEnd())) {
//				sysSpan2id.put(m1.getBegin()+"-"+m1.getEnd(), id1);
//				sysSpans.add(new Span(new int[]{m1.getBegin(), m1.getEnd()}));
//			}
//			for(int q = p-1; q >= 0; q--){
//				Markable m2 = (Markable) lm.get(q);
//				int id2 = m2.getId();
//				if (!sysSpan2id.containsKey(m2.getBegin()+"-"+m2.getEnd())) {
//					sysSpan2id.put(m2.getBegin()+"-"+m2.getEnd(), id2);
//					sysSpans.add(new Span(new int[]{m2.getBegin(), m2.getEnd()}));
//				}
//				sysPairs.add(new int[]{id1, id2});
//				
//			}
//		}
//		java.util.Collections.sort(sysSpans, new SpanOffsetComparator());
//	}
	
	private int sentDist (JCas jcas, Markable m1, Markable m2) {
		PairAttributeCalculator ac = new PairAttributeCalculator(jcas, m1, m2);
		ac.setStopWordsList(stopwords);
		return ac.getSentenceDistance();
	}

//	private boolean isGoldPair (Annotation ann1, Annotation ann2) {
//		if(!sysSpan2id.containsKey(ann1.getBegin()+"-"+ann1.getEnd())) return false;
//		int sysId1 = sysSpan2id.get(ann1.getBegin()+"-"+ann1.getEnd());
//		if(!sysSpan2id.containsKey(ann2.getBegin()+"-"+ann2.getEnd())) return false;
//		int sysId2 = sysSpan2id.get(ann2.getBegin()+"-"+ann2.getEnd());
//		
//		int newId1 = sysId2AlignId.get(sysId1);
//		int newId2 = sysId2AlignId.get(sysId2);
//		
//		if(!alignId2GoldId.containsKey(newId1)) return false;
//		int goldId1 = alignId2GoldId.get(newId1);
//		if(!alignId2GoldId.containsKey(newId2)) return false;
//		int goldId2 = alignId2GoldId.get(newId2);
//		
//		return (goldEqvCls[goldId1-1] == goldEqvCls[goldId2-1]);
//	}
}
