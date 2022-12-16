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

import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

public class FedaFeatureFunction implements FeatureFunction {

	  public static final String DOMAIN_ADAPTATION_ALGORITHM = "FEDA";
	  List<String> domainIds;
	  String currentDomain;
	  
	  public FedaFeatureFunction ( List<String> domains ) {
		  domainIds = domains;
	  }
	  
	  /**
	   * @return replicate the feature for the current domain, the original is a "general" domain
	   */
	  @Override
	  public List<Feature> apply(Feature feature) {
	    Object featureValue = feature.getValue();
	    
	    List<Feature> fedaFeatures = new ArrayList<Feature>();  
	    fedaFeatures.add(feature);
	    if (null==currentDomain) { return fedaFeatures; }
	    
//	    for (String domain : domainIds) {
//		    String featureName = Feature.createName(domain, DOMAIN_ADAPTATION_ALGORITHM, feature.getName());
	    String featureName = Feature.createName(currentDomain, DOMAIN_ADAPTATION_ALGORITHM, feature.getName());
	    
	    fedaFeatures.add(
	    		new Feature(
	    				featureName,
	    				featureValue.toString() )
	    		);
//	    }
	    return fedaFeatures;
	  }

	  public void setDomain(String domain) {
		  currentDomain = domain;
	  }

}
