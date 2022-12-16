/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Template Cas Consumer to write a table to a sql database using jdbc
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 */
@PipeBitInfo(
      name = "JDBC Writer (Template)",
      description = "Stores extracted information and document metadata in a database.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class JdbcWriterTemplate extends AbstractJdbcWriter {

   static private final Logger LOGGER = Logger.getLogger( "JdbcWriterTemplate" );

   // Parameter names for the desc file
   static public final String PARAM_VECTOR_TABLE = "VectorTable";

   static protected final String SPAN_START_LABEL = "START";
   static protected final String SPAN_END_LABEL = "END";

   public enum I2b2FieldInfo implements AbstractJdbcWriter.FieldInfo {
      ENCOUNTER_NUM( 1, "encounter_num", Integer.class ),
      PATIENT_NUM( 2, "patient_num", Long.class ),
      CONCEPT_CD( 3, "concept_cd", String.class ),
      PROVIDER_ID( 4, "provider_id", String.class ),
      START_DATE( 5, "start_date", Timestamp.class ),
      MODIFIER_CD( 6, "modifier_cd", String.class ),
      INSTANCE_NUM( 7, "instance_num", Long.class ),
      VALTYPE_CD( 8, "valtype_cd", String.class ),
      TVAL_CHAR( 9, "tval_char", String.class ),
      I2B2_OBERVATION_BLOB( 10, "observation_blob", String.class );
      final private String __name;
      final private int __index;
      final private Class<?> __class;

      I2b2FieldInfo( final int index, final String name, final Class<?> valueClass ) {
         __name = name;
         __index = index;
         __class = valueClass;
      }

      @Override
      public String getFieldName() {
         return __name;
      }

      @Override
      public int getFieldIndex() {
         return __index;
      }

      @Override
      public Class<?> getValueClass() {
         return __class;
      }
   }


   private String _tableName;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      _tableName = (String)getConfigParameterValue( PARAM_VECTOR_TABLE );
      super.initialize();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected Collection<TableInfo> getTableInfos() {
      final TableInfo tableInfo = new TableInfo() {
         @Override
         public String getTableName() {
            return _tableName;
         }

         @Override
         public FieldInfo[] getFieldInfos() {
            return I2b2FieldInfo.values();
         }
      };
      return Collections.singletonList( tableInfo );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected void writeJCasInformation( final JCas jcas, final int encounterNum,
                                        final long patientNum, final String providerId,
                                        final Timestamp startDate ) throws SQLException {
      saveEntities( jcas, encounterNum, patientNum, providerId, startDate );
   }


   private void saveEntities( final JCas jcas, final int encounterNum, final long patientNum, final String providerId,
                              final Timestamp startDate ) throws SQLException {
      final AnnotationIndex<Annotation> identifiedsIndex = jcas.getAnnotationIndex( IdentifiedAnnotation.type );
      if ( identifiedsIndex == null || identifiedsIndex.size() == 0 ) {
         return;
      }
      final Map<I2b2FieldInfo, Object> fieldInfoValues = new EnumMap<>( I2b2FieldInfo.class );
      fieldInfoValues.put( I2b2FieldInfo.ENCOUNTER_NUM, encounterNum );
      fieldInfoValues.put( I2b2FieldInfo.PATIENT_NUM, patientNum );
      fieldInfoValues.put( I2b2FieldInfo.PROVIDER_ID, providerId );
      fieldInfoValues.put( I2b2FieldInfo.START_DATE, startDate );
      fieldInfoValues.put( I2b2FieldInfo.MODIFIER_CD, "@" );
      fieldInfoValues.put( I2b2FieldInfo.VALTYPE_CD, "T" );
      final Map<I2b2Concept, Collection<IdentifiedAnnotation>> cuiAnnotationListMap = new HashMap<>();
      for ( Annotation annotation : identifiedsIndex ) {
         if ( annotation instanceof IdentifiedAnnotation ) {
            final Collection<UmlsConcept> umlsConcepts
                  = OntologyConceptUtil.getUmlsConcepts( (IdentifiedAnnotation)annotation );
            final Collection<I2b2Concept> i2b2Concepts = createI2b2Concepts( umlsConcepts );
            for ( I2b2Concept i2b2Concept : i2b2Concepts ) {
               Collection<IdentifiedAnnotation> annotationList = cuiAnnotationListMap.get( i2b2Concept );
               if ( annotationList == null ) {
                  annotationList = new ArrayList<>();
                  cuiAnnotationListMap.put( i2b2Concept, annotationList );
               }
               annotationList.add( (IdentifiedAnnotation)annotation );
            }
         }
      }
      for ( Map.Entry<I2b2Concept, Collection<IdentifiedAnnotation>> i2b2ConceptAnnotations : cuiAnnotationListMap
            .entrySet() ) {
         saveI2b2Concept( jcas, fieldInfoValues, i2b2ConceptAnnotations.getKey(), i2b2ConceptAnnotations.getValue() );
      }
   }

   private void saveI2b2Concept( final JCas jcas, final Map<I2b2FieldInfo, Object> fieldInfoValues,
                                 final I2b2Concept i2b2Concept,
                                 final Iterable<IdentifiedAnnotation> annotations ) throws SQLException {
      final String cui = i2b2Concept.getCui();
      String preferredText = i2b2Concept.getPreferredText();
      if ( preferredText == null ) {
         preferredText = "";
      }
      // save Affirmed
      saveAnnotations( jcas, fieldInfoValues, cui, preferredText, annotations, true );
      // save negated
      saveAnnotations( jcas, fieldInfoValues, cui, preferredText, annotations, false );
   }

   /**
    * @param jcas            -
    * @param fieldInfoValues -
    * @param cui             -
    * @param preferredText   -
    * @param annotations     -
    * @param saveAffirmed    -
    * @throws SQLException
    */
   protected void saveAnnotations( final JCas jcas,
                                   final Map<I2b2FieldInfo, Object> fieldInfoValues,
                                 final String cui,
                                 final String preferredText,
                                 final Iterable<IdentifiedAnnotation> annotations,
                                 final boolean saveAffirmed ) throws SQLException {
      long instanceNum = 1;
      final String conceptCode = (saveAffirmed ? "" : "-") + cui;
      fieldInfoValues.put( I2b2FieldInfo.CONCEPT_CD, conceptCode );
      final String tvalChar = preferredText + (saveAffirmed ? "" : " Negated");
      fieldInfoValues.put( I2b2FieldInfo.TVAL_CHAR, tvalChar );
      // Do not use try with resource as it will close the prepared statement, which we do not yet want
      final PreparedStatement preparedStatement = _tableSqlInfoMap.get( _tableName ).getPreparedStatement();
      int batchCount = _tableSqlInfoMap.get( _tableName ).getBatchCount();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final boolean isNegated = annotation.getPolarity() < 0;
         if ( saveAffirmed == isNegated ) {
            continue;
         }
         fieldInfoValues.put( I2b2FieldInfo.INSTANCE_NUM, instanceNum );
         final String observationBlob = createBlob( jcas, annotation );
         fieldInfoValues.put( I2b2FieldInfo.I2B2_OBERVATION_BLOB, observationBlob );
         batchCount = writeTableRow( preparedStatement, batchCount, fieldInfoValues );
         instanceNum++;
      }
      _tableSqlInfoMap.get( _tableName ).setBatchCount( batchCount );
   }


   /**
    * I2b2 wants annotation begin and end offsets plus the covered text
    * @param jcas in case the blob needs to obtain annotation information from the jcas
    * @param annotation -
    * @return a blob with encoded text span and covered text of the annotation
    */
   protected String createBlob( final JCas jcas, final IdentifiedAnnotation annotation ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( '<' ).append( SPAN_START_LABEL ).append( '>' );
      sb.append( annotation.getBegin() );
      sb.append( "</" ).append( SPAN_START_LABEL ).append( '>' );
      sb.append( '<' ).append( SPAN_END_LABEL ).append( '>' );
      sb.append( annotation.getEnd() );
      sb.append( "</" ).append( SPAN_END_LABEL ).append( '>' );
      sb.append( annotation.getCoveredText() );
      return sb.toString();
   }

   /**
    * I2b2 only cares about Cui & preferred text.
    * A Cui may belong to multiple Tuis, making multiple UmlsConcept objects (one per tui).
    * I2b2 does NOT want multiple rows of a single Cui just because it has multiple tuis.
    *
    * @param umlsConcepts -
    * @return -
    */
   static private Collection<I2b2Concept> createI2b2Concepts( final Iterable<UmlsConcept> umlsConcepts ) {
      final Collection<I2b2Concept> i2b2Concepts = new HashSet<>();
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         // Because the hashcode for an I2b2Concept is created from Cui and PrefText, the "new" I2b2Concept
         // may not be unique.  No repeats will be stored
         i2b2Concepts.add( new I2b2Concept( umlsConcept.getCui(), umlsConcept.getPreferredText() ) );
      }
      return i2b2Concepts;
   }


   /**
    * A more useful representation of umls concept for our purposes - we don't want repeat cuis for multiple tuis
    */
   static private class I2b2Concept {

      static public final String PREFERRED_TEXT_UNKNOWN = "Unknown Preferred Text";

      final private String _cui;
      final private String _preferredText;

      final private int _hashcode;

      private I2b2Concept( final String cui ) {
         this( cui, PREFERRED_TEXT_UNKNOWN );
      }

      private I2b2Concept( final String cui, final String preferredText ) {
         _cui = cui;
         _preferredText = preferredText != null ? preferredText : PREFERRED_TEXT_UNKNOWN;
         _hashcode = (cui + "_" + preferredText).hashCode();
      }

      public String getCui() {
         return _cui;
      }

      public String getPreferredText() {
         return _preferredText;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals( final Object value ) {
         return value instanceof I2b2Concept
                && _cui.equals( ((I2b2Concept)value)._cui )
                && _preferredText.equals( ((I2b2Concept)value)._preferredText );
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode() {
         return _hashcode;
      }
   }


}
