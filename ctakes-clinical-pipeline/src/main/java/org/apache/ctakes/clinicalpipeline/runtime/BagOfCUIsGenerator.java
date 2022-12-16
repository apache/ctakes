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
package org.apache.ctakes.clinicalpipeline.runtime;

import java.io.IOException;
import java.util.HashSet;

import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.cas.FSArray;

public class BagOfCUIsGenerator extends BagOfAnnotationsGenerator<IdentifiedAnnotation, String> {

	public BagOfCUIsGenerator(String inputDir, String outputDir)
			throws UIMAException, IOException {
		super(inputDir, outputDir);
	}

	@Override
	protected String extractInformation(IdentifiedAnnotation t) {
		StringBuilder buff = new StringBuilder();
		
		FSArray mentions = t.getOntologyConceptArr();
		
		HashSet<String> uniqueCuis = new HashSet<String>();
		if(mentions == null) return null;
		for(int i = 0; i < mentions.size(); i++){
			if(mentions.get(i) instanceof UmlsConcept){
				UmlsConcept concept = (UmlsConcept) mentions.get(i);
				uniqueCuis.add(concept.getCui());
			}
		}
		
		for(String cui : uniqueCuis){
			if(t.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT) buff.append("-");
			buff.append(cui);
			buff.append("\n");
		}
		
		if(buff.length() == 0) return null;
		return buff.substring(0,buff.length()-1);
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws UIMAException 
	 */
	public static void main(String[] args) throws UIMAException, IOException {
		(new BagOfCUIsGenerator(args[0], args[1])).process();
	}

}
