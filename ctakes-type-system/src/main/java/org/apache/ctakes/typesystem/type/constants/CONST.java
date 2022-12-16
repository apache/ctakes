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
package org.apache.ctakes.typesystem.type.constants;
/**
 * Contains a set of constants for feature values.
 * 
 * @author Mayo
 */
public class CONST {

	public static final int NE_TYPE_ID_UNKNOWN = 0;
	public static final int NE_TYPE_ID_DRUG = 1;
	public static final int NE_TYPE_ID_DISORDER = 2;
	public static final int NE_TYPE_ID_FINDING = 3;
	public static final int NE_TYPE_ID_PROCEDURE = 5;
	public static final int NE_TYPE_ID_ANATOMICAL_SITE = 6;
	public static final int NE_TYPE_ID_CLINICAL_ATTRIBUTE = 7;
	public static final int NE_TYPE_ID_DEVICE = 8;
	public static final int NE_TYPE_ID_LAB = 9;
	public static final int NE_TYPE_ID_PHENOMENA = 10;

   static public final int NE_TYPE_ID_SUBJECT_MODIFIER = 1001;
   static public final int NE_TYPE_ID_PERSON_TITLE = 1002;
   static public final int NE_TYPE_ID_GENERIC_EVENT = 1003;
   static public final int NE_TYPE_ID_GENERIC_ENTITY = 1004;
   static public final int NE_TYPE_ID_TIME_MENTION = 1005;
   static public final int NE_TYPE_ID_GENERIC_MODIFIER = 1006;
   static public final int NE_TYPE_ID_LAB_VALUE_MODIFIER = 1007;

	public static final int MODIFIER_TYPE_ID_UNKNOWN = 0;
	public static final int MODIFIER_TYPE_ID_COURSE_CLASS = 1;
	public static final int MODIFIER_TYPE_ID_SEVERITY_CLASS = 2;
	public static final int MODIFIER_TYPE_ID_LAB_INTERPRETATION_INDICATOR = 3;
	
	public static final int NE_DISCOVERY_TECH_DICT_LOOKUP = 1;
	public static final int NE_DISCOVERY_TECH_GOLD_ANNOTATION = 2;
   public static final int NE_DISCOVERY_TECH_EXPLICIT_AE = 3;

   public static final int NE_POLARITY_NEGATION_ABSENT = 1;
	public static final int NE_POLARITY_NEGATION_PRESENT = -1;
	
	public static final int NE_UNCERTAINTY_PRESENT = 1;
	public static final int NE_UNCERTAINTY_ABSENT = 0;
	
	public static final int NE_HISTORY_OF_PRESENT = 1;
	public static final int NE_HISTORY_OF_ABSENT = 0;	
	
	public static final boolean NE_GENERIC_TRUE = true;
	public static final boolean NE_GENERIC_FALSE = false;

	public static final boolean NE_CONDITIONAL_TRUE = true;
	public static final boolean NE_CONDITIONAL_FALSE = false;

	public static final int NE_CERTAINTY_POSITIVE = NE_POLARITY_NEGATION_ABSENT; // TO BE DEPRECATED
	public static final int NE_CERTAINTY_NEGATED = NE_POLARITY_NEGATION_PRESENT; // TO BE DEPRECATED
	public static final int NE_DIAG_STATUS_CONFIRMED = 0; // TO BE DEPRECATED
	public static final int NE_DIAG_STATUS_HISTORY_OF = 1; // TO BE DEPRECATED
	public static final int NE_DIAG_STATUS_FAM_HISTORY_OF = 2; // TO BE DEPRECATED
	public static final int NE_DIAG_STATUS_PROBABLE = 3; // TO BE DEPRECATED
	
	public static final String ATTR_SEVERITY_SEVERE = "severe";
	public static final String ATTR_SEVERITY_MODERATE = "moderate";
	public static final String ATTR_SEVERITY_SLIGHT = "slight";
	public static final String ATTR_SEVERITY_UNMARKED = "unmarked";

	public static final String ATTR_BODYSIDE_LEFT = "left";
	public static final String ATTR_BODYSIDE_RIGHT = "right";
	public static final String ATTR_BODYSIDE_BILATERAL = "bilateral";
	public static final String ATTR_BODYSIDE_UNMARKED = "unmarked";

	public static final String ATTR_BODYLATERALITY_SUPERIOR = "superior"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_INFERIOR = "inferior"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_DISTAL = "distal"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_PROXIMAL = "proximal"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_MEDIAL = "medial"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_LATERAL = "lateral"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_DORSAL = "dorsal"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_VENTRAL = "ventral"; // Attribute name may change
	public static final String ATTR_BODYLATERALITY_UNMARKED = "unmarked"; // Attribute name may change

