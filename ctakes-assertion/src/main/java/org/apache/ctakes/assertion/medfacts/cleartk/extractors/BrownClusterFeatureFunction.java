/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

import com.google.common.collect.Maps;

public class BrownClusterFeatureFunction implements FeatureFunction {

  public static final String DEFAULT_NAME = "BrownCluster";
  public static final Pattern linePatt = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\d+)");
  
  private HashMap<String,String> word2class = null;
  
  public BrownClusterFeatureFunction() throws ResourceInitializationException{
    word2class = Maps.newHashMap();
    try{
      Scanner scanner = new Scanner(FileLocator.getAsStream("org/apache/ctakes/assertion/models/brown_clusters.txt"));
      while(scanner.hasNextLine()){
        String line = scanner.nextLine().trim();
        Matcher m = linePatt.matcher(line);
        if(m.matches()){
          word2class.put(m.group(2), m.group(1));
        }
      }
    }catch(FileNotFoundException e){
      throw new ResourceInitializationException(e);
    }
  }
  
  @Override
  public List<Feature> apply(@Nullable Feature input) {
    String featureName = Feature.createName(DEFAULT_NAME, input.getName());
    Object featureValue = input.getValue();
    if(featureValue instanceof String){
      return Collections.singletonList(new Feature(featureName, word2class.containsKey(featureValue) ? word2class.get(featureValue) : "NoCluster"));
    }
    return Collections.emptyList();
  }

}
