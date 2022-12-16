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

import java.io.FileNotFoundException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.utils.wiki.WikiIndex;
import org.apache.ctakes.coreference.type.Markable;
import org.apache.ctakes.coreference.type.DemMarkable;
import org.apache.ctakes.coreference.type.NEMarkable;
import org.apache.ctakes.coreference.type.PronounMarkable;

public class SyntaxAttributeCalculator extends PairAttributeCalculator {

//	private Markable m1, m2;
	private TreebankNode n1, n2, lca;
	HashMap<String,Integer> ngrams = null;
	ConllDependencyNode c1, c2;
	ConllDependencyNode depLca=null;
	String path = null;
	String depPath = null;
	WikiIndex wiki = null;
	double sim1=-1.0;
	double sim2=-1.0;
	private static int numNEFeats = 0;
	private static int numDemFeats = 0;
	private static int numPronFeats = 0;
	static ArrayList<String> featSet = new ArrayList<String>();
	static ArrayList<String> pronFeatSet = new ArrayList<String>();
	static ArrayList<String> demFeatSet = new ArrayList<String>();

	// These arrays are trained on _all_
//	static int[] selFeats = {10,11,13,16,22,27,28,29,35,37,39,50,53,100,115,137,144,178,189,204,260,276,277,291,296,299,325,347,359,414,420,424,455,457,462,468,471,474,478,479,482,483,484,496,501,503,543,546,554,558,563,564,568,574,579,590,595,606,607,625,628,635,648,690,770,779,781,803,806,816,818,988,990,995,1017,1018,1078,1080,1081,1082,1090,1091,1092,1102,1153,1160,1200,1271,1272,1297,1298,1299,1300,1301,1346,1398,1401,1409,1411,1437,1440,1441,1451,1452,1506,1625,1650,1651,1652,1662,1758,1769,1910,1918,1935,1962,2082,2124,2150,2277,2302,2317,2328,2333,2358,2381,2399,2400,2507,2549,2656,2775,2994,3079,3089,3128,3129,3273,3403,3641,3760,3761,3762,3763,3909,4021,4022,4023,4110,4156,4517,4800};
//	static int[] pronSelFeats = {9,17,21,26,42,159,306,310,313,317,335,379,486,718,825,828,829,830,831,832,833,834,842,843,844,845,871,931,932,933,934,935,936,996,997,998,999};
//	static int[] demSelFeats = {40,50,102,107,114,121,124,272,380,384,636,896,897,898,899,900,901,902,903,904};

	/* These arrays are trained on mayo full training set only: */
//	static int[] selFeats = {3,12,22,40,42,49,60,68,71,72,76,77,124,130,181,184,185,186,189,200,208,209,210,211,212,215,220,221,222,225,242,249,258,309,310,311,314,319,320,321,330,370,378,424,456,475,498,514,516,521,555,556,558,559,560,561,562,617,646,697,701,705,718,720,722,744,763,800,801,822,844,875,884,885,957,959,991,996,1003,1030,1041,1055,1098,1109,1111,1113,1162,1163,1164,1170,1195,1255,1256,1298,1338,1341,1344,1397,1498,1884,1923,2188,2257,2277,2318,2319,2417,2660};
//	static int[] pronSelFeats = {46,156,160,169,173,206,458,462,463,505,508,509,512,552,555};
//	static int[] demSelFeats = {40,50,102,107,114,121,124,272,380,384,636,896,897,898,899,900,901,902,903,904};  // not used

