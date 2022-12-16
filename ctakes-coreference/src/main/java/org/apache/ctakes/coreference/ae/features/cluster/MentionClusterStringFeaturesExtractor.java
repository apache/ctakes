package org.apache.ctakes.coreference.ae.features.cluster;

import static org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor.contentWords;
import static org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor.endMatch;
import static org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor.soonMatch;
import static org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor.startMatch;
import static org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor.wordOverlap;
import static org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor.wordSubstring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.util.ListIterable;
import org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor;
import org.apache.ctakes.coreference.util.MarkableCacheRelationExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class MentionClusterStringFeaturesExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation>,
        MarkableCacheRelationExtractor{

  private Map<Markable, ConllDependencyNode> cache = null;

  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    if(cache == null){
      throw new RuntimeException("This extractor requires a cached Markable->ConllDependencyNode map to be set with setCache()");
    }
    List<Feature> feats = new ArrayList<>();
    CounterMap<String> featCounts = new CounterMap<>();
    
    if(StringMatchingFeatureExtractor.isPronoun(mention)) return feats;
    
    String m = mention.getCoveredText();
    Set<String> mentionWords = contentWords(mention);
    Set<String> nonHeadMentionWords = new HashSet<>(mentionWords);
    ConllDependencyNode mentionHead = cache.get(mention);
    
    String mentionHeadString = null;
    if(mentionHead != null){
      mentionHeadString = mentionHead.getCoveredText().toLowerCase();
      nonHeadMentionWords.remove(mentionHeadString);

      int maxNonoverlap = 0;

      for(IdentifiedAnnotation member : new ListIterable<IdentifiedAnnotation>(cluster.getMembers())){
        if(member == null){
          System.err.println("Something that shouldn't happen has happened");
          continue;
        }else if(mention.getBegin() < member.getEnd()){
          // during training this might happen -- see a member of a cluster that
          // is actually subsequent to the candidate mention
          continue;
        }else if(StringMatchingFeatureExtractor.isPronoun(member)){
          continue;
        }

        String s = member.getCoveredText();
        Set<String> memberWords = contentWords(member);
        Set<String> nonHeadMemberWords = new HashSet<>(memberWords);
        ConllDependencyNode memberHead = cache.get(member);
        String memberHeadString = null;
        if(memberHead != null){
          memberHeadString = memberHead.getCoveredText().toLowerCase();
          nonHeadMemberWords.remove(memberHeadString);

          if(mentionHeadString.equals(memberHeadString)){

            if(m.equalsIgnoreCase(s)) featCounts.add("MC_STRING_EXACT");
            if(startMatch(m,s)) featCounts.add("MC_STRING_START");
            if(endMatch(m,s)) featCounts.add("MC_STRING_END");
            if(soonMatch(m,s)) featCounts.add("MC_STRING_SOON");
            if(wordOverlap(mentionWords, memberWords)) featCounts.add("MC_OVERLAP");
            if(wordSubstring(mentionWords, memberWords)) featCounts.add("MC_SUB");

            int nonHeadOverlap = wordNonOverlapCount(nonHeadMemberWords, nonHeadMentionWords);
            if(nonHeadOverlap > maxNonoverlap){
              maxNonoverlap = nonHeadOverlap;
            }
          }
        }
      }
      feats.add(new Feature("MC_MAX_NONOVERLAP", maxNonoverlap));
    }
    
    
    for(String featKey : featCounts.keySet()){
      // normalized
//      feats.add(new Feature(featKey, (double) featCounts.get(featKey) / clusterSize));
      // boolean
      feats.add(new Feature(featKey, true));
    }
    return feats;
  }
  
  public static int wordNonOverlapCount(Set<String> w1, Set<String> w2){
    int count = 0;
    
    for(String w : w1){
      if(!w2.contains(w)) count++;
    }
    
    for(String w : w2){
      if(!w1.contains(w)) count++;
    }
    return count;
  }

  @Override
  public void setCache(Map<Markable, ConllDependencyNode> cache) {
    this.cache = cache;
  }
}
