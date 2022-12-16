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
package org.apache.ctakes.ytex.umls.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.ctakes.ytex.umls.model.UmlsAuiFirstWord;


public interface UMLSDao {

	// public abstract List<Object[]> getRelationsForSABs(String sabs[]);
	//
	// public abstract List<Object[]> getAllRelations();

	/**
	 * pattern to match cuis and parse out their number
	 */
	public static final Pattern cuiPattern = Pattern.compile("\\AC(\\d{7})\\Z");

	/**
	 * get all aui, str from mrconso
	 */
	public List<Object[]> getAllAuiStr(String lastAui);

	public void deleteAuiFirstWord();

	public void insertAuiFirstWord(List<UmlsAuiFirstWord> listAuiFirstWord);

	public abstract Map<String, String> getNames(List<String> subList);

	/**
	 * Get the 'last' UmlsAuiFirstWord. We insert them in ascending order of
	 * auis.
	 * 
	 * @return
	 */
	public abstract String getLastAui();

	/**
	 * get a set of all cuis in RXNORM. used for DrugNer - need to set the
	 * coding scheme to RXNORM. Convert the cui into a numeric representation
	 * (chop off the preceding 'C') to save memory.
	 * 
	 * @return
	 */
	public abstract HashSet<Integer> getRXNORMCuis();

	public abstract boolean isRXNORMCui(String cui);

}