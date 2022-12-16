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
package org.apache.ctakes.assertion.medfacts.cleartk;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.cr.XReader;

public class AssertionSampleFeatureGenerator
{
	public AssertionSampleFeatureGenerator()
	{
	}
	
	public void main(String args[]) throws ResourceInitializationException
	{
		AssertionSampleFeatureGenerator generator =
			new AssertionSampleFeatureGenerator();
		
		generator.execute();
	}

	public void execute() throws ResourceInitializationException
	{
		String filename = "/work/medfacts/sharp/data/2012-10-16_full_data_set_updated/Seed_Corpus/clean_dirs/splits/official/train";
		
	    CollectionReader reader = CollectionReaderFactory.createReader(
		        XReader.class,
		        XReader.PARAM_ROOT_FILE,
		        filename,
		        XReader.PARAM_XML_SCHEME,
		        XReader.XMI);
	    
		    
		
//	    entityFeatureExtractors = Arrays.asList(
//	            new CoveredTextExtractor(),
//	            //new TypePathExtractor(IdentifiedAnnotation.class, "stem"),
//	            new ProliferatingExtractor(
//	                new SpannedTextExtractor(),
//	                new LowerCaseProliferator(),    
//	                new CapitalTypeProliferator(),
//	                new NumericTypeProliferator(),
//	                new CharacterNGramProliferator(fromRight, 0, 2),
//	                new CharacterNGramProliferator(fromRight, 0, 3)));
		
	}

}
