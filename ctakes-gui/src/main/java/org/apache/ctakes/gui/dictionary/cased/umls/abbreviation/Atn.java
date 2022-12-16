package org.apache.ctakes.gui.dictionary.cased.umls.abbreviation;

/**
 * Attribute Names
 * https://www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/attribute_names.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Atn {
   AAL_TERM,   //AAL term
   ACCEPTABILITYID,   //Acceptability Id
   ACCEPTED_THERAPEUTIC_USE_FOR,   //Accepted therapeutic use for
   ACTIVE,   //Active
   ADDED_MEANING,   //Additional descriptive information
   ADDITIONAL_GUIDELINE,   //Additional explanatory text that is applicable to a concept (code/heading/subheading).
   ADDON_CODE,   //A "T" in this field indicates that it is an "Add-on" code, i.e. it is commonly carried out in addition to the primary procedure performed
   AMBIGUITY_FLAG,   //Source atom ambiguity flag
   AMT,   //AOT uses MeSH term
   ANADA,   //Abbreviated New Animal Drug application number for the generic drug for MTHSPL
   ANATOMICAL_COORDINATE,   //Anatomical coordinate
   ANDA,   //Abbreviated New (Generic) Drug application number for the MTHSPL drug
   ANSWER_CODE_SYSTEM,   //Answer code system
   ANSWER_CODE,   //Answer code
   ANSWER_LIST_ID,   //Answer list ID
   ANSWER_LIST_NAME,   //Answer list name
   AN,   //MeSH Annotation - an informative MeSH note written primarily for indexers or catalogers that may also be useful in explaining the use of a MeSH term to online searchers.
   AQL,   //MeSH Allowable Qualifier - list of allowable qualifier abbreviations for MeSH main headings (e.g. AA, CL, CS, DF, DU, IM, I,P ME, PK)
   ASK_AT_ORDER_ENTRY,   //Ask at Order Entry (AOE) observations for a clinical observation or laboratory test
   ASSOC_OBSERVATIONS,   //Associated observations
   ATC_LEVEL,   //ATC LEVEL
   ATTRIBUTEDESCRIPTION,   //Attribute Description
   ATTRIBUTEORDER,   //Attribute Order
   ATTRIBUTETYPE,   //Attribute Type
   AUTHORITY,   //Authority
   BIOCARTA_ID,   //BioCarta online maps of molecular pathways, adapted for NCI use ID
   BLA,   //Therapeutic Biologic Applications number for the MTHSPL drug
   BLOCK,   //Block code
   BRAININFO_URL,   //URL of the central directory page in BrainInfo
   CASE_SIGNIFICANCE_ID,   //Case significance ID
   CAS_REGISTRY,   //CAS Registry
   CCDS_ID,   //CCDS ID
   CCF,   //Canonical Clinical Problem Statement System (CCPSS) frequency - the number of times a CCPSS term appears in a patient record.
   CCI,   //ICD-9-CM code(s) clusters in a Clinical Classifications Software (CCS) category - individual ICD-9-CM codes (or ranges of such codes) classified into CCS categories.
   CELL_APPENDAGE_TYPE,   //Cell appendage type
   CFR,   //Code of Federal Regulation Number (e.g. 862.3220, 892.1610)
   CHANGE_CLASS,   //Identifies the type of change made to this version of the Code (i.e., changes from the prior version published in "CDT-2011-2012"), with the following meanings: 30 = No change to existing code, nomenclature or descriptor; 31 = New procedure code and nomenclature, and descriptor, if present; 35 = Revision in the nomenclature or descriptor (or both), or to indicate a revision to an entire subcategory.
   CHANGE_REASON_PUBLIC,   //Detailed explanation about special changes to the term over time.
   CHAPTER,   //Chapter code
   CHARACTERISTIC_TYPE_ID,   //Characteristic type ID
   CHEBI_ID,   //CHEBI ID
   CHEMICAL_FORMULA,   //Chemical Formula
   CHROMOSOME_PAIRS_PER_NUCLEUS,   //Chromosome pairs per nucleus
   CITATION,   //Citation
   CMA_LABEL,   //Center for Morphological Analysis
   CNIDTYPE,   //cNIDType - identifies whether an item is in the classical hierarchy of brains structures (value = h) or not (value = a for ancillary.Useful only to users of NeuroNames versions earlier than 2009.
   CNID,   //cNID - ID number useful only to users of NeuroNames versions earlier than 2009.
   CNU,   //ISO 3166-1 standard country code in numeric (three-digit) format
   COATING,   //FDA Structured Product Label imprint attribute for coating
   CODE_ALSO,   //Instructs that 2 codes may be required to fully describe a condition but the sequencing of the two codes is discretionary, depending on the severity of the conditions and the reason for the encounter.
   CODE_FIRST,   //Certain conditions have both an underlying etiology and multiple body system manifestations due to the underlying etiology. ICD-10-CM has a coding convention that requires the underlying condition be sequenced first followed by the manifestation. Wherever such a combination exists there is a "code first" note at the manifestation code. These instructional notes indicate the proper sequencing order of the codes, etiology followed by manifestation. In most cases the manifestation codes will have in the code title, "in diseases classified elsewhere." Codes with this title are a component of the etiology/manifestation convention. The code title indicates that it is a manifestation code. "In diseases classified elsewhere" codes are never permitted to be used as first listed or principle diagnosis codes. They must be used in conjunction with an underlying condition code and they must be listed following the underlying condition.
   CODING_TIP,   //Coding Tip
   COLORTEXT,   //FDA Structured Product Label imprint attribute for color text
   COLOR,   //FDA Structured Product Label imprint attribute for color
   COMBO_SCORE_NO_TOP_WORDS,   //Combo Score that has no top words
   COMBO_SCORE,   //Combo Score
   COMMENT,   //Comment
   COMMON_ORDER_RANK,   //Ranking of approximately 300 common orders performed by laboratories in USA
   COMMON_SI_TEST_RANK,   //Corresponding SI terms for 2000 common tests performed by laboratories in USA
   COMMON_TEST_RANK,   //Numeric ranking of approximately 2,000 common tests performed by hospitals
   CONCEPT_TYPE,   //Concept type
   CONDITIONAL_NADA,   //Conditional Nada
   CONSIDER,   //Codes to consider before definitive diagnosis
   CONSUMER_NAME,   //An experimental (beta) consumer friendly name
   CONTEXT_SCORE,   //Context Score
   CONTRIBUTING_AUTHOR,   //contributing author
   COPYRIGHT,   //Copyright
   CORRELATIONID,   //Correlation Id
   CPF,   //CPT Full Description - complete text of the CPT full description, in cases where the CPT term in the "STR" field of MRCON has been trimmed from its original form.
   CPTLINK_CONCEPT_ID,   //CPT Link concept identifier
   CPT_LEVEL,   //For headings, a value ranging from H1 (a top-level heading such as Surgery) to H6 (a 6th-level subheading) and HS (a heading for a small family of related codes). For CPT codes, either PC for Parent Code or CC for Child Code.
   CTV3ID,   //The Read Code for a SNOMED CT concept taken from the United Kingdom?s Clinical Terms Version 3 terminology.
   CUI_SCORE,   //CUI Score
   CV_ALGORITHM,   //Content view algorithm
   CV_CATEGORY,   //Content view category
   CV_CLASS,   //Content view class
   CV_CODE,   //Content view code
   CV_CONTRIBUTOR_DATE,   //Date corresponding to the contributor version of this concept view
   CV_CONTRIBUTOR_URL,   //URL corresponding to the contributor version of this concept view
   CV_CONTRIBUTOR_VERSION,   //Version of this content view submitted by the contributor
   CV_CONTRIBUTOR,   //Content view contributor
   CV_DESCRIPTION,   //Content view description
   CV_INCLUDE_OBSOLETE,   //Content view includes obsolete data: YN
   CV_IS_GENERATED,   //Content view generated: Y/N
   CV_MAINTAINER_DATE,   //Date corresponding to the maintainer version of this concept view
   CV_MAINTAINER_URL,   //URL corresponding to the maintainer version of this concept view
   CV_MAINTAINER_VERSION,   //Version of this content view submitted by the maintainer
   CV_MAINTAINER,   //Content view maintainer
   CV_MEMBER,   //Describes the membership for an atom belonging to a particular content view. The value for this attribute is a tilde-delimited microsyntax. The first field is the content view bitflag (e.g. 2048), followed by field name-value pairs describing the member.
   CV_PREVIOUS_META,   //Previous UMLS Metathesaurus version used to generate content view. A null value means the content view is generated based on current UMLS Metathesaurus version.
   CX,   //MeSH Consider Also Note - other word roots or prefixes that should be consulted for concepts related to this MeSH concept, e.g., the value for "Heart" is "consider also terms at cardi- and myocardi-".
   CYTOGENETIC_LOCATION,   //Cytogenetic location of the gene
   DATE_CREATED,   //Date created
   DATE_FIRST_PUBLISHED,   //Date first published
   DATE_LAST_MODIFIED,   //Date last modified
   DATE_MODIFIED,   //Date Modified
   DATE_NAME_CHANGED,   //Date name changed
   DATE_SYMBOL_CHANGED,   //Date symbol changed
   DAYS_POST_FERTILIZATION,   //Days post fertilization
   DAYS_RESTRICTION,   //Days restriction
   DA,   //Metathesaurus Date of entry - YYYYMMDD, e.g., 19920830 - date of entry of the concept into the Metathesaurus.
   DB_XR_ID,   //Database cross-reference ID
   DCSA,   //Controlled Substance Act designation code (e.g. 0,2,3n)
   DC,   //MeSH Descriptor class - type of MeSH term the concept name represents.
   DDFA,   //Drug Doseform Abbreviation (e.g. SOLN)
   DDF,   //Drug Doseform (e.g. chewable tablet)
   DEFAULT_LANGUAGECODE,   //Default language code value
   DEFAULT_MODULE_ID,   //Default module ID
   DEFINITION_STATUS_ID,   //Definition status ID
   DESCRIPTIONFORMAT,   //Description Format
   DESCRIPTIONLENGTH,   //Description Length
   DESCRIPTION_FORMAT,   //Description format
   DESCRIPTION_LENGTH,   //Description length
   DESCRIPTOR_ELEMENT,   //Elements of a multi-paragraph descriptor, such as, E&M, lab panels, and molecular pathology
   DESCRIPTOR_EXTENSION,   //Extension of full description of the concept
   DESIGN_NOTE,   //Design note.
   DESI_DESC,   //Source drug efficacy study implementation indicator
   DEVTYPE,   //Device Type
   DHJC,   //Multum HCPCS J-code Multum clinical drugs linked to HCPCS J-codes where applicable (e.g. J7507)
   DID,   //Descriptor Identifier
   DIMENSION,   //Dimension code
   DISJOINT_FROM,   //Disjoint from
   DISPARAGED,   //Disparaged
   DIV,   //NCBI Division/Phyla (e.g. DIV[NCBI]Viruses)
   DM_SPL_ID,   //DailyMed internal identifier for MTHSPL atom
   DPC,   //Multum Pregnancy Hazard Classification Code assigned to Multum clinical drugs. (e.g. X, D)
   DQ,   //MeSH Date Qualifier Established YYYYMMDD - date the qualifier became available for indexing MEDLARS citations.
   DRTA,   //Drug Route of Administration Abbreviation (e.g. INJ)
   DRT,   //Drug Route of Administration (e.g. Injection (systemic) )
   DRUG_CLASS_TYPE,   //VA Drug class type - Values can be "Major","Minor","Sub Class", or "0","1","2" respectively
   DST,   //Drug Strength (e.g. 0.01%, 0.02 MG, 0.02 MG/ML)
   DX,   //MeSH Date major descriptor established YYYYMMDD - first day of the Index Medicus publication month in which the descriptor (in any form) was available for searching as a major descriptor.
   EC,   //Specifies the MeSH main heading to be used instead of the Descriptor/Qualifier combination specified in the EC_OF attribute.
   EFFECTIVE_TIME,   //Effective time
   ENA,   //International Nucleotide Sequence Database Collaboration accession number(s)
   ENSEMBLGENE_ID,   //Ensembl gene ID
   ENTREZGENE_ID,   //EntrezGene ID
   EPONYM,   //Eponym
   ESSENTIAL_AMINO_ACID,   //Essential Amino Acid
   ESSENTIAL_FATTY_ACID,   //Essential Fatty Acid
   EUPROSTATE16,   //Euprostate 16
   EUPROSTATE27,   //Euprostate 27
   EXAMPLE_UCUM_UNITS,   //The Unified Code for Units of Measure (UCUM)
   EXAMPLE_UNITS,   //Example units
   EXCLUDES1,   //A pure excludes. It means "NOT CODED HERE!" Indicates that the code excluded should never be used at the same time as the code above the Excludes1 note. Used for when two conditions cannot occur together, such as a congenital form versus an acquired form of the same condition.
   EXCLUDES2,   //Represents "Not included here". Indicates that the condition excluded is not part of the condition it is excluded from but a patient may have both conditions at the same time. When an Excludes2 note appears under a code it is acceptable to use both the code and the excluded code together.
   EXCLUDE_DI_CHECK,   //Exclude drug interaction check
   EXEMPT_DEVICE,   //Exempt Device
   EXEMPT_MOD,   //Contains 51 or 63 for codes where Modifier 51 or 63 is Exempt
   EXPORT_ONLY,   //Export Only
   EXTENSIBLE_LIST,   //Extensible List
   EXTERNAL_COPYRIGHT_LINK,   //External copyright link
   EZ,   //Enzyme Commission Number - International Union of Biochemists Enzyme Commission number for an enzyme concept.
   FDA_APPROVAL_PENDING,   //T for codes marked with the FDA approval pending symbol
   FDA_TABLE,   //FDA Table
   FDA_UNII_CODE,   //FDA UNII Code
   FIRST_IN_SUBSET,   //Version of subset first containing this concept
   FOURTHLEVEL,   //Fourth Level code
   FREESURFER_TERM,   //Freesurfer term
   FREQUENCY,   //Frequency
   FROMRSAB,   //Root source abbreviation for the "from" identifiers of a map set
   FROMVSAB,   //Versioned source abbreviation for the "from" identifiers of a map set
   FR,   //MeSH Frequency
   FX,   //MeSH MH Mapping - maps a MeSH MH to a 'See Related' MH.
   GENBANK_ACCESSION_NUMBER,   //GenBank Accession Number
   GENELOCUS,   //Gene Locus
   GENESYMBOL,   //Gene Symbol
   GENE_ENCODES_PRODUCT,   //Gene Encodes Product
   GENE_FAM_DESC,   //Gene family description
   GENE_FAM_ID,   //ID used to designate a gene family or group
   GESTATION_AGE_WEEKS,   //Gestation Age Weeeks
   GLOBAL_PERIOD_CODE,   //Global period code
   GO_COMMENT,   //GO Comment field data
   GO_NAMESPACE,   //Go Namespace field data
   GO_SUBSET,   //Go Subset field data
   GUIDELINE,   //Guideline
   GXR,   //GO Cross Reference to external databases (e.g. MetaCyc:TRNA-CHARGING-PWY)
   HAC,   //HCPCS action code - code denoting the change made to a procedure or modifier code within the HCPCS system.
   HAD,   //HCPCS Action Effective Date - effective date of action to a procedure or modifier code.
   HAQ,   //HCPCS Anesthesia Base Unit Quantity - base unit represents the level of intensity for anesthesia procedure services that reflects all activities except time.
   HAS_BOUNDARY,   //Has boundary
   HAS_DIMENSION,   //Has dimension
   HAS_DIRECT_CELL_LAYER,   //Has direct cell layer
   HAS_DIRECT_PLOIDY,   //Has direct ploidy
   HAS_DIRECT_SHAPE_TYPE,   //Has direct shape type
   HAS_INHERENT_3D_SHAPE,   //has inherent 3d shape
   HAS_MASS,   //Has mass
   HBT,   //HCPCS Berenson-Eggers Type of Service Code - BETOS for the procedure code based on generally agreed upon clinically meaningful groupings of procedures and services.
   HCC,   //HCPCS Coverage Code - code denoting Medicare coverage status. There are two subelements separated by "=".
   HCD,   //HCPCS Code Added Date - year the HCPCS code was added to the HCFA Common Procedure Coding System.
   HCO,   //HL7 Comment
   HGNC_ID,   //HGNC ID
   HIR,   //HCPCS Coverage Issues Manual Reference Section Number - number identifying the Reference Section of the Coverage Issues Manual.
   HIT,   //HL7 Interpretation: German Interpretation according to the German HL/ user group
   HL7AI,   //HL7 approved indicator
   HL7AP,   //HL7 associated concept property
   HL7AR,   //HL7 package artifact
   HL7AT,   //HL7 applies to
   HL7BR,   //HL7 binding realm name
   HL7CC,   //HL7 complete codes indicator
   HL7CD,   //HL7 conductible
   HL7CI,   //HL7 package combined Id
   HL7CO,   //HL7 contributor
   HL7CP,   //HL7 copyright owner
   HL7CSC,   //HL7 conceptual space for class code
   HL7CST,   //HL7 coding strength
   HL7CS,   //HL7 code status
   HL7CY,   //HL7 copyright years
   HL7DC,   //HL7 is document characteristic
   HL7DH,   //HL7 default handling code
   HL7DI,   //HL7 deprecation info
   HL7DK,   //HL7 definition kind
   HL7DV,   //HL7 default value
   HL7ED,   //HL7 effective date
   HL7HA,   //HL7 how applies
   HL7HI,   //HL7 history item
   HL7ID,   //HL7 internal Id
   HL7II,   //HL7 is immutable
   HL7IM,   //HL7 is mandatory indicator
   HL7IN,   //HL7 inverse name
   HL7IR,   //HL7 inverse relationship
   HL7IS,   //HL7 is selectable
   HL7LT,   //HL7 license terms
   HL7MI,   //HL7 maintained indicator
   HL7MN,   //HL7 vocabulary model name
   HL7NA,   //HL7 is navigable
   HL7NO,   //HL7 notation
   HL7OAN,   //HL7 other annotation
   HL7OA,   //HL7 owning affiliate
   HL7OD,   //HL7 OID
   HL7OI,   //HL7 open issue
   HL7PK,   //HL7 package kind
   HL7PL,   //HL7 preferred for language
   HL7PR,   //HL7 package root
   HL7PV,   //HL7 publisher version Id
   HL7RA,   //HL7 rendering application
   HL7RD,   //HL7 release date
   HL7RF,   //HL7 reflexivity
   HL7RG,   //HL7 responsible group organization name
   HL7RK,   //HL7 relationship kind
   HL7RN,   //HL7 package realm namespace
   HL7RT,   //HL7 rendering time
   HL7SCS,   //HL7 supported code system
   HL7SL,   //HL7 supported language
   HL7SV,   //HL7 schema version
   HL7SY,   //HL7 symmetry
   HL7TI,   //HL7 title
   HL7TR,   //HL7 transitive
   HL7TY,   //HL7 property type
   HL7VC,   //HL7 value set content
   HL7VD,   //HL7 version date
   HL7VE,   //HL7 package version
   HL7VP,   //HL7 versioning policy
   HL7XN,   //HL7 xml namespace
   HL7_ATTACHMENT_REQUEST,   //HL7 attachment request
   HL7_ATTACHMENT_STRUCTURE,   //HL7 attachment structure
   HL7_FIELD_SUBFIELD_ID,   //A value which indicates the content should be delivered in the named field/subfield of the HL7 message. When NULL, the data should be sent in an OBX segment with this LOINC code stored in OBX-3 and with the value in the OBX-5
   HLC,   //HCPCS Lab Certification Code - code used to classify laboratory procedures according to the specialty certification categories listed by CMS(formerly HCFA).
   HMP,   //HCPCS Multiple Pricing Indicator Code - code used to identify instances where a procedure could be priced.
   HMR,   //HCPCS Medicare Carriers Manual reference section number - number identifying a section of the Medicare Carriers Manual.
   HM,   //MeSH Heading Mapped To - heading mapped to attribute in C-MeSH containing repeating (MH or MH/SH) elements (e.g. HM = PYRROLIDINONES, HM = *TARTRATES, HM = ESTRONE/* analogs & derivatives)
   HN,   //History Note - for MeSH history notes, the year when the current form of the MeSH term was established as a major and/or minor descriptor.
   HOMOLOGOUS_GENE,   //Homologous Gene
   HPD,   //HCPCS ASC payment group effective date - date the procedure is assigned to the ASC payment group.
   HPG,   //HCPCS ASC payment group code which represents the dollar amount of the facility charge payable by Medicare for the procedure.
   HPI,   //HCPCS Pricing Indicator Code - used to identify the appropriate methodology for developing unique pricing amounts under Part B.
   HPN,   //HCPCS processing note number identifying the processing note contained in Appendix A of the HCPCS Manual.
   HPO_COMMENT,   //HPO Comment
   HSE,   //HL7 Section: Chapter
   HSNU,   //HL7 Sort Number: Number for sorting the values according to the official document
   HSN,   //HCPCS Statute Number identifying statute reference for coverage or noncoverage of procedure or service.
   HTA,   //HL7 Anchor: HTML-Reference to the Standard
   HTD,   //HCPCS Termination Date - last date for which a procedure or code may be used by Medicare Providers.
   HTG,   //HL7 Generate: Yes/No
   HTID,   //HL7 Table ID
   HTS,   //HCPCS Type of Service Code - carrier assigned HCFA Type of Service which describes the particular kind(s) of service represented by the procedure code.
   HTT,   //HL7 Table Type: Number specifying the type of this table (user defined, HL7 defined, ..)
   HUS,   //HL7 Usage: How is this data element used (Original, Added, Deleted)
   HXR,   //HCPCS Cross reference code - an explicit reference crosswalking a deleted code or a code that is not valid for Medicare to a valid current code (or range of codes).
   IAA,   //ICD10AM Abbreviated Descriptor
   IAC,   //ICD10AM Australian Code - Contains a flag "1" for codes that have been exclusively developed in Australia.
   IAD,   //ICD10AM Effective from - indicates the date that the code is effective from.
   IAH,   //ICD10AM Age Edit, higher limit - minimum age is expressed as a three digit field.
   IAL,   //ICD10AM Age Edit, lower limit - minimum age is expressed as a three digit field.
   IAN,   //Annotation or Usage Note - a value of "+" or "DAGGER," denotes a code describing the etiology or underlying cause of the disease; an "*" or "ASTERISK," denotes a code describing the manifestation of a disease. A value of "DEFAULT" indicates that dual coding is not indicated.
   IAR,   //ICD10AM Relationship Note - code embedded in the term, may have a * or +.
   IAS,   //ICD10AM Sex Edit flag to indicate whether the code is valid for a particular sex. Sex flags are 1 (male) or 2 (female).
   IAT,   //ICD10AM Sex Edit Type - all sex flagged codes are followed by a "sex edit type" flag. The sex edit type flags are 1 (fatal) or 2 (warning).
   IAY,   //ICD10AM Age Edit Type - all age flagged codes are followed by an age edit type flag. The age edit type flags are 1 (fatal) or 2 (warning).
   ICA,   //ICD Additional Codes Note - an ICD-9-CM instruction to signal the coder that an additional code should be used if the information is available to provide a more complete picture of that diagnoses or procedure.
   ICC,   //ICD Code Also Note - an ICD-9-CM instruction to signal the coder to code additional information.
   ICD_O_3_CODE( "ICD-O-3_CODE" ),   //ICD-O-3 Code
   ICE,   //ICD Entry Term (e.g. Diarrhea:{dysenteric; epidemic}; Infectious diarrheal disease)
   ICF,   //ICD Fifth-digit Code Note - instruction to coders indicating use of a fifth-digit code.
   ICNP_TYPE,   //ICNP Type Codes: Defines if the code is a Diagnosis/Outcome (DC), Intervention (IC)
   ICN,   //ICD Note - instruction providing additional coding information.
   ICPCCODE,   //ICPC Code
   IDNR,   //Original ID number for source file
   IEEE_CF_CODE10,   //IEEE CF Code10
   IEEE_DESCRIPTION,   //IEEE description
   IEEE_DIM,   //IEEE DIM
   IEEE_REFID,   //IEEE REF ID
   IEEE_UOM_UCUM,   //IEEE UOM UCUM
   II,   //MeSH Indexing Information - for MeSH chemical terms (Term Type=NM), MeSH headings that may be relevant to articles that are also assigned the NM term.
   IMAGING_DOCUMENT_VALUE_SET,   //A value of "TRUE" indicates that the LOINC NUM is a member of the Imaging Documents Value Set
   IMPRINT_CODE,   //Imprint Code
   INACTIVATION_INDICATOR,   //Inactivation indicator
   INC,   //ICD10AM Neoplasm code (e.g. C34.- )
   IND_CODE,   //IND Code
   IND,   //IND
   INFOODS,   //Infoods
   IPX,   //ICD10 code related to an ICPC code - a + indicates that the ICD10 code is broader than the ICPC code; a - indicates that the ICD10 code is narrower than the ICPC code.
   ISLEAF,   //Leaf flag: 0 for hierarchical nodes, 1 for leaf nodes
   IS_DRUG_CLASS,   //Is Drug Class
   IS_RETIRED_FROM_SUBSET,   //In future, some concepts will be marked retired if they are retired by IHTSDO or no longer considered to be useful e.g. when there are more appropriate SNOMED CT concepts
   JHU_DTI_81( "JHU_DTI-81" ),   //JHU DTI-81
   JHU_WHITE_MATTER_TRACTOGRAPHY_ATLAS,   //JHU white matter tractography atlas
   JXR,   //OMIM code that is related to Jablonski term.
   KEGG_ID,   //Kyoto Encyclopedia of Genes and Genomes (KEGG), KEGG Pathway Database, adapted for NCI use ID
   LABELER,   //FDA Structured Product Label Manufacturer/Distributor/Supplier name
   LABEL_TYPE,   //DailyMed label type
   LANGUAGECODE,   //SNOMED CT string identifying a language and, if appropriate, a dialect in which this description is valid.
   LANGUAGE,   //Language
   LAST_IN_SUBSET,   //The version of Subset last containing this concept.
   LATERALITY,   //Laterality
   LCD,   //LOINC CDISC common tests
   LCL,   //LOINC Class - arbitrary classification of terms in LOINC designed to assist LOINC development and to group related observations together (e.g. ABXBACT = Antibiotic susceptibility)
   LCN,   //LOINC Classtype - 1 = Laboratory Class; 2 = Clinical Class; 3 = Claims Attachment; 4 = Surveys
   LCS,   //LOINC status - valid values are "ACTIVE," "TRIAL," "DISCOURAGED," "DEPRECATED." For "DEPRECATED" or "DISCOURAGED" concepts, the term that should now be used may appear in the LMP element.
   LCT,   //LOINC Change Type Code - type of change made to a LOINC term.
   LEA,   //LOINC Example Answers - for some tests and measurements, LOINC has supplied examples of valid answers. These values differ from those in the ANSWERLIST field because that details possible values for nominal scale terms.
   LEGALLY_MARKETED_UNAPPROVED_NEW_ANIMAL_DRUGS_FOR_MINOR_SPECIES,   //Legally Marketed Unapproved New Animal Drugs for Minor Species
   LEVEL,   //RXNORM: Specifies the type of drug concepts - Values can be "Ingredient", "VA Class", or "VA Product"; ICF: hierarchical level number
   LFO,   //LOINC Formula - regression equation details for many OB.US calculated terms.
   LMP,   //LOINC Map to Code of the term that has superseded a term with a LCS value of DEPRECATED.
   LOCUS_GROUP,   //Locus group
   LOCUS_SPECIFIC_DB_XR,   //Locus specific database cross-reference
   LOCUS_TYPE,   //Locus Type
   LOE,   //Level of effort
   LOINC_COMPONENT,   //LOINC component
   LOINC_METHOD_TYP,   //LOINC method type
   LOINC_PROPERTY,   //LOINC property
   LOINC_SCALE_TYP,   //LOINC scale type
   LOINC_SCORE,   //A numeric score associated with a LOINC answer ('LA') concept.
   LOINC_SYSTEM,   //LOINC system
   LOINC_TIME_ASPECT,   //LOINC time aspect
   LOR,   //LOINC ORDER_OBS field. Defines term as order only, observation only, or both. Values are: BOTH OBSERVATION ORDER. A fourth category, Subset, is used for terms that are subsets of a panel but do not represent a package that is known to be orderable.
   LQS,   //Survey Question Source
   LQT,   //Survey Question Text
   LRN2,   //Related names 2
   LSP,   //LOINC Species code
   LSU,   //Submitted Units
   LT,   //Indicates if a chemicals or medical device is a tradename (present in older versions of the Metathesaurus and was discontinued, then brought back starting in 2002AD)
   LUR,   //Units required when used as OBX segment - a Y/N field that indicates that units are required when this LOINC is included as an OBX segment in a HIPAA attachment
   MACRONUTRIENT,   //Macronutrient
   MANUFACTURER_STATUS,   //Manufacturer Status
   MAPADVICE,   //Map Advice
   MAPCATEGORYID,   //Map Category Id
   MAPGROUP,   //Map Group
   MAPPED_UCSC_ID,   //UCSC ID (mapped data)
   MAPPRIORITY,   //Map Priority
   MAPRULE,   //Map Rule
   MAPSETGRAMMAR,   //Grammar used to describe boolean expressions used in a map set
   MAPSETNAME,   //Official name of a map set
   MAPSETRSAB,   //Root source abbreviation for a map set
   MAPSETSID,   //Source asserted identifier for a map set
   MAPSETTYPE,   //Indicates the nature of a map set. Its value is map set specific. It can be used to indicate the inclusion of one to one, one to many and choices of maps.
   MAPSETVERSION,   //Version of the map set
   MAPSETVSAB,   //Versioned source abbreviation for a map set
   MAPSETXRTARGETID,   //Map set target identifier used for XR mappings.
   MAPTARGET,   //Map Target
   MARKETING_CATEGORY,   //Marketing category
   MARKETING_EFFECTIVE_TIME_HIGH,   //The date the MTHSPL drug became active
   MARKETING_EFFECTIVE_TIME_LOW,   //The date the MTHSPL drug became completed
   MARKETING_STATUS,   //The Marketing Status of the MTHSPL drug
   MDA,   //MeSH date of entry YYYYMMDD - date the term was added to the MeSH file, which is prior to the date the term became available for indexing and searching MEDLARS citations. Terms that have been part of MeSH for many years may have no value in this element.
   MEASURE_DEVELOPER,   //Name of the measure developer associated with the Category II code
   MEA,   //In NOC, the "measurement scale" used for a particular outcome.
   MENU_PARENT,   //Link information to Term Menu parent. This may not always be the same as the TermParent. Element will not exist for top level menus, e.g. cancer.
   MENU_TYPE,   //Values are Clinical Trials--Cancer Type, Clinical Trials--Drug, or Cancer Information.
   MESH_DEFINITION,   //MeSH definition for MeSH main heading
   MESH_DUI,   //The MeSH descriptor unique identifier to which the given NDFRT term maps
   MESH_NAME,   //MeSH name
   MESH_UI,   //The MeSH Unique Identifier to which the given NDFRT term maps
   MGD_ID,   //MGD ID
   MGI_ACCESSION_ID,   //MGI Accession ID
   MICRONUTRIENT,   //Micronutrient
   MIMTYPEMEANING,   //OMIM MimType Meaning
   MIMTYPEVALUE,   //OMIM MimType Value
   MIMTYPE,   //OMIM Entry Type
   MIRBASE_ID,   //miRBase ID
   MISO,   //MedDRA Serial Code International SOC Sort Order Digit (01-26)
   MMR,   //MeSH revision date YYYYMMDD - date of the last major revision to the term's MeSH record.
   MMX_RXO,   //Micromedex Product Category Code
   MN,   //MeSH hierarchical number for the concept in the MeSH tree structures. This number also appears in the HCD subelement of the REL and CXT elements.
   MODIFIER_ID,   //Modifier ID
   MODIFIER,   //Modifier
   MODULE_ID,   //Module ID
   MONOGRAPH_OFFICIAL_DATE,   //Monograph Official Date
   MONOGRAPH_STATUS,   //Monograph Status
   MOVED_FROM,   //Moved from
   MPS,   //MedDRA primary SOC (PTs may have multiple treepositions, but each has a primary soc)
   MP_GROUP_URL,   //MEDLINEPLUS group URL
   MP_HEALTH_TOPIC_URL,   //MEDLINEPLUS health topic URL
   MP_OTHER_LANGUAGE_URL,   //MEDLINEPLUS health topic other-language URL
   MP_PRIMARY_INSTITUTE_URL,   //MEDLINEPLUS health topic primary-institute URL
   MR,   //Major revision date YYYYMMDD - date the Metathesaurus entry for the concept underwent any revision in content.
   MSC,   //Minimal Standard (Terminology) Class
   MSP,   //SPN Medical Specialty Panel (responsible for reviewing the product).
   MTH_MAPFROMCOMPLEXITY,   //Indicates the complexity of "from" expressions used in a map set
   MTH_MAPFROMEXHAUSTIVE,   //Indicates whether or not the "from" source of a map set is completely mapped
   MTH_MAPSETCOMPLEXITY,   //Indicates the overall complexity of a map set
   MTH_MAPTOCOMPLEXITY,   //Indicates the complexity of "to" expressions in a map set
   MTH_MAPTOEXHAUSTIVE,   //Indicates whether or not the "to" source is completely mapped
   NADA,   //New Animal Drug Application number for MTHSPL drug
   NCBI_TAXON_ID,   //NCBI_Taxon_ID
   NCI_ID,   //NCI thesaurus concept id
   NDA_AUTHORIZED_GENERIC,   //New Drug Application number for authorized generic MTHSPL drug
   NDA,   //New Drug Application number for MTHSPL drug
   NDC,   //National Drug Code corresponding to a clinical drug (e.g. 000023082503)
   NDFRT_KIND,   //NDFRT kind
   NDF_TRANSMIT_TO_CMOP,   //NDF Transmit to Consolidated Mail Outpatient Pharmacy (CMOP)
   NDF_UNITS,   //NDF units
   NEOPLASTIC_STATUS,   //Neoplastic Status
   NEUROLEX_ID,   //Neurolex ID
   NEUROQUANT,   //Neuroquant
   NFI,   //National formulary indicator - "YES" or "NO" indicating whether a drug is in the VA's National Formulary
   NF_INACTIVATE,   //National Formulary Inactivation Date - the date a drug was removed from the VA's National Formulary
   NF_NAME,   //National Formulary Name
   NICHD_HIERARCHY_TERM,   //NICHD Hierarchy Term
   NON_VACCINE,   //Non Vaccine
   NOTE,   //Note
   NSC_CODE,   //NSC Code
   NSR,   //Neuronames Species Restriction - indication that a Neuronames concept applies to humans, macaques, primates, rats, etc. Most Neuronames concepts have no species restriction.
   NUI,   //NDFRT numerical unique identifier
   NUTRIENT,   //Nutrient
   OCCURRENCE,   //Number of institutions having this concept on their problem list (from 1 to 7), not populated for concepts retired from Subset
   OID,   //Answer list OID
   OL,   //MeSH Online Note - information helpful to online searchers of MEDLINE, especially when the history of a term or cross-reference has implications for online searching. This is a potential source of useful information for rules for search interface programs.
   OMIM_ID,   //OMIM ID
   OMIM_NUMBER,   //OMIM Number
   ORDER_NO,   //Order number
   ORIG_CODE,   //Original code associated with this string
   ORIG_SOURCE,   //Original source associated with this string
   ORIG_STY,   //Original semantic type
   OR_GROUP,   //Indicates that relationships with the same CUI2 and RG are disjunctive expressions
   OTC_MONOGRAPH_FINAL,   //FDA Structured Product Label OTC monograph status
   OTC_MONOGRAPH_NOT_FINAL,   //FDA Structured Product Label OTC monograph status
   OUTDATED_MEANING,   //Outdated Meaning
   PANEL_TYPE,   //Panel type
   PARENT_CLASS,   //VA Internal entry number of the parent class
   PARTITION,   //Partition
   PART_SEQ_ORDER,   //Part sequence order
   PART_TYPE,   //Part type
   PA,   //Pharmacologic Action of MeSH main headings (MH) for drugs and supplementary concept names (NM). The information in this element is also represented by an "isa" relationship between the MH or NM concept and the MeSH concept name for the class of drugs with a particular pharmacologic action.
   PCL,   //Pharmacy Practice Activity Classification (PPAC) Category - all terms are assigned to one of five categories, which connote their hierarchy.
   PDC,   //SPN Product Device Class (level of CDRH regulation: class 1, 2, or 3).
   PDQ_CLOSED_TRIAL_SEARCH_ID,   //PDQ Closed Trial Search ID
   PDQ_OPEN_TRIAL_SEARCH_ID,   //PDQ Open Trial Search ID
   PHYSICAL_STATE,   //Physical state
   PID_ID,   //National Cancer Institute Nature Pathway Interaction Database ID
   PID,   //Legacy PDQ ID
   PIRADSV2,   //Piradsv 2
   PI,   //MeSH heading or heading/subheading combination(s) followed by a date range in parentheses (YYYY).
   PLACE_OF_SERVICE_CODE,   //Place of service code
   PLR,   //Pharmacy Practice Activity Classification (PPAC) Last Revision Date (Format: M/DD/YY time)
   PMID,   //Pubmed ID
   PM,   //Public MeSH note - combines key information from the HN and PI elements in a format that is printed in the MeSH publications.
   POLARITY,   //Polarity
   PRC,   //Product Third Party Review Code from SPN.
   PREF_FLAG_ID,   //Preferred flag
   PREMARKET_APPLICATION,   //Premarket Application
   PREMARKET_NOTIFICATION,   //FDA Structured Product Label premarket notification
   PREV_NAME,   //Previous name
   PREV_SYMBOL,   //Previous symbol
   PROPRIETARY_NAME,   //Proprietary name
   PTR,   //SPN Product Tier (level of CDRH triage: 1, 2, 3, or E{xempt}).
   PUBLISH_VALUE_SET,   //publish value set
   PUBMEDID_PRIMARY_REFERENCE,   //PubMedID Primary Reference
   PXC,   //PDQ Protocol Exclusion Criteria - terms with type "exclusion criteria," which may be indexed on protocol records to identify conditions that exclude a patient from eligibility.
   PYR,   //PsychInfo year designation
   RADLEX_ID,   //RadLex ID
   RADLEX_PLAYBOOK_LONG_NAME,   //RadLex Playbook long name
   RADLEX_PREF_NAME,   //RadLex preferred name
   RANK,   //NCBI Rank (e.g. RANK[NCBI]species)
   REFSEQ_ID,   //RefSeq ID
   REFSET_PATTERN,   //Refset pattern
   REF,   //References or citations related to a given atom or concept.
   REMARKS,   //Specific coding instructions
   REPLACED_BY_SNOMED_CID,   //Current version SNOMEDCT concept id this concept is replaced by
   REPORTABLE,   //Indicates whether a code is reportable
   RGD_ID,   //Rat genome database gene ID
   RID,   //Read Codes Term_id - identifier assigned to a Read term, used in referring to the term in the Read file structure, and may be used in clinical information systems.
   RNA_CENTRAL_ID,   //RNA Central ID
   RN,   //Registry Number - series of numbers and hyphens (any leading zeros in an RN are dropped) or a series of numbers and periods, preceded by EC
   RPID,   //RadLex Playbook ID
   RR,   //The Chemical Abstracts Registry numbers for salts, optical isomers, or isotope-labeled versions of the concept followed by the relationship of this RR to the RN (in parentheses.) Applies to chemicals only. These numbers can be used as links to information in a number of chemical and toxicological databases.
   RVU_FACILITY_PRACTICE_EXPENSE_DISCONTINUED,   //RVU facility practice expense discontinued
   RVU_FACILITY_PRACTICE_EXPENSE_PROFESSIONAL,   //RVU facility practice expense professional
   RVU_FACILITY_PRACTICE_EXPENSE_TECHNICAL,   //RVU facility practice expense technical
   RVU_FACILITY_PRACTICE_EXPENSE,   //RVU facility practice expense
   RVU_MEDICARE_GLOBAL_PERIOD_DISCONTINUED,   //RVU Medicare global period discontinued
   RVU_MEDICARE_GLOBAL_PERIOD_PROFESSIONAL,   //RVU Medicare global period professional
   RVU_MEDICARE_GLOBAL_PERIOD_TECHNICAL,   //RVU Medicare global period technical
   RVU_MEDICARE_GLOBAL_PERIOD,   //RVU Medicare global period
   RVU_NONFACILITY_PRACTICE_EXPENSE_DISCONTINUED,   //RVU nonfacility practice expense discontinued
   RVU_NONFACILITY_PRACTICE_EXPENSE_PROFESSIONAL,   //RVU nonfacility practice expense professional
   RVU_NONFACILITY_PRACTICE_EXPENSE_TECHNICAL,   //RVU nonfacility practice expense technical
   RVU_NONFACILITY_PRACTICE_EXPENSE,   //RVU nonfacility practice expense
   RVU_PLI_DISCONTINUED,   //RVU PLI discontinued
   RVU_PLI_PROFESSIONAL,   //RVU PLI professional
   RVU_PLI_TECHNICAL,   //RVU PLI technical
   RVU_PLI,   //RVU PLI
   RVU_TOTAL_FACILITY_DISCONTINUED,   //RVU total facility discontinued
   RVU_TOTAL_FACILITY_PROFESSIONAL,   //RVU total facility professional
   RVU_TOTAL_FACILITY_TECHNICAL,   //RVU total facility technical
   RVU_TOTAL_FACILITY,   //RVU total facility
   RVU_TOTAL_NONFACILITY_DISCONTINUED,   //RVU total nonfacility discontinued
   RVU_TOTAL_NONFACILITY_PROFESSIONAL,   //RVU total nonfacility professional
   RVU_TOTAL_NONFACILITY_TECHNICAL,   //RVU total nonfacility technical
   RVU_TOTAL_NONFACILITY,   //RVU total nonfacility
   RVU_WORK_DISCONTINUED,   //RVU work discontinued
   RVU_WORK_PROFESSIONAL,   //RVU work professional
   RVU_WORK_TECHNICAL,   //RVU work technical
   RVU_WORK,   //RVU work
   RXAUI,   //RxNorm atom identifier for the atom it is connected to
   RXCUI,   //RxNorm concept identifier for the atom this is connected to
   RXN_ACTIVATED,   //Date the RxNorm atom was reactivated
   RXN_AVAILABLE_STRENGTH,   //Available drug strengths listed in the order of ingredients from the drug
   RXN_BN_CARDINALITY,   //Cardinality of RxNorm Brand Name Atom
   RXN_BOSS_AI,   //RXN Boss AI
   RXN_BOSS_AM,   //RXN Boss AM
   RXN_BOSS_FROM,   //RXN Boss From
   RXN_BOSS_STRENGTH_DENOM_UNIT,   //RXN Boss Strength Denom Unit
   RXN_BOSS_STRENGTH_DENOM_VALUE,   //RXN Boss Strength Denom Value
   RXN_BOSS_STRENGTH_NUM_UNIT,   //RXN Boss Strength Num Unit
   RXN_BOSS_STRENGTH_NUM_VALUE,   //RXN Boss Strength Num Value
   RXN_HUMAN_DRUG,   //Drug available for use in Humans
   RXN_IN_EXPRESSED_FLAG,   //Strength Expressed As Precise Flag
   RXN_OBSOLETED,   //Date the RxNorm atom became obsolete
   RXN_QUALITATIVE_DISTINCTION,   //RXN Qualitative Distinction
   RXN_QUANTITY,   //Normal Form quantity factor
   RXN_STRENGTH,   //Strength plus unit of SCDC
   RXN_VET_DRUG,   //Drug available for use in animals
   RXR,   //Replacement: cross reference from obsolete code to current code
   RXTERM_FORM,   //The RxTerm dose form name for this drug
   SB,   //SNOMED International subset indicator - valid values: * =can code using two T codes or G code for laterality B Bethesda system (Morphology); IC= ICDO (Oncology) related; N=Nursing; N* =Nursing, provisional; U=Ultrastructure (Morphology); V= Veterinary; V* =Veterinary AND can code using two T codes or G code for laterality (Topography)
   SCORE,   //FDA Structured Product Label imprint attribute for score
   SC,   //MeSH Supplemental record class.
   SECONDLEVEL,   //Second Level code
   SEQ_NO,   //Sequence number
   SHAPETEXT,   //FDA Structured Product Label imprint attribute for shape text
   SHAPE,   //FDA Structured Product Label imprint attribute for shape
   SHF,   //SNOMED Hospital Formulary Code - the American Hospital Formulary Code for a chemical contained in SNOMED International (e.g. 84:24:12)
   SIC,   //SNOMED ICD9CM Reference - the ICD9CM code or codes listed as relevant to the meaning of the concept in SNOMED International.
   SID,   //Secondary GO ID (e.g. GO:0020034)
   SIZE,   //FDA Structured Product Label imprint attribute for size
   SLOT_SYNONYM,   //slot synonym
   SMQ_ALGO,   //Standardised MedDRA Query (SMQ) Algorithm. Boolean expression of algorithm for the SMQ. "N" if the SMQ does not utilize an algorithm.
   SMQ_LEVEL,   //Standardised MedDRA Query (SMQ) Level. Value between 1 and 5 identifying the level of the SMQ within the hierarchy of SMQs; 1 is the most general, 5 is the most narrow.
   SMQ_SOURCE,   //Standardised MedDRA Query (SMQ) Source. Source for the development of the SMQ, e.g. medical references.
   SMQ_STATUS,   //Standardised MedDRA Query (SMQ) Status. "A" = active; "I" = inactive.
   SMQ_TERM_ADDVERSION,   //Standardised MedDRA Query (SMQ) Addition Version. Version of MedDRA in which term was added to the SMQ.
   SMQ_TERM_CAT,   //Standardised MedDRA Query (SMQ) Term Category. A single alphabetical letter indicating the category of the term for application of the SMQ algorithm. If the SMQ does not use algorithms, then all Term_category values are assigned "A." For a child SMQ, this field is assigned "S."
   SMQ_TERM_LEVEL,   //Standardised MedDRA Query (SMQ) Term Level. The MedDRA hierarchy level of a term (4=PT, 5=LLT) or 0 (zero) for a child SMQ.
   SMQ_TERM_LMVERSION,   //Standardised MedDRA Query (SMQ) Last Modified Version. Version of MedDRA in which term was last modified in the SMQ.
   SMQ_TERM_SCOPE,   //Standardised MedDRA Query (SMQ) Term Scope. Defines the MedDRA term as a member of the broad scope (1), narrow scope (2) of the SMQ search, or a child SMQ (0) (zero).
   SMQ_TERM_STATUS,   //Standardised MedDRA Query (SMQ) Term Status. Identifies a term as active ("A") or inactive ("I") within this SMQ.
   SMQ_TERM_WEIGHT,   //Standardised MedDRA Query (SMQ) Term Weight. Used for some SMQ algorithms; "0" is used as default.
   SMX,   //SNOMED Multiaxial coding - an alphanumeric string that includes hyphens, parentheses, and sometimes ellipses.
   SNGL_OR_MULT_SRC_PRD,   //Single or multi-source product
   SNOMED_CHILD,   //SNOMED CHILD
   SNOMED_CID,   //SNOMED-CT concept ID mapping for NDFRT disease name term
   SNOMED_PARENT,   //SNOMED PARENT
   SORT,   //Sort field
   SOS,   //Scope Statement
   SOURCEEFFECTIVETIME,   //Source Effective Time
   SOURCE_EFFECTIVE_TIME,   //Source effective time
   SOURCE_UI,   //Unique identifier from the source
   SPECIALTY,   //Specialty
   SPECIES,   //Species
   SPL_SET_ID,   //FDA Structured Product Label SET_ID code
   SRC,   //MeSH Literature source of chemical name - a citation to an article in a journal indexed for MEDLINE in which the chemical has been identified. (Note: Not to be confused with source abbreviation of SRC)
   STATE_OF_DETERMINATION,   //State of determination
   STATUS_REASON,   //Classification of the reason for concept status in LOINC
   STATUS_TEXT,   //Explanation of concept status in narrative text in LOINC
   STRENGTH,   //NDF strength
   ST,   //Concept Attributes Status - valid values: R Reviewed, U Unreviewed
   SUBCATEGORY,   //Subcategory
   SUBSEQUENT_TEXT_PROMPT,   //Subsequent text prompt
   SUBSET_MEMBER,   //Subset member
   SWISS_PROT,   //Swiss Prot
   SWP,   //Swiss Protein Number
   SYCODE,   //Synonym Code
   SYMBOL,   //FDA Structured Product Label imprint attribute for symbol
   SYN_QUALIFIER,   //Synonym qualifier
   TALAIRACH_TERM,   //Talairach Term
   TARGETCOMPONENT,   //Target Component
   TARGETEFFECTIVETIME,   //Target Effective Time
   TARGET_EFFECTIVE_TIME,   //Target effective time
   TA_ID,   //TAID
   TELEMEDICINE,   //Telemedicine
   TERMCODE,   //Term Code
   TERMID,   //Term identifier
   TERMSTATUS,   //Term status
   TERMUI,   //Term unique identifier
   TERM_BROWSER_VALUE_SET_DESCRIPTION,   //Term browser value set description
   TERM_STATUS,   //Term Status
   THIRDLEVEL,   //Third Level code
   TH,   //MeSH Thesaurus ID - identifies thesauri other than MeSH in which the MeSH heading or cross-reference is included.
   TOLERABLE_LEVEL,   //Tolerable Level
   TOP_2000_LAB_RESULTS_SI,   //Top 2000 lab results SI
   TOP_2000_LAB_RESULTS_US,   //Top 2000 lab results US
   TORSAB,   //Root source abbreviation for the "to" identifiers of a map set
   TOVSAB,   //Versioned source abbreviation for the "to" identifiers of a map set
   TRANSLATION,   //Translated name of the neuroanatomical structure
   TYPE_ID,   //Type ID
   TYPE,   //Multum Medical Supply Category (e.g. natural supplements)
   UMLSRELA,   //UMLS relationship attribute
   UMLSREL,   //The UMLS Metathesaurus REL relationship (SY, CHD, RN, RB, RO) assigned to SNOMED CT relationship identifiers.
   UNAPPROVED_DRUG_OTHER,   //Marketing category unapproved_drug_other for MTHSPL atom
   UNAPPROVED_HOMEOPATHIC,   //Unapproved Homeopathic
   UNAPPROVED_MEDICAL_GAS,   //Unapproved Medical Gas
   UNITS_AND_RANGE,   //Units and range
   UNIT,   //Unit
   UNIVERSAL_LAB_ORDERS_VALUE_SET,   //A value of "TRUE" indicates that the LOINC NUM is a member of the LOINC Universal Lab Orders Value Set
   UPC,   //Universal Product Code
   USAGE,   //The average usage percentage among all institutions (i.e. sum of individual usage percentages divided by 7), not populated for concepts retired from Subset
   USDA_ID,   //USDA ID
   USE_ADDITIONAL,   //Certain conditions have both an underlying etiology and multiple body system manifestations due to the underlying etiology. ICD-10-CM has a coding convention that requires the underlying condition be sequenced first followed by the manifestation. Wherever such a combination exists there is a "use additional code" note at the etiology code. These instructional notes indicate the proper sequencing order of the codes, etiology followed by manifestation. In most cases the manifestation codes will have in the code title, "in diseases classified elsewhere." Codes with this title are a component of the etiology/ manifestation convention. The code title indicates that it is a manifestation code. "In diseases classified elsewhere" codes are never permitted to be used as first listed or principle diagnosis codes. They must be used in conjunction with an underlying condition code and they must be listed following the underlying condition.
   USE_FOR,   //Use for
   US_RECOMMENDED_INTAKE,   //US Recommended Intake
   UWT,   //A semantic type provided from terms from the University of Washington Digital Anatomist
   VACCINE_STATUS,   //Vaccine status. CVX codes for inactive vaccines allow transmission of historical immunization records.
   VAC,   //VA Class - the code of an NDF/HT drug class name (e.g. AM110)
   VALUEID,   //Value Id
   VANDF_RECORD,   //Concatenated triple of VANDF file number, VANDF file IEN, VANDF file record status
   VA_CLASS_NAME,   //VA class name
   VA_DISPENSE_UNIT,   //VA dispense unit
   VA_GENERIC_NAME,   //VA generic name
   VA_IEN,   //VA individual entry number
   VEGA_ID,   //VEGA ID
   VERSION_FIRST_RELEASED,   //Version first released
   VERSION_LAST_CHANGED,   //Version last changed
   VIEW,   //View
   VMO,   //VA CMOP (central mail - order pharmacy) ID
   VUID;   //VHA unique identifier assigned to the product

   private final String _name;

   Atn() {
      this( "" );
   }

   Atn( String name ) {
      _name = name;
   }

   public String getName() {
      if ( _name.isEmpty() ) {
         return this.name();
      }
      return _name;
   }


}
