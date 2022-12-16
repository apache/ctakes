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

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;

import static org.apache.ctakes.temporal.utils.TimeRelationConstants.AF;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.BF;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.BO;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.CB;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.CN;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.EO;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.NO;
import static org.apache.ctakes.temporal.utils.TimeRelationConstants.OV;

/**
 * Enumeration of the three temporal relation types used in THYME (before, overlap, contains)
 * and their reciprocals (after, overlap, contained_by)
 * Begins-on and Ends-on added 4-11-2013
 */
public enum TlinkType {
   BEFORE( BF ),        // A+ < B-        where + is end, - is start
   AFTER( AF ),         // A- > B+
   OVERLAP( OV ),       // A- < B- < A+  ||  B- < A- < B+
   CONTAINS( CN ),      // A- < B- && A+ > B+
   CONTAINED_BY( CB ),  // A- > B- && A+ < B+
   BEGINS_ON( BO ),     // A- = B+
   ENDS_ON( EO );       // A+ = B-
//   INITIATES( IN )
//   CONTINUES( CT )
//   REINITIATES( RE )

   // TODO refactor to use TlinkAttributeValue enum

   static public TlinkType getTlinkType( final BinaryTextRelation tlink ) {
      final String type = tlink.getCategory();
      if ( type == null ) {
         return null;
      }
      return getTimeRelationType( type );
   }

   static public TlinkType getTimeRelationType( final String name ) {
      final String upper = name.toUpperCase().replace( "_", "-" );
      if ( upper.equals( "BEFORE" ) ) {
         return BEFORE;
      } else if ( upper.equals( "AFTER" ) ) {
         return AFTER; // Special case for symmetry
      } else if ( upper.equals( "OVERLAP" ) || upper.equals( "UNDEFINED" ) || upper.isEmpty() ) {
         return OVERLAP;
      } else if ( upper.equals( "CONTAINS" ) ) {
         return CONTAINS;
      } else if ( upper.equals( "CONTAINED-BY" ) ) {
         return CONTAINED_BY; // Special case for symmetry
//      } else if ( upper.equals( "BEGINS-ON" ) ) {
      } else if ( upper.equals( "BEGINS-ON" ) || upper.equals( "CONTINUES" ) || upper.equals( "TERMINATES" ) ) {
         return BEGINS_ON;
//      } else if ( upper.equals( "ENDS-ON" ) ) {
      } else if ( upper.equals( "ENDS-ON" ) || upper.equals( "INITIATES" ) || upper.equals( "REINITIATES" ) ) {
         return ENDS_ON;
      }
      return null;
   }

   static public TlinkType getTimeRelationType( final long startA, final long startB,
                                                       final long endA, final long endB ) {
      if ( endA < startB ) {
         return BEFORE;
      } else if ( startA > endB ) {
         return AFTER;
      } else if ( endA == startB ) {
         return ENDS_ON;
      } else if ( startA == endB ) {
         return BEGINS_ON;
      }
      final long startCompare = startA - startB;
      final long endCompare = endA - endB;
      if ( startCompare < 0 && endCompare > 0 ) {
         return CONTAINS;
      } else if ( startCompare > 0 && endCompare < 0 ) {
         return CONTAINED_BY;
      } else if ( startCompare == 0 && endCompare > 0 ) {
         return BEGINS_ON;
      } else if ( startCompare < 0 && endCompare == 0 ) {
         return ENDS_ON;
      }
      return OVERLAP;
   }

   static public TlinkType getCombinedTypeConservative( final TlinkType typeA,
                                                               final TlinkType typeB ) {
      if ( typeA == typeB ) {
         return typeA;
      }
      return OVERLAP;
   }

   @SuppressWarnings("incomplete-switch")
static public TlinkType getCombinedTypeLiberal( final TlinkType typeA,
                                                          final TlinkType typeB ) {
      if ( typeA == typeB ) {
         return typeA;
      }
      if ( typeA == typeB.getReciprocal() ) {
         return OVERLAP;
      }
      switch ( typeA ) {
         case BEFORE: {
            switch ( typeB ) {
               case CONTAINS: return ENDS_ON;
               case CONTAINED_BY: return ENDS_ON;
               case BEGINS_ON: return CONTAINS;
               case ENDS_ON: return ENDS_ON;
               case OVERLAP: return ENDS_ON;
            }
         }
         case AFTER: {
            switch ( typeB ) {
               case CONTAINS: return BEGINS_ON;
               case CONTAINED_BY: return BEGINS_ON;
               case BEGINS_ON: return BEGINS_ON;
               case ENDS_ON: return CONTAINS;
               case OVERLAP: return BEGINS_ON;
            }
         }
         case CONTAINS: {
            switch ( typeB ) {
               case BEGINS_ON: return BEGINS_ON;
               case ENDS_ON: return ENDS_ON;
               case OVERLAP: return CONTAINS;
            }
         }
         case CONTAINED_BY: {
            switch ( typeB ) {
               case BEGINS_ON: return BEGINS_ON;
               case ENDS_ON: return ENDS_ON;
               case OVERLAP: return CONTAINED_BY;
            }
         }
         case OVERLAP: {
            switch ( typeB ) {
               case BEGINS_ON: return BEGINS_ON;
               case ENDS_ON: return ENDS_ON;
            }
         }
      }
      return getCombinedTypeLiberal( typeB, typeA );
   }


