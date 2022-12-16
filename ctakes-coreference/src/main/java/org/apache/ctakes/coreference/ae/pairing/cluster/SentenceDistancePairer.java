package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.ae.EventCoreferenceAnnotator;
import org.apache.ctakes.coreference.util.ClusterUtils;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;

//import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator.CollectionTextRelationIdentifiedAnnotationPair;

public class SentenceDistancePairer extends ClusterMentionPairer_ImplBase {

  private int sentDistance;
  
  public SentenceDistancePairer(int distance){
    this.sentDistance = distance;
  }
  /*
   * Here we want to add only things that are nearby. First we check the semantic types
   * of the cluster we're comparing against. If any member is an Anatomical Site or Medication,
   * we add the cluster no matter what. Otherwise we check how many sentences are in between
   * the mention and the latest element of the cluster.
   */
  @Override
  public List<CollectionTextRelationIdentifiedAnnotationPair> getPairs( JCas jcas, Markable mention ) {
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    Set<String> bestAnaTypes = getBestEnt(jcas, (Markable) mention);
    
    for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
      NonEmptyFSList members = ((NonEmptyFSList)cluster.getMembers());
      Annotation first = (Annotation) members.getHead();
      if(first == null || mention.getBegin() <= first.getEnd()) continue;
      
      // check for distance if they are not anatomical site or medication
      if(!(bestAnaTypes.contains(AnatomicalSiteMention.class.getSimpleName()) ||
          bestAnaTypes.contains(MedicationEventMention.class.getSimpleName()))){
  
        IdentifiedAnnotation mostRecent = (IdentifiedAnnotation) ClusterUtils.getMostRecent(members, mention);
        if(mostRecent == null || EventCoreferenceAnnotator.sentDist(jcas, mostRecent, mention) > this.sentDistance) continue;
      }
  
      // check for types of cluster
      Set<String> bestClusterTypes = getBestEnt(jcas, cluster);
      if(bestAnaTypes.size() > 0 && bestClusterTypes.size() > 0){
        boolean overlap = false;
        for(String semType : bestAnaTypes){
          if(bestClusterTypes.contains(semType)){
            overlap = true;
          }
        }
        // they both correspond to named entities but no overlap in which category of named entity.
        if(!overlap){
          continue;
        }
      }
      pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));      
    }
    return pairs;
  }

}
