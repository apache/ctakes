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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Mapper;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Read named entity annotations from knowtator xml files into the CAS
 * 
 * @author stephen wu
 *
 */
@PipeBitInfo(
		name = "Knowtator XML Reader (Generic)",
		description = "Read named entity annotations from knowtator xml files into the CAS.",
		role = PipeBitInfo.Role.SPECIAL,
		products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION }
)
public class GoldEntityAndAttributeReader extends JCasAnnotator_ImplBase {

	// paramater that should contain the path to knowtator xml files
	public static final String PARAM_INPUTDIR = "InputDirectory";
	// path to knowtator xml files
	public static String inputDirectory;
	// counter for assigning entity ids
	public int identifiedAnnotationId;
//	private final boolean VERBOSE = true;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		
		inputDirectory = (String)aContext.getConfigParameterValue(PARAM_INPUTDIR);
		identifiedAnnotationId = 0;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

			JCas initView;
      try {
        initView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }
      String goldFilePath = inputDirectory + DocIdUtil.getDocumentID( jCas ) + ".knowtator.xml";
			
      SAXBuilder builder = new SAXBuilder();
      Document document;
      try {
        document = builder.build(new File(goldFilePath));
      } catch ( Exception e) {
        throw new AnalysisEngineProcessException(e);
      }// TODO this should be IOException, but the command-line maven build was breaking


		// map knowtator mention ids to entity offsets
      HashMap<String, ArrayList<Span>> allMentions = XMLReader.getEntityMentions(document);
      // map knowtator mention ids to entity types
      HashMap<String, String> entityTypes = XMLReader.getEntityTypes(document);
      // map knowtator mention ids to the ids of mention-attributes (or attributes themselves)
      HashMap<String,List<String>> mentionAttr = XMLReader.getEntityAttributes(document);
      // map knowtator mention-attribute ids to attributes
      String[] complexSlotMention = {"complexSlotMention"};
      HashMap<String, ArgumentInfo> attrPtr = XMLReader.getAttributes(document,complexSlotMention);
      // map knowtator attribute ids to role-value pairs
      HashMap<String, ArgumentInfo> attrs = XMLReader.getAttributes(document);


      System.out.println("What's in attrPtr -- the mention-attribute ids to attributes....");
      for (Entry<String,ArgumentInfo> e : attrPtr.entrySet()) {
    	  System.out.println("attrPtr: " + e.getKey() + " with role " + e.getValue().role + " and value " + e.getValue().value);
      }

      System.out.println("\nWhat's in attrs -- the knowtator attribute ids....");
      for (Entry<String,ArgumentInfo> e : attrs.entrySet()) {
    	  System.out.println("attrs: " + e.getKey() + " with role " + e.getValue().role + " and value " + e.getValue().value);
      }
      
      // pare down hashmap based on types -- keep only NEs
      HashMap<String, ArrayList<Span>> neMentions = filterToNamedEntitiesOnly(allMentions,entityTypes);