   static private TlinkType getTimeRelationType( final int index ) {
      switch ( index ) {
         case BF : return BEFORE;
         case AF : return AFTER;
         case CN : return CONTAINS;
         case CB : return CONTAINED_BY;
         case BO : return BEGINS_ON;
         case EO : return ENDS_ON;
         case OV : return OVERLAP;
      }
      return null;
   }

   // Given relation A to B and relation B to C, return relation A to C
   // This is a very conservative interpretation.
   // There are several OV possibilities that are left out because CN is an equal possibility
//   static private final int[][] TML_ABC_ARRAY =
//         //BF, AF, CN, CB, BO, EO, OV                   A to B
//         {{BF, NO, NO, BF, NO, BF, NO},  // BF
//          {NO, AF, NO, AF, AF, NO, NO},  // AF
//          {BF, AF, CN, NO, AF, BF, NO},  // CN
//          {NO, NO, OV, CB, NO, NO, NO},  // CB           B to C
//          {NO, AF, NO, AF, AF, OV, NO},  // BO
//          {BF, NO, NO, BF, OV, BF, NO},  // EO
//          {NO, NO, NO, NO, NO, NO, NO}}; // OV     then A to C

//   static private final int[][] TML_ABC_ARRAY =
//         //BF, AF, CN, CB, BO, EO, OV                   A to B
//         {{BF, NO, NO, BF, NO, BF, NO},  // BF
//          {NO, AF, NO, AF, AF, NO, NO},  // AF
//          {BF, AF, CN, NO, AF, BF, NO},  // CN
//          {NO, NO, OV, CB, OV, OV, OV},  // CB           B to C
//          {NO, AF, OV, AF, AF, OV, NO},  // BO
//          {BF, AF, OV, BF, OV, BF, NO},  // EO
//          {NO, NO, OV, NO, NO, NO, NO}}; // OV     then A to C

   static private final int[][] TML_ABC_ARRAY =
         //BF, AF, CN, CB, BO, EO, OV                   A to B
         {{BF, NO, NO, BF, NO, BF, NO},  // BF
          {NO, AF, NO, AF, AF, NO, NO},  // AF
          {BF, AF, CN, NO, AF, BF, NO},  // CN
          {NO, NO, OV, CB, OV, OV, OV},  // CB           B to C
          {NO, AF, NO, AF, AF, OV, NO},  // BO
          {BF, AF, NO, BF, OV, BF, NO},  // EO
          {NO, NO, OV, NO, NO, NO, NO}}; // OV     then A to C

   final private int _index;

   private TlinkType( final int index ) {
      _index = index;
   }

   private int getIndex() {
      return _index;
   }

   /**
    * @param tlinkTypeBtoC a relation with a start argument coincidental with this relation
    * @return for this relation A to B and the given relation B to C, return relation A to C
    */
   public TlinkType getTimeRelationTypeAtoC( final TlinkType tlinkTypeBtoC ) {
      // The array elements are fetched [row][column]
      // Checked and works 7/10/13 spf
      final int relationIndex = TML_ABC_ARRAY[tlinkTypeBtoC.getIndex()][getIndex()];
//      System.out.println( "A " + toString() + " B and B " + relationTypeBtoC + " C so A " + getTlinkType( relationIndex ) + " C");
      return getTimeRelationType( relationIndex );
   }

   /**
    * @return the reciprocal Temporal Relation type of (this) relation type
    */
   public TlinkType getReciprocal() {
      switch ( this ) {
         case BEFORE:
            return AFTER;
         case AFTER:
            return BEFORE;
         case OVERLAP:
            return OVERLAP;
         case CONTAINS:
            return CONTAINED_BY;
         case CONTAINED_BY:
            return CONTAINS;
         case BEGINS_ON:
            return ENDS_ON;
         case ENDS_ON:
            return BEGINS_ON;
      }
      return null;
   }

//   public Attribute getAsAttribute() {
//      final String attributeValue  = getAttributeValue( this );
//      return new DefaultAttribute( DefinedAttributeType.RELATION_TYPE, attributeValue );
//   }

//   static private String getAttributeValue( final TlinkType tlinkType ) {
//      switch ( tlinkType ) {
//         case BEFORE: return "BEFORE";
//         case AFTER : return "AFTER";
//         case OVERLAP : return "OVERLAP";
//         case CONTAINS: return "CONTAINS";
//         case CONTAINED_BY: return "CONTAINED-BY";
//         case BEGINS_ON: return "BEGINS-ON";
//         case ENDS_ON: return "ENDS-ON";
//      }
//      return null;
//   }




}