	public static final String ATTR_HISTORYOF_INDICATOR_PRESENT = "historyOf_present";
	public static final String ATTR_HISTORYOF_INDICATOR_ABSENT = "historyOf_absent";	
	
	public static final String ATTR_SUBJECT_PATIENT = "patient";
	public static final String ATTR_SUBJECT_FAMILY_MEMBER = "family_member";
	public static final String ATTR_SUBJECT_DONOR_FAMILY_MEMBER = "donor_family_member";
	public static final String ATTR_SUBJECT_DONOR_OTHER = "donor_other";
	public static final String ATTR_SUBJECT_OTHER = "other";

	// ATTR_PROCEDURE_DEVICE -- any string
	// ATTR_PROCEDURE_METHOD -- any string
	
	// ATTR_LAB_VALUE_NUMBER -- any string
	// ATTR_LAB_VALUE_UNIT -- any string
	// ATTR_LAB_REFERENCE_RANGE -- any string
	
	public static final String ATTR_LAB_DELTAFLAG_CHANGEUP = "change_up";
	public static final String ATTR_LAB_DELTAFLAG_CHANGEDOWN = "change_down";
	public static final String ATTR_LAB_DELTAFLAG_NOCHANGE = "no_change";

	public static final String ATTR_LAB_ABNORMAL_TRUE = "abnormal";
	public static final String ATTR_LAB_ABNORMAL_VERYTRUE = "very_abnormal";

	public static final String ATTR_LAB_ORDINAL_RESISTANT = "resistant";
	public static final String ATTR_LAB_ORDINAL_POSITIVE = "positive";
	public static final String ATTR_LAB_ORDINAL_REACTIVE = "reactive";
	public static final String ATTR_LAB_ORDINAL_INTERMEDIATERESISTANCE = "intermediate_resistance";
	public static final String ATTR_LAB_ORDINAL_INTERMEDIATE = "intermediate";
	public static final String ATTR_LAB_ORDINAL_NEGATIVE = "negative";
	public static final String ATTR_LAB_ORDINAL_NOTDETECTED = "not_detected";
	public static final String ATTR_LAB_ORDINAL_DETECTED = "detected";
	public static final String ATTR_LAB_ORDINAL_WEAKLY_REACTIVE = "weakly_reactive";
	public static final String ATTR_LAB_ORDINAL_MODERATELYSUSCEPTIBLE = "moderately_susceptible";
	public static final String ATTR_LAB_ORDINAL_VERYSUSCEPTIBLE = "very_susceptible";
	public static final String ATTR_LAB_ORDINAL_SUSCEPTIBLE = "susceptible";
	public static final String ATTR_LAB_ORDINAL_1ORMORE = "1+";
	public static final String ATTR_LAB_ORDINAL_2ORMORE = "2+";
	public static final String ATTR_LAB_ORDINAL_3ORMORE = "3+";
	public static final String ATTR_LAB_ORDINAL_4ORMORE = "4+";
	public static final String ATTR_LAB_ORDINAL_SMALL = "small";
	public static final String ATTR_LAB_ORDINAL_TRACE = "trace";
	public static final String ATTR_LAB_ORDINAL_NORMAL = "normal";
	public static final String ATTR_LAB_ORDINAL_NO_OR_NA_RANGE = "no_range_or_normal_range_n/a";
	public static final String ATTR_LAB_ORDINAL_ANTICOMPLEMENTARYPRESENT = "anti_complementary_substance_present";
	public static final String ATTR_LAB_ORDINAL_CYTOTOXICPRESENT = "cytotoxic_substance_present";
	public static final String ATTR_LAB_ORDINAL_QUALITYCONTROLFAIL = "quality_control_failure";

	public static final boolean ATTR_LAB_ESTIMATED_FLAG_TRUE = true;
	public static final boolean ATTR_LAB_ESTIMATED_FLAG_FALSE = false;
	
	public static final String ATTR_MEDICATION_DOSAGE_1 = "1";
	public static final String ATTR_MEDICATION_DOSAGE_2 = "2";
	public static final String ATTR_MEDICATION_DOSAGE_3 = "3";
	public static final String ATTR_MEDICATION_DOSAGE_4 = "4";
	public static final String ATTR_MEDICATION_DOSAGE_5 = "5";
	public static final String ATTR_MEDICATION_DOSAGE_UNDETERMINED = "undetermined";
	public static final String ATTR_MEDICATION_DOSAGE_OTHER = "other";
	
