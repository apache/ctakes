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
package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.CleartkSequenceAnnotator;

public abstract class TemporalSequenceAnnotator_ImplBase extends
    CleartkSequenceAnnotator<String> {
  
  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
      if (!THYMEData.SEGMENTS_TO_SKIP.contains(segment.getId())) {
        this.process(jCas, segment);
      }
    }
  }

  public abstract void process(JCas jCas, Segment segment) throws AnalysisEngineProcessException;

}
