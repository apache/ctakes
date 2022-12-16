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

import java.util.List;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

/**
 * Preserve only those event-event relations whose both arguments have covering system event mentions.
 */
public class PreserveUMLSEventEventRelationsInGold extends JCasAnnotator_ImplBase {                                               
  
  public static final String GOLD_VIEW_NAME = "GoldView";

  @Override                                                                                                                  
  public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     
    
    JCas goldView;                                                                                                           
    try {                                                                                                                    
      goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
    } catch (CASException e) {                                                                                               
      throw new AnalysisEngineProcessException(e);                                                                           
    }                                                                                                                                                                                                                                         

    JCas systemView;
    try {
      systemView = jCas.getView("_InitialView");
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }
    
    for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {            
      RelationArgument arg1 = relation.getArg1();                                                                             
      RelationArgument arg2 = relation.getArg2(); 

      String event2Text;
      String event1Text;
      if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof EventMention) {
        event1Text = arg1.getArgument().getCoveredText().toLowerCase();
        event2Text = arg2.getArgument().getCoveredText().toLowerCase();
      } else {
        // this is not an event-event relation
        continue;
      }
      
      List<EventMention> coveringSystemEventMentions1 = JCasUtil.selectCovered(
          systemView, 
          EventMention.class, 
          arg1.getArgument().getBegin(), 
          arg1.getArgument().getEnd());

      List<EventMention> coveringSystemEventMentions2 = JCasUtil.selectCovered(
          systemView, 
          EventMention.class, 
          arg2.getArgument().getBegin(), 
          arg2.getArgument().getEnd());
      
      if(coveringSystemEventMentions1.size() > 0 && coveringSystemEventMentions2.size() > 0) {
        // keep this relation instance
        System.out.println("keeping: " + event1Text + "-" + event2Text);
        continue;
      }
      
      System.out.println("removing: " + event1Text + "-" + event2Text);
      arg1.removeFromIndexes();                                                                                            
      arg2.removeFromIndexes();                                                                                            
      relation.removeFromIndexes();
    }
    
    for(EventMention mention : Lists.newArrayList(JCasUtil.select(goldView, EventMention.class))) {
      List<EventMention> coveringSystemEventMentions = JCasUtil.selectCovered(
          systemView, 
          EventMention.class, 
          mention.getBegin(), 
          mention.getEnd());
      
      if(coveringSystemEventMentions.size() > 0) {
        // these are the kind we keep
        continue;
      } 
      
      mention.removeFromIndexes();
    }
  }                                                                                                                          
}                                                                                                                            