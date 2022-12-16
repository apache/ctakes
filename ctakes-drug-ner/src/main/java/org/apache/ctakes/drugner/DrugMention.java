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
package org.apache.ctakes.drugner;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.jcas.JCas;


import org.apache.ctakes.core.fsm.output.DateToken;
import org.apache.ctakes.core.util.FSUtil;
import org.apache.ctakes.drugner.elements.AssociatedPrimaryCodeElement;
import org.apache.ctakes.drugner.elements.ConfidenceScoreElement;
import org.apache.ctakes.drugner.elements.DosageElement;
import org.apache.ctakes.drugner.elements.DrugChangeStatusElement;
import org.apache.ctakes.drugner.elements.DurationElement;
import org.apache.ctakes.drugner.elements.FormElement;
import org.apache.ctakes.drugner.elements.FrequencyElement;
import org.apache.ctakes.drugner.elements.FrequencyUnitElement;
import org.apache.ctakes.drugner.elements.RouteElement;
import org.apache.ctakes.drugner.elements.StartDateElement;
import org.apache.ctakes.drugner.elements.StrengthElement;
import org.apache.ctakes.drugner.elements.StrengthUnitElement;
import org.apache.ctakes.drugner.elements.TextMentionElement;
import org.apache.ctakes.drugner.fsm.output.elements.DrugChangeStatusToken;
import org.apache.ctakes.drugner.fsm.output.elements.FrequencyUnitToken;
import org.apache.ctakes.drugner.fsm.output.elements.RouteToken;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.drugner.type.DosagesAnnotation;
import org.apache.ctakes.drugner.type.DrugChangeStatusAnnotation;
import org.apache.ctakes.drugner.type.DurationAnnotation;
import org.apache.ctakes.drugner.type.FormAnnotation;
import org.apache.ctakes.drugner.type.FrequencyAnnotation;
import org.apache.ctakes.drugner.type.FrequencyUnitAnnotation;
import org.apache.ctakes.drugner.type.RouteAnnotation;
import org.apache.ctakes.drugner.type.StrengthAnnotation;
import org.apache.ctakes.drugner.type.StrengthUnitAnnotation;

/**
 * Contains information from a single document - is not the result of data
 * mining.
 */
public class DrugMention implements DrugModel {

	/**
	 * The word(s) in the note that indicate the drug.
	 */
	public TextMentionElement drugMentionText;

	public AssociatedPrimaryCodeElement associatedCodePrimary;

	/**
	 * terminology unique identifier (e.g. RxNorm identifier). if not in the
	 * terminology, we will use the drug mention as the id.
	 */
	public AssociatedPrimaryCodeElement associatedCodeSecondary;

	/**
	 * If there is a specific date in the note associated with the drug mention,
	 * the specific date. Also time might be capture by more general markers,
	 * e.g. "subsequently".
	 */
	public StartDateElement startDate;

	public DateToken endDate;

	/**
	 * e.g. 5 mg
	 */
	public DosageElement dosage;

	/**
	 * e.g. twice daily
	 */
	public FrequencyElement frequency;

	/**
	 * e.g. twice daily
	 */
	public FrequencyUnitElement frequencyUnit;

	/**
	 * e.g. "for 2 weeks"
	 */
	public DurationElement duration;

	/**
	 * e.g oral Topical Examples include medications applied onto the skin,
	 * asthma medications, enema, eye or ear drops, decongestants, vaginal
	 * creams. Enteral (oral) Most drugs taken by mouth such as tablets or
	 * capsules. Enteral (gastric) Examples include medications given through a
	 * gastric or duodenal tube. Enteral (rectal) Drugs in suppositories or
	 * enema form. Parenteral (intravenous) Parenteral (intra-arterial)
	 * Parenteral (intramuscular) Parenteral (intracardiac) Parenteral
	 * (subcutaneous) Medications such as insulin. Parenteral (intrathecal) Into
	 * the spinal canal. Parenteral (intraperitoneal) Parenteral (transdermal)
	 * Such as patches. Parenteral (transmucosal)
	 */
	public RouteElement route;

	public StrengthElement strength;

	/**
	 * Span the drug form, if available. The spanned mention needs to be mapped
	 * to the standard form available in the dropdown menu. In general don't
	 * infer. However, for a medication described in terms of cc or mL, the
	 * number of cc's or mL's is usually not a strength - for example "5 cc
	 * 0.5%" (or 5 mL 0.5%), indicates a strength of 0.5% -- for those, create a
	 * Form, with "cc" or "mL" spanned, and mapped to liquid, so that we capture
	 * the unit such as cc or mL.
	 */
	public StrengthUnitElement strengthUnit;

	/**
	 * Span the drug form, if available. The spanned mention needs to be mapped
	 * to the standard form available in the dropdown menu. In general don't
	 * infer. However, for a medication described in terms of cc or mL, the
	 * number of cc's or mL's is usually not a strength - for example "5 cc
	 * 0.5%" (or 5 mL 0.5%), indicates a strength of 0.5% -- for those, create a
	 * Form, with "cc" or "mL" spanned, and mapped to liquid, so that we capture
	 * the unit such as cc or mL.
	 */
	public FormElement form;

	/**
	 * aka change. - start: refers to an explicit mention of when the drug was
	 * started - stop: refers to an explicit mention of when the drug was
	 * stopped - increase and decrease: refer to explicit mentions of drug
	 * attribute changes - noChange: nothing is explicitly stated regarding
	 * changes. This will be the default case and does not need to be created.
	 * If there is some mention on the text such as "I told the patient to
	 * continue with his dosage of XXX 300 mg", then there would be textual
	 * evidence for noChange.
	 */
	public DrugChangeStatusElement changeStatus;

	public ConfidenceScoreElement confidence;

	public boolean findMaxValue = true;

