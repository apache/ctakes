package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.textspan.DefaultAspanComparator;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.ResultOfTextRelation;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.Role.ANNOTATOR;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * See Jira at https://issues.apache.org/jira/browse/CTAKES-441
 *
 * @author Kean Kaufmann
 * @since 11/13/2017
 */
@PipeBitInfo(
      name = "LabValueFinder",
      description = "Associates Lab Mentions with values.",
      role = ANNOTATOR,
      dependencies = { SECTION, BASE_TOKEN, IDENTIFIED_ANNOTATION },
      products = { GENERIC_RELATION }
)
final public class LabValueFinder extends JCasAnnotator_ImplBase {

   public static final String PARAM_ALL_SECTIONS = "allSections";
   public static final String PARAM_SECTIONS = "sections";
   public static final String PARAM_VALUE_WORDS = "valueWords";
   public static final String PARAM_MAX_NEWLINES = "maxLineCount";
   public static final int DEFAULT_MAX_LINE_COUNT = 2;
   public static final String PARAM_LAB_TUIS = "labTUIs";
   public static final String PARAM_LAB_X_CUIS = "excludeCUIs";
   public static final String PARAM_USE_DRUGS = "useDrugs";

   static private final String[] REQUIRED_SECTIONS = { "2.16.840.1.113883.10.20.22.2.3.1" };
   static private final String[] REQUIRED_VALUE_WORDS = { "positive", "negative", "elevated", "normal", "increased", "decreased" };
   static private final String[] REQUIRED_LAB_TUIS = {
         "T059",    // Laboratory Procedure
         "T060",    // Diagnostic Procedure (e.g. GFR)
         "T201" }; // Clinical Attribute (e.g. A/G Ratio)
   static private final String[] REQUIRED_EXCLUDE_CUIS = {
         "C1443182",     // "Calculated (procedure)"
         "C1715372",    // "Medical problem"
         "C1441604" }; // "High sensitivity"

   static final Logger LOGGER = Logger.getLogger( "LabValueFinder" );

   @ConfigurationParameter( name = PARAM_ALL_SECTIONS,
         description = "Use all Annotatable sections.  This ignores the value of " + PARAM_SECTIONS,
         defaultValue = "true",
         mandatory = false )
   private String _useAllSectionText;
   private boolean _useAllSections;

   @ConfigurationParameter( name = PARAM_SECTIONS,
         description = "Annotatable sections",
         defaultValue = {},
         mandatory = false )
   private String[] _annotatableSections;
   private Collection<String> annotatableSections;

   @ConfigurationParameter( name = PARAM_VALUE_WORDS,
         description = "Words indicating values",
         defaultValue = {},
         mandatory = false )
   private String[] _valueWords;
   private Collection<String> valueWords;

   @ConfigurationParameter( name = PARAM_MAX_NEWLINES,
         description = "Maximum newlines between lab and value",
         mandatory = false )
   private int maxLineCount = DEFAULT_MAX_LINE_COUNT;

   @ConfigurationParameter( name = PARAM_LAB_TUIS,
         description = "TUIs indicating lab measurements",
         defaultValue = {} )
   private String[] _labTuis;
   private Collection<String> labTuis;

   @ConfigurationParameter( name = PARAM_LAB_X_CUIS,
         description = "CUIs not indicating specific lab measurements",
         defaultValue = {},
         mandatory = false )
   private String[] _excludeCuis;
   private Collection<String> excludeCuis;

   @ConfigurationParameter( name = PARAM_USE_DRUGS,
         description = "Use Medications in addition to Labs.",
         defaultValue = "false",
         mandatory = false )
   private String _useDrugsText;
   private boolean _useDrugs;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      // Always call the super first
      super.initialize( context );

      _useAllSections = Boolean.parseBoolean( _useAllSectionText );
      // Start the lists with the required parameters, then add user parameters.
      annotatableSections = gatherParameters( REQUIRED_SECTIONS, _annotatableSections );
      valueWords = gatherParameters( REQUIRED_VALUE_WORDS, _valueWords );
      labTuis = gatherParameters( REQUIRED_LAB_TUIS, _labTuis );
      excludeCuis = gatherParameters( REQUIRED_EXCLUDE_CUIS, _excludeCuis );
      _useDrugs = Boolean.parseBoolean( _useDrugsText );

