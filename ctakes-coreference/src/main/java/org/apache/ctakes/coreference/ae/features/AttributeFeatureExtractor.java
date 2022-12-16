package org.apache.ctakes.coreference.ae.features;

import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isGeneric;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isHistory;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isNegated;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isPatient;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isUncertain;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

public class AttributeFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation, IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation ante, IdentifiedAnnotation ana)
      throws AnalysisEngineProcessException {
    List<Feature> features = new ArrayList<>();
    
    boolean anaNegated = isNegated(ana);
    features.add(new Feature("MC_ana_NEGATED", anaNegated));
    boolean anaUncertain = isUncertain(ana);
    features.add(new Feature("MC_ana_UNCERTAIN", anaUncertain));
    boolean anaGen = isGeneric(ana);
    features.add(new Feature("MC_ana_GENERIC", anaGen));
    boolean anaSubj = isPatient(ana);
    features.add(new Feature("MC_ana_PATIENT", anaSubj));
    boolean anaHist = isHistory(ana);
    features.add(new Feature("MC_ana_HISTORY", anaHist));
    boolean anaTimex = isTimex(ana);
    features.add(new Feature("MC_ana_TIMEX", anaTimex));
    
    boolean anteNegated = isNegated(ante);
    features.add(new Feature("MC_ante_NEGATED", anteNegated));
    boolean anteUncertain = isUncertain(ante);
    features.add(new Feature("MC_ante_UNCERTAIN", anteUncertain));
    boolean anteGen = isGeneric(ante);
    features.add(new Feature("MC_ante_GENERIC", anteGen));
    boolean anteSubj = isPatient(ante);
    features.add(new Feature("MC_ante_PATIENT", anteSubj));
    boolean anteHist = isHistory(ante);
    features.add(new Feature("MC_ante_HISTORY", anteHist));
    boolean anteTimex = isTimex(ante);
    features.add(new Feature("MC_ante_TIMEX", anteTimex));
    
    features.add(new Feature("MC_AGREE_NEG", anteNegated == anaNegated));
    features.add(new Feature("MC_AGREE_UNC", anteUncertain == anaUncertain));    
    features.add(new Feature("MC_AGREE_TIMEX", anteTimex == anaTimex));

    return features;
  }
  
  private static boolean isTimex(Annotation a){
    return JCasUtil.selectCovered(TimeMention.class, a).size() > 0;
  }

}
