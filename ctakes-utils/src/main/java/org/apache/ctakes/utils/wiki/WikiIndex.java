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
package org.apache.ctakes.utils.wiki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 * A wrapper for a wikipedia lucene index.
 * 
 * @author dmitriy dligach
 *
 */
public class WikiIndex {

	public static int defaultMaxHits = 10;
	// TODO: remove dependency of a hardcoded path
	public static String defaultIndexPath = "/home/dima/i2b2/wiki-index/index_nometa";
	public static String defaultSearchField = "text";

	private int maxHits;
	private String indexPath;
	private String searchField;

	private IndexReader indexReader;
	private IndexSearcher indexSearcher;
	private Analyzer standardAnalyzer;
	private QueryParser queryParser;
	private DefaultSimilarity similarity;
	private int numDocs;
  	
  private boolean useCache = true;
  private Cache lastQuery = null;
  
  public WikiIndex(int maxHits, String indexPath, String searchField, boolean approximate) {
  	this.maxHits = maxHits;
  	this.indexPath = indexPath;
  	this.searchField = searchField;
  	this.similarity = approximate ? new ApproximateSimilarity() : new DefaultSimilarity();
  }
  
  public WikiIndex(int maxHits, String indexPath, String searchField){
	  this(maxHits, indexPath, searchField, false);
  }
  
  public WikiIndex() {
  	maxHits = defaultMaxHits;
  	indexPath = defaultIndexPath;
  	searchField = defaultSearchField;
  }
  
  public void initialize() throws CorruptIndexException, IOException {

  	indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
  	numDocs = indexReader.numDocs();
  	indexSearcher = new IndexSearcher(indexReader);
  	standardAnalyzer = new StandardAnalyzer(Version.LUCENE_40);
  	queryParser = new QueryParser(Version.LUCENE_40, searchField, standardAnalyzer);
  	lastQuery = new Cache();
  }
  
  /**
   * Search the index. Return a list of article titles and their scores.
 * @throws org.apache.lucene.queryparser.classic.ParseException 
   */
  public ArrayList<SearchResult> search(String queryText) throws ParseException, IOException {

  	ArrayList<SearchResult> articleTitles = new ArrayList<SearchResult>();
  	
  	String escaped = QueryParser.escape(queryText);
  	Query query = queryParser.parse(escaped);
  	
  	ScoreDoc[] scoreDocs = indexSearcher.search(query, null, maxHits).scoreDocs;
  	for(ScoreDoc scoreDoc : scoreDocs) {
  		ScoreDoc redirectScoreDoc = handlePossibleRedirect(scoreDoc);
  		Document doc = indexSearcher.doc(redirectScoreDoc.doc);
  		articleTitles.add(new SearchResult(doc.get("title"), redirectScoreDoc.score));
  	}
  	
  	return articleTitles;
  }
  
  /**
   * Send two queries to the index.
   * For each query, form a tfidf vector that represents N top matching documents.
   * Return cosine similarity between the two tfidf vectors.
   */
  public double getCosineSimilarity(String queryText1, String queryText2) throws ParseException, IOException {
	  HashMap<String, Double> vector1  = null;
	  if(useCache && lastQuery.t1 != null && lastQuery.t1.equals(queryText1)){
		  vector1 = lastQuery.v1;
	  }else if(useCache && lastQuery.t2 != null && lastQuery.t2.equals(queryText1)){
		  vector1 = lastQuery.v2;
	  }else{
		  // start from scratch
		  ArrayList<Terms> termFreqVectors1 = getTermFreqVectors(queryText1);
		  if(termFreqVectors1.size() == 0) return 0;
		  vector1 = makeTfIdfVector(termFreqVectors1);
	  }
	  
	  if(vector1.size() == 0) {
		  return 0; // e.g. redirects to a non-existent page
	  }
	  
	  HashMap<String, Double> vector2 = null;
	  if(useCache && lastQuery.t1 != null && lastQuery.t1.equals(queryText2)){
		  vector2 = lastQuery.v1;
	  }else if(useCache && lastQuery.t2 != null && lastQuery.t2.equals(queryText2)){
		  vector2 = lastQuery.v2;
	  }else{	  
		  ArrayList<Terms> termFreqVectors2 = getTermFreqVectors(queryText2);
		  if(termFreqVectors2.size() == 0) return 0;
		  vector2 = makeTfIdfVector(termFreqVectors2);
	  }
	  
	  if(vector2.size() == 0) {
		  return 0; // e.g. redirects to a non-existent page
	  }

	  if(useCache){
		  lastQuery.t1 = queryText1;
		  lastQuery.v1 = vector1;
		  lastQuery.t2 = queryText2;
		  lastQuery.v2 = vector2;
	  }

	  double dotProduct = computeDotProduct(vector1, vector2);
	  double norm1 = computeEuclideanNorm(vector1);
	  double norm2 = computeEuclideanNorm(vector2);

	  return dotProduct / (norm1 * norm2);
  }

