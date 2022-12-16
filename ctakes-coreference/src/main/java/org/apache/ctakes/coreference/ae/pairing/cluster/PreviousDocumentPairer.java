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
package org.apache.ctakes.coreference.ae.pairing.cluster;

import org.apache.ctakes.coreference.util.ClusterMentionFetcher;
import org.apache.ctakes.coreference.util.ThymeCasOrderer;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by tmill on 9/21/17.
 */
public class PreviousDocumentPairer extends CrossDocumentPairer_ImplBase {
    @Override
    public List<ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair> getPairs(JCas jcas, Markable m, JCas prevCas) {
        List<ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair> clusters = new ArrayList<>();
        if(prevCas == null) return clusters;

        for(CollectionTextRelation chain : JCasUtil.select(prevCas, CollectionTextRelation.class)){
            Collection<Markable> members = JCasUtil.select(chain.getMembers(), Markable.class);
            if(members.size() > 1) {
                clusters.add(new ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair(chain, m));
            }else{
                Markable singleton = members.iterator().next();
                if(singleton.getCoveredText().contains(m.getCoveredText()) ||
                        m.getCoveredText().contains(singleton.getCoveredText())){
                    clusters.add(new ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair(chain, m));
                }
            }
        }
        return clusters;
    }
}