	// phrase + dep feats (Mayo-only train/dev split from paths.txt)
//	static int[] selFeats = {6,8,33,36,39,42,55,63,68,76,122,124,126,127,129,138,154,163,166,171,172,181,185,186,193,195,197,199,200,201,204,208,228,244,261,267,277,280,291,315,345,357,394,437,481,530,531,534,535,562,630,632,635,638,662,670,686,688,689,691,692,693,695,696,702,703,704,705,707,711,717,748,767,769,862,927,944,949,959,962,964,984,986,1007,1033,1073,1082,1113,1155,1164,1166,1204,1205,1219,1220,1227,1231,1233,1234,1246,1247,1259,1262,1265,1269,1306,1385,1424,1429,1436,1489,1527,1531,1533,1683,1686,1772,1937,2252,2669,2672,3069,3075,3097,3102,3122,3134,3138,3203,3368,3413,3751,3803,3812,3819,3831,3837,3841,3965,4163,4296,4580,4591,4603,4772,4798,4799,5024,5025,5055,5068,5090,5644,5645,5933,5934,5935,5939,5940,6037,6073,6076,6077,6379,6385,6501,6502,6515,6534,6593,6598,6601,6602,6603,6934,6935,6938,6946,6991,7039,7082,7213,7233,7235,7242,7317,7318,7418,7812,7813,7817,7846,8176,8324,8325,8333,8669,9059,9209,9210,9212,9215,9254};
//	static int[] pronSelFeats = {20,41,56,60,69,88,95,117,118,154,181,182,190,452,898,930,932,944,963,1023,1028,1045,1075,1083,1446,1447,1448,1451,1901,1906,1994,1995,1996,1997,1998,1999,2000,2366,2367,2410,2411,2412,2662,2821,3091,3614,4323,4325,5556,5576,5829,5831,5840,6524,6714,6715,7032,7033,7034,7039,7040,7048,7271,7272,7274,7275,7280,7538,8673};
//	static int[] demSelFeats = {};

	// mayo-only train/dev split feature set (trained on const_paths.txt) 
//	static int[] selFeats = {17,18,21,27,28,41,48,85,88,94,98,121,128,169,197,272,281,337,343,344,346,349,352,359,360,361,362,363,367,368,369,370,399,443,484,515,559,569,570,572,573,593,597,599,629,660,661,665,672,685,692,754,822,823,860,925,937,939,1161,1164,1172,1628,1640,1641,1653,2115,2135,2136,2167,2168,2169,2171,2247,2355,2368,2505,2694};
//	static int[] pronSelFeats = {10,27,28,41,47,61,94,95,97,100,445,470,472,510,538,635,805,806,942,1041,1293,1295,1296,1297,2076,2077,2130,2153,2175,2176};

	// not using the feature:
	static int[] selFeats = {0};
	static int[] pronSelFeats = {0};
	
//	static{
//		// read in feature files for each classifier type
//		featSet = loadFeatures(selFeats, "ngramids.mayo.txt");
//		pronFeatSet = loadFeatures(pronSelFeats, "pronngramids.mayo.txt");
//		numNEFeats = selFeats.length;
//		numDemFeats = 0;
//		numPronFeats = pronSelFeats.length;
//	}

	static ArrayList<String> loadFeatures(int[] featInds, String filename){
		ArrayList<String> feats = new ArrayList<String>();
		Scanner scanner = null;
		try{
			scanner = new Scanner(FileLocator.getFile(filename));
		}catch(FileNotFoundException e){
			// return empty feature list.
			System.err.println("Exception reading filename: " + e.getMessage());
			return feats;
		}
		int ind = 0;
		int lineNum = 0;
		String line = null;
		while(scanner.hasNextLine()){
			lineNum++;
			line = scanner.nextLine();
			if(featInds[ind] == lineNum){
				feats.add(line);
				ind++;
			}
			if(ind >= featInds.length)	break;
		}
		return feats;
	}

	public SyntaxAttributeCalculator(JCas jcas, Markable m1, Markable m2){
		this(jcas,m1,m2,null);
	}
	
	public SyntaxAttributeCalculator(JCas jcas, Markable m1, Markable m2, WikiIndex wiki) {
		super(jcas,m1,m2);
		n1 = MarkableTreeUtils.markableNode(jcas, m1.getBegin(), m1.getEnd());
		n2 = MarkableTreeUtils.markableNode(jcas, m2.getBegin(), m2.getEnd());
		lca = n2;
		while(true){
			if(n1 == null || lca == null || lca.getBegin() <= n1.getBegin()){
				break;
			}
			lca = lca.getParent();
		}
		ngrams = new HashMap<String,Integer>();
		calcFullPath();
		this.wiki = wiki;
		if(this.wiki != null) initWikiSim();
//		c1 = MarkableDepUtils.markableNode(jcas, m1.getBegin(), m1.getEnd(), n1);
//		c2 = MarkableDepUtils.markableNode(jcas, m2.getBegin(), m2.getEnd(), n2);
//		depLca = getDepLCA(c1,c2);
//		calcDepPath();
	}

	public static int getNumNEFeats(){	return numNEFeats;	}
	public static int getNumDemFeats(){ return numDemFeats; }
	public static int getNumPronFeats(){ return numPronFeats; }
	
	private static String calcNPunderPP(TreebankNode n){
		if(n != null && n.getParent() != null && n.getParent().getNodeType().equals("PP")){
			return "Y";
		}
		return "N";
	}

