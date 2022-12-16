package org.apache.ctakes.coreference.ae.features.cluster;

import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isGeneric;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isHistory;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isNegated;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isPatient;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isUncertain;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class MentionClusterAttributeFeaturesExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation>, FeatureExtractor1<Markable> {

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    List<Feature> features = new ArrayList<>();
    
    boolean mentionNegated = isNegated(mention);
    boolean mentionUnc = isUncertain(mention);
//    boolean mentionGen = isGeneric(mention);
//    boolean mentionSubj = isPatient(mention);
//    boolean mentionHist = isHistory(mention);
    
    boolean mentionTimex = isTimex(mention);

    boolean matchNeg = true;
    boolean clusterTimex = false;  // if any cluster member is timex
    boolean matchUnc = true;
//    boolean matchGen = true;
//    boolean matchSubj = true;
//    boolean matchHist = true;
    
    for(Markable member : JCasUtil.select(cluster.getMembers(), Markable.class)){
      if(member.getBegin() > mention.getEnd()){
        break;
      }
      if(mentionNegated != isNegated(member)){
        matchNeg = false;
      }
      if(mentionUnc != isUncertain(member)){
        matchUnc = false;
      }
//      if(mentionGen != isGeneric(member)){
//        matchGen = false;
//      }
//      if(mentionSubj != isPatient(member)){
//        matchSubj = false;
//      }
//      if(mentionHist != isHistory(member)){
//        matchHist = false;
//      }
      if(isTimex(member)){
        clusterTimex = true;
      }
    }
    
    features.add(new Feature("MC_AGREE_NEG", matchNeg));
    features.add(new Feature("MC_AGREE_UNC", matchUnc));
//    features.add(new Feature("MC_AGREE_GEN", matchGen));
//    features.add(new Feature("MC_AGREE_SUBJ", matchSubj));
//    features.add(new Feature("MC_AGREE_HIST", matchHist));
    
    features.add(new Feature("MC_AGREE_TIMEX", clusterTimex == mentionTimex));

    /// check attributes like location/degree/negation/uncertainty
    /*
    Set<String> mentionSites = new HashSet<>();
    
    
    if(mentionHead != null){
      for(IdentifiedAnnotation annot : JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, mentionHead)){
        LocationOfTextRelation rel = getLocation(annot);
        if(rel != null){
          AnatomicalSiteMention site = (AnatomicalSiteMention)rel.getArg2().getArgument();
          for(UmlsConcept concept : JCasUtil.select(site.getOntologyConceptArr(), UmlsConcept.class)){
            mentionSites.add(concept.getCui());
          }
        }
      }
    }

    if(mentionSites.size() > 0){
      Set<String> memberSites = new HashSet<>();
      for(Markable member : JCasUtil.select(cluster.getMembers(), Markable.class)){
        if(mention.getBegin() <= member.getBegin()) break;
        ConllDependencyNode memberHead = DependencyUtility.getNominalHeadNode(jCas, member);
        if(memberHead == null) continue;
        
        for(IdentifiedAnnotation annot : JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, memberHead)){
          LocationOfTextRelation rel = getLocation(annot);
          if(rel != null){
            boolean conflict = true;
            AnatomicalSiteMention site = (AnatomicalSiteMention)rel.getArg2().getArgument();
            for(UmlsConcept concept : JCasUtil.select(site.getOntologyConceptArr(), UmlsConcept.class)){
              memberSites.add(concept.getCui());
              if(mentionSites.contains(concept.getCui())){
                conflict = false;
              }
            }
            if(conflict){
              features.add(new Feature("MC_LOCATION_CONFLICT", true));
            }
          }
        }
      }
    }
    */
    return features;
  }

  @Override
  public List<Feature> extract(JCas view, Markable mention)
      throws CleartkExtractorException {
    List<Feature> features = new ArrayList<>();
    
    boolean mentionNegated = isNegated(mention);
    features.add(new Feature("MC_MENTION_NEGATED", mentionNegated));
    
    boolean mentionUncertain = isUncertain(mention);
    features.add(new Feature("MC_MENTION_UNCERTAIN", mentionUncertain));
    
    boolean mentionGen = isGeneric(mention);
    features.add(new Feature("MC_MENTION_GENERIC", mentionGen));
    
    boolean mentionSubj = isPatient(mention);
    features.add(new Feature("MC_MENTION_PATIENT", mentionSubj));
    
    boolean mentionHist = isHistory(mention);
    features.add(new Feature("MC_MENTION_HISTORY", mentionHist));

    boolean mentionTimex = isTimex(mention);
    features.add(new Feature("MC_MENTION_TIMEX", mentionTimex));

    return features;
  }
  
  private static boolean isTimex(Annotation a){
    return JCasUtil.selectCovered(TimeMention.class, a).size() > 0;
  }
  
  @SuppressWarnings("unused")
  private static LocationOfTextRelation getLocation(IdentifiedAnnotation annot){
    LocationOfTextRelation rel = null;
    if(annot instanceof ProcedureMention){
      rel = ((ProcedureMention)annot).getBodyLocation();
    }else if(annot instanceof DiseaseDisorderMention){
      rel = ((DiseaseDisorderMention)annot).getBodyLocation();
    }else if(annot instanceof SignSymptomMention){
      rel = ((SignSymptomMention)annot).getBodyLocation();
    }
    return rel;
  }
}
