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
package org.apache.ctakes.relationextractor.ae.features;

import java.util.List;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

/**
 * Define an interface for people to implement feature extractors.
 */
public interface RelationFeaturesExtractor<T1,T2> {

  /**
   * Extract features for the pair of named entity mentions.
   * 
   * @param jCas
   *          The JCas containing the two named entity mentions.
   * @param arg1
   *          The first identified annotation in the text.
   * @param arg2
   *          The second identified annotation in the text.
   * @return A list of features indicative of the relation between the named entities
   */
  public List<Feature> extract(JCas jCas, T1 arg1, T2 arg2)
      throws AnalysisEngineProcessException;
}