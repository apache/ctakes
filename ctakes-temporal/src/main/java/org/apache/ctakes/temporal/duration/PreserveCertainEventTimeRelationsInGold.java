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
import java.util.HashSet;
import java.util.Map;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Preserve only those event-time relations whose event argument has duration data
 * and whose time argument can be normalized using Steve's timex normalizer.
 */
public class PreserveCertainEventTimeRelationsInGold extends JCasAnnotator_ImplBase {                                               
  
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
    
    // remove relations where one or both arguments have no duration data
    for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {            
      RelationArgument arg1 = relation.getArg1();                                                                             
      RelationArgument arg2 = relation.getArg2(); 
      
      String eventText;
      String timeText;
      if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention) {
        timeText = arg1.getArgument().getCoveredText().toLowerCase(); 
        eventText = Utils.normalizeEventText(jCas, arg2.getArgument());
      } else if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention) {
        eventText = Utils.normalizeEventText(jCas, arg1.getArgument());
        timeText = arg2.getArgument().getCoveredText().toLowerCase();  
      } else {
        // this is not a event-time relation
        continue;
      }    

      HashSet<String> timeUnits = Utils.getTimeUnits(timeText);
      if(textToDistribution.containsKey(eventText) && timeUnits.size() > 0) {
        // there is duration information and we are able to get time units, so keep this
        continue;
      }
      
      arg1.removeFromIndexes();                                                                                            
      arg2.removeFromIndexes();                                                                                            
      relation.removeFromIndexes();
    }
    
    // remove events (that didn't participate in relations) that have no data
    for(EventMention mention : Lists.newArrayList(JCasUtil.select(goldView, EventMention.class))) {
      String eventText = Utils.normalizeEventText(jCas, mention);
      if(textToDistribution.containsKey(eventText)) {
        // these are the kind we keep
        continue;
      } 
      mention.removeFromIndexes();
    }
    
    // finally remove time expressions (that didn't participate in relations) that have no data
    for(TimeMention mention : Lists.newArrayList(JCasUtil.select(goldView, TimeMention.class))) {
      HashSet<String> timeUnits = Utils.getTimeUnits(mention.getCoveredText().toLowerCase());
      if(timeUnits.size() > 0) {
        continue;
      }
      mention.removeFromIndexes();
    }
  }                                                                                                                          
}                                                                                                                            