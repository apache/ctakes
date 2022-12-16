package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

@PipeBitInfo(
      name = "Medication Table Writer",
      description = "Writes a table of Medication information to file, sorted by character index.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class MedicationTableFileWriter extends AbstractTableFileWriter {
// test line for checkin, post infra https://issues.apache.org/jira/browse/INFRA-21596

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Preferred Text ",
            " Document Text ",
            " Strength ",
            " Dose ",
            " Form ",
            " Route ",
            " Frequency ",
            " Duration " );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      return JCasUtil.select( jCas, MedicationMention.class )
                     .stream()
                     .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                     .map( ModifierRow::new )
                     .map( ModifierRow::getColumns )
                     .collect( Collectors.toList() );
   }


   /**
    * Simple container for annotation information.
    */
   static private class ModifierRow {

      private final String _prefText;
      private final String _docText;
      private final String _strength;
      private final String _dose;
      private final String _form;
      private final String _route;
      private final String _frequency;
      private final String _duration;

      private ModifierRow( final MedicationMention med ) {
         _prefText = IdentifiedAnnotationUtil.getPreferredTexts( med )
                                             .stream()
                                             .sorted()
                                             .collect( Collectors.joining( ";" ) );
         _docText = med.getCoveredText();
         final MedicationStrengthModifier strengthMod = med.getMedicationStrength();
         if ( strengthMod == null ) {
            _strength = "";
         } else {
            final Attribute strength = strengthMod.getNormalizedForm();
            _strength = strength instanceof MedicationStrength
                        ?
                        ( (MedicationStrength) strength ).getNumber() + " " + ( (MedicationStrength) strength ).getUnit()
                        : "";
         }
         final MedicationDosageModifier doseMod = med.getMedicationDosage();
         if ( doseMod == null ) {
            _dose = "";
         } else {
            final Attribute dose = doseMod.getNormalizedForm();
            _dose = dose instanceof MedicationDosage
                    ? ( (MedicationDosage) dose ).getValue()
                    : "";
         }
         final MedicationFormModifier formMod = med.getMedicationForm();
         if ( formMod == null ) {
            _form = "";
         } else {
            final Attribute form = formMod.getNormalizedForm();
            _form = form instanceof MedicationForm
                    ? ( (MedicationForm) form ).getValue()
                    : "";
         }
         final MedicationRouteModifier routeMod = med.getMedicationRoute();
         if ( routeMod == null ) {
            _route = "";
         } else {
            final Attribute route = routeMod.getNormalizedForm();
            _route = route instanceof MedicationRoute
                     ? ( (MedicationRoute) route ).getValue()
                     : "";
         }
         final MedicationFrequencyModifier freqMod = med.getMedicationFrequency();
         if ( freqMod == null ) {
            _frequency = "";
         } else {
            final Attribute frequency = freqMod.getNormalizedForm();
            _frequency = frequency instanceof MedicationFrequency
                         ? ( (MedicationFrequency) frequency ).getNumber() + " "
                           + ( (MedicationFrequency) frequency ).getUnit()
                         : "";
         }
         final MedicationDurationModifier durationMod = med.getMedicationDuration();
         if ( durationMod == null ) {
            _duration = "";
         } else {
            final Attribute duration = durationMod.getNormalizedForm();
            _duration = duration instanceof MedicationDuration
                        ? ( (MedicationDuration) duration ).getValue()
                        : "";
         }
      }

      public List<String> getColumns() {
         return Arrays.asList(
               _prefText,
               _docText,
               _strength,
               _dose,
               _form,
               _route,
               _frequency.replace( " null", "" ),
               _duration );
      }

   }


}

