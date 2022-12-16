package org.apache.ctakes.temporal.utils;

import java.util.HashMap;
import java.util.Map;

public class SoftMaxUtil {

   public static <X> Map<X,Double> getDistributionFromScores(Map<X,Double> scores){
     Map<X,Double> dists = new HashMap<>();
     if(isDistribution(scores)){
       return scores;
     }
     
     double sum = 0.0;
     for(Map.Entry<X, Double> entry : scores.entrySet()){
       double val = entry.getValue();
       double adjusted = 1.0 / (1.0 + Math.exp(-val));
       dists.put(entry.getKey(), adjusted);
       sum += adjusted;
     }
     
     for(X key : scores.keySet()){
       dists.put(key, dists.get(key) / sum);
     }
     
     return dists;
   }
   
   public static <X> boolean isDistribution(Map<X,Double> scores){
      double sum = 0.0;
      for(Double val : scores.values()){
        if(val < 0){
          return false;
        }
        sum += val;
      }
      
      // check if the sum is close to 1:
      if(Math.abs(sum - 1.0) < 0.01){
        return true;
      }
      
      return false;
   }
}
