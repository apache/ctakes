package org.apache.ctakes.core.cleartk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bounds;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;

public class PrecedingWithPadding extends Preceding {

  public int dims;
  
  public PrecedingWithPadding(int end, int dims){
    super(0, end);
    this.dims = dims;
  }
  
  @Override
  public <SEARCH_T extends Annotation> List<Feature> extract(JCas jCas,
      Annotation focusAnnotation, Bounds bounds,
      Class<SEARCH_T> annotationClass, FeatureExtractor1<SEARCH_T> extractor)
      throws CleartkExtractorException {
    LinkedList<Feature> rawFeats = new LinkedList<>(super.extract(jCas, focusAnnotation, bounds, annotationClass, extractor));
    List<Feature> processedFeats = new ArrayList<>();

    for(Feature feat : rawFeats){
      if(feat.getValue().toString().startsWith("OOB")){
        // add one feature for each dimension and set it to 0.
        for(int j = 0; j < this.dims; j++){
          processedFeats.add(new Feature(feat.getName() + "_" + j, 0.0));
        }
      }else{
        processedFeats.add(feat);
      }
    }
    return processedFeats;
  }  
}
