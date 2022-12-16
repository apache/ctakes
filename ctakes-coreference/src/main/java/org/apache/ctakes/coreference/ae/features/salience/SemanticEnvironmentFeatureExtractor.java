package org.apache.ctakes.coreference.ae.features.salience;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import com.google.common.collect.Sets;

import static org.apache.ctakes.dependency.parser.util.DependencyUtility.*;

public class SemanticEnvironmentFeatureExtractor implements
    FeatureExtractor1<Markable> {

  // this is a subset of the attitude verbs listed in White et al:
  // Discovering classes of attitude verbs using subcategorization frame distributsion
  // NELS 2012.
  private static Set<String> propVerbs = 
      Sets.newHashSet("allow", "believe", "bother", "demand", "deny", "doubt", "expect", "feel", "forbid", "guess", "hate", "hear", "hope", "imagine", "need", "promise", "realize", "remember", "said", "say", "see", "suppose", "tell", "think", "understand", "want", "worry");
  
  public List<Feature> extract(JCas jcas, Markable markable)
      throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jcas, markable);
    if(head == null){
      return feats;
    }
    Sentence sent = DependencyUtility.getSentence(jcas, markable);
    List<ConllDependencyNode> sentNodes = DependencyUtility.getDependencyNodes(jcas, sent);
    List<ConllDependencyNode> covering = DependencyUtility.getProgeny(head, sentNodes);
    
    List<EventMention> events = JCasUtil.selectCovered(jcas, EventMention.class, markable);
    EventMention markableEvent = null;
    for(EventMention event : events){
      ConllDependencyNode eventHead = getNominalHeadNode(jcas, event);
      if(eventHead == head){
        if(markableEvent == null || (event.getEnd()-event.getBegin()) > (markableEvent.getEnd()-markableEvent.getBegin())){
          markableEvent = event;
        }
      }
    }
    
    boolean neg = false;
    if(markableEvent != null){
      neg = markableEvent.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT;
      feats.add(new Feature("SemEnvNegation", neg));
    }
    
    boolean modal = presenceOfModality(head, sentNodes);
    feats.add(new Feature("SemEnvModality", modal));
    
    boolean underPropVerb = presenceOfAttitude(jcas, head);
    feats.add(new Feature("SemEnvAttitude", underPropVerb));
    
    
    // modal * pronoun, neg * pronoun
    if(head.getPostag().startsWith("PRP") || (head.getPostag().equals("DT") && !head.getDeprel().equals("det"))){
      feats.add(new Feature("SemEnvProTrueModal"+modal, true));
      feats.add(new Feature("SemEnvProTrueNeg"+neg, true));
      feats.add(new Feature("SemEnvProTrueAtt"+underPropVerb, true));
    }else{
      feats.add(new Feature("SemEnvProFalseModal"+modal, true));
      feats.add(new Feature("SemEnvProFalseNeg"+neg, true));
      feats.add(new Feature("SemEnvProFalseAtt"+underPropVerb, true));
    }
    
    // modal * Proper noun
    if(head.getPostag().equals("NNP")){
      feats.add(new Feature("SemEnvProperTrueModal"+modal, true));
      feats.add(new Feature("SemEnvProperTrueNeg"+neg, true));
      feats.add(new Feature("SemEnvProperTrueAtt"+underPropVerb, true));
    }else{
      feats.add(new Feature("SemEnvProperFalseModal"+modal,true));
      feats.add(new Feature("SemEnvProperFalseNeg"+neg, true));
      feats.add(new Feature("SemEnvProperFalseAtt"+underPropVerb, true));
    }
    
    boolean indefinite = false;
    for(ConllDependencyNode node : covering){
      if(node.getId() != 0 && (node.getPostag().equals("DT") && 
          (node.getLemma().equals("a") || node.getLemma().equals("an")))){
        indefinite = true;
      }
    }
    feats.add(new Feature("Neg"+neg+"Indef"+indefinite, true));
    
    feats.add(new Feature("Neg"+neg+"Mods"+(covering.size()-1), true));
    
    return feats;
  }

  private static final boolean presenceOfModality(ConllDependencyNode head, List<ConllDependencyNode> sentNodes) {
    boolean modal = false;
    ConllDependencyNode vb = null;
    
    if(head.getHead() != null){
      vb = head.getHead();
      while(vb.getHead() != null && !vb.getPostag().startsWith("VB")){
        vb = vb.getHead();
      }
      
      for(ConllDependencyNode node : sentNodes){
        if(node.getHead() == vb && node.getPostag().equals("MD")){
          modal = true;
          break;
        }
      }
    }
    return modal;
  }

  private static final boolean presenceOfAttitude(JCas jcas, ConllDependencyNode head){
    boolean att = false;
    
    for(ConllDependencyNode cur : getPathToTop(jcas, head)){
      if(propVerbs.contains(cur.getLemma())){
        att = true;
        break;
      }
    }
    
    return att;
  }
}
