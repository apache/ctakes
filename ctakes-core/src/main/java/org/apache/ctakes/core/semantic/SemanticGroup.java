package org.apache.ctakes.core.semantic;

import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.function.Function;

import static org.apache.ctakes.typesystem.type.constants.CONST.*;

/**
 * @deprecated use SemanticGroup in core.util.annotation
 */
@Deprecated
public enum SemanticGroup {
   DRUG( NE_TYPE_ID_DRUG, "Drug", MedicationMention.class, MedicationMention::new ),
   DISORDER( NE_TYPE_ID_DISORDER, "Disorder", DiseaseDisorderMention.class, DiseaseDisorderMention::new ),
   FINDING( NE_TYPE_ID_FINDING, "Finding", SignSymptomMention.class, SignSymptomMention::new ),
   PROCEDURE( NE_TYPE_ID_PROCEDURE, "Procedure", ProcedureMention.class, ProcedureMention::new ),
   ANATOMY( NE_TYPE_ID_ANATOMICAL_SITE, "Anatomy", AnatomicalSiteMention.class, AnatomicalSiteMention::new ),
   CLINICAL_ATTRIBUTE( NE_TYPE_ID_CLINICAL_ATTRIBUTE, "Attribute", SignSymptomMention.class, SignSymptomMention::new ),
   DEVICE( NE_TYPE_ID_DEVICE, "Device", EntityMention.class, EntityMention::new ),
   LAB( NE_TYPE_ID_LAB, "Lab", LabMention.class, LabMention::new ),
   PHENOMENON( NE_TYPE_ID_PHENOMENA, "Phenomenon", EventMention.class, EventMention::new ),
   SUBJECT( NE_TYPE_ID_SUBJECT_MODIFIER, "Subject", SubjectModifier.class, SubjectModifier::new ),
   TITLE( NE_TYPE_ID_PERSON_TITLE, "Title", PersonTitleAnnotation.class, PersonTitleAnnotation::new ),
   EVENT( NE_TYPE_ID_GENERIC_EVENT, "Event", EventMention.class, EventMention::new ),
   ENTITY( NE_TYPE_ID_GENERIC_ENTITY, "Entity", EntityMention.class, EntityMention::new ),
   TIME( NE_TYPE_ID_TIME_MENTION, "Time", TimeMention.class, TimeAnnotation::new ),
   MODIFIER( NE_TYPE_ID_GENERIC_MODIFIER, "Modifier", Modifier.class, Modifier::new ),
   LAB_MODIFIER( NE_TYPE_ID_LAB_VALUE_MODIFIER, "LabModifier", LabValueModifier.class, LabValueModifier::new ),
   UNKNOWN( NE_TYPE_ID_UNKNOWN, "Unknown", IdentifiedAnnotation.class, IdentifiedAnnotation::new );

   private final int _code;
   private final String _name;
   private final Class<? extends IdentifiedAnnotation> _clazz;
   private final Function<JCas, ? extends IdentifiedAnnotation> _creator;

   SemanticGroup( final int code, final String name,
                  final Class<? extends IdentifiedAnnotation> clazz,
                  final Function<JCas, ? extends IdentifiedAnnotation> creator ) {
      _code = code;
      _name = name;
      _clazz = clazz;
      _creator = creator;
   }

   public int getCode() {
      return _code;
   }

   public String getName() {
      return _name;
   }

   public Class<? extends IdentifiedAnnotation> getCtakesClass() {
      return _clazz;
   }

   public Function<JCas, ? extends IdentifiedAnnotation> getCreator() {
      return _creator;
   }

   static public org.apache.ctakes.core.util.annotation.SemanticGroup getGroup( final int code ) {
      return org.apache.ctakes.core.util.annotation.SemanticGroup.getGroup( code );
   }

   static public org.apache.ctakes.core.util.annotation.SemanticGroup getGroup( final String name ) {
      return org.apache.ctakes.core.util.annotation.SemanticGroup.getGroup( name );
   }

   static public Collection<org.apache.ctakes.core.util.annotation.SemanticGroup> getGroups(
         final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.SemanticGroup.getGroups( annotation );
   }

   static public org.apache.ctakes.core.util.annotation.SemanticGroup getBestGroup(
         final IdentifiedAnnotation annotation ) {
      return org.apache.ctakes.core.util.annotation.SemanticGroup.getBestGroup( getGroups( annotation ) );
   }

   static public org.apache.ctakes.core.util.annotation.SemanticGroup getBestGroup(
         final Collection<org.apache.ctakes.core.util.annotation.SemanticGroup> groups ) {
      return org.apache.ctakes.core.util.annotation.SemanticGroup.getBestGroup( groups );
   }

}
