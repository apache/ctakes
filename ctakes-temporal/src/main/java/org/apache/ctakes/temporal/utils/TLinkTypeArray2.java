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
package org.apache.ctakes.temporal.utils;

import org.apache.ctakes.temporal.utils.AnnotationIdCollection;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;


import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.ctakes.temporal.utils.TlinkType.AFTER;
import static org.apache.ctakes.temporal.utils.TlinkType.BEFORE;
import static org.apache.ctakes.temporal.utils.TlinkType.BEGINS_ON;
import static org.apache.ctakes.temporal.utils.TlinkType.CONTAINS;
import static org.apache.ctakes.temporal.utils.TlinkType.ENDS_ON;
import static org.apache.ctakes.temporal.utils.TlinkType.OVERLAP;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 6/18/13
 */
public class TLinkTypeArray2 {

   // given relation A to B and relation B to C, return relation A to C

   /**
    * @param tlinkTypesAtoB for a relation with an end argument coincidental with (relationType23's relation), the store
    * @param tlinkTypesBtoC for a relation with a start argument coincidental with (relationType12's relation), the store
    * @return for a relation with the start argument of (relationTypeAtoB's relations) and the end argument of
    *         (relationTypeBtoC's relations), the set of time relations if one can be derived from the first two other sets, else null
    */
   static private TlinkTypeSet getTlinkTypesAtoC( final TlinkTypeSet tlinkTypesAtoB,
                                                  final TlinkTypeSet tlinkTypesBtoC ) {
      if ( tlinkTypesAtoB == null || tlinkTypesBtoC == null
            || tlinkTypesAtoB.isEmpty() || tlinkTypesBtoC.isEmpty() ) {
         return null;
      }
      final TlinkTypeSet tlinkTypeSetAtoC = new TlinkTypeSet();
      for ( TlinkType tlinkTypeAtoB : tlinkTypesAtoB ) {
         for ( TlinkType tlinkTypeBtoC : tlinkTypesBtoC ) {
            final TlinkType tertiary = tlinkTypeAtoB.getTimeRelationTypeAtoC( tlinkTypeBtoC );
            if ( tertiary != null ) {
               tlinkTypeSetAtoC.add( tertiary );
            }
         }
      }
      return tlinkTypeSetAtoC;
   }



   //  Array of relation types for each event/time to other event/time
   private final TlinkTypeSet[][] _tlinkTypesArray;
   //  map of entity ids in relations and their corresponding column/row indices in _tlinkTypesArray
   // Key = entity id, Value = column/row index
//   private final Map<Integer, Integer> _entityIdIndices;

   private final AnnotationIdCollection _entityIdCollection;
   private final List<Integer> _entityIdList;

//   public TLinkTypeArray2( final AnnotationCollection annotationCollection ) {
//      this( annotationCollection.getTimeRelations(), annotationCollection.getReferenceCollection() );
//   }

   public TLinkTypeArray2( final List<BinaryTextRelation> tlinkList, final AnnotationIdCollection entityIdCollection ) {
      final Set<Integer> tlinkAnnotationIds = new HashSet<Integer>();
      for ( BinaryTextRelation tlink : tlinkList ) {
         // Add all entities, even for relations that have no valid relation type
         tlinkAnnotationIds.add( entityIdCollection.getAnnotationId( tlink.getArg1().getArgument() ) );
         tlinkAnnotationIds.add( entityIdCollection.getAnnotationId( tlink.getArg2().getArgument() ) );
      }
      _entityIdList = new ArrayList<Integer>( tlinkAnnotationIds );
      Collections.sort( _entityIdList ); // just so that -reported- closure counts are consistent
      final int entityCount = _entityIdList.size();
      _tlinkTypesArray = new TlinkTypeSet[entityCount][entityCount];
      _entityIdCollection = entityIdCollection;
      populateTlinkTypesArray( tlinkList );
   }

   public TlinkTypeSet getTlinkTypes( final BinaryTextRelation tlink ) {
      final Annotation entityA = tlink.getArg1().getArgument();
      final Annotation entityB = tlink.getArg2().getArgument();
      final int entityIdA = _entityIdCollection.getAnnotationId( entityA );
      final int entityIdB = _entityIdCollection.getAnnotationId( entityB );
      final int indexA = _entityIdList.indexOf( entityIdA );
      final int indexB = _entityIdList.indexOf( entityIdB );
      return getTlinkTypes( indexA, indexB );
   }