  public ArrayList<Terms> getTermFreqVectors(String queryString) throws ParseException, IOException{
	  	String escaped = QueryParser.escape(queryString);
	  	Query query = queryParser.parse(escaped);
	  	ScoreDoc[] scoreDocs = indexSearcher.search(query, maxHits).scoreDocs;

	  	ArrayList<Terms> termFreqVectors = new ArrayList<Terms>();
	  	for(ScoreDoc scoreDoc : scoreDocs) {
	  		ScoreDoc redirectScoreDoc = handlePossibleRedirect(scoreDoc);
	  		Terms termFreqVector = indexReader.getTermVector(redirectScoreDoc.doc, "text");
	  		termFreqVectors.add(termFreqVector);
	  	}

	  	return termFreqVectors;
  }
  
  /**
   * Form a tfidf vector for the set of pages matching each query. 
   * Return the terms that are common to the two sets.
   */
//  public ArrayList<String> getCommmonTerms(String queryText1, String queryText2) throws ParseException, IOException {
//
//  	String escaped1 = QueryParser.escape(queryText1);
//  	Query query1 = queryParser.parse(escaped1);
//  	ScoreDoc[] scoreDocs1 = indexSearcher.search(query1, null, maxHits).scoreDocs;
//  	
//  	ArrayList<TermFreqVector> termFreqVectors1 = new ArrayList<TermFreqVector>();
//  	for(ScoreDoc scoreDoc : scoreDocs1) {
//  		ScoreDoc redirectScoreDoc = handlePossibleRedirect(scoreDoc);
//  		termFreqVectors1.add(indexReader.getTermFreqVector(redirectScoreDoc.doc, "text"));
//  	}
//  	HashMap<String, Double> vector1 = makeTfIdfVector(termFreqVectors1);
//  	
//  	String escaped2 = QueryParser.escape(queryText2);
//  	Query query2 = queryParser.parse(escaped2);
//  	ScoreDoc[] scoreDocs2 = indexSearcher.search(query2, null, maxHits).scoreDocs;
//  	
//  	ArrayList<TermFreqVector> termFreqVectors2 = new ArrayList<TermFreqVector>();
//  	for(ScoreDoc scoreDoc : scoreDocs2) {
//  		ScoreDoc redirectScoreDoc = handlePossibleRedirect(scoreDoc);
//  		termFreqVectors2.add(indexReader.getTermFreqVector(redirectScoreDoc.doc, "text"));
//  	}
//  	HashMap<String, Double> vector2 = makeTfIdfVector(termFreqVectors2);
//  	
//  	
//  	HashMap<String, Double> sum = addVectors(vector1, vector2);
//  	
//    Function<String, Double> getValue = Functions.forMap(sum);
//    ArrayList<String> keys = new ArrayList<String>(sum.keySet());
//  	Collections.sort(keys, Ordering.natural().reverse().onResultOf(getValue)); 
//  	
//  	return removeStringsFromList(queryText1, queryText2, keys);
//  }

  /**
   * Take a list of strings and remove all occurences of two string arguments from it. Use stemming.
   */
//  private static ArrayList<String> removeStringsFromList(String s1, String s2, ArrayList<String> list) {
//  	
//  	String stem1 = getStem(s1);
//  	String stem2 = getStem(s2);
//  	
//  	ArrayList<String> result = new ArrayList<String>();
//  	
//  	for(String s : list) {
//  		String stem = getStem(s);
//  		if(stem.equals(stem1) || stem.equals(stem2)) {
//  			continue;
//  		}
//  		result.add(s);
//  	}
//  	return result;
//  }
//  
  /**
   * Stem a word using Porter stemmer
   */
//  private static String getStem(String word) {
//  	
//		PorterStemmer stemmer = new PorterStemmer();
//		stemmer.add(word.toCharArray(), word.length());
//		stemmer.stem();
//		
//		return stemmer.toString();
//  }
  
  
  /**
   * Return the document to which the input document redirects. 
   * Return the same document if there is no redirect for the input document.
   */
  private ScoreDoc handlePossibleRedirect(ScoreDoc scoreDoc) throws ParseException, CorruptIndexException, IOException  {
  
  	Document doc = indexSearcher.doc(scoreDoc.doc);
  	String redirectTitle = doc.get("redirect"); 
  	
  	// check if there is a redirect
  	if(redirectTitle == null) {
  		return scoreDoc; 
  	}
  	
  	QueryParser redirectQueryParser = new QueryParser(Version.LUCENE_40, "title", standardAnalyzer);

  	String redirectTitleNoUnderscores = redirectTitle.replaceAll("_", " ");
  	String redirectTitleQuoted = '"' + redirectTitleNoUnderscores + '"';
  	String redirectTitleEscaped = QueryParser.escape(redirectTitleQuoted);
  	Query redirectQuery  = redirectQueryParser.parse(redirectTitleEscaped);

  	ScoreDoc[] redirectScoreDocs = indexSearcher.search(redirectQuery, null, 1).scoreDocs; 
  	if(redirectScoreDocs.length < 1) {
  		System.out.println("failed redirect: " + redirectTitle + " -> " + redirectTitle);
  		return scoreDoc; // redirect query did not return any results
  	}
  	ScoreDoc redirectScoreDoc = redirectScoreDocs[0];

  	return redirectScoreDoc;

  }
  
