package org.apache.ctakes.coreference.ae.features.salience;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class ClinicalFeatureExtractor implements FeatureExtractor1<Markable> {

  @Override
  public List<Feature> extract(JCas jcas, Markable markable){    
    List<Feature> feats = new ArrayList<>();
    
    List<Paragraph> coveringPars = JCasUtil.selectCovering(jcas, Paragraph.class, markable);
    List<Sentence> coveringSents = JCasUtil.selectCovering(jcas, Sentence.class, markable);
    Sentence coveringSent = DependencyUtility.getSentence(jcas, markable);

    if(coveringPars.size() == 1 && coveringSents.size() == 1){
      List<Sentence> parSents = JCasUtil.selectCovered(Sentence.class, coveringPars.get(0));
      if(parSents.size() == 1){
        // covering paragraph for this markable is exactly one sentence long -- 
        // AKA it is a header
        feats.add(new Feature("ClinIsHeader", true));
      }else{
        int sentPos = 0;
        for(int i = 0; i < parSents.size(); i++){
          if(parSents.get(i) == coveringSent){
            sentPos = i;
            break;
          }
        }
        if(sentPos < parSents.size() / 3){
          feats.add(new Feature("ClinSentPosBegin", true));
        }else if(sentPos > (2*parSents.size() / 3)){
          feats.add(new Feature("ClinSentPosEnd", true));
        }else{
          feats.add(new Feature("ClinSentPosMiddle", true));
        }
      }
    }
    
    
    
    List<EventMention> events = JCasUtil.selectCovered(EventMention.class, markable);
    EventMention longestEvent = null;
    for(EventMention event : events){
      if(event.getTypeID() > 0){
        if(longestEvent == null || (event.getEnd()-event.getBegin()) > (longestEvent.getEnd()-longestEvent.getBegin())){
          longestEvent = event;
        }
      }
    }
    if(longestEvent != null){
      feats.add(new Feature("ClinSemType" + longestEvent.getClass().getSimpleName(), true));
    }
    
    
    
    return feats;
  }
}
