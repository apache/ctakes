package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.ae.EventCoreferenceAnnotator;
import org.apache.ctakes.coreference.util.ClusterUtils;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;

//import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator.CollectionTextRelationIdentifiedAnnotationPair;

public class ClusterPairer extends ClusterMentionPairer_ImplBase {
  private int sentDist;
  public ClusterPairer(int dist){
    this.sentDist = dist;
  }
  
  /*
   * getClusterPairs()
   * In this method we allow to link to clusters containing more than one mention even if they
   * are beyond a sentence distance. First we check whether the most recent mention in the cluster
   * is within the specified sentence distance (presumably longer than the sentence distance passed into
   * the method that constrains by distance). The wrinkle is that during training many clusters will have multiple
   * members but only one before the focus mention. So we need to count the members of a cluster until we 
   * get to the most recent one in the cluster. If that value is > 1 then we allow the pairing.
   */
  @Override
  public List<CollectionTextRelationIdentifiedAnnotationPair> getPairs(JCas jcas, Markable mention) {
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
      NonEmptyFSList members = ((NonEmptyFSList)cluster.getMembers());
      Annotation first = (Annotation) members.getHead();
      if(first == null || mention.getBegin() <= first.getEnd()){
        continue;
      }

      IdentifiedAnnotation mostRecent = (IdentifiedAnnotation) ClusterUtils.getMostRecent((NonEmptyFSList)cluster.getMembers(), mention);
      if(mostRecent == null || EventCoreferenceAnnotator.sentDist(jcas, mostRecent, mention) > sentDist){
        continue;
      }
      int numMembers=0;
      for(Markable m : JCasUtil.select(cluster.getMembers(), Markable.class)){
        numMembers++;
        if(m == mostRecent) break;
      }
      if(numMembers > 1){
        pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
      }
    }
    
    return pairs;  }

}
