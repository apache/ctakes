package org.apache.ctakes.core.cc.pretty.plaintext;

import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.cc.pretty.cell.*;
import org.apache.ctakes.core.cc.pretty.row.DefaultItemRow;
import org.apache.ctakes.core.cc.pretty.row.ItemRow;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.textspan.DefaultTextSpan;
import org.apache.ctakes.core.util.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Writes Document text, pos, semantic types and cuis to file.  Each Sentence starts a new series of pretty text lines.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/24/2015
 */
final public class PrettyTextWriter {


   static private final Logger LOGGER = Logger.getLogger( "PrettyTextWriter" );
   static private final String FILE_EXTENSION = ".pretty.txt";

   private String _outputDirPath;

   /**
    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
    * @throws IllegalArgumentException if the provided path points to a File and not a Directory
    * @throws SecurityException        if the File System has issues
    */
   public void setOutputDirectory( final String outputDirectoryPath ) throws IllegalArgumentException,
                                                                             SecurityException {
      // If no outputDir is specified (null or empty) the current working directory will be used.  Else check path.
      if ( outputDirectoryPath == null || outputDirectoryPath.isEmpty() ) {
         _outputDirPath = "";
         LOGGER.debug( "No Output Directory Path specified, using current working directory "
                       + System.getProperty( "user.dir" ) );
         return;
      }
      final File outputDir = new File( outputDirectoryPath );
      if ( !outputDir.exists() ) {
         outputDir.mkdirs();
      }
      if ( !outputDir.isDirectory() ) {
         throw new IllegalArgumentException( outputDirectoryPath + " is not a valid directory path" );
      }
      _outputDirPath = outputDirectoryPath;
      LOGGER.debug( "Output Directory Path set to " + _outputDirPath );
   }

   /**
    * Process the jcas and write pretty sentences to file.  Filename is based upon the document id stored in the cas
    *
    * @param jcas ye olde ...
    */
   public void process( final JCas jcas ) {
      LOGGER.info( "Starting processing" );
      final String docId = DocIdUtil.getDocumentIdForFile( jcas );
      File outputFile;
      if ( _outputDirPath == null || _outputDirPath.isEmpty() ) {
         outputFile = new File( docId + FILE_EXTENSION );
      } else {
         outputFile = new File( _outputDirPath, docId + FILE_EXTENSION );
      }
      writeFile( jcas, outputFile.getPath() );
      LOGGER.info( "Finished processing" );
   }

