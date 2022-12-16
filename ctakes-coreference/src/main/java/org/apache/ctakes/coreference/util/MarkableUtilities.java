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
package org.apache.ctakes.coreference.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

public class MarkableUtilities {
  /**
   * 
   * @param jCas
   * @return Mapping from all Markables in the CAS to UMLS IdentifiedAnnotations that share the same dependency head.
   * Coreference takes place over Markables which may include IdentifiedAnnotations as well as pronouns. So we 
   * get the head token for every Markable, then find all the IdentifiedAnnotations that cover that head, then
   * filter to those are UMLS semantic group types and whose dependency head is the same as the Markable.
   */
  public static Map<Markable,List<IdentifiedAnnotation>> indexCoveringUmlsAnnotations(JCas jCas){
    Map<Markable,List<IdentifiedAnnotation>> map = new HashMap<>();

    Map<ConllDependencyNode, Collection<IdentifiedAnnotation>> dep2event = JCasUtil.indexCovering(jCas, ConllDependencyNode.class, IdentifiedAnnotation.class);

    for(CollectionTextRelation cluster : JCasUtil.select(jCas, CollectionTextRelation.class)){
      List<Markable> memberList = new ArrayList<>(JCasUtil.select(cluster.getMembers(), Markable.class));
      for(Markable member : memberList){
        map.put(member, new ArrayList<>());
        ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jCas, member);

        for(IdentifiedAnnotation covering : dep2event.get(head)){
          if(isUmlsAnnotation(covering) && head == DependencyUtility.getNominalHeadNode(jCas, covering)){
            map.get(member).add(covering);
          }
        }
      }
    }
    return map;
  }

  private static boolean isUmlsEvent(IdentifiedAnnotation a){
    return a instanceof DiseaseDisorderMention || a instanceof SignSymptomMention || a instanceof ProcedureMention || a instanceof MedicationMention;
  }

  private static boolean isUmlsAnnotation(IdentifiedAnnotation a){
    return isUmlsEvent(a) || a instanceof AnatomicalSiteMention;
  }

}