   public List<BinaryTextRelation> getClosedTlinks(JCas jCas) {
      final int length = getAnnotationIdCount();
      if ( length == 0 ) {
         return Collections.emptyList();
      }
      final List<BinaryTextRelation> closedTlinks = new ArrayList<BinaryTextRelation>();
      for ( int row = 0; row<length; row++ ) {
         for ( int column = 0; column<length; column++ ) {
            if ( row == column ) {
               continue;
            }
            final TlinkTypeSet tlinkTypeSet = _tlinkTypesArray[row][column];
            if (  tlinkTypeSet == null || tlinkTypeSet.isEmpty() ) {
               continue;
            }
            for ( TlinkType tlinkType : tlinkTypeSet ) {
               final BinaryTextRelation tlink = createTlink(jCas, row, column, tlinkType );
               if ( tlink != null ) {
//                  System.out.println(row+","+column+" "+tlink.getFirstAnnotation().getSpannedText()+ " " + tlinkType + " " + tlink.getSecondAnnotation().getSpannedText());
                  closedTlinks.add( tlink );
               }
            }
         }
      }
      return closedTlinks;
   }

   private BinaryTextRelation createTlink( final JCas jCas, int row, final int column, final TlinkType tlinkType ) {
	   //      final ClassType tlinkClass = new CustomClassType( "TLINK" );
	   final Annotation entityA = getAnnotation( row );
	   final Annotation entityB = getAnnotation( column );
	   if ( entityA == null || entityB == null ) {
		   return null;
	   }
	   //      final Attribute tlinkAttribute = tlinkType.getAsAttribute();
	   ////      if ( entityB instanceof Timex ) {
	   ////         System.out.println( "TRTA2 " + entityA.getSpannedText() + " " + tlinkType.getValue() + " " + entityB.getSpannedText() + " " + timeRelationType );
	   ////      }
	   //      return new DefaultRelation( entityA, entityB, tlinkClass, "Closure", tlinkAttribute );
	   RelationArgument arg1 = new RelationArgument(jCas);
	   arg1.setArgument(entityA);
	   RelationArgument arg2 = new RelationArgument(jCas);
	   arg2.setArgument(entityB);
	   TemporalTextRelation relation = new TemporalTextRelation(jCas);
	   relation.setArg1(arg1);
	   relation.setArg2(arg2);
	   relation.setCategory(tlinkType.name().replace( "_", "-" ));//check if this is correct
	   return relation;
   }

   private int getAnnotationId( final int index ) {
      return _entityIdList.get( index );
   }

   private Annotation getAnnotation( final int index ) {
      final int entityId = getAnnotationId( index );
      return _entityIdCollection.getAnnotation( entityId );
   }

   //   --------------------  Population of array  --------------------   //

   private void populateTlinkTypesArray( final List<BinaryTextRelation> tlinkList ) {
      // initialize array with existing known time relation types
      int explicitCount = 0;
      for ( BinaryTextRelation tlink : tlinkList ) {
         if ( addTlinkType( tlink ) ) {
            explicitCount += 2;
         }
      }
      // add to array inferred time relation types
      final int entityIdCount = getAnnotationIdCount();
      // column
      int inferredInIteration = -1; // seed
      int iterations = 0;
      int totalInferred = 0;
      while (inferredInIteration != 0 ) {
         inferredInIteration = 0;
         for ( int index = 0; index < entityIdCount; index++ ) {
            inferredInIteration += inferForIndex( index );
         }
         totalInferred += inferredInIteration;
         iterations++;
      }
      // TODO What follows is just info
      int tlinkCount = 0;
      for ( int i=0; i<entityIdCount; i++ ) {
          for ( int j=0; j<entityIdCount; j++ ) {
             if ( _tlinkTypesArray[i][j] != null ) {
                tlinkCount += _tlinkTypesArray[i][j].size();
//                System.out.println( i+","+j+" " + getAnnotation( i ).getSpannedText() + " " +_tlinkTypesArray[i][j] + " " + getAnnotation( j ).getSpannedText());
             }
          }
      }
      System.out.println( "===================================================================================");
      System.out.println( "   Marked Entities: " + entityIdCount
                          + ", Marked TLinks: " + tlinkList.size()
                          + ", Proper TLinks: " + explicitCount
                          + ", Inferred TLinks: " + totalInferred
                          + ", Total TLinks: " + tlinkCount
                          + ", Iterations: " + iterations );
      System.out.println( "===================================================================================");
   }


