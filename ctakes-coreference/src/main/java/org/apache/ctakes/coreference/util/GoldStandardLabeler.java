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
package org.apache.ctakes.coreference.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.ctakes.coreference.eval.helpers.Span;
import org.apache.ctakes.coreference.eval.helpers.SpanAlignment;
import org.apache.ctakes.coreference.eval.helpers.SpanOffsetComparator;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.coreference.type.Markable;

public class GoldStandardLabeler {
	private String dir = null;
	private String docName = null;
	Vector<Span> sysSpans = null;
	Vector<Span> goldSpans = null;
	Hashtable<String,Integer> goldSpan2id = null;
	Hashtable<String,Integer> sysSpan2id = null;
	Hashtable<Integer, Integer> sysId2AlignId = null;
	Hashtable<Integer, Integer> goldId2AlignId = null;
	Hashtable<Integer, Integer> alignId2GoldId = null;
//	HashMap<String,Markable> markables = null;
	int[] goldEqvCls;
	ParentPtrTree ppt;

	public GoldStandardLabeler(String dir, String doc, List<Annotation> lm){
		this.dir = dir;
		this.docName = doc;
		goldSpan2id = new Hashtable<String, Integer>();
		sysSpan2id = new Hashtable<String, Integer>();
		sysId2AlignId = new Hashtable<Integer, Integer>();
		goldId2AlignId = new Hashtable<Integer, Integer>();
		alignId2GoldId = new Hashtable<Integer, Integer>();
		sysSpans = new Vector<Span>();
		goldSpans = new Vector<Span>();
//		markables = new HashMap<String, Markable>();
//		initializeDataStructures();
		loadGoldStandard();
		loadSystemPairs(lm);
		SpanAlignment sa = new SpanAlignment(goldSpans.toArray(new Span[goldSpans.size()]),
				sysSpans.toArray(new Span[sysSpans.size()]));
		int[] id = sa.get1();
		for (int i = 0; i < id.length; i++){
			alignId2GoldId.put(id[i], goldSpan2id.get(goldSpans.get(i).toString()));
			goldId2AlignId.put(goldSpan2id.get(goldSpans.get(i).toString()), id[i]);
		}
		id = sa.get2();
		for (int i = 0; i < id.length; i++){
			sysId2AlignId.put(sysSpan2id.get(sysSpans.get(i).toString()), id[i]);
		}
	}
	

	public boolean isGoldPair (Annotation ann1, Annotation ann2) {
		if(!sysSpan2id.containsKey(ann1.getBegin()+"-"+ann1.getEnd())) return false;
		int sysId1 = sysSpan2id.get(ann1.getBegin()+"-"+ann1.getEnd());
		if(!sysSpan2id.containsKey(ann2.getBegin()+"-"+ann2.getEnd())) return false;
		int sysId2 = sysSpan2id.get(ann2.getBegin()+"-"+ann2.getEnd());

		int newId1 = sysId2AlignId.get(sysId1);
		int newId2 = sysId2AlignId.get(sysId2);

		if(!alignId2GoldId.containsKey(newId1)) return false;
		int goldId1 = alignId2GoldId.get(newId1);
		if(!alignId2GoldId.containsKey(newId2)) return false;
		int goldId2 = alignId2GoldId.get(newId2);

		return (goldEqvCls[goldId1-1] == goldEqvCls[goldId2-1]);
	}

	private void loadGoldStandard() {
//		File f = new File("/home/tmill/mnt/rc-pub/odie/gold-pairs/"+docName);
		File f = new File(dir + File.separator + docName);
		Vector<int[]> goldPairs = new Vector<int[]>();
		int id = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String l;
			//		ArrayList<int[]> ppt_arr = new ArrayList<int[]>();
			while ((l = br.readLine())!=null) {
				String[] p = l.split("\\t");
				if (!goldSpan2id.containsKey(p[0])){
					goldSpan2id.put(p[0], ++id);
					String[] s = p[0].split("[-:]");
					int[] a = new int[s.length];
					for (int i = 0; i < s.length; i++)
						a[i] = Integer.parseInt(s[i]);
					goldSpans.add(new Span(a));
				}
				if (!goldSpan2id.containsKey(p[1])){
					goldSpan2id.put(p[1], ++id);
					String[] s = p[1].split("[-:]");
					int[] a = new int[s.length];
					for (int i = 0; i < s.length; i++)
						a[i] = Integer.parseInt(s[i]);
					goldSpans.add(new Span(a));					
				}
				goldPairs.add(new int[]{goldSpan2id.get(p[0]), goldSpan2id.get(p[1])});
				//			ppt_arr.add(new int[]{span2id.get(p[0]), span2id.get(p[1])});
			}
			br.close();
			java.util.Collections.sort(goldSpans, new SpanOffsetComparator());
			ppt = new ParentPtrTree(goldPairs.toArray(new int[][] {}));
			goldEqvCls = new int[ppt.getSize()];
			ppt.equivCls(goldEqvCls);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadSystemPairs(List<Annotation> lm){
		Vector<int[]> sysPairs = new Vector<int[]>();
		// build system chains...
		for (int p = 1; p < lm.size(); ++p) {
			Markable m1 = (Markable) lm.get(p);
			int id1 = m1.getId();
			if (!sysSpan2id.containsKey(m1.getBegin()+"-"+m1.getEnd())) {
				sysSpan2id.put(m1.getBegin()+"-"+m1.getEnd(), id1);
				sysSpans.add(new Span(new int[]{m1.getBegin(), m1.getEnd()}));
			}
			for(int q = p-1; q >= 0; q--){
				Markable m2 = (Markable) lm.get(q);
				int id2 = m2.getId();
				if (!sysSpan2id.containsKey(m2.getBegin()+"-"+m2.getEnd())) {
					sysSpan2id.put(m2.getBegin()+"-"+m2.getEnd(), id2);
					sysSpans.add(new Span(new int[]{m2.getBegin(), m2.getEnd()}));
				}
				sysPairs.add(new int[]{id1, id2});

			}
		}
		java.util.Collections.sort(sysSpans, new SpanOffsetComparator());
	}
}
