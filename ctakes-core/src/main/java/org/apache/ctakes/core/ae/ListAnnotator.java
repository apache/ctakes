package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/26/2016
 */
@PipeBitInfo(
      name = "List Annotator",
      description = "Annotates formatted List Sections by detecting them using Regular Expressions provided in an input File.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.LIST }
)
final public class ListAnnotator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ListAnnotator" );


   static public final String LIST_TYPES_PATH = "LIST_TYPES_PATH";
   static private final String LIST_TYPES_DESC
         = "path to a file containing a list of regular expressions and corresponding list types.";

   /**
    * classic ctakes default segment id
    */
   static private final String DEFAULT_LIST_ID = "SIMPLE_LIST";

   @ConfigurationParameter(
         name = LIST_TYPES_PATH,
         description = LIST_TYPES_DESC,
         defaultValue = "org/apache/ctakes/core/list/DefaultListRegex.bsv"
   )
   private String _listTypesPath;


   /**
    * Holder for list type as defined in the user's specification bsv file
    */
   static private final class ListType {
      private final String __name;
      private final Pattern __listPattern;
      private final Pattern __entrySeparator;

      private ListType( final String name, final String listRegex, final String entrySplitRegex ) {
         __name = name;
         __listPattern = listRegex == null ? null
                                           : Pattern.compile( listRegex, Pattern.MULTILINE );
         __entrySeparator = entrySplitRegex == null ? null
                                                    : Pattern.compile( entrySplitRegex, Pattern.MULTILINE );
      }
   }

   private final Collection<ListType> _listTypes = new HashSet<>();

