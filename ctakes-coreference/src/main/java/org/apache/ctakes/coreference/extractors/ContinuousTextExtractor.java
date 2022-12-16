package org.apache.ctakes.coreference.extractors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;

public class ContinuousTextExtractor implements NamedFeatureExtractor1<BaseToken>  {
  private int dims;
  private WordEmbeddings words = null;
  
  public ContinuousTextExtractor(String vecFile) throws CleartkExtractorException {
    super();
    try {
      words = WordVectorReader.getEmbeddings(FileLocator.getAsStream(vecFile));
    } catch (IOException e) {
      e.printStackTrace();
      throw new CleartkExtractorException(e);
    }
  }
  
  @Override
  public List<Feature> extract(JCas view, BaseToken token) throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    
    String wordText = token.getCoveredText();
    WordVector vec = null;
    if(words.containsKey(wordText)){
      vec = words.getVector(wordText);
    }else if(words.containsKey(wordText.toLowerCase())){
      vec = words.getVector(wordText.toLowerCase());
    }else{
      return feats;
    }
    
    for(int i = 0; i < vec.size(); i++){
      feats.add(new Feature(getFeatureName() + "_" + i, vec.getValue(i)));
    }
    return feats;
  }

  @Override
  public String getFeatureName() {
    return "ContinuousText";
  }
    
}
