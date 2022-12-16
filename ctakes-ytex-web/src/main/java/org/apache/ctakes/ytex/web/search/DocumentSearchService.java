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


import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Service for searching documents.
 * Executes queries defined in ytex/search.properties.
 * @author vijay
 *
 */
public interface DocumentSearchService {

	public abstract List<DocumentSearchResult> searchByCui(String cui);

	/**
	 * Extended search
	 * @param code concept CUI or code.  this is the only required argument
	 * @param documentTypeName document type name.  (in VACS @see DocumentType)
	 * @param dateFrom document date greater than or equal to this
	 * @param dateTo document date less than or equal to this
	 * @param patientId patient id (study id in VACS)
	 * @param negationStatus true - only affirmed terms.  false - only negated terms. 
	 * @return list of results matching query
	 */
	public List<DocumentSearchResult> extendedSearch(String code, String documentTypeName, Date dateFrom,
			Date dateTo, Integer patientId, Boolean negationStatus);

	/**
	 * perform full text search
	 * @param searchTerm
	 * @return list of maps for each record.  map keys correspond to search query headings.  map (i.e. query) must contain DOCUMENT_ID (integer) and NOTE (string) fields.
	 */
	public List<Map<String, Object>> fullTextSearch(String searchTerm);

	/**
	 * retrieve note for specified document id, retrieved via full text search
	 * @param documentId
	 * @return note text.
	 */
	public String getFullTextSearchDocument(int documentId);

}