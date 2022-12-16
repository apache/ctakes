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
package org.apache.ctakes.relationextractor.pipelines;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;

/**
 * This is a sample relation annotation consumer. 
 * Currently it displays the relation annotations.
 * 
 * @author dmitriy dligach
 *
 */
public class RelationExtractorConsumer extends JCasAnnotator_ImplBase {

	// TODO: turn these into configuration parameters
	public final boolean displayEntities = false;
	public final boolean displayContext = false;
	
	@Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {

    JCas systemView;
    try {
      systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }	  
    
    if(displayEntities) {
    	System.out.println();
    	for(IdentifiedAnnotation identifiedAnnotation : JCasUtil.select(systemView, IdentifiedAnnotation.class)) {
    		String text = identifiedAnnotation.getCoveredText();
    		int type = identifiedAnnotation.getTypeID();
    		System.out.format("%s/%d\n", text, type);
    	}
    	System.out.println();
    }

    // print relations
    System.out.println();
    for(BinaryTextRelation binaryTextRelation : JCasUtil.select(systemView, BinaryTextRelation.class)) {
    	    	
    	String category = binaryTextRelation.getCategory();
    	
    	IdentifiedAnnotation entity1; // entity whose role is "Argument"
    	IdentifiedAnnotation entity2; // entity whose role is "Related_to"
    	
    	if(binaryTextRelation.getArg1().getRole().equals("Argument")) {
    		entity1 = (IdentifiedAnnotation) binaryTextRelation.getArg1().getArgument();
    		entity2 = (IdentifiedAnnotation) binaryTextRelation.getArg2().getArgument();
    	} else {
    		entity1 = (IdentifiedAnnotation) binaryTextRelation.getArg2().getArgument();
    		entity2 = (IdentifiedAnnotation) binaryTextRelation.getArg1().getArgument();
    	}
    	
    	String arg1 = entity1.getCoveredText();
    	String arg2 = entity2.getCoveredText();
    	
    	int type1 = entity1.getTypeID();
    	int type2 = entity2.getTypeID();
    	
    	// print relation and its arguments: location_of(colon/6, colon cancer/2)
    	System.out.format("%s(%s/%d, %s/%d)\n", category, arg1, type1, arg2, type2);

    	if(displayContext) {
    		List<Sentence> list = JCasUtil.selectCovering(jCas, Sentence.class, entity1.getBegin(), entity1.getEnd());
    		
    		// print the sentence containing this instance
    		for(Sentence s : list) {
    			System.out.println(s.getCoveredText());
    		}
    		System.out.println();
    	}
    }
  }
}