  /**
   * Return a hash table that maps terms to their tfidf values.
   * The input is a list of TermFreqVector objects. The return
   * value is formed by summing up individual tfidf vectors.
   */
  private HashMap<String, Double> makeTfIdfVector(ArrayList<Terms> termFreqVectors) throws IOException {

  	// map terms to their tfidf values
	  CounterMap<String> countVector = new CounterMap<String>();
  	HashMap<String, Double> tfIdfVector = new HashMap<String, Double>(); 

  	for(Terms terms : termFreqVectors) {
  		if(terms == null) {
  			continue; // some documents are empty
  		}
  		
//  		String[] terms = termFreqVector.getTerms();
//  		int[] freqs = termFreqVector.getTermFrequencies();
  		TermsEnum termsEnum = terms.iterator(null);

  		while(termsEnum.next() != null){
  			BytesRef term = termsEnum.term();
  			String termStr = term.utf8ToString();
  			countVector.add(termStr);
  		}
  		
  		for(String key : countVector.keySet()){
  			double tf = similarity.tf((long)countVector.get(key));
  			double idf = similarity.idf(indexReader.docFreq(new Term("text", key)), numDocs);
  			tfIdfVector.put(key, tf*idf);
  		}
/*  		for(int i = 0; i < terms.length; i++) {
  			double tf = similarity.tf(freqs[i]); // defaultSimilarity.tf(freqs[i]);
  			double idf = similarity.idf(indexReader.docFreq(new Term("text", terms[i])), numDocs);
  			
  			if(tfIdfVector.containsKey(terms[i])) {
  				tfIdfVector.put(terms[i], tfIdfVector.get(terms[i]) + tf * idf);
  			}
  			else {
  				tfIdfVector.put(terms[i], tf * idf);
  			}
  		} */
  	}
  	return tfIdfVector;
  }
  
  private double computeEuclideanNorm(HashMap<String, Double> tfIdfVector) {
  	
  	double sumOfSquares = 0;
  	
  	for(double tfidf : tfIdfVector.values()) {
  		sumOfSquares = sumOfSquares + tfidf*tfidf;  //Math.pow(tfidf, 2);
  	}
  	
  	return ApproximateMath.asqrt(sumOfSquares);
  }
  
  private double computeDotProduct(HashMap<String, Double> vector1, HashMap<String, Double> vector2) {
  	
  	double dotProduct = 0;
  	Map<String, Double> smallSet = null;
  	Map<String, Double> largeSet = null;
  	if(vector1.size() > vector2.size()){
  		smallSet = vector2;
  		largeSet = vector1;
  	}else{
  		smallSet = vector1;
  		largeSet = vector2;
  	}
  	
  	for(String term : smallSet.keySet()) {
  		if(largeSet.containsKey(term)) {
  			dotProduct = dotProduct + smallSet.get(term) * largeSet.get(term);
  		}
  	}
  	
  	return dotProduct;
  }

  private HashMap<String, Double> addVectors(HashMap<String, Double> vector1, HashMap<String, Double> vector2) {
  	
  	HashMap<String, Double> sum = new HashMap<String, Double>();
  	Map<String, Double> smallSet = null;
  	Map<String, Double> largeSet = null;
  	if(vector1.size() > vector2.size()){
  		smallSet = vector2;
  		largeSet = vector1;
  	}else{
  		smallSet = vector1;
  		largeSet = vector2;
  	}
  	
  	for(String term : smallSet.keySet()) {
  		if(largeSet.containsKey(term)) {
  			sum.put(term, smallSet.get(term) + largeSet.get(term));
  		}
  	}
  	
  	return sum;
  }
  
  public void close() throws IOException {
  	
  	indexReader.close();
//  	indexSearcher.close();
  	standardAnalyzer.close();
  }
}

class Cache{
	String t1 = null;
	String t2 = null;
	HashMap<String,Double> v1 = null;
	HashMap<String,Double> v2 = null;
}
