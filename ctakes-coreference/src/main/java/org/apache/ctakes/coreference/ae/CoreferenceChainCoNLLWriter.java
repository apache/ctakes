package org.apache.ctakes.coreference.ae;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class CoreferenceChainCoNLLWriter {
  private PrintWriter out = null;
  private PrintWriter icOut = null;
  int docNum=0;

  public CoreferenceChainCoNLLWriter(String outputFile) throws FileNotFoundException {
    out = new PrintWriter(outputFile);
  }
  
  public void writeCas(JCas jCas) throws AnalysisEngineProcessException {
    String myView = jCas.getViewName();
    File filename = new File(ViewUriUtil.getURI(jCas));
//    JCas chainsCas = null;
//    try {
//       chainsCas = goldViewName != null ? jCas.getView(goldViewName) : jCas;
//    } catch (CASException e) {
//      e.printStackTrace();
//      throw new AnalysisEngineProcessException(e);
//    }
    int chainNum = 1;
    HashMap<Annotation, Integer> ent2chain = new HashMap<>();
    
//    if(isGold) System.out.println("\nGold chains:");
//    else{
//      icOut.println(String.format("#begin document (%s); part 000", filename.getPath()));
//      System.out.println("\nChains:");
//    }
    
    
    Collection<CollectionTextRelation> rels = JCasUtil.select(jCas, CollectionTextRelation.class);
    if(rels.size() == 0){
      return;
    }
    
    // build a map from every markable that is in a chain to the chain number it is in (number is not important as long as they are
    // distinct so we just number them in the order the uima annotation in gives them to us)
    // This has to be reverse compatible with older coref module that added RelationArguments to a chain instead of Markables.
    // So we grab the chain elements, check their type, then grab the markable annotation depending on that type.
    for(CollectionTextRelation chain : rels){
      FSList members = chain.getMembers();
      // if we are doing cluster-mention coreference, some clusters will be singletons, we do not use those in conll scoring
      if(members instanceof NonEmptyFSList && 
          ((NonEmptyFSList)members).getTail() instanceof EmptyFSList) continue;
      
      while(members instanceof NonEmptyFSList){
        TOP head = ((NonEmptyFSList) members).getHead();
        Annotation mention = null;
        if(head instanceof Annotation){
          mention = (Annotation) head;
        }else{
          mention = ((RelationArgument)head).getArgument();
        }
//        Annotation mention = (Annotation) ((NonEmptyFSList) members).getHead();
        ent2chain.put(mention, chainNum);
        members = ((NonEmptyFSList)members).getTail();
        System.out.print("Mention: " + mention.getCoveredText().replace("\n", "<CR>"));
        System.out.print(" (" + mention.getBegin() + ", " + mention.getEnd() + ")");
        if(!mention.getView().getViewName().equals(myView)){
          System.out.print("[DOC:" + mention.getView().getViewName() + "]");
        }
        System.out.print("  ----->    ");
      }
      System.out.println();
      chainNum++;
    }
    
    // Here we are using newline tokens to delimit sentences because the sentence
    // breaks that cTAKES creates may not be correct and some gold markables might
    // wrap sentences which might be confusing to the consumer of this file.
    out.println("#begin document (" + filename.getPath() + "); part 000");
    List<BaseToken> tokens = new ArrayList<>(JCasUtil.select(jCas, BaseToken.class));
    Multiset<Integer> endSet = HashMultiset.create();
    int tokenId = 0;
    int sentId = 0;
    BaseToken nextToken = tokens.get(0);

    for(int i = 0; i < tokens.size(); i++){
      boolean endSentToken = false;
      BaseToken token = nextToken;
      if(i+1 < tokens.size()){
        nextToken = tokens.get(i+1);
        if(nextToken instanceof NewlineToken || (token.getCoveredText().equals(".") && !(endSet.size() > 0))){
          endSentToken = true;
        }
      }

      // if we see a newline token at the end of a sentence break the sentence
      // only print out if we are not at the start of the sentence:
      if(token instanceof NewlineToken){
        continue;
      }
      
      int lastInd = token.getEnd();
      // fix for some bad tokenization
      if(token.getCoveredText().length() > 1 && token.getCoveredText().endsWith(".")){
        lastInd = token.getEnd()-1;
      }
      List<Markable> markables = new ArrayList<>(JCasUtil.selectCovering(jCas, Markable.class, token.getBegin(), lastInd));
      List<Annotation> startMention = new ArrayList<>();
      Multiset<Integer> endMention = HashMultiset.create();
      List<Integer> wholeMention = new ArrayList<>();
      
      for(Annotation markable : markables){
        if(ent2chain.containsKey(markable)){
          if(markable.getBegin() == token.getBegin()){
            if(markable.getEnd() == token.getEnd()){
              wholeMention.add(ent2chain.get(markable));
            }else{
              startMention.add(markable);
            }
          }else if(markable.getEnd() <= token.getEnd()){
            if(endMention.contains(ent2chain.get(markable))){
              System.err.println("There is a duplicate element -- should be handled by multiset");
            }
            if(markable.getEnd() < token.getEnd()){
              System.err.println("There is a markable that ends in the middle of a token!");
            }
            endMention.add(ent2chain.get(markable));
          }
          
//          if(!isGold){
//            icOut.println(String.format("%d-%d-%d\n", sentId, markable.getBegin(), markable.getEnd()));
//          }
        }
      }

      
      out.print(filename.getPath());
      out.print('\t');
      out.print(docNum);
      out.print('\t');
      out.print(tokenId++);
      out.print('\t');
      out.print(token instanceof NewlineToken ? "Newline" : TreeUtils.escapePunct(token.getCoveredText()));
      out.print('\t');
      out.print(token.getPartOfSpeech());
      out.print('\t');
      // parse bit -- assume flat parse
      if(tokenId == 1){
        out.print("(NOPARSE*");
        // special case for one word sentences:
        if(endSentToken){
          out.print(")");
        }
      }else if(endSentToken){
        out.print("*)");
      }else{
        out.print("*");
      }      
      out.print('\t');
      // predicate lemma -- can ignore?
      out.print('-'); out.print('\t');
      // predicate frameset id -- can ignore?
      out.print('-'); out.print('\t');
      // word sense 
      out.print('-'); out.print('\t');
      // speaker/author
      out.print('-'); out.print('\t');
      // named entities
      out.print('*'); out.print('\t');
      
      StringBuffer buff = new StringBuffer();
//      while(endStack.size() > 0 && endMention.contains(endStack.peek())){
      for(int ind : endMention){
//        int ind = endStack.pop();
//        int ind = endMention.get(j);
        if(endSet.contains(ind)){
          buff.append(ind);
          buff.append(')');
          buff.append('|');
        }
        endSet.remove(ind);
//        endMention.remove(ind);
      }
      for(int ind : wholeMention){
        buff.append('(');
        buff.append(ind);
        buff.append(')');
        buff.append('|');
      }
      // sort start mentions by ordering of ending
      while(startMention.size() > 0){
        int ind;
        Annotation latestEnd = null;
        for(int j = 0; j < startMention.size(); j++){
          if(latestEnd == null || startMention.get(j).getEnd() > latestEnd.getEnd()){
            latestEnd = startMention.get(j);
          }
        }
        startMention.remove(latestEnd);
        ind = ent2chain.get(latestEnd);
        buff.append('(');
        buff.append(ind);
        buff.append('|');
        endSet.add(ind);
//        endStack.push(ind);
      }

      // In some datasets markables end in the middle of a token -- this is a problem because our check above is for all markables that cover the
      // current token. In this case the markable end will still be unused when we get to the end of the sentence. We'll just hack it by throwing
      // them on the last token of the sentence.
      if(endSentToken && endSet.size() > 0){
        System.err.println("Error! There are opened markables that never closed! Putting them on the end of the sentence.");
        for(int ind : endSet){
          buff.append(ind);
          buff.append(')');
          buff.append('|');
        }
        endSet.clear();
      }
      if(buff.length() > 0){
        out.println(buff.substring(0,  buff.length()-1));
      }else{
        out.println("_");
      }
     
      if(endSentToken){
        out.println();
        tokenId = 0;
        sentId++;
      }
    }
//    if(!isGold){
//      icOut.println("#end document");
//    }
    out.println("#end document " + filename.getPath());
    out.flush();
    docNum++;
  }
}