	/**
	 * regex pattern to look for, in this case alpha upper and lower characters
	 */
	private static final Pattern lookForAlpha = Pattern.compile("[a-zA-Z]+");

	/**
	 * regex pattern to look for, in this case alpha upper and lower characters
	 */
	private static final Pattern lookForPercent = Pattern.compile("[%]");

	public DrugMention(JCas jcas, int beginPos, int endPos) {
		Iterator drugStatusTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas, DrugChangeStatusAnnotation.type, beginPos, endPos);
		while (drugStatusTokenItr.hasNext()){
			DrugChangeStatusAnnotation focusToken = (DrugChangeStatusAnnotation) drugStatusTokenItr.next();
			String localStatus = null;
			if ((localStatus = findDrugChangeStatusElement(jcas, focusToken
					.getBegin(), focusToken.getEnd())) == null) {
				changeStatus = new DrugChangeStatusElement(focusToken
						.getCoveredText(), focusToken.getBegin(),
						focusToken.getEnd());
			} else {
				setDrugChangeStatusElement(localStatus, focusToken
						.getBegin(), focusToken.getEnd());
			}
		}
		
		if (changeStatus != null 
				&& (changeStatus.getDrugChangeStatus().equals(DrugChangeStatusToken.INCREASEFROM) 
						|| changeStatus.getDrugChangeStatus().equals(DrugChangeStatusToken.DECREASE))) {
			findMaxValue = false;
			
		}
			
