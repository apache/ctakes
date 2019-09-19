package org.apache.ctakes.core.cc.html;


import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.semantic.SemanticGroup;
import org.apache.ctakes.core.semantic.SemanticTui;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.core.util.textspan.DefaultTextSpan;
import org.apache.ctakes.core.util.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/8/2016
 */
@PipeBitInfo(
      name = "HTML Writer",
      description = "Writes html files with document text and simple markups (Semantic Group, CUI, Negation).",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, SENTENCE, BASE_TOKEN },
      usables = { DOCUMENT_ID_PREFIX, IDENTIFIED_ANNOTATION, EVENT, TIMEX, TEMPORAL_RELATION }
)
final public class HtmlTextWriter extends AbstractJCasFileWriter {

   // TODO https://www.w3schools.com/howto/howto_css_switch.asp
   // TODO https://www.w3schools.com/html/tryit.asp?filename=tryhtml_layout_flexbox
   // TODO https://www.w3schools.com/html/html5_new_elements.asp

// TODO https://css-tricks.com/snippets/css/a-guide-to-flexbox/
// TODO https://www.quackit.com/css/flexbox/tutorial/nested_flex_containers.cfm

   static final String TOOL_TIP = "TIP";

   static final String UNCERTAIN_NEGATED = "UNN_";
   static final String NEGATED = "NEG_";
   static final String UNCERTAIN = "UNC_";
   static final String AFFIRMED = "AFF_";
   static final String GENERIC = "GNR_";
   static final String SPACER = "SPC_";
   static final String NEWLINE = "NL_";
   static final String WIKI_BEGIN = "WIK_";
   static final String WIKI_CENTER = "_WK_";
   static final String WIKI_END = "_WIK";

   static private final Logger LOGGER = Logger.getLogger( "HtmlTextWriter" );

   static private final String PREFERRED_TERM_UNKNOWN = "Unknown Preferred Term";
   static private final String CTAKES_VERSION = "4.0.1";

   static private final String FILE_EXTENSION = ".pretty.html";
   static private final String CSS_FILENAME = "ctakes.pretty.css";
   static private final String JS_FILENAME = "ctakes.pretty.js";

