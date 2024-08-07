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
package org.apache.ctakes.ytex.web.search;

import com.icesoft.faces.component.selectinputtext.SelectInputText;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSF Backing Bean for semanticSearch.jspx. Search for documents based on
 * concept ids, negation status, patient, and date. Based on the IceFaces
 * autocomplete example.
 * 
 * @author vijay
 * 
 */
public class SemanticSearchBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger("SemanticSearchBean");

	private transient ConceptSearchService umlsFirstWordService;
	private transient DocumentSearchService documentSearchService;
	private Date dateFrom;
	private Date dateTo;
	private Integer patientId;
	private Boolean negationStatus;

	public ConceptSearchService getUmlsFirstWordService() {
		return umlsFirstWordService;
	}

	public void setUmlsFirstWordService(
			ConceptSearchService umlsFirstWordService) {
		this.umlsFirstWordService = umlsFirstWordService;
	}

	public DocumentSearchService getDocumentSearchService() {
		return documentSearchService;
	}

	public void setDocumentSearchService(
			DocumentSearchService documentSearchService) {
		this.documentSearchService = documentSearchService;
	}

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	public Integer getPatientId() {
		return patientId;
	}

	public void setPatientId(Integer patientId) {
		if (patientId != null && patientId != 0) {
            this.patientId = patientId;
        }
	}

	public Boolean getNegationStatus() {
		return negationStatus;
	}

	public void setNegationStatus(Boolean negationStatus) {
		this.negationStatus = negationStatus;
	}

	public ConceptFirstWord getSearchCUI() {
		return searchCUI;
	}

	public void setSearchCUI(ConceptFirstWord searchCUI) {
		this.searchCUI = searchCUI;
	}

	public List<DocumentSearchResult> getSearchResultList() {
		return searchResultList;
	}

	public void setSearchResultList(List<DocumentSearchResult> searchResultList) {
		this.searchResultList = searchResultList;
	}

	// default city, no value.
	private ConceptFirstWord currentCUI = new ConceptFirstWord();

	private ConceptFirstWord searchCUI = new ConceptFirstWord();

	// list of possible matches.
	private List<SelectItem> matchesList = new ArrayList<SelectItem>();

	private List<DocumentSearchResult> searchResultList = new ArrayList<DocumentSearchResult>();

	public void searchListen(ActionEvent event) {
		this.searchCUI = this.currentCUI;
		if (this.currentCUI != null) {
			if (this.getNegationStatus() != null || this.getPatientId() != null
					|| this.getDateFrom() != null || this.getDateTo() != null) {
				this.searchResultList = this.documentSearchService
						.extendedSearch(this.currentCUI.getConceptId(), null,
								this.getDateFrom(), this.getDateTo(),
								this.getPatientId(), this.getNegationStatus());
			} else {
				this.searchResultList = this.documentSearchService
						.searchByCui(this.currentCUI.getConceptId());
			}
			if ( LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}", this.searchResultList);
            }
		}
	}

	public void resetListen(ActionEvent event) {
		this.setNegationStatus(null);
		this.setDateFrom(null);
		this.setDateTo(null);
		this.setPatientId(null);
		this.setSearchResultList(null);
		this.searchCUI = null;
		// can't clear cui in UI - only in backing bean
		// this.currentCUI = null;
		// this.matchesList = null;
	}

	/**
	 * Called when a user has modifed the SelectInputText value. This method
	 * call causes the match list to be updated.
	 * 
	 * @param event
	 */
	public void updateList(ValueChangeEvent event) {

		// get a new list of matches.
		setMatches(event);

		// Get the auto complete component from the event and assing
		if (event.getComponent() instanceof SelectInputText) {
			SelectInputText autoComplete = (SelectInputText) event
					.getComponent();
			// if no selected item then return the previously selected item.
			if (autoComplete.getSelectedItem() != null) {
				currentCUI = (ConceptFirstWord) autoComplete.getSelectedItem()
						.getValue();
			}
			// otherwise if there is a selected item get the value from the
			// match list
			else {
				ConceptFirstWord tempCUI = getMatch(autoComplete.getValue()
						.toString());
				if (tempCUI != null) {
					currentCUI = tempCUI;
				}
			}
		}
	}

	/**
	 * Gets the currently selected city.
	 * 
	 * @return selected city.
	 */
	public ConceptFirstWord getCurrentCUI() {
		return currentCUI;
	}

	/**
	 * The list of possible matches for the given SelectInputText value
	 * 
	 * @return list of possible matches.
	 */
	public List<SelectItem> getList() {
		return matchesList;
	}

	public static String formatUMLSFirstWord(ConceptFirstWord fword) {
		return fword.getText() + " [" + fword.getConceptId() + ']';
	}

	public static ConceptFirstWord extractUMLSFirstWord(String fword) {
		String tokens[] = fword.split("[|]");
		ConceptFirstWord umlsFWord = new ConceptFirstWord();
		// last token is cui
		if (tokens.length > 1) {
			String cui = tokens[tokens.length - 1];
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < tokens.length - 1; i++) {
				builder.append(tokens[i]);
			}
			String text = builder.toString();
			umlsFWord.setConceptId(cui);
			umlsFWord.setText(text);
		}
		return umlsFWord;
	}

	public static Comparator<Object> umlsFirstWordComparator = new Comparator<Object>() {

		// compare method for city entries.
		@Override
        public int compare(Object o1, Object o2) {
			String s1;
			String s2;

			if (o1 instanceof ConceptFirstWord) {
				s1 = formatUMLSFirstWord((ConceptFirstWord) o1);
			} else {
				s1 = o1.toString();
			}

			if (o2 instanceof ConceptFirstWord) {
				s2 = formatUMLSFirstWord((ConceptFirstWord) o2);
			} else {
				s2 = o2.toString();
			}
			return s1.compareTo(s2);
		}
	};

	private ConceptFirstWord getMatch(String value) {
		ConceptFirstWord result = null;
		if (matchesList != null) {
			SelectItem si;
			Iterator<SelectItem> iter = matchesList.iterator();
			while (iter.hasNext()) {
				si = iter.next();
				if (value.equals(si.getLabel())) {
					return (ConceptFirstWord) si.getValue();
				}
			}
		}
		return result;
	}

	/**
	 * Utility method for building the match list given the current value of the
	 * SelectInputText component.
	 * 
	 * @param event
	 */
	private void setMatches(ValueChangeEvent event) {

		Object searchWord = event.getNewValue();
		String searchString;
		if (searchWord instanceof SelectItem) {
			searchString = ((SelectItem) searchWord).getLabel();
		} else {
			searchString = searchWord.toString();
		}
		if (searchString != null && searchString.length() > 2) {
			List<ConceptFirstWord> cuis = this.umlsFirstWordService
					.getConceptByFirstWord(searchString);
			this.matchesList = new ArrayList<SelectItem>(cuis.size());
			for (ConceptFirstWord cui : cuis) {
				this.matchesList.add(new SelectItem(cui,
						formatUMLSFirstWord(cui)));
			}
		}
	}

}
