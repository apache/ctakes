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

   public int getCode() {
      return _code;
   }

   public String getName() {
      return _name;
   }

   public String getLongName() {
      return _longName;
   }

   public Class<? extends IdentifiedAnnotation> getCtakesClass() {
      return _clazz;
   }

   public Function<JCas, ? extends IdentifiedAnnotation> getCreator() {
      return _creator;
   }

   static public SemanticGroup getGroup( final int code ) {
      return Arrays.stream( values() )
            .filter( g -> g._code == code )
            .findFirst()
            .orElse( UNKNOWN );
   }

   static public SemanticGroup getGroup( final String name ) {
      return Arrays.stream( values() )
            .filter( g -> g._name.equals( name ) )
            .findFirst()
            .orElse( UNKNOWN );
   }

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

   static public SemanticGroup getBestGroup( final IdentifiedAnnotation annotation ) {
      final SemanticGroup typeIdGroup = getBestTypeIdGroup( annotation );
      if ( typeIdGroup != UNKNOWN ) {
         return typeIdGroup;
      }
      return getBestGroup( getGroups( annotation ) );
   }

   static public SemanticGroup getBestGroup( final Collection<SemanticGroup> groups ) {
      return groups.stream()
                   .min( BestGrouper.INSTANCE )
                   .orElse( UNKNOWN );
   }

   static private SemanticGroup getBestTypeIdGroup( final IdentifiedAnnotation annotation ) {
      final int typeId = annotation.getTypeID();
      return Arrays.stream( values() )
                   .filter( g -> g.getCode() == typeId )
                   .findFirst()
                   .orElse( UNKNOWN );
   }

}
