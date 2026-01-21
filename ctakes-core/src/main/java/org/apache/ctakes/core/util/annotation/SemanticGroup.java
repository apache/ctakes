package org.apache.ctakes.core.util.annotation;

import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.ctakes.typesystem.type.constants.CONST.*;

/**
 * The major UMLS-related semantic groups as decided at the inception of cTAKES.
 * The major 5 groups are: Medication, Disease/Disorder, Sign/Symptom, Procedure, Anatomical Site.
 * Minor groups are: Clinical Attribute, Device, Lab, Phenomenon, Subject, Person Title, Event, Entity, Time, Modifier, Lab Modifier, and Unknown.
 */
public enum SemanticGroup {
   DRUG( NE_TYPE_ID_DRUG, "Drug", "Medication", MedicationMention.class, MedicationMention::new ),
   DISORDER( NE_TYPE_ID_DISORDER, "Disorder", "Disease/Disorder", DiseaseDisorderMention.class, DiseaseDisorderMention::new ),
   FINDING( NE_TYPE_ID_FINDING, "Finding", "Sign/Symptom", SignSymptomMention.class, SignSymptomMention::new ),
   PROCEDURE( NE_TYPE_ID_PROCEDURE, "Procedure", "Procedure", ProcedureMention.class, ProcedureMention::new ),
   ANATOMY( NE_TYPE_ID_ANATOMICAL_SITE, "Anatomy", "Anatomical Site", AnatomicalSiteMention.class, AnatomicalSiteMention::new ),
   CLINICAL_ATTRIBUTE( NE_TYPE_ID_CLINICAL_ATTRIBUTE, "Attribute", "Clinical Attribute", SignSymptomMention.class, SignSymptomMention::new ),
   DEVICE( NE_TYPE_ID_DEVICE, "Device", "Device", EntityMention.class, EntityMention::new ),
   LAB( NE_TYPE_ID_LAB, "Lab", "Lab", LabMention.class, LabMention::new ),
   PHENOMENON( NE_TYPE_ID_PHENOMENA, "Phenomenon", "Phenomenon", EventMention.class, EventMention::new ),
   SUBJECT( NE_TYPE_ID_SUBJECT_MODIFIER, "Subject", "Subject", SubjectModifier.class, SubjectModifier::new ),
   TITLE( NE_TYPE_ID_PERSON_TITLE, "Title", "Person Title", PersonTitleAnnotation.class, PersonTitleAnnotation::new ),
   EVENT( NE_TYPE_ID_GENERIC_EVENT, "Event", "Event", EventMention.class, EventMention::new ),
   ENTITY( NE_TYPE_ID_GENERIC_ENTITY, "Entity", "Entity", EntityMention.class, EntityMention::new ),
   TIME( NE_TYPE_ID_TIME_MENTION, "Time", "Timex3", TimeMention.class, TimeAnnotation::new ),
   MODIFIER( NE_TYPE_ID_GENERIC_MODIFIER, "Modifier", "Modifier", Modifier.class, Modifier::new ),
   LAB_MODIFIER( NE_TYPE_ID_LAB_VALUE_MODIFIER, "LabModifier", "Lab Modifier", LabValueModifier.class, LabValueModifier::new ),
   UNKNOWN( NE_TYPE_ID_UNKNOWN, "Unknown", "Unknown Semantic Group", IdentifiedAnnotation.class, IdentifiedAnnotation::new );

   private final int _code;
   private final String _name;
   private final String _longName;
   private final Class<? extends IdentifiedAnnotation> _clazz;
   private final Function<JCas, ? extends IdentifiedAnnotation> _creator;

   SemanticGroup( final int code, final String name,
                  final String longName,
                  final Class<? extends IdentifiedAnnotation> clazz,
                  final Function<JCas, ? extends IdentifiedAnnotation> creator ) {
      _code = code;
      _name = name;
      _longName = name;
      _clazz = clazz;
      _creator = creator;
   }

   /**
    * @return internal cTAKES CONST code for group. These were old constants that shouldn't be used.
    */
   public int getCode() {
      return _code;
   }

   /**
    * @return Short name for the group.  e.g. "Disease" or "Drug".
    */
   public String getName() {
      return _name;
   }

