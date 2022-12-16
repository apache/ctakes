package org.apache.ctakes.core.cleartk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;
import org.cleartk.ml.Feature;


public class ContinuousTextExtractor implements NamedFeatureExtractor1<BaseToken>  {
  public enum OovStrategy {OOV_FEATURE, EMPTY_VECTOR, MEAN_VECTOR}
  
	private int dims;
	private WordEmbeddings words = null;
	private OovStrategy oovStrategy = null;
	
  public ContinuousTextExtractor(String vecFile) throws
  CleartkExtractorException {
    this(vecFile, OovStrategy.OOV_FEATURE);
  }
  
    public ContinuousTextExtractor(String vecFile, OovStrategy oovStrategy) throws
	CleartkExtractorException {
		super();
		try {
			words =
					WordVectorReader.getEmbeddings(FileLocator.getAsStream(vecFile));
		} catch (IOException e) {
			e.printStackTrace();
			throw new CleartkExtractorException(e);
		}
		this.oovStrategy = oovStrategy;
	}
	@Override
	public List<Feature> extract(JCas view, BaseToken token) throws
	CleartkExtractorException {
		List<Feature> feats = new ArrayList<>();

		String wordText = token.getCoveredText();
		WordVector vec = null;
		if(words.containsKey(wordText)){
			vec = words.getVector(wordText);
		}else if(words.containsKey(wordText.toLowerCase())){
			vec = words.getVector(wordText.toLowerCase());
		}else{
		  if(this.oovStrategy == OovStrategy.OOV_FEATURE){
		    feats.add(new Feature(getFeatureName(), "OOV"));
		    return feats;
		  }else if(this.oovStrategy == OovStrategy.EMPTY_VECTOR){
		    vec = new WordVector("_empty_", new double[words.getDimensionality()]);
		  }else if(this.oovStrategy == OovStrategy.MEAN_VECTOR){
		    vec = words.getMeanVector();
		  }
		}

		for(int i = 0; i < vec.size(); i++){
			feats.add(new Feature(getFeatureName() + "_" + i, vec.getValue(i)));
		}
		return feats;
	}

	public int getEmbeddingsDimensionality(){
	  return words.getDimensionality();
	}
	
	@Override
	public String getFeatureName() {
		return "ContinuousText";
	}

}
