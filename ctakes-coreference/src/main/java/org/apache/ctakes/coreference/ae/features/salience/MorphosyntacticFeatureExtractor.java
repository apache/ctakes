package org.apache.ctakes.coreference.ae.features.salience;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

/*
 * Citations:
 * Recasens, de Marneffe, Potts: The Life and Death of Discourse Entities: Identifying Singleton Mentions
 * NAACL-HLT 2013 short paper, 627-633.
 * 
 * BBN corpus description (for the 18 named entity types)
 * https://catalog.ldc.upenn.edu/docs/LDC2013T19/OntoNotes-Release-5.0.pdf
 * 
 * This feature extractor is intended to implement the features in Table 2, described
 * in the subsection of 3 called "Internal morphosyntax of the mention."
 * Left off the table are the 18 NE types from CoNLL. Most of these are not relevant
 * to our task, especially since we resolve person mentions with simple rules.
 */
public class MorphosyntacticFeatureExtractor implements FeatureExtractor1<Markable> {

  public List<Feature> extract(JCas jcas, Markable markable)
      throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jcas, markable);
    if(head == null){
      return feats;
    }
    List<ConllDependencyNode> covering = DependencyUtility.getProgeny(head, DependencyUtility.getDependencyNodes(jcas, DependencyUtility.getSentence(jcas, markable)));

    if(head.getId() != 0 && (head.getPostag().startsWith("PRP") || 
        (head.getPostag().equals("DT") && !head.getDeprel().equals("det")))){
      // 2 conditions -- head is a pronoun POS tag (he, she, it) like PRP or PRP$
      // or head is a determiner (This, that) that does not have a determiner dependency relation
      // -- usually marked as nsubj or dobj when used as pronoun (This was..., discussed this with...) 
      // but would be "det" when used as in "this discussion"
      feats.add(new Feature("MorphoIsPronoun", true));
    }else{
      feats.add(new Feature("MorphoIsPronoun", false));
    }
    
    feats.add(new Feature("MorphoIsProper", (head != null && head.getPostag() != null && head.getPostag().equals("NNP"))));
    
    // skip animacy and person features for now -- planning to not do person mentions
    
    // replace singular/other with plural/other
    feats.add(new Feature("MorphoPlural", head.getPostag().equals("NNS")));
    
    boolean indefinite = false;
    boolean containsNum = false;
    for(ConllDependencyNode node : covering){
      if(node.getPostag().equals("DT") && 
          (node.getLemma().equals("a") || node.getLemma().equals("an"))){
        indefinite = true;
      }
      
      if(node.getPostag().equals("CD")){
        containsNum = true;
      }
    }
    
    feats.add(new Feature("MorphoIndefinite", indefinite));
    feats.add(new Feature("MorphoNumeric", containsNum)); // lump together many NE types from OntoNotes (date, time, ordinal, percent, quantity)
    feats.add(new Feature("MorphoNumModifiers", covering.size()-1));    
    
    return feats;
  }

  
}
