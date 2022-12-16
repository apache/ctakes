///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//package org.apache.ctakes.dictionary.lookup2.consumer;
//
//import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
//import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
//import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
//import org.apache.ctakes.dictionary.lookup2.term.SpannedRareWordTerm;
//import org.apache.ctakes.dictionary.lookup2.util.SemanticUtil;
//import org.apache.ctakes.typesystem.type.constants.CONST;
//import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
//import org.apache.ctakes.typesystem.type.textsem.EntityMention;
//import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
//import org.apache.uima.UimaContext;
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.jcas.cas.FSArray;
//
//import java.sql.Connection;
//import java.sql.Driver;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import java.util.Set;
//
///**
// * Author: SPF
// * Affiliation: CHIP-NLP
// * Date: 12/16/13
// */
//public class WsdTermConsumer extends AbstractTermConsumer {
//
//   static private final String JDBC_DRIVER = "org.hsqldb.jdbcDriver";
//   static private final String DB_URL = "jdbc:hsqldb:res:resources/org/apache/ctakes/dictionary/lookup/cuiRelations/cuiRelations";
//   static private final String DB_USER = "sa";
//   static private final String DB_PASS = "";
//   static private final String DB_TABLE = "cuiRelations";
//   final private Connection _connection;
//   private PreparedStatement _metadataStatement;
//
//   public WsdTermConsumer( final UimaContext uimaContext, final Properties properties ) {
//      super( uimaContext, properties );
//      _connection = createDatabaseConnection();
//   }
//
//   protected void consumeTypeIdHits( final JCas jcas, final String codingScheme, final int typeId,
//                                              final Map<TextSpan, Collection<RareWordTerm>> lookupHitMap )
//         throws AnalysisEngineProcessException {
//      // Do nothing
//   }
//
//   static private void registerDriver() {
//      try {
//         Driver driver = (Driver)Class.forName( JDBC_DRIVER ).newInstance();
//         DriverManager.registerDriver( driver );
//      } catch ( Exception e ) {
//         // TODO At least four different exceptions are thrown here, and should be caught and handled individually
//         System.err.println( "Could not register Driver " + JDBC_DRIVER );
//         System.err.println( e.getMessage() );
//         System.exit( 1 );
//      }
//   }
//
//   static public Connection createDatabaseConnection() {
//      registerDriver();
//      Connection connection = null;
//      try {
//         connection = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS );
//      } catch ( SQLException sqlE ) {
//         // thrown by Connection.prepareStatement(..) and getTotalRowCount(..)
//         System.err.println( "Could not establish connection to " + DB_URL + " as " + DB_USER );
//         System.err.println( sqlE.getMessage() );
//         System.exit( 1 );
//      }
//      return connection;
//   }
//
//   /**
//    *
//    * @param cui text of the rare word to use for term lookup
//    * @return an sql call to use for term lookup
//    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
//    */
//   private PreparedStatement initMetaDataStatement( final String cui ) throws SQLException {
//      if ( _metadataStatement == null ) {
//         final String lookupSql = "SELECT * FROM " + DB_TABLE + " WHERE CUI = ?";
//         _metadataStatement = _connection.prepareStatement( lookupSql );
//      }
//      _metadataStatement.clearParameters();
//      _metadataStatement.setString( 1, cui );
//      return _metadataStatement;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void consumeHits( final JCas jcas, final RareWordDictionary dictionary,
//                            final Collection<SpannedRareWordTerm> dictionaryTerms )
//         throws AnalysisEngineProcessException {
//      final String codingScheme = getCodingScheme();
//      final String entityType = dictionary.getSemanticGroup();
//      // cTakes IdentifiedAnnotation only accepts an integer as a typeId.
//      final int typeId = SemanticUtil.getSemanticGroupId( entityType );
//      // iterate over the LookupHit objects
//      final Map<TextSpan, Collection<RareWordTerm>> lookupHitMap = createLookupHitMap( dictionaryTerms );
//      // Set of Cuis to avoid duplicates at this offset
//      final Set<String> cuiSet = new HashSet<String>();
//      // Collection of UmlsConcept objects
//      final Collection<UmlsConcept> conceptList = new ArrayList<UmlsConcept>();
//      try {
//         for ( Map.Entry<TextSpan, Collection<RareWordTerm>> entry : lookupHitMap.entrySet() ) {
//            cuiSet.clearCollection();
//            conceptList.clearCollection();
//            final Collection<RareWordTerm> bestTerms = getBestRareWordTerms( entry.getValue(), dictionaryTerms );
//            for ( RareWordTerm lookupHit : bestTerms ) {
//               final String cui = lookupHit.getCuiCode() ;
//               //String text = lh.getDictMetaDataHit().getMetaFieldValue("text");
//               if ( cuiSet.add( cui ) ) {
//                  final UmlsConcept concept = new UmlsConcept( jcas );
//                  concept.setCodingScheme( codingScheme );
//                  concept.setCui( cui );
//                  concept.setTui( lookupHit.getTui() );
//                  conceptList.add( concept );
//               }
//            }
//            // Skip updating CAS if all Concepts for this type were filtered out for this span.
//            if ( conceptList.isEmpty() ) {
//               continue;
//            }
//            // code is only valid if the covered text is also present in the filter
//            final int neBegin = entry.getKey().getStart();
//            final int neEnd = entry.getKey().getEnd();
//            final FSArray conceptArr = new FSArray( jcas, conceptList.size() );
//            int arrIdx = 0;
//            for ( UmlsConcept umlsConcept : conceptList ) {
//               conceptArr.set( arrIdx, umlsConcept );
//               arrIdx++;
//            }
//            final IdentifiedAnnotation identifiedAnnotation = new EntityMention( jcas );
//            identifiedAnnotation.setTypeID( typeId );
//            identifiedAnnotation.setBegin( neBegin );
//            identifiedAnnotation.setEnd( neEnd );
//            identifiedAnnotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
//            identifiedAnnotation.setOntologyConceptArr( conceptArr );
//            //            identifiedAnnotation.setConfidence( 0.1 );
//            identifiedAnnotation.addToIndexes();
//         }
//      } catch ( Exception e ) {
//         // TODO Poor form - refactor
//         throw new AnalysisEngineProcessException( e );
//      }
//   }
//
//
//   private Collection<RareWordTerm> getBestRareWordTerms( final Collection<RareWordTerm> spanTerms,
//                                                            final Collection<SpannedRareWordTerm> dictionaryTerms ) {
//      if ( spanTerms.size() <= 1 ) {
//         return spanTerms;
//      }
//      final Map<RareWordTerm, Integer> termValidityMap = new HashMap<RareWordTerm, Integer>( spanTerms.size() );
//      int highestValidity = 0;
//      for ( RareWordTerm term : spanTerms ) {
//         final int validity = getValidityByRelation( term, dictionaryTerms );
//         highestValidity = Math.max( highestValidity, validity );
//         termValidityMap.put( term, validity );
//      }
//      // Anything that is a synonym or above should be valid, or highest validity
//      highestValidity = Math.min( highestValidity, RelationType.SY.__relationStrength );
//      final Collection<RareWordTerm> bestTerms = new ArrayList<RareWordTerm>();
//      for ( Map.Entry<RareWordTerm,Integer> entry : termValidityMap.entrySet() ) {
//         if ( entry.getValue() == highestValidity ) {
//            bestTerms.add( entry.getKey() );
//         }
//      }
//      return bestTerms;
//   }
//
//
//   private int getValidityByRelation( final RareWordTerm term,
//                                             final Collection<SpannedRareWordTerm> dictionaryTerms ) {
//      final Collection<RelatedCui> relatedCuis = getRelatedCuis( term.getCuiCode() );
//      int validity = 0;
//      for ( RelatedCui relatedCui : relatedCuis ) {
//         if ( haveCui( relatedCui.__cui, dictionaryTerms ) ) {
//            validity += relatedCui.__relationType.__relationStrength;
//         }
//      }
//      return validity;
//   }
//
//   private Collection<RelatedCui> getRelatedCuis( final String cui ) {
//      final List<RelatedCui> relatedCuis = new ArrayList<RelatedCui>();
//      try {
//         initMetaDataStatement( cui );
//         final ResultSet resultSet = _metadataStatement.executeQuery();
//         while ( resultSet.next() ) {
//            final RelatedCui relatedCui = new RelatedCui( resultSet.getString( FIELD_INDEX.CUI.__index),
//                                                          resultSet.getString( FIELD_INDEX.RELATION_TYPE.__index ) );
//            relatedCuis.add( relatedCui );
//         }
//         // Though the ResultSet interface documentation states that there are automatic closures,
//         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
//         resultSet.close();
//         return relatedCuis;
//      } catch ( SQLException e ) {
////         throw new DictionaryException( e );
//      }
//      return relatedCuis;
//   }
//
//   static private boolean haveCui( final String cui, final Collection<SpannedRareWordTerm> dictionaryTerms ) {
//      for ( SpannedRareWordTerm term : dictionaryTerms ) {
//         if ( term.getRareWordTerm().getCuiCode().equals( cui ) ) {
//            return true;
//         }
//      }
//      return false;
//   }
//
//   static public enum RelationType {
//      // RL/SY : Synonym; SIB : Sibling; PAR : Parent; CHD : Child; RN,RB,RO : Narrow, Broad, Other; XR : No Relation
//      RL(9), SY(9), SIB(7), PAR(7), CHD(7), RN(8), RB(8), RO(5), XR(-5), UNKNOWN(0);
//      private final int __relationStrength;
//      private RelationType( final int relationStrength ) {
//         __relationStrength = relationStrength;
//      }
//      static private RelationType getRelationType( final String relationName ) {
//         for ( RelationType type : RelationType.values() ) {
//            if ( relationName.equals( type.name() ) ) {
//               return type;
//            }
//         }
//         return UNKNOWN;
//      }
//   }
//
//   static public class RelatedCui {
//      final private String __cui;
//      final private RelationType __relationType;
//      public RelatedCui( final String cui, final String relationName ) {
//         __cui = cui;
//         __relationType = RelationType.getRelationType( relationName );
//      }
//   }
//
//   /**
//    * Column (field) indices in the database.  Notice that these are constant and not configurable.
//    * If a configurable implementation is desired then create an extension.
//    */
//   static private enum FIELD_INDEX {
//      CUI( 1 ), RELATION_TYPE( 2 );
//      final private int __index;
//      private FIELD_INDEX( final int index ) {
//         __index = index;
//      }
//   }
//
//}
