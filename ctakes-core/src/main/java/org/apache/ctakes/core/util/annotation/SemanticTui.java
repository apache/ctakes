package org.apache.ctakes.core.util.annotation;

import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.util.annotation.SemanticGroup.*;

/**
 * The major UMLS-related semantic types as decided at the inception of cTAKES.
 * Each semantic type has a single UMLS TUI.  Semantic types are collected in Semantic Groups.
 * T116, "Amino Acid, Peptide, or Protein", DRUG
 * T020, "Acquired Abnormality", DISORDER
 * T052, "Activity", EVENT
 * T100, "Age Group", SUBJECT
 * T003, "Alga", ENTITY
 * T087, "Amino Acid Sequence", DRUG
 * T011, "Amphibian", ENTITY
 * T190, "Anatomical Abnormality", DISORDER
 * T008, "Animal", ENTITY
 * T017, "Anatomical Structure", ANATOMY
 * T195, "Antibiotic", DRUG
 * T194, "Archaeon", ENTITY
 * T123, "Biologically Active Substance", DRUG
 * T007, "Bacterium", ENTITY
 * T031, "Body Substance", FINDING
 * T022, "Body System", ANATOMY
 * T053, "Behavior", FINDING
 * T038, "Biologic Function", PHENOMENON
 * T012, "Bird", ENTITY
 * T029, "Body Location or Region", ANATOMY
 * T091, "Biomedical Occupation or Discipline", TITLE
 * T122, "Biomedical or Dental Material", DRUG
 * T023, "Body Part, Organ, or Organ Component", ANATOMY
 * T030, "Body Space or Junction", ANATOMY
 * T118, "Carbohydrate", DRUG
 * T026, "Cell Component", ANATOMY
 * T043, "Cell Function", FINDING
 * T025, "Cell", ANATOMY
 * T019, "Congenital Abnormality", DISORDER
 * T103, "Chemical", DRUG
 * T120, "Chemical Viewed Functionally", DRUG
 * T104, "Chemical Viewed Structurally", DRUG
 * T185, "Classification", FINDING
 * T201, "Clinical Attribute", CLINICAL_ATTRIBUTE
 * T200, "Clinical Drug", DRUG
 * T077, "Conceptual Entity", FINDING
 * T049, "Cell or Molecular Dysfunction", DISORDER
 * T088, "Carbohydrate Sequence", DRUG
 * T060, "Diagnostic Procedure", PROCEDURE
 * T056, "Daily or Recreational Activity", FINDING
 * T047, "Disease or Syndrome", DISORDER
 * T203, "Drug Delivery Device", DEVICE
 * T065, "Educational Activity", PROCEDURE
 * T069, "Environmental Effect of Humans", PHENOMENON
 * T111, "Eicosanoid", ENTITY
 * T196, "Element, Ion, or Isotope", DRUG
 * T050, "Experimental Model of Disease", DISORDER
 * T018, "Embryonic Structure", ANATOMY
 * T071, "Entity", ENTITY
 * T126, "Enzyme", DRUG
 * T051, "Event", EVENT
 * T099, "Family Group", SUBJECT
 * T021, "Fully Formed Anatomical Structure", ANATOMY
 * T013, "Fish", ENTITY
 * T033, "Finding", FINDING
 * T004, "Fungus", ENTITY
 * T168, "Food", DRUG
 * T169, "Functional Concept", FINDING
 * T045, "Genetic Function", FINDING
 * T083, "Geographic Area", ENTITY
 * T028, "Gene or Genome", FINDING
 * T064, "Governmental or Regulatory Activity", EVENT
 * T102, "Group Attribute", SUBJECT
 * T096, "Group", SUBJECT
 * T068, "Human-caused Phenomenon or Process", PHENOMENON
 * T093, "Health Care Related Organization", ENTITY
 * T058, "Health Care Activity", PROCEDURE
 * T131, "Hazardous or Poisonous Substance", DRUG
 * T125, "Hormone", DRUG
 * T016, "Human", SUBJECT
 * T078, "Idea or Concept", FINDING
 * T129, "Immunologic Factor", DRUG
 * T055, "Individual Behavior", FINDING
 * T197, "Inorganic Chemical", DRUG
 * T037, "Injury or Poisoning", DISORDER
 * T170, "Intellectual Product", FINDING
 * T009, "Invertebrate", ENTITY
 * T130, "Indicator, Reagent, or Diagnostic Aid", DRUG
 * T171, "Language", ENTITY
 * T059, "Laboratory Procedure", PROCEDURE
 * T034, "Laboratory or Test Result", LAB
 * T119, "Lipid", DRUG
 * T015, "Mammal", ENTITY
 * T063, "Molecular Biology Research Technique", PROCEDURE
 * T066, "Machine Activity", PROCEDURE
 * T074, "Medical Device", DEVICE
 * T041, "Mental Process", FINDING
 * T073, "Manufactured Object", DEVICE
 * T048, "Mental or Behavioral Dysfunction", DISORDER
 * T044, "Molecular Function", FINDING
 * T085, "Molecular Sequence", FINDING
 * T191, "Neoplastic Process", DISORDER
 * T114, "Nucleic Acid, Nucleoside, or Nucleotide", DRUG
 * T070, "Natural Phenomenon or Process", PHENOMENON
 * T124, "Neuroreactive Substance or Biogenic Amine", DRUG
 * T086, "Nucleotide Sequence", FINDING
 * T057, "Occupational Activity", EVENT
 * T090, "Occupation or Discipline", SUBJECT
 * T115, "Organophosphorous Compound", DRUG
 * T109, "Organic Chemical", DRUG
 * T032, "Organism Attribute", SUBJECT
 * T040, "Organism Function", FINDING
 * T001, "Organism", ENTITY
 * T092, "Organization", ENTITY
 * T042, "Organ or * Tissue Function", FINDING
 * T046, "Pathologic Function", FINDING
 * T072, "Physical Object", ENTITY
 * T067, "Phenomenon or Process", PHENOMENON
 * T039, "Physiologic Function", FINDING
 * T121, "Pharmacologic Substance", DRUG
 * T002, "Plant", ENTITY
 * T101, "Patient or Disabled Group", SUBJECT
 * T098, "Population Group", SUBJECT
 * T097, "Professional or Occupational Group", SUBJECT
 * T094, "Professional Society", ENTITY
 * T080, "Qualitative Concept", MODIFIER
 * T081, "Quantitative Concept", LAB_MODIFIER
 * T192, "Receptor", FINDING
 * T014, "Reptile", ENTITY
 * T062, "Research Activity", PROCEDURE
 * T075, "Research Device", DEVICE
 * T006, "Rickettsia or Chlamydia", DISORDER
 * T089, "Regulation or Law", ENTITY
 * T167, "Substance", DRUG
 * T095, "Self-help or Relief Organization", ENTITY
 * T054, "Social Behavior", FINDING
 * T184, "Sign or Symptom", FINDING
 * T082, "Spatial Concept", MODIFIER
 * T110, "Steroid", DRUG
 * T024, "Tissue", ANATOMY
 * T079, "Temporal Concept", TIME
 * T061, "Therapeutic or Preventive Procedure", PROCEDURE
 * T005, "Virus", DISORDER
 * T127, "Vitamin", DRUG
 * T010, "Vertebrate", ENTITY
 * T204, "Eukaryote", ENTITY
 * UNKNOWN, "Unknown", SemanticGroup.UNKNOWN
 */
