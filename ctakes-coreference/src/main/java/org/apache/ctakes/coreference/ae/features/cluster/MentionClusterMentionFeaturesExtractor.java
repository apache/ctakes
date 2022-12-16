package org.apache.ctakes.coreference.ae.features.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.coreference.extractors.ContinuousTextExtractor;
import org.apache.ctakes.relationextractor.ae.features.DependencyTreeFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bag;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.FirstCovered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.LastCovered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.NamingExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;

public class MentionClusterMentionFeaturesExtractor implements FeatureExtractor1<Markable> {

  private FeatureExtractor1<BaseToken> coveredText = new CoveredTextExtractor<>();
  private FeatureExtractor1<Markable> tokenIdentityContext = new CleartkExtractor<>(
      BaseToken.class,
      coveredText,
      new FirstCovered(1),
      new LastCovered(1),
      new Bag(new Covered()),
      new Preceding(3),
      new Following(3));
  
  private FeatureExtractor1<BaseToken> continuousText = null;
  private FeatureExtractor1<Markable> tokenVectorContext = null;      

  private FeatureExtractor1<BaseToken> pos = new TypePathExtractor<>(BaseToken.class, "partOfSpeech");

  /**
   * All part-of-speech tags of the mention as a bag
   */
  private FeatureExtractor1<Markable> tokenPOS = new CleartkExtractor<>(
      BaseToken.class,
      pos,
      new Bag(new Covered()));

  /**
   * All extractors for mention 1, with features named to distinguish them from mention 2
   */
  private FeatureExtractor1<Markable> mentionFeaturesExtractor = new NamingExtractor1<>(
      "mention1pos",
      tokenPOS);

  public MentionClusterMentionFeaturesExtractor() throws CleartkExtractorException{
    this(null);
  }
  
  public MentionClusterMentionFeaturesExtractor(String vectorFile) throws CleartkExtractorException {
    if(vectorFile != null){
      this.continuousText = new ContinuousTextExtractor(vectorFile);
      this.tokenVectorContext = new CleartkExtractor<>(
          BaseToken.class,
          continuousText,
          new FirstCovered(1),
          new LastCovered(1),
//          new Bag(new Covered()),
          new Preceding(1),
          new Following(1));
    }
  }
  
  @Override
  public List<Feature> extract(JCas view, Markable focusAnnotation) throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    // token features:
    feats.addAll(tokenIdentityContext.extract(view, focusAnnotation));
    
    
    // token vector features:
//    if(this.tokenVectorContext != null){
//      feats.addAll(this.tokenVectorContext.extract(view, focusAnnotation));
//    }
    
    // pos features:
    feats.addAll(mentionFeaturesExtractor.extract(view, focusAnnotation));
    
    // Always do num covered and dep features
    feats.add(new Feature("NumCoveredTokens", JCasUtil.selectCovered(BaseToken.class, focusAnnotation).size()));
    feats.addAll(DependencyTreeFeaturesExtractor.extractForNode(view, focusAnnotation, "dep"));
    
    return feats;
  }

}