	public String calcNPunderPP1(){
		return calcNPunderPP(n1);
	}

	public String calcNPunderPP2(){
		return calcNPunderPP(n2);
	}

	private static String calcNPunderS(TreebankNode n){
		if(n != null && n.getParent() != null && n.getParent().getNodeType().equals("S")){
			return "Y";
		}
		return "N";
	}

	public String calcNPunderS1(){
		return calcNPunderS(n1);
	}

	public String calcNPunderS2(){
		return calcNPunderS(n2);
	}

	private static String calcNPunderVP(TreebankNode n){
		if(n != null && n.getParent() != null && n.getParent().getNodeType().equals("VP")){
			return "Y";
		}
		return "N";
	}

	public String calcNPunderVP1(){
		return calcNPunderVP(n1);
	}

	public String calcNPunderVP2(){
		return calcNPunderVP(n2);
	}

	public boolean calcNPSubj(TreebankNode n){
		if(n == null) return false;
		if(n.getNodeType().equals("NP")){
			StringArray tags = n.getNodeTags();
			if(tags != null && tags.size() > 0){
				for(int i = 0; i < tags.size(); i++){
					if(tags.get(i).equals("SBJ")){
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean calcNPSubj1(){
		return calcNPSubj(n1);
	}
	
	public boolean calcNPSubj2(){
		return calcNPSubj(n2);
	}
	
	public boolean calcNPSubjBoth(){
		return (calcNPSubj1() && calcNPSubj2());
	}

	public void initWikiSim(){
		if(wiki == null) sim1 = 0.0;
		else{
			try{
				sim1 = wiki.getCosineSimilarity(ms1, ms2);
				sim2 = wiki.getCosineSimilarity(es1, es2);
			}catch(Exception e){
				sim1 = 0.0;
				sim2 = 0.0;
			}
		}
	}

	public void initEntityWikiSim(){
		if(wiki == null) sim2 = 0.0;
		else{
			try{
				sim2 = wiki.getCosineSimilarity(es1, es2);
			}catch(Exception e){
				sim2 = 0.0;
			}
		}		
	}
	
	public double calcWikiSim(){
		if(sim1 < 0.0) initWikiSim();
		return sim1;
	}
	
	public double calcEntityWikiSim(){
		if(sim2 < 0.0) initEntityWikiSim();
		return sim2;
	}
	
	public double calcSimSum(){
		if(sim1 < 0.0) initWikiSim();
		if(sim2 < 0.0) initEntityWikiSim();
		return (sim1+sim2)/2.0;
	}

	public int numNgrams(Markable m) throws UnexpectedException{
		if(m instanceof NEMarkable){
			return featSet.size();
		}else if(m instanceof PronounMarkable){
			return pronFeatSet.size();
		}else if(m instanceof DemMarkable){
			return demFeatSet.size();
		}else{
			throw new UnexpectedException("The type passed into numNgrams was not expected!");
		}
	}

	public String calcCatNgrams(Integer i, Markable m){
		if(m instanceof NEMarkable){
			if(ngrams.containsKey(featSet.get(i))){
				return "Y";
			}
		}else if(m instanceof DemMarkable){
			if(ngrams.containsKey(demFeatSet.get(i))){
				return "Y";
			}
		}else if(m instanceof PronounMarkable){
			if(ngrams.containsKey(pronFeatSet.get(i))){
				return "Y";
			}
		}
		return "N";
	}

	public String calcFullPath(){
		if(path==null){
			if(n1 == null || n2 == null || n2.getBegin() <= n1.getEnd()){
				path = "";
			}else{
				// First make our way up from the antecedent (node 2), stopping when we hit the LCA or
				// the node labeled TOP (the LCA of all trees in the absence of a discourse model)
				StringBuffer buf = new StringBuffer();
				TreebankNode cur = n2.getParent();
				//				if(n2 instanceof TerminalTreebankNode) cur = cur.getParent();

				while(cur != lca && !(cur instanceof TopTreebankNode)){
					buf.append(cur.getNodeType());
					buf.append("<");
					if(cur.getParent() != null){
						cur = cur.getParent();
					}else{
						break;
					}
				}
				buf.append(lca==null?"TOP":lca.getNodeType());

				StringBuffer bwd = new StringBuffer();
				//			if(lca == null) cur = n1.getRoot();
				cur = n1.getParent();
				//				if(n1 instanceof TerminalTreebankNode) cur = cur.getParent();
				while(cur != lca && !(cur instanceof TopTreebankNode)){
					bwd.insert(0,cur.getNodeType());
					bwd.insert(0,">");
					cur = cur.getParent();
				}
				buf.append(bwd);
				path = buf.toString();
				initNGrams(ngrams, path,3);
				initNGrams(ngrams, path,4);
				initNGrams(ngrams, path,5);
			}
		}
		return path;
	}

	public double calcPathLength(){
		return getPathLength() / (double) CorefConsts.SENTDIST;
	}
	
	public int getPathLength(){
		String[] nodes = path.split("[<>]");
		return nodes.length;
	}

	private static ConllDependencyNode getDepLCA(ConllDependencyNode c1, ConllDependencyNode c2) {
		HashSet<Annotation> ancestors = new HashSet<Annotation>();
		ConllDependencyNode temp = null;
		temp = c2.getHead();
		while(temp != null){
			ancestors.add(temp);
			temp = temp.getHead();
		}
		temp = c1.getHead();
		while(temp != null){
			if(ancestors.contains(temp)){
				break;
			}
			temp = temp.getHead();
		}
		return temp;
	}

	public String calcDepPath(){
		if(depPath == null){
			if(c1 == null || c2 == null || c2.getBegin() <= c1.getEnd()){
				depPath = "";
			}else{
				StringBuffer buf = new StringBuffer();

				// first go up from the anaphor to the Lowest common ancestor... (LCA)
				buf.append(c2.getDeprel());
				ConllDependencyNode cur = c2.getHead();
				while(cur != depLca && cur != null){
					String rel = cur.getDeprel();
					if(rel == null){
						cur = null;
						break;
					}
					buf.append("<");
					buf.append(cur.getDeprel());
					cur = cur.getHead();
				}

				// add a "discourse node" if the relation goes between sentences.
				if(cur == null) buf.append("<TOP");

				// now up from the antecedent to the LCA
				StringBuffer bwd = new StringBuffer();
				bwd.append(c1.getDeprel());
				bwd.insert(0, ">");
				cur = c1.getHead();
				while(cur != depLca && cur != null){
					String rel = cur.getDeprel();
					if(rel == null){
						cur = null;
						break;
					}
					bwd.insert(0,cur.getDeprel());
					bwd.insert(0,">");
					cur = cur.getHead();
				}

				buf.append(bwd);
				depPath = buf.toString();
				initNGrams(ngrams, depPath, 3);
				initNGrams(ngrams, depPath, 4);
				initNGrams(ngrams, depPath, 5);
			}
		}
		return depPath;
	}

	private static void initNGrams(HashMap<String,Integer> ngrams, String path, int n) {
		// Find the collection of trigrams in this string and add them to the hash map.
		// start by finding the endpoint of the first trigram, then iteratively move the endpoint forward one unit
		// while moving a beginning point forward one gram as well.
		int begin=0;
		int end = 0;
		int numBracks = 0;

		while(numBracks < n && end < path.length()-1){
			end++;
			if(path.charAt(end) == '<' || path.charAt(end) == '>'){
				numBracks++;
			}
		}

		if(numBracks < (n-1)) return;

		//	System.err.println("begin = " + begin + " and end = " + end);
		String tg = null;
		if(end == path.length()-1){
			tg = path.substring(begin);
		}else{
			tg = path.substring(begin, end);
		}
		//	System.err.println(tg);
		int count = 0;
		try{
			count = (ngrams.containsKey(tg) ? ngrams.get(tg) : 0) + 1;
			ngrams.put(tg, count);
		}catch(NullPointerException e){
			System.err.println("Choked on key: " + (tg==null?"null":tg));
		}

		// while there are still characters:
		while(end < path.length()-1){
			// increment end pointer
			while(end < path.length()-1){
				end++;
				if(path.charAt(end) == '<' || path.charAt(end) == '>'){
					break;
				}
			}
			// increment beginning pointer
			while(true){
				begin++;
				if(path.charAt(begin) == '<' || path.charAt(begin) == '>'){
					begin++;
					break;
				}
			}
			if(end == path.length()-1) ++end;
			tg = path.substring(begin, end);
			count = (ngrams.containsKey(tg) ? ngrams.get(tg) : 0) + 1;
			ngrams.put(tg, count);
			//		System.err.println(tg);
		}
	}

	public HashMap<String, Integer> getNGrams() {
		return ngrams;
	}
}
