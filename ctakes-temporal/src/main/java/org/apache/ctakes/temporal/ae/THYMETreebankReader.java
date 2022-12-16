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
package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ContractionToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.SymbolToken;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Level;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.treebank.TreebankFormatParser;

public class THYMETreebankReader extends JCasAnnotator_ImplBase {

	public static Logger logger = Logger.getLogger(THYMETreebankReader.class);
	public static final String TREEBANK_DIRECTORY = "treebankDirectory";
	private static final Pattern headerPatt = Pattern.compile("\\[(meta|start|end) [^\\]]*?\\]"); //"\\[meta [^\\]]*\\]");
	
	@ConfigurationParameter(name = TREEBANK_DIRECTORY, mandatory = true)
	protected File treebankDirectory;
	File[] subdirs = null;

	enum TOKEN_TYPE {WORD, PUNCT, SYMBOL, NUM, NEWLINE, CONTRACTION }
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		subdirs = treebankDirectory.listFiles(new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() && !pathname.isHidden();
			}});
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		URI uri = ViewUriUtil.getURI(jcas);
		logger.info("Document id is: " + uri.toString());

		String fn = uri.getPath().substring(uri.getPath().lastIndexOf('/')+1) + ".xml.tree";
		File treeFile = null;
		for(File subdir : subdirs){
			treeFile = new File(subdir, fn);
			if(treeFile.exists()) break;
			treeFile = null;
		}

		if(treeFile == null){
			this.getContext().getLogger().log(Level.WARNING,"Could not find treeFile: " + fn);
			// FIXME do automatic parse?
			return;
		}

		String tbText;

		try {
			tbText = FileUtils.file2String(treeFile);
		} catch (IOException e1) {
			// shouldn't do automatic parse here -- something wrong with file itself, not the parse
			throw new AnalysisEngineProcessException(e1);
		}

		StringBuffer fileText = new StringBuffer(jcas.getDocumentText());
		
		// find and replace section headers with whitespace so the TreebankFormatParser skips over them...
		Matcher m = headerPatt.matcher(fileText);
		while(m.find()){
			int headerLen = m.group().length();
			fileText.replace(m.start(), m.end(), getWhitespaceString(headerLen));
		}
		
		List<org.cleartk.util.treebank.TopTreebankNode> utilTrees;
		try {
			utilTrees = TreebankFormatParser.parseDocument(tbText, 0, fileText.toString());
		} catch (Exception e) {
			this.getContext().getLogger().log(Level.WARNING,
					String.format("Skipping %s due to alignment problems", fn),
					e);
			// FIXME - do automatic parse here...
			
			return;
		}
		
		// if we get this far, the gold standard exists and we will let it do all of our tokenization.
		// first we need to remove sentence and token annotations
		List<Sentence> sents = new ArrayList<Sentence>(JCasUtil.select(jcas, Sentence.class));
		for(Sentence sent : sents){
			sent.removeFromIndexes();
		}
		HashMap<String,TOKEN_TYPE> tokMap = new HashMap<>();
		List<BaseToken> toks = new ArrayList<BaseToken>(JCasUtil.select(jcas, BaseToken.class));
		for(BaseToken tok : toks){
		  String key = getAnnotationKey(tok);
		  
		  if(tok instanceof WordToken){
		    tokMap.put(key, TOKEN_TYPE.WORD);
		  }else if(tok instanceof PunctuationToken){
		    tokMap.put(key, TOKEN_TYPE.PUNCT);
		  }else if(tok instanceof SymbolToken){
		    tokMap.put(key, TOKEN_TYPE.SYMBOL);
		  }else if(tok instanceof NumToken){
		    tokMap.put(key,  TOKEN_TYPE.NUM);
		  }else if(tok instanceof NewlineToken){
		    tokMap.put(key, TOKEN_TYPE.NEWLINE);
		  }else if(tok instanceof ContractionToken){
		    tokMap.put(key, TOKEN_TYPE.CONTRACTION);
		  }
			tok.removeFromIndexes();
		}
		

		// add Token, Sentence and TreebankNode annotations for the text
		for (org.cleartk.util.treebank.TopTreebankNode utilTree : utilTrees) {

			// create a Sentence and set its parse
			TopTreebankNode tree = convert(utilTree, jcas);
			Sentence sentence = new Sentence(jcas, tree.getBegin(), tree.getEnd());
			sentence.addToIndexes();

			// create the Tokens and add them to the Sentence
			for (int i = 0; i < tree.getTerminals().size(); i++) {
				TreebankNode leaf = tree.getTerminals(i);
        if (leaf.getBegin() != leaf.getEnd()) {
          String key = getAnnotationKey(leaf);
          BaseToken token = null;
          if(tokMap.containsKey(key)){
            TOKEN_TYPE tokType = tokMap.get(key);
            switch(tokType){            
            case CONTRACTION:
              token = new ContractionToken(jcas, leaf.getBegin(), leaf.getEnd());
              break;
            case NEWLINE:
              token = new NewlineToken(jcas, leaf.getBegin(), leaf.getEnd());
              break;
            case NUM:
              token = new NumToken(jcas, leaf.getBegin(), leaf.getEnd());
              break;
            case PUNCT:
              token = new PunctuationToken(jcas, leaf.getBegin(), leaf.getEnd());
              break;
            case SYMBOL:
              token = new SymbolToken(jcas, leaf.getBegin(), leaf.getEnd());
              break;
            case WORD:
              token = new WordToken(jcas, leaf.getBegin(), leaf.getEnd());
              break;
            default:
              token = new BaseToken(jcas, leaf.getBegin(), leaf.getEnd());
            }
          }else{
            token = new BaseToken(jcas, leaf.getBegin(), leaf.getEnd());
          }
          token.setPartOfSpeech(leaf.getNodeType());
          token.addToIndexes();
        }
			}
		}
	}

	private static String getWhitespaceString(int headerLen) {
		char[] chars = new char[headerLen];
		Arrays.fill(chars, ' ');
		return new String(chars);
	}

	// the ctakes syntax typesystem was modeled after cleartk -- as a result, the following methods borrow very liberally from 
	// org.cleartk.syntax.constituent.util.TreebankNodeUtility, which has a convert method for going from
	// a "normal" tree to a cleartk/uima tree.  This does the same, except goes to a ctakes/uima tree.
	private static TopTreebankNode convert(org.cleartk.util.treebank.TopTreebankNode inTree, JCas jcas){
		TopTreebankNode outTree = new TopTreebankNode(jcas, inTree.getTextBegin(), inTree.getTextEnd());
		outTree.setTreebankParse(inTree.getTreebankParse());
	    convert(inTree, jcas, outTree, null);
	    initTerminalNodes(outTree, jcas);


		outTree.addToIndexes();
		return outTree;
	}

	public static void initTerminalNodes(
			TopTreebankNode uimaNode,
			JCas jCas) {
		List<TerminalTreebankNode> terminals = new ArrayList<TerminalTreebankNode>();
		_initTerminalNodes(uimaNode, terminals);

		for (int i = 0; i < terminals.size(); i++) {
			TerminalTreebankNode terminal = terminals.get(i);
			terminal.setIndex(i);
		}

		FSArray terminalsFSArray = new FSArray(jCas, terminals.size());
		terminalsFSArray.copyFromArray(
				terminals.toArray(new FeatureStructure[terminals.size()]),
				0,
				0,
				terminals.size());
		uimaNode.setTerminals(terminalsFSArray);
	}

	private static void _initTerminalNodes(
			TreebankNode node,
			List<TerminalTreebankNode> terminals) {
		FSArray children = node.getChildren();
		for (int i = 0; i < children.size(); i++) {
			TreebankNode child = (TreebankNode) children.get(i);
			if (child instanceof TerminalTreebankNode) {
				terminals.add((TerminalTreebankNode) child);
			} else
				_initTerminalNodes(child, terminals);
		}
	}

	public static TreebankNode convert(
			org.cleartk.util.treebank.TreebankNode pojoNode,
			JCas jCas,
			TreebankNode uimaNode,
			TreebankNode parentNode) {
		uimaNode.setNodeType(pojoNode.getType());
		uimaNode.setNodeTags(new StringArray(jCas, pojoNode.getTags().length));
		FSCollectionFactory.fillArrayFS(uimaNode.getNodeTags(), pojoNode.getTags());
		uimaNode.setNodeValue(pojoNode.getValue());
		uimaNode.setLeaf(pojoNode.isLeaf());
		uimaNode.setParent(parentNode);

		List<TreebankNode> uimaChildren = new ArrayList<TreebankNode>();
		for (org.cleartk.util.treebank.TreebankNode child : pojoNode.getChildren()) {
			TreebankNode childNode;
			if (child.isLeaf()) {
				childNode = new TerminalTreebankNode(jCas, child.getTextBegin(), child.getTextEnd());
			} else {
				childNode = new TreebankNode(
						jCas,
						child.getTextBegin(),
						child.getTextEnd());
			}
			uimaChildren.add(convert(child, jCas, childNode, uimaNode));
			childNode.addToIndexes();
		}
		FSArray uimaChildrenFSArray = new FSArray(jCas, uimaChildren.size());
		uimaChildrenFSArray.copyFromArray(
				uimaChildren.toArray(new FeatureStructure[uimaChildren.size()]),
				0,
				0,
				uimaChildren.size());
		uimaNode.setChildren(uimaChildrenFSArray);
		return uimaNode;
	}

	public static AnalysisEngineDescription getDescription(File treebankDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				THYMETreebankReader.class,
				THYMETreebankReader.TREEBANK_DIRECTORY,
				treebankDirectory);
	}
	
	public static void main(String[] args){
		String testString = "[meta rev_date=\"02/20/2010\" start_date=\"02/20/2010\" rev=\"0002\"]\n\n" +
						    "[start section id=\"20112\"]\n\n" +
						    "#1 Dilated esophagus on CT-scan\n" +
						    "#2 Adenocarcinoma right colon\n" +
						    "#3 Symptomatic anemia\n" +
						    "#4 Hypothyroidism";
		Matcher m = headerPatt.matcher(testString);
//		System.out.println("Matches = " + m.matches());
		
		while(m.find()){
			System.out.println("FOund match at: " + m.start() + "-" + m.end());
		}
	}
	
	public static final String getAnnotationKey(Annotation a){
	  return a.getBegin() + "-" + a.getEnd();
	}
}
