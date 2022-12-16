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
package org.apache.ctakes.relationextractor.data;

import java.io.File;
import java.io.FilenameFilter;

import java.io.IOException;
import java.util.HashMap;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.apache.ctakes.core.util.Mapper;
import org.apache.ctakes.relationextractor.knowtator.RelationInfo;
import org.apache.ctakes.relationextractor.knowtator.XMLReader;
import org.apache.ctakes.typesystem.type.constants.CONST;

/**
 * Calculate relation frequencies in a data set that consists of XML files exported from Knowtator.
 * 
 * @author dmitriy dligach
 *
 */
public class Stats {
		
	// read all the relations in the data or just the SHARP ones?
	public static final boolean readOnlySharpRelations = true;

	public static void getEntityStats(String inputDir) throws JDOMException, IOException {

		File dir = new File(inputDir);

		// key: entity type, value: total instances of this entity
		HashMap<String, Integer> entityCounts = new HashMap<String, Integer>(); 
		// total number of entities
		int totalEntityCount = 0;

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File fileDir, String name) {
				return name.endsWith(".xml");
			}
		};

		for(String file : dir.list(filter)) {

			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(new File(inputDir, file));

			for(String enitytType : XMLReader.getEntityTypes(document).values()) {

				if(Mapper.getEntityTypeId(enitytType) == CONST.NE_TYPE_ID_UNKNOWN) {
					continue;
				}
				
				totalEntityCount++;

				if(entityCounts.containsKey(enitytType)) {
					entityCounts.put(enitytType, entityCounts.get(enitytType) + 1);
				} 
				else {
					entityCounts.put(enitytType, 1);
				}
			}
		}

		System.out.println(inputDir);
		reportStats(entityCounts, totalEntityCount);
	}

	public static void getRelationStats(String inputDir) throws JDOMException, IOException {
		
		File dir = new File(inputDir);
		
		// key: relation, value: total instances of this relation
	  HashMap<String, Integer> relationCounts = new HashMap<String, Integer>(); 
		// total number of relations
	  int totalRelationCount = 0;
		
	  FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File fileDir, String name) {
				return name.endsWith(".xml");
			}
		};

		for(String file : dir.list(filter)) {
			
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(new File(inputDir, file));

			for(RelationInfo relationInfo : XMLReader.getRelations(document)) {
	    	
				if(readOnlySharpRelations) {
	    		if(! Constants.sharpRelationsSelected.contains(relationInfo.category)) {
	    			continue; // ignore this relation
	    		}
	    	}
	  
				totalRelationCount++;
				
				if(relationCounts.containsKey(relationInfo.category)) {
					relationCounts.put(relationInfo.category, relationCounts.get(relationInfo.category) + 1);
				} 
				else {
					relationCounts.put(relationInfo.category, 1);
				}
			}
		}

		System.out.println(inputDir);
		reportStats(relationCounts, totalRelationCount);
	}
	
  public static void reportStats(HashMap<String, Integer> counts, int totalCount) {

    for(String key : counts.keySet()) {
    	System.out.format("%-25s %5d (%.2f%%)\n", key, counts.get(key), (double) counts.get(key) * 100 / totalCount);
    }
    
    System.out.format("\n%-25s %5d (%d%%)\n\n", "total", totalCount, 100);
  }
  
	public static void main(String[] args) throws JDOMException, IOException {
		
		Stats.getEntityStats(Constants.sharpTrainXmlPath);
		Stats.getRelationStats(Constants.sharpTrainXmlPath);
	}
}
