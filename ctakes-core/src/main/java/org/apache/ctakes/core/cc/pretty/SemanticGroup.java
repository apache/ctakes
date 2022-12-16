package org.apache.ctakes.core.cc.pretty;

import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;

import java.util.*;
import java.util.stream.Collectors;

/**
 * enumeration of ctakes semantic types:
 * anatomical site, disease/disorder, finding (sign/symptom), test/procedure, and medication
 */
public enum SemanticGroup {
   /////////  TODO
   ////////////////////////////  Similar is in org.apache.ctakes.dictionary.lookup2.util.SemanticUtil
   ////////////////////////////  and should be moved to core if this new class is taken up
   // cTakes types
   ANATOMICAL_SITE( "Anatomy", "ANT", "T021", "T022", "T023", "T024", "T025", "T026", "T029", "T030" ),
   DISORDER( "Disorder", "DIS", "T019", "T020", "T037", "T047", "T048", "T049", "T050", "T190", "T191" ),
   FINDING( "Finding", "FND", "T033", "T034", "T040", "T041", "T042", "T043", "T044", "T045", "T046",
         "T056", "T057", "T184" ),
   PROCEDURE( "Procedure", "PRC", "T059", "T060", "T061" ),
   MEDICATION( "Drug", "DRG", "T109", "T110", "T114", "T115", "T116", "T118", "T119",
         "T121", "T122", "T123", "T124", "T125", "T126", "T127",
         "T129", "T130", "T131", "T195", "T196", "T197", "T200", "T203" );

   static public final String UNKNOWN_SEMANTIC = "Unknown";
   static public final String UNKNOWN_SEMANTIC_CODE = "UNK";
   static public final String EVENT_SEMANTIC = "Event";
   static public final String EVENT_CODE = "EVT";
   static public final String TIMEX_SEMANTIC = "Time";
   static public final String TIMEX_CODE = "TMX";
   static public final String ENTITY_SEMANTIC = "Entity";
   static public final String ENTITY_CODE = "ENT";
   final private String _name;
   final private String _code;
   final private Collection<String> _tuis;

   /**
    * ctakes semantic type defined by tuis
    *
    * @param name short name of the type: anatomy, disorder, finding, procedure, drug
    * @param code short code of the type: ANT, DIS, FND, PRC, DRG
    * @param tuis tuis that define the semantic type
    */
   SemanticGroup( final String name, final String code, final String... tuis ) {
      _name = name;
      _code = code;
      _tuis = Arrays.asList( tuis );
   }

   /**
    * @return name of this semantic type
    */
   public String getName() {
      return _name;
   }

   /**
    * @return code of this semantic type
    */
   public String getCode() {
      return _code;
   }

   /**
    * @param annotation -
    * @return all applicable semantic names for the annotation
    */
   static public Collection<String> getSemanticNames( final IdentifiedAnnotation annotation ) {
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      if ( umlsConcepts == null || umlsConcepts.isEmpty() ) {
         if ( annotation instanceof EventMention ) {
            return Collections.singletonList( EVENT_SEMANTIC );
         } else if ( annotation instanceof TimeMention ) {
            return Collections.singletonList( TIMEX_SEMANTIC );
         } else if ( annotation instanceof EntityMention ) {
            return Collections.singletonList( ENTITY_SEMANTIC );
         }
         return Collections.emptyList();
      }
      final Collection<String> semanticNames = new HashSet<>();
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         semanticNames.add( getSemanticName( annotation, umlsConcept ) );
      }
      final List<String> semanticList = new ArrayList<>( semanticNames );
      Collections.sort( semanticList );
      return semanticList;
   }

   /**
    * @param annotation -
    * @param concept    -
    * @return semantic name
    */
   static public String getSemanticName( final IdentifiedAnnotation annotation, final UmlsConcept concept ) {
      final String tui = concept.getTui();
      final String semanticName = SemanticGroup.getSemanticName( tui );
      if ( semanticName != null && !semanticName.equals( UNKNOWN_SEMANTIC ) ) {
         return semanticName;
      }
      if ( annotation instanceof EventMention ) {
         return EVENT_SEMANTIC;
      } else if ( annotation instanceof TimeMention ) {
         return TIMEX_SEMANTIC;
      } else if ( annotation instanceof EntityMention ) {
         return ENTITY_SEMANTIC;
      }
      return getSimpleName( annotation );
   }

   /**
    * @param tui a tui of interest
    * @return the name of a Semantic type associated with the tui
    */
   static public String getSemanticName( final String tui ) {
      if ( tui == null || tui.isEmpty() ) {
         return UNKNOWN_SEMANTIC;
      }
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         if ( semanticGroup._tuis.contains( tui ) ) {
            return semanticGroup._name;
         }
      }
      return UNKNOWN_SEMANTIC;
   }

   /**
    * @param annotations -
    * @return all semantic codes for the annotations
    */
   static public Collection<String> getSemanticCodes( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<String> umlsCodes = annotations.stream()
            .map( OntologyConceptUtil::getUmlsConcepts )
            .flatMap( Collection::stream )
            .map( SemanticGroup::getSemanticCode )
            .distinct()
            .sorted()
            .collect( Collectors.toList() );
      if ( umlsCodes != null && !umlsCodes.isEmpty() ) {
         return umlsCodes;
      }
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Class<? extends IdentifiedAnnotation> clazz = annotation.getClass();
         if ( clazz.equals( EventMention.class ) ) {
            return Collections.singletonList( EVENT_CODE );
         } else if ( clazz.equals( TimeMention.class ) ) {
            return Collections.singletonList( TIMEX_CODE );
         } else if ( clazz.equals( EntityMention.class ) ) {
            return Collections.singletonList( ENTITY_CODE );
         }
      }
      return Collections.emptyList();
   }

   /**
    * @param concept    -
    * @return the code of a Semantic type associated with the concept
    */
   static public String getSemanticCode( final UmlsConcept concept ) {
      final String tui = concept.getTui();
      return getSemanticCode( tui );
   }

   /**
    * @param tui a tui of interest
    * @return the code of a Semantic type associated with the tui
    */
   static public String getSemanticCode( final String tui ) {
      if ( tui == null || tui.isEmpty() ) {
         return UNKNOWN_SEMANTIC_CODE;
      }
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         if ( semanticGroup._tuis.contains( tui ) ) {
            return semanticGroup._code;
         }
      }
      return UNKNOWN_SEMANTIC_CODE;
   }

   /**
    * @param code the code of a Semantic type
    * @return the name of a Semantic type associated with the given code
    */
   static public String getNameForCode( final String code ) {
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         if ( semanticGroup._code.equals( code ) ) {
            return semanticGroup._name;
         }
      }
      return UNKNOWN_SEMANTIC;
   }

   /**
    *
    * @param annotation annotation whose name could not be derived by tui
    * @return the simple class name, with any suffix "Mention" removed
    */
   static private String getSimpleName( final IdentifiedAnnotation annotation ) {
      final String simpleName = annotation.getClass().getSimpleName();
      if ( simpleName.endsWith( "Mention" ) ) {
         return simpleName.substring( 0, simpleName.length() - 7 );
      }
      return simpleName;
   }


}