	public static final String ATTR_MEDICATION_ROUTE_TOPICAL = "topical";
	public static final String ATTR_MEDICATION_ROUTE_ENTERAL_ORAL = "oral";
	public static final String ATTR_MEDICATION_ROUTE_ENTERAL_GASTRIC = "gastric";
	public static final String ATTR_MEDICATION_ROUTE_ENTERAL_RECTAL = "rectal";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_INTRAVENOUS = "intravenous";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_INTRAARTERIAL = "intra-arterial";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_INTRAMUSCULAR = "intramuscular";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_INTRACARDIAC = "intracardiac";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_SUBCUTANEOUS = "subcutaneous";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_INTRATHECAL = "intrathecal";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_INTRAPERIOTONEAL = "intraperiotoneal";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_TRANSDERMAL = "transdermal";
	public static final String ATTR_MEDICATION_ROUTE_PARENTERAL_TRANSMUCOSAL = "transmucosal";
	public static final String ATTR_MEDICATION_ROUTE_OTHER = "other";
	public static final String ATTR_MEDICATION_ROUTE_UNDETERMINED = "undetermined";

	public static final String ATTR_MEDICATION_FORM_AEROSOL = "aerosol";
	public static final String ATTR_MEDICATION_FORM_CAPSULE = "capsule";
	public static final String ATTR_MEDICATION_FORM_CREAM = "cream";
	public static final String ATTR_MEDICATION_FORM_ELIXIR = "elixir";
	public static final String ATTR_MEDICATION_FORM_EMULSION = "emulsion";
	public static final String ATTR_MEDICATION_FORM_ENEMA = "enema";
	public static final String ATTR_MEDICATION_FORM_GEL = "gel";
	public static final String ATTR_MEDICATION_FORM_IMPLANT = "implant";
	public static final String ATTR_MEDICATION_FORM_INHALANT = "inhalant";
	public static final String ATTR_MEDICATION_FORM_INJECTION = "injection";
	public static final String ATTR_MEDICATION_FORM_LIQUID = "liquid";
	public static final String ATTR_MEDICATION_FORM_LOTION = "lotion";
	public static final String ATTR_MEDICATION_FORM_LOZENGE = "lozenge";
	public static final String ATTR_MEDICATION_FORM_OINTMENT = "ointment";
	public static final String ATTR_MEDICATION_FORM_PATCH = "patch";
	public static final String ATTR_MEDICATION_FORM_PELLET = "pellet";
	public static final String ATTR_MEDICATION_FORM_PILL = "pill";
	public static final String ATTR_MEDICATION_FORM_POWDER = "powder";
	public static final String ATTR_MEDICATION_FORM_SHAMPOO = "shampoo";
	public static final String ATTR_MEDICATION_FORM_SOAP = "soap";
	public static final String ATTR_MEDICATION_FORM_SOLUTION = "solution";
	public static final String ATTR_MEDICATION_FORM_SPRAY = "spray";
	public static final String ATTR_MEDICATION_FORM_SUPPOSITORY = "suppository";
	public static final String ATTR_MEDICATION_FORM_SYRUP = "syrup";
	public static final String ATTR_MEDICATION_FORM_TABLET = "tablet";
	public static final String ATTR_MEDICATION_FORM_OTHER = "other";
	
	public static final String ATTR_MEDICATION_STATUSCHANGE_START = "start";
	public static final String ATTR_MEDICATION_STATUSCHANGE_STOP = "stop";
	public static final String ATTR_MEDICATION_STATUSCHANGE_INCREASE = "increase";
	public static final String ATTR_MEDICATION_STATUSCHANGE_DECREASE = "decrease";
	public static final String ATTR_MEDICATION_STATUSCHANGE_NOCHANGE = "noChange";
	
	public static final String ATTR_MEDICATION_ALLERGY_INDICATOR_PRESENT = "allergy_present";
	public static final String ATTR_MEDICATION_ALLERGY_INDICATOR_ABSENT = "allergy_absent";	

	// ATTR_MEDICATION_STRENGTH is any number/unit pair
	// ATTR_MEDICATION_DURATION is any string
	// ATTR_MEDICATION_FREQUENCY is any number/unit pair
	
	public static final int REL_DISCOVERY_TECH_GOLD_ANNOTATION = 1;

	public static final String MED_STATUS_CHANGE_START = ATTR_MEDICATION_STATUSCHANGE_START; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_STOP = ATTR_MEDICATION_STATUSCHANGE_STOP; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_INCREASEFROM = "increasefrom"; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_DECREASEFROM = "decreasefrom"; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_INCREASE = ATTR_MEDICATION_STATUSCHANGE_INCREASE; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_DECREASE = ATTR_MEDICATION_STATUSCHANGE_DECREASE; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_NOCHANGE = ATTR_MEDICATION_STATUSCHANGE_NOCHANGE; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_SUM = "add"; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_MAX = "maximum"; // TO BE DEPRECATED
	public static final String MED_STATUS_CHANGE_OTHER = "change"; // TO BE DEPRECATED
	

	public static final String TIME_CLASS_DATE = "DATE"; 

}