   public void writeFile( final JCas jCas, final String outputFilePath ) {
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFilePath ) ) ) {
         final Collection<Sentence> sentences = JCasUtil.select( jCas, Sentence.class );
         for ( Sentence sentence : sentences ) {
            writeSentence( jCas, sentence, writer );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not not write pretty file " + outputFilePath );
         LOGGER.error( ioE.getMessage() );
      }
   }

   /**
    * Write a sentence from the document text
    *
    * @param jcas     ye olde ...
    * @param sentence annotation containing the sentence
    * @param writer   writer to which pretty text for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static public void writeSentence( final JCas jcas,
                                      final AnnotationFS sentence,
                                      final BufferedWriter writer ) throws IOException {
      // Create the base row
      final Map<TextSpan, ItemCell> baseItemMap = createBaseItemMap( jcas, sentence );
      // Create covering annotations (item cells that cover more than one base cell)
      final Map<Integer, Collection<ItemCell>> coveringItemMap
            = createCoveringItemMap( jcas, sentence, baseItemMap );
      // Create annotation rows with shorter spans on top
      final Collection<ItemRow> itemRows = new ArrayList<>();
      final ItemRow baseItemRow = new DefaultItemRow();
      baseItemMap.values().forEach( baseItemRow::addItemCell );
      itemRows.add( baseItemRow );
      itemRows.addAll( createItemRows( coveringItemMap ) );
      // Create map of all text span offsets to adjusted offsets
      final Map<Integer, Integer> offsetAdjustedMap = createOffsetAdjustedMap( itemRows );
      // print all of the item rows
      printItemRows( offsetAdjustedMap, itemRows, writer );

      printTLinks( jcas, sentence, writer );
      writer.newLine();
   }

   /**
    * @param jcas     ye olde ...
    * @param sentence annotation containing the sentence
    * @return map of text spans and item cells that represent those spans
    */
   static private Map<TextSpan, ItemCell> createBaseItemMap( final JCas jcas, final AnnotationFS sentence ) {
      final int sentenceBegin = sentence.getBegin();
      final Collection<BaseToken> baseTokens
            = org.apache.uima.fit.util.JCasUtil.selectCovered( jcas, BaseToken.class, sentence );
      final Map<TextSpan, ItemCell> baseItemMap = new HashMap<>();
      for ( BaseToken baseToken : baseTokens ) {
         final TextSpan textSpan = new DefaultTextSpan( baseToken, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         if ( baseToken instanceof NewlineToken ) {
            final ItemCell itemCell = new DefaultBaseItemCell( textSpan, " ", "" );
            baseItemMap.put( textSpan, itemCell );
            continue;
         }
         final String tokenText = baseToken.getCoveredText();
         final String tokenPos = getTokenPos( baseToken );
         final ItemCell itemCell = new DefaultBaseItemCell( textSpan, tokenText, tokenPos );
         baseItemMap.put( textSpan, itemCell );
      }
      return baseItemMap;
   }


   /**
    *
    * @param jcas ye olde ...
    * @param sentence annotation containing the sentence
    * @param baseItemMap map of text spans and item cells that represent those spans
    * @return map of number of spanned base items and collections of item cells spanning that number of base item cells
    */
   static private Map<Integer, Collection<ItemCell>> createCoveringItemMap( final JCas jcas,
                                                                            final AnnotationFS sentence,
                                                                            final Map<TextSpan, ItemCell> baseItemMap ) {
      final Collection<TextSpan> usedTextSpans = new HashSet<>();
      final Collection<ItemCell> requiredCells = new HashSet<>();
      final Collection<ItemCell> eventCells = new HashSet<>();

      final int sentenceBegin = sentence.getBegin();
      final Collection<IdentifiedAnnotation> identifiedAnnotations
            = JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, sentence );
      for ( IdentifiedAnnotation annotation : identifiedAnnotations ) {
         final TextSpan textSpan = new DefaultTextSpan( annotation, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         final Map<String, Collection<String>> semanticCuis = getSemanticCuis( annotation );
         if ( !semanticCuis.isEmpty() ) {
            final ItemCell itemCell = new DefaultUmlsItemCell( textSpan, annotation.getPolarity(), semanticCuis );
            requiredCells.add( itemCell );
            usedTextSpans.add( textSpan );
         } else if ( annotation instanceof TimeMention ) {
            requiredCells.add( new TimexCell( textSpan ) );
         } else if ( annotation instanceof EventMention ) {
            eventCells.add( new EventCell( textSpan, annotation.getPolarity() ) );
         }
      }
      final Map<Integer, Collection<ItemCell>> coveringAnnotationMap = new HashMap<>();
      for ( ItemCell itemCell : requiredCells ) {
         final Collection<ItemCell> coveredBaseItems = getCoveredBaseItems( itemCell.getTextSpan(), baseItemMap );
         coveringAnnotationMap.putIfAbsent( coveredBaseItems.size(), new HashSet<>() );
         coveringAnnotationMap.get( coveredBaseItems.size() ).add( itemCell );
      }
      for ( ItemCell itemCell : eventCells ) {
         if ( usedTextSpans.contains( itemCell.getTextSpan() ) ) {
            continue;
         }
         final Collection<ItemCell> coveredBaseItems = getCoveredBaseItems( itemCell.getTextSpan(), baseItemMap );
         coveringAnnotationMap.putIfAbsent( coveredBaseItems.size(), new HashSet<>() );
         coveringAnnotationMap.get( coveredBaseItems.size() ).add( itemCell );
      }
      return coveringAnnotationMap;
   }


   /**
    * @param itemRows item rows
    * @return map of original document offsets to adjusted printable offsets
    */
   static private Map<Integer, Integer> createOffsetAdjustedMap( final Iterable<ItemRow> itemRows ) {
      // Create list of all text span offsets.  Had to be here because BaseTokens did not contain all offsets.
      final Collection<Integer> offsets = new HashSet<>();
      for ( ItemRow itemRow : itemRows ) {
         final Collection<ItemCell> rowItemCells = itemRow.getItemCells();
         for ( ItemCell itemCell : rowItemCells ) {
            offsets.add( itemCell.getTextSpan().getBegin() );
            offsets.add( itemCell.getTextSpan().getEnd() );
         }
      }
      // Create map of all text span offsets to adjusted offsets
      final List<Integer> offsetList = new ArrayList<>( offsets );
      Collections.sort( offsetList );
      final Map<Integer, Integer> offsetAdjustedMap = new HashMap<>( offsetList.size() );
      for ( Integer offset : offsetList ) {
         offsetAdjustedMap.put( offset, offset );
      }
      for ( ItemRow itemRow : itemRows ) {
         final Collection<ItemCell> rowItemCells = itemRow.getItemCells();
         for ( ItemCell itemCell : rowItemCells ) {
            final TextSpan textSpan = itemCell.getTextSpan();
            final int needWidth = itemCell.getWidth();
            final int nowWidth = offsetAdjustedMap.get( textSpan.getEnd() ) -
                                 offsetAdjustedMap.get( textSpan.getBegin() );
            if ( needWidth > nowWidth ) {
               final int delta = needWidth - nowWidth;
               offsetList.stream()
                     .filter( o -> o >= textSpan.getEnd() )
                     .forEach( o -> offsetAdjustedMap.put( o, offsetAdjustedMap.get( o ) + delta ) );
//               for ( Integer originalOffset : offsetList ) {
//                  if ( originalOffset >= textSpan.getEnd() ) {
//                     final Integer oldAdjustedOffset = offsetAdjustedMap.get( originalOffset );
//                     offsetAdjustedMap.put( originalOffset, oldAdjustedOffset + delta );
//                  }
//               }
            }
         }
      }
      return offsetAdjustedMap;
   }

   /**
    * @param offsetAdjustedMap map of original document offsets to adjusted printable offsets
    * @param itemRows          item rows
    * @param writer            writer to which pretty text for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static private void printItemRows( final Map<Integer, Integer> offsetAdjustedMap,
                                      final Iterable<ItemRow> itemRows,
                                      final BufferedWriter writer ) throws IOException {
      int rowWidth = 0;
      for ( int adjustedOffset : offsetAdjustedMap.values() ) {
         rowWidth = Math.max( rowWidth, adjustedOffset );
      }
      // Write Sentence Rows
      boolean firstLine = true;
      for ( ItemRow itemRow : itemRows ) {
         final int rowHeight = itemRow.getHeight();
         for ( int i = 0; i < rowHeight; i++ ) {
            final String lineText = itemRow.getTextLine( i, rowWidth, offsetAdjustedMap );
            if ( !lineText.isEmpty() ) {
               if ( firstLine ) {
                  writer.write( "SENTENCE:  " + lineText );
                  firstLine = false;
               } else {
                  writer.write( "           " + lineText );

               }
               writer.newLine();
            }
         }
      }
   }


   /**
    * Print TLinks as "arg1 relationType arg2"
    * @param jcas ye olde ...
    * @param sentence annotation containing the sentence
    * @param writer writer to which pretty text for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static private void printTLinks( final JCas jcas,
                                    final AnnotationFS sentence,
                                    final BufferedWriter writer ) throws IOException {
      final Collection<TemporalTextRelation> tlinks = JCasUtil.select( jcas, TemporalTextRelation.class );
      if ( tlinks == null || tlinks.isEmpty() ) {
         return;
      }
      final Collection<TemporalTextRelation> sentenceTlinks = new ArrayList<>();
      final TextSpan sentenceTextSpan = new DefaultTextSpan( sentence.getBegin(), sentence.getEnd() );
      for ( TemporalTextRelation tlink : tlinks ) {
         final Annotation argument1 = tlink.getArg1().getArgument();
         final TextSpan argument1Span = new DefaultTextSpan( argument1, 0 );
         if ( sentenceTextSpan.overlaps( argument1Span ) ) {
            sentenceTlinks.add( tlink );
         }
      }
      if ( sentenceTlinks.isEmpty() ) {
         return;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( "TLINKS:    " );
      for ( TemporalTextRelation tlink : sentenceTlinks ) {
         sb.append( tlink.getArg1().getArgument().getCoveredText() ).append( " " );
         sb.append( tlink.getCategory() ).append( " " );
         sb.append( tlink.getArg2().getArgument().getCoveredText() ).append( " , " );
      }
      sb.setLength( sb.length() - 3 );
      writer.write( sb.toString() );
      writer.newLine();
   }


   /**
    * @param textSpan    text span of interest
    * @param baseItemMap map of text spans and item cells that represent those spans
    * @return item cells for covered base items
    */
   static private Collection<ItemCell> getCoveredBaseItems( final TextSpan textSpan,
                                                            final Map<TextSpan, ItemCell> baseItemMap ) {
      return baseItemMap.entrySet().stream()
            .filter( e -> e.getKey().overlaps( textSpan ) )
            .map( Map.Entry::getValue )
            .collect( Collectors.toList() );
   }


   /**
    * Create annotation rows with shorter spans on top
    *
    * @param coveringItemMap map of all item cells for the sentence,
    *                        key = number of tokens covered, value = item cells
    * @return list of item rows, each containing non-overlapping item cells
    */
   static private Collection<ItemRow> createItemRows( final Map<Integer, Collection<ItemCell>> coveringItemMap ) {
      final List<Integer> sortedCounts = new ArrayList<>( coveringItemMap.keySet() );
      Collections.sort( sortedCounts );
      final Collection<ItemRow> itemRows = new ArrayList<>();
      for ( Integer coveredCount : sortedCounts ) {
         final Collection<ItemCell> itemCells = coveringItemMap.get( coveredCount );
         for ( ItemCell itemCell : itemCells ) {
            boolean added = false;
            for ( ItemRow itemRow : itemRows ) {
               added = itemRow.addItemCell( itemCell );
               if ( added ) {
                  break;
               }
            }
            if ( !added ) {
               final ItemRow itemRow = new DefaultItemRow();
               itemRow.addItemCell( itemCell );
               itemRows.add( itemRow );
            }
         }
      }
      return itemRows;
   }

   /**
    * @param baseToken some token
    * @return a part of speech text representation if the basetoken is a word token, else ""
    */
   static private String getTokenPos( final BaseToken baseToken ) {
      if ( !(baseToken instanceof WordToken) ) {
         return "";
      }
      // We are only interested in tokens that are -words-
      final String tokenPos = baseToken.getPartOfSpeech();
      if ( tokenPos == null ) {
         return "";
      }
      return tokenPos;
   }


   /**
    * @param identifiedAnnotation an annotation of interest
    * @return map of semantic type names and cuis within those types as they apply to the annotation
    */
   static private Map<String, Collection<String>> getSemanticCuis( final IdentifiedAnnotation identifiedAnnotation ) {
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( identifiedAnnotation );
      if ( umlsConcepts == null || umlsConcepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String, Collection<String>> semanticCuis = new HashMap<>();
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         final String cui = trimTo8( umlsConcept.getCui() );
         String semanticName = SemanticGroup.getSemanticName( identifiedAnnotation, umlsConcept );
         semanticCuis.putIfAbsent( semanticName, new HashSet<>() );
         semanticCuis.get( semanticName ).add( cui );
      }
      return semanticCuis;
   }


   static private String trimTo8( final String text ) {
      if ( text.length() <= 8 ) {
         return text;
      }
      return "<" + text.substring( text.length() - 7, text.length() );
   }

}
