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
package org.apache.ctakes.assertion.cr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

public class XMLReader {

	static String[] attrHeadings = {"booleanSlotMention","stringSlotMention"};
	private static boolean VERBOSE = true; 
	
	/**
	 * Get spans of named entity annotations indexed on knowtator mention id
	 */
  public static HashMap<String, ArrayList<Span>> getEntityMentions(Document document) {

  	// key: mention id, value: list of spans (need a list to handle disjoint spans)
  	HashMap<String, ArrayList<Span>> entityMentions = new HashMap<String, ArrayList<Span>>(); 

      Element elementRoot = document.getRootElement();
      List<?> annotations = elementRoot.getChildren("annotation");

      for (int i = 0; i < annotations.size(); i++) {
        Element elementAnnotation = (Element) annotations.get(i);

        List<?> elementSpans = elementAnnotation.getChildren("span");

        if(elementSpans.size() == 0) {
          continue; // spanless annotation, e.g. a relation; there should be no spannedText                                    
        }

        ArrayList<Span> spans = new ArrayList<Span>();
        for(int j = 0; j < elementSpans.size(); j++) {
          Element elementSpan = (Element) elementSpans.get(j);

          String start = elementSpan.getAttributeValue("start");
          String end = elementSpan.getAttributeValue("end");

          Span span = new Span(Integer.parseInt(start), Integer.parseInt(end));
          spans.add(span);
        }

        String mentionId = elementAnnotation.getChild("mention").getAttributeValue("id");                          
        
        entityMentions.put(mentionId, spans);
      }
    return entityMentions;
  }

  /**
   * Type of each named entity indexed on mention ids
   */
  public static HashMap<String, String> getEntityTypes(Document document) {

    // key: mention id, value: semantic type of the corresponding entity (e.g. "sign_symptom")                                      
    HashMap<String, String> entityTypes = new HashMap<String, String>();

      Element root = document.getRootElement();
      List<?> classMentions = root.getChildren("classMention");

      for (int i = 0; i < classMentions.size(); i++) {
        Element classMention = (Element) classMentions.get(i);
        String id = classMention.getAttributeValue("id");
        String cl = classMention.getChildText("mentionClass");
        entityTypes.put(id, cl);
      }
    return entityTypes;
  }
  
  /**
   * Attribute mention IDs of each named entity, indexed on mention ids
   * Filter based on entity types, so non-NE get left out.
   */
  public static HashMap<String, List<String>> getEntityAttributes(Document document) {

    // key: mention id, value: list of attributes associated with this                                  
    HashMap<String, List<String>> entityAttr = new HashMap<String, List<String>>();

    Element root = document.getRootElement();
    List<?> classMentions = root.getChildren("classMention");

    for (int i = 0; i < classMentions.size(); i++) {
    	Element classMention = (Element) classMentions.get(i);
    	String id = classMention.getAttributeValue("id");

    	List<String> lsAttr = new ArrayList<String>(); 
    	List lsSlotMention = classMention.getChildren("hasSlotMention");

    	for (Object slotMention : lsSlotMention) {
    		String slotid = ((Element) slotMention).getAttributeValue("id");
    		lsAttr.add(slotid);
    	}
    	
    	entityAttr.put(id, lsAttr);
    }
    return entityAttr;
  }
  
  /**
   * Attributes of each named entity indexed on mention ids. For now, manually handles different types of Slots
   */
  public static HashMap<String, ArgumentInfo> getAttributes(Document document) {
	  return getAttributes(document,attrHeadings);
  }
  
  /**
   * Attributes of each named entity indexed on mention ids. For now, manually handles different types of Slots
   */
  public static HashMap<String, ArgumentInfo> getAttributes(Document document, String[] headings) {
	  
	  // key: mention id value: map from attribute to attribute value                                      
	  HashMap<String, ArgumentInfo> entityAttr = new HashMap<String,ArgumentInfo>();

	  Element root = document.getRootElement();

//	  // key: complexSlotMention id, value: complexSlotMention value                                                           
//	  List<ArgumentInfo> listSlotMentions = new ArrayList<ArgumentInfo>();

	  // read all ??SlotMentions which additional slots                         
	  for (String heading : headings) {
		  List<?> slotMentions = root.getChildren(heading);
		  for (int i = 0; i < slotMentions.size(); i++) {
			  Element complexSlotMention = (Element) slotMentions.get(i);

			  String id = complexSlotMention.getAttributeValue("id");
			  String value = complexSlotMention.getChild(heading+"Value").getAttributeValue("value");
			  String attr = complexSlotMention.getChild("mentionSlot").getAttributeValue("id"); // e.g. "Related_to"             

			  if (entityAttr.containsKey(id)) {
				  if (VERBOSE ) { System.err.println("WARNING: found more than one attribute in an attribute mention"); }
				  entityAttr.put(id,new ArgumentInfo(value, normalizeName(attr)));
			  } else {
				  entityAttr.put(id,new ArgumentInfo(value, normalizeName(attr)));
			  }
			  //		  listSlotMentions.put(id, new ArgumentInfo(value, normalizeName(attr)));		  
		  }
	  }
//	  // now read all classMentions which have relation type and arguments (as hasSlotMention(s))                                 
//	  List<?> classMentions = root.getChildren("classMention");
//	  for (int i = 0; i < classMentions.size(); i++) {
//		  Element classMention = (Element) classMentions.get(i);
//		  List<?> hasSlotMentions = classMention.getChildren("hasSlotMention");
//
//		  if(hasSlotMentions.size() >= 2) {
//			  String relationType = classMention.getChildText("mentionClass");
////			  addRelation(relations, hasSlotMentions, hashComplexSlotMentions, relationType);  // save this relation and args
//		  }
//	  }
	  return entityAttr;
  }
  
