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
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;
//
import java.util.ArrayList;
//import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class SurroundingExtractor implements FeatureExtractor1
{
//	protected static final Logger logger = Logger.getLogger(SurroundingExtractor.class);
//	
////	Class<? extends Annotation> ancestorAnnotationClass;
////	
////	public SurroundingExtractor(Class<? extends Annotation> ancestorAnnotationClass)
////	{
////		this.ancestorAnnotationClass = ancestorAnnotationClass;
////	}
//
	@Override
	public List<Feature> extract(JCas jcas, Annotation targetAnnotation)
			throws CleartkExtractorException
	{
//		logger.info("SurroundingExtractor.extract() BEGIN");
//		//JCasUtil.indexCovering(jcas, IdentifiedAnnotation.class, this.ancestorAnnotationClass)
//		
//		Map<IdentifiedAnnotation, Collection<Zone>> coveringMap =
//			JCasUtil.indexCovering(jcas, IdentifiedAnnotation.class, Zone.class);
//		
//		IdentifiedAnnotation targetEntityAnnotation = (IdentifiedAnnotation)targetAnnotation;
//		
//		Collection<Zone> zoneList = coveringMap.get(targetEntityAnnotation);
//		
//		if (zoneList == null || zoneList.isEmpty())
//		{
//			//return null;
//			logger.info("SurroundingExtractor.extract() early END (no zones)");
//			new ArrayList<Feature>();
//		}
//		
		ArrayList<Feature> featureList = new ArrayList<Feature>();
//		for (Zone zone : zoneList)
//		{
//			Feature currentFeature = new Feature("zone", zone.getLabel());
//			logger.info(String.format("zone: %s", zone.getLabel()));
//			logger.info(String.format("zone feature: %s", currentFeature.toString()));
//			featureList.add(currentFeature);
//		}
//		
//		logger.debug("SurroundingExtractor.extract() END");
		return featureList;
	}
//
}
