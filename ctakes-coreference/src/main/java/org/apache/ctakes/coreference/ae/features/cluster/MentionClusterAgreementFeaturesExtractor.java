package org.apache.ctakes.coreference.ae.features.cluster;

import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.getGender;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isDefinite;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.isDemonstrative;
import static org.apache.ctakes.coreference.ae.features.TokenFeatureExtractor.numberSingular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.util.ListIterable;
import org.apache.ctakes.coreference.util.MarkableCacheRelationExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class MentionClusterAgreementFeaturesExtractor implements RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>, FeatureExtractor1<Markable>, MarkableCacheRelationExtractor {

  private Map<Markable, ConllDependencyNode> cache = null;

  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    if(cache == null){
      throw new RuntimeException("This extractor requires a call to setCache()");
    }
    List<Feature> features = new ArrayList<>();

    String s = mention.getCoveredText().toLowerCase();
    boolean isDem = isDemonstrative(s);
    boolean isDef = isDefinite(s);
    String gender = getGender(s);
    boolean singular = numberSingular(jCas, mention, s, cache.get(mention));

    boolean matchDem = false;
    boolean matchDef = false;
    boolean matchGender = false;
    boolean matchNumber = false;
    
    for(IdentifiedAnnotation member : new ListIterable<IdentifiedAnnotation>(cluster.getMembers())){
      if(member == null){
        System.err.println("Found an empty cluster member in agreement features extractor.");
        continue;
      }else if(mention.getBegin() < member.getEnd()){
        // during training this might happen -- see a member of a cluster that
        // is actually subsequent to the candidate mention
        continue;
      }
      String m = member.getCoveredText().toLowerCase();
      if(!matchDem && isDemonstrative(m) == isDem){
        matchDem = true;
      }
      if(!matchDef && isDefinite(m) == isDef){
        matchDef = true;
      }
      if(!matchGender && getGender(m).equals(gender)){
        matchGender = true;
      }
      if(!matchNumber && numberSingular(jCas, member, m, cache.get(member)) == singular){
        matchNumber = true;
      }
    }
    
    features.add(new Feature("MC_AGREE_DEM", matchDem));
    features.add(new Feature("MC_AGREE_DEF", matchDef));
    features.add(new Feature("MC_AGREE_GEN", matchGender));
    features.add(new Feature("MC_AGREE_NUM", matchNumber));
    
    return features;
  }

  @Override
  public List<Feature> extract(JCas jCas, Markable mention) throws CleartkExtractorException {
    if(cache == null){
      throw new RuntimeException("This extractor requires a call to setCache()");
    }
    List<Feature> features = new ArrayList<>();

    String s = mention.getCoveredText().toLowerCase();

    boolean isDem = isDemonstrative(s);
    boolean isDef = isDefinite(s);
    features.add(new Feature("MC_MENTION_DEM", isDem));
    features.add(new Feature("MC_MENTION_DEF", isDef));
    
    String gender = getGender(s);
    features.add(new Feature("MC_MENTION_GENDER", gender));

    boolean singular = numberSingular(jCas, mention, s, cache.get(mention));
    features.add(new Feature("MC_MENTION_NUMBER", singular));

    return features;
  }

  @Override
  public void setCache(Map<Markable, ConllDependencyNode> cache) {
    this.cache = cache;
  }


}
