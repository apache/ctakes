package org.apache.ctakes.gui.dictionary.cased.umls.file;

import java.util.Arrays;

/**
 * Term Type in Source
 * https:// www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/abbreviations.html
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/18/2018
 */
public enum Tty {
   AA( "Attribute type abbreviation" ),
   AB( "Abbreviation in any source vocabulary", true ),
   ACR( "Acronym", true ),
   AC( "Activities" ),
   AD( "Adjective" ),
   AM( "Short form of modifier" ),
   AS( "Attribute type synonym" ),
   AT( "Attribute type" ),
   AUN( "Authority name" ),
   //   BD( "Fully-specified drug brand name that can be prescribed", true ),
   BD( "Fully-specified drug brand name that can be prescribed" ),
   BN( "Fully-specified drug brand name that can not be prescribed", true ),
   BPCK( "Branded Drug Delivery Device", true ),
   BR( "Binding realm" ),
   CA2( "ISO 3166-1 standard country code in alpha-2 (two-letter) format" ),
   CA3( "ISO 3166-1 standard country code in alpha-3 (three-letter) format" ),
   CCN( "Chemical code name" ),
   CCS( "FIPS 10-4 country code" ),
   CC( "Trimmed ICPC component process" ),
   CDA( "Clinical drug name in abbreviated format", true ),
   //   CDC( "Clinical drug name in concatenated format (NDDF), Clinical drug name (NDFRT)", true ),
   CDC( "Clinical drug name in concatenated format (NDDF), Clinical drug name (NDFRT)" ),
   CDD( "Clinical drug name in delimited format" ),
   CDO( "Concept domain" ),
   CD( "Clinical Drug", true ),
   CE( "Entry term for a Supplementary Concept" ),
   CHN( "Chemical structure name" ),
   CL( "Class" ),
   CMN( "Common name" ),
   CN( "LOINC official component name" ),
   CO( "Component name (these are hierarchical terms, as opposed to the LOINC component names which are analytes)" ),
   CPR( "Concept property" ),
   CP( "ICPC component process (in original form)" ),
   CR( "Concept relationship" ),
   CSN( "Chemical Structure Name" ),
   CSY( "Code system" ),
   CS( "Short component process in ICPC, i.e. include some abbreviations" ),
   CU( "Common usage" ),
   CV( "Content view" ),
   CX( "Component, with abbreviations expanded." ),
   DC10( "Diagnostic criteria for ICD10 code" ),
   DC9( "Diagnostic criteria for ICD9 code" ),
   DEV( "Descriptor entry version" ),
   DE( "Descriptor" ),
   DFG( "Dose Form Group" ),
   DF( "Dose Form" ),
   DI( "Disease name" ),
   DN( "Display Name", true ),
   DO( "Domain" ),
   DP( "Drug Product", true ),
   DSV( "Descriptor sort version" ),
   DS( "Short form of descriptor" ),
   DT( "Definitional term, present in the Metathesaurus because of its connection to a Dorland's definition or to a definition created especially for the Metathesaurus" ),
   EP( "Print entry term", true ),
   EQ( "Equivalent name", true ),
   ES( "Short form of entry term", true ),
   ETAL( "Entry Term Alias", true ),
   ETCF( "Entry term, consumer friendly description", true ),
   ETCLIN( "Entry term, clinician description", true ),
   ET( "Entry term", true ),
   EX( "Expanded form of entry term", true ),
   FBD( "Foreign brand name", true ),
   FI( "Finding name" ),
   FN( "Full form of descriptor" ),
   FSY( "Foreign Synonym" ),
   GLP( "Global period" ),
   GN( "Generic drug name", true ),
   GO( "Goal" ),
   GPCK( "Generic Drug Delivery Device", true ),
   GT( "Glossary term" ),
   HC( "Hierarchical class" ),
   HD( "Hierarchical descriptor" ),
   HGJKN1( "Japanese High Level Group Term (kana1)" ),
   HGJKN( "Japanese High Level Group Term (kana)" ),
   HG( "High Level Group Term" ),
   HS( "Short or alternate version of hierarchical term" ),
   HTJKN1( "Japanese Hierarchical term (kana1)" ),
   HTJKN( "Japanese Hierarchical term (kana)" ),
   HTN( "HL7 Table Name" ),
   HT( "Hierarchical term" ),
   HX( "Expanded version of short hierarchical term" ),
   ID( "Nursing indicator" ),
   IN( "Name for an ingredient", true ),
   IS( "Obsolete Synonym" ),
   IT( "Index term" ),
   IVC( "Intervention categories" ),
   IV( "Intervention" ),
   LA( "LOINC answer" ),
   LC( "Long common name" ),
   LLTJKN1( "Japanese Lower Level Term (kana1)" ),
   LLTJKN( "Japanese Lower Level Term (kana)" ),
   LLT( "Lower Level Term" ),
   LN( "LOINC official fully specified name" ),
   LO( "Obsolete official fully specified name" ),
   LPDN( "LOINC parts display name" ),
   LPN( "LOINC parts name", true ),
   LS( "Expanded system/sample type (The expanded version was created for the Metathesaurus and includes the full name of some abbreviations.)" ),
   LVDN( "Linguistic variant display name" ),
   LV( "Lexical variant" ),
   MD( "CCS multi-level diagnosis categories" ),
   MH( "Main heading" ),
   MIN( "name for a multi-ingredient" ),
   MP( "Preferred names of modifiers" ),
   MS( "Multum names of branded and generic supplies or supplements" ),
   MTH_AB( "MTH abbreviation" ),
   MTH_ACR( "MTH acronym" ),
   MTH_BD( "MTH fully-specified drug brand name that can be prescribed" ),
   MTH_CHN( "MTH chemical structure name" ),
   MTH_CN( "MTH Component, with abbreviations expanded." ),
   MTH_ET( "Metathesaurus entry term" ),
   MTH_FN( "MTH Full form of descriptor" ),
   MTH_HG( "MTH High Level Group Term" ),
   MTH_HT( "MTH Hierarchical term" ),
   MTH_HX( "MTH Hierarchical term expanded" ),
   MTH_IS( "Metathesaurus-supplied form of obsolete synonym" ),
   MTH_LLT( "MTH Lower Level Term" ),
   MTH_LN( "MTH Official fully specified name with expanded abbreviations" ),
   MTH_LO( "MTH Expanded LOINC obsolete fully specified name" ),
   MTH_LV( "MTH lexical variant" ),
   MTH_OAF( "Metathesaurus-supplied form of obsolete active fully specified name" ),
   MTH_OAP( "Metathesaurus-supplied form of obsolete active preferred term" ),
   MTH_OAS( "Metathesaurus-supplied form of obsolete active synonym" ),
   MTH_OET( "Metathesaurus obsolete entry term" ),
   MTH_OF( "Metathesaurus-supplied form of obsolete fully specified name" ),
   MTH_OL( "MTH Non-current Lower Level Term" ),
   MTH_OPN( "Metathesaurus obsolete preferred term, natural language form" ),
   MTH_OP( "Metathesaurus obsolete preferred term" ),
   MTH_OS( "MTH System-organ class" ),
   MTH_PTGB( "Metathesaurus-supplied form of British preferred term" ),
   MTH_PTN( "Metathesaurus preferred term, natural language form" ),
   MTH_PT( "Metathesaurus preferred term" ),
   MTH_RXN_BD( "RxNorm Created BD" ),
   MTH_RXN_CDC( "RxNorm Created CDC" ),
   MTH_RXN_CD( "RxNorm Created CD" ),
   MTH_RXN_DP( "RxNorm Created DP" ),
   MTH_RXN_RHT( "RxNorm Created reference hierarchy term" ),
   MTH_SI( "MTH Sign or symptom of" ),
   MTH_SMQ( "Metathesaurus version of Standardised MedDRA Query" ),
   MTH_SYGB( "Metathesaurus-supplied form of British synonym" ),
   MTH_SY( "MTH Designated synonym" ),
   MV( "Multi-level procedure category" ),
   N1( "Chemical Abstracts Service Type 1 name of a chemical" ),
   NA( "Name aliases" ),
   NM( "Name of Supplementary Concept" ),
   NPT( "HL7 non-preferred for language term" ),
   NP( "Non-preferred term" ),
   NS( "Short form of non-preferred term" ),
   NX( "Expanded form of non-preferred term" ),
   OAF( "Obsolete active fully specified name" ),
   OAM( "Obsolete Modifier Abbreviation" ),
   OAP( "Obsolete active preferred term", true ),
   OAS( "Obsolete active synonym" ),
   OA( "Obsolete abbreviation" ),
   OC( "Nursing outcomes" ),
   OET( "Obsolete entry term" ),
   OF( "Obsolete fully specified name", true ),
   OLC( "Obsolete Long common name" ),
   OLJKN1( "Japanese Non-current Lower Level Term (kana1)" ),
   OLJKN( "Japanese Non-current Lower Level Term (kana)" ),
   OL( "Non-current Lower Level Term" ),
   OM( "Obsolete modifiers in HCPCS" ),
   ONP( "Obsolete non-preferred for language term" ),
   OOSN( "Obsolete official short name" ),
   OPN( "Obsolete preferred term, natural language form" ),
   OP( "Obsolete preferred name" ),
   OR( "Orders" ),
   OSJKN1( "Japanese System-organ class in the WHO Adverse Reaction Terminology (kana1)" ),
   OSJKN( "Japanese System-organ class in the WHO Adverse Reaction Terminology (kana)" ),
   OSN( "Official short name" ),
   OS( "System-organ class" ),
   PCE( "Preferred entry term for Supplementary Concept" ),
   PC( "Preferred -trimmed term- in ICPC" ),
   PEP( "Preferred entry term" ),
   PHENO_ET( "Phenotype entry term" ),
   PHENO( "Phenotype" ),
   PIN( "Name from a precise ingredient" ),
   PM( "Machine permutation", true ),
   PN( "Metathesaurus preferred name", true ),
   POS( "Place of service" ),
   PQ( "Qualifier for a problem" ),
   PR( "Name of a problem" ),
   PSC( "Protocol selection criteria" ),
   //   PSN( "Prescribable Names", true, false ),
   PSN( "Prescribable Names" ),
   PS( "Short forms that needed full specification" ),
   PTAV( "Preferred Allelic Variant" ),
   PTCS( "Preferred Clinical Synopsis" ),
   PTGB( "British preferred term" ),
   PTJKN1( "Japanese Designated preferred name (kana1)" ),
   PTJKN( "Japanese Designated preferred name (kana)" ),
   PTN( "Preferred term, natural language form" ),
   PT( "Designated preferred name", true ),
   PXQ( "Preferred qualifier term" ),
   PX( "Expanded preferred terms (pair with PS)" ),
   QAB( "Qualifier abbreviation" ),
   QEV( "Qualifier entry version" ),
   QSV( "Qualifier sort version" ),
   RAB( "Root abbreviation" ),
   RHT( "Root hierarchical term" ),
   RPT( "Root preferred term" ),
   RSY( "Root synonym" ),
   RS( "Extracted related names in SNOMED2" ),
   RT( "Term that is related to, but often considered non-synonymous with, the preferred term" ),
   RXN_IN( "Rxnorm Preferred Ingredient" ),
   RXN_PT( "Rxnorm Preferred", true ),
   //   SBDC( "Semantic Branded Drug Component", true, false ),
//   SBDF( "Semantic branded drug and form", true, false ),
//   SBDG( "Semantic branded drug group", true ),
//   SBD( "Semantic branded drug", true, false ),
   SBDC( "Semantic Branded Drug Component" ),
   SBDF( "Semantic branded drug and form" ),
   SBDG( "Semantic branded drug group" ),
   SBD( "Semantic branded drug" ),
   SB( "Named subset of a source" ),
   SCALE( "Scale" ),
   //   SCDC( "Semantic Drug Component", true, false ),
//   SCDF( "Semantic clinical drug and form", true, false ),
//   SCDG( "Semantic clinical drug group", true ),
//   SCD( "Semantic Clinical Drug", true, false ),
   SCDC( "Semantic Drug Component" ),
   SCDF( "Semantic clinical drug and form" ),
   SCDG( "Semantic clinical drug group" ),
   SCD( "Semantic Clinical Drug" ),
   SCN( "Scientific name" ),
   SC( "Special Category term", true ),
   SD( "CCS single-level diagnosis categories" ),
   SI( "Name of a sign or symptom of a problem" ),
   SMQ( "Standardised MedDRA Query" ),
   SP( "CCS single-level procedure categories" ),
   SSN( "Source short name, used in the UMLS Knowledge Source Server" ),
   SS( "Synonymous -short- forms" ),
   ST( "Step" ),
   SU( "Active Substance", true ),
   SX( "Mixed-case component synonym with expanded abbreviations" ),
   SYGB( "British synonym", true ),
   SYN( "Designated alias" ),
   //   SY( "Designated synonym", true, false ),
   SY( "Designated synonym", true ),
   TA( "Task" ),
   TC( "Term class" ),
   TG( "Name of the target of an intervention" ),
   TMSY( "Tall Man synonym" ),
   TQ( "Topical qualifier" ),
   TX( "CCPSS synthesized problems for TC termgroup" ),
   UAUN( "Unique authority name" ),
   UCN( "Unique common name" ),
   UE( "Unique equivalent name" ),
   USN( "Unique scientific name" ),
   USY( "Unique synonym" ),
   VAB( "Versioned abbreviation" ),
   VPT( "Versioned preferred term" ),
   VSY( "Versioned synonym" ),
   VS( "Value Set" ),
   XD( "Expanded descriptor in AOD" ),
   XM( "Cross mapping set" ),
   XQ( "Alternate name for a qualifier" ),

