package org.apache.ctakes.dockhand.gui.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2019
 */
public enum Option {
   SIMPLE_SEGMENT( "Single Section",
         "Create a single Section that covers the entire document.",
         "add SimpleSegmentAnnotator" ),

   SPLIT_SECTION( "Multiple Sections",
         "Create multiple Sections based upon discovered Section Headers.",
         "add BsvRegexSectionizer" ),

   VALID_SECTION( "Only Valid Sections",
         "Filter Sections based upon discovered Section Headers.",
         "add SectionFilter" ),


   OPEN_NLP_SENTENCE( "Simple Sentences",
         "Create Sentences relying upon End of Line characters.",
         "add SentenceDetector" ),

   PROSE_SENTENCE( "Prose Sentences",
         "Create Sentences when End of Line characters may not indicate sentence boundaries.",
         "add SentenceDetectorAnnotatorBIO classifierJarPath=/org/apache/ctakes/core/models/sentdetect/model.jar",
         "add EolSentenceFixer",
         "add MrsDrSentenceJoiner"
   ),


   PARAGRAPH( "Paragraphs",
         "Create Paragraphs based upon Whitespace.",
         "add ParagraphAnnotator" ),


   LIST( "Lists",
         "Create Lists based upon formatted Sections.",
         "add ListAnnotator" ),


   TOKEN( "Tokens",
         "Create Word and other Penn TreeBank Tokens.",
         "add TokenizerAnnotatorPTB",
         "add ContextDependentTokenizerAnnotator" ),


   ENTITY( "Entities",
         "Create Named Entites based upon Dictionary Lookup.",
         "addDescription POSTagger",
         "add Chunker",
         "addDescription adjuster.ChunkAdjuster NP,NP 1",
         "addDescription adjuster.ChunkAdjuster NP,PP,NP 2",
//         "// path to the xml file containing information for dictionary lookup configuration.",
//         "cli LookupXml=l",
         "// umls credentials",
           "cli umlsKey=key",
         "add DefaultJCasTermAnnotator" ),

//   CUSTOM_ENTITY( "Custom Entities",
//         "Create Custom Named Entites based upon Dictionary Lookup.",
//         "add ContextDependentTokenizerAnnotator",
//         "addDescription POSTagger",
//         "add Chunker",
//         "addDescription adjuster.ChunkAdjuster NP,NP 1",
//         "addDescription adjuster.ChunkAdjuster NP,PP,NP 2",
//         "add DefaultJCasTermAnnotator " + someSpecifiedDictionaryConfigurationXml ),


   ML_ATTRIBUTE( "ML Attributes",
         "Assign Negation, Uncertainty, Historic, Generic, Subject using Machine Learning generated models.",
         "addDescription ClearNLPDependencyParserAE",
         "addLogged ClearNLPSemanticRoleLabelerAE",
         "package org.apache.ctakes.assertion.medfacts.cleartk",
         "addDescription PolarityCleartkAnalysisEngine",
         "addDescription UncertaintyCleartkAnalysisEngine",
         "addDescription HistoryCleartkAnalysisEngine",
         "addDescription ConditionalCleartkAnalysisEngine",
         "addDescription GenericCleartkAnalysisEngine",
         "addDescription SubjectCleartkAnalysisEngine",
         "add SubjectSectionFixer" ),

//   RULE_ATTRIBUTE( "RULE Attributes",
//         "Assign Negation, Uncertainty, Historic, Generic, Subject using Machine Learning generated models.",
//         "load AssertionSubPipe" ),


   LOCATION( "Locations",
         "Connect Entity Locations based upon a Machine Learning Model.",
         "addLogged ModifierExtractorAnnotator classifierJarPath=/org/apache/ctakes/relation/extractor/models"
         + "/modifier_extractor/model.jar",
         "addLogged LocationOfRelationExtractorAnnotator "
         + "classifierJarPath=/org/apache/ctakes/relation/extractor/models/location_of/model.jar" ),


