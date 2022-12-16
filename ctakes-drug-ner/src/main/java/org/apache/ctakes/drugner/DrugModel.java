/**
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
/**
 * Contains information from a single document - is not the result of data mining.
 * 
 */
package org.apache.ctakes.drugner;

public interface DrugModel {

	
	public String getPrimaryAssociatedCodeElement();
	public String getSecondaryAssociatedCodeElement();
	public String getStartDateElement();
	public String getEndDateElement();
	public String getDosageElement();
	public String getFrequencyElement();
	public String getFrequencyUnitElement();
	public String getFormElement();
	public String getDurationElement();
	public String getRouteElement();
	public String getDrugChangeStatusElement();
	public String getStrengthElement();
	public String getStrengthUnitElement();
	public double getConfidenceScoreElement();

	
	public void setPrimaryAssociatedCodeElement(String name, int beginOffset, int endOffset);
	public void setSecondaryAssociatedCodeElement(String name, int beginOffset, int endOffset);
	public void setFormElement(String name, int beginOffset, int endOffset);
	public void setStartDateElement(String name, int beginOffset, int endOffset);
	public void setEndDateElement(String name, int beginOffset, int endOffset);
	public void setDosageElement(String name, int beginOffset, int endOffset);
	public void setFrequencyElement(String name, int beginOffset, int endOffset);
	public void setFrequencyUnitElement(String name, int beginOffset, int endOffset);
	public void setDurationElement(String name, int beginOffset, int endOffset);
	public void setRouteElement(String name, int beginOffset, int endOffset);
	public void setStrengthElement(String name, int beginOffset, int endOffset);
	public void setStrengthUnitElement(String name, int beginOffset, int endOffset);
	public void setDrugChangeStatusElement(String name, int beginOffset, int endOffset);
	public void setConfidenceScoreElement(double score, int beginOffset, int endOffset);
	

}

