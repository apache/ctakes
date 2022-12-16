package org.apache.ctakes.coreference.ae.features.cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.core.util.ListIterable;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class MentionClusterSectionFeaturesExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation>, FeatureExtractor1<Markable> {

  @Override
  public List<Feature> extract(JCas jcas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    Set<Integer> parsWithAnteHeader = new HashSet<>();
    
    boolean anteInHeader = false;
    int anaPar = -1;
    
    // Find section headers -- paragraphs 
    // FIXME - should be paragraphs that only cover one sentence
    List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
    for(int i = 0; i < pars.size(); i++){
      Paragraph par = pars.get(i);
      // find the paragraph with the anaphor
      if(mention.getBegin() >= par.getBegin() && mention.getEnd() <= par.getEnd()){
        anaPar = i;
      }

      if(par.getBegin() > mention.getEnd()){
        break;
      }
      
      List<Sentence> coveredSents = JCasUtil.selectCovered(Sentence.class, par);
      if(coveredSents == null || coveredSents.size() == 0 || coveredSents.size() > 1) continue;
      
      // if we get this far then we are in a paragraph comprised of a single sentence 
      for(Markable member : new ListIterable<Markable>(cluster.getMembers())){
        if(member.getBegin() >= par.getBegin() && member.getEnd() <= par.getEnd()){
          parsWithAnteHeader.add(i);
          anteInHeader = true;
          break;
        }
      }
      
    }

    feats.add(new Feature("AnteInHeader", parsWithAnteHeader.size() > 0));
    if(anteInHeader && parsWithAnteHeader.contains(anaPar-1)){
      feats.add(new Feature("AnteHeaderHeadsAna", true));      
    }

    return feats;
  }

  @Override
  public List<Feature> extract(JCas jcas, Markable mention) throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    boolean anaInHeader = false;
    int anaPar = -1;

    // Find section headers -- paragraphs 
    List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
    for(int i = 0; i < pars.size(); i++){
      Paragraph par = pars.get(i);
      if(par.getBegin() > mention.getEnd()){
        break;
      }
      // find the paragraph with the anaphor
      if(mention.getBegin() >= par.getBegin() && mention.getEnd() <= par.getEnd()){
        anaPar = i;
      }
      List<Sentence> coveredSents = JCasUtil.selectCovered(jcas, Sentence.class, par);
      if(coveredSents != null && coveredSents.size() == 1){
        if(anaPar == i){
          anaInHeader = true;
          break;
        }
      }
    }
    feats.add(new Feature("AnaInHeader", anaInHeader));

    return feats;
  }

}
