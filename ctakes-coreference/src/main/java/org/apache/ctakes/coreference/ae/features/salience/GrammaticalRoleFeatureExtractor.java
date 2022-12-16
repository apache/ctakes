package org.apache.ctakes.coreference.ae.features.salience;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

/*
 * Citations:
 * Recasens, de Marneffe, Potts: The Life and Death of Discourse Entities: Identifying Singleton Mentions
 * NAACL-HLT 2013 short paper, 627-633.
 * 
 * This class implements features in Table 3. Since there is highly ambiguous descriptions
 * of the features (e.g., Sentence Position=End as well as Sentence Position=Last, 
 * I looked at the source code for the system to determine precisely how the features
 * were defined.
 * First, last means literally first or last token in sentence.
 * Begin, middle, and end mean which third of the sentence is it in.
 */
public class GrammaticalRoleFeatureExtractor implements FeatureExtractor1<Markable> {

  public List<Feature> extract(JCas jcas, Markable markable)
      throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jcas, markable);
    if(head == null){
      return feats;
    }
    Sentence sent = DependencyUtility.getSentence(jcas, markable);
    List<ConllDependencyNode> sentNodes = DependencyUtility.getDependencyNodes(jcas, sent);
//    List<ConllDependencyNode> covering = DependencyUtility.getProgeny(head, sentNodes);
    int numNodes = sentNodes.size()-1; // remove root whole sentence node
    
    feats.add(new Feature("GrammaticalRoleSentencePositionFirst", head.getId() == 1));
    feats.add(new Feature("GrammaticalRoleSentencePositionLast", head.getId() == numNodes));
    if(head.getId() < (numNodes / 3)){
      feats.add(new Feature("GrammaticalRoleSentencePositionBegin", true));
    }else if(head.getId() > 2*(numNodes/3)){
      feats.add(new Feature("GrammaticalRoleSentencePositionEnd", true));
    }else{
      feats.add(new Feature("GrammaticalRoleSentencePositionMiddle", true));
    }
    
    String deprel = head.getDeprel();
    if(deprel.equals("nsubj")){
      feats.add(new Feature("GrammaticalRoleRelSubj", true));
    }else if(deprel.equals("dobj") || deprel.equals("iobj")){
      feats.add(new Feature("GrammaticalRoleRelVerbArg", true));
    }else if(deprel.equals("nn")){
      feats.add(new Feature("GrammaticalRoleRelNounArg", true));
    }else if(deprel.equals("root")){
      feats.add(new Feature("GrammaticalRoleRelRoot", true));
    }else if(deprel.equals("conj")){
      feats.add(new Feature("GrammaticalRoleRelConj", true));
    }else{
      feats.add(new Feature("GrammaticalRoleRelOther", true));
    }
    
    return feats;
  }

}
