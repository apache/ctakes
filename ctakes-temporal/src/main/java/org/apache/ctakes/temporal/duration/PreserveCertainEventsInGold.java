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
package org.apache.ctakes.temporal.duration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Preserve only those events that have duration data.
 */
public class PreserveCertainEventsInGold extends JCasAnnotator_ImplBase {                                               
  
  public static final String GOLD_VIEW_NAME = "GoldView";

  @Override                                                                                                                  
  public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     

    File durationLookup = new File(Utils.durationDistributionPath);                      
    Map<String, Map<String, Float>> textToDistribution = null;                                                                 
    try {                                                                                                                      
      textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Utils.Callback());                                    
    } catch(IOException e) {                                                                                                   
      e.printStackTrace();                                                                                                     
      return;                                                                                                                  
    }  
    
    JCas goldView;                                                                                                           
    try {                                                                                                                    
      goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
    } catch (CASException e) {                                                                                               
      throw new AnalysisEngineProcessException(e);                                                                           
    }                                                                                                                                                                                                                                         
    
    for(EventMention mention : Lists.newArrayList(JCasUtil.select(goldView, EventMention.class))) {
      if(textToDistribution.containsKey(mention.getCoveredText().toLowerCase())) {
        // these are the kind we keep
        continue;
      } 
      mention.removeFromIndexes();
    }
  }                                                                                                                          
}                                                                                                                            