public enum SemanticTui {
   T116( 116, "Amino Acid, Peptide, or Protein", DRUG ),
   T020( 20, "Acquired Abnormality", DISORDER ),
   T052( 52, "Activity", EVENT ),
   T100( 100, "Age Group", SUBJECT ),
   T003( 3, "Alga", ENTITY ),
   T087( 87, "Amino Acid Sequence", DRUG ),
   T011( 11, "Amphibian", ENTITY ),
   T190( 190, "Anatomical Abnormality", DISORDER ),
   T008( 8, "Animal", ENTITY ),
   T017( 17, "Anatomical Structure", ANATOMY ),
   T195( 195, "Antibiotic", DRUG ),
   T194( 194, "Archaeon", ENTITY ),
   T123( 123, "Biologically Active Substance", DRUG ),
   T007( 7, "Bacterium", ENTITY ),
   T031( 31, "Body Substance", FINDING ),
   T022( 22, "Body System", ANATOMY ),
   T053( 53, "Behavior", FINDING ),
   T038( 38, "Biologic Function", PHENOMENON ),
   T012( 12, "Bird", ENTITY ),
   T029( 29, "Body Location or Region", ANATOMY ),
   T091( 91, "Biomedical Occupation or Discipline", TITLE ),
   T122( 122, "Biomedical or Dental Material", DRUG ),
   T023( 23, "Body Part, Organ, or Organ Component", ANATOMY ),
   T030( 30, "Body Space or Junction", ANATOMY ),
   T118( 118, "Carbohydrate", DRUG ),
   T026( 26, "Cell Component", ANATOMY ),
   T043( 43, "Cell Function", FINDING ),
   T025( 25, "Cell", ANATOMY ),
   T019( 19, "Congenital Abnormality", DISORDER ),
   T103( 103, "Chemical", DRUG ),
   T120( 120, "Chemical Viewed Functionally", DRUG ),
   T104( 104, "Chemical Viewed Structurally", DRUG ),
   T185( 185, "Classification", FINDING ),
   T201( 201, "Clinical Attribute", CLINICAL_ATTRIBUTE ),
   T200( 200, "Clinical Drug", DRUG ),
   T077( 77, "Conceptual Entity", FINDING ),
   T049( 49, "Cell or Molecular Dysfunction", DISORDER ),
   T088( 88, "Carbohydrate Sequence", DRUG ),
   T060( 60, "Diagnostic Procedure", PROCEDURE ),
   T056( 56, "Daily or Recreational Activity", FINDING ),
   T047( 47, "Disease or Syndrome", DISORDER ),
   T203( 203, "Drug Delivery Device", DEVICE ),
   T065( 65, "Educational Activity", PROCEDURE ),
   T069( 69, "Environmental Effect of Humans", PHENOMENON ),
   T111( 111, "Eicosanoid", ENTITY ),
   T196( 196, "Element, Ion, or Isotope", DRUG ),
   T050( 50, "Experimental Model of Disease", DISORDER ),
   T018( 18, "Embryonic Structure", ANATOMY ),
   T071( 71, "Entity", ENTITY ),
   T126( 126, "Enzyme", DRUG ),
   T051( 51, "Event", EVENT ),
   T099( 99, "Family Group", SUBJECT ),
   T021( 21, "Fully Formed Anatomical Structure", ANATOMY ),
   T013( 13, "Fish", ENTITY ),
   T033( 33, "Finding", FINDING ),
   T004( 4, "Fungus", ENTITY ),
   T168( 168, "Food", DRUG ),
   T169( 169, "Functional Concept", FINDING ),
   // double-check
   T045( 45, "Genetic Function", FINDING ),
   T083( 83, "Geographic Area", ENTITY ),
   T028( 28, "Gene or Genome", FINDING ),
   T064( 64, "Governmental or Regulatory Activity", EVENT ),
   T102( 102, "Group Attribute", SUBJECT ),
   T096( 96, "Group", SUBJECT ),
   T068( 68, "Human-caused Phenomenon or Process", PHENOMENON ),
   T093( 93, "Health Care Related Organization", ENTITY ),
   T058( 58, "Health Care Activity", PROCEDURE ),
   T131( 131, "Hazardous or Poisonous Substance", DRUG ),
   T125( 125, "Hormone", DRUG ),
   T016( 16, "Human", SUBJECT ),
   T078( 78, "Idea or Concept", FINDING ),
   T129( 129, "Immunologic Factor", DRUG ),
   T055( 55, "Individual Behavior", FINDING ),
   T197( 197, "Inorganic Chemical", DRUG ),
   T037( 37, "Injury or Poisoning", DISORDER ),
   T170( 170, "Intellectual Product", FINDING ),
   // double-check
   T009( 9, "Invertebrate", ENTITY ),
   T130( 130, "Indicator, Reagent, or Diagnostic Aid", DRUG ),
   T171( 171, "Language", ENTITY ),
   T059( 59, "Laboratory Procedure", PROCEDURE ),
   T034( 34, "Laboratory or Test Result", LAB ),
   T119( 119, "Lipid", DRUG ),
   T015( 15, "Mammal", ENTITY ),
   T063( 63, "Molecular Biology Research Technique", PROCEDURE ),
   T066( 66, "Machine Activity", PROCEDURE ),
   T074( 74, "Medical Device", DEVICE ),
   T041( 41, "Mental Process", FINDING ),
   T073( 73, "Manufactured Object", DEVICE ),
   T048( 48, "Mental or Behavioral Dysfunction", DISORDER ),
   T044( 44, "Molecular Function", FINDING ),
   T085( 85, "Molecular Sequence", FINDING ),
   T191( 191, "Neoplastic Process", DISORDER ),
   T114( 114, "Nucleic Acid, Nucleoside, or Nucleotide", DRUG ),
   T070( 70, "Natural Phenomenon or Process", PHENOMENON ),
   T124( 124, "Neuroreactive Substance or Biogenic Amine", DRUG ),
   T086( 86, "Nucleotide Sequence", FINDING ),
   T057( 57, "Occupational Activity", EVENT ),
   T090( 90, "Occupation or Discipline", SUBJECT ),
   T115( 115, "Organophosphorous Compound", DRUG ),
   T109( 109, "Organic Chemical", DRUG ),
   T032( 32, "Organism Attribute", SUBJECT ),
   T040( 40, "Organism Function", FINDING ),
   T001( 1, "Organism", ENTITY ),
   T092( 92, "Organization", ENTITY ),
   T042( 42, "Organ or Tissue Function", FINDING ),
   T046( 46, "Pathologic Function", FINDING ),
   T072( 72, "Physical Object", ENTITY ),
   T067( 67, "Phenomenon or Process", PHENOMENON ),
   T039( 39, "Physiologic Function", FINDING ),
   T121( 121, "Pharmacologic Substance", DRUG ),
   T002( 2, "Plant", ENTITY ),
   T101( 101, "Patient or Disabled Group", SUBJECT ),
   T098( 98, "Population Group", SUBJECT ),
   T097( 97, "Professional or Occupational Group", SUBJECT ),
   T094( 94, "Professional Society", ENTITY ),
   T080( 80, "Qualitative Concept", MODIFIER ),
   T081( 81, "Quantitative Concept", LAB_MODIFIER ),
   T192( 192, "Receptor", FINDING ),
   T014( 14, "Reptile", ENTITY ),
   T062( 62, "Research Activity", PROCEDURE ),
   T075( 75, "Research Device", DEVICE ),
   T006( 6, "Rickettsia or Chlamydia", DISORDER ),
   T089( 89, "Regulation or Law", ENTITY ),
   T167( 167, "Substance", DRUG ),
   // Double-check
   T095( 95, "Self-help or Relief Organization", ENTITY ),
   T054( 54, "Social Behavior", FINDING ),
   T184( 184, "Sign or Symptom", FINDING ),
   T082( 82, "Spatial Concept", MODIFIER ),
   T110( 110, "Steroid", DRUG ),
   T024( 24, "Tissue", ANATOMY ),
   T079( 79, "Temporal Concept", TIME ),
   T061( 61, "Therapeutic or Preventive Procedure", PROCEDURE ),
   T005( 5, "Virus", DISORDER ),
   T127( 127, "Vitamin", DRUG ),
   T010( 10, "Vertebrate", ENTITY ),
   T204( 204, "Eukaryote", ENTITY ),
   UNKNOWN( 0, "Unknown", SemanticGroup.UNKNOWN );

