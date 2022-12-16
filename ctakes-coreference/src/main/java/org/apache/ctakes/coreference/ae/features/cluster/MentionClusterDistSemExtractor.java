package org.apache.ctakes.coreference.ae.features.cluster;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.ListIterable;
import org.apache.ctakes.coreference.ae.features.StringMatchingFeatureExtractor;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

public class MentionClusterDistSemExtractor implements
    RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation> {

  public static final double DEFAULT_SIM = 0.5;  
  
  private WordEmbeddings words = null;
  
  public MentionClusterDistSemExtractor() throws FileNotFoundException, IOException{
    this("org/apache/ctakes/coreference/distsem/mimic_vectors.txt");
  }
  
  public MentionClusterDistSemExtractor(String embeddingsPath) throws FileNotFoundException, IOException{
    words = WordVectorReader.getEmbeddings(FileLocator.getAsStream(embeddingsPath));
  }

  @Override
  public List<Feature> extract(JCas jCas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    if(StringMatchingFeatureExtractor.isPronoun(mention)) return feats;
    
    double maxSim = 0.0;
    double maxPhraseSim = 0.0;
    
    ConllDependencyNode mentionNode = DependencyUtility.getNominalHeadNode(jCas, mention);
    
    double[] mentionVec = getPhraseVec(mention);
    boolean exactMatch = false;
    
    // first, do not bother with pronouns:
    String mentionHead = mentionNode != null ? mentionNode.getCoveredText().toLowerCase() : null;
    if(mentionHead != null){
      for(Markable member : new ListIterable<Markable>(cluster.getMembers())){
        if(mention.getBegin() < member.getEnd()){
          // during training this might happen -- see a member of a cluster that
          // is actually subsequent to the candidate mention
          break;
        }

        double[] memberVec = getPhraseVec(member);
        
        double phraseSim = 0.0;
        for(int i = 0; i < memberVec.length; i++){
          phraseSim += (mentionVec[i] * memberVec[i]);
        }
        if(phraseSim > maxPhraseSim){
          maxPhraseSim = phraseSim;
        }
        
        ConllDependencyNode memberNode = DependencyUtility.getNominalHeadNode(jCas, member);
        String memberHead = memberNode != null ? memberNode.getCoveredText().toLowerCase() : null;
        if(mentionHead.equals(memberHead)){
          exactMatch = true;
        }
        if(memberNode != null && words.containsKey(memberHead) && words.containsKey(mentionHead)){
          double sim = words.getSimilarity(mentionHead, memberHead);
          if(sim > maxSim){
            maxSim = sim;
          }
        }
      }
    }
    if(exactMatch){
      maxSim = 0.0;
    }
    
    feats.add(new Feature("HEAD_SIMILARITY_WORD2VEC", maxSim));
//    feats.add(new Feature("PHRASE_SIMILARITY_WORD2VEC", maxPhraseSim));
    
    return feats;
  }

  private double[] getPhraseVec(Annotation annotation){
    double[] phraseVec = new double[words.getDimensionality()];
    double vecLength = 0.0;
    
    for(BaseToken token : JCasUtil.selectCovered(BaseToken.class, annotation)){
      String word = token.getCoveredText().toLowerCase();
      if(words.containsKey(word)){
        WordVector vec = words.getVector(word);
        for(int i = 0; i < phraseVec.length; i++){
          double val = vec.getValue(i);
          phraseVec[i] += val;
          vecLength = (val * val);
        }
      }
    }
    
    // normalize vector:
    for(int i = 0; i < phraseVec.length; i++){
      double val = phraseVec[i];
      vecLength += (val * val);
    }
    vecLength = Math.sqrt(vecLength);
    
    if(vecLength > 0.0){
      for(int i = 0; i < phraseVec.length; i++){
        phraseVec[i] /= vecLength;
      }    
    }
    
    return phraseVec;
  }
}
