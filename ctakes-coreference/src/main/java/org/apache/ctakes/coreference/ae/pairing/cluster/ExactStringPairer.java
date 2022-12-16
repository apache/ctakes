package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.util.ClusterUtils;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;

//import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator.CollectionTextRelationIdentifiedAnnotationPair;

public class ExactStringPairer extends ClusterMentionPairer_ImplBase {

  private Set<String> markableStrings = null;
  
  @Override
  public void reset(JCas jcas){
    markableStrings = new HashSet<>();
  }
  /*
   * getExactStringMatchPairs()
   * For mentions that have the exact string repeated elsewhere in the document we want to
   * allow matching across any distance. We don't use the sentence distance parameter here.
   * We make use of a global variable markableStrings that is a HashSet containig all the markable
   * strings from this document.
   */
  @Override
  public List<CollectionTextRelationIdentifiedAnnotationPair> getPairs(JCas jcas, Markable mention) {
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    
    if(markableStrings.contains(mention.getCoveredText().toLowerCase())){
      for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
        Annotation mostRecent = ClusterUtils.getMostRecent((NonEmptyFSList)cluster.getMembers(), mention);
        if(mostRecent == null) continue;

        for(Markable m : JCasUtil.select(cluster.getMembers(), Markable.class)){
          if(m == mostRecent) break;
          // see if any of the members of the cluster have the exact same string as this 
          if(m.getCoveredText().toLowerCase().equals(mention.getCoveredText().toLowerCase())){
            pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
            break;
          }
        }
      }
    }
    markableStrings.add(mention.getCoveredText().toLowerCase());
    return pairs;
  }
}