   private final int _code;
   private final String _name;
   private SemanticGroup _group;

   SemanticTui( final int code, final String name, final SemanticGroup group ) {
      _code = code;
      _name = name;
      _group = group;
   }

   /**
    * @return type for UMLS TUI.
    */
   public int getCode() {
      return _code;
   }

   /**
    * @return Name for the type.
    */
   public String getSemanticType() {
      return _name;
   }

   /**
    * Allows a user to override the semantic group associated with a tui.
    * This is useful when differentiating things like chemicals and drugs.
    *
    * @param group -
    */
   public void setGroup( final SemanticGroup group ) {
      _group = group;
   }

   /**
    * @return semantic group for this type.
    */
   public SemanticGroup getGroup() {
      return _group;
   }

   /**
    * @return internal cTAKES CONST code for group. These were old constants that shouldn't be used.
    */
   public int getGroupCode() {
      return _group.getCode();
   }

   /**
    * @return Short name for the group for this type.  e.g. "Disease" or "Drug".
    */
   public String getGroupName() {
      return _group.getName();
   }

   /**
    * @return the cTAKES java / type system class that represents the group for this type.
    */
   public Class<? extends IdentifiedAnnotation> getCtakesClass() {
      return _group.getCtakesClass();
   }

   /**
    * @return A function that can create the java / type system class that represents the group for this type.
    */
   public Function<JCas, ? extends IdentifiedAnnotation> getCreator() {
      return _group.getCreator();
   }