   private int inferForIndex( final int indexA ) {
      final int entityIdCount = getAnnotationIdCount();
      int inferredCount = 0;
      for ( int indexB = 0; indexB < entityIdCount; indexB++ ) {
         if ( indexA == indexB ) {
            continue;
         }
         final TlinkTypeSet tlinkTypesAtoB = getTlinkTypes( indexA, indexB );
         if ( tlinkTypesAtoB == null ) {
            // No time relation types for cell, can't infer something from nothing, move on
            continue;
         }
         for ( int indexC = 0; indexC < entityIdCount; indexC++ ) {
            if ( indexC == indexA || indexC == indexB ) {
               continue;
            }
            final TlinkTypeSet tlinkTypesBtoC = getTlinkTypes( indexB, indexC );
            if ( tlinkTypesBtoC == null ) {
               // No time relation types for cell, can't infer something from nothing, move on
               continue;
            }
            inferredCount += inferTlinkTypesForIndexAtoBtoC( indexA, indexB, indexC,
                                                             tlinkTypesAtoB, tlinkTypesBtoC );
            if ( !hasTlinkType( indexA, indexB, OVERLAP ) ) {
               // search for overlap between two nodes marked overlap
               inferredCount += inferTlinkTypesForAnnotationAtoBwithCandD( indexA, indexB, indexC, OVERLAP );
            }
            if ( !hasTlinkType( indexA, indexB, CONTAINS ) ) {
               // search for overlap between two nodes marked contains
               inferredCount += inferTlinkTypesForAnnotationAtoBwithCandD( indexA, indexB, indexC, CONTAINS );
            }
         }
      }
      return inferredCount;
   }

   private int inferTlinkTypesForIndexAtoBtoC( final int indexA, @SuppressWarnings("unused") final int indexB, final int indexC,
                                               final TlinkTypeSet tlinkTypesAtoB,
                                               final TlinkTypeSet tlinkTypesBtoC ) {
      int addedCount = 0;
      final TlinkTypeSet tlinkTypesAtoC = getTlinkTypesAtoC( tlinkTypesAtoB,
                                                                    tlinkTypesBtoC );
      //         // DEBUG
//               final int lastCount = addedCount;
      //         // END DEBUG
      addedCount += addTlinkTypes( indexA, indexC, tlinkTypesAtoC );
      //         // DEBUG
//               if ( addedCount > lastCount ) {
//                  System.out.println( indexA + " " + timeRelationTypesAtoB + " " + indexB + ", "
//                                            + indexB  + " " + timeRelationTypesBtoC + " " + indexC + " "
//                                            + " ~ " + indexA + " " + timeRelationTypesAtoC + " " + indexC
//                                            + " & " + indexC + " " + timeRelationTypesAtoC.createReciprocals() + " " + indexA );
//               }
      //         // END DEBUG
      return addedCount;
   }

   // search overlap between two nodes marked overlap   or  contains between two nodes marked contains
   private int inferTlinkTypesForAnnotationAtoBwithCandD( final int indexA, final int indexB, final int indexC,
                                                      final TlinkType tlinkType ) {
      final int entityCount = getAnnotationIdCount();
      int addedCount = 0;
      // if A ov C && A ov D, C < B || C eo B, B < D || D bo B, then A ov B   ( A ov C < B < D ov A )
      // if A cn C && A cn D, (C < B || C eo B), (B < D || D bo B), then A cn B   ( A cn C < B < D cn A )
      if ( hasTlinkType( indexA, indexC, tlinkType ) ) {
         // A ov C   or   A cn C
         for ( int indexD = 0; indexD < entityCount; indexD++ ) {
            if ( indexD == indexA || indexD == indexB || indexD == indexC ) {
               continue;
            }
            if ( hasTlinkType( indexA, indexD, tlinkType ) ) {
               // A ov D   or   A cn D
               // TODO This needs to be changed so two before/after make a contained and begins/ends-on makes overlap
               if ( (hasTlinkType( indexB, indexC, AFTER )
                     || hasTlinkType( indexB, indexC, BEGINS_ON ))
                     && (hasTlinkType( indexB, indexD, BEFORE )
                     || hasTlinkType( indexB, indexD, ENDS_ON )) ) {
                  // A ov C < B < D ov A, therefore A ov B   or   A cn C < B < D cn A, therefore A cn B
                  if ( addTlinkType( indexA, indexB, tlinkType ) ) {
                     addedCount++;
                  }
               }
            }
         }
      }
      return addedCount;
   }


