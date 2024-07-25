package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2016
 */
@PipeBitInfo(
      name = "Paragraph Annotator",
      description = "Annotates Paragraphs by detecting them using Regular Expressions provided in an input File or by empty text lines.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.PARAGRAPH }
)
final public class ParagraphAnnotator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LogManager.getLogger( "ParagraphAnnotator" );


   static public final String PARAGRAPH_TYPES_PATH = "PARAGRAPH_TYPES_PATH";
   static public final String PARAGRAPH_TYPES_DESC
         = "path to a file containing a list of regular expressions and corresponding paragraph types.";


   @ConfigurationParameter(
         name = PARAGRAPH_TYPES_PATH,
         description = PARAGRAPH_TYPES_DESC,
         mandatory = false
   )
   private String _paragraphTypesPath;

   // Allows spaces or tabs within the double-eol paragraph separator.
   static private final String DEFAULT_PARAGRAPH = "Default Paragraph||(?:(?:[\\t ]*\\r?\\n){2,})";

   /**
    * Holder for section type as defined in the user's specification bsv file
    */
   static private final class ParagraphType {
      private final String __name;
      private final Pattern __separatorPattern;

      private ParagraphType( final String name, final String separatorRegex ) {
         __name = name;
         __separatorPattern = separatorRegex == null ? null : Pattern.compile( separatorRegex, Pattern.MULTILINE );
      }
   }

   private final Collection<ParagraphType> _paragraphTypes = new HashSet<>();


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LOGGER.info( "Initializing ..." );
      super.initialize( context );
      if ( _paragraphTypesPath == null ) {
         LOGGER.info( "No " + PARAGRAPH_TYPES_DESC );
         LOGGER.info( "Using default paragraph separator: two newlines" );
         parseBsvLine( DEFAULT_PARAGRAPH );
         return;
      }
      LOGGER.info( "Parsing " + _paragraphTypesPath );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator
            .getAsStream( _paragraphTypesPath ) ) ) ) {
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
      LOGGER.info( "Annotating Paragraphs ..." );
      if ( _paragraphTypes.isEmpty() ) {
         LOGGER.info( "Finished processing, no paragraph types defined" );
         return;
      }
      createParagraphs( jcas );
//      LOGGER.info( "Finished processing" );
   }


   private Collection<Pair<Integer>> findSeparators( final String docText ) {
      final Collection<Pair<Integer>> separators = new HashSet<>();
      for ( ParagraphType paragraphType : _paragraphTypes ) {
         if ( paragraphType.__separatorPattern == null ) {
            continue;
         }
         separators.addAll( findSeparators( docText, paragraphType.__separatorPattern ) );
      }
      return separators;
   }

   // package protected for unit tests
   static Collection<Pair<Integer>> findSeparators( final String docText,
                                                    final Pattern pattern ) {
      // the start tag of this tag is the start of the current match
      // the end tag of this tag is the end of the current match, exclusive
      try ( RegexSpanFinder finder = new RegexSpanFinder( pattern ) ) {
         return finder.findSpans( docText );
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.error( iaE.getMessage() );
      }
      return Collections.emptyList();
   }


   /**
    * All tags are treated equally as segment bounds, whether header or footer
    *
    * @param jcas -
    */
   private void createParagraphs( final JCas jcas ) {
      final Collection<Segment> sections = JCasUtil.select( jcas, Segment.class );
      for ( Segment section : sections ) {
         final int offset = section.getBegin();
         final String text = section.getCoveredText();
         final Collection<Pair<Integer>> separators = findSeparators( text );
         if ( separators.isEmpty() ) {
            // whole text is simple paragraph
            final Paragraph paragraph = new Paragraph( jcas, offset, section.getEnd() );
            paragraph.addToIndexes();
            continue;
         }
         final List<Pair<Integer>> boundsList = new ArrayList<>( separators );
         Collections.sort( boundsList, ( p1, p2 ) -> p1.getValue1() - p2.getValue2() );
         Pair<Integer> leftBounds = boundsList.get( 0 );
         int paragraphEnd;
         if ( leftBounds.getValue1() > 0 ) {
            // Add unspecified generic first paragraph
            paragraphEnd = leftBounds.getValue1();
            if ( offset < 0 || offset + paragraphEnd < 0 ) {
               LOGGER.error( "First Paragraph out of bounds " + offset + "," + (offset + paragraphEnd) );
            } else {
               final Paragraph paragraph = new Paragraph( jcas, offset, offset + paragraphEnd );
               paragraph.addToIndexes();
            }
            // will start the next paragraph with bounds at 0
         }
         final int length = boundsList.size();
         // add segments 1 -> n
         for ( int i = 0; i < length; i++ ) {
            leftBounds = boundsList.get( i );
            final int paragraphBegin = leftBounds.getValue1();
            if ( i + 1 < length ) {
               paragraphEnd = boundsList.get( i + 1 ).getValue1();
            } else {
               // the last paragraph
               paragraphEnd = text.length();
            }
            if ( offset + paragraphBegin < 0 || offset + paragraphEnd < 0 ) {
               LOGGER.error( "Paragraph out of bounds " + (offset + paragraphBegin) + "," + (offset + paragraphEnd) );
            } else {
               final Paragraph paragraph = new Paragraph( jcas, offset + paragraphBegin, offset + paragraphEnd );
               paragraph.addToIndexes();
            }
         }
      }
   }


   private void parseBsvLine( final String line ) {
      if ( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "//" ) ) {
         // comment
         return;
      }
      final String[] splits = line.split( "\\|\\|" );
      if ( splits.length < 2 ) {
         LOGGER.warn( "Bad Paragraph definition: " + line + " ; please use the following:\n" +
                      "NAME||SEPARATOR_REGEX" );
         return;
      }
      // paragraph Name is always first
      final String name = splits[ 0 ].trim();
      // separator regex
      String separatorRegex = splits[ 1 ].trim();
      final ParagraphType paragraphType = new ParagraphType( name, separatorRegex );
      _paragraphTypes.add( paragraphType );
   }

}
