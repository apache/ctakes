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
package org.apache.ctakes.core.util;

import org.apache.ctakes.typesystem.type.constants.CONST;

/**
 * Map various category names to their ctakes integer id. 
 * If an entity type that cannot be extracted by CTAKEs
 * automatically is passed, "unknown relation" id is returned.
 *  
 * @author dmitriy dligach
 * @deprecated use org.apache.ctakes.core.util.annotation.SemanticGroup.  e.g. SemanticGroup.getBestGroup( annotation ).getCode();
 */
@Deprecated
public class Mapper {

	/**
	 * Map entity type to its integer id.
    * @deprecated use org.apache.ctakes.core.util.annotation.SemanticGroup.  SemanticGroup.getGroup( name ).getCode();
	 */
   @Deprecated
   public static int getEntityTypeId( String entityType ) {

		if(entityType.equals("Disease_Disorder")) return CONST.NE_TYPE_ID_DISORDER;
	  else if(entityType.equals("Procedure")) return CONST.NE_TYPE_ID_PROCEDURE;
	  else if(entityType.equals("Medications/Drugs")) return CONST.NE_TYPE_ID_DRUG;
	  else if(entityType.equals("Sign_symptom")) return CONST.NE_TYPE_ID_FINDING;
	  else if(entityType.equals("Anatomical_site")) return CONST.NE_TYPE_ID_ANATOMICAL_SITE;
	  else return CONST.NE_TYPE_ID_UNKNOWN;
	}
	
	/**
	 * Map modifier type to its integer id.
    * @deprecated use org.apache.ctakes.core.util.annotation.SemanticGroup.  SemanticGroup.getGroup( name ).getCode();
	 */
   @Deprecated
   public static int getModifierTypeId( String modifierType ) {
		
		if(modifierType.equals("course_class")) return CONST.MODIFIER_TYPE_ID_COURSE_CLASS;
		else if(modifierType.equals("severity_class")) return CONST.MODIFIER_TYPE_ID_SEVERITY_CLASS;
		else if(modifierType.equals("lab_interpretation_indicator")) return CONST.MODIFIER_TYPE_ID_LAB_INTERPRETATION_INDICATOR;
		else return CONST.MODIFIER_TYPE_ID_UNKNOWN;
	}
}