   private boolean isIndexOk( final int entityIndex ) {
      return entityIndex >= 0 && entityIndex < getAnnotationIdCount();
   }

   private int getAnnotationIdCount() {
      return _entityIdList.size();
   }

   private boolean hasTlinkType( final int indexA, final int indexB,
                                 final TlinkType tlinkType ) {
      if ( indexA == indexB ) {
         // entities are the same, therefore no relationship
         return false;
      }
      final TlinkTypeSet tlinkTypes = getTlinkTypes( indexA, indexB );
      return tlinkTypes != null && tlinkTypes.contains( tlinkType );
   }

   private TlinkTypeSet getTlinkTypes( final int indexA, final int indexB ) {
      if ( indexA == indexB || !isIndexOk( indexA ) || !isIndexOk( indexB ) ) {
         return null;
      }
      return _tlinkTypesArray[indexA][indexB];
   }

   private int addTlinkTypes( final int indexA, final int indexC,
                              final TlinkTypeSet tlinkTypes ) {
      if ( tlinkTypes == null || tlinkTypes.isEmpty()
            || !isIndexOk( indexA ) || !isIndexOk( indexC ) ) {
         return 0;
      }
      if ( _tlinkTypesArray[indexA][indexC] == null ) {
         _tlinkTypesArray[indexA][indexC] = tlinkTypes;
         _tlinkTypesArray[indexC][indexA] = tlinkTypes.createReciprocals();
         return 2;
      }
      int addedCount = 0;
      if ( _tlinkTypesArray[indexC][indexA] == null ) {
         _tlinkTypesArray[indexC][indexA] =  new TlinkTypeSet();
      }
      for ( TlinkType tlinkType : tlinkTypes ) {
//         if  ( getAnnotation(indexC) instanceof Timex && getAnnotation(indexA) instanceof Timex ) {
//            System.out.println( getAnnotation(indexA).getSpannedText() + " " + relationType  + " " + getAnnotation(indexC).getSpannedText() );
//         }
         boolean added = _tlinkTypesArray[indexA][indexC].add( tlinkType );
         if ( added ) {
            addedCount++;
         }
         added = _tlinkTypesArray[indexC][indexA].add( tlinkType.getReciprocal() );
         if ( added ) {
            addedCount++;
         }
      }
      return addedCount;
   }





   ///////////       Add TlinkType       /////////////

   private boolean addTlinkType( final BinaryTextRelation tlink ) {
      // Checked and ok 7/10/13 spf
      final TlinkType tlinkType = TlinkType.getTlinkType( tlink );
      if ( tlinkType == null ) {
         return false;
      }
      final boolean added1 = addTlinkType( tlink.getArg1().getArgument(), tlink.getArg2().getArgument(),
                                           tlinkType );
      // Add reciprocal
      final boolean added2 = addTlinkType( tlink.getArg2().getArgument(), tlink.getArg1().getArgument(),
                                           tlinkType.getReciprocal() );
      return added1 || added2;
   }

   private boolean addTlinkType( final Annotation entityA, final Annotation entityB,
                                 final TlinkType tlinkType ) {
      // Checked and ok 7/12/13 spf
      final int entityIdA = _entityIdCollection.getAnnotationId( entityA );
      final int entityIdB = _entityIdCollection.getAnnotationId( entityB );
      return addTlinkType( entityIdA, entityIdB, tlinkType );
   }

