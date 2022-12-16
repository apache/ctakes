package org.apache.ctakes.rest.service.response;

import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/1/2020
 */
final public class UmlsJsonFormatter implements ResponseFormatter {

   static private final List<Class<? extends IdentifiedAnnotation>> WANTED_CLASSES = Arrays.asList(
         // UMLS types:
         DiseaseDisorderMention.class,
         SignSymptomMention.class,
         ProcedureMention.class,
         AnatomicalSiteMention.class,
         MedicationMention.class
   );


   /**
    * Returns UMLS identified annotation information formatted in json.
    * {@inheritDoc}
    */
   @Override
   public String getResultText( final JCas jCas ) throws AnalysisEngineProcessException {
      final Map<String, List<UmlsObject>> umlsMap
            = JCasUtil.select( jCas, IdentifiedAnnotation.class ).stream()
                      .filter( a -> WANTED_CLASSES.contains( a.getClass() ) )
                      .map( UmlsObject::new )
                      .collect( Collectors.groupingBy( UmlsObject::getType ) );
      return new GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson( umlsMap );
   }


   static public class UmlsObject {
      final private String _type;
      final public int begin;
      final public int end;
      final public String text;
      final public Boolean negated;
      final public Boolean uncertain;
      final public Boolean generic;
      final public Boolean conditional;
      final public Boolean historic;

      public Map<String, List<OntologyObject>> conceptMap;

      private UmlsObject( final IdentifiedAnnotation annotation ) {
         _type = annotation.getClass().getSimpleName();
         begin = annotation.getBegin();
         end = annotation.getEnd();
         text = annotation.getCoveredText();
         negated = booleanOrNull( IdentifiedAnnotationUtil.isNegated( annotation ) );
         uncertain = booleanOrNull( IdentifiedAnnotationUtil.isUncertain( annotation ) );
         generic = booleanOrNull( IdentifiedAnnotationUtil.isGeneric( annotation ) );
         conditional = booleanOrNull( IdentifiedAnnotationUtil.isConditional( annotation ) );
         historic = booleanOrNull( IdentifiedAnnotationUtil.isHistoric( annotation ) );
         conceptMap
               = OntologyConceptUtil.getUmlsConceptStream( annotation )
                                    .map( OntologyObject::new )
                                    .collect( Collectors.groupingBy( OntologyObject::getSemanticGroup ) );
         if ( conceptMap.isEmpty() ) {
            conceptMap = null;
         }
      }

      final public String getType() {
         return _type;
      }

      // gson can ignore (not serialize) properties with null values.
      static private Boolean booleanOrNull( final boolean value ) {
         return value ? Boolean.TRUE : null;
      }
   }

   static private class OntologyObject {
      final public String semanticGroup;
      final public String semanticType;
      final public String preferredText;
      final public String cui;
      final public String tui;
      final public String codingScheme;
      final public String code;

      private OntologyObject( final UmlsConcept concept ) {
         preferredText = textOrNull( concept.getPreferredText() );
         cui = textOrNull( concept.getCui() );
         final SemanticTui semanticTui = SemanticTui.getTui( concept );
         semanticGroup = semanticTui.getGroupName();
         semanticType = semanticTui.getSemanticType();
         tui = textOrNull( concept.getTui() );
         codingScheme = textOrNull( concept.getCodingScheme() );
         code = textOrNull( concept.getCode() );
      }

      private String getSemanticGroup() {
         return semanticGroup;
      }

      // gson can ignore (not serialize) properties with null values.
      static private String textOrNull( final String text ) {
         return text.isEmpty() ? null : text;
      }
   }


}