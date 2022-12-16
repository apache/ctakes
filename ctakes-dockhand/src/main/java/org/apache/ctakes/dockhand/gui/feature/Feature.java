package org.apache.ctakes.dockhand.gui.feature;


import java.util.*;

import static org.apache.ctakes.dockhand.gui.feature.Option.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2019
 */
public enum Feature {
   SECTIONS( "Sections", "Split Document by Sections.", SIMPLE_SEGMENT, SPLIT_SECTION ),
   SENTENCES( "Sentences", "Split Document Text by Sentences.", OPEN_NLP_SENTENCE, PROSE_SENTENCE ),
   PARAGRAPHS( "Paragraphs", "Split Document Text by Paragraphs.", PARAGRAPH ),
   LISTS( "Lists", "Detect Formatted Lists and Tables.", LIST ),
   TOKENS( "Tokens", "Detect Word, Number and other distinct Token Types.", TOKEN ),
   ENTITIES( "Entities", "Normalize Named Entities.", ENTITY ),
   ATTRIBUTES( "Attributes", "Add Attributes such as Negation to Entities.", ML_ATTRIBUTE ),
   EVENTS( "Events", "Detect Temporal Events.", EVENT ),
   TIMES( "Times", "Detect Time and Date Expressions.", TIME ),
   DOCTIMEREL( "Relative Event Times", "Determine Relation between Temporal Events and the Time at which the Document was Written.", DOC_TIME_REL ),
   E_T_LINKS( "Event Times", "Determine Relations between Temporal Events and Times.", E_T_LINK ),
   E_E_LINKS( "Event Relations", "Determine Relations between Temporal Events and other Events.", E_E_LINK ),
   LOCATIONS( "Locations", "Determine Locations between Entities.", LOCATION ),
   SEVERITIES( "Severities", "Determine Severity of Entities.", SEVERITY ),
   COREFERENCES( "Coreferences", "Determine which Entity mentions refer to the same World Entity.", COREFERENT );

   // TODO Giant file Attributes ?  Uses Windowed ***
   // TODO Dictionary : Default, Custom.  Allow Custom to either select existing or launch dictionary creator.

   static public final Map<Feature, Collection<Feature>> REQUIREMENTS = new EnumMap<>( Feature.class );

   static {
      REQUIREMENTS.put( SECTIONS, Collections.emptyList() );
      REQUIREMENTS.put( SENTENCES, Collections.singleton( SECTIONS ) );
      REQUIREMENTS.put( PARAGRAPHS, Arrays.asList( SECTIONS, SENTENCES ) );
      REQUIREMENTS.put( LISTS, Arrays.asList( SECTIONS, SENTENCES ) );
      REQUIREMENTS.put( TOKENS, Arrays.asList( SECTIONS, SENTENCES ) );
      REQUIREMENTS.put( ENTITIES, Arrays.asList( SECTIONS, SENTENCES, TOKENS ) );
      REQUIREMENTS.put( ATTRIBUTES, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES ) );
      REQUIREMENTS.put( LOCATIONS, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES ) );
      REQUIREMENTS.put( SEVERITIES, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES ) );
      REQUIREMENTS.put( EVENTS, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES ) );
      REQUIREMENTS.put( TIMES, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES, EVENTS ) );
      REQUIREMENTS.put( DOCTIMEREL, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES, EVENTS, TIMES ) );
      REQUIREMENTS.put( E_T_LINKS, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES, EVENTS, TIMES ) );
      REQUIREMENTS.put( E_E_LINKS, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES, EVENTS, TIMES ) );
      REQUIREMENTS.put( COREFERENCES, Arrays.asList( SECTIONS, SENTENCES, TOKENS, ENTITIES ) );
   }


   private final String _name;
   private final String _description;
   private final List<Option> _options;

   Feature( final String name, final String description, final Option... options ) {
      _name = name;
      _description = description;
      _options = Arrays.asList( options );
   }

   public String getName() {
      return _name;
   }

   public String getDescription() {
      return _description;
   }

   public List<Option> getOptions() {
      return _options;
   }

   public Collection<Feature> getRequirements() {
      return REQUIREMENTS.getOrDefault( this, Collections.emptyList() );
   }

}