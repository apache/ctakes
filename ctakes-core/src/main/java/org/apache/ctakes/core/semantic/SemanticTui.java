package org.apache.ctakes.core.semantic;

import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.function.Function;

import static org.apache.ctakes.core.util.annotation.SemanticGroup.*;

/**
 * @deprecated use SemanticTui in core.util.annotation
 */
@Deprecated
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
   UNKNOWN( 0, "Unknown", SemanticGroup.UNKNOWN );

   private final int _code;
   private final String _name;
   private final SemanticGroup _group;

   SemanticTui( final int code, final String name, final SemanticGroup group ) {
      _code = code;
      _name = name;
      _group = group;
   }

   public int getCode() {
      return _code;
   }

   public String getSemanticType() {
      return _name;
   }

   public SemanticGroup getGroup() {
      return _group;
   }

   public int getGroupCode() {
      return _group.getCode();
   }

   public String getGroupName() {
      return _group.getName();
   }

   public Class<? extends IdentifiedAnnotation> getCtakesClass() {
      return _group.getCtakesClass();
   }

   public Function<JCas, ? extends IdentifiedAnnotation> getCreator() {
      return _group.getCreator();
   }

   static public org.apache.ctakes.core.util.annotation.SemanticTui getTui( final String semanticType ) {
      return org.apache.ctakes.core.util.annotation.SemanticTui.getTui( semanticType );
   }

   static public org.apache.ctakes.core.util.annotation.SemanticTui getTui( final int code ) {
      return org.apache.ctakes.core.util.annotation.SemanticTui.getTui( code );
   }

   static public org.apache.ctakes.core.util.annotation.SemanticTui getTuiFromCode( final String tuiCode ) {
      return org.apache.ctakes.core.util.annotation.SemanticTui.getTuiFromCode( tuiCode );
   }

   static public Collection<org.apache.ctakes.core.util.annotation.SemanticTui> getTuis(
         final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.SemanticTui.getTuis( annotation );
   }

   static public org.apache.ctakes.core.util.annotation.SemanticTui getTui( final UmlsConcept umlsConcept ) {
      return org.apache.ctakes.core.util.annotation.SemanticTui.getTui( umlsConcept );
   }

}
