package org.apache.ctakes.coreference.ae.features.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.util.ListIterable;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class MentionClusterSalienceFeaturesExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation>, FeatureExtractor1<Markable> {

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    double maxSalience = 0.0;
    for(Markable member : new ListIterable<Markable>(cluster.getMembers())){
      if(mention.getBegin() < member.getEnd()){
        // during training this might happen -- see a member of a cluster that
        // is actually subsequent to the candidate mention
        break;
      }
      if(member.getConfidence() > maxSalience){
        maxSalience = member.getConfidence();
      }
    }
    
    feats.add(new Feature("MC_MAX_SALIENCE", maxSalience));
    return feats;
  }

  @Override
  public List<Feature> extract(JCas jCas, Markable mention) throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    feats.add(new Feature("MC_MENTION_SALIENCE", mention.getConfidence()));

    return feats;
  }

}
