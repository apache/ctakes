package org.apache.ctakes.coreference.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class SectionFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  public List<Feature> extract(JCas jcas, IdentifiedAnnotation ante,
      IdentifiedAnnotation ana) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    boolean anteInHeader = false;
    boolean anaInHeader = false;
    int antePar = -1;
    int anaPar = -1;
    
    // Find section headers -- paragraphs 
    List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
    for(int i = 0; i < pars.size(); i++){
      Paragraph par = pars.get(i);
      if(par.getBegin() > ana.getEnd()){
        break;
      }
      if(ante.getBegin() >= par.getBegin() && ante.getEnd() <= par.getEnd()){
        antePar = i;
      }
      if(ana.getBegin() >= par.getBegin() && ana.getEnd() <= par.getEnd()){
        anaPar = i;
      }
      List<Sentence> coveredSents = JCasUtil.selectCovered(jcas, Sentence.class, par);
      if(coveredSents != null && coveredSents.size() == 1){
        if(antePar == i){
          anteInHeader = true;
        }
        if(anaPar == i){
          anaInHeader = true;
        }
      }
    }

    feats.add(new Feature("AnteInHeader", anteInHeader));
    feats.add(new Feature("AnaInHeader", anaInHeader));
    if(anteInHeader && antePar+1 == anaPar){
      feats.add(new Feature("AnteHeaderHeadsAna", true));      
    }
    return feats;
  }
}
