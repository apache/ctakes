/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.rest.service;

import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tmill on 12/20/18.
 */
public class CuiResponse {
    final private String _type;
    public int begin;
    public int end;
    public String text;
    public int polarity;
    public List<Map<String,String>> conceptAttributes = new ArrayList<>();

    public CuiResponse(Annotation annotation){
        _type = annotation.getClass().getSimpleName();
        begin = annotation.getBegin();
        end = annotation.getEnd();
        text = annotation.getCoveredText();

        if(annotation instanceof IdentifiedAnnotation) {
            IdentifiedAnnotation ia = (IdentifiedAnnotation) annotation;
            polarity = ia.getPolarity();
            if(ia.getOntologyConceptArr() != null) {
                for (UmlsConcept concept : JCasUtil.select(ia.getOntologyConceptArr(), UmlsConcept.class)) {
                    Map<String, String> atts = new HashMap<>();
                    atts.put("codingScheme", concept.getCodingScheme());
                    atts.put("cui", concept.getCui());
                    atts.put("code", concept.getCode() == null ? "n/a" : concept.getCode());
                    atts.put("tui", concept.getTui());
                    conceptAttributes.add(atts);
                }
            }
        }
    }

    final public String getType() {
        return _type;
    }

}
