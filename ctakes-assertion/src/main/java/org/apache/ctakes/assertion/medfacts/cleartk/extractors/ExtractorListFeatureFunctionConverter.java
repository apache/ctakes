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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.function.FeatureFunction;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;

public class ExtractorListFeatureFunctionConverter {
	public static <T extends Annotation> List<FeatureFunctionExtractor<T>> convert( List<? extends FeatureExtractor1<T>> extractors, FeatureFunction ff ) {

		List<FeatureFunctionExtractor<T>> featureFunctionExtractors = new ArrayList<>();
		if (null!=extractors) {
			for (FeatureExtractor1<T> extractor : extractors) {
				featureFunctionExtractors.add(
						new FeatureFunctionExtractor<>(extractor,ff)
						);
			}
		}
		
		return featureFunctionExtractors;
	}

}
