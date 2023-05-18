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
package org.apache.ctakes.preprocessor;

import java.util.*;

/**
 *
 * @author Mayo Clinic
 */
public class DocumentMetaData
{	
	private String iv_text;
	
	final private List<Annotation> iv_scAnnotationList = new ArrayList<>();
	
	// key = unique key, value = document meta data
	final private Map<String,String> iv_docMetaDataMap = new HashMap<>();
	
	// key = segment ID, value = SegmentMetaData object
	final private Map<String, SegmentMetaData> iv_segMetaDataHash = new HashMap<>();

	/**
	 * Adds a meta data entry for the document.
	 * @param key -
	 * @param value -
	 */
	public void addMetaData(final String key, final String value)
	{
		iv_docMetaDataMap.put(key, value);
	}

	/**
	 * Gets a map of meta data about the document.
	 * @return -
	 */
	public Map<String,String> getMetaData()
	{
		return iv_docMetaDataMap;
	}

	/**
	 * Adds a segment.
	 * @param smd -
	 */
	public void addSegment(final SegmentMetaData smd)
	{
		iv_segMetaDataHash.put(smd.id, smd);
	}

	/**
	 * Gets a set of segment identifiers.  Each identifier is a String object.
	 * @return Set of String objects, each String is a segment identifier.
	 */
	public Set<String> getSegmentIdentifiers()
	{
		return iv_segMetaDataHash.keySet();
	}

	/**
	 * Gets meta data about the specified segment.
	 * @param segmentID Identifier of segment.
	 * @return SegmentMetaData object that contains meta data about the
	 * 	       specified segment.
	 */
	public SegmentMetaData getSegment(final String segmentID)
	{
		return iv_segMetaDataHash.get(segmentID);
	}
	
	/**
	 * Adds a single annotation to the syntactic cue annotation list.
	 * @param a -
	 */
	public void addAnnotation(final Annotation a)
	{
		iv_scAnnotationList.add(a);
	}
	
	/**
	 * Adds a list of annotations to the syntactic cue annotation list.
	 * @param aList -
	 */
	public void addAnnotations(final List<Annotation> aList)
	{
		iv_scAnnotationList.addAll(aList);
	}

	/**
	 * Gets a list of Annotation objects that represent annotations based 
	 * off of document syntactic cues.
	 * @return List of Annotation objects.
	 */
	public List getAnnotations()
	{
		return iv_scAnnotationList;
	}

    /**
     * @return -
     */
    public String getText()
    {
        return iv_text;
    }

    /**
     * @param string -
     */
    public void setText(final String string)
    {
        iv_text = string;
    }

}
