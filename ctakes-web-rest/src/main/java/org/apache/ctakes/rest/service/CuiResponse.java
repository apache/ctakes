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

import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;

/**
 * Created by tmill on 12/20/18.
 */
public class CuiResponse {
    // TODO - We don't seem to be returning relations (etc.) anywhere, so why does the war contain so many modules?
    // TODO - consider having multiple return types available.  see ctakes-tiny-rest service/response/* and
    //  service/TinyController.
    // TODO - consider an additional json such as UmlsJsonFormatter.
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
            final IdentifiedAnnotation ia = (IdentifiedAnnotation) annotation;
            polarity = ia.getPolarity();
            final Collection<OntologyConcept> concepts
                  = OntologyConceptUtil.getOntologyConcepts( ia );
            for ( OntologyConcept concept : concepts ) {
                final Map<String,String> attributes = new HashMap<>();
                attributes.put( "codingScheme", concept.getCodingScheme() );
                attributes.put( "code", concept.getCode() == null ? "n/a" : concept.getCode() );
                if ( concept instanceof UmlsConcept ) {
                    attributes.put( "cui", ( (UmlsConcept) concept ).getCui() );
                    attributes.put("tui", ( (UmlsConcept) concept ).getTui() );
                }
                conceptAttributes.add( attributes );
            }
        }
    }

    final public String getType() {
        return _type;
    }

}
