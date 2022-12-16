package org.apache.ctakes.coreference.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class SalienceFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation, IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation ante, IdentifiedAnnotation ana)
      throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    feats.add(new Feature("MP_ANTE_SALIENCE", ante.getConfidence()));
    feats.add(new Feature("MP_ANA_SALIENCE", ana.getConfidence()));
    return feats;
  }

}
