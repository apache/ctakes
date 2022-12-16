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
package org.apache.ctakes.constituency.parser.util;

import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.util.Span;
import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final public class TreeUtils {

   static private final Pattern WHITESPACE_PATTERN = Pattern.compile( "\\s++" );

   static private final Map<String, String> BRACKET_MAP = new HashMap<>( 6 );

   static {
      BRACKET_MAP.put( "(", "-LRB-" );
      BRACKET_MAP.put( "[", "-LRB-" );
      BRACKET_MAP.put( ")", "-RRB-" );
      BRACKET_MAP.put( "]", "-RRB-" );
      BRACKET_MAP.put( "{", "-LCB-" );
      BRACKET_MAP.put( "}", "-RCB-" );
   }


   private TreeUtils() {
   }




	public static List<TreebankNode> getNodeList(TopTreebankNode tree){
		ArrayList<TreebankNode> list = new ArrayList<TreebankNode>();
		list.add(tree);
		int ind = 0;	
		while(ind < list.size()){
			TreebankNode cur = list.get(ind);
			for(int i = 0; i < cur.getChildren().size(); i++){
				list.add(cur.getChildren(i));
			}
			ind++;
		}
		return list;
	}
	
	public static List<Parse> getNodeList(Parse tree){
		ArrayList<Parse> list = new ArrayList<Parse>();
		list.add(tree);
		int ind = 0;	
		while(ind < list.size()){
			Parse cur = list.get(ind);
			Parse[] children = cur.getChildren();
			for(int i = 0; i < children.length; i++){
				list.add(children[i]);
			}
			ind++;
		}
		return list;
	}
	
	public static String tree2str(TreebankNode pathTree){
		StringBuffer buff = new StringBuffer();
		buff.append("(");
		try{
		buff.append(pathTree.getNodeType());
		}catch(Exception e){
			System.err.println("Caught NPE");
		}
		if(pathTree.getLeaf()){ //pathTree.getChildren().size() == 1 && pathTree.getChildren(0).getLeaf()){
			buff.append(" ");
			buff.append(pathTree.getNodeValue());
//			buff.append(")");
		}else{
			for(int i = 0; i < pathTree.getChildren().size(); i++){
				buff.append(" ");
				buff.append(tree2str(pathTree.getChildren(i)));
			}
		}
		buff.append(")");
		return buff.toString();
	}

//	public static boolean contains(TreebankNode n, SimpleTree frag){
//		if(fragmentMatch(n,frag)) return true;
//		
//		for(int i = 0; i < n.getChildren().size(); i++){
//			if(fragmentMatch(n.getChildren(i), frag)) return true;
//		}
//		return false;
//	}
//	
//	private static boolean fragmentMatch(TreebankNode n, SimpleTree frag){
//		boolean same = false;
//		if(n.getNodeType().equals(frag.cat) && (frag.children.size() == 0 || n.getChildren().size() == frag.children.size())){
//			same = true;
//			for(int i = 0; i < frag.children.size(); i++){
//				if(!fragmentMatch(n.getChildren(i), frag.children.get(i))){
//					same = false;
//					break;
//				}
//			}
//		}
//		return same;
//	}
	
	public static boolean containsIgnoreCase(SimpleTree node, SimpleTree frag){
		return contains(node, frag, true);
	}

	public static boolean contains(SimpleTree node, SimpleTree frag){
		return contains(node, frag, false);
	}

	public static boolean contains(SimpleTree node, SimpleTree frag, boolean ignoreCase){
		if(fragmentMatch(node,frag, ignoreCase)) return true;

		for(int i = 0; i < node.children.size(); i++){
			if(contains(node.children.get(i), frag, ignoreCase)) return true;
		}
		return false;
	}

	public static int countFrags(SimpleTree node, SimpleTree frag){
	  int count = 0;
	  
	  if(fragmentMatch(node, frag, true)) count++;
	  
	  for(int i = 0; i < node.children.size(); i++){
	    count += countFrags(node.children.get(i), frag);
	  }
	  return count;
	}
	
	private static boolean fragmentMatch(SimpleTree node, SimpleTree frag, boolean ignoreCase){
		boolean same = false;
		if((ignoreCase && node.cat.equalsIgnoreCase(frag.cat)) || (!ignoreCase && node.cat.equals(frag.cat))){
			if((frag.children.size() == 0 || node.children.size() == frag.children.size())){

				same = true;
				for(int i = 0; i < frag.children.size(); i++){
					if(!fragmentMatch(node.children.get(i), frag.children.get(i), ignoreCase)){
						same = false;
						break;
					}
				}
			}
		}
		return same;
	}
	
	public static int countDepFrags(SimpleTree node, SimpleTree frag){
	  int count = 0;
	  if(depFragmentMatch(node, frag, true)) count++;
	  
	  for(int i = 0; i < node.children.size(); i++){
	    count += countFrags(node.children.get(i), frag);
	  }
	  return count;
	}
	
	public static boolean containsDepFragIgnoreCase(SimpleTree node, SimpleTree frag){
	   return containsDepFrag(node, frag, true);
	}

	public static boolean containsDepFrag(SimpleTree node, SimpleTree frag, boolean ignoreCase){
	  if(depFragmentMatch(node, frag, ignoreCase)) return true;
	  
	  for(int i = 0; i < node.children.size(); i++){
	    if(containsDepFrag(node.children.get(i), frag, ignoreCase)) return true;
	  }
	  return false;
	}
	
	private static boolean depFragmentMatch(SimpleTree node, SimpleTree frag, boolean ignoreCase){
	  boolean same = false;
	  if(frag.children.size() > 1){
	    System.err.println("Only chain fragments are currently supported!");
	    throw new UIMA_UnsupportedOperationException();
	  }
	  
	  if((ignoreCase && node.cat.equalsIgnoreCase(frag.cat)) || (!ignoreCase && node.cat.equals(frag.cat))){
	    if(frag.children.size() == 0){
	      return true;
	    }
	    for(int i = 0; i < node.children.size(); i++){
	      if(depFragmentMatch(node.children.get(i), frag.children.get(0), ignoreCase)){
	        return true;
	      }
	    }
	  }
	  
	  return same;
	}

	public static int getHighestIndexTerm(TreebankNode inTree) {
		if(inTree instanceof TerminalTreebankNode){
			return ((TerminalTreebankNode) inTree).getIndex();
		}
			return getHighestIndexTerm(inTree.getChildren(inTree.getChildren().size()-1));
	}

	public static TopTreebankNode getTopNode(TreebankNode inTree) {
		TreebankNode cur = inTree;
		TopTreebankNode top = null;
		
		while(!(cur instanceof TopTreebankNode)){
			cur = cur.getParent();
		}
		top = (TopTreebankNode) cur;
		return top;
	}


   /**
    * @param jcas     ye olde ...
    * @param parse    opennlp parse
    * @param sentence -
    * @return a top treebank node for the sentence
    * @throws AnalysisEngineProcessException thrown by {@link #recursivelyCreateStructure}
    */
   public static TopTreebankNode buildAlignedTree( final JCas jcas, final Parse parse, final Sentence sentence )
         throws AnalysisEngineProcessException {
      final FSArray terminalArray = TreeUtils.getTerminals( jcas, sentence );
      return buildAlignedTree( jcas, parse, terminalArray, sentence );
   }

   /**
    * @param jcas          ye olde ...
    * @param parse         opennlp parse
    * @param terminalArray [token] terminals in the sentence
    * @param sentence      -
    * @return a top treebank node for the sentence
    * @throws AnalysisEngineProcessException thrown by {@link #recursivelyCreateStructure}
    */
   public static TopTreebankNode buildAlignedTree( final JCas jcas, final Parse parse,
                                                   final FSArray terminalArray, final Sentence sentence )
         throws AnalysisEngineProcessException {
      final StringBuffer parseBuffer = new StringBuffer();
      if ( parse != null ) {
         parse.show( parseBuffer );
      }
      final TopTreebankNode top = new TopTreebankNode( jcas, sentence.getBegin(), sentence.getEnd() );
      top.setTreebankParse( parseBuffer.toString() );
      top.setTerminals( terminalArray );
      top.setParent( null );
      if ( parse != null ) {
         recursivelyCreateStructure( jcas, top, parse, top );
      }
      return top;
   }


   /**
    * @param jcas     ye olde ...
    * @param sentence sentence annotation
    * @return terminals for the sentence
    */
   public static FSArray getTerminals( final JCas jcas, final Sentence sentence ) {
      final List<BaseToken> baseTokens = org.apache.uima.fit.util.JCasUtil
            .selectCovered( jcas, BaseToken.class, sentence );
		return getTerminals( jcas, baseTokens );
	}

	/**
	 * @param jcas       ye olde ...
	 * @param baseTokens base tokens in a window (usually sentence)
	 * @return terminals for the sentence
	 */
	public static FSArray getTerminals( final JCas jcas, final List<BaseToken> baseTokens ) {
		final List<BaseToken> wordList = new ArrayList<>();
		for ( BaseToken baseToken : baseTokens ) {
			if ( !(baseToken instanceof NewlineToken) ) {
				wordList.add( baseToken );
			}
		}
		final FSArray terminals = new FSArray( jcas, wordList.size() );
		int termIndex = 0;
		for ( BaseToken word : wordList ) {
			final TerminalTreebankNode ttn = new TerminalTreebankNode( jcas, word.getBegin(), word.getEnd() );
			ttn.setChildren( null );
			ttn.setIndex( termIndex );
			ttn.setTokenIndex( termIndex );
			ttn.setLeaf( true );
			ttn.setNodeTags( null );
			final String wordText = word.getCoveredText();
			if ( word instanceof PunctuationToken && BRACKET_MAP.containsKey( wordText ) ) {
				ttn.setNodeValue( BRACKET_MAP.get( wordText ) );
			} else {
				ttn.setNodeValue( wordText );
			}
//			ttn.addToIndexes();
			terminals.set( termIndex, ttn );
			termIndex++;
		}
		return terminals;
	}


	public static String getSplitSentence( final FSArray terminalArray ) {
//		int offset = 0;  // what was this for?
      final StringBuilder sb = new StringBuilder();
      for ( int i = 0; i < terminalArray.size(); i++ ) {
         final TerminalTreebankNode ttn = (TerminalTreebankNode)terminalArray.get( i );
         final String word = WHITESPACE_PATTERN.matcher( ttn.getNodeValue() ).replaceAll( "" );
         //			if(i == 0) offset = ttn.getBegin();
         if ( !word.isEmpty() ) {
            sb.append( " " ).append( word );
         }
      }
      // Do we want to trim the first whitespace?
      return sb.toString();
   }
	
	private static void recursivelyCreateStructure(JCas jcas, TreebankNode parent, Parse parse, TopTreebankNode root) throws AnalysisEngineProcessException{
		String[] typeParts;
		if(parse.getType().startsWith("-")){
			// check for dash at the start (for escaped types like -RRB- and so forth that cannot take function tags anyways)
			typeParts = new String[]{parse.getType()};
		}else{
			typeParts = parse.getType().split("-");
		}
		parent.setNodeType(typeParts[0]);
		parent.setNodeValue(null);
		parent.setLeaf(parse.getChildCount() == 0);
		StringArray tags = new StringArray(jcas, typeParts.length-1);
		for(int i = 1; i < typeParts.length; i++){
			tags.set(i-1, typeParts[i]);
		}
		parent.setNodeTags(tags);
		parent.setHeadIndex(parse.getHeadIndex());
		
		Parse[] subtrees = parse.getChildren();
		FSArray children = new FSArray(jcas, subtrees.length);
		
		for(int i = 0; i < subtrees.length; i++){
			Parse subtree = subtrees[i];
			if(subtree.getChildCount() == 1 && subtree.getChildren()[0].getChildCount() == 0){
				// pre-terminal case - now we can set the type (POS tag) and point the parent in the right direction
				TerminalTreebankNode term = root.getTerminals(subtree.getHeadIndex());
				term.setNodeType(subtree.getType());
				children.set(i,term);
				term.setParent(parent);
				term.addToIndexes();
			}else{
				try{
					TreebankNode child = new TreebankNode(jcas);
					child.setParent(parent);
					children.set(i, child);
					recursivelyCreateStructure(jcas, child, subtree, root);
					child.addToIndexes();
				}catch(NullPointerException e){
					System.err.println("MaxentParserWrapper Error: " + e);
					throw new AnalysisEngineProcessException();
				}
			}
		}
		// after we've built up all the children we can fill in the span of the parent.
		parent.setBegin(((TreebankNode)children.get(0)).getBegin());
		parent.setEnd(((TreebankNode)children.get(subtrees.length-1)).getEnd());
		parent.setChildren(children);
//		parent.addToIndexes();
	}

	public static void replaceChild(TreebankNode parent, TreebankNode oldTree,
			TreebankNode newTree) {
		// if parent is null that means we're already at the top -- no pointers to fix.
		if(parent != null){
			for(int i = 0; i < parent.getChildren().size(); i++){
				if(parent.getChildren(i) == oldTree){
					parent.setChildren(i, newTree);
            }
         }
      }
   }

   /**
    * @param sentenceOffset begin offest character index for sentence
    * @param text           text of the sentence
    * @param terminalArray  [token] terminals in the sentence
    * @return open nlp Parse
    */
   public static Parse ctakesTokensToOpennlpTokens( final int sentenceOffset, final String text,
                                                    final FSArray terminalArray ) {
      // based on the first part of parseLine in the opennlp libraries
      final Parse sentenceParse = new Parse( text, new Span( 0, text
            .length() ), AbstractBottomUpParser.INC_NODE, 0, 0 );
      for ( int i = 0; i < terminalArray.size(); i++ ) {
         final TerminalTreebankNode token = (TerminalTreebankNode)terminalArray.get( i );
         final Span span = new Span( token.getBegin() - sentenceOffset, token.getEnd() - sentenceOffset );
         sentenceParse.insert( new Parse( text, span, AbstractBottomUpParser.TOK_NODE, 0, i ) );
      }
      return sentenceParse;
   }

   public static String escapePunct(String in){
     if(BRACKET_MAP.containsKey(in)){
       return BRACKET_MAP.get(in);
     }
     return in;
   }

}

