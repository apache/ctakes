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
package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;



/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class DefaultTermConsumer extends AbstractTermConsumer {
   static private final Logger LOGGER = Logger.getLogger( "DefaultTermConsumer" );

   // case insensitive blacklist
   static private final String BLACKLIST_KEY = "Blacklist";
   // case sensitive blacklist
   static private final String CS_BLACKLIST_KEY = "CsBlacklist";

   final private UmlsConceptCreator _umlsConceptCreator;

   private final CollectionMap<Integer, String, Set<String>> _blacklists = new HashSetMap<>();
   private final CollectionMap<Integer, String, Set<String>> _csBlacklists = new HashSetMap<>();

   private boolean _checkBlacklist;

   public DefaultTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      this( uimaContext, properties, new DefaultUmlsConceptCreator() );
   }

   public DefaultTermConsumer( final UimaContext uimaContext, final Properties properties,
                               final UmlsConceptCreator umlsConceptCreator ) {
      super( uimaContext, properties );
      _umlsConceptCreator = umlsConceptCreator;
      String blacklistPath = EnvironmentVariable.getEnv( BLACKLIST_KEY, uimaContext );
      if ( blacklistPath == null || blacklistPath.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         blacklistPath = properties.getProperty( BLACKLIST_KEY );
      }
      if ( blacklistPath != null && !blacklistPath.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         loadBlacklist( blacklistPath, _blacklists, false );
      }
      String csBlacklistPath = EnvironmentVariable.getEnv( CS_BLACKLIST_KEY, uimaContext );
      if ( csBlacklistPath == null || csBlacklistPath.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         csBlacklistPath = properties.getProperty( CS_BLACKLIST_KEY );
      }
      if ( csBlacklistPath != null && !csBlacklistPath.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         loadBlacklist( csBlacklistPath, _csBlacklists, true );
      }
      _checkBlacklist = !_blacklists.isEmpty() || !_csBlacklists.isEmpty();
   }

   /**
    *
    * @param blacklistPath path to file containing text that should be blacklisted from the dictionary
    * @param blacklists collection of semantically grouped blacklists
    * @param caseSensitive use case sensitivity in populating the blacklists
    */
   private void loadBlacklist( final String blacklistPath,
                               final CollectionMap<Integer, String, Set<String>> blacklists,
                               final boolean caseSensitive ) {
      LOGGER.info( "Loading Term Blacklist " + blacklistPath );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( blacklistPath ) ) ) ) {
         String line;
         String[] splits;
         while ( (line = reader.readLine()) != null ) {
            line = line.trim();
            if ( line.isEmpty() || line.startsWith( "//" ) || line.startsWith( "#" ) ) {
               continue;
            }
            splits = line.split( "\\|" );
            if ( splits.length != 2 ) {
               LOGGER.warn( "Blacklist line is not correct <semanticType>|<text> format " + line );
               continue;
            }
            final String[] keys = splits[ 0 ].split( "," );
            final String text = splits[ 1 ].trim();
            for ( String key : keys ) {
               final Integer num = attemptParseInt( key.trim() );
               if ( caseSensitive ) {
                  blacklists.placeValue( num, text );
               } else {
                  blacklists.placeValue( num, text.toLowerCase() );
               }
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not load blacklist " + blacklistPath );
      }
   }

   /**
    * Since uimafit parameter values can be integers, check for an integer value
    *
    * @param value String value parsed from file
    * @return the value as an Integer, or the original String if an Integer could not be resolved
    */
   static private Integer attemptParseInt( final String value ) {
      try {
         return Integer.valueOf( value );
      } catch ( NumberFormatException nfE ) {
         return CONST.NE_TYPE_ID_UNKNOWN;
      }
   }

   /**
    *
    * @param cTakesSemantic semantic code integer
    * @param jCas ye olde ...
    * @param textSpan span of candidate text
    * @return true if the candidate text is in the blacklist for the semantic type
    */
   private boolean inBlacklist( final int cTakesSemantic, final JCas jCas, final TextSpan textSpan ) {
      if ( !_checkBlacklist ) {
         return false;
      }
      final String text = jCas.getDocumentText().substring( textSpan.getStart(), textSpan.getEnd() ).trim();
      return _csBlacklists.containsValue( cTakesSemantic, text )
            || _blacklists.containsValue( cTakesSemantic, text.toLowerCase() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeTypeIdHits( final JCas jcas, final String codingScheme, final int cTakesSemantic,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                                  final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {
      // Collection of UmlsConcept objects
      final Collection<UmlsConcept> umlsConceptList = new ArrayList<>();
      try {
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            umlsConceptList.clear();
            if ( inBlacklist( cTakesSemantic, jcas, spanCuis.getKey() ) ) {
               continue;
            }
            for ( Long cuiCode : spanCuis.getValue() ) {
               umlsConceptList.addAll(
                     createUmlsConcepts( jcas, codingScheme, cTakesSemantic, cuiCode, cuiConcepts ) );
            }
            final FSArray conceptArr = new FSArray( jcas, umlsConceptList.size() );
            int arrIdx = 0;
            for ( UmlsConcept umlsConcept : umlsConceptList ) {
               conceptArr.set( arrIdx, umlsConcept );
               arrIdx++;
            }
            final IdentifiedAnnotation annotation = createSemanticAnnotation( jcas, cTakesSemantic );
            annotation.setTypeID( cTakesSemantic );
            annotation.setBegin( spanCuis.getKey().getStart() );
            annotation.setEnd( spanCuis.getKey().getEnd() );
            annotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
            annotation.setOntologyConceptArr( conceptArr );
            annotation.addToIndexes();
         }
      } catch ( CASRuntimeException crtE ) {
         // What is really thrown?  The jcas "throwFeatMissing" is not a great help
         throw new AnalysisEngineProcessException( crtE );
      }
   }

   static private IdentifiedAnnotation createSemanticAnnotation( final JCas jcas, final int cTakesSemantic ) {
      return SemanticGroup.getGroup( cTakesSemantic )
                          .getCreator()
                          .apply( jcas );
   }

   private Collection<UmlsConcept> createUmlsConcepts( final JCas jcas,
                                                       final String codingScheme,
                                                       final int cTakesSemantic,
                                                       final Long cuiCode,
                                                       final CollectionMap<Long, Concept, ? extends Collection<Concept>> conceptMap ) {
      final Collection<Concept> concepts = conceptMap.getCollection( cuiCode );
      if ( concepts == null || concepts.isEmpty() ) {
         return Collections.singletonList( createSimpleUmlsConcept( jcas, codingScheme,
               CuiCodeUtil.getInstance().getAsCui( cuiCode ) ) );
      }
      final Collection<UmlsConcept> umlsConcepts = new HashSet<>();
      for ( Concept concept : concepts ) {
         final Collection<Integer> allSemantics = concept.getCtakesSemantics();
         if ( !allSemantics.contains( cTakesSemantic ) ) {
            continue;
         }
         boolean added = false;
         final Collection<String> tuis = concept.getCodes( Concept.TUI );
         if ( !tuis.isEmpty() ) {
            for ( String tui : tuis ) {
               // the concept could have tuis outside this cTakes semantic group
               if ( SemanticTui.getTuiFromCode( tui ).getGroupCode() == cTakesSemantic ) {
                  umlsConcepts.addAll( _umlsConceptCreator.createUmlsConcepts( jcas, codingScheme, tui, concept ) );
                  added = true;
               }
            }
         }
         if ( !added ) {
            umlsConcepts.addAll( _umlsConceptCreator.createUmlsConcepts( jcas, codingScheme, null, concept ) );
         }
      }
      return umlsConcepts;
   }


   static private UmlsConcept createSimpleUmlsConcept( final JCas jcas, final String codingScheme, final String cui ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCodingScheme( codingScheme );
      umlsConcept.setCui( cui );
      return umlsConcept;
   }

}
