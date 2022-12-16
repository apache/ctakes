package org.apache.ctakes.coreference.ae.features.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.coreference.util.ClusterUtils;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

public class MentionClusterStackFeaturesExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();

    // This feature didn't work.
//    feats.add(new Feature("ClusterSize_" + size, true));
//    feats.add(new Feature("ClusterSize", size));
    
    NonEmptyFSList members = ((NonEmptyFSList)cluster.getMembers());
    Annotation mostRecent = ClusterUtils.getMostRecent(members, mention);
    if(mostRecent == null){
      return feats;
    }
    int mentionEnd = mostRecent.getEnd();
    int numIntervening = 0;
    int numNonSingletonIntervening = 0;
    
    // this feature is how far down the current cluster is on the stack -- to calculate it
    // we go over all other clusters in the cas, look at the most recent element, and
    // see if it is more recent than the current cluster underconsideration
    for(CollectionTextRelation otherCluster : JCasUtil.select(jCas, CollectionTextRelation.class)){
      if(otherCluster == cluster) continue;

      members = ((NonEmptyFSList)otherCluster.getMembers());
      mostRecent = ClusterUtils.getMostRecent(members, mention);
      if(mostRecent != null && mostRecent.getEnd() > mentionEnd){
        numIntervening++;
        if(ClusterUtils.getSize(members) > 1){
          numNonSingletonIntervening++;
        }
      }
    }
    
//    feats.add(new Feature("ClusterStackPositionInclSingleton"+numIntervening,true));
//    feats.add(new Feature("ClusterStackPosition"+numNonSingletonIntervening,true));
    feats.add(new Feature("ClusterStackPositionInclSingleton", 1 + Math.log10(numIntervening+1)));
    feats.add(new Feature("ClusterStackPosition", 1 + Math.log10(numNonSingletonIntervening+1)));
    return feats;
  }

}
