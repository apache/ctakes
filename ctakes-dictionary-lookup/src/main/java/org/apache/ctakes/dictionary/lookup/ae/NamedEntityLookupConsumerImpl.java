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
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.LabMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;

/**
 * @author Mayo Clinic
 */
public class NamedEntityLookupConsumerImpl extends BaseLookupConsumerImpl {

   private static final String CODE_MF_PRP_KEY = "codeMetaField";

   private static final String CODING_SCHEME_PRP_KEY = "codingScheme";

   private static final String TYPE_ID_FIELD = "typeIdField";

   private final  Properties _properties;

   private static int iv_maxSize;

   public NamedEntityLookupConsumerImpl( final UimaContext aCtx, final Properties props, final int maxListSize ) {
      // TODO property validation could be done here
      _properties = props;
      iv_maxSize = maxListSize;
   }

   public NamedEntityLookupConsumerImpl( final UimaContext aCtx, final Properties props ) {
      // TODO property validation could be done here
      _properties = props;
   }

   private int countUniqueCodes( final Collection<LookupHit> hitsAtOffset ) {
      final String CODE_MF = _properties.getProperty( CODE_MF_PRP_KEY );
      final Set<String> codes = new HashSet<>();
      for ( LookupHit lookupHit : hitsAtOffset ) {
         final MetaDataHit mdh = lookupHit.getDictMetaDataHit();
         final String code = mdh.getMetaFieldValue( CODE_MF );
         codes.add( code );
      }
      return codes.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas, final Iterator<LookupHit> lhItr ) throws AnalysisEngineProcessException {
      final String TYPE_ID = _properties.getProperty( TYPE_ID_FIELD );
      final String CODE_MF = _properties.getProperty( CODE_MF_PRP_KEY );
      final String CODING_SCHEME = _properties.getProperty( CODING_SCHEME_PRP_KEY );
      int typeId = CONST.NE_TYPE_ID_UNKNOWN;
      if ( TYPE_ID != null ) {
         try {
            typeId = Integer.parseInt( TYPE_ID );
         } catch ( NumberFormatException nfe ) {
            typeId = CONST.NE_TYPE_ID_UNKNOWN;
         }
      }
      final Map<LookupHitKey, Set<LookupHit>> lookupHitMap = createLookupHitMap( lhItr );
      for ( Map.Entry<LookupHitKey, Set<LookupHit>> entry : lookupHitMap.entrySet() ) {
         final int uniqueCodeCount = countUniqueCodes( entry.getValue() );
         final FSArray ocArr = new FSArray( jcas, uniqueCodeCount );
         // iterate over the LookupHit objects and create
         // a corresponding JCas OntologyConcept object that will
         // be placed in a FSArray
         int ocArrIdx = 0;
         final Set<String> codes = new HashSet<>();
         for ( LookupHit lookupHit : entry.getValue() ) {
            final MetaDataHit mdh = lookupHit.getDictMetaDataHit();
            final String code = mdh.getMetaFieldValue( CODE_MF );
            if ( !codes.contains( code ) ) {
               // create only first entry in the array for a code
               final OntologyConcept oc = new OntologyConcept( jcas );
               oc.setCode( code );
               oc.setCodingScheme( CODING_SCHEME );
               ocArr.set( ocArrIdx, oc );
               ocArrIdx++;
               codes.add( code );
            }
         }
         
         IdentifiedAnnotation neAnnot = new IdentifiedAnnotation(jcas);
         if ( typeId == CONST.NE_TYPE_ID_DRUG ) {
            neAnnot = new MedicationMention( jcas );
         } else if ( typeId == CONST.NE_TYPE_ID_ANATOMICAL_SITE ) {
             neAnnot = new AnatomicalSiteMention( jcas );
         } else if ( typeId == CONST.NE_TYPE_ID_DISORDER ) {
             neAnnot = new DiseaseDisorderMention( jcas );
         } else if ( typeId == CONST.NE_TYPE_ID_FINDING ) {
             neAnnot = new SignSymptomMention( jcas );
         } else if ( typeId == CONST.NE_TYPE_ID_LAB ) {
             neAnnot = new LabMention( jcas );
         } else if ( typeId == CONST.NE_TYPE_ID_PROCEDURE ) {
             neAnnot = new ProcedureMention( jcas );
         } else {
             neAnnot = new EntityMention( jcas );
         }
         final int neBegin = entry.getKey().__start;
         final int neEnd = entry.getKey().__end;
         neAnnot.setBegin( neBegin );
         neAnnot.setEnd( neEnd );
         neAnnot.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
         neAnnot.setOntologyConceptArr( ocArr );
         neAnnot.setTypeID( typeId );
         neAnnot.addToIndexes();
      }
   }

}
