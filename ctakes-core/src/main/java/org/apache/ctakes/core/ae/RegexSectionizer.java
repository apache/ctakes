package org.apache.ctakes.core.ae;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.core.util.regex.TimeoutMatcher;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/20/2016
 */
@PipeBitInfo(
      name = "Regex Sectionizer (A)",
      description = "Annotates Document Sections by detecting Section Headers using Regular Expressions.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
abstract public class RegexSectionizer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LogManager.getLogger( "RegexSectionizer" );

   static public final String PARAM_TAG_DIVIDERS = "TagDividers";
   @ConfigurationParameter(
         name = PARAM_TAG_DIVIDERS,
         description = "True if lines of divider characters ____ , ---- , === should divide sections",
         defaultValue = "true",
         mandatory = false
   )
   private boolean _tagDividers = true;

   /**
    * classic ctakes default segment id
    */
   static private final String DEFAULT_SEGMENT_ID = "SIMPLE_SEGMENT";
   static private final String SECTION_NAME_EX = "SECTION_NAME";
   static public final String DIVIDER_LINE_NAME = "DIVIDER_LINE";
   static private final Pattern DIVIDER_LINE_PATTERN = Pattern.compile( "^[\\t ]*[_\\-=]{4,}[\\t ]*$" );

   private enum TagType {
      HEADER, FOOTER, DIVIDER
   }


   /**
    * Holder for section type as defined in the user's specification bsv file
    */
   static protected final class SectionType {
      static private final SectionType DEFAULT_TYPE = new SectionType( DEFAULT_SEGMENT_ID, null, null, true );
      private final String __name;
      private final Pattern __headerPattern;
      private final Pattern __footerPattern;
      private final boolean __shouldParse;

      public SectionType( final String name, final String headerRegex, final String footerRegex,
                          final boolean shouldParse ) {
         __name = name;
         __headerPattern = headerRegex == null ? null : Pattern
               .compile( headerRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE );
         __footerPattern = footerRegex == null ? null : Pattern
               .compile( footerRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE );
         __shouldParse = shouldParse;
      }
   }

   /**
    * Holder for information about a section tag discovered in text
    */
   static final class SectionTag {
      private final String __name;
      private final String __typeName;
      private final TagType __tagType;

      private SectionTag( final String name, final String typeName, final TagType tagType ) {
         __name = name;
         __typeName = typeName;
         __tagType = tagType;
      }
   }

   static protected final SectionTag LINE_DIVIDER_TAG
         = new SectionTag( DIVIDER_LINE_NAME, DIVIDER_LINE_NAME, TagType.DIVIDER );

   /**
    * Normally I would put this in a singleton but I'm not sure that a singleton will work well with/as uima ae
    *
    * @param segmentId id of a section / segment
    * @return false iff a section by the given id is known and was assigned the "don't parse" flag
    */
   static public boolean shouldParseSegment( final String segmentId ) {
      final SectionType sectionType = _sectionTypes.getOrDefault( segmentId, SectionType.DEFAULT_TYPE );
      return sectionType.__shouldParse;
   }


   static private final Object SECTION_TYPE_LOCK = new Object();
   static private final Map<String, SectionType> _sectionTypes = new HashMap<>();
   static private volatile boolean _sectionsLoaded = false;

   static protected void addSectionType( final SectionType sectionType ) {
      _sectionTypes.put( sectionType.__name, sectionType );
   }

   static public Map<String, SectionType> getSectionTypes() {
      return Collections.unmodifiableMap( _sectionTypes );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing ..." );
      super.initialize( context );
      synchronized (SECTION_TYPE_LOCK) {
         if ( !_sectionsLoaded ) {
            loadSections();
            _sectionsLoaded = true;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Sections ..." );
      if ( _sectionTypes.isEmpty() ) {
         LOGGER.info( "Finished processing, no section types defined" );
         return;
      }
      final String docText = jcas.getDocumentText();
      final Map<Pair<Integer>, SectionTag> headerTags = findHeaderTags( docText );
      if ( headerTags.isEmpty() ) {
         LOGGER.debug( "No section headers found" );
      }
      final Collection<Pair<Integer>> subsumedTags = getSubsumedBounds( headerTags.keySet() );
      headerTags.keySet().removeAll( subsumedTags );
      final Map<Pair<Integer>, SectionTag> footerTags = findFooterTags( docText );
      final Map<Pair<Integer>, SectionTag> dividerLines = new HashMap<>();
      if ( _tagDividers ) {
         dividerLines.putAll( findDividerLines( docText ) );
      }
      createSegments( jcas, headerTags, footerTags, dividerLines );
//      LOGGER.info( "Finished processing" );
   }


   /**
    * return subsumed bounds:
    * |=============|
    * |xxxxxxxx|
    * |xxxx|
    * |===============|
    * |================|
    * |xxxxxxxx|
    */
   // Todo make TextSpanUtil
   static private Collection<Pair<Integer>> getSubsumedBounds( final Collection<Pair<Integer>> bounds ) {
      final List<Pair<Integer>> boundsList = new ArrayList<>( bounds );
      final Collection<Pair<Integer>> subsumedBounds = new HashSet<>();
      for ( int i = 0; i < boundsList.size() - 1; i++ ) {
         final Pair<Integer> pairI = boundsList.get( i );
         for ( int j = i + 1; j < boundsList.size(); j++ ) {
            final Pair<Integer> pairJ = boundsList.get( j );
            if ( pairI.getValue1() <= pairJ.getValue1() && pairJ.getValue2() <= pairI.getValue2() ) {
               subsumedBounds.add( pairJ );
            } else if ( pairJ.getValue1() <= pairI.getValue1() && pairI.getValue2() <= pairJ.getValue2() ) {
               subsumedBounds.add( pairI );
            }
         }
      }
      return subsumedBounds;
   }

   /**
    * return latter overlapped bounds:
    * |=============|
    * |xxxxxxxx|
    * |xxxx|
    * |xxxxxxxxxxxxxx|
    * |xxxxxxxxxxxxxxx|
    * |xxxxxxx|
    */
   // Todo make TextSpanUtil
   static private Collection<Pair<Integer>> getOverlappedBounds( final Collection<Pair<Integer>> bounds ) {
      final List<Pair<Integer>> boundsList = new ArrayList<>( bounds );
      final Collection<Pair<Integer>> overlappedBounds = new HashSet<>();
      for ( int i = 0; i < boundsList.size() - 1; i++ ) {
         final Pair<Integer> pairI = boundsList.get( i );
         for ( int j = i + 1; j < boundsList.size(); j++ ) {
            final Pair<Integer> pairJ = boundsList.get( j );
            if ( pairI.getValue1() <= pairJ.getValue1() && pairJ.getValue1() <= pairI.getValue2() ) {
               overlappedBounds.add( pairJ );
            } else if ( pairJ.getValue1() <= pairI.getValue1() && pairI.getValue1() <= pairJ.getValue2() ) {
               overlappedBounds.add( pairI );
            }
         }
      }
      return overlappedBounds;
   }

   /**
    * Sorts by first offset, longer bounds first, removing subsumed and latter overlapped bounds:
    * |=============|
    * |xxxxxxxx|
    * |xxxx|
    * |xxxxxxxxxxxxxx|
    * |xxxxxxxxxxxxxxx|
    * |========|
    */
   // Todo make TextSpanUtil
   static private List<Pair<Integer>> sortAndTrimBounds( final Collection<Pair<Integer>> bounds ) {
      final List<Pair<Integer>> boundsList = new ArrayList<>( bounds );
      boundsList.sort( new PairIntSorter() );
      final Collection<Pair<Integer>> removalBounds = new HashSet<>();
      for ( int i = 0; i < boundsList.size() - 1; i++ ) {
         final Pair<Integer> pairI = boundsList.get( i );
         for ( int j = i + 1; j < boundsList.size(); j++ ) {
            final Pair<Integer> pairJ = boundsList.get( j );
            if ( pairJ.getValue1() > pairI.getValue2() ) {
               // pairJ after pairI.  Move on to next pairI
               break;
            }
            // PairI subsumes or overlaps pairJ.  Remove pairJ.
            removalBounds.add( pairJ );
         }
      }
      boundsList.removeAll( removalBounds );
      return boundsList;
   }


   /**
    * Load Sections in a manner appropriate for the Regex Sectionizer
    *
    * @throws ResourceInitializationException -
    */
   abstract protected void loadSections() throws ResourceInitializationException;

   /**
    * find all section separator header tags
    *
    * @param docText -
    * @return section tags mapped to index pairs
    */
   static private Map<Pair<Integer>, SectionTag> findHeaderTags( final String docText ) {
      final Map<Pair<Integer>, SectionTag> headerTags = new HashMap<>();
      for ( SectionType sectionType : _sectionTypes.values() ) {
         if ( sectionType.__headerPattern == null ) {
            continue;
         }
         headerTags
               .putAll( findSectionTags( docText, sectionType.__name, sectionType.__headerPattern, TagType.HEADER ) );
      }
      return headerTags;
   }

   /**
    * find all section separator footer tags
    *
    * @param docText -
    * @return section tags mapped to index pairs
    */
   static private Map<Pair<Integer>, SectionTag> findFooterTags( final String docText ) {
      final Map<Pair<Integer>, SectionTag> footerTags = new HashMap<>();
      for ( SectionType sectionType : _sectionTypes.values() ) {
         if ( sectionType.__footerPattern == null ) {
            continue;
         }
         footerTags
               .putAll( findSectionTags( docText, sectionType.__name, sectionType.__footerPattern, TagType.FOOTER ) );
      }
      return footerTags;
   }

   /**
    * @param docText    -
    * @param typeName   section type name
    * @param tagPattern regex pattern for section type
    * @param tagType    header or footer
    * @return section tags mapped to index pairs
    */
   static Map<Pair<Integer>, SectionTag> findSectionTags( final String docText,
                                                          final String typeName,
                                                          final Pattern tagPattern,
                                                          final TagType tagType ) {
      final Map<Pair<Integer>, SectionTag> sectionTags = new HashMap<>();
      try ( TimeoutMatcher finder = new TimeoutMatcher( tagPattern, docText ) ) {
         Matcher tagMatcher = finder.nextMatch();
         while ( tagMatcher != null ) {
            String name;
            // the start tag of this tag is the start of the current match
            // the end tag of this tag is the end of the current match, exclusive
            final Pair<Integer> tagBounds = new Pair<>( tagMatcher.start(), tagMatcher.end() );
            try {
               name = tagMatcher.group( SECTION_NAME_EX );
               if ( name == null || name.isEmpty() ) {
                  name = typeName;
               }
            } catch ( IllegalArgumentException iaE ) {
               name = typeName;
            }
            sectionTags.put( tagBounds, new SectionTag( name, typeName, tagType ) );
            tagMatcher = finder.nextMatch();
         }
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.error( iaE.getMessage() );
      }
      return sectionTags;
   }

   /**
    * All tags are treated equally as segment bounds, whether header or footer
    *
    * @param jcas       -
    * @param headerTags segment names are assigned based upon preceding headers
    * @param footerTags footers reset segment names to {@link #DEFAULT_SEGMENT_ID}
    * @param dividerLines divider lines reset segment names to {@link #DEFAULT_SEGMENT_ID}
    */
   static private void createSegments( final JCas jcas,
                                       final Map<Pair<Integer>, SectionTag> headerTags,
                                       final Map<Pair<Integer>, SectionTag> footerTags,
                                       final Map<Pair<Integer>, SectionTag> dividerLines ) {
      final String docText = jcas.getDocumentText();
      final Map<Pair<Integer>, SectionTag> sectionTags = new HashMap<>( headerTags.size() + footerTags.size() );
      sectionTags.putAll( headerTags );
      sectionTags.putAll( footerTags );
      sectionTags.putAll( dividerLines );
      if ( sectionTags.isEmpty() ) {
         // whole text is simple segment
         final Segment docSegment = new Segment( jcas, 0, docText.length() );
         docSegment.setId( DEFAULT_SEGMENT_ID );
         docSegment.setPreferredText( DEFAULT_SEGMENT_ID );
         docSegment.addToIndexes();
         return;
      }
//      final List<Pair<Integer>> boundsList = createBoundsList( sectionTags.keySet() );
      final List<Pair<Integer>> boundsList = sortAndTrimBounds( sectionTags.keySet() );
      Pair<Integer> leftBounds = boundsList.get( 0 );
      int sectionEnd;
      if ( leftBounds.getValue1() > 0 ) {
         // Add unspecified generic first segment
         sectionEnd = leftBounds.getValue1();
         if ( !docText.substring( 0, sectionEnd ).trim().isEmpty() ) {
            final Segment simpleSegment = new Segment( jcas, 0, sectionEnd );
            simpleSegment.setId( DEFAULT_SEGMENT_ID );
            simpleSegment.setPreferredText( DEFAULT_SEGMENT_ID );
            simpleSegment.addToIndexes();
            // will start the next segment with bounds at 0
         }
      }
      final int length = boundsList.size();
      // add segments 1 -> n
      for ( int i = 0; i < length; i++ ) {
         leftBounds = boundsList.get( i );
         int sectionBegin = leftBounds.getValue2();
         if ( i + 1 < length ) {
            sectionEnd = boundsList.get( i + 1 ).getValue1();
         } else {
            // the last segment
            sectionEnd = docText.length();
         }
         if ( sectionEnd > sectionBegin && !docText.substring( sectionBegin, sectionEnd ).trim().isEmpty() ) {
            while ( Character.isWhitespace( docText.charAt( sectionBegin ) ) ) {
               sectionBegin++;
            }
         }
         final SectionTag leftTag = sectionTags.get( leftBounds );
         final Segment segment = new Segment( jcas, sectionBegin, sectionEnd );
         if ( leftTag.__tagType == TagType.HEADER ) {
            // this tag is for a header, so the following segment has a defined name
            segment.setId( leftTag.__typeName );
            segment.setPreferredText( leftTag.__name );
            segment.setTagText( jcas.getDocumentText().substring( leftBounds.getValue1(), sectionBegin ) );
         } else {
            // this tag is for a footer or divider line, so the following segment is generic
            segment.setId( DEFAULT_SEGMENT_ID );
            segment.setPreferredText( DEFAULT_SEGMENT_ID );
         }
         segment.addToIndexes();
      }
   }

   // Todo make TextSpanUtil
   static private List<Pair<Integer>> createBoundsList( final Collection<Pair<Integer>> bounds ) {
      final List<Pair<Integer>> boundsList = new ArrayList<>( bounds );
      boundsList.sort( ( p1, p2 ) -> p1.getValue1() - p2.getValue2() );
      final Collection<Pair<Integer>> removalBounds = new HashSet<>();
      for ( int i = 0; i < boundsList.size() - 1; i++ ) {
         final Pair<Integer> pairI = boundsList.get( i );
         for ( int j = i + 1; j < boundsList.size(); j++ ) {
            final Pair<Integer> pairJ = boundsList.get( j );
            if ( pairJ.getValue1() >= pairI.getValue2() ) {
               break;
            }
            if ( pairI.getValue2() >= pairJ.getValue2() ) {
               removalBounds.add( pairJ );
               break;
            } else if ( pairI.getValue1() >= pairJ.getValue1() && pairJ.getValue2() > pairI.getValue2() ) {
               removalBounds.add( pairI );
               break;
            }
         }
      }
      boundsList.removeAll( removalBounds );
      return boundsList;
   }


   /**
    * @param text -
    * @return true if the text to lower case is "true" or "false"
    */
   static protected boolean isBoolean( final String text ) {
      final String text2 = text.trim().toLowerCase();
      return text2.equalsIgnoreCase( "true" ) || text2.equalsIgnoreCase( "false" );
   }


   /**
    * Find line dividers
    *
    * @param docText -
    * @return section tags mapped to index pairs
    */
   static private Map<Pair<Integer>, SectionTag> findDividerLines( final String docText ) {
      final Function<Pair<Integer>, SectionTag> lineDividerTag = p -> LINE_DIVIDER_TAG;
      try ( RegexSpanFinder finder = new RegexSpanFinder( DIVIDER_LINE_PATTERN ) ) {
         return finder.findSpans( docText ).stream().collect( Collectors.toMap( Function.identity(), lineDividerTag ) );
      } catch ( IllegalArgumentException iaE ) {
         return Collections.emptyMap();
      }
   }


   /**
    * Sorts by first offset, longer bounds first:
    *   |=============|
    *      |========|
    *      |====|
    *        |==============|
    *                |==============|
    *                     |========|
    */
   static private final class PairIntSorter implements Comparator<Pair<Integer>> {
      public int compare( final Pair<Integer> p1, final Pair<Integer> p2 ) {
         final int start = p1.getValue1() - p2.getValue1();
         if ( start != 0 ) {
            return start;
         }
         return p2.getValue2() - p1.getValue2();
      }
   }


}