   UNKNOWN( "Unknown" );

   final String _description;
   private final boolean _collect;
   private final boolean _keep;

   Tty( final String description ) {
      this( description, false, false );
   }

   Tty( final String description, final boolean keep ) {
      this( description, keep, keep );
   }

   Tty( final String description, final boolean collect, final boolean keep ) {
      _description = description;
      _collect = collect;
      _keep = keep;
   }

   public String getDescription() {
      return _description;
   }

   public boolean collect() {
      return _collect;
   }

   public boolean keep() {
      return _keep;
   }

   static public Tty getType( final String name ) {
      return Arrays.stream( values() )
                   .filter( s -> name.equalsIgnoreCase( s.name() ) )
                   .findFirst()
                   .orElse( UNKNOWN );
   }

   static public boolean collect( final String name ) {
      return getType( name ).collect();
   }

   static public boolean keep( final String name ) {
      return getType( name ).keep();
   }


   //  https:// www.nlm.nih.gov/research/umls/knowledge_sources/metathesaurus/release/precedence_suppressibility.html

   //  This Appendix displays the default order of Source|Term Types and suppressibility
   //  as set by NLM and distributed in the Metathesaurus® in MRRANK.RRF or MRRANK in ORF.
   // 
   //  Users are encouraged to change the order of Source|Term Type precedence and suppressibility to suit their requirements.
   //  The default settings will not be suitable for all applications.
   //  The highest ranking Source|Term Type within a concept determines the preferred name for that concept.
   //  Use MetamorphoSys to change the selection of preferred names or to alter suppressibility.