   /**
    * @return name of this type to lowercase and without commas.
    */
   private String getMatchType() {
      return getMatchable( _name );
   }

   /**
    * @param semanticType name of a semantic type.
    * @return Type for the given type name.
    */
   static public SemanticTui getTui( final String semanticType ) {
      // Attempt to match name ( e.g. "Cell" ).
      final String toMatch = getMatchable( semanticType );
      for ( SemanticTui tui : SemanticTui.values() ) {
         if ( tui.getMatchType()
                 .equals( toMatch ) ) {
            return tui;
         }
      }
      // Attempt to match code ( e.g. "T001" ).
      return getTuiFromCode( toMatch );
   }

   /**
    * @param code UMLS TUI code of a semantic type, not including "T" prefix.
    * @return Type for the given TUI.
    */
   static public SemanticTui getTui( final int code ) {
      for ( SemanticTui tui : SemanticTui.values() ) {
         if ( tui.getCode() == code ) {
            return tui;
         }
      }
      return UNKNOWN;
   }

   /**
    * @param tuiCode UMLS TUI code of a semantic type, including "T" prefix.
    * @return Type for the given TUI.
    */
   static public SemanticTui getTuiFromCode( final String tuiCode ) {
      for ( SemanticTui tui : SemanticTui.values() ) {
         if ( tui.name()
                 .equalsIgnoreCase( tuiCode ) ) {
            return tui;
         }
      }
      return UNKNOWN;

   }

