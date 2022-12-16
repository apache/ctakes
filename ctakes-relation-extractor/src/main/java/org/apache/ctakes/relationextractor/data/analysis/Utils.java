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
package org.apache.ctakes.relationextractor.data.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.cr.XMIReader;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * Various useful classes and methods.
 */
public class Utils {
  
  public static final String embeddingPath = "/Users/dima/Boston/Vectors/Models/ties-plus-oov.txt";
  
  /**
   * Instantiate an XMI collection reader.
   */
  public static CollectionReader getCollectionReader(File inputDirectory) throws Exception {

    List<String> fileNames = new ArrayList<>();
    for(File file : inputDirectory.listFiles()) {
      if(! (file.isHidden())) {
        fileNames.add(file.getPath());
      }
    }

    String[] paths = new String[fileNames.size()];
    fileNames.toArray(paths);

    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
  
  /**
   * Given an annotation, retrieve its last word.
   */
  public static String getLastWord(JCas systemView, Annotation annotation) {
    
    List<WordToken> tokens = JCasUtil.selectCovered(systemView, WordToken.class, annotation);
    if(tokens.size() == 0) {
          return annotation.getCoveredText();
    }
    
    WordToken lastToken = tokens.get(tokens.size() - 1);
    return lastToken.getCoveredText();
  }
  
  /**
   * Read word embeddings from file.
   */
  public static class Callback implements LineProcessor <Map<String, List<Double>>> {
    
    private Map<String, List<Double>> wordToVector;
    
    public Callback() {
      wordToVector = new HashMap<>();
    }
    
    public boolean processLine(String line) throws IOException {
      
      String[] elements = line.split(" "); // e.g. skin -0.024690 0.108761 0.038441 -0.088759 ...
      List<Double> vector = new ArrayList<>();
      
      for(int dimension = 1; dimension < elements.length; dimension++) {
        vector.add(Double.parseDouble(elements[dimension]));
      }
      
      wordToVector.put(elements[0], vector);
      return true;
    }
    
    public Map<String, List<Double>> getResult() {
      
      return wordToVector;
    }
  }
  
  public static void main(String[] args) throws IOException {
    
    File word2vec = new File(embeddingPath);
    Map<String, List<Double>> data = Files.readLines(word2vec, Charsets.UTF_8, new Callback());
    System.out.println(data.get("skin"));
    System.out.println(data.get("oov"));
  }
}
