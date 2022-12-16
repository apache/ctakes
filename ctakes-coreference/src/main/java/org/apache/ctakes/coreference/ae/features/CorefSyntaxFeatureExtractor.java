package org.apache.ctakes.coreference.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class CorefSyntaxFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    ConllDependencyNode head1 = DependencyUtility.getNominalHeadNode(jCas, arg1);
    ConllDependencyNode head2 = DependencyUtility.getNominalHeadNode(jCas, arg2);
    
    if(head1 != null){
      feats.add(new Feature("Arg1Head", head1.getCoveredText().toLowerCase()));
    }
    if(head2 != null){
      feats.add(new Feature("Arg2Head", head2.getCoveredText().toLowerCase()));
    }
    return feats;
  }

}