   private boolean addTlinkType( final int entityIdA, final int entityIdB,
                                 final TlinkType tlinkType ) {
      // Checked and ok 7/12/13 spf
      final int indexA = _entityIdList.indexOf( entityIdA );
      final int indexB = _entityIdList.indexOf( entityIdB );
      if ( tlinkType == null || !isIndexOk( indexA ) || !isIndexOk( indexB ) ) {
         return false;
      }
      if ( _tlinkTypesArray[indexA][indexB] == null ) {
         _tlinkTypesArray[indexA][indexB] = new TlinkTypeSet();
      }
      return _tlinkTypesArray[indexA][indexB].add( tlinkType );
   }




//
//      // for testing
//   static private final String XML_UMLS_FILE_PATH
//         = "C:\\Spiffy\\Data\\THYME2\\Gold\\UMLS\\set08\\ID008_clinic_024.knowtator.xml";
//   static private final String XML_THYME_FILE_PATH
//         = "C:\\Spiffy\\Data\\THYME2\\Gold\\THYME\\Set08\\ID008_clinic_024.knowtator.xml";
//   static private final String XML_CHAIN_FILE_PATH
//         = "C:\\Spiffy\\Data\\THYME2\\Gold\\THYME_Chains\\Set08\\ID008_clinic_024.chains";
//
//
//   static private AnnotationCollection getAnnotationCollection( final String filePath ) {
//      final KnowtatorXmlParser xmlParser = new KnowtatorXmlParser();
//      xmlParser.parseFile( filePath );
//      return xmlParser.getAnnotationCollection();
//   }
//
//   static private java.util.List<CoreferenceChain> getCoreferenceCollection( final String filePath,
//                                                                             final Collection<Event> eventCollection,
//                                                                             final Collection<Annotation> entityCollection ) {
//      final java.util.List<Set<TextSpan>> textSpanChains = new ArrayList<Set<TextSpan>>();
//      try {
//         final BufferedReader reader = new BufferedReader( new FileReader( filePath ) );
//         String line = reader.readLine();
//         while ( line != null ) {
//            final Set<TextSpan> textSpans = new HashSet<TextSpan>();
//            final String[] indicesArray = line.split( "\\s+" );
//            for ( String textSpanIndices : indicesArray ) {
//               final String[] indices = textSpanIndices.split( "-" );
//               final int start = Integer.parseInt( indices[0] );
//               final int end = Integer.parseInt( indices[1] );
//               textSpans.add( new DefaultTextSpan( start, end ) );
//            }
//            textSpanChains.add( textSpans );
//            line = reader.readLine();
//         }
//      } catch ( IOException ioE ) {
//         System.err.println( "Couldn't read coreference chain file " + filePath );
//         System.err.println( ioE.getMessage() );
//         return Collections.emptyList();
//      }
//
//      final Set<CoreferenceChain> chains = new HashSet<CoreferenceChain>();
//      for ( Set<TextSpan> textSpanChain : textSpanChains ) {
//         final HashSet<Annotation> entitySet = new HashSet<Annotation>(0);
//         for ( TextSpan elementSpan : textSpanChain ) {
//            for ( Event event : eventCollection ) {
//               final TextSpan overlapSpan = elementSpan.getIntersectionSpan( event.getTextSpan() );
//               if ( overlapSpan != null && overlapSpan.getLength() > 0 ) {
//                  entitySet.add( event );
//               }
//            }
//            for ( Annotation entity : entityCollection ) {
//               final TextSpan overlapSpan = elementSpan.getIntersectionSpan( entity.getTextSpan() );
//               if ( overlapSpan != null && overlapSpan.getLength() > 0 ) {
//                  entitySet.add( entity );
//               }
//            }
//         }
//         if ( entitySet.size() > 1 ) {
//            chains.add( new DefaultAnnotationChain( "TLinkTypeArray2", entitySet.toArray( new Annotation[entitySet.size()] ) ) );
//         }
//      }
//
//      return new ArrayList<CoreferenceChain>( chains );
//   }
//
//
//   public static void main( String[] args ) {
//      final AnnotationCollection thymeAnnotationCollection = getAnnotationCollection( XML_THYME_FILE_PATH );
//      final AnnotationCollection umlsAnnotationCollection = getAnnotationCollection( XML_UMLS_FILE_PATH );
//      final java.util.List<CoreferenceChain> coreferenceChains = getCoreferenceCollection( XML_CHAIN_FILE_PATH,
//                                                                                           thymeAnnotationCollection.getEvents(),
//                                                                                           umlsAnnotationCollection.getNamedEntities() );
//      final AnnotationCollection coreferenceCollection
//            = new ImmutableAnnotationCollection.AnnoteCollectBuilder().coreferenceChains( coreferenceChains ).build();
//
//      final java.util.List<AnnotationCollection> annotationCollectionList = new ArrayList<AnnotationCollection>( 2 );
//      annotationCollectionList.add( thymeAnnotationCollection );
//      annotationCollectionList.add( umlsAnnotationCollection );
//      annotationCollectionList.add( coreferenceCollection );
//      final AnnotationCollection annotationCollection
//            = new ImmutableAnnotationCollection.AnnoteCollectMerger().all( annotationCollectionList ).build();
//
//      new TLinkTypeArray2( annotationCollection );
//   }


}