   //  Source Abbreviation	Term Type	Suppressible
   // MTH	PN	No
   // RXNORM	MIN	No
   // MTHCMSFRF	PT	No
   // RXNORM	SCD	No
   // RXNORM	SBD	No
   // RXNORM	SCDG	No
   // RXNORM	SBDG	No
   // RXNORM	IN	No
   // RXNORM	PSN	No
   // RXNORM	SCDF	No
   // RXNORM	SBDF	No
   // RXNORM	SCDC	No
   // RXNORM	DFG	No
   // RXNORM	DF	No
   // RXNORM	SBDC	No
   // RXNORM	BN	No
   // RXNORM	PIN	No
   // RXNORM	BPCK	No
   // RXNORM	GPCK	No
   // RXNORM	SY	No
   // RXNORM	TMSY	No
   // MSH	MH	No
   // MSH	TQ	No
   // MSH	PEP	No
   // MSH	ET	No
   // MSH	XQ	No
   // MSH	PXQ	No
   // MSH	NM	No
   // SNOMEDCT_US	PT	No
   // SNOMEDCT_US	FN	No
   // SNOMEDCT_US	SY	No
   // SNOMEDCT_US	PTGB	No
   // SNOMEDCT_US	SYGB	No
   // SNOMEDCT_US	MTH_PT	No
   // SNOMEDCT_US	MTH_FN	No
   // SNOMEDCT_US	MTH_SY	No
   // SNOMEDCT_US	MTH_PTGB	No
   // SNOMEDCT_US	MTH_SYGB	No
   // SNOMEDCT_US	SB	No
   // SNOMEDCT_US	XM	No
   // SNOMEDCT_VET	PT	No
   // SNOMEDCT_VET	FN	No
   // SNOMEDCT_VET	SY	No
   // SNOMEDCT_VET	SB	No
   // HPO	PT	No
   // HPO	SY	No
   // HPO	ET	No
   // HPO	OP	Yes
   // HPO	IS	Yes
   // NCBI	SCN	No
   // MTHSPL	MTH_RXN_DP	No
   // MTHSPL	DP	No
   // MTHSPL	SU	No
   // ATC	RXN_PT	No
   // ATC	PT	No
   // VANDF	PT	No
   // VANDF	CD	No
   // VANDF	IN	No
   // USP	CD	No
   // USP	IN	No
   // USPMG	HC	No
   // USPMG	PT	No
   // MMX	MTH_RXN_CD	No
   // MMX	MTH_RXN_BD	No
   // MMX	CD	No
   // MMX	BD	No
   // DRUGBANK	IN	No
   // DRUGBANK	SY	No
   // DRUGBANK	FSY	No
   // MSH	N1	No
   // MSH	PCE	No
   // MSH	CE	No
   // CPM	PT	No
   // NEU	PT	No
   // NEU	ACR	Yes
   // NEU	SY	No
   // NEU	OP	Yes
   // NEU	IS	Yes
   // FMA	PT	No
   // FMA	SY	No
   // FMA	AB	Yes
   // FMA	OP	Yes
   // FMA	IS	Yes
   // UWDA	PT	No
   // UWDA	SY	No
   // UMD	PT	No
   // UMD	SY	No
   // UMD	ET	No
   // UMD	RT	No
   // GS	CD	No
   // MMSL	CD	No
   // GS	MTH_RXN_BD	No
   // GS	BD	No
   // MMSL	MTH_RXN_BD	No
   // MMSL	BD	No
   // MMSL	SC	No
   // MMSL	MS	No
   // MMSL	GN	No
   // MMSL	BN	No
   // ATC	RXN_IN	No
   // ATC	IN	No
   // MMSL	IN	No
   // VANDF	AB	No
   // GS	MTH_RXN_CD	No
   // VANDF	MTH_RXN_CD	No
   // NDDF	MTH_RXN_CDC	No
   // NDDF	CDC	No
   // NDDF	CDD	No
   // NDDF	CDA	No
   // NDDF	IN	No
   // NDDF	DF	No
   // NDFRT	MTH_RXN_RHT	No
   // NDFRT	HT	No
   // MED-RT	PT	No
   // MED-RT	FN	No
   // NDFRT	FN	No
   // NDFRT	PT	No
   // MED-RT	SY	No
   // NDFRT	SY	No
   // NDFRT	AB	No
   // SPN	PT	No
   // MDR	PT	No
   // MDR	MTH_PT	No
   // MDR	HG	No
   // MDR	MTH_HG	No
   // MDR	OS	No
   // MDR	MTH_OS	No
   // MDR	HT	No
   // MDR	MTH_HT	No
   // MDR	LLT	No
   // MDR	MTH_LLT	No
   // MDR	SMQ	No
   // MDR	MTH_SMQ	No
   // MDR	AB	Yes
   // CPT	PT	No
   // CPT	SY	No
   // CPT	ETCLIN	No
   // CPT	POS	No
   // CPT	GLP	No
   // CPT	ETCF	No
   // CPT	MP	No
   // HCPT	PT	No
   // HCPCS	PT	No
   // CDT	PT	No
   // CDT	OP	Yes
   // MVX	PT	No
   // CVX	PT	No
   // CVX	RXN_PT	No
   // CVX	AB	No
   // HCDT	PT	No
   // HCPCS	MP	No
   // HCPT	MP	No
   // ICD10AE	PT	No
   // ICD10	PT	No
   // ICD10AE	PX	No
   // ICD10	PX	No
   // ICD10AE	PS	Yes
   // ICD10	PS	Yes
   // ICD10AMAE	PT	No
   // ICD10AM	PT	No
   // ICD10AMAE	PX	No
   // ICD10AM	PX	No
   // ICD10AMAE	PS	Yes
   // ICD10AM	PS	Yes
   // OMIM	PT	No
   // OMIM	PHENO	No
   // OMIM	PHENO_ET	No
   // OMIM	PTAV	No
   // OMIM	PTCS	No
   // OMIM	ETAL	No
   // OMIM	ET	No
   // OMIM	HT	No
   // OMIM	ACR	No
   // MEDCIN	PT	No
   // MEDCIN	FN	No
   // MEDCIN	XM	No
   // MEDCIN	SY	No
   // HGNC	PT	No
   // HGNC	ACR	No
   // HGNC	MTH_ACR	No
   // HGNC	NA	No
   // HGNC	SYN	No
   // ICNP	PT	No
   // ICNP	MTH_PT	No
   // ICNP	XM	No
   // PNDS	PT	No
   // PNDS	HT	No
   // PNDS	XM	No
   // PDQ	PT	No
   // PDQ	HT	No
   // PDQ	PSC	No
   // PDQ	SY	No
   // PDQ	MTH_SY	No
   // CHV	PT	No
   // MEDLINEPLUS	PT	No
   // NCI	PT	No
   // NCI	SY	No
   // NCI_BioC	SY	No
   // NCI_PI-RADS	PT	No
   // NCI_CareLex	PT	No
   // NCI_CareLex	SY	No
   // NCI_CDC	PT	No
   // NCI_CDISC	PT	No
   // NCI_CDISC	SY	No
   // NCI	CSN	No
   // NCI_DCP	PT	No
   // NCI_DCP	SY	No
   // NCI	DN	No
   // NCI_DTP	PT	No
   // NCI_DTP	SY	No
   // NCI	FBD	No
   // NCI_FDA	AB	No
   // NCI_CTRP	PT	No
   // NCI_CTRP	SY	No
   // NCI_CTRP	DN	No
   // NCI_FDA	PT	No
   // NCI_FDA	SY	No
   // NCI	HD	No
   // NCI_GENC	PT	No
   // NCI_GENC	CA2	No
   // NCI_GENC	CA3	No
   // NCI_CRCH	PT	No
   // NCI_CRCH	SY	No
   // NCI_DICOM	PT	No
   // NCI_CDISC-GLOSS	PT	No
   // NCI_CDISC-GLOSS	SY	No
   // NCI_BRIDG	PT	No
   // NCI_RENI	DN	No
   // NCI_BioC	PT	No
   // NCI	CCN	No
   // NCI_CTCAE	PT	No
   // NCI_CTCAE_5	PT	No
   // NCI_CTCAE_3	PT	No
   // NCI_CTEP-SDC	PT	No
   // NCI_CTEP-SDC	SY	No
   // NCI	CCS	No
   // NCI_JAX	PT	No
   // NCI_JAX	SY	No
   // NCI_KEGG	PT	No
   // NCI_ICH	AB	No
   // NCI_ICH	PT	No
   // NCI_NCI-HL7	AB	No
   // NCI_NCI-HGNC	PT	No
   // NCI_NCI-HGNC	SY	No
   // NCI_NCI-HL7	PT	No
   // NCI_UCUM	AB	No
   // NCI_UCUM	PT	No
   // NCI_KEGG	AB	No
   // NCI_KEGG	SY	No
   // NCI_NICHD	PT	No
   // NCI_NICHD	SY	No
   // NCI_PID	PT	No
   // NCI_NCPDP	PT	No
   // NCI_GAIA	PT	No
   // NCI_GAIA	SY	No
   // NCI_ZFin	PT	No
   // NCI_NCI-GLOSS	PT	No
   // NCI_ICH	SY	No
   // NCI_NCI-HL7	SY	No
   // NCI_UCUM	SY	No
   // NCI_NCPDP	SY	No
   // NCI_ZFin	SY	No
   // NCI_NCI-GLOSS	SY	No
   // NCI	OP	Yes
   // NCI_NICHD	OP	Yes
   // NCI	AD	No
   // NCI	CA2	No
   // NCI	CA3	No
   // NCI	BN	No
   // NCI	AB	No
   // MTHICPC2EAE	PT	No
   // ICPC2EENG	PT	No
   // MTHICPC2ICD10AE	PT	No
   // SOP	PT	No
   // ICF	HT	No
   // ICF	PT	No
   // ICF	MTH_HT	No
   // ICF	MTH_PT	No
   // ICF-CY	HT	No
   // ICF-CY	PT	No
   // ICF-CY	MTH_HT	No
   // ICF-CY	MTH_PT	No
   // ICPC2ICD10ENG	PT	No
   // ICPC	PX	No
   // ICPC	PT	No
   // ICPC	PS	Yes
   // ICPC	PC	No
   // ICPC	CX	No
   // ICPC	CP	No
   // ICPC	CS	Yes
   // ICPC	CC	No
   // ICPC2EENG	CO	No
   // ICPC	CO	No
   // MTHICPC2EAE	AB	Yes
   // ICPC2EENG	AB	Yes
   // ICPC2P	PTN	No
   // ICPC2P	MTH_PTN	No
   // ICPC2P	PT	No
   // ICPC2P	MTH_PT	Yes
   // ICPC2P	OPN	Yes
   // ICPC2P	MTH_OPN	Yes
   // ICPC2P	OP	Yes
   // ICPC2P	MTH_OP	Yes
   // AOT	PT	No
   // AOT	ET	No
   // HCPCS	OP	Yes
   // HCDT	OP	Yes
   // HCPT	OP	Yes
   // HCPCS	OM	Yes
   // HCPCS	OAM	Yes
   // GO	PT	No
   // GO	MTH_PT	No
   // GO	ET	No
   // GO	MTH_ET	No
   // GO	SY	No
   // GO	MTH_SY	No
   // GO	OP	Yes
   // GO	MTH_OP	Yes
   // GO	OET	Yes
   // GO	MTH_OET	Yes
   // GO	IS	Yes
   // GO	MTH_IS	Yes
   // PDQ	ET	No
   // PDQ	CU	No
   // PDQ	MTH_LV	No
   // PDQ	LV	No
   // PDQ	MTH_AB	No
   // PDQ	MTH_ACR	No
   // PDQ	ACR	No
   // PDQ	AB	No
   // PDQ	BD	No
   // PDQ	FBD	No
   // PDQ	OP	Yes
   // PDQ	CCN	No
   // PDQ	CHN	No
   // PDQ	MTH_CHN	No
   // PDQ	IS	Yes
   // PDQ	MTH_BD	No
   // NCBI	USN	No
   // NCBI	USY	No
   // NCBI	SY	No
   // NCBI	UCN	No
   // NCBI	CMN	No
   // NCBI	UE	No
   // NCBI	EQ	No
   // NCBI	AUN	Yes
   // NCBI	UAUN	Yes
   // LNC	LN	No
   // LNC	MTH_LN	No
   // LNC	OSN	No
   // LNC	CN	No
   // LNC	MTH_CN	No
   // LNC	LPN	No
   // LNC	LPDN	No
   // LNC	HC	No
   // LNC	HS	No
   // LNC	OLC	Yes
   // LNC	LC	No
   // LNC	XM	No
   // LNC	LS	No
   // LNC	LO	Yes
   // LNC	MTH_LO	Yes
   // LNC	OOSN	Yes
   // LNC	LA	No
   // ICD10CM	PT	No
   // ICD9CM	PT	No
   // MDR	OL	Yes
   // MDR	MTH_OL	Yes
   // ICD10CM	HT	No
   // ICD9CM	HT	No
   // CCS_10	HT	No
   // CCS_10	MD	No
   // CCS_10	MV	No
   // CCS_10	SD	No
   // CCS_10	SP	No
   // CCS_10	XM	No
   // CCS	HT	No
   // CCS	MD	No
   // CCS	SD	No
   // CCS	MV	No
   // CCS	SP	No
   // CCS	XM	No
   // ICPC2ICD10ENG	XM	No
   // ICD10AE	HT	No
   // ICD10PCS	PT	No
   // ICD10PCS	PX	No
   // ICD10PCS	HX	No
   // ICD10PCS	MTH_HX	No
   // ICD10PCS	HT	No
   // ICD10PCS	HS	Yes
   // ICD10PCS	AB	Yes
   // ICD10	HT	No
   // ICD10AE	HX	No
   // ICD10	HX	No
   // ICD10AE	HS	Yes
   // ICD10	HS	Yes
   // ICD10AMAE	HT	No
   // ICD10AM	HT	No
   // UMD	HT	No
   // ICPC	HT	No
   // NUCCPT	PT	No
   // NUCCPT	OP	Yes
   // NUCCPT	MP	No
   // HL7V3.0	CSY	No
   // HL7V3.0	PT	No
   // HL7V2.5	PT	No
   // HL7V3.0	CDO	No
   // HL7V3.0	VS	No
   // HL7V3.0	BR	No
   // HL7V3.0	CPR	No
   // HL7V3.0	CR	No
   // HL7V3.0	NPT	No
   // HL7V3.0	OP	Yes
   // HL7V3.0	ONP	Yes
   // HL7V2.5	HTN	No
   // CPT	HT	No
   // CDT	HT	No
   // MTHHH	HT	No
   // CCC	PT	No
   // CCC	HT	No
   // NIC	IV	No
   // NIC	HC	No
   // NANDA-I	PT	No
   // NANDA-I	HT	No
   // NANDA-I	HC	No
   // NANDA-I	RT	No
   // OMS	MTH_SI	No
   // OMS	PR	No
   // OMS	TG	No
   // OMS	HT	No
   // OMS	PQ	No
   // OMS	IVC	No
   // OMS	SI	No
   // OMS	SCALE	No
   // NIC	AC	No
   // NOC	OC	No
   // NOC	ID	No
   // NIC	HT	No
   // NOC	HT	No
   // NOC	HC	No
   // CCC	MTH_HT	No
   // CCC	MP	No
   // ALT	PT	No
   // ALT	HT	No
   // MTH	CV	No
   // MTH	XM	No
   // MTH	PT	No
   // MTH	SY	No
   // MTH	RT	No
   // ICD10CM	ET	No
   // MTHICD9	ET	No
   // ICD10CM	AB	Yes
   // ICD9CM	AB	Yes
   // PSY	PT	No
   // PSY	HT	No
   // PSY	ET	No
   // MEDLINEPLUS	ET	No
   // MEDLINEPLUS	SY	No
   // MEDLINEPLUS	HT	No
   // LCH_NW	PT	No
   // LCH	PT	No
   // MSH	HT	No
   // MSH	HS	No
   // MSH	DEV	Yes
   // MSH	DSV	Yes
   // MSH	QAB	Yes
   // MSH	QEV	Yes
   // MSH	QSV	Yes
   // MSH	PM	No
   // LCH_NW	XM	No
   // CPT	AB	Yes
   // HCPT	AB	Yes
   // HCPCS	AB	Yes
   // WHO	PT	No
   // WHO	OS	No
   // WHO	HT	No
   // WHO	IT	No
   // SNMI	PT	No
   // SNMI	PX	Yes
   // SNMI	HT	No
   // SNMI	HX	Yes
   // SNMI	RT	No
   // SNMI	SY	No
   // SNMI	SX	Yes
   // SNMI	AD	No
   // SNM	PT	No
   // SNM	RT	No
   // SNM	HT	No
   // SNM	SY	No
   // SNM	RS	No
   // RCD	PT	No
   // RCD	OP	Yes
   // RCD	SY	No
   // RCD	IS	Yes
   // RCD	AT	No
   // RCD	AS	Yes
   // RCD	AB	Yes
   // RCDSA	PT	No
   // RCDSY	PT	No
   // RCDAE	PT	No
   // RCDSA	SY	No
   // RCDSY	SY	No
   // RCDAE	SY	No
   // RCDSA	OP	Yes
   // RCDSY	OP	Yes
   // RCDAE	OP	Yes
   // RCDSA	IS	Yes
   // RCDSY	IS	Yes
   // RCDAE	IS	Yes
   // RCDAE	AT	No
   // RCDSA	AB	Yes
   // RCDSY	AB	Yes
   // RCDAE	AB	Yes
   // RCDSA	OA	Yes
   // RCDSY	OA	Yes
   // RCDAE	OA	Yes
   // RCD	OA	Yes
   // RCDAE	AA	Yes
   // RCD	AA	Yes
   // CSP	PT	No
   // CSP	SY	No
   // CSP	ET	No
   // CSP	AB	No
   // MTH	DT	No
   // HCPT	OA	Yes
   // HCPT	AM	Yes
   // HCPCS	OA	Yes
   // HCPCS	AM	Yes
   // HCDT	AB	Yes
   // ALT	AB	Yes
   // HCDT	OA	Yes
   // CHV	SY	No
   // RXNORM	ET	No
   // SNOMEDCT_VET	OAP	Yes
   // SNOMEDCT_VET	OP	Yes
   // SNOMEDCT_US	OAP	Yes
   // SNOMEDCT_US	OP	Yes
   // SNOMEDCT_VET	OAF	Yes
   // SNOMEDCT_VET	OF	Yes
   // SNOMEDCT_US	OAF	Yes
   // SNOMEDCT_US	OF	Yes
   // SNOMEDCT_VET	OAS	Yes
   // SNOMEDCT_VET	IS	Yes
   // SNOMEDCT_US	OAS	Yes
   // SNOMEDCT_US	IS	Yes
   // SNOMEDCT_US	MTH_OAP	Yes
   // SNOMEDCT_US	MTH_OP	Yes
   // SNOMEDCT_US	MTH_OAF	Yes
   // SNOMEDCT_US	MTH_OF	Yes
   // SNOMEDCT_US	MTH_OAS	Yes
   // SNOMEDCT_US	MTH_IS	Yes
   // DSM-5	DC10	No
   // DSM-5	DC9	No
   // DXP	DI	No
   // DXP	FI	No
   // DXP	SY	No
   // RAM	PT	No
   // RAM	RT	No
   // ULT	PT	No
   // BI	PT	No
   // BI	AB	No
   // BI	SY	No
   // BI	RT	No
   // PCDS	GO	No
   // PCDS	OR	No
   // PCDS	PR	No
   // PCDS	CO	No
   // PCDS	HX	No
   // PCDS	HT	No
   // MTHMST	PT	No
   // MTHMST	SY	No
   // DDB	PT	No
   // DDB	SY	No
   // CST	PT	No
   // COSTAR	PT	No
   // CST	SC	No
   // CST	HT	No
   // CST	GT	No
   // CCPSS	TX	No
   // CCPSS	TC	Yes
   // CCPSS	PT	No
   // CCPSS	MP	No
   // AOD	DE	No
   // AOD	DS	No
   // AOD	XD	No
   // AOD	FN	No
   // AOD	ET	No
   // AOD	ES	No
   // AOD	EX	No
   // AOD	NP	No
   // AOD	NS	No
   // AOD	NX	No
   // QMR	PT	No
   // JABL	PC	No
   // JABL	PT	No
   // JABL	SS	No
   // JABL	SY	No
   // AIR	FI	No
   // AIR	DI	No
   // AIR	SY	No
   // AIR	HT	No
   // PPAC	DO	No
   // PPAC	CL	No
   // PPAC	AC	No
   // PPAC	ST	No
   // PPAC	TA	No
   // MCM	PT	No
   // MCM	RT	No
   // SCTSPA	PT	No
   // SCTSPA	FN	No
   // SCTSPA	SY	No
   // SCTSPA	MTH_PT	No
   // SCTSPA	MTH_FN	No
   // SCTSPA	MTH_SY	No
   // SCTSPA	SB	No
   // SCTSPA	OP	Yes
   // SCTSPA	OAF	Yes
   // SCTSPA	OAP	Yes
   // SCTSPA	OAS	Yes
   // SCTSPA	OF	Yes
   // SCTSPA	IS	Yes
   // SCTSPA	MTH_OP	Yes
   // SCTSPA	MTH_OAF	Yes
   // SCTSPA	MTH_OAP	Yes
   // SCTSPA	MTH_OAS	Yes
   // SCTSPA	MTH_OF	Yes
   // SCTSPA	MTH_IS	Yes
   // MSHPOR	MH	No
   // MSHPOR	PEP	No
   // MSHPOR	ET	No
   // MSHSPA	MH	No
   // MSHSPA	PEP	No
   // MSHSPA	ET	No
   // MSHCZE	MH	No
   // MSHCZE	PEP	No
   // MSHCZE	ET	No
   // MSHDUT	MH	No
   // MSHSWE	MH	No
   // MSHSWE	TQ	No
   // MSHNOR	MH	No
   // MSHGER	MH	No
   // MSHNOR	PEP	No
   // MSHGER	PEP	No
   // MSHNOR	DSV	Yes
   // MSHGER	DSV	Yes
   // MSHNOR	ET	No
   // MSHGER	ET	No
   // MSHFIN	MH	No
   // MSHLAV	MH	No
   // MSHSCR	MH	No
   // MSHFRE	MH	No
   // MSHLAV	PEP	No
   // MSHSCR	PEP	No
   // MSHFRE	PEP	No
   // MSHLAV	EP	No
   // MSHSCR	ET	No
   // MSHFRE	ET	No
   // MSHITA	MH	No
   // MSHITA	PEP	No
   // MSHITA	ET	No
   // MSHJPN	PT	No
   // MSHPOL	MH	No
   // MSHRUS	MH	No
   // MSHJPN	SY	No
   // KCD5	HT	No
   // TKMT	PT	No
   // KCD5	PT	No
   // MSHPOL	SY	No
   // MSHRUS	SY	No
   // MSHDUT	SY	No
   // MDRSPA	PT	No
   // MDRSPA	HG	No
   // MDRSPA	HT	No
   // MDRSPA	LLT	No
   // MDRSPA	OS	No
   // MDRSPA	SMQ	No
   // MDRSPA	OL	Yes
   // MDRSPA	AB	Yes
   // MDRDUT	PT	No
   // MDRDUT	HG	No
   // MDRDUT	HT	No
   // MDRDUT	LLT	No
   // MDRDUT	OS	No
   // MDRDUT	SMQ	No
   // MDRDUT	OL	Yes
   // MDRDUT	AB	Yes
   // MDRFRE	PT	No
   // MDRFRE	HG	No
   // MDRFRE	HT	No
   // MDRFRE	LLT	No
   // MDRFRE	SMQ	No
   // MDRFRE	OS	No
   // MDRFRE	OL	Yes
   // MDRFRE	AB	Yes
   // MDRGER	PT	No
   // MDRGER	HG	No
   // MDRGER	HT	No
   // MDRGER	LLT	No
   // MDRGER	SMQ	No
   // MDRGER	OS	No
   // MDRGER	OL	Yes
   // MDRGER	AB	Yes
   // MDRITA	PT	No
   // MDRITA	HG	No
   // MDRITA	HT	No
   // MDRITA	LLT	No
   // MDRITA	SMQ	No
   // MDRITA	OS	No
   // MDRITA	OL	Yes
   // MDRITA	AB	Yes
   // MDRJPN	PT	No
   // MDRJPN	PTJKN	No
   // MDRJPN	PTJKN1	No
   // MDRJPN	HG	No
   // MDRJPN	HGJKN	No
   // MDRJPN	HGJKN1	No
   // MDRJPN	HT	No
   // MDRJPN	HTJKN	No
   // MDRJPN	HTJKN1	No
   // MDRJPN	LLT	No
   // MDRJPN	LLTJKN	No
   // MDRJPN	LLTJKN1	No
   // MDRJPN	OS	No
   // MDRJPN	SMQ	No
   // MDRJPN	OL	Yes
   // MDRJPN	OLJKN	Yes
   // MDRJPN	OLJKN1	Yes
   // MDRCZE	PT	No
   // MDRHUN	PT	No
   // MDRPOR	PT	No
   // MDRCZE	HG	No
   // MDRHUN	HG	No
   // MDRPOR	HG	No
   // MDRCZE	HT	No
   // MDRHUN	HT	No
   // MDRPOR	HT	No
   // MDRCZE	LLT	No
   // MDRHUN	LLT	No
   // MDRPOR	LLT	No
   // MDRCZE	OS	No
   // MDRHUN	OS	No
   // MDRPOR	OS	No
   // MDRCZE	SMQ	No
   // MDRHUN	SMQ	No
   // MDRPOR	SMQ	No
   // MDRCZE	OL	Yes
   // MDRHUN	OL	Yes
   // MDRPOR	OL	Yes
   // MDRCZE	AB	Yes
   // MDRHUN	AB	Yes
   // MDRPOR	AB	Yes
   // MDRJPN	OSJKN	No
   // MDRJPN	OSJKN1	No
   // WHOFRE	HT	No
   // WHOGER	HT	No
   // WHOPOR	HT	No
   // WHOSPA	HT	No
   // LNC-DE-CH	OSN	No
   // LNC-DE-CH	OOSN	Yes
   // LNC-DE-DE	LN	No
   // LNC-DE-DE	LO	Yes
   // LNC-EL-GR	LN	No
   // LNC-EL-GR	LO	Yes
   // LNC-ES-AR	LN	No
   // LNC-ES-AR	OSN	No
   // LNC-ES-AR	LO	Yes
   // LNC-ES-AR	OOSN	Yes
   // LNC-ES-CH	OSN	No
   // LNC-ES-CH	OOSN	Yes
   // LNC-ES-ES	LN	No
   // LNC-ES-ES	LO	Yes
   // LNC-ET-EE	LN	No
   // LNC-ET-EE	LO	Yes
   // LNC-FR-BE	LN	No
   // LNC-FR-BE	LO	Yes
   // LNC-FR-CA	LN	No
   // LNC-FR-CA	LO	Yes
   // LNC-FR-CH	OSN	No
   // LNC-FR-CH	OOSN	Yes
   // LNC-FR-FR	LN	No
   // LNC-FR-FR	LC	No
   // LNC-FR-FR	OLC	Yes
   // LNC-FR-FR	LO	Yes
   // LNC-IT-CH	OSN	No
   // LNC-IT-CH	OOSN	Yes
   // LNC-IT-IT	LN	No
   // LNC-IT-IT	LO	Yes
   // LNC-KO-KR	LN	No
   // LNC-KO-KR	LO	Yes
   // LNC-NL-NL	LN	No
   // LNC-NL-NL	LO	Yes
   // LNC-PT-BR	LN	No
   // LNC-PT-BR	OSN	No
   // LNC-PT-BR	LO	Yes
   // LNC-PT-BR	OOSN	Yes
   // LNC-RU-RU	LN	No
   // LNC-RU-RU	LO	Yes
   // LNC-TR-TR	LN	No
   // LNC-TR-TR	LO	Yes
   // LNC-ZH-CN	LN	No
   // LNC-ZH-CN	LO	Yes
   // LNC-DE-AT	LN	No
   // LNC-DE-AT	LO	Yes
   // LNC-DE-AT	LVDN	No
   // WHOFRE	PT	No
   // WHOGER	PT	No
   // WHOPOR	PT	No
   // WHOSPA	PT	No
   // WHOFRE	IT	No
   // WHOGER	IT	No
   // WHOPOR	IT	No
   // WHOSPA	IT	No
   // WHOFRE	OS	No
   // WHOGER	OS	No
   // WHOPOR	OS	No
   // WHOSPA	OS	No
   // CPTSP	PT	No
   // DMDUMD	PT	No
   // DMDUMD	ET	No
   // DMDUMD	RT	No
   // DMDICD10	PT	No
   // DMDICD10	HT	No
   // ICPCBAQ	PT	No
   // ICPCDAN	PT	No
   // ICPC2EDUT	PT	No
   // ICD10DUT	PT	No
   // ICD10DUT	HT	No
   // ICPC2ICD10DUT	PT	No
   // ICPCDUT	PT	No
   // ICPCFIN	PT	No
   // ICPCFRE	PT	No
   // ICPCGER	PT	No
   // ICPCHEB	PT	No
   // ICPCHUN	PT	No
   // ICPCITA	PT	No
   // ICPCNOR	PT	No
   // ICPCPOR	PT	No
   // ICPCSPA	PT	No
   // ICPCSWE	PT	No
   // ICPCBAQ	CP	No
   // ICPCDAN	CP	No
   // ICPCDUT	CP	No
   // ICPCFIN	CP	No
   // ICPCFRE	CP	No
   // ICPCGER	CP	No
   // ICPCHEB	CP	No
   // ICPCHUN	CP	No
   // ICPCITA	CP	No
   // ICPCNOR	CP	No
   // ICPCPOR	CP	No
   // ICPCSPA	CP	No
   // ICPCSWE	CP	No
   // MTHMSTFRE	PT	No
   // MTHMSTITA	PT	No
   // SRC	RPT	No
   // SRC	RHT	No
   // SRC	RAB	No
   // SRC	RSY	No
   // SRC	VPT	No
   // SRC	VAB	No
   // SRC	VSY	No
   // SRC	SSN	No
}
