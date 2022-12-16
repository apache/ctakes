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
package org.apache.ctakes.relationextractor.ae;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.NamedEntityFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.junit.Test;

public class NamedEntityFeaturesExtractorTest {

  @Test
  public void test() throws Exception {
    // create and populate a JCas with some EntityMention objects
    JCas jCas = JCasFactory.createJCas();
    jCas.setDocumentText("aaa bbb ccc ddd");
    EntityMention e1 = new EntityMention(jCas, 0, 3);
    e1.setTypeID(42);
    e1.addToIndexes();
    EntityMention e2 = new EntityMention(jCas, 8, 11);
    e2.setTypeID(1);
    e2.addToIndexes();
    EntityMention between = new EntityMention(jCas, 4, 7);
    between.addToIndexes();
    
    // run the feature extractor over the JCas
    NamedEntityFeaturesExtractor extractor = new NamedEntityFeaturesExtractor();
    List<Feature> features = extractor.extract(jCas, e1, e2);
    
    // make sure that the features that we expect are there
    assertTrue(features.contains(new Feature("mention1_TypeID", "42")));
    assertTrue(features.contains(new Feature("mention2_TypeID", "1")));
    assertTrue(features.contains(new Feature("Distance_EntityMention", 1)));
    assertTrue(features.contains(new Feature("type1type2", "42_1")));
    assertTrue(features.contains(new Feature("mention1InMention2", false)));
    assertTrue(features.contains(new Feature("mention2InMention1", false)));
  }
}
