package org.apache.ctakes.coreference.extractors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bounds;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Context;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class ContinuousBag implements Context {

  private Context[] contexts;
  private String name = null;
  private Map<String, double[]> vectors = null;
  private int dims;
  
  public ContinuousBag(File vecFile, Context... contexts) throws FileNotFoundException {
    this.contexts = contexts;
    this.vectors = readVectorFile(vecFile);
//    String[] names = new String[contexts.length + 1];
//    names[0] = "ContinuousBag";
//    for (int i = 1; i < names.length; ++i) {
//      names[i] = contexts[i - 1].getName();
//    }
    this.name = Feature.createName("ContinuousBag");
  }
  
  private Map<String, double[]> readVectorFile(File vecFile) throws FileNotFoundException{
    Map<String, double[]> vectorMap = new HashMap<>();
    try(Scanner scanner = new Scanner(vecFile)){
      while(scanner.hasNextLine()){
        String[] termVec = scanner.nextLine().trim().split("\\s+");
        if(termVec.length == 2) continue; // some files have the first line with the dimensions
        dims = termVec.length-1;
        double[] vector = new double[dims];
        for(int i = 0; i < dims; i++){
          vector[i] = Double.parseDouble(termVec[i+1]);
        }
        vectorMap.put(termVec[0], vector);
      }
    }
    return vectorMap;
  }
  
  public String getName() {
    return this.name;
  }

  public <SEARCH_T extends Annotation> List<Feature> extract(JCas jCas, Annotation focusAnnotation, Bounds bounds,
      Class<SEARCH_T> annotationClass, FeatureExtractor1<SEARCH_T> extractor) throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    for (Context context : this.contexts) {
      double[] contextVec = new double[dims];
      int numComponents = 0;
      for (Feature feature : context.extract(
          jCas,
          focusAnnotation,
          bounds,
          annotationClass,
          extractor)) {
        
        if(this.vectors.containsKey(feature.getValue())){
          double[] featVec = this.vectors.get(feature.getValue().toString().toLowerCase());
          addToVector(contextVec, featVec);
          numComponents++;
        }
      }
      if(numComponents > 0){
        for(int i = 0; i < dims; i++){
          feats.add(new Feature(Feature.createName(this.name, context.getName(), String.valueOf(i)), contextVec[i] / numComponents));
        }
      }
    }
    return feats;
  }
  
  private static void addToVector(double[] vec1, double[] vec2){
    for(int i = 0; i < vec1.length; i++){
      vec1[i] += vec2[i];
    }
  }
  
  public static class Surrounding implements CleartkExtractor.Context {

    public String getName() {
      return "Surrounding";
    }

    public <SEARCH_T extends Annotation> List<Feature> extract(JCas jCas, Annotation focusAnnotation, Bounds bounds,
        Class<SEARCH_T> annotationClass, FeatureExtractor1<SEARCH_T> extractor) throws CleartkExtractorException {
      List<Feature> feats = new ArrayList<>();
      
      return feats;
    }
    
  }
}
