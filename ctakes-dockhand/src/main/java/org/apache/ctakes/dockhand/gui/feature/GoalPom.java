package org.apache.ctakes.dockhand.gui.feature;

import java.util.Arrays;
import java.util.Collection;

import static org.apache.ctakes.dockhand.gui.feature.Feature.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2019
 */
public enum GoalPom {
   CORE( "token", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS ),
   LOOKUP( "entity", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES ),
   ASSERTION( "attribute", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES ),
   RELATION( "relation", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES, LOCATIONS, SEVERITIES ),
   TEMPORAL( "temporal", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES, LOCATIONS, SEVERITIES,
         EVENTS, TIMES, DOCTIMEREL, E_T_LINKS, E_E_LINKS ),
   COREFERENCE( "coreference", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES, COREFERENCES ),
   RELATION_COREF( "relation_coref", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES, LOCATIONS,
         SEVERITIES, COREFERENCES ),
   RELATION_TEMPORAL( "relation_temporal", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES, LOCATIONS,
         SEVERITIES, COREFERENCES ),
   TEMPORAL_COREF( "temporal_coref", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES,
         EVENTS, TIMES, DOCTIMEREL, E_T_LINKS, E_E_LINKS, COREFERENCES ),
   RELATION_TEMPORAL_COREF( "relation_temporal_coref", 10, SECTIONS, SENTENCES, PARAGRAPHS, LISTS, TOKENS, ENTITIES, ATTRIBUTES,
         LOCATIONS, SEVERITIES, EVENTS, TIMES, DOCTIMEREL, E_T_LINKS, E_E_LINKS, COREFERENCES );

   private final String _goal;
   private final int _mb;
   private final Feature[] _layersHandled;

   GoalPom( final String goal, final int mb, final Feature... layersHandled ) {
      _goal = goal;
      _mb = mb;
      _layersHandled = layersHandled;
   }

   public String getPomFile() {
      return _goal + "_pom.xml";
   }

   public int getMegaBytes() {
      return _mb;
   }

   public Collection<Feature> getLayersHandled() {
      return Arrays.asList( _layersHandled );
   }

   static public GoalPom getAppropriatePom( final Collection<Feature> features ) {
      GoalPom simplestPom = GoalPom.CORE;
      for ( GoalPom goalPom : values() ) {
         if ( goalPom.getLayersHandled().containsAll( features ) ) {
            simplestPom = goalPom;
            break;
         }
      }
      return simplestPom;
   }


}
