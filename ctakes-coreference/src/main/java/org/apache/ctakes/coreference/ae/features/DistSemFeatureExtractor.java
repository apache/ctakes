package org.apache.ctakes.coreference.ae.features;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class DistSemFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  // default value is 0.5 (rather than 0.0) because we don't want to assume OOV words are dissimilar
  public static final double DEFAULT_SIM = 0.5;  
  
  private WordEmbeddings words = null;
  
  public DistSemFeatureExtractor() throws FileNotFoundException, IOException{
    words = WordVectorReader.getEmbeddings(FileLocator.getAsStream("org/apache/ctakes/coreference/distsem/mimic_vectors.txt"));
  }
  
  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    
    double sim = 0.0;
//    double[] a1vec = getArgVector(arg1);
//    double[] a2vec = getArgVector(arg2);
//    
//    if(a1vec != null && a2vec != null){
//      for(int i = 0; i < a1vec.length; i++){
//        sim += a1vec[i] * a2vec[i];
//      }
//    }else{
//      sim = DEFAULT_SIM;
//    }
//    
//    assert !Double.isNaN(sim);
//    
//    feats.add(new Feature("ARG_SIMILARITY_WORD2VEC", sim));
    
    ConllDependencyNode node1 = DependencyUtility.getNominalHeadNode(jCas, arg1);
    ConllDependencyNode node2 = DependencyUtility.getNominalHeadNode(jCas, arg2);
    String head1 = node1 != null ? node1.getCoveredText().toLowerCase() : null;
    String head2 = node2 != null ? node2.getCoveredText().toLowerCase() : null;
    if(head1 != null && head2 != null && words.containsKey(head1) && words.containsKey(head2)){
      sim = words.getSimilarity(head1, head2);
    }else{
      sim = DEFAULT_SIM;
    }
    feats.add(new Feature("HEAD_SIMILARITY_WORD2VEC", sim));
    
    return feats;
  }

  
  @SuppressWarnings("unused")
  private double[] getArgVector(IdentifiedAnnotation arg){
    double[] vec = null;
    
    Collection<BaseToken> tokens = JCasUtil.selectCovered(BaseToken.class, arg);
    
    for(BaseToken token : tokens){
      WordVector wv = words.getVector(token.getCoveredText());
      if(wv == null){
        wv = words.getVector(token.getCoveredText().toLowerCase());
      }
      if(wv != null){
        if(vec == null){
          vec = new double[wv.size()];
          Arrays.fill(vec, 0.0);
        }
        for(int i = 0; i < vec.length; i++){
          vec[i] += wv.getValue(i);
        }
      }
    }
    
    if(vec != null){
      double len = 0.0;
      for(int i = 0; i < vec.length; i++){
        len += vec[i]*vec[i];
      }
      len = Math.sqrt(len);
      assert !Double.isNaN(len);
      for(int i = 0; i < vec.length; i++){
        vec[i] /= len;
      }
    }
    return vec;
  }
}
