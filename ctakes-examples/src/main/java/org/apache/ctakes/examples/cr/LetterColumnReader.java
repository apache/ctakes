package org.apache.ctakes.examples.cr;

import org.apache.ctakes.core.cr.AbstractFileTreeReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.ProgressManager;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.ctakes.core.util.doc.TextBySectionBuilder;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@PipeBitInfo(
      name = "LetterColumnReader",
      description = "Build Patient document text from columnar Letter text.",
      role = PipeBitInfo.Role.READER
)
public class LetterColumnReader extends AbstractFileTreeReader {

   static private final Logger LOGGER = LogManager.getLogger( "LetterColumnReader" );

   static private final Pattern LETTER_PATTERN = Pattern.compile( "\\bLetter [0-9]+\\|" );

   private String _fileId = "";
   private int _letterTotal = 0;
   private int _letterCount = 0;
   private final List<Letter> _fileLetters = new ArrayList<>();
   private int _fileLetterIndex = 0;

   private JCasBuilder _jCasBuilder = new JCasBuilder();


   /**
    * Gets the total number of documents that will be returned by this
    * collection reader.
    *
    * @return the number of documents in the collection.
    */
   @Override
   public int getNoteCount() {
      return _letterTotal;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() {
      if ( _fileLetterIndex < _fileLetters.size() ) {
         return true;
      }
      final boolean hasNext = getCurrentIndex() < getFiles().size();
      if ( !hasNext ) {
         ProgressManager.getInstance()
                        .updateProgress( _letterTotal );
      }
      return hasNext;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final JCas jcas ) throws IOException, CollectionException {
      if ( _fileLetterIndex < _fileLetters.size() ) {
         final Letter letter = _fileLetters.get( _fileLetterIndex );
         _fileLetterIndex++;
         _letterCount++;
         ProgressManager.getInstance()
                        .updateProgress( _letterCount );
         _jCasBuilder.setDocId( _fileId + "_" + letter._id )
                     .setDocTime( letter._date )
                     .rebuild( jcas );
         final TextBySectionBuilder builder = new TextBySectionBuilder();
         letter._sections
               .forEach( p -> builder.addSection( p.getValue1(), p.getValue2() ) );
         builder.populate( jcas );
         return;
      }
      final int currentFileIndex = getCurrentIndex();
      final File file = getFiles().get( currentFileIndex );
      setCurrentIndex( currentFileIndex + 1 );
      _fileId = createDocumentID( file, getValidExtensions() );
      readFile( jcas, file );
      getNext( jcas );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[]{
            new ProgressImpl( _letterCount, _letterTotal, Progress.ENTITIES )
      };
   }


   /**
    * Places Document Text (and other information) in JCas.
    *
    * @param jCas unpopulated jcas data container.
    * @param file file to be read.
    * @throws IOException should anything bad happen.
    */
   protected void readFile( JCas jCas, File file ) throws IOException {
      // Read the file, building a document only using lines preceded by "Text:"
      LOGGER.info( "Reading File " + file.getPath() );
      final String fileText = readByBuffer( file );
      _fileLetters.clear();
      _fileLetterIndex = 0;
      if ( !fileText.isEmpty() ) {
         _fileLetters.addAll( readLetters( fileText ) );
         _letterTotal += _fileLetters.size();
      }
      _jCasBuilder = getJCasBuilder( file ).setDocType( "Letter" )
                                           .nullDocText();
      ProgressManager.getInstance()
                     .updateProgress( _letterCount, _letterTotal );
      LOGGER.info( "Parsed " + _fileLetters.size() + " letters" );
   }


   /**
    * @param rawText complete raw text as read from file.
    * @return letters parsed from file text.
    * @throws IOException if things go wrong.
    */
   static private List<Letter> readLetters( final String rawText ) throws IOException {
      final List<Integer> letterStarts;
      try ( RegexSpanFinder finder = new RegexSpanFinder( LETTER_PATTERN ) ) {
         letterStarts = finder.findSpans( rawText )
                              .stream()
                              .map( Pair::getValue1 )
                              .collect( Collectors.toList() );
      } catch ( IllegalArgumentException iaE ) {
         throw new IOException( "Illegal Argument " + iaE.getMessage() );
      }
      if ( letterStarts.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Letter> letters = new ArrayList<>();
      Letter currentLetter = new Letter();
      for ( int i = 0; i < letterStarts.size() - 1; i++ ) {
         final String letterLine = rawText.substring( letterStarts.get( i ), letterStarts.get( i + 1 ) );
         final Letter newOrCurrent = handleLetterLine( currentLetter, letterLine );
         if ( !newOrCurrent._id.equals( currentLetter._id ) ) {
            if ( currentLetter.hasInfo() ) {
               letters.add( currentLetter );
            }
            currentLetter = newOrCurrent;
         }
      }
      final String lastLetterLine = rawText.substring( letterStarts.get( letterStarts.size() - 1 ) );
      final Letter newOrCurrent = handleLetterLine( currentLetter, lastLetterLine );
      if ( currentLetter.hasInfo() ) {
         letters.add( currentLetter );
      }
      if ( newOrCurrent.hasInfo() && !newOrCurrent._id.equals( currentLetter._id ) ) {
         letters.add( newOrCurrent );
      }
      return letters;
   }


   /**
    * @param letter the letter currently being populated with sections.
    * @param line   a block of text representing a letter line.
    * @return the letter provided with the text processed OR a new letter with the text processed.
    */
   static private Letter handleLetterLine( final Letter letter, final String line ) {
      final LineType lineType = letter.addLine( line );
      if ( lineType != LineType.NEXT_LETTER ) {
         return letter;
      }
      return handleLetterLine( new Letter(), line );
   }


   /**
    * Reads file using buffered input stream
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   private String readByBuffer( final File file ) throws IOException {
      final String encoding = getValidEncoding();
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( new FileInputStream( file ), buffer.length ) ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
            if ( encoding != null && !encoding.isEmpty() && !UNKNOWN.equals( encoding ) ) {
               sb.append( new String( buffer, 0, length, encoding ) );
            } else {
               sb.append( new String( buffer, 0, length ) );
            }
         }
      } catch ( FileNotFoundException fnfE ) {
         throw new IOException( fnfE );
      }
      return sb.toString();
   }


   private enum LineType {
      MALFORMED,
      EMPTY,
      SECTION,
      NEXT_LETTER
   }

   static private final class Letter {

      private String _id;
      private String _date;
      private final List<Pair<String>> _sections = new ArrayList<>();

      private LineType addLine( final String line ) {
         final String[] splits = StringUtil.fastSplit( line, '|' );
         if ( !isLineValid( splits ) ) {
            return LineType.MALFORMED;
         }
         if ( _id == null ) {
            _id = splits[ 0 ];
         } else if ( !_id.equals( splits[ 0 ] ) ) {
            return LineType.NEXT_LETTER;
         }
         if ( splits[ 3 ].isEmpty() ) {
            // There is no letter content.
            return LineType.EMPTY;
         }
         _sections.add( new Pair<>( splits[ 2 ], splits[ 3 ] ) );
         _date = splits[ 6 ];
         return LineType.SECTION;
      }

      private boolean hasInfo() {
         return _id != null && _date != null && !_sections.isEmpty();
      }

      static private boolean isLineValid( final String[] splits ) {
         if ( splits.length != 7 ) {
            LOGGER.debug( "Incorrect number of columns ... skipping." );
            return false;
         }
         if ( splits[ 0 ].trim()
                         .isEmpty() ) {
            LOGGER.debug( "No Letter Title ... skipping." );
            return false;
         }
         if ( splits[ 6 ].trim()
                         .isEmpty() ) {
            LOGGER.debug( "No Letter Date ... skipping." );
            return false;
         }
         return true;
      }

   }


}