      for(Map.Entry<String, ArrayList<Span>> mention : neMentions.entrySet()) {
    	  String mentionId = mention.getKey();
    	  
//    	  // pare down what to consider -- keep only valid NEs, discard modifiers
//    	  if (!filterToNamedEntitiesOnly(allMentions,
//    			  mentionId,entityTypes.get(mentionId))) {
//    		  continue;
//    	  }
    	  
    	  Span first = null;
    	  Span last = null;
    	  // for disjoint spans, just ignore the gap
    	  first = mention.getValue().get(0);
    	  last = mention.getValue().get(mention.getValue().size() - 1);

    	  // put entity and attributes into the CAS
    	  // choose either entity or event
    	  IdentifiedAnnotation eMention;
    	  int type = Mapper.getEntityTypeId(entityTypes.get(mentionId));
    	  if (type==CONST.NE_TYPE_ID_ANATOMICAL_SITE) {
    		  eMention = new EntityMention(initView, first.start, last.end);  
    	  } else if (type==CONST.NE_TYPE_ID_DISORDER
    			  || type==CONST.NE_TYPE_ID_DRUG
    			  || type==CONST.NE_TYPE_ID_FINDING
    			  || type==CONST.NE_TYPE_ID_PROCEDURE
//    			  || type==CONST.NE_TYPE_ID_ANATOMICAL_SITE
    			  ) {
    		  eMention = new EventMention(initView, first.start, last.end);
    	  } else {
    		  eMention = new IdentifiedAnnotation(initView, first.start, last.end);
    	  }
    	  
    	  // set easy attributes
//    	  eMention.setTypeID(Mapper.getEntityTypeId(entityTypes.get(mentionId)));
			eMention.setTypeID( SemanticGroup.getGroup( entityTypes.get( mentionId ) ).getCode() );
    	  eMention.setId(identifiedAnnotationId++);
    	  eMention.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_GOLD_ANNOTATION);
    	  eMention.setConfidence(1);

    	  if (mentionId.endsWith("4351")) {
    		  System.out.println();
    	  }
    	  
          List<ArgumentInfo> assocAttributes = getLeafAttributes(mentionId,
        		  mentionAttr,attrPtr,attrs,new ArrayList<>());
          
          for (ArgumentInfo a : assocAttributes) {

//    	  // set harder attributes from cas -- look through all attribute ids attached to this mentionId
//    	  for (String attrId : mentionAttr.get(mentionId) ) {
//    		  // make sure this attribute was actually somewhere in the knowtator file
//    		  if (!attrs.containsKey(attrId)) {
//    			  if (VERBOSE) { System.err.println("WARNING: attribute not found: "+attrId); }
//    			  continue;
//    		  }
    			
    		  // look up the attribute id and set values accordingly
    		  checkForAttrValue(eMention, a.role, a.value);
    	  }
    	  
