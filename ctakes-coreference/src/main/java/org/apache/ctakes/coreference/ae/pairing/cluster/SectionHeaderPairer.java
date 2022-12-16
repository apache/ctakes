package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.ae.EventCoreferenceAnnotator;
import org.apache.ctakes.coreference.util.ClusterUtils;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;

//import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator.CollectionTextRelationIdentifiedAnnotationPair;


public class SectionHeaderPairer extends ClusterMentionPairer_ImplBase {

  private int sentDist;

  public SectionHeaderPairer(int dist) {
    this.sentDist = dist;
  }
  
  /*
   * getSectionHeaderPairs()
   * Here we want to add clusters where one of the members is on a line all by itself (a section header)
   * To do this we leverage the annotatino of Paragraphs, roughly the areas between newlines. If such a 
   * span only contains one sentence then we consider it a "header" (or also as important a list item).
   * If it is a header we add it. Here we use sentDist to not bother adding things that will be added by
   * the "sentence distance" method.
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

      // first check if it is sentence distance range -- if so we can ignore because it will be include by other pair generator
      IdentifiedAnnotation mostRecent = (IdentifiedAnnotation) ClusterUtils.getMostRecent(members, mention);
      if(mostRecent == null || EventCoreferenceAnnotator.sentDist(jcas, mostRecent, mention) <= sentDist){
        continue;
      }

      // now check if any of the mentions are in a section header
      List<Paragraph> pars = JCasUtil.selectCovered(jcas, Paragraph.class, 0, mention.getBegin());
      for(int j = 0; j < pars.size(); j++){
        boolean match = false;
        Paragraph par = pars.get(j); // pars.get(pars.size()-j-1);
        List<Sentence> coveredSents = JCasUtil.selectCovered(jcas, Sentence.class, par);
        if(coveredSents != null && coveredSents.size() == 1){
          // this is sentences that are the same span as paragraphs -- how we model section headers
          // see if any of the cluster mentions are in the section header
          for(Markable m : JCasUtil.select(members, Markable.class)){
            if(dominates(par, m)){
              pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
              match = true;
              break;
            }
          }
        }
        if(match) break;
      }
    }
    return pairs;
  }
}
