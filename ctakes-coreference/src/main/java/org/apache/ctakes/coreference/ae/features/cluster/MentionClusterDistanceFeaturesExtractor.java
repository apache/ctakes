package org.apache.ctakes.coreference.ae.features.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class MentionClusterDistanceFeaturesExtractor
    implements RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster, IdentifiedAnnotation mention)
      throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    int minDistance = Integer.MAX_VALUE;
    int neMinDistance = Integer.MAX_VALUE;
    int sentMinDistance = Integer.MAX_VALUE;
    
    for(Markable member : JCasUtil.select(cluster.getMembers(), Markable.class)){
        int dist = JCasUtil.selectBetween(BaseToken.class, member, mention).size();
        minDistance = Math.min(minDistance, dist);
        
        int neDist = JCasUtil.selectBetween(Markable.class, member, mention).size();
        neMinDistance = Math.min(neMinDistance, neDist);
        
        int sentDist = JCasUtil.selectBetween(Sentence.class, member, mention).size();
        sentMinDistance = Math.min(sentMinDistance, sentDist);
    }
    feats.add(new Feature("MinTokenDistance", minDistance / 4000.0));
    feats.add(new Feature("MinMarkableDistance", neMinDistance / 900.0));
    feats.add(new Feature("MinSentDistance", sentMinDistance / 350.0));
    
    return feats;
  }

}
