package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator;
import org.apache.ctakes.coreference.util.ClusterMentionFetcher;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.*;

import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;

//import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator.CollectionTextRelationIdentifiedAnnotationPair;

public class HeadwordPairer extends ClusterMentionPairer_ImplBase {
  private Map<String, Set<Markable>> headWordMarkables = null;
  
  @Override
  public void reset(JCas jcas){
    super.reset(jcas);
    headWordMarkables = new HashMap<>();
  }
  
  @Override
  public List<CollectionTextRelationIdentifiedAnnotationPair> getPairs(JCas jcas, Markable mention) {
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();

    ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, mention);
    if(headNode == null){
      Logger.getLogger(MentionClusterCoreferenceAnnotator.class).warn("There is a markable with no dependency node covering it.");
      return pairs;
    }
    String head = headNode.getCoveredText().toLowerCase();
    if(headWordMarkables.containsKey(head)){
       final Set<Markable> headSet = headWordMarkables.get( head );
       ClusterMentionFetcher.populatePairs( jcas, mention, headSet, pairs );
    } else {
      headWordMarkables.put(head, new HashSet<Markable>());
    }
    headWordMarkables.get(head).add(mention);
    
    return pairs;  
  }
}