		Iterator dateTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				DateAnnotation.type, beginPos, endPos);
		while (dateTokenItr.hasNext()){
			DateAnnotation focusToken = (DateAnnotation) dateTokenItr.next();
			String localDate = null;
			if ((localDate = findStartDateElement(jcas, focusToken.getBegin(),
					focusToken.getEnd())) == null) {

				startDate = new StartDateElement(focusToken.getCoveredText(),
						focusToken.getBegin(), focusToken.getEnd());
			} else {
				setStartDateElement(localDate, focusToken.getBegin(),
						focusToken.getEnd());
			}
		}
			
		Iterator doseTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				DosagesAnnotation.type, beginPos, endPos);
		while (doseTokenItr.hasNext()){
			DosagesAnnotation focusToken = (DosagesAnnotation) doseTokenItr.next();
			String localDose = null;
			if ((localDose = findDosageElement(jcas, focusToken.getBegin(),
					focusToken.getEnd())) == null) {

				dosage = new DosageElement(focusToken.getCoveredText(),
						focusToken.getBegin(), focusToken.getEnd());
			} else {
				setDosageElement(localDose, focusToken.getBegin(),
						focusToken.getEnd());
				
			}
		}
		Iterator frequencyTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				FrequencyAnnotation.type, beginPos, endPos);
		while (frequencyTokenItr.hasNext()){
			FrequencyAnnotation focusToken = (FrequencyAnnotation) frequencyTokenItr.next();
			String localFreq = null;
			if ((localFreq = findFrequencyElement(jcas, focusToken
					.getBegin(), focusToken.getEnd())) == null) {

				frequency = new FrequencyElement(focusToken
						.getCoveredText(), focusToken.getBegin(),
						focusToken.getEnd());
			} else if ((frequency != null && localFreq != null)
					&& (frequency.getFrequencyMention().compareTo("") != 0)
					&& (localFreq.compareTo("") != 0)
					&& (parseIntValue(localFreq) > parseIntValue(frequency
							.getFrequencyMention()) && findMaxValue == true)) {
				// Need a way to find the largest frequency and/or convert
				// it to factor daily dosage

				setFrequencyElement(localFreq, focusToken.getBegin(),
						focusToken.getEnd());
		} else {
			setFrequencyElement(localFreq, focusToken.getBegin(),
					focusToken.getEnd());
		}
		}	
			Iterator frequencyUnitTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
					FrequencyUnitAnnotation.type, beginPos, endPos);
			while (frequencyUnitTokenItr.hasNext()){
				FrequencyUnitAnnotation focusToken = (FrequencyUnitAnnotation) frequencyUnitTokenItr.next();
				String localFreq = null;
				if ((localFreq = findFrequencyUnitElement(jcas,
								focusToken.getBegin(), focusToken.getEnd())) == null) {
					frequencyUnit = new FrequencyUnitElement(focusToken
							.getCoveredText(), focusToken.getBegin(),
							focusToken.getEnd());
				} else if (frequencyUnit == null
						&& findMaxValue == true) {
					// Need a way to find the largest frequency and/or convert
					// it to factor daily dosage
					setFrequencyUnitElement(localFreq, focusToken.getBegin(),
							focusToken.getEnd());
				}
			}
			Iterator strengthUnitTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas, StrengthUnitAnnotation.type, beginPos, endPos);
			while (strengthUnitTokenItr.hasNext()){
				StrengthUnitAnnotation focusToken = (StrengthUnitAnnotation) strengthUnitTokenItr.next();
				setStrengthUnitElement(focusToken.getCoveredText(),
						focusToken.getBegin(), focusToken.getEnd());
			}
			Iterator strengthTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas, StrengthAnnotation.type, beginPos, endPos);
			while (strengthTokenItr.hasNext()){
				StrengthAnnotation focusToken = (StrengthAnnotation) strengthTokenItr.next();
				 

						String localStrength = null;
						if ((localStrength = findStrengthElement(jcas, focusToken
								.getBegin(), focusToken.getEnd())) == null) {
							strength = new StrengthElement(focusToken.getCoveredText(),
									focusToken.getBegin(), focusToken.getEnd());
						} else {
							if (strength != null) {
								// check for range and compare
								int spacePosition = strength.getStrengthMention()
										.indexOf(" ");
								int spacePos = localStrength
								.indexOf(" ");
								if (spacePosition > 0 && spacePos > 0
										&& parseDoubleValue(strength
												.getStrengthMention().substring(0,
														spacePosition)) < parseDoubleValue(localStrength
												.substring(0, localStrength
														.indexOf(" ")))&& findMaxValue == true) {
									setStrengthElement(localStrength, focusToken
											.getBegin(), focusToken.getEnd());
								} else {
									String stringRange = strength.getStrengthMention();
									int hyphPosition = 0;
									if ((stringRange.length() > 0)
											&& (stringRange.indexOf('-') > 0)) {
										hyphPosition = stringRange.indexOf('-');
										Double firstValue = new Double(
												parseDoubleValue(stringRange
														.subSequence(0, hyphPosition)));
										Double secondValue = new Double(
												parseDoubleValue(stringRange
														.substring(hyphPosition + 2)));
										if (firstValue.doubleValue() >= secondValue
												.doubleValue() && findMaxValue == true) {
											setStrengthElement(firstValue.toString(),
													focusToken.getBegin(), focusToken
															.getEnd());
										} else {
											setStrengthElement(firstValue.toString(),
													focusToken.getBegin(), focusToken
															.getEnd());
										}
									}
								}
							} else if ((localStrength.length() > 0)
									&& (localStrength.indexOf('-') > 0)) {

								int hyphPosition = 0;

								hyphPosition = localStrength.indexOf('-');
								Double firstValue = new Double(
										parseDoubleValue(localStrength.subSequence(0,
												hyphPosition)));
								Double secondValue = new Double(
										parseDoubleValue(localStrength
												.substring(hyphPosition + 2)));
								if (firstValue.doubleValue() >= secondValue
										.doubleValue() && findMaxValue == true) {
									setStrengthElement(firstValue.toString(),
											focusToken.getBegin(), focusToken.getEnd());
								} else {
									setStrengthElement(firstValue.toString(),
											focusToken.getBegin(), focusToken.getEnd());
								}

								setStrengthElement(localStrength,
										focusToken.getBegin(), focusToken.getEnd());
							} else {
								setStrengthElement(localStrength,
										focusToken.getBegin(), focusToken.getEnd());
							}
						}
			}
			Iterator formTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas, FormAnnotation.type, beginPos, endPos);
			while (formTokenItr.hasNext()){
				FormAnnotation focusToken = (FormAnnotation) formTokenItr.next();
				String localForm = null;
				if ((localForm = findFormElement(jcas, focusToken.getBegin(),
						focusToken.getEnd())) == null) {
					form = new FormElement(focusToken.getCoveredText(),
							focusToken.getBegin(), focusToken.getEnd());
				} else {
					setFormElement(localForm, focusToken.getBegin(), focusToken
							.getEnd());
				}
			}
			Iterator routeTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas, RouteAnnotation.type, beginPos, endPos);
			while (routeTokenItr.hasNext()){
				RouteAnnotation focusToken = (RouteAnnotation) routeTokenItr.next();
				String localRoute = null;
				if ((localRoute = findRouteElement(jcas, focusToken.getBegin(),
						focusToken.getEnd())) == null) {
					route = new RouteElement(focusToken.getCoveredText(),
							focusToken.getBegin(), focusToken.getEnd());
				} else {
					setRouteElement(localRoute, focusToken.getBegin(),
							focusToken.getEnd());
				}
			}
			Iterator durationTokenItr = FSUtil.getAnnotationsIteratorInSpan(jcas, DurationAnnotation.type, beginPos, endPos);
			while (durationTokenItr.hasNext()){
				DurationAnnotation focusToken = (DurationAnnotation) durationTokenItr.next();
				String localDuration = null;
				if ((localDuration = findDurationElement(jcas, focusToken
						.getBegin(), focusToken.getEnd())) == null) {
					duration = new DurationElement(focusToken.getCoveredText(),
							focusToken.getBegin(), focusToken.getEnd());
				} else {
					setDurationElement(localDuration, focusToken.getBegin(),
							focusToken.getEnd());
				}
			}

			
	

	}

	public void setPrimaryAssociatedCodeElement(String name, int beginOffset,
			int endOffset) {
		// need a method to find most suitable primary cui here
		associatedCodePrimary = new AssociatedPrimaryCodeElement(name, beginOffset,
				endOffset);

	}

	public void setSecondaryAssociatedCodeElement(String name, int beginOffset,
			int endOffset) {

		// need a method to find most suitable secondary cui here
		associatedCodeSecondary = new AssociatedPrimaryCodeElement(name, beginOffset,
				endOffset);
	}

	public void setStartDateElement(String date, int beginOffset, int endOffset) {
         startDate = new StartDateElement(date, beginOffset, endOffset);
	}

	public void setEndDateElement(String name, int beginOffset, int endOffset) {

	}

	public void setDosageElement(String dose, int beginOffset, int endOffset) {
		dosage = new DosageElement(dose, beginOffset, endOffset);

	}

	public void setFrequencyElement(String name, int beginOffset, int endOffset) {

		frequency = new FrequencyElement(name, beginOffset, endOffset);
	}

	public void setFrequencyUnitElement(String name, int beginOffset,
			int endOffset) {

		frequencyUnit = new FrequencyUnitElement(name, beginOffset, endOffset);
	}

	public void setFormElement(String name, int beginOffset, int endOffset) {

		form = new FormElement(name, beginOffset, endOffset);
	}

	public void setDurationElement(String name, int beginOffset, int endOffset) {
		duration = new DurationElement(name, beginOffset, endOffset);
	}

	public void setRouteElement(String name, int beginOffset, int endOffset) {
		route = new RouteElement(name, beginOffset, endOffset);
	}

	public void setDrugChangeStatusElement(String name, int beginOffset,
			int endOffset) {
		changeStatus = new DrugChangeStatusElement(name, beginOffset, endOffset);

	}

	public void setConfidenceScoreElement(double score, int beginOffset,
			int endOffset) {

		confidence = new ConfidenceScoreElement(score, beginOffset, endOffset);
	}

	public void setStrengthElement(String name, int beginOffset, int endOffset) {
		strength = new StrengthElement(name, beginOffset, endOffset);
	}

	public String getStrengthUnitElement() {
		// TODO Auto-generated method stub
		if (strengthUnit != null)
			return strengthUnit.getStrengthMention();
		else
			return null;
	}


	public void setStrengthUnitElement(String name, int beginOffset, int endOffset) {
		// TODO Auto-generated method stub
		strengthUnit = new StrengthUnitElement(name, beginOffset, endOffset);
		
	}
	public String getPrimaryAssociatedCodeElement() {
		if (associatedCodePrimary != null)
			return associatedCodePrimary.getCuiCode();
		else
			return null;
	}

	public String getSecondaryAssociatedCodeElement() {
		if (associatedCodeSecondary != null)
			return associatedCodeSecondary.getCuiCode();
		else
			return null;
	}

	public String getStartDateElement() {
		if (startDate != null)
			return startDate.getDate();
		else return "";
	}

	public String getEndDateElement() {
		return endDate.toString();
	}

	public String getDosageElement() {
		if (dosage != null)
			return dosage.getDosage();
		else
			return "1.0";
	}

	public String getFrequencyElement() {
		if (frequency != null)
			return frequency.getFrequencyMention();
		else
			return "1.0";
	}

	public String getFrequencyUnitElement() {
		if (frequencyUnit != null)
			return frequencyUnit.getFrequencyUnitMention();
		else
			return null;
	}

	public String getFormElement() {
		if (form != null)
			return form.getFormMention();
		else
			return null;
	}

	public String getDurationElement() {
		if (duration != null)
			return duration.getDuration();
		else
			return null;
	}

	public String getRouteElement() {
		if (route != null)
			return route.getRouteMention();
		else
			return null;
	}

	public String getDrugChangeStatusElement() {
		if (changeStatus != null)
			return changeStatus.getDrugChangeStatus();
		else
			return DrugChangeStatusElement.NOCHANGE_STATUS;
	}

	public String getStrengthElement() {
		if (strength != null)
			return strength.getStrengthMention();
		else
			return "";
	}

	public double getConfidenceScoreElement() {
		if (confidence != null)
			return confidence.getConfidenceScoreElement();
		else
			return 0;
	}

	private String findStartDateElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				DateAnnotation.type, beginOffset, endOffset + 1);

		while (firItr.hasNext()) {
			DateAnnotation da = (DateAnnotation) firItr.next();
			if (da.getBegin() == beginOffset)
				return da.getCoveredText();
		}
		return null;
	}

	private String findEndDateElement(JCas jcas, int beginOffset, int endOffset) {
		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas, DateAnnotation.type, beginOffset, endOffset + 1);
		while (firItr.hasNext()) {
			DateAnnotation da = (DateAnnotation) firItr.next();
			if (da.getBegin() == beginOffset)
				return da.getCoveredText();
		}
		return null;
	}

	private String findDosageElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				DosagesAnnotation.type, beginOffset, endOffset + 1);

		while (firItr.hasNext()) {
			DosagesAnnotation da = (DosagesAnnotation) firItr.next();
			if (da.getBegin() == beginOffset) {

				int posSpace = da.getCoveredText().lastIndexOf(' ');
				int posHyph = da.getCoveredText().lastIndexOf('-');

				String lastTerm = da.getCoveredText();
				int ofSpace = da.getCoveredText().indexOf("of");
                if (ofSpace > 0)
                	lastTerm = da.getCoveredText().substring(0, ofSpace).trim();
				if ((lastTerm.compareToIgnoreCase("one-half-tablet") == 0)
						|| (lastTerm.compareToIgnoreCase("one-half") == 0)
						|| (lastTerm.compareToIgnoreCase("1/2") == 0)
						|| (lastTerm.compareToIgnoreCase("half-tablet") == 0)) {
					return "0.5";
				} else if (lastTerm.compareToIgnoreCase("one-and-a-half") == 0) {
					return "1.5";
				} else if ((lastTerm.compareToIgnoreCase("one-quarter") == 0)
					|| (lastTerm.compareToIgnoreCase("one-fourth") == 0)
					|| (lastTerm.compareToIgnoreCase("1/4") == 0)
					|| (lastTerm.compareToIgnoreCase("a-fourth") == 0)) {
					return ".25";
				} else if ((lastTerm.compareToIgnoreCase("one-third") == 0)
						|| (lastTerm.compareToIgnoreCase("thirds") == 0)
						|| (lastTerm.compareToIgnoreCase("1/3") == 0)
						|| (lastTerm.compareToIgnoreCase("a-third") == 0)) {
					return ".33";
				} else {
                    
					if (posSpace > 0)
						lastTerm = lastTerm.substring(posSpace + 1);
					else if (posHyph > 0)
						lastTerm = lastTerm.substring(posHyph + 1);
				}
				return convertFromTextToNum(lastTerm);

			}
		}
		return null;
	}

	private String findFormElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				FormAnnotation.type, beginOffset, endOffset + 1);

		while (firItr.hasNext()) {
			FormAnnotation da = (FormAnnotation) firItr.next();
			if (da.getBegin() == beginOffset) {
				if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.AEROSOL) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("aerosols") == 0))
					return FormElement.AEROSOL;

				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.CREAM) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("creams") == 0))
					return FormElement.CREAM;

				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.ELIXIR) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("elixirs") == 0))
					return FormElement.ELIXIR;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.EMULSION) == 0)
						|| (da.getCoveredText().compareToIgnoreCase(
								"emulutions") == 0))
					return FormElement.EMULSION;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.ENEMA) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("ememas") == 0))
					return FormElement.ENEMA;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.GEL) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("gels") == 0))
					return FormElement.GEL;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.IMPLANT) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("implants") == 0))
					return FormElement.IMPLANT;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.INHALANT) == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("inhalants") == 0))
					return FormElement.INHALANT;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.INJECTION) == 0)
						|| (da.getCoveredText().compareToIgnoreCase(
								"injections") == 0))
					return FormElement.INJECTION;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.LIQUID) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("liquids") == 0))
					return FormElement.LIQUID;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.LOTION) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("lotions") == 0))
					return FormElement.LOTION;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.LOZENGE) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("lozenges") == 0))
					return FormElement.LOZENGE;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.OINTMENT) == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("ointments") == 0))
					return FormElement.OINTMENT;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.PATCH) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("patches") == 0))
					return FormElement.PATCH;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.PILL) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("pills") == 0))
					return FormElement.PILL;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.POWDER) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("powders") == 0))
					return FormElement.POWDER;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.SHAMPOO) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("shampoos") == 0))
					return FormElement.SHAMPOO;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.SOAP) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("soaps") == 0))
					return FormElement.SOAP;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.SOLUTION) == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("solutions") == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("soln") == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("concentrate") == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("concentrat") == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("conc") == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("solu") == 0))
					
					return FormElement.SOLUTION;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.SPRAY) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("sprays") == 0)
						|| (da.getCoveredText().compareToIgnoreCase(
								"nebulizers") == 0)
						|| (da.getCoveredText()
								.compareToIgnoreCase("nebulizer") == 0))
					return FormElement.SPRAY;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.SUPPOSITORY) == 0)
						|| (da.getCoveredText().compareToIgnoreCase(
								"suppositories") == 0))
					return FormElement.SUPPOSITORY;
				else if ((da.getCoveredText().compareToIgnoreCase(FormElement.SYRINGE) == 0 )
						|| (da.getCoveredText().compareToIgnoreCase("syrnge") == 0))
					return FormElement.SYRINGE;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.SYRUP) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("syrups") == 0))
					return FormElement.SYRUP;
				else if ((da.getCoveredText().compareToIgnoreCase(
						FormElement.TABLET) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("tablets") == 0)
// Capsule needs to be handled differently
//						|| (da.getCoveredText().compareToIgnoreCase("capsule") == 0)
//						|| (da.getCoveredText().compareToIgnoreCase("capsules") == 0)
//						|| (da.getCoveredText().compareToIgnoreCase("cap") == 0)
//						|| (da.getCoveredText().compareToIgnoreCase("caps") == 0)
						|| (da.getCoveredText().compareToIgnoreCase("tab") == 0)
						|| (da.getCoveredText().compareToIgnoreCase("tabs") == 0))
					return FormElement.TABLET;
				else if ((da.getCoveredText().compareToIgnoreCase(FormElement.CAPSULE) == 0)
						|| (da.getCoveredText().compareToIgnoreCase("capsule") == 0)
						|| (da.getCoveredText().compareToIgnoreCase("capsules") == 0)
						|| (da.getCoveredText().compareToIgnoreCase("cap") == 0)
						|| (da.getCoveredText().compareToIgnoreCase("caps") == 0))
					return FormElement.CAPSULE;
				return null;
			}
		}
		return null;
	}

	private String findFrequencyElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				FrequencyAnnotation.type, beginOffset, endOffset + 1);
		while (firItr.hasNext()) {
			FrequencyAnnotation da = (FrequencyAnnotation) firItr.next();
			if (da.getBegin() == beginOffset) {
				int posSpace = da.getCoveredText().indexOf(' ');
				int posHyph = da.getCoveredText().indexOf('-');
				int lastPosHyph = da.getCoveredText().lastIndexOf('-');
				String firstTerm = da.getCoveredText();
				String lastTerm = da.getCoveredText();
				if (posSpace > 0)
					firstTerm = da.getCoveredText().substring(0, posSpace);
				else if (posHyph > 0)
					firstTerm = firstTerm.substring(0, posHyph);
				
				if (lastPosHyph > 0){
					lastTerm = lastTerm.substring(lastPosHyph+1, lastTerm.length());
					
					//this.setFrequencyUnitElement(lastTerm, beginOffset+lastPosHyph+1, endOffset);
				}
				int lastSpace = lastTerm.lastIndexOf(' ');
				if (lastSpace > 0)
					lastTerm = lastTerm.substring(0, lastSpace);
				while ((lastSpace = lastTerm.lastIndexOf(' ')) > 1)
					lastTerm = lastTerm.substring(lastSpace+1);
				String returnFirstValue = convertFromTextToNum(firstTerm);
				String returnLastValue = convertFromTextToNum(lastTerm);
				try {
				if (new Double(returnFirstValue).intValue() 
						< new Double(returnLastValue).intValue() && findMaxValue){
					return returnLastValue;
				} 
				else
					return returnFirstValue;
				} 
				catch (NumberFormatException nfe){
					return returnFirstValue;
				}

			}
		}
		return null;
	}

	private String findFrequencyUnitElement(JCas jcas, int beginOffset,
			int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				FrequencyUnitAnnotation.type, beginOffset, endOffset + 1);

		while (firItr.hasNext()) {

			FrequencyUnitAnnotation da = (FrequencyUnitAnnotation) firItr
					.next();
			if (da.getBegin() == beginOffset && da.getPeriod() > holdLargestPeriod) {
				holdLargestPeriod = da.getPeriod();
				int posHyph = da.getCoveredText().lastIndexOf('-');
				String lastTerm = da.getCoveredText();
				int szString = lastTerm.toString().length();
				if (posHyph > 0) {
					lastTerm = lastTerm.substring(posHyph + 1, szString);
				}

				if (da.getPeriod() == FrequencyUnitToken.QUANTITY_ONE) {

					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_TWO) {

					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))) {
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 2.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {

						setFrequencyElement("2.0", beginOffset, endOffset);
					}
					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_THREE) {
					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))){
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 3.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {
						setFrequencyElement("3.0", beginOffset, endOffset);
					}
					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_FOUR) {

					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))) {
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 4.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {
						setFrequencyElement("4.0", beginOffset, endOffset);
					}
					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_FIVE) {
					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))){
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 5.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {
						setFrequencyElement("5.0", beginOffset, endOffset);
					}
					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_SIX) {
					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))) {
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 6.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {
						setFrequencyElement("6.0", beginOffset, endOffset);

					}
					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_SEVEN) {
					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))){
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 7.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {
						setFrequencyElement("7.0", beginOffset, endOffset);
					}
					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_EIGHT) {
					if (frequency != null
							&& (frequency.getBeginOffset() != beginOffset && frequency
									.getEndOffset() != endOffset)) {
						if ((dosage == null
								|| (dosage != null
										&& convertFromTextToNum(
												dosage.getDosage()).compareTo(
												"1") == 0 && (dosage
										.getBeginOffset() != beginOffset && dosage
										.getEndOffset() != endOffset))) 
										&& (changeStatus != null  && (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.OTHER) != 0)
										&&  (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.DECREASEFROM) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASE) != 0)
										&& (changeStatus.getDrugChangeStatus().compareTo(DrugChangeStatusToken.INCREASEFROM) != 0))) {
							setFrequencyElement(
									frequency.getFrequencyMention(), frequency
											.getBeginOffset(), frequency
											.getEndOffset());
						} else {
							double updateFreq = new Double(frequency
									.getFrequencyMention()).doubleValue() * 8.0;
							setFrequencyElement(String.valueOf(updateFreq), beginOffset,
									endOffset);
						}
					} else {
						setFrequencyElement("8.0", beginOffset, endOffset);
					}

					return FrequencyUnitElement.DAILY;
				} else if (da.getPeriod() == new Float(
						FrequencyUnitToken.QUANTITY_EVERY_OTHER_DAY)
						.floatValue()) {
					return FrequencyUnitElement.EVERYOTHERDAY;
				} else if (da.getPeriod() == new Float(
						FrequencyUnitToken.QUANTITY_WEEKLY).floatValue()) {
					return FrequencyUnitElement.WEEKLY;
				} else if (da.getPeriod() == new Float(
						FrequencyUnitToken.QUANTITY_MONTHLY).floatValue()) {
					return FrequencyUnitElement.MONTHLY;
				} else if (da.getPeriod() == FrequencyUnitToken.QUANTITY_PRN) {
					return FrequencyUnitElement.ASNEEDED;
				}
				return lastTerm;

			}
		}
		return null;
	}

	private String findDurationElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				DurationAnnotation.type, beginOffset, endOffset + 1);
		while (firItr.hasNext()) {
			DurationAnnotation da = (DurationAnnotation) firItr.next();
			if (da.getBegin() == beginOffset)
				return da.getCoveredText();
		}
		return null;
	}

	private String findRouteElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				RouteAnnotation.type, beginOffset, endOffset + 1);
		while (firItr.hasNext()) {
			RouteAnnotation ra = (RouteAnnotation) firItr.next();
			if (ra.getBegin() == beginOffset) {
				if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.GASTRIC)
					return RouteElement.GASTRIC;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.ORAL)
					return RouteElement.ORAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.INTRAARTERIAL)
					return RouteElement.INTRAARTERIAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.INTRACARDIAC)
					return RouteElement.INTRACARDIAC;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.INTRAMUSCULAR)
					return RouteElement.INTRAMUSCULAR;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.INTRAPERITONEAL)
					return RouteElement.INTRAPERITONEAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.INTRATHECAL)
					return RouteElement.INTRATHECAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.INTRAVENOUS)
					return RouteElement.INTRAVENOUS;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.RECTAL)
					return RouteElement.RECTAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.SUBCUTANEOUS)
					return RouteElement.SUBCUTANEOUS;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.TOPICAL)
					return RouteElement.TOPICAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.TRANSDERMAL)
					return RouteElement.TRANSDERMAL;
				else if (new Integer(ra.getIntakeMethod()).intValue() == RouteToken.TRANSMUCOSAL)
					return RouteElement.TRANSMUCOSAL;

				return ra.getCoveredText();
			}
		}
		return null;
	}

	private String findDrugChangeStatusElement(JCas jcas, int beginOffset,
			int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				DrugChangeStatusAnnotation.type, beginOffset, endOffset + 1);
		while (firItr.hasNext()) {
			DrugChangeStatusAnnotation dcsa = (DrugChangeStatusAnnotation) firItr
					.next();
			if (dcsa.getBegin() == beginOffset) {
				int posSpace = dcsa.getCoveredText().indexOf(' ');
				int posHyph = dcsa.getCoveredText().indexOf('-');
				String firstTerm = dcsa.getCoveredText();
				if (posSpace > 0)
					firstTerm = dcsa.getCoveredText().substring(0, posSpace);
				if (posHyph > 0)
					firstTerm = firstTerm.substring(0, posHyph);

				return dcsa.getChangeStatus();//convertToChangeStatus(firstTerm);

			}
		}
		return null;
	}

	private String findStrengthElement(JCas jcas, int beginOffset, int endOffset) {

		Iterator firItr = FSUtil.getAnnotationsIteratorInSpan(jcas,
				StrengthAnnotation.type, beginOffset, endOffset + 1);
		while (firItr.hasNext()) {
			StrengthAnnotation dcsa = (StrengthAnnotation) firItr.next();
			String strength = dcsa.getCoveredText();
            int findHyph = strength.indexOf('-');
            if (findHyph > 0){
            	// large value in the range
            	strength = strength.substring(findHyph+1);
            } 
			if (dcsa.getBegin() == beginOffset) {

				return parseRegex(strength);
			}
		}

		return null;
	}

	private String parseRegex(String stringGlob) {
		if (stringGlob.matches("[0-9]*[\\.]*[0-9]+\\s[a-zA-Z]+")) {
			// do nothing for now
		} else if (stringGlob.matches("[0-9]*[\\.]*[0-9]+[-]+[a-zA-Z]+")) {
			stringGlob = stringGlob.replaceAll("-", " ");
			
		} else if (stringGlob.matches("[0-9]*[\\.]*[0-9]+[a-zA-Z]+")) {
			int intRegex = indexOfRegex(stringGlob, lookForAlpha);
			
			stringGlob = stringGlob.substring(0, intRegex) + " "
					+ stringGlob.substring(intRegex);
		} else if (stringGlob.matches("[0-9]*[\\.]*[0-9]+[\\%]")) {
			int intRegex = indexOfRegex(stringGlob, lookForPercent);
			
			stringGlob = stringGlob.substring(0, intRegex) + " "
					+ stringGlob.substring(intRegex);
		} else if (stringGlob.matches("[0-9]*[\\.]*[0-9]+[\\s][\\%]")) {

			// do nothing for now

		} else if (stringGlob.matches("[0-9]*[\\.]*[0-9]+[-]\\%")) {
			
			stringGlob = stringGlob.replaceAll("-", "");
		}
		return stringGlob.trim().replace('\n', ' ').replaceAll(",", "").replaceAll("-", "");
	}



	public int parseIntValue(Object strength) {

		String text = (String) strength;
		String strengthNumText = "";
		String subText = "";
		boolean containsNums = false;
		boolean doneHere = false;
		int textSize = text.length();
		int pos = 0;
		Integer posInt = null;
		while (!doneHere && (textSize > pos) && (textSize >= 1)) {
			try {
				String numString = text.substring(pos, pos + 1);
				/*
				 * if (numString.compareTo(".") == 0) { subText =
				 * text.substring(pos + 1, textSize); pos++; }
				 */
				Integer posNum = Integer.decode(numString);
				int checkInt = posNum.intValue();

				if ((checkInt >= 0) && (checkInt <= 9)) {
					containsNums = true;
					subText = text.substring(pos + 1, textSize);
					pos++;
					strengthNumText = strengthNumText + numString;

				} else
					return 0;
			}

			catch (NullPointerException npe) {
				return 0;
			} catch (NumberFormatException nfe) {
				if (!containsNums)
					return 0;
				else
					doneHere = true;

			}
		}
		return new Integer(strengthNumText).intValue();

	}

	public double parseDoubleValue(Object strength) {

		String text = (String) strength;
		String strengthNumText = "";
		String subText = "";
		boolean containsNums = false;
		boolean doneHere = false;
		int textSize = text.length();
		int pos = 0;
		Integer posInt = null;
		while (!doneHere && (textSize > pos) && (textSize >= 1)) {
			try {
				String numString = text.substring(pos, pos + 1);

				if (numString.compareTo(".") == 0) {
					subText = text.substring(pos + 1, textSize);
					pos++;
					strengthNumText = strengthNumText + numString;
				}
				else {
					Integer posNum = Integer.decode(numString);
					int checkInt = posNum.intValue();

					if ((checkInt >= 0) && (checkInt <= 9)) {
						containsNums = true;
						subText = text.substring(pos + 1, textSize);
						pos++;
						strengthNumText = strengthNumText + numString;

				} else
					return 0;
				}
			}

			catch (NullPointerException npe) {
				return 0;
			} catch (NumberFormatException nfe) {
				if (!containsNums)
					return 0;
				else
					doneHere = true;

			}
		}
		return new Double(strengthNumText).doubleValue();

	}

	public String convertFromTextToNum(String firstTerm) {
		// First see if the text consists of a number range and take the
		// greater of the two
		int hyph = firstTerm.lastIndexOf('-');
		if (hyph > 0){
			firstTerm = firstTerm.substring(0, hyph);
			int hyphChild = firstTerm.lastIndexOf('-');
			if (hyphChild > 0 )
				firstTerm = firstTerm.substring(0, hyphChild);
			int lastHyph = firstTerm.indexOf('-');
			if (lastHyph > 0)
				firstTerm = firstTerm.substring(0, lastHyph);
		}
        int space = firstTerm.indexOf(' ');
        if (space > 0)
        	firstTerm = firstTerm.substring(0, space);
        
		if ((firstTerm.compareToIgnoreCase("first") == 0)
				|| (firstTerm.compareToIgnoreCase("one") == 0)
				|| (firstTerm.compareToIgnoreCase("daily") == 0)
				|| (firstTerm.compareToIgnoreCase(FrequencyUnitElement.DAILY) == 0)
				|| (firstTerm.compareToIgnoreCase("once") == 0)) {
			return "1";
		} else if ((firstTerm.compareToIgnoreCase("twice") == 0)
				|| (firstTerm.compareToIgnoreCase("second") == 0)
				|| (firstTerm.compareToIgnoreCase("two") == 0)) {
			return "2";
		} else if ((firstTerm.compareToIgnoreCase("third") == 0)
				|| (firstTerm.compareToIgnoreCase("three") == 0)) {
			return "3";
		} else if ((firstTerm.compareToIgnoreCase("forth") == 0)
				|| (firstTerm.compareToIgnoreCase("four") == 0)) {
			return "4";
		} else if ((firstTerm.compareToIgnoreCase("fifth") == 0)
				|| (firstTerm.compareToIgnoreCase("five") == 0)) {
			return "5";
		} else if (firstTerm.compareToIgnoreCase("six") == 0) {
			return "6";
		} else if (firstTerm.compareToIgnoreCase("seven") == 0) {
			return "7";
		} else if (firstTerm.compareToIgnoreCase("eight") == 0) {
			return "8";
		} else if (firstTerm.compareToIgnoreCase("nine") == 0) {
			return "9";
		} else if (firstTerm.compareToIgnoreCase("ten") == 0) {
			return "10";
		} else if ((firstTerm.compareToIgnoreCase("half") == 0)
				|| (firstTerm
						.compareToIgnoreCase(FrequencyUnitElement.EVERYOTHERDAY) == 0)) {
			return ".5";
		} else if ((firstTerm.compareToIgnoreCase(FrequencyUnitElement.WEEKLY) == 0)
		        || (firstTerm.compareToIgnoreCase("weekly") == 0)){
			return ".14";
		} else if ((firstTerm.compareToIgnoreCase(FrequencyUnitElement.MONTHLY) == 0) 
				|| (firstTerm.compareToIgnoreCase("monthly") == 0)){
			return ".03";
		} else if ((firstTerm.compareToIgnoreCase(FrequencyUnitElement.HOURLY) == 0) 
				|| (firstTerm.compareToIgnoreCase("hourly") == 0)){
			return "24";
		}else
			return firstTerm;
	}

	public String convertToChangeStatus(String firstTerm) {
		if ((firstTerm.compareToIgnoreCase(DrugChangeStatusElement.STOP_STATUS) == 0)
				|| (firstTerm.compareToIgnoreCase("stopped") == 0)
				|| (firstTerm.compareToIgnoreCase("past") == 0)
				|| (firstTerm.compareToIgnoreCase("stopping") == 0)
				|| (firstTerm.compareToIgnoreCase("discontinue") == 0)
				|| (firstTerm.compareToIgnoreCase("discontinued") == 0)
				|| (firstTerm.compareToIgnoreCase("DISCONTINUED MEDICATIONS") == 0)
				|| (firstTerm.compareToIgnoreCase("DISCONTINUE MEDICATIONS") == 0)) {
			return DrugChangeStatusElement.STOP_STATUS;
		} else if ((firstTerm.compareToIgnoreCase("new") == 0)
				|| (firstTerm.compareToIgnoreCase("new dose") == 0)
				|| (firstTerm.compareToIgnoreCase("start") == 0)
				|| (firstTerm.compareToIgnoreCase("started") == 0)
				|| (firstTerm.compareToIgnoreCase("new medication") == 0)
				|| (firstTerm.compareToIgnoreCase("NEW MEDICATIONS") == 0)) {
			return DrugChangeStatusElement.START_STATUS;
		} else if ((firstTerm.compareToIgnoreCase("increase") == 0)
				|| (firstTerm.compareToIgnoreCase("increased") == 0)) {
			return DrugChangeStatusElement.INCREASE_STATUS;
		} else if ((firstTerm.compareToIgnoreCase("decrease") == 0)
				|| (firstTerm.compareToIgnoreCase("lower") == 0)
				|| (firstTerm.compareToIgnoreCase("decreased") == 0)) {
			return DrugChangeStatusElement.DECREASE_STATUS;
		} else if ((firstTerm.compareToIgnoreCase("then") == 0)
				|| (firstTerm.compareToIgnoreCase("changed") == 0)
				|| (firstTerm.compareToIgnoreCase("change") == 0)) {
			return "change";
		} else if ((firstTerm.compareToIgnoreCase("and") == 0)
				|| (firstTerm.compareToIgnoreCase("plus") == 0)) {
			return "add";
		} else
			return DrugChangeStatusElement.NOCHANGE_STATUS;
	} 

	// -------------------------- PUBLIC STATIC METHODS
	// --------------------------


	public int getDosageBegin() {
		return dosage.getBeginOffset();
	}

	public int getDosageEnd() {
		return dosage.getEndOffset();
	}


	public int getFrequencyBegin() {
		return frequency.getBeginOffset();
	}

	public int getFrequencyEnd() {
		return frequency.getEndOffset();
	}



	public int getFUBegin() {
		return frequencyUnit.getBeginOffset();
	}

	public int getFUENd() {
		return frequencyUnit.getEndOffset();
	}



	public int getFormBegin() {
		return form.getBeginOffset();
	}

	public int getFormEnd() {
		return form.getEndOffset();
	}

	public int getDuratationBegin() {
		return duration.getBeginOffset();
	}

	public int getDuratationEnd() {
		return duration.getEndOffset();
	}


	public int getRouteBegin() {
		return route.getBeginOffset();
	}

	public int getRouteEnd() {
		return route.getEndOffset();
	}
	public int getStrengthUnitBegin() {
		
		return strengthUnit.getBeginOffset();
	
}

public int getStrengthUnitEnd() {
	
		return strengthUnit.getEndOffset();
	
}


	public int getStrengthBegin() {
		
			return strength.getBeginOffset();
		
	}

	public int getStrengthEnd() {
		
			return strength.getEndOffset();
		
	}
	// Add drug change status offsets begin
	public int getChangeStatusBegin() {

		return changeStatus.getBeginOffset();

	}
	public int getChangeStatusEnd() {

		return changeStatus.getEndOffset();

	}
	// Add drug change status offsets end
	/**
	 * Scan a string for the first occurrence of some regex Pattern.
	 * 
	 * @param lookForAlpha
	 *            the pattern to look for
	 * @param lookIn
	 *            the String to scan.
	 * @return offset relative to start of lookIn where it first found the
	 *         pattern, -1 if not found.
	 */

	public static int indexOfRegex(String lookIn, Pattern lookFor) {
		Matcher m = lookFor.matcher(lookIn);
		if (m.find()) {
			return m.start();
		} else {
			return -1;
		}
	}
	
private double holdLargestPeriod = -1;


}
