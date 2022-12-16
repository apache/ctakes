/**
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

import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
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

import java.sql.SQLException;
import java.util.*;

/**
 * Implementation that takes UMLS dictionary lookup hits and stores as NamedEntity
 * objects only the ones that have a SNOMED synonym.
 * Override abstract method <code>getSnomedCodes</code> and implement
 * looking up the CUI->SNOMED mappings
 *
 * @author Mayo Clinic
 */
public abstract class UmlsToSnomedConsumerImpl extends BaseLookupConsumerImpl {

   static private final String CUI_MF_PRP_KEY = "cuiMetaField";
   static private final String TUI_MF_PRP_KEY = "tuiMetaField";

   static private final String CODING_SCHEME_PRP_KEY = "codingScheme";

   static private final String MEDICATION_TUIS_PRP_KEY = "medicationTuis";
   static private final String ANT_SITE_TUIS_PRP_KEY = "anatomicalSiteTuis";
   static private final String PROCEDURE_TUIS_PRP_KEY = "procedureTuis";
   static private final String DISORDER_TUIS_PRP_KEY = "disorderTuis";
   static private final String FINDING_TUIS_PRP_KEY = "findingTuis";

   private Set<String> _medicationSet = new HashSet<>();
   private Set<String> _antSiteTuiSet = new HashSet<>();
   private Set<String> _procedureTuiSet = new HashSet<>();
   private Set<String> _disorderTuiSet = new HashSet<>();
   private Set<String> _findingTuiSet = new HashSet<>();
   private Set<String> _validTuiSet = new HashSet<>();

   protected Properties props;


   public UmlsToSnomedConsumerImpl( final UimaContext aCtx, final Properties properties ) throws Exception {
      // TODO property validation could be done here
      props = properties;

      _medicationSet = loadList( props.getProperty( MEDICATION_TUIS_PRP_KEY ) ); // 1
      _antSiteTuiSet = loadList( props.getProperty( ANT_SITE_TUIS_PRP_KEY ) );   // 6
      _procedureTuiSet = loadList( props.getProperty( PROCEDURE_TUIS_PRP_KEY ) );// 5
      _disorderTuiSet = loadList( props.getProperty( DISORDER_TUIS_PRP_KEY ) );  // 2
      _findingTuiSet = loadList( props.getProperty( FINDING_TUIS_PRP_KEY ) );    // 3  aka sign/symptom

      _validTuiSet.addAll( _medicationSet );
      _validTuiSet.addAll( _antSiteTuiSet );
      _validTuiSet.addAll( _procedureTuiSet );
      _validTuiSet.addAll( _disorderTuiSet );
      _validTuiSet.addAll( _findingTuiSet );
   }