      LOGGER.debug( PARAM_MAX_NEWLINES + " = " + maxLineCount );
      LOGGER.info( labTuis.size() + " lab TUIs: " + labTuis.toString() );
   }

   /**
    * @param requiredValues -
    * @param userValues     -
    * @return A collection of all values in upper case
    */
   static private Collection<String> gatherParameters( final String[] requiredValues, final String[] userValues ) {
      final Collection<String> values = Arrays.stream( requiredValues )
            .map( String::toUpperCase )
            .collect( Collectors.toSet() );
      for ( String value : userValues ) {
         values.add( value.toUpperCase() );
      }
      return values;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Associating Labs with values ..." );

      final List<Class<? extends Annotation>> valueClasses = Arrays.asList( NumToken.class, FractionAnnotation.class );

      // Time may filter wanted clock positions such as in breast cancer
      final Map<Annotation, List<IdentifiedAnnotation>> filterMap = createCoveringMap( jCas, valueClasses,
            Arrays.asList( DateAnnotation.class, TimeAnnotation.class ) );

      final Map<Annotation, List<IdentifiedAnnotation>> subsumeMap = createCoveringMap( jCas, valueClasses,
            Arrays.asList( FractionAnnotation.class, RangeAnnotation.class, MeasurementAnnotation.class ) );

      for ( Segment segment : JCasUtil.select( jCas, Segment.class ) ) {
         if ( _useAllSections || annotatableSections.isEmpty() || annotatableSections.contains( segment.getId() ) ) {
            final List<LabMention> mentions = annotateMentions( jCas, segment );
            fillInValues( jCas, mentions, filterMap, subsumeMap, segment.getBegin(), segment.getEnd() );
         }
      }

      LOGGER.info( "Finished." );
   }

   /**
    * @param jCas    -
    * @param segment -
    * @return Existing and extracted LabMentions in the segment.
    */
   private List<LabMention> annotateMentions( final JCas jCas, final Segment segment ) {
      final List<LabMention> unvaluedLabMentions = new ArrayList<>();
      for ( IdentifiedAnnotation annotation : JCasUtil.selectCovered( jCas, IdentifiedAnnotation.class, segment ) ) {
         // first check to see if the annotation is a lab mention.
         if ( LabMention.class.isInstance( annotation ) ) {
            // Check for existing value.
            final ResultOfTextRelation relation = ((LabMention) annotation).getLabValue();
            if ( relation != null && relation.getArg2() != null ) {
               // LabMention is already fully established with a value.  Skip it.
               continue;
            } else if ( relation == null ) {
               // LabMention needs a value relation.
               initValueRelation( jCas, (LabMention) annotation );
            }
            // add the LabMention and move on.
            unvaluedLabMentions.add( (LabMention)annotation );
            continue;
         }
         // Annotation was not a LabMention, but check to see if any part of it can be.
         if ( _useDrugs && MedicationMention.class.isInstance( annotation ) ) {
            LOGGER.info( "Using Drug " + annotation.getCoveredText() );
            // We have valid lab concepts in the annotation.  Create an overlapping LabMention with those concepts.
            final LabMention lab = createLabMention( jCas,
                  OntologyConceptUtil.getUmlsConcepts( annotation ), annotation.getBegin(), annotation.getEnd() );
            unvaluedLabMentions.add( lab );
         }
         // Check using hardcoded extra concept types.
         final Collection<UmlsConcept> validConcepts
               = OntologyConceptUtil.getUmlsConceptStream( annotation )
               .filter( c -> labTuis.contains( c.getTui() ) )
               .filter( c -> !excludeCuis.contains( c.getCui() ) ).collect( Collectors.toList() );
         if ( validConcepts.isEmpty() ) {
            continue;
         }
         // We have valid lab concepts in the annotation.  Create an overlapping LabMention with those concepts.
         final LabMention lab = createLabMention( jCas, validConcepts, annotation.getBegin(), annotation.getEnd() );
         unvaluedLabMentions.add( lab );
      }
      return unvaluedLabMentions;
   }

   /**
    * @param jCas -
    * @param lab  for which a value relation should be initialized.
    */
   static private void initValueRelation( final JCas jCas, final LabMention lab ) {
      final ResultOfTextRelation relation = new ResultOfTextRelation( jCas );
      RelationArgument arg1 = new RelationArgument( jCas );
      arg1.setArgument( lab );
      relation.setArg1( arg1 );
      // set lab mention's value as the relation.
      lab.setLabValue( relation );
   }

   /**
    * @param jCas     -
    * @param concepts Lab concepts.
    * @param begin    begin index for a new LabMention.
    * @param end      end index for a new LabMention.
    * @return a new LabMention with the given attributes.
    */
   static private LabMention createLabMention( final JCas jCas, final Collection<UmlsConcept> concepts,
                                               final int begin, final int end ) {
      final LabMention lab = new LabMention( jCas, begin, end );
      lab.setId( CONST.NE_TYPE_ID_LAB );
      lab.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
      // copy in the concepts.
      final FSArray conceptArray = new FSArray( jCas, concepts.size() );
      int arrIdx = 0;
      for ( UmlsConcept umlsConcept : concepts ) {
         conceptArray.set( arrIdx, umlsConcept );
         arrIdx++;
      }
      lab.setOntologyConceptArr( conceptArray );
      // create value relation, argument 1 is the lab mention.
      initValueRelation( jCas, lab );
      lab.addToIndexes();
      LOGGER.debug( "created " + getDebugText( lab ) );
      return lab;
   }

   /**
    * @param docText      -
    * @param segmentBegin begin index of the section.
    * @param segmentEnd   end index of the section.
    * @return List of all newline indices in the section, plus the end of the section.
    */
   static private List<Integer> getNewLines( final String docText, final int segmentBegin, final int segmentEnd ) {
      final List<Integer> newLines = new ArrayList<>();
      int index = docText.indexOf( '\n', segmentBegin );
      while ( index >= 0 && index < segmentEnd ) {
         newLines.add( index );
         index = docText.indexOf( '\n', index + 1 );
      }
      newLines.add( segmentEnd );
      return newLines;
   }

   private void fillInValues( final JCas jCas,
                              final List<LabMention> labs,
                              final Map<Annotation, List<IdentifiedAnnotation>> filterMap,
                              final Map<Annotation, List<IdentifiedAnnotation>> subsumeMap,
                              final int segmentBegin,
                              final int segmentEnd ) {
      if ( labs == null || labs.isEmpty() ) {
         return;
      }
      final List<Integer> newLines = getNewLines( jCas.getDocumentText(), segmentBegin, segmentEnd );

      final List<LabMention> sortedLabs = sortOverlapsByLength( labs );
      final int numMentions = sortedLabs.size();
      final Collection<Annotation> candidateSet = new HashSet<>();
      for ( int i = 0; i < numMentions; i++ ) {
         candidateSet.clear();
         final LabMention lab = sortedLabs.get( i );
         Annotation value = null;
         final LabMention nextLab = (i + 1 < sortedLabs.size()) ? sortedLabs.get( i + 1 ) : null;
         final int nextLabBegin = nextLab != null ? nextLab.getBegin() : newLines.get( newLines.size() - 1 );
         final int windowBegin = lab.getEnd();
         final int windowEnd = getValueWindowEnd( windowBegin, nextLabBegin, newLines );

         LOGGER.debug( "Seeking value for: " + getDebugText( lab ) + " between " + windowBegin + " and " + windowEnd );
         for ( NumToken numToken : JCasUtil.selectCovered( jCas, NumToken.class, windowBegin, windowEnd ) ) {
            LOGGER.debug( "   " + getDebugText( numToken ) );
            final List<IdentifiedAnnotation> filters = filterMap.get( numToken );
            if ( filters != null && !filters.isEmpty() ) {
               LOGGER.debug( "      Filtering due to " + getDebugText( filters.get( 0 ) ) );
            } else {
               final List<IdentifiedAnnotation> subsumers = subsumeMap.getOrDefault( numToken, Collections.emptyList() );
               if ( subsumers.isEmpty() ) {
                  candidateSet.add( numToken );
               } else {
                  candidateSet.addAll( subsumers );
                  LOGGER.debug( "subsuming candidate: " + getDebugText( numToken ) );
               }
            }
         }
         if ( !candidateSet.isEmpty() ) {
            // get first, shortest span value
            final List<Annotation> candidateList = new ArrayList<>( candidateSet );
            candidateList.sort( DefaultAspanComparator.getInstance() );
            // prefer non-range values, if any
            value = candidateList.stream()
                  .filter( a -> !(a instanceof RangeAnnotation) )
                  .findFirst()
                  .orElse( candidateList.get( 0 ) );
            LOGGER.debug( "Set to value: " + getDebugText( value ) );
         } else {
            // attempt to find a text (word) value
            value = JCasUtil.selectCovered( jCas, WordToken.class, windowBegin, windowEnd ).stream()
                  .filter( w -> valueWords.contains( w.getCoveredText().toUpperCase() ) )
                  .findFirst()
                  .orElse( null );
         }
         if ( value != null ) {
            LOGGER.debug( "setting lab value to " + getDebugText( value ) );
            final RelationArgument arg2 = new RelationArgument( jCas );
            arg2.setArgument( value );
            lab.getLabValue().setArg2( arg2 );
         }
      }
   }

   // first of: start of next mention, start of max newline, or end of segment
   private int getValueWindowEnd( final int windowBegin, final int nextLabBegin, final List<Integer> newLines ) {
      int eolSkips = 0;
      int maxNewLine = newLines.get( newLines.size() - 1 );
      for ( Integer newLine : newLines ) {
         if ( newLine >= windowBegin ) {
            eolSkips++;
            if ( eolSkips > maxLineCount ) {
               break;
            }
            maxNewLine = newLine;
            if ( newLine > nextLabBegin ) {
               break;
            }
         }
      }
      return Math.min( maxNewLine, nextLabBegin );
   }


   @SuppressWarnings( { "rawtypes", "unchecked" } ) // hold my beer and watch this...
   static private Map<Annotation, List<IdentifiedAnnotation>> createCoveringMap( final JCas jCas,
                                                                                 final List<Class<? extends Annotation>> coveredClasses,
                                                                                 final List<Class<? extends IdentifiedAnnotation>> coveringClasses ) {
      final Map<Annotation, List<IdentifiedAnnotation>> allCovering = new HashMap<>();
      for ( Class covered : coveredClasses ) {
         for ( Class covering : coveringClasses ) {
            final Map<? extends Annotation, List<? extends IdentifiedAnnotation>> map
                  = JCasUtil.indexCovering( jCas, covered, covering );
            map.forEach( ( k, v ) -> allCovering.computeIfAbsent( k, c -> new ArrayList<>() )
                                                .addAll( v ) );
         }
      }
      return allCovering;
   }

   /**
    * The method name does not really describe what it does, but it is close.
    *
    * @param list -
    * @param <T>  we only deal with annotations.
    * @return a sorted list.
    */
   static private <T extends Annotation> List<T> sortOverlapsByLength( final List<T> list ) {
      final List<T> sortedList = new ArrayList<>( list );
      sortedList.sort( ( a1, a2 ) -> {
         int begin1 = a1.getBegin();
         int end1 = a1.getEnd();
         int begin2 = a2.getBegin();
         int end2 = a2.getEnd();
         int beginCompare = Integer.compare( begin1, begin2 );
         return ((beginCompare < 0) ? Integer.compare( end1, begin2 )
               : (beginCompare == 0) ? Integer.compare( end1, end2 ) : Integer.compare( begin1, end2 ));
      } );
      return sortedList;
   }

   static private String getDebugText( final Annotation a ) {
      return a.getType().getShortName() + "(" + a.getBegin() + "-" + a.getEnd() + "): " + a.getCoveredText();
   }


   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( LabValueFinder.class );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final Object... objects ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( LabValueFinder.class, objects );
   }


}
