package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.temporal.utils.CalendarUtil;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
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
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.apache.ctakes.temporal.utils.CalendarUtil.NULL_CALENDAR;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2019
 */
@PipeBitInfo(
      name = "SimpleMedDatesFinder",
      description = "Finds start and stop dates for medication events.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
      products = PipeBitInfo.TypeProduct.TEMPORAL_RELATION
)
final public class SimpleMedDatesFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SimpleMedDatesFinder" );

   /**
    * specifies the type of window to use for lookup
    */
   static public final String PARAM_LOOKUP_WINDOW_ANNOTATION = "LookupWindow";
   static public final String PARAM_SECTION_LIST_PATH = "SectionList";
   static public final String PARAM_CUI_LIST_PATH = "CuiList";


   static private final String DEFAULT_LOOKUP_WINDOW = "org.apache.ctakes.typesystem.type.textspan.Paragraph";
   @ConfigurationParameter( name = PARAM_LOOKUP_WINDOW_ANNOTATION,
         description = "Type of Lookup window to use.  Default is Paragraph.",
         mandatory = false,
         defaultValue = DEFAULT_LOOKUP_WINDOW
   )
   private String _windowClassName;


   static public final String SECTION_LIST_DESC
         = "Path to a file containing a list of sections of interest.  If none is specified then all sections are viable.";
   @ConfigurationParameter(
         name = PARAM_SECTION_LIST_PATH,
         description = SECTION_LIST_DESC,
         mandatory = false
   )
   private String _sectionListPath;


   static public final String CUI_LIST_DESC
         = "path to a file containing a list of cuis of interest.  If none is specified then all cuis are viable.";
   @ConfigurationParameter(
         name = PARAM_CUI_LIST_PATH,
         description = CUI_LIST_DESC,
         mandatory = false
   )
   private String _cuiListPath;


   private Class<? extends Annotation> _lookupClass;
   private final Collection<String> _sectionList = new ArrayList<>();
   private final Collection<String> _cuiList = new ArrayList<>();


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      // Always call the super first
      super.initialize( context );

      loadSections();
      loadCuis();

      try {
         final Class<?> windowClass = Class.forName( _windowClassName );
         if ( !Annotation.class.isAssignableFrom( windowClass ) ) {
            LOGGER.error( "Lookup Window Class " + _windowClassName + " not found" );
            throw new ResourceInitializationException( new ClassNotFoundException() );
         }
         _lookupClass = (Class<? extends Annotation>)windowClass;
      } catch ( ClassNotFoundException cnfE ) {
         LOGGER.error( "Lookup Window Class " + _windowClassName + " not found" );
         throw new ResourceInitializationException( cnfE );
      }
      LOGGER.info( "Using Simple Event Date lookup window type: " + _windowClassName );

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Medication Dates ..." );

      final Map<Annotation, Collection<Annotation>> windowAnnotationMap = new HashMap<>();
      if ( _sectionList.isEmpty() ) {
         windowAnnotationMap.putAll( JCasUtil.indexCovered( jCas, _lookupClass, Annotation.class ) );
      } else {
         if ( _lookupClass.equals( Segment.class ) ) {
            final Map<Segment, Collection<Annotation>> sectionAnnotationMap
                  = JCasUtil.indexCovered( jCas, Segment.class, Annotation.class );
            for ( Map.Entry<Segment, Collection<Annotation>> sectionAnnotations : sectionAnnotationMap.entrySet() ) {
               final Segment section = sectionAnnotations.getKey();
               if ( _sectionList.contains( section.getPreferredText() )
                    || _sectionList.contains( section.getId() ) ) {
                  windowAnnotationMap.put( section, sectionAnnotations.getValue() );
               }
            }
         } else {
            final Map<Segment, Collection<Annotation>> sectionWindowMap
                  = JCasUtil.indexCovered( jCas, Segment.class, _lookupClass );
            for ( Map.Entry<Segment, Collection<Annotation>> sectionWindows : sectionWindowMap.entrySet() ) {
               final Segment section = sectionWindows.getKey();
               if ( _sectionList.contains( section.getPreferredText() )
                    || _sectionList.contains( section.getId() ) ) {
                  final Collection<Annotation> annotations
                        = JCasUtil.selectCovered( jCas, Annotation.class, section );
                  windowAnnotationMap.putAll( splitCovered( annotations, sectionWindows.getValue() ) );
               }
            }

         }
      }

      for ( Map.Entry<Annotation, Collection<Annotation>> windowAnnotations : windowAnnotationMap.entrySet() ) {
         processWindow( jCas, windowAnnotations.getKey(), windowAnnotations.getValue() );
      }
      LOGGER.info( "Finished." );
   }


   /**
    * For instance "Aspirin, 10mg.  Started 1/1/2000.  Stopped 12/31/2000."
    *
    * @param jCas        ye olde ...
    * @param window      contains relatable annotations.
    * @param annotations -
    */
   private void processWindow( final JCas jCas,
                               final Annotation window,
                               final Collection<Annotation> annotations ) {
      final int offset = window.getBegin();
      final String text = window.getCoveredText().toLowerCase();
      final List<Integer> startSpans = getTextIndices( text, "started" );
      final List<Integer> stopSpans = getTextIndices( text, "stopped" );
      if ( startSpans.isEmpty() && stopSpans.isEmpty() ) {
         return;
      }

      final List<Pair<Integer>> spans = new ArrayList<>();
      final Map<Pair<Integer>, EventMention> medMap = new HashMap<>();
      final Map<Pair<Integer>, Calendar> calendarMap = new HashMap<>();

      for ( Annotation annotation : annotations ) {
         final Pair<Integer> span = createInWindowSpan( annotation, offset );
         if ( (annotation instanceof MedicationEventMention || annotation instanceof MedicationMention)
              && !spans.contains( createInWindowSpan( annotation, offset ) )
              && (_cuiList.isEmpty()
                  || OntologyConceptUtil.getCuis( (IdentifiedAnnotation)annotation )
                                        .stream()
                                        .anyMatch( _cuiList::contains )) ) {
            spans.add( span );
            medMap.put( span, (EventMention)annotation );
         } else if ( (annotation instanceof DateAnnotation || annotation instanceof TimeMention)
                     && !spans.contains( span ) ) {
            final Calendar calendar = CalendarUtil.createTimexCalendar( annotation );
            if ( !NULL_CALENDAR.equals( calendar ) ) {
               spans.add( span );
               calendarMap.put( span, calendar );
            }
         } else if ( annotation instanceof Chunk ) {
            final Calendar calendar = CalendarUtil.getTextCalendar( annotation.getCoveredText() );
            if ( !NULL_CALENDAR.equals( calendar ) ) {
               spans.add( span );
               calendarMap.put( span, calendar );
            }
         }
      }
      startSpans.sort( Integer::compareTo );
      stopSpans.sort( Integer::compareTo );
      spans.sort( Comparator.comparingInt( Pair::getValue1 ) );
      cleanSpans( spans, calendarMap );

      processSpans( jCas, offset, spans, medMap, calendarMap, startSpans, stopSpans );
   }

   /**
    * Once spans and their contained information have been pulled out of a window, create start and stop TimeMentions
    *
    * @param jCas           ye olde ...
    * @param offset         the offset of the window beginning in the document
    * @param medOrDateSpans list of both medication and calendar spans
    * @param medMap         span and medication
    * @param calendarMap    span and calendar
    * @param startSpans     spans with text "started"
    * @param stopSpans      spans with text "stopped"
    */
   static private void processSpans( final JCas jCas,
                                     final int offset,
                                     final List<Pair<Integer>> medOrDateSpans,
                                     final Map<Pair<Integer>, EventMention> medMap,
                                     final Map<Pair<Integer>, Calendar> calendarMap,
                                     final List<Integer> startSpans,
                                     final List<Integer> stopSpans ) {
      int startIndex = startSpans.size() - 1;
      int stopIndex = stopSpans.size() - 1;

      Date startDate = null;
      Date stopDate = null;
      TimeMention startMention = null;
      TimeMention stopMention = null;

      for ( int z = medOrDateSpans.size() - 1; z >= 0; z-- ) {
         final Pair<Integer> medOrDateSpan = medOrDateSpans.get( z );
         final int startSpanBegin = startIndex >= 0 ? startSpans.get( startIndex ) : Integer.MAX_VALUE;
         final int stopSpanBegin = stopIndex >= 0 ? stopSpans.get( stopIndex ) : Integer.MAX_VALUE;
         final EventMention med = medMap.get( medOrDateSpan );
         if ( med != null ) {
            if ( med instanceof MedicationEventMention ) {
               if ( startDate != null ) {
                  ((MedicationEventMention)med).setStartDate( startDate );
               }
               if ( stopDate != null ) {
                  ((MedicationEventMention)med).setEndDate( stopDate );
               }
            } else if ( med instanceof MedicationMention ) {
               if ( startMention != null ) {
                  ((MedicationMention)med).setStartDate( startMention );
               }
               if ( stopMention != null ) {
                  ((MedicationMention)med).setEndDate( stopMention );
               }
            }
         } else {
            final Calendar calendar = calendarMap.get( medOrDateSpan );
            if ( medOrDateSpan.getValue1() > startSpanBegin &&
                 medOrDateSpan.getValue1() < startSpanBegin + 15 ) {
               startDate = CalendarUtil.createTypeDate( jCas, calendar );
               startMention = new TimeMention( jCas,
                     offset + medOrDateSpan.getValue1(), offset + medOrDateSpan.getValue2() );
               startMention.setDate( startDate );
               startIndex--;
            } else if ( medOrDateSpan.getValue1() > stopSpanBegin && medOrDateSpan.getValue1() < stopSpanBegin + 15 ) {
               stopDate = CalendarUtil.createTypeDate( jCas, calendar );
               stopMention = new TimeMention( jCas,
                     offset + medOrDateSpan.getValue1(), offset + medOrDateSpan.getValue2() );
               stopMention.setDate( stopDate );
               stopIndex--;
               startDate = null;
               startMention = null;
            }
         }
      }
   }

   /**
    *  For faster processing of windows, instead of calling JCasUtil.indexCovered(..), do it with known possibilities
    * @param annotations possibly covered
    * @param covering covering annotations
    * @return -
    */
   static private Map<Annotation, Collection<Annotation>> splitCovered(
         final Collection<Annotation> annotations,
         final Collection<Annotation> covering ) {
      final Map<Annotation, Collection<Annotation>> covered = new HashMap<>();
      for ( Annotation annotation : annotations ) {
         final int begin = annotation.getBegin();
         for ( Annotation cover : covering ) {
            if ( begin >= cover.getBegin() && begin < cover.getEnd() ) {
               covered.computeIfAbsent( cover, c -> new ArrayList<>() ).add( annotation );
            }
         }
      }
      return covered;
   }

   /**
    *
    * @param windowText text with in a window to be searched
    * @param searchText text wanted
    * @return collection of begin indices, one for each discovered text in the window
    */
   static private List<Integer> getTextIndices( final String windowText, final String searchText ) {
      final String text = windowText.toLowerCase();
      final int maxIndex = text.length() - 1;
      final List<Integer> indices = new ArrayList<>();
      int index = text.indexOf( searchText );
      while ( index >= 0 ) {
         indices.add( index );
         if ( index == maxIndex ) {
            break;
         }
         index = text.indexOf( searchText, index + 1 );
      }
      return indices;
   }

   /**
    * @param annotation within a window
    * @param offset     the offset of the window beginning in the document
    * @return a Pair with begin end values equal to those of the annotation minus the window offset
    */
   static private Pair<Integer> createInWindowSpan( final Annotation annotation, final int offset ) {
      return new Pair<>( annotation.getBegin() - offset, annotation.getEnd() - offset );
   }

   /**
    * Sometimes TimeMentions and/or Chunks overlap and create incomplete dates.
    * For instance, chunk "7-1-2002", TimeMention "7-1".
    *
    * @param spans       contains both calendar and medication spans
    * @param calendarMap -
    */
   static private void cleanSpans( final List<Pair<Integer>> spans, final Map<Pair<Integer>, Calendar> calendarMap ) {
      final Collection<Pair<Integer>> removalSpans = new ArrayList<>();
      for ( int i = 0; i < spans.size() - 1; i++ ) {
         final Pair<Integer> iSpan = spans.get( i );
         if ( !calendarMap.keySet().contains( iSpan ) ) {
            continue;
         }
         for ( int j = i + 1; j < spans.size(); j++ ) {
            final Pair<Integer> jSpan = spans.get( j );
            if ( !calendarMap.keySet().contains( jSpan ) ) {
               continue;
            }
            if ( (iSpan.getValue1() <= jSpan.getValue1() && jSpan.getValue2() < iSpan.getValue2())
                 || (iSpan.getValue1() < jSpan.getValue1() && jSpan.getValue2() <= iSpan.getValue2()) ) {
               removalSpans.add( jSpan );
            } else if ( (jSpan.getValue1() <= iSpan.getValue1() && iSpan.getValue2() < jSpan.getValue2())
                        || (jSpan.getValue1() < iSpan.getValue1() && iSpan.getValue2() <= jSpan.getValue2()) ) {
               removalSpans.add( iSpan );
            }
         }
      }
      spans.removeAll( removalSpans );
      calendarMap.keySet().removeAll( removalSpans );
   }

   /**
    * If a custom medication section list was specified, load it
    * @throws ResourceInitializationException -
    */
   synchronized private void loadSections() throws ResourceInitializationException {
      if ( _sectionListPath == null ) {
         return;
      }
      loadList( _sectionListPath, _sectionList );
   }

   /**
    * If a custom medication cui list was specified, load it
    * @throws ResourceInitializationException -
    */
   synchronized private void loadCuis() throws ResourceInitializationException {
      if ( _cuiListPath == null ) {
         return;
      }
      loadList( _cuiListPath, _cuiList );
   }

   /**
    * Generic load method
    * @param filePath -
    * @param list -
    * @throws ResourceInitializationException -
    */
   synchronized private void loadList( final String filePath, final Collection<String> list )
         throws ResourceInitializationException {
      if ( filePath == null ) {
         return;
      }
      LOGGER.info( "Parsing " + filePath );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator
            .getAsStream( filePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String value = readBsvLine( line );
            if ( !value.isEmpty() ) {
               list.add( value );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      LOGGER.info( "Finished Parsing" );
   }

   /**
    * @param line double-bar separated text
    */
   private String readBsvLine( final String line ) {
      if ( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "//" ) ) {
         // comment
         return "";
      }
      final String[] splits = line.split( "\\|" );
      if ( splits.length >= 1 ) {
         // We are only interested in the first entry
         return splits[ 0 ].trim();
      }
      return "";
   }


   static public AnalysisEngineDescription createEngineDescription( final String cuiListPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( SimpleMedDatesFinder.class,
            PARAM_CUI_LIST_PATH, cuiListPath );
   }

}