   /**
    * @param annotation Some annotation.
    * @return The types appropriate for the annotation. Done using known TUIs for the annotation.
    */
   static public Collection<SemanticTui> getTuis( final IdentifiedAnnotation annotation ) {
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      if ( umlsConcepts != null && !umlsConcepts.isEmpty() ) {
         return umlsConcepts.stream()
               .map( UmlsConcept::getTui )
               .distinct()
               .map( SemanticTui::getTuiFromCode )
               .collect( Collectors.toList() );
      }
      if ( annotation instanceof EventMention ) {
         return Collections.singletonList( T051 );
      } else if ( annotation instanceof TimeMention ) {
         return Collections.singletonList( T079 );
      }
      return Collections.singletonList( UNKNOWN );
   }

   /**
    * @param umlsConcept Some UMLS concept of a real-world thing.
    * @return The type appropriate for the concept.
    */
   static public SemanticTui getTui( final UmlsConcept umlsConcept ) {
      return getTuiFromCode( umlsConcept.getTui() );
   }

   /**
    * Allows a user to override the semantic group associated with a tui.
    * This is useful when differentiating things like chemicals and drugs.
    *
    * @param tui   -
    * @param group -
    */
   static public void setGroup( final int tui, final String group ) {
      getTui( tui ).setGroup( SemanticGroup.getGroup( group ) );
   }

   /**
    * Allows a user to override the semantic group associated with a tui.
    * This is useful when differentiating things like chemicals and drugs.
    *
    * @param type  -
    * @param group -
    */
   static public void setGroup( final String type, final String group ) {
      getTui( type ).setGroup( SemanticGroup.getGroup( group ) );
   }

   /**
    * @param text -
    * @return text to lowercase and without commas.
    */
   static private String getMatchable( final String text ) {
      return text.toLowerCase()
                 .replaceAll( ",", "" );
   }

}
