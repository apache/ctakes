package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;
import static org.apache.ctakes.dependency.parser.util.DependencyUtility.*;

@PipeBitInfo(
      name = "Markable Annotator (Deterministic)",
      description = "Annotates Markables for use by Coreference Annotators.",
      dependencies = { SECTION, SENTENCE,
            IDENTIFIED_ANNOTATION, DEPENDENCY_NODE,
            TREE_NODE, TIMEX },
      products = { MARKABLE }
)
public class DeterministicMarkableAnnotator extends JCasAnnotator_ImplBase {

  // list starters like A.  or #1    or 3)
  static Pattern headerPatt = Pattern.compile("^(([A-Z][\\.\\:\\)])|(#\\d+)|(\\d+[\\.\\:\\)])) *");

  @Override
  public void initialize(UimaContext uc) throws ResourceInitializationException{
    super.initialize(uc);
  }

  @Override
  public void process(JCas jCas)
      throws AnalysisEngineProcessException {
    
//    createMarkablesUsingConstituencyTrees(jCas);
    createMarkablesUsingDependencyTrees(jCas);
    
    for(TimeMention timex : JCasUtil.select(jCas, TimeMention.class)){
      boolean collision = false;
      for(Markable other : JCasUtil.selectCovered(jCas, Markable.class, timex.getBegin(), timex.getEnd())){
        if(other.getBegin() == timex.getBegin() && other.getEnd() == timex.getEnd()){
          collision = true;
          break;
        }
      }
      if(!collision){
        Markable m = new Markable(jCas, timex.getBegin(), timex.getEnd());
        m.addToIndexes(jCas);
      }
    }
  }

  private static void createMarkablesUsingDependencyTrees(JCas jCas) {
    for(Segment seg : JCasUtil.select(jCas, Segment.class)){
      for(ConllDependencyNode node : JCasUtil.selectCovered(jCas, ConllDependencyNode.class, seg)){
        String nodeText = node.getCoveredText().toLowerCase();
        List<TerminalTreebankNode> terms = JCasUtil.selectCovered(TerminalTreebankNode.class, node);
        TerminalTreebankNode term = null;
        if(terms.size() > 0){
          term = terms.get(0);
        }
        
        if(node.getId() == 0){
          continue;
        }
        if(nodeText.matches("\\p{Punct}+")){
          continue;
        }
        // 1) get nouns, and expand the markable to the phrase they cover
        // 2) get determiners like "this" and "these"
        // 3) non-passive "it"
        if(node.getPostag().startsWith("NN") && term != null && term.getNodeType().startsWith("N")){
          Markable markable = expandNodeToMarkable(jCas, node);
          if(markable == null) continue;
          markable.addToIndexes();
        }else if(node.getPostag().equals("DT") && !node.getDeprel().equals("det")){
          Markable markable = new Markable(jCas, node.getBegin(), node.getEnd());
          markable.addToIndexes();
        }else if(node.getCoveredText().toLowerCase().equals("it") && node.getDeprel().contains("bj")){
          // contains "bj" includes nsubj, all the obj's, and all the *bjpass*'s.
          Markable markable = new Markable(jCas, node.getBegin(), node.getEnd());
          markable.addToIndexes();
        }
      }
    }
  }

  // Post-process to remove those kinds of nodes which may or may not be correctly parsed but do not tend to align with gold annotated
  // markables (and usually our intuitions as well, so it's not completely hacky).
  private static List<ConllDependencyNode> removeUnannotatedNodes(ConllDependencyNode originalNode,
      List<ConllDependencyNode> progeny) {
    List<ConllDependencyNode> filtered = new ArrayList<>();
    
    for(ConllDependencyNode node: progeny){
      if(node == originalNode) filtered.add(node);
      
      boolean blockedByConj = false;
      for(ConllDependencyNode pathEl : DependencyUtility.getPath(progeny, node, originalNode)){
        if(pathEl == originalNode) continue;
        if(pathEl.getDeprel().equals("conj") || pathEl.getDeprel().equals("cc") || pathEl.getPostag().equals(".") || pathEl.getPostag().equals(",") || pathEl.getDeprel().equals("punct") || pathEl.getDeprel().equals("meta") 
            || pathEl.getCoveredText().matches("(([A-Z][\\.\\:\\)])|(#\\d+)|(\\d+[\\.\\:\\)]))")){
          blockedByConj = true;
          break;
        }
      }
      if(!blockedByConj){
        filtered.add(node);
      }
    }
    
    return filtered;
  }

