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
package org.apache.ctakes.temporal.ae.feature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class CoveredTextToValuesExtractor implements FeatureExtractor1 {

  private String name;

  private Map<String, double[]> textDoublesMap;

  private double[] meanValues;
  
  public static Map<String, double[]> parseTextDoublesMap(File file, Charset charset) throws IOException {
    return Files.readLines(file, charset, new StringToDoublesProcessor());
  }

  static class StringToDoublesProcessor implements LineProcessor<Map<String, double[]>> {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Map<String, double[]> result = new HashMap<String, double[]>();

    private int length = -1;

    @Override
    public Map<String, double[]> getResult() {
      return this.result;
    }

    @Override
    public boolean processLine(String line) throws IOException {
      String[] parts = line.trim().split(",");
      String key = parts[0];
      int partsOffset = 0;
      if (this.length == -1) {
        this.length = parts.length;
      } else if (parts.length != this.length) {
        String message = "expected %d parts, found %d, skipping line '%s'";
        this.logger.warning(String.format(message, this.length, parts.length, line));
        return true;
      }
      double[] values = new double[parts.length - 1];
      for (int i = 0; i < values.length; ++i) {
        values[i] = Double.parseDouble(parts[i + 1 + partsOffset]);
      }
      this.result.put(key, values);
      return true;
    }
  }

  public CoveredTextToValuesExtractor(String name, Map<String, double[]> textDoublesMap) {
    super();
    this.name = name;
    this.textDoublesMap = textDoublesMap;
    int nMapEntries = this.textDoublesMap.size();
    if (nMapEntries == 0) {
      throw new IllegalArgumentException("textDoublesMap cannot be empty");
    }
    int nValues = textDoublesMap.entrySet().iterator().next().getValue().length;
    this.meanValues = new double[nValues];
    for (double[] values : textDoublesMap.values()) {
      for (int i = 0; i < values.length; ++i) {
        this.meanValues[i] += values[i];
      }
    }
    for (int i = 0; i < this.meanValues.length; ++i) {
      this.meanValues[i] /= nMapEntries;
    }
  }

  @Override
  public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
	  ArrayList<Feature> features = new ArrayList<Feature>();
	  if (annotation instanceof  WordToken){
		  double[] values = this.textDoublesMap.get(annotation.getCoveredText().toLowerCase());
		  if (values == null) {
			  values = this.meanValues;
		  }

		  for (int i = 0; i < values.length; ++i) {
			  String featureName = Feature.createName(this.name, String.valueOf(i));
			  features.add(new Feature(featureName, values[i]));
		  }
	  }
	  return features;
  }

}