   SEVERITY( "Severities",
         "Connect Entity Severities based upon a Machine Learning Model.",
         "addLogged ModifierExtractorAnnotator classifierJarPath=/org/apache/ctakes/relation/extractor/models"
         + "/modifier_extractor/model.jar",
         "addLogged DegreeOfRelationExtractorAnnotator "
         + "classifierJarPath=/org/apache/ctakes/relation/extractor/models/degree_of/model.jar" ),


   EVENT( "Events",
         "Create Temporal Events based upon a Machine Learning Model.",
         "addDescription EventAnnotator" ),

   TIME( "Times",
         "Create Time Mentions based upon a Machine Learning Model.",
         "add BackwardsTimeAnnotator classifierJarPath=/org/apache/ctakes/temporal/models/timeannotator/model.jar" ),

   DOC_TIME_REL( "Relative Event Times",
         "Assign Event Occurrence Relative to Document Creation based upon a Machine Learning Model.",
         "add DocTimeRelAnnotator classifierJarPath=/org/apache/ctakes/temporal/models/doctimerel/model.jar" ),

   E_T_LINK( "Event Times",
         "Assign Event Occurrence Relative to Times based upon a Machine Learning Model.",
         "add EventTimeRelationAnnotator classifierJarPath=/org/apache/ctakes/temporal/models/eventtime/model.jar" ),

   E_E_LINK( "Relative Event Timing",
         "Assign Event Occurrence Relative to other Events based upon a Machine Learning Model.",
         "add EventEventRelationAnnotator classifierJarPath=/org/apache/ctakes/temporal/models/eventevent/model.jar" ),


   COREFERENT( "Coreferences",
         "Assign Coreferent Entities based upon a Machine Learning Model.",
         "add ConstituencyParser",
         "add DeterministicMarkableAnnotator",
         "addDescription MarkableSalienceAnnotator /org/apache/ctakes/temporal/models/salience/model.jar",
         "addDescription MentionClusterCoreferenceAnnotator /org/apache/ctakes/coreference/models/mention-cluster/model.jar" );

   private final String _name;
   private final String _description;
   private final String[] _piperLines;

   Option( final String name, final String description, final String... piperLines ) {
      _name = name;
      _description = description;
      _piperLines = piperLines;
   }

   public String getName() {
      return _name;
   }

   public String getDescription() {
      return _description;
   }

   // TODO make some sort of "enabled" list, so that a piper command is only added if previous piper commands exist.
   public enum PiperLineRequirements {
      LIST_PARAGRAPH_FIXER( "add ListParagraphFixer",
            "add ParagraphAnnotator",
            "add ListAnnotator" ),
      SECTION_FILTER( "add SectionFilter",
            "add BsvRegexSectionizer" ),
      SUBJECT_SECTION_FIXER( "add SubjectSectionFixer",
            "add BsvRegexSectionizer" ),
      LIST_SENTENCE_FIXER( "add ListSentenceFixer",
            "add SentenceDetectorAnnotatorBIO classifierJarPath=/org/apache/ctakes/core/models/sentdetect/model.jar",
            "add ListAnnotator" ),
      PARAGRAPH_SENTENCE_FIXER( "add ParagraphSentenceFixer",
            "add SentenceDetectorAnnotatorBIO classifierJarPath=/org/apache/ctakes/core/models/sentdetect/model.jar",
            "add ParagraphAnnotator" );

      private final String _addable;
      private final String[] _requirements;

      PiperLineRequirements( final String piperAddable, final String... piperRequirements ) {
         _addable = piperAddable;
         _requirements = piperRequirements;
      }

      static public boolean hasRequirements( final String piperLine, final Collection<String> previousLines ) {
         for ( PiperLineRequirements value : values() ) {
            if ( value._addable.equals( piperLine ) ) {
               return previousLines.containsAll( Arrays.asList( value._requirements ) );
            }
         }
         return true;
      }
   }


   public List<String> getPiperLines() {
      final List<String> piperLines = new ArrayList<>();
      piperLines.add( "" );
      piperLines.add( "//   " + _description );
      piperLines.addAll( Arrays.asList( _piperLines ) );
      return piperLines;
   }

}
