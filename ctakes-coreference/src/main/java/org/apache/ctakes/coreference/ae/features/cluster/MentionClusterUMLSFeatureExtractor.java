package org.apache.ctakes.coreference.ae.features.cluster;

import static org.apache.ctakes.coreference.ae.features.UMLSFeatureExtractor.alias;
import static org.apache.ctakes.coreference.ae.features.UMLSFeatureExtractor.getDocId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.util.ListIterable;
import org.apache.ctakes.coreference.util.MarkableCacheRelationExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class MentionClusterUMLSFeatureExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation>, FeatureExtractor1<Markable>,
        MarkableCacheRelationExtractor{

  String docId = null;
  Map<ConllDependencyNode,Collection<IdentifiedAnnotation>> coveringMap = null;
  Map<Markable, ConllDependencyNode> cache = null;

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {

    if(cache == null){
      throw new RuntimeException("This extractor requires a Markable cache.");
    }

    List<Feature> feats = new ArrayList<>();
    Set<String> trueFeats = new HashSet<>();
    
    if(docId == null || !getDocId(jCas).equals(docId)){
      docId = getDocId(jCas);
      coveringMap = JCasUtil.indexCovering(jCas, ConllDependencyNode.class, IdentifiedAnnotation.class);
    }
    
    ConllDependencyNode head = cache.get(mention);
    
    if(head != null){
      List<IdentifiedAnnotation> rmList = new ArrayList<>();
      // get the entities covering this markable:
      List<IdentifiedAnnotation> mentionEnts = new ArrayList<>(coveringMap.get(head)); //JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, head1.getBegin(), head1.getEnd());'
      for(IdentifiedAnnotation ann : mentionEnts){
        if(!(ann instanceof EntityMention || ann instanceof EventMention) || ann.getClass() == EventMention.class){
          rmList.add(ann);
        }
      }
      for(IdentifiedAnnotation toRm : rmList){
        mentionEnts.remove(toRm);
      }
      
      Set<IdentifiedAnnotation> clusterEnts = new HashSet<>();
      for(Markable member : new ListIterable<Markable>(cluster.getMembers())){
        ConllDependencyNode memberHead = cache.get(member);
        rmList.clear();
        // get the named entities covering this cluster member:
        List<IdentifiedAnnotation> ents2 = new ArrayList<>(coveringMap.get(memberHead)); //JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, head2.getBegin(), head2.getEnd());
        for(IdentifiedAnnotation ann : ents2){
          if(!(ann instanceof EntityMention || ann instanceof EventMention) || ann.getClass() == EventMention.class){
            rmList.add(ann);
          }
        }
        for(IdentifiedAnnotation toRm : rmList){
          ents2.remove(toRm);
        }
        
        clusterEnts.addAll(ents2);
      }
      
      if(clusterEnts.size() == 0 && mentionEnts.size() > 0){
        trueFeats.add("ClusterNoCui_MentionCui");
      }else if(clusterEnts.size() > 0 && mentionEnts.size() == 0){
        trueFeats.add("ClusterCui_MentionNoCui");          
      }else if(clusterEnts.size() == 0 && mentionEnts.size() == 0){
        trueFeats.add("ClusterMentionNoCui");
      }else{
        trueFeats.add("ClusterMentionBothCui");
      }
      
      if((clusterEnts.size() == 0 && mentionEnts.size() > 0) ||
          (clusterEnts.size() > 0 && mentionEnts.size() == 0)){
        trueFeats.add("ClusterOrMentionNoCui");
      }
      
//      int minDistance = Integer.MAX_VALUE;
      for(IdentifiedAnnotation ent1 : clusterEnts){
        HashSet<String> a1Tuis = new HashSet<>(); 
        String a1SemType = ent1.getClass().getSimpleName();
        trueFeats.add("ClusterSemType" + a1SemType);
        FSArray cons1 = ent1.getOntologyConceptArr();
        if(cons1 != null){
          for(int i = 0; i < cons1.size(); i++){
            if(cons1.get(i) instanceof UmlsConcept){
              UmlsConcept concept = (UmlsConcept)cons1.get(i);
              if(concept.getTui() != null){
                a1Tuis.add(concept.getTui());
              }
            }
          }
        }
        for(IdentifiedAnnotation ent2 : mentionEnts){
          HashSet<String> a2Tuis = new HashSet<>();
          String a2SemType = ent2.getClass().getSimpleName();
//          trueFeats.add("MentionSemType" + a2SemType);
                   
          if(alias(ent1, ent2)){
            trueFeats.add("UMLS_ALIAS");
          }

          /*
          if(!trueFeats.contains("UMLS_ALIAS") && isHypernym(ent1, ent2)){
            trueFeats.add("IS_HYPERNYM");
          }
          
          if(!trueFeats.contains("UMLS_ALIAS") && isHyponym(ent1, ent2)){
            trueFeats.add("IS_HYPONYM");
          }
          */

//          int pairDist = graphDistance(ent1, ent2);
//          if(Math.abs(pairDist) < Math.abs(minDistance)){
//            minDistance = pairDist;
//          }
          
          trueFeats.add("MentionClusterSemTypePair" + a1SemType + "_" + a2SemType);
          
          FSArray cons2 = ent2.getOntologyConceptArr();
          if(cons2 != null){
            for(int i = 0; i < cons2.size(); i++){
              if(cons2.get(i) instanceof UmlsConcept){
                UmlsConcept concept = (UmlsConcept)cons2.get(i);
                if(concept.getTui() != null){
                  a2Tuis.add(concept.getTui());
                }
              }
            }
          }
          for(String tui1 : a1Tuis){
//            trueFeats.add("ClusterTui_" +  tui1);
            for(String tui2 : a2Tuis){
//              trueFeats.add("ClusterTui_" + tui1 + "_MentionTui_ " + tui2);
              if(tui1.equals(tui2)){
                trueFeats.add("ClusterMentionTuiMatch");
              }
            }
          }
//          for(String tui2 : a2Tuis){
//            trueFeats.add("MentionTui_" + tui2);
//          }
        }
      }
//      double distFeat = 0.0;
//      if(minDistance != Integer.MAX_VALUE){
//        distFeat = 1.0 / minDistance;
//        if(distFeat < 0){
//          feats.add(new Feature("AncestorDistance", -distFeat));
//        }else{
//          feats.add(new Feature("DescendentDistance", distFeat));
//        }
//      }        
    }
    
    
    for(String feat : trueFeats){
      feats.add(new Feature(feat, true));
    }
    return feats;
  }

  @Override
  public List<Feature> extract(JCas jCas, Markable mention) throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    Set<String> trueFeats = new HashSet<>();
    
    if(docId == null || !getDocId(jCas).equals(docId)){
        docId = getDocId(jCas);
        coveringMap = JCasUtil.indexCovering(jCas, ConllDependencyNode.class, IdentifiedAnnotation.class);
    }
    
    ConllDependencyNode head = cache.get(mention);

    List<IdentifiedAnnotation> rmList = new ArrayList<>();
    // get the entities covering this markable:
    List<IdentifiedAnnotation> mentionEnts = new ArrayList<>(coveringMap.get(head)); //JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, head1.getBegin(), head1.getEnd());'
    for(IdentifiedAnnotation ann : mentionEnts){
      if(!(ann instanceof EntityMention || ann instanceof EventMention) || ann.getClass() == EventMention.class){
        rmList.add(ann);
      }
    }
    for(IdentifiedAnnotation toRm : rmList){
      mentionEnts.remove(toRm);
    }

    for(IdentifiedAnnotation ent : mentionEnts){
      String a2SemType = ent.getClass().getSimpleName();
      trueFeats.add("MentionSemType" + a2SemType);
    }
    
    for(String feat : trueFeats){
      feats.add(new Feature(feat, true));
    }

    return feats;
  }

  @Override
  public void setCache(Map<Markable, ConllDependencyNode> cache) {
    this.cache = cache;
  }
}
