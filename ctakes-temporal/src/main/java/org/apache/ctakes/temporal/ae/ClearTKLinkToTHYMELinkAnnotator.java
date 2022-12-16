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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.timeml.type.Anchor;
import org.cleartk.timeml.type.TemporalLink;

import java.util.HashSet;

@PipeBitInfo(
      name = "ClearTK Thyme Linker",
      description = "Maps old THYME project relations to a binary text relation representation.",
      products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class ClearTKLinkToTHYMELinkAnnotator extends JCasAnnotator_ImplBase {

  static HashSet<String> ctkRels = new HashSet<String>();
  
  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for(TemporalLink link : JCasUtil.select(jCas, TemporalLink.class)){
      BinaryTextRelation rel = new BinaryTextRelation(jCas);
      RelationArgument arg1 = new RelationArgument(jCas);
      Anchor source = link.getSource();
      arg1.setArgument(new Annotation(jCas, source.getBegin(), source.getEnd()));
      arg1.addToIndexes();
      
      RelationArgument arg2 = new RelationArgument(jCas);
      Anchor target = link.getTarget();
      arg2.setArgument(new Annotation(jCas, target.getBegin(), target.getEnd()));
      arg2.addToIndexes();
      
      String cat = getMappedCategory(link.getRelationType());
      if(cat.endsWith("-1")){
        rel.setArg1(arg2);
        rel.setArg2(arg1);
        rel.setCategory(cat.substring(0, cat.length()-2));
      }else{
        rel.setCategory(getMappedCategory(link.getRelationType()));
        rel.setArg1(arg1);
        rel.setArg2(arg2);
      }
      rel.addToIndexes();
    }
  }

  public static AnalysisEngineDescription getAnnotatorDescription() throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(ClearTKLinkToTHYMELinkAnnotator.class);
  }

  private static String getMappedCategory(String cleartkCat){
    if(!ctkRels.contains(cleartkCat)){
      System.err.println("New relation: " + cleartkCat);
      ctkRels.add(cleartkCat);
    }
    
    if(cleartkCat.equals("AFTER")){
      return "BEFORE-1";
    }else if(cleartkCat.equals("INCLUDES")){
      return "CONTAINS";
    }else if(cleartkCat.equals("IS_INCLUDED")){
      return "CONTAINS-1";
    }else{
      return cleartkCat;
    }
  }
}