   /**
    * Searches for the Snomed codes that are synonyms of the UMLS concept with CUI <code>umlsCode</code>
    *
    * @param umlsCode                                   -
    * @return Set of SNOMED codes for the given UMLS CUI.
    * @throws SQLException, DictionaryException
    */
   protected abstract Set<String> getSnomedCodes( final String umlsCode ) throws SQLException, DictionaryException;


   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas, final Iterator<LookupHit> lhItr ) throws AnalysisEngineProcessException {
      try {
         final String cuiPropKey = props.getProperty( CUI_MF_PRP_KEY );
         final String tuiPropKey = props.getProperty( TUI_MF_PRP_KEY );
         final Map<LookupHitKey, Set<LookupHit>> lookupHitMap = createLookupHitMap( lhItr );
         // iterate over the LookupHit objects
         for ( Map.Entry<LookupHitKey, Set<LookupHit>> entry : lookupHitMap.entrySet() ) {
            // code is only valid if the covered text is also present in the filter
            final int neBegin = entry.getKey().__start;
            final int neEnd = entry.getKey().__end;
            // Use key "cui,tui" to avoid duplicates at this offset
            final Set<String> cuiTuiSet = new HashSet<>();
            // key = type of named entity, val = set of UmlsConcept objects
            final Map<Integer,Set<UmlsConcept>> conceptMap = new HashMap<>();
            // Iterate over the LookupHit objects and group Snomed codes by NE type
            // For each NE type for which there is a hit, get all the Snomed codes
            // that map to the given CUI.
            for ( LookupHit lookupHit : entry.getValue() ) {
               final MetaDataHit mdh = lookupHit.getDictMetaDataHit();
               final String cui = mdh.getMetaFieldValue( cuiPropKey );
               final String tui = mdh.getMetaFieldValue( tuiPropKey );
               //String text = lh.getDictMetaDataHit().getMetaFieldValue("text");
               if ( !_validTuiSet.contains( tui ) ) {
                  continue;
               }
               final String cuiTuiKey = getUniqueKey( cui, tui );
               if ( cuiTuiSet.contains( cuiTuiKey ) ) {
                  continue;
               }
               cuiTuiSet.add( cuiTuiKey );
               final Set<String> snomedCodeSet = getSnomedCodes( cui );
               if ( !snomedCodeSet.isEmpty() ) {
                  final Integer neType = getNamedEntityType( tui );
                  Set<UmlsConcept> conceptSet;
                  if ( conceptMap.containsKey( neType ) ) {
                     conceptSet = conceptMap.get( neType );
                  } else {
                     conceptSet = new HashSet<>();
                     conceptMap.put( neType, conceptSet );
                  }
                  final Collection<UmlsConcept> conceptCol = createConceptCol( jcas, cui, tui, snomedCodeSet );
                  conceptSet.addAll( conceptCol );
               }
            }

            for ( Map.Entry<Integer,Set<UmlsConcept>> conceptEntry : conceptMap.entrySet() ) {
               final Set<UmlsConcept> conceptSet = conceptEntry.getValue();
               // Skip updating CAS if all Concepts for this type were filtered out for this span.
               if ( !conceptSet.isEmpty() ) {
                  final FSArray conceptArr = new FSArray( jcas, conceptSet.size() );
                  int arrIdx = 0;
                  for ( UmlsConcept umlsConcept : conceptSet ) {
                     conceptArr.set( arrIdx, umlsConcept );
                     arrIdx++;
                  }

                  IdentifiedAnnotation neAnnot;
                  final int conceptKey = conceptEntry.getKey();
                  if ( conceptKey == CONST.NE_TYPE_ID_DRUG ) {
                     neAnnot = new MedicationMention( jcas );
                  } else if ( conceptKey == CONST.NE_TYPE_ID_ANATOMICAL_SITE ) {
                      neAnnot = new AnatomicalSiteMention( jcas );
                  } else if ( conceptKey == CONST.NE_TYPE_ID_DISORDER ) {
                      neAnnot = new DiseaseDisorderMention( jcas );
                  } else if ( conceptKey == CONST.NE_TYPE_ID_FINDING ) {
                      neAnnot = new SignSymptomMention( jcas );
                  } else if ( conceptKey == CONST.NE_TYPE_ID_LAB ) {
                      neAnnot = new LabMention( jcas );
                  } else if ( conceptKey == CONST.NE_TYPE_ID_PROCEDURE ) {
                      neAnnot = new ProcedureMention( jcas );
                  } else {
                      neAnnot = new EntityMention( jcas );
                  }
                  neAnnot.setTypeID( conceptKey );
                  neAnnot.setBegin( neBegin );
                  neAnnot.setEnd( neEnd );
                  neAnnot.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
                  neAnnot.setOntologyConceptArr( conceptArr );
                  neAnnot.addToIndexes();
               }
            }
         }
      } catch ( Exception e ) {
         throw new AnalysisEngineProcessException( e );
      }
   }


   private int getNamedEntityType( final String tui ) throws IllegalArgumentException {
      if ( _medicationSet.contains( tui ) ) {
         return CONST.NE_TYPE_ID_DRUG;
      } else if ( _disorderTuiSet.contains( tui ) ) {
         return CONST.NE_TYPE_ID_DISORDER;
      } else if ( _findingTuiSet.contains( tui ) ) {
         return CONST.NE_TYPE_ID_FINDING;
      } else if ( _antSiteTuiSet.contains( tui ) ) {
         return CONST.NE_TYPE_ID_ANATOMICAL_SITE;
      } else if ( _procedureTuiSet.contains( tui ) ) {
         return CONST.NE_TYPE_ID_PROCEDURE;
      } else {
         throw new IllegalArgumentException( "TUI is not part of valid named entity types: " + tui );
      }
   }

   /**
    * For each SNOMED code, create a corresponding JCas UmlsConcept object and
    * store in a Collection.
    *
    * @param jcas -
    * @param snomedCodesCol -
    * @return -
    */
   private Collection<UmlsConcept> createConceptCol( final JCas jcas, final String cui, final String tui,
                                        final Collection<String> snomedCodesCol ) {
      final String codingSchemeKey = props.getProperty( CODING_SCHEME_PRP_KEY );
      final List<UmlsConcept> conceptList = new ArrayList<>();
      for ( String snomedCode : snomedCodesCol ) {
         final UmlsConcept uc = new UmlsConcept( jcas );
         uc.setCode( snomedCode );
         uc.setCodingScheme( codingSchemeKey );
         uc.setCui( cui );
         uc.setTui( tui );
         conceptList.add( uc );
      }
      return conceptList;
   }

   private static String getUniqueKey( final String cui, final String tui ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( cui );
      sb.append( ':' );
      sb.append( tui );
      return sb.toString();
   }

   /**
    * Load a comma delimited list
    *
    * @param delimitedString -
    * @return -
    */
   private static Set<String> loadList( final String delimitedString ) {
      if ( delimitedString == null || delimitedString.isEmpty() ) {
         return Collections.emptySet();
      }
      final String[] stringArray = delimitedString.split( "," );
      final Set<String> stringSet = new HashSet<>();
      for ( String text : stringArray ) {
         final String trimText = text.trim();
         if ( !trimText.isEmpty() ) {
            stringSet.add( trimText );
         }
      }
      return stringSet;
   }
}
