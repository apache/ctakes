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

import java.util.Collection;
import java.util.List;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.Predicate;
import org.apache.ctakes.typesystem.type.textsem.SemanticArgument;
import org.apache.ctakes.typesystem.type.textsem.SemanticRoleRelation;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class PredicateArgumentExtractor {

  private Multimap<BaseToken, Predicate> tokenPredicateMap;

  private Multimap<BaseToken, SemanticArgument> tokenArgumentMap;

  public PredicateArgumentExtractor(JCas jCas) {
    this.tokenPredicateMap = ArrayListMultimap.create();
    for (Predicate predicate : JCasUtil.select(jCas, Predicate.class)) {
      for (BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, predicate)) {
        this.tokenPredicateMap.put(token, predicate);
      }
    }
    this.tokenArgumentMap = ArrayListMultimap.create();
    for (SemanticArgument argument : JCasUtil.select(jCas, SemanticArgument.class)) {
      for (BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, argument)) {
        this.tokenArgumentMap.put(token, argument);
      }
    }
  }

  public List<Feature> extract(BaseToken token) {
    List<Feature> features = Lists.newArrayList();
    Collection<Predicate> predicates = this.tokenPredicateMap.get(token);
    features.add(new Feature("Predicate", !predicates.isEmpty()));
    for (Predicate predicate : predicates) {
      features.add(new Feature("Predicate_Lex", predicate.getCoveredText()));
    }
    for (SemanticArgument argument : this.tokenArgumentMap.get(token)) {
      SemanticRoleRelation relation = argument.getRelation();
      String category = relation.getCategory();
      features.add(new Feature("Argument", category));
      String predicateText = relation.getPredicate().getCoveredText();
      features.add(new Feature("Argument_Lex", String.format("%s_%s", category, predicateText)));
    }
    return features;
  }
}
