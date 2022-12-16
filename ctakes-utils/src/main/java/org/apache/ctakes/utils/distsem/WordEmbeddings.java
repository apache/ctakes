package org.apache.ctakes.utils.distsem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordEmbeddings {

  private Map<String,WordVector> vectors = null;
  private int dimensionality = 0;
  private WordVector meanVector = null;
  private WordVector rawMeanVector = null;
  
  public WordEmbeddings(int dim){
    this.vectors = new HashMap<>();
    this.dimensionality = dim;
    this.meanVector = new WordVector("_mean_", new double[this.dimensionality]);
    this.rawMeanVector = new WordVector("_mean_raw", new double[this.dimensionality]);
  }
  
  public WordEmbeddings(Map<String,WordVector> vectors){
    this.vectors = vectors;
  }

  public double getSimilarity(String word1, String word2){
    WordVector vec1 = getVector(word1);
    WordVector vec2 = getVector(word2);
    
    assert vec1.size() == vec2.size();
    double sim = 0.0;
    for(int i = 0; i < vec1.size(); i++){
      sim += (vec1.getValue(i) * vec2.getValue(i));
    }
    
    sim = sim / (vec1.getLength()*vec2.getLength());
    return sim;
  }

  public void add(String line){
    int wordBreak = line.indexOf(' ');
    String word = line.substring(0, wordBreak);
    String[] dims = line.substring(wordBreak+1).split(" ");
    
    if(this.meanVector == null){
      this.meanVector = new WordVector("_mean_", new double[dims.length]);
      this.rawMeanVector = new WordVector("_mean_raw", new double[dims.length]);
    }
    
    double[] vector = new double[dims.length];
    for(int i = 0; i < dims.length; i++){
      vector[i] = Double.valueOf(dims[i]);
      meanVector.vector[i] += vector[i];
    }
    vectors.put(word, new WordVector(word, vector));
  }
  
  public boolean containsKey(String word){
    return vectors.containsKey(word);
  }
  
  public WordVector getVector(String word){
    if(vectors != null){
      return vectors.get(word);
    }
    return null;
  }

  public int getDimensionality(){
    return this.dimensionality;
  }
  
  public List<String> getSimilarWords(String word, int maxWords) {
    double[] sims = new double[maxWords];
    List<String> words = new ArrayList<>(20);
    Arrays.fill(sims, -1);
    for(String comp : vectors.keySet()){
      double sim = getSimilarity(word, comp);
      if(word.equals(comp)){
        continue;
      }else if(words.size() == 0){
        words.add(comp);
        sims[0] = sim;
        continue;
      }else if(sim < sims[maxWords-1]){
        // most words won't be greater than the minimum similarity -- quit right away
        continue;
      }
      for(int i = Math.min(maxWords-1, words.size()); i >= 0; i--){
        // compare the similarity.
        // if we're here we know that sim > sims[i], just seeing if we can keep going backwards
        if(i > 0 && sim > sims[i-1]){
          // shift over the score and the word
          sims[i] = sims[i-1];
          if(words.size() <= i){
            words.add(words.get(i-1));
          }else{
            words.set(i, words.get(i-1));
          }
        }else{
          // found our position for our new word:
          sims[i] = sim;
          if(words.size() <= i){
            words.add(comp);
          }else{
            words.set(i, comp);
          }
          break;
        }
      }
    }
    return words;
  }
  
  public WordVector getMeanVector(){
    for(int i = 0; i < this.rawMeanVector.getLength(); i++){
      this.meanVector.vector[i] = this.rawMeanVector.vector[i] / vectors.size();
    }
    return this.meanVector;
  }
}
