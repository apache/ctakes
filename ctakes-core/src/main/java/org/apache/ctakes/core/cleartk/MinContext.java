package org.apache.ctakes.core.cleartk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bounds;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Context;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class MinContext implements CleartkExtractor.Context {

  private Context[] contexts;

  private String name;

  /**
   * Constructs a {@link Context} which converts the features extracted by the argument contexts
   * into a bag of features where all features have the same name.
   * 
   * @param contexts
   *          The contexts which should be combined into a bag.
   */
  public MinContext(Context... contexts) {
    this.contexts = contexts;
    String[] names = new String[contexts.length + 1];
    names[0] = "Min";
    for (int i = 1; i < names.length; ++i) {
      names[i] = contexts[i - 1].getName();
    }
    this.name = Feature.createName(names);
  }

  public String getName() {
    return this.name;
  }

  public <SEARCH_T extends Annotation> List<Feature> extract(JCas jCas,
      Annotation focusAnnotation, Bounds bounds,
      Class<SEARCH_T> annotationClass, FeatureExtractor1<SEARCH_T> extractor)
      throws CleartkExtractorException {
    HashMap<String,Double> runningTotals = new HashMap<>();

    for (Context context : this.contexts) {
      for (Feature feature : context.extract(
          jCas,
          focusAnnotation,
          bounds,
          annotationClass,
          extractor)) {
        try{
          double val = Double.parseDouble(feature.getValue().toString());
          if(!runningTotals.containsKey(feature.getName())){
            runningTotals.put(feature.getName(), 0.0);
          }
          runningTotals.put(feature.getName(), Double.min(runningTotals.get(feature.getName()), val));
        }catch(Exception e){
          // just ignore this feature?
        }
      }
    }
    List<Feature> features = new ArrayList<>();
    for(String key : runningTotals.keySet()){
      features.add(new Feature(this.name + "_" + key, runningTotals.get(key)));
    }
    return features;
  }

}