    	  // add to CAS
    	  eMention.addToIndexes();
      }
	}

	private List<ArgumentInfo> getLeafAttributes(String id,
			HashMap<String, List<String>> mentionAttr,
			HashMap<String, ArgumentInfo> attrPtr, HashMap<String, ArgumentInfo> attrs, List<ArgumentInfo> output) {

		// if this is a mention id
		if (mentionAttr.containsKey(id)) {
//			if (mentionAttr.get(id).size()>1 && VERBOSE) {
//				System.err.println("WARNING: expected an attribute's mention to have only one attr, but not so");
//			}
			for (String attrId : mentionAttr.get(id)) {
				// assumes that if you're in an attribute mention, you only have one value
				if (attrPtr.containsKey(attrId)) {
					ArgumentInfo a = attrPtr.get(attrId);
					if ( notRelationArgument( attrPtr.get( attrId ).role ) )
						getLeafAttributes(attrPtr.get(attrId).value, mentionAttr, attrPtr, attrs, output);
					
				} else if (attrs.containsKey(attrId)){
					ArgumentInfo a = attrs.get(attrId);
					if ( notRelationArgument( attrs.get( attrId ).role ) )
						output.add(attrs.get(attrId));
				}
			}
		} 
		
		// if this is an attribute id
		else if (attrPtr.containsKey(id)) {
			if ( !attrPtr.get(id).role.equals("Related_to_CU") 
					&& !attrPtr.get(id).role.equals("Argument_CU") )
				getLeafAttributes(attrPtr.get(id).value, mentionAttr, attrPtr, attrs, output);
		} else if (attrs.containsKey(id)){
			if ( !attrs.get(id).role.equals("Related_to_CU") 
					&& !attrs.get(id).role.equals("Argument_CU") )
				output.add(attrs.get(id));
		}
		
		return output;
	}

	private boolean notRelationArgument( String role ) {
		if (normalizeRoleName(role).equals("Related_to")) {
			return false;
		}
		return !normalizeRoleName( role ).equals( "Argument" );
	}

	private void checkForAttrValue(IdentifiedAnnotation eMention, String role,
			String value) {
		if (role.contains("_normalization")) {
			  if (role.startsWith("conditional")) {
				  eMention.setConditional(Boolean.parseBoolean( value ) );
			  } else if (role.startsWith("generic")) {
				  eMention.setGeneric(Boolean.parseBoolean( value ) );
			  } else if (role.startsWith("negation_indicator")) {
				  // assumes that the string from Knowtator is exactly "negation_present"
				  if (value.equals("negation_present")) { 
					  eMention.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
				  } else {
					  eMention.setPolarity(CONST.NE_POLARITY_NEGATION_ABSENT);
				  }
			  } else if (role.startsWith("subject")) {
				  // assumes that the strings from Knowtator are exactly what's in the type system
				  eMention.setSubject(value);
			  } else if (role.startsWith("uncertainty_indicator")) {
				  // assumes that the string from Knowtator is exactly "indicator_present"
				  if (value.equals("indicator_present")) { 
					  eMention.setUncertainty(CONST.NE_UNCERTAINTY_PRESENT);
				  } else {
					  eMention.setUncertainty(CONST.NE_UNCERTAINTY_ABSENT);
				  }
//			  } else if (role.startsWith("generic")) {
//				  eMention.setGeneric(Boolean.valueOf(value));
			  }
		  }
	}

	// Takes the Knowtator schema value and filters out things that are not NE.
	//   In principle can have a parallel "filterToAttributesOnly"
	private HashMap<String, ArrayList<Span>> filterToNamedEntitiesOnly(
			HashMap<String, ArrayList<Span>> entityMentions,
			HashMap<String, String> entityTypes) {
		HashMap<String, ArrayList<Span>> newEntityMentions = new HashMap<>();
		
		for (Entry<String, String> etype : entityTypes.entrySet()) {
			if (etype.getValue().equals("Anatomical_site") 
					|| etype.getValue().equals("Disease_Disorder")					
						|| etype.getValue().equals("Lab")					
							|| etype.getValue().equals("Medications")					
								|| etype.getValue().equals("Procedure")					
									|| etype.getValue().equals("Sign_symptom")					
			) {
				if (entityMentions.containsKey(etype.getKey())) {
					newEntityMentions.put(etype.getKey(),entityMentions.get(etype.getKey()));
				}
			}
		}
		
		return newEntityMentions;
	}
	
	// Takes the Knowtator schema value and filters out things that are not NE.
	//   In principle can have a parallel "filterToAttributesOnly"
//	private boolean filterToNamedEntitiesOnly(
//			HashMap<String, ArrayList<Span>> entityMentions,
//			String typeKey, String typeValue) {
//		// Note: Nothing toLowerCase() will ever match another string with UpperCase Characters!
//		if (typeValue.toLowerCase().equals("Anatomical_site")
//				|| typeValue.toLowerCase().equals("Disease_Disorder")
//				|| typeValue.toLowerCase().equals("Lab")
//				|| typeValue.toLowerCase().equals("Medications")
//				|| typeValue.toLowerCase().equals("Procedure")
//				|| typeValue.toLowerCase().equals("Sign_symptom")
//		) {
//			if (entityMentions.containsKey(typeKey)) {
//				return true;
//			}
//		}
//
//		return false;
//	}
		
	/**
	 * Convert Argument_CU and Related_to_CU to Argument and Related_to.
	 * This will not be necessary in the future when the data will be 
	 * post-processed to remove _CU suffixes. 
	 * 
	 * Currently mipacq data does not have the suffixes and sharp data does.
	 */
	private static String normalizeRoleName(String role) {

		if(role.equals("Argument_CU")) {
			return "Argument";
		} 

		if(role.equals("Related_to_CU")) {
			return "Related_to";
		}

		return role;

	}
}