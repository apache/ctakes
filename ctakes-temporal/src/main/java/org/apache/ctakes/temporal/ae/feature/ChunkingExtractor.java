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

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.chunking.Chunking;

import com.google.common.collect.Lists;

public class ChunkingExtractor {

  private String name;

  private List<?> subChunkLabels;

  public <SUB_CHUNK_TYPE extends Annotation, CHUNK_TYPE extends Annotation> ChunkingExtractor(
      String name,
      Chunking<?, SUB_CHUNK_TYPE, CHUNK_TYPE> chunking,
      JCas jCas,
      List<SUB_CHUNK_TYPE> subChunks,
      List<CHUNK_TYPE> chunks) throws AnalysisEngineProcessException {
    this.name = name;
    this.subChunkLabels = chunking.createOutcomes(jCas, subChunks, chunks);
  }

  public List<Feature> extract(int tokenIndex, int nBefore, int nAfter) {
    List<Feature> features = Lists.newArrayList();
    int begin = Math.max(tokenIndex - nBefore, 0);
    int end = Math.min(tokenIndex + nAfter + 1, this.subChunkLabels.size());
    for (int i = begin; i < end; ++i) {
      String featureName = String.format("%s_%d", this.name, i - begin - nBefore);
      features.add(new Feature(featureName, this.subChunkLabels.get(i)));
    }
    return features;
  }

}
