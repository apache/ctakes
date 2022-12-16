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
package org.apache.ctakes.temporal.utils;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

public class PatientViewsUtil {

  public static final String GOLD_PREFIX = "GoldView";
  public static final String VIEW_PREFIX = "DocView";
  public static final String URI_PREFIX = "UriView";
  public static final String NUM_DOCS_NAME = "NumDocsView";
  
  public static String getViewName(int i){
    return String.join("_", VIEW_PREFIX, String.valueOf(i));
  }

  public static String getGoldViewName(int i) {
    return String.join("_", GOLD_PREFIX, String.valueOf(i));
  }
  
  public static String getUriViewName(int i){
    return String.join("_", URI_PREFIX, String.valueOf(i));
  }
  
  public static String getNumDocsViewName() {
    return NUM_DOCS_NAME;
  }

  public static boolean isGoldView(JCas jcas){
    return jcas.getViewName().startsWith(GOLD_PREFIX);
  }

  public static boolean isSameDocument(Annotation a1, Annotation a2){
    return a1.getView().equals(a2.getView());
  }

  public static JCas getPreviousDocumentCas(JCas jcas){
    String name = jcas.getViewName();
    if(name.startsWith(VIEW_PREFIX)){
      int docNum = Integer.parseInt(name.split("_")[1]);
      if(docNum == 0) return null;
      try {
        return jcas.getView(getViewName(docNum-1));
      } catch (CASException e) {
        e.printStackTrace();
        System.err.println("Error: Could not find CAS for previous document for document number: " + docNum);
        return null;
      }
    }
    return null;
  }
  public static String getAnnotatorName(String rawName, int viewNum){
    return String.join("_", rawName, "DocumentIndex", String.valueOf(viewNum));
  }
  
  public AnalysisEngineDescription getRenamedPipelineForDoc(AggregateBuilder builder, int i) throws ResourceInitializationException{
    AnalysisEngineDescription aed = builder.createAggregateDescription();
    
    return aed;
  }
}
