/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.medfacts.zoner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author robyn
 */
public class MayoZoneTranslator {
    
    // we will take a document that has MAYO [section] indicators and create
    // a RangeList like ZonerCli would create.  The ranges will include the 
    // Mayo section headers just as the Zoner's ranges include the header strings.
    
    private static final Map<String, String> ZONE_MAP;

    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("20100", "transcription details"); // Revision History
        aMap.put("20101", "attending physician");   // Referral Source
        aMap.put("20102", "chief complaint");       // Chief Complaint / Reason for Visit
        aMap.put("20103", "history of present illness"); // History of Present Illness
        aMap.put("20104", "medication");            // Current Medications
        aMap.put("20105", "allergy");               // Allergies
        aMap.put("20106", "review of systems");     // System Reviews
        aMap.put("20107", "past medical history");  // Past Medical / Surgical History
        aMap.put("20108", "social history");        // Social History
        aMap.put("20109", "family history");        // Family History 
        aMap.put("20110", "physical examination");  // Vital Signs
        aMap.put("20111", "physical examination");  // Physical Examination
        aMap.put("20112", "impression/plan");       // Impression / Report / Plan
        aMap.put("20113", "diagnoses");             // Diagnosis
        aMap.put("20114", "transcription details"); // Administrative
        aMap.put("20115", "instructions");          // Special Instructions
        aMap.put("20116", "instructions");          // Advance Directives
        aMap.put("20117", "service specialty");     // Service Contributors  
        aMap.put("20118", "other treatments/procedures"); // Immunizations
        aMap.put("20119", "clinical findings");     // Admission Findings / Test Results 
        aMap.put("20120", "hospital course");       // Problem Oriented Hosp. Course (I/O/P)
        aMap.put("20121", "physical examination");  // Final Physical Examination
        aMap.put("20122", "complications");         // Adverse Reactions 
        aMap.put("20123", "diet");                  // Diet / Nutrition
        aMap.put("20124", "condition");             // Discharge Condition 
        aMap.put("20125", "condition");             // Condition at Discharge
        aMap.put("20126", "follow-up");             // Ongoing Care Orders  
        aMap.put("20127", "physical examination");  // Admission Physical Exam
        aMap.put("20128", "follow-up");             // Ongoing Care
        aMap.put("20129", "follow-up");             // Follow Up Agreements
        aMap.put("20130", "transcription details"); // PFH and CVI Dates
        // there is no 20131
        aMap.put("20132", "operations/procedures"); // Findings/Procedures/Surgeries
        aMap.put("20133", "medication");            // Admission Medications 
        aMap.put("20134", "discharge activity");    // Discharge Activity
        aMap.put("20135", "instructions");          // Anticipated Problems and Suggested Interventions
        aMap.put("20136", "follow-up");             // Post op services related to Surgical Procedure
        aMap.put("20137", "instructions");          // EMTALA Statement
        aMap.put("20138", "instructions");          // Patient Education
        aMap.put("20139", "diagnoses");             // Principle Diagnosis
        aMap.put("20140", "diagnoses");             // Secondary Diagnoses
        aMap.put("20141", "diagnoses");             // Final Pathology Diagnosis
        aMap.put("20142", "attending physician");   // Supervisor Present
        aMap.put("20143", "procedure description"); // Pre-Procedure Information
        aMap.put("20144", "procedure description"); // Precedure Information
        aMap.put("20145", "subjective");            // Subjective
        aMap.put("20146", "laboratory");            // Labs
        aMap.put("20147", "diagnoses");             // Wound
        aMap.put("20148", "instructions");          // Informed Consent 
        aMap.put("20149", "radiology");             // Post-Procedure Film
        aMap.put("20150", "disposition");           // Patient Disposition
        aMap.put("20151", "laboratory");            // Specimins Removed
        aMap.put("20152", "clinical findings");     // Findings  
        aMap.put("20153", "attending physician");   // Primary Surgeon(s)
        aMap.put("20154", "attending physician");   // Assistants
        aMap.put("20155", "complications");         // Complications
        aMap.put("20156", "other treatments/procedures"); // Drains
        aMap.put("20157", "other treatments/procedures"); // Anesthesia 
        aMap.put("20158", "other treatments/procedures"); // Fluids
        aMap.put("20159", "clinical findings");     // Estimated Blood Loss
        aMap.put("20160", "medication");            // Prescriptions
        aMap.put("20161", "follow-up");             // Follow-Up Letter
        aMap.put("20162", "work return");           // May Return to Work
        aMap.put("20163", "instructions");          // Instructions to Include  
        aMap.put("20164", "procedure description"); // Procedure Performed and Description
        aMap.put("20165", "diagnoses");             // Post-Procedure Diagnosis
        aMap.put("20166", "reason for hospitalization"); // Reason for Transfer
        aMap.put("20167", "condition");             // Condition at Transfer  
        aMap.put("20168", "other treatments/procedures"); // Summary of Care
        aMap.put("20169", "attending physician");   // Physician Attestation
        aMap.put("20170", "hospital course");       // Brief Hospital Course
        // there is no 20171
        aMap.put("20172", "condition");             // Condition at Discharge
        aMap.put("20173", "disposition");           // Discharge Disposition  
        aMap.put("20174", "transcription details"); // Contact Information
        aMap.put("20175", "attending physician");   // Service Contributor / SUM2
        aMap.put("20176", "instructions");          // Instructions for Continuing care
        // There is no 20177
        aMap.put("20178", "consultants");           // Consults
        aMap.put("20179", "radiology");             // Radiological Review
        
        ZONE_MAP = Collections.unmodifiableMap(aMap);
    }

    
}