//   private final ExecutorService _executor = Executors.newSingleThreadExecutor();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing ..." );
      super.initialize( context );
      if ( _listTypesPath == null ) {
         LOGGER.error( "No " + LIST_TYPES_DESC );
         return;
      }
      LOGGER.info( "Parsing " + _listTypesPath );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator
            .getAsStream( _listTypesPath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            parseBsvLine( line );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
//      LOGGER.info( "Finished Parsing" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Annotating Lists ..." );
      if ( _listTypes.isEmpty() ) {
         LOGGER.info( "Finished processing, no list types defined" );
         return;
      }
      final Collection<Paragraph> paragraphs = JCasUtil.select( jcas, Paragraph.class );
      if ( paragraphs != null && !paragraphs.isEmpty() ) {
         for ( Paragraph paragraph : paragraphs ) {
            try {
               final Map<Pair<Integer>, ListType> listTypes = findListTypes( paragraph.getCoveredText() );
               final Map<Pair<Integer>, ListType> uniqueListTypes = getUniqueListTypes( listTypes );
               createLists( jcas, uniqueListTypes, paragraph.getCoveredText(), paragraph.getBegin() );
            } catch ( StringIndexOutOfBoundsException oobE ) {
               // I'm not sure how this ever happens.  Paragraph bounds from the ParagraphAnnotator are always valid.
               // I have run ~1000 notes without problem, but one note in Seer causes problems.  Ignore.
            }
         }
      } else {
         for ( Segment section : JCasUtil.select( jcas, Segment.class ) ) {
            final Map<Pair<Integer>, ListType> listTypes = findListTypes( section.getCoveredText() );
            final Map<Pair<Integer>, ListType> uniqueListTypes = getUniqueListTypes( listTypes );
            createLists( jcas, uniqueListTypes, section.getCoveredText(), section.getBegin() );
         }
      }
//      LOGGER.info( "Finished processing" );
   }


   private Map<Pair<Integer>, ListType> findListTypes( final String text ) {
      final Map<Pair<Integer>, ListType> listTypes = new HashMap<>();
      for ( ListType listType : _listTypes ) {
         if ( listType.__listPattern == null ) {
            continue;
         }
         try ( RegexSpanFinder finder = new RegexSpanFinder( listType.__listPattern ) ) {
            final List<Pair<Integer>> spans = finder.findSpans( text );
            spans.forEach( s -> listTypes.put( s, listType ) );
         }
      }
      return listTypes;
   }


   /**
    * Get rid of list overlaps
    *
    * @param listTypes -
    * @return list types that don't overlap
    */
   static private Map<Pair<Integer>, ListType> getUniqueListTypes( final Map<Pair<Integer>, ListType> listTypes ) {
      if ( listTypes == null || listTypes.size() <= 1 ) {
         return listTypes;
      }
      final Collection<Pair<Integer>> removalTypeBounds = new HashSet<>();
      final Map<Pair<Integer>, Pair<Integer>> newTypeBounds = new HashMap<>();
      while ( true ) {
         final List<Pair<Integer>> sortedBounds = listTypes.keySet().stream()
               .sorted( ( p1, p2 ) -> (p2.getValue2() - p2.getValue1()) - (p1.getValue2() - p1.getValue1()) )
               .collect( Collectors.toList() );
         for ( int i = 0; i < sortedBounds.size() - 1; i++ ) {
            final Pair<Integer> boundsI = sortedBounds.get( i );
            // boundsI is larger than boundsJ, therefore dominant
            for ( int j = i + 1; j < sortedBounds.size(); j++ ) {
               final Pair<Integer> boundsJ = sortedBounds.get( j );
               if ( boundsJ.getValue1() >= boundsI.getValue1() && boundsJ.getValue1() <= boundsI.getValue2() ) {
                  removalTypeBounds.add( boundsJ );
                  if ( boundsJ.getValue2() > boundsI.getValue2() ) {
                     // Add J as a second list
                     newTypeBounds.put( new Pair<>( boundsI.getValue2(), boundsJ.getValue2() ), boundsJ );
                  }
               } else if ( boundsJ.getValue2() >= boundsI.getValue1() && boundsJ.getValue2() <= boundsI.getValue2() ) {
                  removalTypeBounds.add( boundsJ );
                  if ( boundsJ.getValue1() < boundsI.getValue1() ) {
                     // Add J as a second list
                     newTypeBounds.put( new Pair<>( boundsJ.getValue1(), boundsI.getValue1() ), boundsJ );
                  }
               }
            }
         }
         if ( removalTypeBounds.isEmpty() ) {
            return listTypes;
         }
         for ( Map.Entry<Pair<Integer>, Pair<Integer>> pairEntry : newTypeBounds.entrySet() ) {
            listTypes.put( pairEntry.getKey(), listTypes.get( pairEntry.getValue() ) );
         }
         removalTypeBounds.addAll( newTypeBounds.values() );
         listTypes.keySet().removeAll( removalTypeBounds );
         if ( listTypes.size() == 1 ) {
            return listTypes;
         }
         newTypeBounds.clear();
         removalTypeBounds.clear();
      }
   }


   static private Collection<Pair<Integer>> findEntrySeparators( final String listText, final Pattern entrySeparator ) {
      final Collection<Pair<Integer>> separators = new HashSet<>();
      final Matcher tagMatcher = entrySeparator.matcher( listText );
      while ( tagMatcher.find() ) {
         // the start tag of this tag is the start of the current match
         // the end tag of this tag is the end of the current match, exclusive
         final Pair<Integer> tagBounds = new Pair<>( tagMatcher.start(), tagMatcher.end() );
         separators.add( tagBounds );
      }
      return separators;
   }


   static private Collection<ListEntry> findListEntries( final JCas jCas, final Pair<Integer> listBounds,
                                                         final String listText,
                                                         final int offset, final Pattern entrySeparator ) {
      final Collection<Pair<Integer>> separators = findEntrySeparators( listText, entrySeparator );
      final int listBegin = listBounds.getValue1();
      final int listEnd = listBounds.getValue2();
      if ( separators.isEmpty() ) {
         // whole text is simple entry
         final ListEntry listEntry = new ListEntry( jCas,
               offset + listBounds.getValue1(), offset + listBounds.getValue2() );
         listEntry.addToIndexes();
         LOGGER.warn( "One List Entry for " + listText );
         return Collections.singletonList( listEntry );
      }
      final Collection<ListEntry> listEntries = new ArrayList<>( separators.size() + 1 );
      final List<Pair<Integer>> boundsList = new ArrayList<>( separators );
      boundsList.sort( ( p1, p2 ) -> p1.getValue1() - p2.getValue2() );
      Pair<Integer> leftBounds;
      int previousEntryEnd = listBegin;
      final int length = boundsList.size();
      // add entries 1 -> n
      for ( int i = 0; i < length; i++ ) {
         leftBounds = boundsList.get( i );
         final int entryBegin = previousEntryEnd;
         final int entryEnd = listBegin + leftBounds.getValue2();
         if ( entryEnd - entryBegin <= 0 ) {
            continue;
         }
         final ListEntry listEntry = new ListEntry( jCas, offset + entryBegin, offset + entryEnd );
         listEntry.addToIndexes();
         listEntries.add( listEntry );
         previousEntryEnd = entryEnd;
      }
      if ( previousEntryEnd < listEnd ) {
         // add an entry for the end of the list
         final ListEntry listEntry = new ListEntry( jCas, offset + previousEntryEnd, offset + listEnd );
         listEntry.addToIndexes();
         listEntries.add( listEntry );
      }
      return listEntries;
   }


   /**
    * All tags are treated equally as segment bounds, whether header or footer
    *
    * @param jcas      -
    * @param listTypes segment names are assigned based upon preceding headers
    * @param text      -
    * @param offset    offset of the given text within the document
    */
   static private void createLists( final JCas jcas,
                                    final Map<Pair<Integer>, ListType> listTypes, final String text,
                                    final int offset ) {
      if ( listTypes == null || listTypes.isEmpty() ) {
         return;
      }
      for ( Map.Entry<Pair<Integer>, ListType> boundedListType : listTypes.entrySet() ) {
         final Pair<Integer> listBounds = boundedListType.getKey();
         final ListType listType = boundedListType.getValue();
         final Collection<ListEntry> listEntries = findListEntries( jcas, listBounds,
               text.substring( listBounds.getValue1(), listBounds.getValue2() ), offset, listType.__entrySeparator );
         final FSList fsList = FSCollectionFactory.createFSList( jcas, listEntries );
         fsList.addToIndexes();
         final org.apache.ctakes.typesystem.type.textspan.List list
               = new org.apache.ctakes.typesystem.type.textspan.List( jcas,
               offset + listBounds.getValue1(), offset + listBounds.getValue2() );
         list.setId( listType.__name );
         list.setItems( fsList );
         list.addToIndexes();
      }
   }


   private void parseBsvLine( final String line ) {
      if ( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "//" ) ) {
         // comment
         return;
      }
      final String[] splits = line.split( "\\|\\|" );
      if ( splits.length < 3 || isBoolean( splits[ 1 ] ) ) {
         LOGGER.warn( "Bad List definition: " + line + " ; please use one of the following:\n" +
                      "NAME||LIST_REGEX||ENTRY_SEPARATOR_REGEX" );
         return;
      }
      // Section Name is always first
      final String name = splits[ 0 ].trim();
      final String listRegex = splits[ 1 ].trim();
      final String separatorRegex = splits[ 2 ].trim();
      final ListType listType = new ListType( name, listRegex, separatorRegex );
      _listTypes.add( listType );
   }

   static private boolean isBoolean( final String text ) {
      final String text2 = text.trim().toLowerCase();
      return text2.equalsIgnoreCase( "true" ) || text2.equalsIgnoreCase( "false" );
   }


   static public AnalysisEngineDescription createEngineDescription( final String sectionTypesPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ListAnnotator.class,
            LIST_TYPES_PATH, sectionTypesPath );
   }


}
