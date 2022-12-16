package org.apache.ctakes.utils.distsem;

public class WordVector {

  String word;
  double[] vector;
  int size;
  
  public WordVector(String word, double[] vector){
    this.word = word;
    this.vector = vector;
    this.size = vector.length;
  }
  
  public double getValue(int i){
    if(i >= 0 && i < vector.length){
      return vector[i];
    }else{
      throw new ArrayIndexOutOfBoundsException();
    }
  }
  
  public double getLength(){
    double len = 0;
    for(int i = 0; i < size; i++){
      len += vector[i]*vector[i];
    }
    
    return Math.sqrt(len);
  }
  
  public int size(){
    return size;
  }
}