  public static Markable expandEventToMarkable(JCas jCas, EventMention event){
    ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jCas, event);
    if(head == null) return null;
    return expandNodeToMarkable(jCas, head);
  }

  public static Markable expandNodeToMarkable(JCas jCas, ConllDependencyNode node){
    Markable markable = null;
    String nodeText = node.getCoveredText();

    if(node.getForm().matches("\\s+")) return null;
    // TODO fix this godawful hack:
    if(nodeText.equals("date") || nodeText.equals("tablet") || nodeText.equals("hg") || nodeText.equals("lb") || nodeText.equals("status")
            || nodeText.equals("capsule") || nodeText.equals("mg") || nodeText.equals("cm")){

      return null;
    }
    int begin = node.getBegin();
    int end = node.getEnd();
//          if(node.getHead().getId() != 0){
    List<ConllDependencyNode> progeny = getProgeny(node, getDependencyNodes(jCas, getSentence(jCas, node)));
    progeny = removeUnannotatedNodes(node, progeny);
    if(progeny.size() > 0){
      for(ConllDependencyNode child : progeny){
        if(child.getBegin() < begin){
          begin = child.getBegin();
        }
        if(child.getEnd() > end){
          end = child.getEnd();
        }
      }
    }
//          }
    ConllDependencyNode parent = node.getHead();
    if(parent != null && parent.getId() != 0){
      // if parent is inside the bounds of the proposed markable prune it a bit.
      if(parent.getBegin() < node.getBegin() && parent.getBegin() > begin){
        // get the following token:
        BaseToken nextToken = JCasUtil.selectFollowing(BaseToken.class, parent, 1).get(0);
        begin = nextToken.getBegin();
      }
      // parent is after the current head node but before the proposed markable is meant to end:
      if(parent.getEnd() >  node.getEnd() && parent.getEnd() < end){
        BaseToken prevToken = JCasUtil.selectPreceding(BaseToken.class, parent, 1).get(0);
        end = prevToken.getEnd();
      }
    }

    Matcher m = headerPatt.matcher(nodeText);
    if(m.find()){
      begin = begin + m.end();
    }

    markable = new Markable(jCas, begin, end);
    return markable;
  }

  @SuppressWarnings("unused")
private static void createMarkablesUsingConstituencyTrees(JCas jCas) {
    // personal pronouns:
//  for(WordToken token : JCasUtil.select(jCas, WordToken.class)){
//    if(token.getPartOfSpeech().startsWith("PRP") ||
//        token.getCoveredText().equalsIgnoreCase("here")){
//      Markable markable = new Markable(jCas, token.getBegin(), token.getEnd());
//      markable.addToIndexes();
//    }
//  }

  // NPs:
    for(TreebankNode tree : JCasUtil.select(jCas, TreebankNode.class)){
      if(tree.getNodeType().equals("NP")){
        String nodeText = tree.getCoveredText();
        // cases to skip: 1) already included by pos tag above
        // 2) existential "there"
        // 3) proper names 
        // 4) numbers
        if(tree.getChildren().size() == 1){
          if(tree.getChildren(0).getNodeType().equals("PRP") ||
              tree.getChildren(0).getNodeType().equals("EX") ||
              tree.getChildren(0).getNodeType().equals("CD")) {
            continue;
          }
        }
        Markable markable = null;
        Matcher m = headerPatt.matcher(nodeText);
        int start = tree.getBegin();
        int end = tree.getEnd();
        if(m.find()){
          start = start + m.end();
        }
        if((nodeText.endsWith(".") || nodeText.endsWith(":")) && end-1 > start){
          end = end-1;
          //            System.err.println("Adjusting end with pair: (" + start + ", " + end + ")");
        }

        markable = new Markable(jCas, start, end);
        markable.addToIndexes();

        // N* modifiers of NPs: (
        for(int i = 0; i < tree.getChildren().size()-1; i++){
          TreebankNode child = tree.getChildren(i);
          if(child instanceof TerminalTreebankNode && child.getNodeType().startsWith("N") && !child.getNodeType().equals("NNP")){
            markable = new Markable(jCas, child.getBegin(), child.getEnd());
            markable.addToIndexes();
          }
        }
      }
    }
  }
}