   /**
    * @return Long name for the group.  e.g. "Disease/Disorder" or "Medication".
    */
   public String getLongName() {
      return _longName;
   }

   /**
    * @return the cTAKES java / type system class that represents the group.
    */
   public Class<? extends IdentifiedAnnotation> getCtakesClass() {
      return _clazz;
   }

   /**
    * @return A function that can create the java / type system class that represents the group.
    */
   public Function<JCas, ? extends IdentifiedAnnotation> getCreator() {
      return _creator;
   }

   /**
    * @param code internal cTAKES CONST code for a group. These were old constants that shouldn't be used.
    * @return the group with the given code.
    */
   static public SemanticGroup getGroup( final int code ) {
      return Arrays.stream( values() )
            .filter( g -> g._code == code )
            .findFirst()
            .orElse( UNKNOWN );
   }

   /**
    * @param name -short- name for a group.
    * @return the group with the given -short- name.
    */
   static public SemanticGroup getGroup( final String name ) {
      return Arrays.stream( values() )
            .filter( g -> g._name.equals( name ) )
            .findFirst()
            .orElse( UNKNOWN );
   }

   /**
    * @param name old name for a group. These were old constants that shouldn't be used.
    * @return the group with the given -short- name.
    */
   static public SemanticGroup getGroupFromOld( final String name ) {
      return switch ( name ) {
         case "Disease_Disorder" -> DISORDER;
         case "Medications/Drugs" -> DRUG;
         case "Sign_symptom" -> FINDING;
         case "Anatomical_site" -> ANATOMY;
         default -> getGroup( name );
      };
   }

   /**
    * @param annotation Some annotation.
    * @return The groups appropriate for the annotation. Done using known TUIs for the annotation.
    */
   static public Collection<SemanticGroup> getGroups( final IdentifiedAnnotation annotation ) {
      final Collection<SemanticGroup> groups
            = SemanticTui.getTuis( annotation )
                         .stream()
                         .map( SemanticTui::getGroup )
                         .collect( Collectors.toSet() );
      final SemanticGroup typeIdGroup = getBestTypeIdGroup( annotation );
      if ( typeIdGroup == UNKNOWN || groups.contains( typeIdGroup ) ) {
         return groups;
      }
      final Collection<SemanticGroup> allGroups = new HashSet<>( groups );
      allGroups.add( typeIdGroup );
      return allGroups;
   }

   /**
    * Compares two groups for the most descriptive/accurate.  e.g. Procedure is before Phenomenon.
    */
   static private final class BestGrouper implements Comparator<SemanticGroup> {
      static private final BestGrouper INSTANCE = new BestGrouper();

      public int compare( final SemanticGroup g1, final SemanticGroup g2 ) {
         if ( g1 == SemanticGroup.UNKNOWN ) {
            return 1;
         }
         if ( g2 == SemanticGroup.UNKNOWN ) {
            return -1;
         }
         return g2._code - g1._code;
      }
   }

   /**
    * @param annotation Some annotation.
    * @return The most appropriate group for the annotation. Done using known TUIs for the annotation.
    */
   static public SemanticGroup getBestGroup( final IdentifiedAnnotation annotation ) {
      final SemanticGroup typeIdGroup = getBestTypeIdGroup( annotation );
      if ( typeIdGroup != UNKNOWN ) {
         return typeIdGroup;
      }
      return getBestGroup( getGroups( annotation ) );
   }

   /**
    * @param groups collection of groups.
    * @return The most appropriate group for the annotation.
    */
   static public SemanticGroup getBestGroup( final Collection<SemanticGroup> groups ) {
      return groups.stream()
                   .min( BestGrouper.INSTANCE )
                   .orElse( UNKNOWN );
   }

   /**
    * Gets the best group based upon internal cTAKES type code for a group.
    * These were old constants that shouldn't be used.  However, an annotation may have a type id but no TUIs.
    * @param annotation -
    * @return the group based upon a type id set in the annotation.
    */
   static private SemanticGroup getBestTypeIdGroup( final IdentifiedAnnotation annotation ) {
      final int typeId = annotation.getTypeID();
      return Arrays.stream( values() )
                   .filter( g -> g.getCode() == typeId )
                   .findFirst()
                   .orElse( UNKNOWN );
   }

}