   static private final Collection<String> _usedDirectories = ConcurrentHashMap.newKeySet();

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      synchronized (_usedDirectories) {
         if ( _usedDirectories.add( outputDir ) ) {
            final String cssPath = outputDir + '/' + CSS_FILENAME;
            CssWriter.writeCssFile( cssPath );
            final String jsPath = outputDir + '/' + JS_FILENAME;
            JsWriter.writeJsFile( jsPath );
         }
      }
      final File htmlFile = new File( outputDir, fileName + FILE_EXTENSION );
      LOGGER.info( "Writing HTML to " + htmlFile.getPath() + " ..." );
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( htmlFile ) ) ) {
         final String title = DocumentIDAnnotationUtil.getDocumentID( jCas );
         writer.write( startBody() );
         writer.write( getCssLink( CSS_FILENAME ) );
         writer.write( getJsLink( JS_FILENAME ) );
         writer.write( startContainer() );
         writer.write( getHeader( title ) );
         writer.write( getNav() );
         writer.write( startArticle() );

         final Collection<Segment> sections = JCasUtil.select( jCas, Segment.class );
         final Map<Segment, Collection<org.apache.ctakes.typesystem.type.textspan.List>> lists
               = JCasUtil.indexCovered( jCas, Segment.class, org.apache.ctakes.typesystem.type.textspan.List.class );
         final Map<org.apache.ctakes.typesystem.type.textspan.List, Collection<ListEntry>> listEntries
               = JCasUtil.indexCovered( jCas, org.apache.ctakes.typesystem.type.textspan.List.class, ListEntry.class );
         final Map<Segment, Collection<Sentence>> sectionSentences
               = JCasUtil.indexCovered( jCas, Segment.class, Sentence.class );
         final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations
               = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
         final Map<Sentence, Collection<BaseToken>> sentenceTokens
               = JCasUtil.indexCovered( jCas, Sentence.class, BaseToken.class );
         final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
         // TODO at each paragraph end index add a newline unless it is the end of a section
         final Collection<Paragraph> paragraphs = JCasUtil.select( jCas, Paragraph.class );
         cullAnnotations( sentenceAnnotations.values() );

         final Collection<CollectionTextRelation> corefRelations = JCasUtil.select( jCas,
               CollectionTextRelation.class );
         final Map<Markable, TextSpan> markableSpans = mapMarkableSpans( jCas, corefRelations );
         final Map<TextSpan, Collection<Integer>> corefSpans = createCorefSpans( corefRelations, markableSpans );

         writeSections( sections, paragraphs, lists, listEntries, sectionSentences, sentenceAnnotations, sentenceTokens,
               relations, corefSpans, writer );
         writer.write( endArticle() );

         writer.write( getFooter() );
         writer.write( endContainer() );

         writer.write( startJavascript() );
         if ( !corefRelations.isEmpty() ) {
            writeCorefInfos( corefRelations, writer );
         }
         writer.write( endJavascript() );

         writer.write( endBody() );
      }
      LOGGER.info( "Finished Writing" );
   }

   static private void cullAnnotations( final Collection<Collection<IdentifiedAnnotation>> sentenceAnnotations ) {
      final java.util.function.Predicate<IdentifiedAnnotation> keep = a -> EventMention.class.isInstance( a )
            || TimeMention.class.isInstance( a ) || EntityMention.class.isInstance( a );
      final Collection<IdentifiedAnnotation> keepers = new HashSet<>();
      for ( Collection<IdentifiedAnnotation> annotations : sentenceAnnotations ) {
         annotations.stream()
               .filter( keep )
               .forEach( keepers::add );
         annotations.retainAll( keepers );
         keepers.clear();
      }
   }

   /**
    * @param corefRelations coreference chains
    * @return a map of markable text span ends to chain numbers
    */
   static private Map<TextSpan, Collection<Integer>> createCorefSpans( final Collection<CollectionTextRelation> corefRelations,
                                                                       final Map<Markable, TextSpan> markableSpans ) {
      if ( corefRelations == null || corefRelations.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<TextSpan, Collection<Integer>> corefSpans = new HashMap<>();
      int index = 1;
      for ( CollectionTextRelation corefRelation : corefRelations ) {
         final FSList chainHead = corefRelation.getMembers();
         final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
         for ( Markable markable : markables ) {
            final TextSpan span = markableSpans.get( markable );
            corefSpans.putIfAbsent( span, new ArrayList<>() );
            corefSpans.get( span )
                  .add( index );
         }
         index++;
      }
      return corefSpans;
   }

   /**
    * This is a bit messy, but necessary.
    *
    * @param jCas           -
    * @param corefRelations -
    * @return map of markable to identified annotation
    */
   static private Map<Markable, TextSpan> mapMarkableSpans( final JCas jCas,
                                                            final Collection<CollectionTextRelation> corefRelations ) {
      if ( corefRelations == null || corefRelations.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<Markable, Collection<ConllDependencyNode>> markableNodes = JCasUtil.indexCovered( jCas, Markable.class,
            ConllDependencyNode.class );
      final Map<ConllDependencyNode, Collection<IdentifiedAnnotation>> nodeAnnotations
            = JCasUtil.indexCovering( jCas, ConllDependencyNode.class, IdentifiedAnnotation.class );
      cullAnnotations( nodeAnnotations.values() );
      final Map<Markable, TextSpan> spanMap = new HashMap<>();
      for ( CollectionTextRelation coref : corefRelations ) {
         final Collection<Markable> markables = JCasUtil.select( coref.getMembers(), Markable.class );
         for ( Markable markable : markables ) {
            final Collection<ConllDependencyNode> nodes = markableNodes.get( markable );
            if ( nodes == null || nodes.isEmpty() ) {
               continue;
            }
            final ConllDependencyNode headNode = getNominalHeadNode( new ArrayList<>( nodes ) );
            final Collection<IdentifiedAnnotation> annotations = nodeAnnotations.get( headNode );
            if ( annotations == null || annotations.isEmpty() ) {
               spanMap.put( markable, new DefaultTextSpan( headNode.getBegin(), headNode.getEnd() ) );
               continue;
            }
            TextSpan bestSpan = null;
            int bestLength = 0;
            for ( IdentifiedAnnotation annotation : annotations ) {
               if ( !EventMention.class.equals( annotation.getClass() )
                     && annotation.getBegin() == markable.getBegin() && annotation.getEnd() == markable.getEnd() ) {
                  // Prefer an exact non-event match over the longest match
                  bestSpan = new DefaultTextSpan( annotation.getBegin(), annotation.getEnd() );
                  break;
               }
               if ( annotation.getEnd() - annotation.getBegin() > bestLength ) {
                  bestLength = annotation.getEnd() - annotation.getBegin();
                  bestSpan = new DefaultTextSpan( annotation.getBegin(), annotation.getEnd() );
               }
            }
            if ( bestSpan != null ) {
               spanMap.put( markable, bestSpan );
            } else {
               spanMap.put( markable, new DefaultTextSpan( headNode.getBegin(), headNode.getEnd() ) );
            }
         }
      }
      return spanMap;
   }

   /**
    * Finds the head node out of a few ConllDependencyNodes. Biased toward nouns.
    **/
   public static ConllDependencyNode getNominalHeadNode(
         List<ConllDependencyNode> nodes ) {
      ArrayList<ConllDependencyNode> anodes = new ArrayList<>( nodes );
      Boolean[][] matrixofheads = new Boolean[ anodes.size() ][ anodes.size() ];
      List<ConllDependencyNode> outnodes = new ArrayList<>();

      // Remove root from consideration
      for ( int i = 0; i < anodes.size(); i++ ) {
         if ( anodes.get( i )
               .getId() == 0 ) {
            anodes.remove( i );
         }
      }

      // Create a dependency matrix
      for ( int id1 = 0; id1 < anodes.size(); id1++ ) {
         for ( int id2 = 0; id2 < anodes.size(); id2++ ) {
            // no head-dependency relationship between id1 and id2
            if ( id1 == id2 || anodes.get( id1 )
                  .getId() != anodes.get( id2 )
                  .getHead()
                  .getId() ) {
               matrixofheads[ id2 ][ id1 ] = false;
            }
            // a match
            else {
               matrixofheads[ id2 ][ id1 ] = true;
            }
         }
      }

      // Search the dependency matrix for the head
      for ( int idhd = 0; idhd < anodes.size(); idhd++ ) {
         boolean occupiedCol = false;
         for ( int row = 0; row < anodes.size(); row++ ) {
            if ( matrixofheads[ row ][ idhd ] ) {
               occupiedCol = true;
            }
         }
         if ( occupiedCol ) {
            boolean occupiedRow = false;
            for ( int col = 0; col < anodes.size(); col++ ) {
               if ( matrixofheads[ idhd ][ col ] ) {
                  occupiedRow = true;
               }
            }
            if ( !occupiedRow ) {
               outnodes.add( anodes.get( idhd ) );
            }
         }
      }

      // Unheaded phrases
      if ( outnodes.isEmpty() ) {
         // pick a noun from the left, if there is one
         for ( int i = 0; i < anodes.size(); i++ ) {
            if ( Pattern.matches( "N..?", anodes.get( i )
                  .getPostag() ) ) {
               return anodes.get( i );
            }
         }
         // default to picking the rightmost node
         return anodes.get( anodes.size() - 1 );
      }
      // Headed phrases
      else {
         // pick a noun from the left, if there is one
         for ( int i = 0; i < outnodes.size(); i++ ) {
            if ( Pattern.matches( "N..?", outnodes.get( i )
                  .getPostag() ) ) {
               return outnodes.get( i );
            }
         }
         // otherwise, pick the rightmost node with dependencies
         return outnodes.get( outnodes.size() - 1 );
      }
   }

   // The assumption is that any given span can only have one exact EventMention.
   static private IdentifiedAnnotation getEvent( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
            .filter( a -> EventMention.class.equals( a.getClass() ) )
            .findAny()
            .orElse( null );
   }

   /**
    * @param annotationMap -
    * @return map of umls annotations to events
    */
   static private Map<IdentifiedAnnotation, IdentifiedAnnotation> getAnnotationEvents( final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap ) {
      final Map<IdentifiedAnnotation, IdentifiedAnnotation> annotationEvents = new HashMap<>();
      final Map<TextSpan, IdentifiedAnnotation> unusedEvents = new HashMap<>();
      for ( Map.Entry<TextSpan, Collection<IdentifiedAnnotation>> entry : annotationMap.entrySet() ) {
         final Collection<IdentifiedAnnotation> annotations = entry.getValue();
         final IdentifiedAnnotation event = getEvent( annotations );
         if ( event != null ) {
            if ( annotations.size() > 1 ) {
               final int pre = annotationEvents.size();
               annotations.stream()
                     .filter( EventMention.class::isInstance )
                     .filter( a -> !event.equals( a ) )
                     .forEach( a -> annotationEvents.put( a, event ) );
               if ( annotationEvents.size() > pre ) {
                  annotations.remove( event );
               } else {
                  unusedEvents.put( entry.getKey(), event );
               }
            } else {
               unusedEvents.put( entry.getKey(), event );
            }
         }
      }
      if ( unusedEvents.isEmpty() ) {
         return annotationEvents;
      }
      final Map<TextSpan, IdentifiedAnnotation> usedEvents = new HashMap<>();
      for ( Map.Entry<TextSpan, Collection<IdentifiedAnnotation>> entry : annotationMap.entrySet() ) {
         final TextSpan span = entry.getKey();
         TextSpan usedEventSpan = null;
         for ( Map.Entry<TextSpan, IdentifiedAnnotation> unusedEvent : unusedEvents.entrySet() ) {
            if ( !span.equals( unusedEvent.getKey() ) && span.contains( unusedEvent.getKey() ) ) {
               entry.getValue()
                     .stream()
                     .filter( EventMention.class::isInstance )
                     .forEach( a -> annotationEvents.put( a, unusedEvent.getValue() ) );
               usedEventSpan = unusedEvent.getKey();
               usedEvents.put( usedEventSpan, unusedEvent.getValue() );
               break;
            }
         }
         if ( usedEventSpan != null ) {
            unusedEvents.remove( usedEventSpan );
            if ( unusedEvents.isEmpty() ) {
               break;
            }
         }
      }
      usedEvents.forEach( ( s, e ) -> annotationMap.get( s )
            .remove( e ) );
      final Collection<TextSpan> emptySpans = annotationMap.entrySet()
            .stream()
            .filter( e -> e.getValue()
                  .isEmpty() )
            .map( Map.Entry::getKey )
            .collect( Collectors.toList() );
      annotationMap.keySet()
            .removeAll( emptySpans );
      return annotationEvents;
   }

   /**
    * @return html to start the body
    */
   static private String startBody() {
      return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<body>\n";
   }

   /**
    * @param filePath path to the css file
    * @return html to link to css
    */
   static private String getCssLink( final String filePath ) {
      return "<link rel=\"stylesheet\" href=\"" + filePath + "\" type=\"text/css\" media=\"screen\">";
   }

   /**
    * @param filePath path to the js file
    * @return html to link to js
    */
   static private String getJsLink( final String filePath ) {
      return "<script type=\"text/javascript\" src=\"ctakes.pretty.js\"></script>\n";
   }

   static private String startJavascript() {
      return "<script type=\"text/javascript\">";
   }

   static private String endJavascript() {
      return "</script>";
   }

   static private String startContainer() {
      return "<div class=\"flex-container\">\n";
   }

   static private String getHeader( final String title ) {
      return "<header>\n" +
            "  <h1>" + title + "</h1>\n" +
            "</header>\n";
   }

   /**
    * html for right-hand annotation information panel
    */
   static private String getNav() {
      return "<nav class=\"nav\">\n" +
            "    <div id=\"ia\">\n" +
            "      Annotation Information\n" +
            "    </div>\n" +
            getLegend() +
            "</nav>\n";
   }

   static private String startArticle() {
      return "<article class=\"article\">\n";
   }

   static private String endArticle() {
      return "</article>\n";
   }

   static private String getFooter() {
      final LocalDateTime time = LocalDateTime.now();
      final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "LL dd yyyy, HH:mm:ss" );
      return "<footer>\n" +
            "Processed by Apache cTAKES<sup>&copy;</sup> on " + formatter.format( time ) + "\n" +
            "</footer>\n";
   }

   static private String endContainer() {
      return "</div>\n";
   }

   /**
    * write html for all sections (all text) in the document
    *
    * @param sectionSentences    map of sections and their contained sentences
    * @param sentenceAnnotations map of sentences and their contained annotations
    * @param sentenceTokens      map of sentences and their contained base tokens
    * @param relations           all relations
    * @param corefSpans          map of text spans to coreference chain indices
    * @param writer              writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSections( final Map<Segment, Collection<Sentence>> sectionSentences,
                                      final Collection<Paragraph> paragraphs,
                                      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations,
                                      final Map<Sentence, Collection<BaseToken>> sentenceTokens,
                                      final Collection<BinaryTextRelation> relations,
                                      final Map<TextSpan, Collection<Integer>> corefSpans,
                                      final BufferedWriter writer ) throws IOException {
      final Collection<Integer> paragraphBegins = paragraphs.stream()
            .map( Annotation::getBegin )
            .collect( Collectors.toList() );
      final List<Segment> sections = new ArrayList<>( sectionSentences.keySet() );
      sections.sort( Comparator.comparingInt( Segment::getBegin ) );
      for ( Segment section : sections ) {
         writeSectionHeader( section, writer );
         writer.write( "\n<p>\n" );
         final List<Sentence> sentences = new ArrayList<>( sectionSentences.get( section ) );
         sentences.sort( Comparator.comparingInt( Sentence::getBegin ) );
         for ( Sentence sentence : sentences ) {
            final Collection<IdentifiedAnnotation> annotations = sentenceAnnotations.get( sentence );
            final Collection<BaseToken> tokens = sentenceTokens.get( sentence );
            writeSentence( sentence, annotations, tokens, relations, corefSpans, writer );
            if ( paragraphBegins.contains( sentence.getEnd() ) ) {
               writer.write( "\n</p>\n<p>\n" );
            }
         }
         writer.write( "\n</p>\n" );
      }
   }

   /**
    * write html for all sections (all text) in the document
    *
    * @param sectionSentences    map of sections and their contained sentences
    * @param sentenceAnnotations map of sentences and their contained annotations
    * @param sentenceTokens      map of sentences and their contained base tokens
    * @param relations           all relations
    * @param corefSpans          map of text span ends to coreference chain indices
    * @param writer              writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSections( final Collection<Segment> sectionSet,
                                      final Collection<Paragraph> paragraphs,
                                      final Map<Segment, Collection<org.apache.ctakes.typesystem.type.textspan.List>> lists,
                                      final Map<org.apache.ctakes.typesystem.type.textspan.List, Collection<ListEntry>> listEntries,
                                      final Map<Segment, Collection<Sentence>> sectionSentences,
                                      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations,
                                      final Map<Sentence, Collection<BaseToken>> sentenceTokens,
                                      final Collection<BinaryTextRelation> relations,
                                      final Map<TextSpan, Collection<Integer>> corefSpans,
                                      final BufferedWriter writer ) throws IOException {
      if ( lists.isEmpty() ) {
         writeSections( sectionSentences, paragraphs, sentenceAnnotations, sentenceTokens, relations, corefSpans,
               writer );
         return;
      }
      final Collection<Integer> paragraphBegins = paragraphs.stream()
            .map( Annotation::getBegin )
            .collect( Collectors.toList() );
      final List<Segment> sections = new ArrayList<>( sectionSet );
      sections.sort( Comparator.comparingInt( Segment::getBegin ) );
      final Map<Integer, Integer> enclosers = new HashMap<>();
      for ( Map.Entry<org.apache.ctakes.typesystem.type.textspan.List, Collection<ListEntry>> entry : listEntries.entrySet() ) {
         final int listEnd = entry.getKey()
               .getEnd();
         entry.getValue()
               .forEach( e -> enclosers.put( e.getBegin(), listEnd ) );
      }
      for ( Segment section : sections ) {
         writeSectionHeader( section, writer );
         final Collection<Sentence> sentenceSet = sectionSentences.get( section );
         if ( sentenceSet == null ) {
            continue;
         }
         writer.write( "\n<p>\n" );
         final List<Sentence> sentences = new ArrayList<>( sentenceSet );
         sentences.sort( Comparator.comparingInt( Sentence::getBegin ) );
         int currentEnd = -1;
         boolean freshEntry = false;
         for ( Sentence sentence : sentences ) {
            final Collection<IdentifiedAnnotation> annotations = sentenceAnnotations.get( sentence );
            final Collection<BaseToken> tokens = sentenceTokens.get( sentence );
            final Integer end = enclosers.get( sentence.getBegin() );
            if ( end != null ) {
               freshEntry = true;
               if ( currentEnd < 0 ) {
                  startList( sentence, annotations, tokens, relations, corefSpans, writer );
                  currentEnd = end;
               } else {
                  writeListEntry( sentence, annotations, tokens, relations, corefSpans, writer );
               }
            } else {
               if ( currentEnd >= 0 && sentence.getBegin() > currentEnd ) {
                  endList( sentence, annotations, tokens, relations, corefSpans, writer );
                  currentEnd = -1;
                  freshEntry = false;
                  continue;
               }
               if ( freshEntry ) {
                  freshEntry = false;
                  writer.write( "\n<br>\n" );
               }
               writeSentence( sentence, annotations, tokens, relations, corefSpans, writer );
               if ( paragraphBegins.contains( sentence.getEnd() ) ) {
                  writer.write( "\n</p>\n<p>\n" );
               }
            }
         }
         if ( currentEnd >= 0 ) {
            endList( writer );
         }
         writer.write( "\n</p>\n" );
      }
   }

   /**
    * @param sentence     sentence of interest
    * @param annotations  identified annotations in the section
    * @param baseTokenMap baseTokens in the section
    * @param relations    all relations
    * @param corefSpans   map of text span ends to coreference chain indices
    * @return marked up text
    */
   static private String createLineText( final Sentence sentence,
                                         final Collection<IdentifiedAnnotation> annotations,
                                         final Map<TextSpan, String> baseTokenMap,
                                         final Collection<BinaryTextRelation> relations,
                                         final Map<TextSpan, Collection<Integer>> corefSpans ) {
      final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap = createAnnotationMap( sentence,
            annotations );
      final Map<IdentifiedAnnotation, IdentifiedAnnotation> annotationEvents = getAnnotationEvents( annotationMap );
      final Map<Integer, String> tags = createTags( sentence, annotationMap, annotationEvents, relations, corefSpans );
      final StringBuilder sb = new StringBuilder();
      int previousIndex = -1;
      for ( Map.Entry<TextSpan, String> entry : baseTokenMap.entrySet() ) {
         final String text = entry.getValue();
         final int begin = entry.getKey()
               .getBegin();
         if ( begin != previousIndex ) {
            final String beginTag = tags.get( begin );
            if ( beginTag != null ) {
               sb.append( beginTag );
            }
         }
         sb.append( text );
         final int end = entry.getKey()
               .getEnd();
         final String endTag = tags.get( end );
         if ( endTag != null ) {
            sb.append( endTag );
         }
         sb.append( " " );
         previousIndex = end;
      }
      return sb.toString();
   }

   static private void startList( final Sentence sentence,
                                  final Collection<IdentifiedAnnotation> annotations,
                                  final Collection<BaseToken> baseTokens,
                                  final Collection<BinaryTextRelation> relations,
                                  final Map<TextSpan, Collection<Integer>> corefSpans,
                                  final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      writer.write( "\n<ul>\n<li>" );
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefSpans );
      writer.write( lineText );
   }

   /**
    * Write html for a sentence from the document text
    *
    * @param sentence    sentence of interest
    * @param annotations identified annotations in the section
    * @param baseTokens  baseTokens in the section
    * @param relations   all relations
    * @param writer      writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeListEntry( final Sentence sentence,
                                       final Collection<IdentifiedAnnotation> annotations,
                                       final Collection<BaseToken> baseTokens,
                                       final Collection<BinaryTextRelation> relations,
                                       final Map<TextSpan, Collection<Integer>> corefSpans,
                                       final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      writer.write( "</li>\n<li>" );
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefSpans );
      writer.write( lineText );
   }

   static private void endList( final Sentence sentence,
                                final Collection<IdentifiedAnnotation> annotations,
                                final Collection<BaseToken> baseTokens,
                                final Collection<BinaryTextRelation> relations,
                                final Map<TextSpan, Collection<Integer>> corefSpans,
                                final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefSpans );
      writer.write( lineText + "</li>\n</ul>\n" );
   }

   static private void endList( final BufferedWriter writer ) throws IOException {
      writer.write( "</li>\n</ul>\n" );
   }

   /**
    * write html for section header
    *
    * @param section -
    * @param writer  writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSectionHeader( final Segment section, final BufferedWriter writer ) throws IOException {
      String sectionId = section.getId();
      if ( sectionId == null || sectionId.equals( "SIMPLE_SEGMENT" ) ) {
         return;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n<h3" );
      final String sectionTag = getSafeText( section.getTagText() );
      if ( sectionTag != null && !sectionTag.trim()
            .isEmpty() ) {
         sb.append( " onClick=\"iaf(\'" )
               .append( sectionTag.trim() )
               .append( "')\"" );
      }
      sb.append( ">" )
            .append( getSafeText( sectionId ) );
      final String sectionName = section.getPreferredText();
      if ( sectionName != null && !sectionName.trim()
            .isEmpty() && !sectionName.trim()
            .equals( sectionId ) ) {
         sb.append( " : " )
               .append( getSafeText( sectionName ) );
      }
      sb.append( "</h3>\n" );
      writer.write( sb.toString() );
   }


   /**
    * Write html for a sentence from the document text
    *
    * @param sentence    sentence of interest
    * @param annotations identified annotations in the section
    * @param baseTokens  baseTokens in the section
    * @param relations   all relations
    * @param corefSpans  map of text span ends to coreference chain indices
    * @param writer      writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSentence( final Sentence sentence,
                                      final Collection<IdentifiedAnnotation> annotations,
                                      final Collection<BaseToken> baseTokens,
                                      final Collection<BinaryTextRelation> relations,
                                      final Map<TextSpan, Collection<Integer>> corefSpans,
                                      final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefSpans );
      writer.write( lineText + "\n<br>\n" );
   }

   /**
    * removes empty spans and replaces non-html compatible characters with their html ok equivalents
    *
    * @param sentence   -
    * @param baseTokens in the sentence
    * @return a map of text spans and their contained text
    */
   static private Map<TextSpan, String> createBaseTokenMap( final Sentence sentence,
                                                            final Collection<BaseToken> baseTokens ) {
      final int sentenceBegin = sentence.getBegin();
      final Map<TextSpan, String> baseItemMap = new LinkedHashMap<>();
      for ( BaseToken baseToken : baseTokens ) {
         final TextSpan textSpan = new DefaultTextSpan( baseToken, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         String text = getSafeText( baseToken );
         if ( text.isEmpty() ) {
            continue;
         }
         baseItemMap.put( textSpan, text );
      }
      return baseItemMap;
   }

   static private String getSafeText( final Annotation annotation ) {
      if ( annotation == null ) {
         return "";
      }
      return getSafeText( annotation.getCoveredText()
            .trim() );
   }

   static private String getSafeText( final String text ) {
      if ( text.isEmpty() ) {
         return "";
      }
      String safeText = text.replace( "'", "&apos;" );
      safeText = safeText.replace( "\"", "&quot;" );
      safeText = safeText.replace( "@", "&amp;" );
      safeText = safeText.replace( "<", "&lt;" );
      safeText = safeText.replace( ">", "&gt;" );
      return safeText;
   }

   /**
    * @param sentence    -
    * @param annotations annotations within the sentence
    * @return map of text spans and all annotations within those spans.  Accounts for overlap, etc.
    */
   static private Map<TextSpan, Collection<IdentifiedAnnotation>> createAnnotationMap( final Sentence sentence,
                                                                                       final Collection<IdentifiedAnnotation> annotations ) {
      final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap = new HashMap<>();
      final int sentenceBegin = sentence.getBegin();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final TextSpan textSpan = new DefaultTextSpan( annotation, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         final Collection<SemanticGroup> semanticGroups = SemanticGroup.getGroups( annotation );
         if ( !semanticGroups.isEmpty() ) {
            annotationMap.putIfAbsent( textSpan, new ArrayList<>() );
            annotationMap.get( textSpan )
                  .add( annotation );
         }
      }
      return annotationMap;
   }

   /**
    * sorts by begins, then by ends if begins are equal
    */
   static private class TextSpanComparator implements Comparator<TextSpan> {
      public int compare( final TextSpan t1, final TextSpan t2 ) {
         int r = t1.getBegin() - t2.getBegin();
         if ( r != 0 ) {
            return r;
         }
         return t1.getEnd() - t2.getEnd();
      }
   }

   static private final Comparator<TextSpan> TEXT_SPAN_COMPARATOR = new TextSpanComparator();

   /**
    * Creates map of text span indices and whether each span represents the beginning of one or more annotations,
    * the inside of two or more overlapping annotations, or the end of two or more overlapping annotations
    *
    * @param textSpans -
    * @return B I E map
    */
   static private Map<Integer, Character> createIndexMap( final Collection<TextSpan> textSpans ) {
      if ( textSpans.isEmpty() ) {
         return Collections.emptyMap();
      }
      final List<TextSpan> spanList = new ArrayList<>( textSpans );
      spanList.sort( TEXT_SPAN_COMPARATOR );
      final int spanCount = spanList.size();
      final int spanCountMinus = spanCount - 1;
      final Map<Integer, Character> indexMap = new HashMap<>();
      for ( int i = 0; i < spanCountMinus; i++ ) {
         final TextSpan textSpan = spanList.get( i );
         final int begin = textSpan.getBegin();
         indexMap.putIfAbsent( begin, 'B' );
         final int end = textSpan.getEnd();
         indexMap.putIfAbsent( end, 'E' );
         for ( int j = i + 1; j < spanCount; j++ ) {
            TextSpan nextSpan = spanList.get( j );
            if ( nextSpan.getBegin() > end ) {
               break;
            }
            if ( nextSpan.getBegin() > begin ) {
               indexMap.put( nextSpan.getBegin(), 'I' );
            }
            if ( nextSpan.getEnd() < end ) {
               indexMap.put( nextSpan.getEnd(), 'I' );
            } else if ( nextSpan.getEnd() > end ) {
               indexMap.put( end, 'I' );
            }
         }
      }
      final TextSpan lastSpan = spanList.get( spanCountMinus );
      indexMap.putIfAbsent( lastSpan.getBegin(), 'B' );
      indexMap.putIfAbsent( lastSpan.getEnd(), 'E' );
      return indexMap;
   }

   /**
    * @param indexMap map of text span indices and the B I E status of the spans
    * @return new spans representing the smallest required unique span elements of overlapping spans
    */
   static private Collection<TextSpan> createAdjustedSpans( final Map<Integer, Character> indexMap ) {
      if ( indexMap.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Integer> indexList = new ArrayList<>( indexMap.keySet() );
      Collections.sort( indexList );
      final int indexCount = indexList.size();
      final Collection<TextSpan> newSpans = new ArrayList<>();
      Integer index1 = indexList.get( 0 );
      Character c1 = indexMap.get( index1 );
      for ( int i = 1; i < indexCount; i++ ) {
         final Integer index2 = indexList.get( i );
         final Character c2 = indexMap.get( index2 );
         if ( c1.equals( 'B' ) || c1.equals( 'I' ) ) {
            newSpans.add( new DefaultTextSpan( index1, index2 ) );
         }
         index1 = index2;
         c1 = c2;
      }
      return newSpans;
   }

   /**
    * @param adjustedList  spans representing the smallest required unique span elements of overlapping spans
    * @param annotationMap map of larger overlapping text spans and their annotations
    * @return map of all annotations within or overlapping the small span elements
    */
   static private Map<TextSpan, Collection<IdentifiedAnnotation>> createAdjustedAnnotations(
         final List<TextSpan> adjustedList, final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap ) {
      final List<TextSpan> spanList = new ArrayList<>( annotationMap.keySet() );
      spanList.sort( TEXT_SPAN_COMPARATOR );
      final Map<TextSpan, Collection<IdentifiedAnnotation>> spanAnnotations = new HashMap<>( adjustedList.size() );
      final int spanCount = spanList.size();
      int previousMatchIndex = 0;
      for ( TextSpan adjusted : adjustedList ) {
         boolean matched = false;
         for ( int i = previousMatchIndex; i < spanCount; i++ ) {
            final TextSpan annotationsSpan = spanList.get( i );
            if ( annotationsSpan.overlaps( adjusted ) ) {
               if ( !matched ) {
                  previousMatchIndex = i;
                  matched = true;
               }
               spanAnnotations.putIfAbsent( adjusted, new HashSet<>() );
               spanAnnotations.get( adjusted )
                     .addAll( annotationMap.get( annotationsSpan ) );
            }
         }
      }
      return spanAnnotations;
   }

   static private Map<TextSpan, Collection<Integer>> getSentenceCorefs( final Sentence sentence,
                                                                        final Map<TextSpan, Collection<Integer>> corefSpans ) {
      final Map<TextSpan, Collection<Integer>> sentenceCorefs = new HashMap<>();
      final int sentenceBegin = sentence.getBegin();
      final int sentenceEnd = sentence.getEnd();
      for ( Map.Entry<TextSpan, Collection<Integer>> entry : corefSpans.entrySet() ) {
         final int entryBegin = entry.getKey()
               .getBegin();
         if ( entryBegin >= sentenceBegin && entryBegin < sentenceEnd ) {
            sentenceCorefs.put(
                  new DefaultTextSpan( entryBegin - sentenceBegin, entry.getKey()
                        .getEnd() - sentenceBegin ),
                  entry.getValue() );
         }
      }
      return sentenceCorefs;
   }

   /**
    * @param sentence      begin offset of sentence
    * @param annotationMap map of all annotations within or overlapping the small span elements
    * @param relations     all relations
    * @param corefSpans    map of text span ends to coreference chain indices
    * @return html for span elements
    */
   static private Map<Integer, String> createTags( final Sentence sentence,
                                                   final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap,
                                                   final Map<IdentifiedAnnotation, IdentifiedAnnotation> annotationEvents,
                                                   final Collection<BinaryTextRelation> relations,
                                                   final Map<TextSpan, Collection<Integer>> corefSpans
   ) {
      if ( annotationMap.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Collection<TextSpan> spans = new HashSet<>( annotationMap.keySet() );
      // TODO move coref adjustment uphill
      final Map<TextSpan, Collection<Integer>> sentenceCorefs = getSentenceCorefs( sentence, corefSpans );
      spans.addAll( sentenceCorefs.keySet() );
      final Map<Integer, Character> indexMap = createIndexMap( spans );
      final Collection<TextSpan> adjustedSpans = createAdjustedSpans( indexMap );
      final List<TextSpan> adjustedList = new ArrayList<>( adjustedSpans );
      adjustedList.sort( TEXT_SPAN_COMPARATOR );
      final Map<TextSpan, Collection<IdentifiedAnnotation>> adjustedAnnotations
            = createAdjustedAnnotations( adjustedList, annotationMap );

      final int sentenceBegin = sentence.getBegin();
      final Map<Integer, String> indexTags = new HashMap<>();
      for ( TextSpan adjustedSpan : adjustedList ) {
         final StringBuilder sb = new StringBuilder( "<span" );
         final Collection<IdentifiedAnnotation> annotations = adjustedAnnotations.get( adjustedSpan );
         final String polarityClasses = createPolaritiesText( annotations );
         if ( !polarityClasses.isEmpty() ) {
            sb.append( " class=\"" )
                  .append( polarityClasses )
                  .append( '\"' );
         }
         final String clickInfo = createClickInfo( annotations, annotationEvents, relations );
         if ( !clickInfo.isEmpty() ) {
            sb.append( " onClick=\"iaf(\'" )
                  .append( clickInfo )
                  .append( "\')\"" );
         }
         final String tip = createTipText( annotations );
         if ( !tip.isEmpty() ) {
            sb.append( " " + TOOL_TIP + "=\"" )
                  .append( tip )
                  .append( '\"' );
         }
         sb.append( '>' );
         // coref chain
         final int adjustedEnd = sentenceBegin + adjustedSpan.getEnd();
         final StringBuilder sb2 = new StringBuilder();
         final Collection<IdentifiedAnnotation> endAnnotations = getEndAnnotations( annotations, adjustedEnd );
         final Collection<SemanticGroup> groups = endAnnotations.stream()
               .map( SemanticGroup::getGroups )
               .flatMap( Collection::stream )
               .distinct()
               .collect( Collectors.toList() );
         final Collection<String> encodings = groups.stream()
               .map( SemanticMarkup::getMarkup )
               .map( SemanticMarkup::getEncoding )
               .sorted()
               .collect( Collectors.toList() );
         String firstEncoding = encodings.stream()
               .findAny()
               .orElse( SemanticMarkup.UNKNOWN_MARK.getEncoding() );
         if ( annotations != null && endAnnotations.size() != annotations.size() ) {
            firstEncoding += " " + polarityClasses;
         }
         final Collection<Integer> chains = sentenceCorefs.get( adjustedSpan );
         if ( chains != null && !chains.isEmpty() ) {
            for ( Integer chain : chains ) {
//               sb2.append( "<span class=\"" ).append( semantic ).append( "\"" );
               sb2.append( "<span class=\"" )
                     .append( firstEncoding )
                     .append( "\"" );
               sb2.append( " onClick=\"crf" )
                     .append( chain )
                     .append( "()\">" );
               sb2.append( "<sup>" )
                     .append( chain )
                     .append( "</sup></span>" );
            }
         } else {
            for ( SemanticGroup group : groups ) {
               if ( group == SemanticGroup.EVENT
                     || group == SemanticGroup.TIME
                     || group == SemanticGroup.ENTITY
                     || group == SemanticGroup.UNKNOWN ) {
                  continue;
               }
               final SemanticMarkup markup = SemanticMarkup.getMarkup( group );
               sb2.append( "<span class=\"" )
                     .append( markup.getEncoding() );
               if ( endAnnotations.size() != annotations.size() ) {
                  sb2.append( " " )
                        .append( polarityClasses );
               }
               sb2.append( "\"></span>" );
            }
         }

         final Integer begin = adjustedSpan.getBegin();
         final String previousTag = indexTags.getOrDefault( begin, "" );
         indexTags.put( begin, previousTag + sb.toString() );
         indexTags.put( adjustedSpan.getEnd(), "</span>" + sb2.toString() );
      }
      return indexTags;
   }

   static private Collection<IdentifiedAnnotation> getEndAnnotations( final Collection<IdentifiedAnnotation> annotations,
                                                                      final int adjustedEnd ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return Collections.emptyList();
      }
      return annotations.stream()
            .filter( a -> a.getEnd() == adjustedEnd )
            .collect( Collectors.toSet() );
   }

   /**
    * @param annotations -
    * @return html with annotation information: polarity, semantic, cui, text, pref text
    */
   static private String createClickInfo( final Collection<IdentifiedAnnotation> annotations,
                                          final Map<IdentifiedAnnotation, IdentifiedAnnotation> annotationEvents,
                                          final Collection<BinaryTextRelation> relations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return "";
      }
      final Map<String, Map<String, Collection<String>>> polarInfoMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String polarity = createPolarity( annotation );
         polarInfoMap.putIfAbsent( polarity, new HashMap<>() );
         final IdentifiedAnnotation event = annotationEvents.get( annotation );
         final Map<String, Collection<String>> infoMap = createInfoMap( annotation, event, relations );
         for ( Map.Entry<String, Collection<String>> infoEntry : infoMap.entrySet() ) {
            polarInfoMap.get( polarity )
                  .putIfAbsent( infoEntry.getKey(), new HashSet<>() );
            polarInfoMap.get( polarity )
                  .get( infoEntry.getKey() )
                  .addAll( infoEntry.getValue() );
         }
      }
      final List<String> polarities = new ArrayList<>( polarInfoMap.keySet() );
      Collections.sort( polarities );
      final StringBuilder sb = new StringBuilder();
      for ( String polarity : polarities ) {
         sb.append( polarity )
               .append( NEWLINE );
         final Map<String, Collection<String>> infoMap = polarInfoMap.get( polarity );
         final List<String> semantics = new ArrayList<>( infoMap.keySet() );
         Collections.sort( semantics );
         for ( String semantic : semantics ) {
            sb.append( semantic )
                  .append( NEWLINE );
            final List<String> texts = new ArrayList<>( infoMap.get( semantic ) );
            Collections.sort( texts );
            for ( String text : texts ) {
               sb.append( text )
                     .append( NEWLINE );
            }
         }
      }
      return sb.toString();
   }

   /**
    * @param annotation -
    * @return map of semantic to text for annotations
    */
   static private Map<String, Collection<String>> createInfoMap( final IdentifiedAnnotation annotation,
                                                                 final IdentifiedAnnotation event,
                                                                 final Collection<BinaryTextRelation> relations ) {
      final Collection<UmlsConcept> concepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      final Map<String, Collection<String>> semanticMap = new HashMap<>();
      final String coveredText = getCoveredText( annotation );
      final String safeText = getSafeText( coveredText );
      String relationText = getRelationText( annotation, relations );
      if ( event != null ) {
         relationText += getRelationText( event, relations );
      }
      for ( UmlsConcept concept : concepts ) {
         final SemanticGroup group = SemanticTui.getTui( concept )
               .getGroup();
         final String encoding = SemanticMarkup.getMarkup( group )
               .getEncoding();
         semanticMap.putIfAbsent( encoding, new HashSet<>() );
         final String prefText = getPreferredText( coveredText, concept );
         String text = getWikiText( safeText, prefText ) + NEWLINE + getCodes( concept ) + getCodedPrefText(
               prefText ) + relationText;
         if ( annotation instanceof EventMention ) {
            text += getDocTimeRel( (EventMention) annotation );
         }
         semanticMap.get( encoding )
               .add( text );
      }
      if ( concepts.isEmpty() ) {
         String postText = "";
         final SemanticGroup group = SemanticGroup.getBestGroup( annotation );
         final String encoding = SemanticMarkup.getMarkup( group )
               .getEncoding();
         if ( annotation instanceof EventMention ) {
            postText = getDocTimeRel( (EventMention) annotation );
         }
         semanticMap.putIfAbsent( encoding, new HashSet<>() );
         semanticMap.get( encoding )
               .add( safeText + NEWLINE + postText + relationText );
      }
      return semanticMap;
   }

   static private String createWikiLink( final String coveredText, final String wikiText ) {
      return WIKI_BEGIN + wikiText + WIKI_CENTER + coveredText + WIKI_END;
   }

   /**
    * @param concept -
    * @return cui if it exists and any codes if they exist
    */
   static private String getCodes( final UmlsConcept concept ) {
      String codes = "";
      final String cui = concept.getCui();
      if ( cui != null && !cui.isEmpty() ) {
         codes += SPACER + cui + NEWLINE;
      }
      final String tui = concept.getTui();
      if ( tui != null && !tui.isEmpty() ) {
         codes += SPACER + tui + NEWLINE;
      }
      final String code = concept.getCode();
      if ( code != null && !code.isEmpty() ) {
         codes += SPACER + code + NEWLINE;
      }
      return codes;
   }

   /**
    * @param annotation -
    * @return the covered text
    */
   static private String getCoveredText( final IdentifiedAnnotation annotation ) {
      return annotation.getCoveredText()
            .replace( '\r', ' ' )
            .replace( '\n', ' ' );
   }

   /**
    * @param coveredText -
    * @param concept     -
    * @return the covered text plus preferred text if it exists and is not equal to the covered text
    */
   static private String getPreferredText( final String coveredText, final UmlsConcept concept ) {
      final String preferredText = concept.getPreferredText();
      if ( preferredText != null && !preferredText.isEmpty()
            && !preferredText.equals( PREFERRED_TERM_UNKNOWN )
            && !preferredText.equalsIgnoreCase( coveredText )
            && !preferredText.equalsIgnoreCase( coveredText + 's' )
            && !coveredText.equalsIgnoreCase( preferredText + 's' ) ) {
         return getSafeText( preferredText );
      }
      return "";
   }

   static private String getCodedPrefText( final String preferredText ) {
      if ( !preferredText.isEmpty() ) {
         return SPACER + "[" + preferredText + "]" + NEWLINE;
      }
      return "";
   }

   static private String getWikiText( final String coveredText, final String preferredText ) {
      String wikiText = coveredText;
      // oddly enough, searches more frequently have the covered text instead of the preferred text
//      if ( preferredText != null && !preferredText.isEmpty() && !preferredText.contains( "," ) ) {
//         wikiText = preferredText;
//      }
//      return WIKI_BEGIN + wikiText.replace( ' ', '_' ).toLowerCase() + WIKI_CENTER + coveredText + WIKI_END;  // wikipedia
//      return WIKI_BEGIN + wikiText.replace( ' ', '%' ).toLowerCase() + WIKI_CENTER + coveredText + WIKI_END;  // webmd
      return WIKI_BEGIN + wikiText.replace( ' ', '+' )
            .toLowerCase() + WIKI_CENTER + coveredText + WIKI_END;  // most sites
   }

   /**
    * @param eventMention -
    * @return a line of text with doctimerel if available
    */
   static private String getDocTimeRel( final EventMention eventMention ) {
      final Event event = eventMention.getEvent();
      if ( event == null ) {
         return "";
      }
      final EventProperties eventProperties = event.getProperties();
      if ( eventProperties == null ) {
         return "";
      }
      final String dtr = eventProperties.getDocTimeRel();
      if ( dtr == null || dtr.isEmpty() ) {
         return "";
      }
      return SPACER + "[" + dtr.toLowerCase() + "] doc time" + NEWLINE;
   }

   /**
    * @param annotations -
    * @return polarity representation for all provided annotations
    */
   static private String createPolaritiesText( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return GENERIC;
      }
      return annotations.stream()
            .map( HtmlTextWriter::createPolarity )
            .distinct()
            .sorted()
            .collect( Collectors.joining( " " ) );
   }

   /**
    * @param annotation -
    * @return polarity for a single annotation
    */
   static private String createPolarity( final IdentifiedAnnotation annotation ) {
      if ( annotation instanceof TimeMention || annotation instanceof EntityMention ) {
         return GENERIC;
      }
      if ( annotation.getPolarity() < 0 ) {
         if ( annotation.getUncertainty() > 0 ) {
            return UNCERTAIN_NEGATED;
         } else {
            return NEGATED;
         }
      } else if ( annotation.getUncertainty() > 0 ) {
         return UNCERTAIN;
      } else {
         return AFFIRMED;
      }
   }

   /**
    * @param annotations -
    * @return tooltip text with semantic names for given annotations
    */
   static private String createTipText( final Collection<IdentifiedAnnotation> annotations ) {
      if ( annotations == null || annotations.isEmpty() ) {
         return "";
      }
      final Map<String, Integer> semanticCounts = getSemanticCounts( annotations );
      final List<String> semantics = new ArrayList<>( semanticCounts.keySet() );
      Collections.sort( semantics );
      final StringBuilder sb = new StringBuilder();
      for ( String semanticName : semantics ) {
         sb.append( semanticName );
         final int count = semanticCounts.get( semanticName );
         if ( count > 1 ) {
            sb.append( '(' )
                  .append( count )
                  .append( ')' );
         }
         sb.append( ' ' );
      }
      return sb.toString();
   }

   static private String getRelationText( final IdentifiedAnnotation annotation,
                                          final Collection<BinaryTextRelation> relations ) {
      return relations.stream()
            .map( r -> getRelationText( annotation, r ) )
            .collect( Collectors.joining() );
   }

   static private String getRelationText( final IdentifiedAnnotation annotation,
                                          final BinaryTextRelation relation ) {
      if ( relation.getArg1()
            .getArgument()
            .equals( annotation ) ) {
         return SPACER + "[" + relation.getCategory() + "] " + getSafeText( relation.getArg2()
               .getArgument() ) + NEWLINE;
      } else if ( relation.getArg2()
            .getArgument()
            .equals( annotation ) ) {
         return SPACER + getSafeText( relation.getArg1()
               .getArgument() ) + " [" + relation.getCategory() + "]" + NEWLINE;
      }
      return "";
   }

   /**
    * @param annotations -
    * @return counts of semantic types for annotations
    */
   static private Map<String, Integer> getSemanticCounts( final Collection<IdentifiedAnnotation> annotations ) {
      // Check concepts with the same cui can have multiple tuis.  This can make it look like there are extra counts.
      final Collection<String> usedCuis = new HashSet<>();
      final Map<String, Integer> semanticCounts = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String annotationSemanticName = SemanticGroup.getBestGroup( annotation )
               .getName();
         final Collection<UmlsConcept> concepts = OntologyConceptUtil.getUmlsConcepts( annotation );
         for ( UmlsConcept concept : concepts ) {
            if ( !usedCuis.add( concept.getCui() ) ) {
               continue;
            }
            String semanticName = SemanticTui.getTui( concept )
                  .getGroupName();
            if ( semanticName.equals( SemanticGroup.UNKNOWN.getName() ) ) {
               semanticName = annotationSemanticName;
            }
            semanticCounts.putIfAbsent( semanticName, 0 );
            final int count = semanticCounts.get( semanticName );
            semanticCounts.put( semanticName, count + 1 );
         }
         usedCuis.clear();
         if ( concepts.isEmpty() ) {
            semanticCounts.putIfAbsent( annotationSemanticName, 0 );
            final int count = semanticCounts.get( annotationSemanticName );
            semanticCounts.put( annotationSemanticName, count + 1 );
         }
      }
      return semanticCounts;
   }

   /**
    * This method needs to be in this class so that it can properly link the coref chain numbers
    *
    * @param corefRelations -
    * @param writer         writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeCorefInfos( final Collection<CollectionTextRelation> corefRelations, final BufferedWriter writer )
         throws IOException {
      if ( corefRelations == null || corefRelations.isEmpty() ) {
         return;
      }
      int index = 1;
      for ( CollectionTextRelation corefRelation : corefRelations ) {
         final FSList chainHead = corefRelation.getMembers();
         final Collection<IdentifiedAnnotation> markables
               = FSCollectionFactory.create( chainHead, IdentifiedAnnotation.class );
         final String text = markables.stream()
               .sorted( Comparator.comparingInt( Annotation::getBegin ) )
               .map( HtmlTextWriter::getSafeText )
               .collect( Collectors.joining( "<br>" ) );
         writer.write( "  function crf" + index + "() {\n" );
         writer.write(
               "    document.getElementById(\"ia\").innerHTML = \"<br><h3>Coreference Chain</h3>" + text + "\";\n" );
         writer.write( "  }\n" );
         index++;
      }
   }

   static private String getLegend() {
      return "<div class=\"legend\"><h3>Legend</h3>\n" +
            "  <hr>\n" +
            "  <table style=\"line-height: 120%\">\n" +
            "    <tr>\n" +
            "      <td><span class=\"" + AFFIRMED + "\">Affirmed Event</span></td>\n" +
            "      <td><span class=\"" + NEGATED + "\">Negated Event</span></td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "      <td><span class=\"" + UNCERTAIN + "\">Uncertain Event</span></td>\n" +
            "      <td><span class=\"" + UNCERTAIN_NEGATED + "\">Uncertain Negated</span></td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "      <td><span class=\"" + GENERIC + "\">Time or Generic</span></td>\n" +
            "    </tr>\n" +
            "  </table>\n" +
            "  <hr>\n" +
            "  <table>\n" +
            "    <tr>\n" +
            "      <td>Sign / Symptom<span class=\"" + SemanticMarkup.FINDING_MARK.getEncoding() + "\"></span></td>\n" +
            "      <td>Procedure<span class=\"" + SemanticMarkup.PROCEDURE_MARK.getEncoding() + "\"></span></td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "      <td>Disease / Disorder<span class=\"" + SemanticMarkup.DISORDER_MARK.getEncoding() + "\"></span></td>\n" +
            "      <td>Medication<span class=\"" + SemanticMarkup.DRUG_MARK.getEncoding() + "\"></span></td>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "      <td>Anatomical Site<span class=\"" + SemanticMarkup.ANATOMY_MARK.getEncoding() + "\"></span></td>\n" +
            "    </tr>\n" +
            "  </table>\n" +
            "  <hr>\n" +
            "  <table>\n" +
            "    <tr>\n" +
            "      <td>Coreference Element<span class=\"" + SemanticMarkup.ENTITY_MARK.getEncoding() + "\"><sup>1</sup></span></td>\n" +
            "    </tr>\n" +
            "  </table>\n" +
            "</div>\n";
   }

   /**
    * @return html to end body
    */
   static private String endBody() {
      return "</body>\n" +
            "</html>\n";
   }


}
