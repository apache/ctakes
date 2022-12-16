Case Description||^[\t ]*CASE DESCRIPTION[^:]*:\s+
History of Present Illness||^[\t ]*(?:(?:HPI\/CC:)|(?:CC\/HPI:)|(?:S:)|(?:(?:HISTORY OF (?:THE )?(?:PRESENT |PHYSICAL )?ILLNESS)(?: \(HPI(?:, PROBLEM BY PROBLEM)?\))?[\t ]*:?))\s+
Past Medical History||^[\t ]*(?:(?:PMHX?)|(?:HISTORY OF (?:THE )?PAST ILLNESS)|(?:PAST MEDICAL HISTORY))[\t ]*:?\s+
Chief Complaint||^[\t ]*(?:CHIEF|PRIMARY) COMPLAINTS?[\t ]*:?\s+
Patient History||^[\t ]*(?:(?:PERSONAL|PATIENT) (?:(?:AND )?SOCIAL )?HISTORY)|(?:(?:PSYCHO)?SOC(?:IAL)? HISTORY)|(?:HISTORY (?:OF )(?:OTHER )?SOCIAL (?:FUNCTIONs?|FACTORS?))|(?:PSO)|(?:P?SHX)|(?:SOCHX)|(?:HISTORY:)[\t ]*:?\s+
Review of Systems||^[\t ]*(?:(?:ROS:)|(?:(?:REVIEW (?:OF )?SYSTEMS?)|(?:SYSTEMS? REVIEW)[\t ]*:?))\s+
Family Medical History||^[\t ]*(?:FAMILY (?:MEDICAL )?HISTORY)|(?:HISTORY (?:OF )?FAMILY MEMBER DISEASES?)|(?:FAM HX)|FH|FMH|FMHX|FAMHX|FHX[\t ]*:?\s+
Medications||^[\t ]*(?:(?:MEDS:)|(?:(?:CURRENT )?MEDICATIONS?))[\t ]*:?\s+
Allergies||^[\t ]*ALLERGIES[\t ]*:?\s+
General Exam||^[\t ]*(?:(?:PE:)|(?:O:)|(?:(?:REVIEW (?:OF )?)?(?:GENERAL(?: PHYSICAL)?|PHYSICAL) (?:EXAM(?:INATION)?|STATUS|APPEARANCE|CONSTITUTIONAL)S?(?: SYMPTOMS?)?[\t ]*:?))\s+
Vital Signs||^[\t ]*(?:VITS|(?:VITAL(?:S|(?: (?:SIGNS|NOTES)))))[\t ]*:?\s+
Identifying Data||^[\t ]*IDENTIFYING DATA[\t ]*:?\s+
Clinical History||^[\t ]*CLINICAL HISTORY[\t ]*:?\s+
Current Health||^[\t ]*CURRENT HEALTH(?: STATUS)?[\t ]*:?\s+
Narrative History||^[\t ]*NARRATIVE HISTORY[\t ]*:?\s+
Analysis of Problem||^[\t ]*(?:(?:A\/P:)|(?:ANALYSIS (?:OF )?(?:ADMIT(?:TING)? |IDENTIF(?:Y|IED) )?PROBLEMS?[\t ]*:?))\s+
Telemetry||^[\t ]*TELE(?:METRY)?[\t ]*:?\s+
Technical Comment||^[\t ]*TECHNICAL COMMENT[\t ]*:?\s+
Discharge Activity||^[\t ]*DISCHARGE ACTIVITY[\t ]*:?\s+
Occupational Environmental History||^[\t ]*OCCUPATION(?:AL)? ENVIRONMENT(?:AL)? HISTORY[\t ]*:?\s+
Immunosuppressants Medications||^[\t ]*(?:CYTOTOXIC )?IMMUN(?:OSUPPRESSANT|IZATION)S?(?: MEDICATIONS?)?(?: ADMINISTRATION HISTORY)?[\t ]*:?\s+
Medications Outside Hospital||^[\t ]*MEDICATIONS? (?:AT )?OUTSIDE HOSPITAL[\t ]*:?\s+
Reason for Consult||^[\t ]*REASON (?:FOR )?(?:CONSULT(?:ATION)?|REFERRAL)(?: ?\/? ?QUESTIONS?)?[\t ]*:?\s+
Problem List||^[\t ]*(?:SIGNIFICANT )?PROBLEMS?(?: LIST)?[\t ]*:?\s+
Living Situation||^[\t ]*LIV(?:E|ING) SITUATION[\t ]*:?\s+
Cytologic Diagnosis||^[\t ]*CYTOLOGIC (?:DIAGNOSIS|DX)[\t ]*:?\s+
Discharge Instructions||^[\t ]*DISCHARGE INSTRUCTIONS?[\t ]*:?\s+
Body Surface Area||^[\t ]*(?:(?:BODY SURFACE AREA)|BSA)[\t ]*:?\s+
Discharge Condition||^[\t ]*(?:(?:DISCHARGE CONDITION)|(?:CONDITION (?:(?:AT|ON) )?DISCHARGE))[\t ]*:?\s+
Diagnosis at Death||^[\t ]*(?:(?:DIAGNOSIS|DX|CAUSE) (?:AT |OF )?DEATH)|COD[\t ]*:?\s+
Adverse Reactions||^[\t ]*(?:HISTORY (?:OF )?)?(?:(?:ALLERG(?:Y|IES)(?:(?:\/| AND )?ADVERSE REACTIONS?)?)|(?:ADVERSE REACTIONS?(?:(?:\/| AND )?ALLERG(?:Y|IES)?)?))( DISORDERS?)?(?: HISTORY)?[\t ]*:?\s+
Emergency Department Course||^[\t ]*(?:EMERGENCY|ED) (?:DEPARTMENT|ROOM) (?:COURSE|MANAGEMENT)[\t ]*:?\s+
Consultation Attending||^[\t ]*CONSULTATION ATTENDING[\t ]*:?\s+
Body Length||^[\t ]*(?:BODY )?LENGTH[\t ]*:?\s+
Past Surgical History||^[\t ]*(?:(?:SURGHX)|(?:(?:PAST|PREVIOUS|PRIOR)? ?(?:SURG(?:ERY|ICAL)?|OPERATIVE|SIGNIFICANT) (?:HISTORY|HX|PROCEDURES?)))[\t ]*:?\s+
Height||^[\t ]*(?:HEIGHT|HT)[\t ]*:?\s+
Principle Diagnosis||^[\t ]*(?:PRINCI(?:PLE|PAL)|MAIN|PRIMARY) (?:DIAGNOSI?E?S|DX)[\t ]*:?\s+
Oxygen Saturation||^[\t ]*(?:OXYGEN|O2) ?SAT(?:URATION)?[\t ]*:?\s+
Principal Procedures||^[\t ]*(?:PRINCIPAL|PRIMARY) PROCEDURES?[\t ]*:?\s+
Psychological Stressors||^[\t ]*(?:PSYCHOLOGICAL )?STRESS(?:ORS?| LEVEL)[\t ]*:?\s+
Pathologic Data||^[\t ]*(?:(?:BIOPS(?:Y|IES))|BX|(?:PATHOLOGIC DATA))[\t ]*:?\s+
Medication History||^[\t ]*(?:(?:MEDICATION ?(?:TREATMENT|USE)? HISTORY)|(?:HISTORY (?:OF )?MEDICATION ?(?:TREATMENT|USE)?))[\t ]*:?\s+
Treatment Goals||^[\t ]*(?:TREATMENT )?GOALS?[\t ]*:?\s+
Input Fluids||^[\t ]*INPUT FLUIDS?[\t ]*:?\s+
Admission Date||^[\t ]*ADMISSION DATE[\t ]*:?\s+
History Source||^[\t ]*(?:HISTORY|HX) (?:SOURCES?|(?:(?:OBTAIN(?:ED)?(?: FROM)?)))[\t ]*:?\s+
Current Pregnancy||^[\t ]*CURRENT PREGNANCY[\t ]*:?\s+
Special Procedures||^[\t ]*SPECIAL PROCEDURES?[\t ]*:?\s+
Operative Findings||^[\t ]*OPERATIVE FINDINGS?[\t ]*:?\s+
Fluid Balance||^[\t ]*(?:(?:FLUID BALANCE)|(?:I(?:NPUT)? ?\/? ?O(?:UTPUT)?))[\t ]*:?\s+
Blood Pressure||^[\t ]*(?:BLOOD PRESSURE|BP)[\t ]*:?\s+
Post Procedure Diagnosis||^[\t ]*POST\-?(?:PROCEDURE|OP|OPERATIVE) DIAGNOSIS[\t ]*:?\s+
Final Diagnosis||^[\t ]*FINAL DIAGNOSIS[\t ]*:?\s+
Cancer Risk Factors||^[\t ]*CANCER RISK FACTORS?[\t ]*:?\s+
Invasive Diagnostic Procedure History||^[\t ]*INVASIVE DIAGNOSTIC PROCEDURE HISTORY[\t ]*:?\s+
Technique||^[\t ]*TECHNIQUE[\t ]*:?\s+
Medications By Type||^[\t ]*MEDICATIONS? (?:BY )?TYPE[\t ]*:?\s+
Hematologic History||^[\t ]*(?:HEME|HEMATOLOGIC) (?:HISTORY|HX)[\t ]*:?\s+
Respiratory Rate||^[\t ]*RESP(?:IRAT(?:ORY|IONS?))?(?: RATE)?[\t ]*:?\s+
Attending Addendum||^[\t ]*ATTENDING ADDENDUM[\t ]*:?\s+
Gross Description||^[\t ]*GROSS DESC(?:RIPTION)?[\t ]*:?\s+
Microscopic Description||^[\t ]*MICROSCOPIC DESC(?:RIPTION)?[\t ]*:?\s+
Substance Abuse Treatment||^[\t ]*(?:TREATMENT (?:FOR|OF)? ?)?(?:SUBSTANCE|DRUG|ALCOHOL) (?:ABUSE|ADDICTION)(?: TREATMENT)?[\t ]*:?\s+
Hospital Course||^[\t ]*(?:BRIEF|HISTORY|HX)? ?HOSPITAL COURSE[\t ]*:?\s+
Histology Summary||^[\t ]*HISTO(?:LOGY)? (?:TISSUE )?SUMMARY[\t ]*:?\s+
Addendum||^[\t ]*ADDEND(?:A|UM)[\t ]*:?\s+
Medications at Transfer||^[\t ]*MEDICATIONS?(?: AT)? TRANSFER[\t ]*:?\s+
Findings||^[\t ]*(?:(?:DIAGNOSTIC )?(?:INDICATIONS? ?\/? )?FINDINGS?(?: (?:AT )?SURGERY)?)|(?:INDICATIONS?:)[\t ]*:?\s+
Instructions||^[\t ]*INSTRUCTIONS?[\t ]*:?\s+
Current Antibiotics||^[\t ]*CURRENT ANTIBIOTICS?[\t ]*:?\s+
Ethanol Use||^[\t ]*(?:HISTORY (?:OF )?)?(?:ALCOHOL|ETHANOL|ETOH)(?: USE)?[\t ]*:?\s+
Maximum Temperature||^[\t ]*MAX(?:IMUM)? TEMP(?:ERATURE)?[\t ]*:?\s+
Smoking Use||^[\t ]*(?:SMOKING|CIGAR(?:ETTE)?)(?: USE)?[\t ]*:?\s+
Admission Condition||^[\t ]*(?:(?:CONDITION(?:ON |AT )? ADMISSION)|(?:ADMISSION CONDITION))[\t ]*:?\s+

// The following are incomplete
Other Systems Reviewed||^[\t ]*OTHER SYSTEMS REVIEWED[\t ]*:?\s+
Objective||^[\t ]*OBJECTIVE[\t ]*:?\s+
Impression||^[\t ]*IMPRESSION[\t ]*:?\s+
Diagnosis||^[\t ]*DIAGNOS(?:I|E)S[\t ]*:?\s+
Plan||^[\t ]*(?:ASSESSMENT AND )?PLAN[\t ]*:?\s+
Labs||^[\t ]*LABS?\/(?:ANC(?:ILLARY)?|STUDIES)[\t ]*:?\s+
Diet||^[\t ]*DIET[\t ]*:?\s+
Vaccinations||^[\t ]*VACCINATIONS?[\t ]*:?\s+
Past Medications||^[\t ]*PAST MEDICATIONS?[\t ]*:?\s+

// The following are not clinical document sections, but allow skipping of unwanted text
XML||\A<\?xml (?:[^>]*>)*
Past MS Medications||^[\t ]*Past MS Medications?[\t ]*:?\s+