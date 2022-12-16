package org.apache.ctakes.coreference.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class TemporalFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    String a1dtr = getDocTimeRelForArg(jCas, arg1);
    String a2dtr = getDocTimeRelForArg(jCas, arg2);

    feats.add(new Feature("Arg1DTR_" + a1dtr, true));
    feats.add(new Feature("Arg2DTR_" + a2dtr, true));
    
    if(a1dtr.equals(a2dtr)){
      if(!a1dtr.equals("NA")){
        feats.add(new Feature("DTR_Match", true));
      }
    }
    
    return feats;
  }

  private static String getDocTimeRelForArg(JCas jCas, IdentifiedAnnotation arg){
    String dtr = "NA";
    
    // find EventMentions and grab their event properties
    ConllDependencyNode node = DependencyUtility.getNominalHeadNode(jCas, arg);
    if(node != null){
      List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, node);
      for(EventMention event : events){
        if(event.getClass().getSimpleName().equals("EventMention")){
          if(event.getEvent() != null && event.getEvent().getProperties() != null && event.getEvent().getProperties().getDocTimeRel() != null){
            dtr = event.getEvent().getProperties().getDocTimeRel();
          }
        }
      }
    }
    return dtr;
  }
}