package org.apache.ctakes.coreference.ae.features.cluster;

import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class MentionClusterAttributeVectorExtractor implements 
  RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation arg1, IdentifiedAnnotation arg2)
      throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    return null;
  }

}
