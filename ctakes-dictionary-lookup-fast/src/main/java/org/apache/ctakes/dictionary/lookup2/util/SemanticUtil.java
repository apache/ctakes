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
package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.core.util.annotation.SemanticTui;

/**
 *
 * Utility class to aid in the handling of semantic groups, semantic types, and tuis.
 * Used most by the term consumers.
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/25/14
 * @deprecated
 */
@Deprecated
final public class SemanticUtil {

   private SemanticUtil() {
   }

   // cTakes types
//   static private final String[] DRUG = { "T109", "T110", "T114", "T115", "T116", "T118", "T119",
//                                          "T121", "T122", "T123", "T124", "T125", "T126", "T127",
//                                          "T129", "T130", "T131", "T195", "T196", "T197", "T200", "T203" };
//   static private final String[] DISO = { "T019", "T020", "T037", "T047", "T048", "T049", "T050", "T190", "T191" };
//   static private final String[] FIND = { "T033", "T034", "T040", "T041", "T042", "T043", "T044", "T045", "T046",
//                                          "T056", "T057", "T184" };
//   static private final String[] PROC = { "T059", "T060", "T061" };
//   static private final String[] ANAT = { "T021", "T022", "T023", "T024", "T025", "T026", "T029", "T030" };

   // non-cTakes types
   // cTakes ID 7.  What is Clinical Attribute?  Just the single [standard] type?
   //   static private final String[] CLNQ = { "T201" };
   // cTakes ID 8
   //   static private final String[] DEVI = { "T203", "T074", "T075" };
   // cTakes ID 9.  What is LAB?  T034 is cTakes FIND and [standard] PHEN (test result), T059 is cTakes and [standard] PROC
   //   static private final String[] LABQ = { "T034", "T059" };
   // cTakes ID 10
   //   static private final String[] PHEN = { "T034", "T038", "T068", "T069", "T067", "T070" };


//   static private final Collection<String> ANAT_TUIS = new HashSet<>( Arrays.asList( ANAT ) );
//   static private final Collection<String> DISO_TUIS = new HashSet<>( Arrays.asList( DISO ) );
//   static private final Collection<String> FIND_TUIS = new HashSet<>( Arrays.asList( FIND ) );
//   static private final Collection<String> PROC_TUIS = new HashSet<>( Arrays.asList( PROC ) );
//   static private final Collection<String> DRUG_TUIS = new HashSet<>( Arrays.asList( DRUG ) );


//   static public final String UNKNOWN_SEMANTIC_GROUP = "UNKNOWN_SEMANTIC_GROUP";
//   static public final String UNKNOWN_SEMANTIC_ZERO = "0";


//   /**
//    * cTakes IdentifiedAnnotation only accepts an integer as a typeId, which historically map to cTakes semantic groups
//    *
//    * @param entityType the text name of the semantic group or type
//    * @return the integer value of the entity type or {@code CONST.NE_TYPE_ID_UNKNOWN} if none or improperly formed
//    */
//   static public int getSemanticGroupId( final String entityType ) {
//      if ( entityType == null || entityType.isEmpty() ) {
//         return CONST.NE_TYPE_ID_UNKNOWN;
//      }
//      if ( entityType.equalsIgnoreCase( "DRUG" ) ) {
//         return CONST.NE_TYPE_ID_DRUG;
//      } else if ( entityType.equalsIgnoreCase( "DISO" ) ) {
//         return CONST.NE_TYPE_ID_DISORDER;
//      } else if ( entityType.equalsIgnoreCase( "FIND" ) ) {
//         return CONST.NE_TYPE_ID_FINDING;
//      } else if ( entityType.equalsIgnoreCase( "PROC" ) ) {
//         return CONST.NE_TYPE_ID_PROCEDURE;
//      } else if ( entityType.equalsIgnoreCase( "ANAT" ) ) {
//         return CONST.NE_TYPE_ID_ANATOMICAL_SITE;
//      }
//      try {
//         return Integer.parseInt( entityType );
//      } catch ( NumberFormatException nfe ) {
//         return CONST.NE_TYPE_ID_UNKNOWN;
//      }
//   }
//
//   /**
//    * Sometimes a
//    *
//    * @param tuis a comma-delimited collection of tuis that apply to some annotation
//    * @return all cTakes groups for the given tuis
//    */
//   static public Collection<Integer> getSemanticGroupIdFromTui( final String tuis ) {
//      final Collection<Integer> typeIds = new HashSet<>( 1 );
//      final String[] splits = LookupUtil.fastSplit( tuis, ',' );
//      for ( String tui : splits ) {
//         if ( ANAT_TUIS.contains( tui ) ) {
//            typeIds.add( CONST.NE_TYPE_ID_ANATOMICAL_SITE );
//         } else if ( DISO_TUIS.contains( tui ) ) {
//            typeIds.add( CONST.NE_TYPE_ID_DISORDER );
//         } else if ( FIND_TUIS.contains( tui ) ) {
//            typeIds.add( CONST.NE_TYPE_ID_FINDING );
//         } else if ( PROC_TUIS.contains( tui ) ) {
//            typeIds.add( CONST.NE_TYPE_ID_PROCEDURE );
//         } else if ( DRUG_TUIS.contains( tui ) ) {
//            typeIds.add( CONST.NE_TYPE_ID_DRUG );
//         } else {
//            typeIds.add( CONST.NE_TYPE_ID_UNKNOWN );
//         }
//      }
//      return typeIds;
//   }

   /**
    * Sometimes a
    *
    * @param tui a comma-delimited collection of tuis that apply to some annotation
    * @return the cTakes group for the given tui
    */
   static public Integer getTuiSemanticGroupId( final String tui ) {
      return SemanticTui.getTuiFromCode( tui ).getGroupCode();
//      if ( ANAT_TUIS.contains( tui ) ) {
//         return CONST.NE_TYPE_ID_ANATOMICAL_SITE;
//      } else if ( DISO_TUIS.contains( tui ) ) {
//         return CONST.NE_TYPE_ID_DISORDER;
//      } else if ( FIND_TUIS.contains( tui ) ) {
//         return CONST.NE_TYPE_ID_FINDING;
//      } else if ( PROC_TUIS.contains( tui ) ) {
//         return CONST.NE_TYPE_ID_PROCEDURE;
//      } else if ( DRUG_TUIS.contains( tui ) ) {
//         return CONST.NE_TYPE_ID_DRUG;
//      }
//      return CONST.NE_TYPE_ID_UNKNOWN;
   }


}