  public static ArrayList<RelationInfo> getRelations(Document document) {

    ArrayList<RelationInfo> relations = new ArrayList<RelationInfo>();

      Element root = document.getRootElement();

      // key: complexSlotMention id, value: complexSlotMention value                                                           
      HashMap<String, ArgumentInfo> hashComplexSlotMentions = new HashMap<String, ArgumentInfo>();

      // first read all complexSlotMentions which contain argument roles (Related_to or Argument)                         
      List<?> complexSlotMentions = root.getChildren("complexSlotMention");
      for (int i = 0; i < complexSlotMentions.size(); i++) {
        Element complexSlotMention = (Element) complexSlotMentions.get(i);

        String id = complexSlotMention.getAttributeValue("id");
        String value = complexSlotMention.getChild("complexSlotMentionValue").getAttributeValue("value");
        String role = complexSlotMention.getChild("mentionSlot").getAttributeValue("id"); // e.g. "Related_to"             

        hashComplexSlotMentions.put(id, new ArgumentInfo(value, normalizeName(role)));
      }

      // now read all classMentions which have relation type and arguments (as hasSlotMention(s))                                 
      List<?> classMentions = root.getChildren("classMention");
      for (int i = 0; i < classMentions.size(); i++) {
        Element classMention = (Element) classMentions.get(i);
        List<?> hasSlotMentions = classMention.getChildren("hasSlotMention");

        if(hasSlotMentions.size() >= 2) {
          String relationType = classMention.getChildText("mentionClass");
          addRelation(relations, hasSlotMentions, hashComplexSlotMentions, relationType);  // save this relation and args
        }
      }
    return relations;
  }

  private static void addRelation(ArrayList<RelationInfo> relations, List<?> hasSlotMentions, 
  		HashMap<String, ArgumentInfo> hashComplexSlotMentions, String relationType) {
  	// add relation arguments and other relation information to the list of relations                                                  

  	// get the ids of the arguments; sometimes there are three hasSlotMention(s) but not all of them are arguments             
  	ArrayList<String> ids = new ArrayList<String>();
  	for(int i = 0; i < hasSlotMentions.size(); i++) {
  		String id = ((Element) hasSlotMentions.get(i)).getAttributeValue("id");
  		if(hashComplexSlotMentions.containsKey(id)) {
    		String role = hashComplexSlotMentions.get(id).role;
    		// check the role explicitly; in sharp data (unlike in mipacq), one
    		// of the hasSlotMention(s) can be a negation attribute with a span
  			if(role.equals("Argument") || role.equals("Related_to")) {
  				ids.add(id);                               
  			}
  		}
  	}
   	
  	// exactly two arguments are allowed
  	if(ids.size() != 2) {
  		return; 
  	}
  	
  	String id1 = hashComplexSlotMentions.get(ids.get(0)).value;          // obtain mention id1                                       
  	String role1 = hashComplexSlotMentions.get(ids.get(0)).role;         // e.g. Argument                                                                                             

  	String id2 = hashComplexSlotMentions.get(ids.get(1)).value;          // obtain mention id2                                       
  	String role2 = hashComplexSlotMentions.get(ids.get(1)).role;         // e.g. Related_to     

  	relations.add(new RelationInfo(id1, id2, role1, role2, relationType));
  }
  
  /**
   * Convert Argument_CU and Related_to_CU to Argument and Related_to.
   * This will not be necessary in the future when the data will be 
   * post-processed to remove _CU suffixes. 
   * 
   * Currently mipacq data does not have the suffixes and sharp data does.
   */
  private static String normalizeName(String role) {
  	
  	if(role.equals("Argument_CU")) {
  		return "Argument";
  	} 
  	
  	if(role.equals("Related_to_CU")) {
  		return "Related_to";
  	}
  	
  	return role;
  		
  }
}
